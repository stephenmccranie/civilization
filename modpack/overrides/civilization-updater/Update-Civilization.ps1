param([string]$InstancePath = $env:INST_DIR)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$script:CivShowProgress = $false
$script:CivProgress = $null

function New-CivProgressWindow {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
        $form = [Windows.Forms.Form]::new()
        $form.Text = 'Civilization Launcher'
        $form.ClientSize = [Drawing.Size]::new(520,160)
        $form.StartPosition = 'CenterScreen'
        $form.FormBorderStyle = 'FixedDialog'
        $form.MaximizeBox = $false; $form.MinimizeBox = $false
        $form.ControlBox = $false
        $heading = [Windows.Forms.Label]::new(); $heading.SetBounds(20,16,480,28)
        $heading.Font = [Drawing.Font]::new('Segoe UI',12,[Drawing.FontStyle]::Bold)
        $label = [Windows.Forms.Label]::new(); $label.SetBounds(20,50,480,38)
        $bar = [Windows.Forms.ProgressBar]::new(); $bar.SetBounds(20,94,480,22)
        $status = [Windows.Forms.Label]::new(); $status.SetBounds(20,126,480,24)
        $form.Controls.AddRange(@($heading,$label,$bar,$status))
        return @{Form=$form;Heading=$heading;Label=$label;Bar=$bar;Status=$status;Expected=0L}
}
function Open-CivProgress([string]$Title, [string]$Detail, [long]$ExpectedBytes=0) {
    if (!$script:CivShowProgress) { return }
    if (!$script:CivProgress) {
        $script:CivProgress=New-CivProgressWindow
        $script:CivProgress.Form.Show(); $script:CivProgress.Form.Activate()
    }
    $script:CivProgress.Heading.Text = $Title
    $script:CivProgress.Label.Text = $Detail
    $script:CivProgress.Expected = $ExpectedBytes
    $script:CivProgress.Status.Text = 'Please wait. Minecraft will start automatically.'
    $script:CivProgress.Bar.Style = 'Marquee'
    [Windows.Forms.Application]::DoEvents()
}
function Update-CivProgress([long]$Bytes) {
    if (!$script:CivProgress) { return }
    $expected=$script:CivProgress.Expected
    if ($expected -gt 0) {
        $script:CivProgress.Bar.Style='Continuous'
        $script:CivProgress.Bar.Value=[int][Math]::Min(100,100.0*$Bytes/$expected)
        $script:CivProgress.Status.Text=('{0:N1} / {1:N1} MB downloaded' -f ($Bytes/1MB),($expected/1MB))
    }
    [Windows.Forms.Application]::DoEvents()
}
function Close-CivProgress {
    if ($script:CivProgress) { $script:CivProgress.Form.Close(); $script:CivProgress.Form.Dispose(); $script:CivProgress=$null }
}
function Show-CivFailure([string]$Message) {
    if (!$script:CivShowProgress) { return }
    Add-Type -AssemblyName System.Windows.Forms
    [Windows.Forms.MessageBox]::Show($Message,'Civilization Launcher','OK','Error') | Out-Null
}

function Get-CivJson([string]$Url) {
    Invoke-RestMethod -Uri $Url -Headers @{ 'User-Agent'='Civilization-updater/1' } -TimeoutSec 15
}
function Save-CivDownload([string]$Url, [string]$Path) {
    $client=[Net.WebClient]::new()
    $client.Headers['User-Agent']='Civilization-updater/1'
    try {
        # Complete on the thread pool, independently of the optional UI message loop.
        $context=[Threading.SynchronizationContext]::Current
        try {
            [Threading.SynchronizationContext]::SetSynchronizationContext($null)
            $task=$client.DownloadFileTaskAsync([uri]$Url,$Path)
        } finally { [Threading.SynchronizationContext]::SetSynchronizationContext($context) }
        $watch=[Diagnostics.Stopwatch]::StartNew(); $lastBytes=-1L
        while (!$task.IsCompleted) {
            $bytes=if (Test-Path -LiteralPath $Path) { (Get-Item -LiteralPath $Path).Length } else { 0L }
            if ($bytes -ne $lastBytes) { $watch.Restart(); $lastBytes=$bytes }
            Update-CivProgress $bytes
            if ($watch.Elapsed.TotalSeconds -gt 90) { $client.CancelAsync(); throw 'Download stalled for 90 seconds' }
            [Threading.Thread]::Sleep(100)
        }
        $task.GetAwaiter().GetResult()
        if (Test-Path -LiteralPath $Path) { Update-CivProgress (Get-Item -LiteralPath $Path).Length }
    } finally { $client.Dispose() }
}
function Assert-CivUrl([string]$Url, [string]$Tag) {
    $prefix = 'https://github.com/stephenmccranie/civilization/releases/download/' + $Tag + '/'
    if (!$Url.StartsWith($prefix, [StringComparison]::Ordinal) -or $Url.Substring($prefix.Length) -notmatch '^[a-zA-Z0-9_.-]+$') { throw 'Unexpected release asset URL' }
}
function Get-CivJarVersion([string]$Path) {
    $zip = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $zip.GetEntry('META-INF/neoforge.mods.toml')
        if (!$entry -or $entry.Length -gt 65536) { throw 'Missing mod metadata' }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ($metadata -notmatch '(?m)^modId\s*=\s*"civilization"') { throw 'Not a Civilization JAR' }
        if ($metadata -notmatch '(?m)^version\s*=\s*"(\d+\.\d+\.\d+-dev)"') { throw 'Unexpected mod version' }
        return $Matches[1]
    } finally { $zip.Dispose() }
}
function Get-CivGameDirectory([string]$Instance) {
    foreach ($name in @('minecraft', '.minecraft')) {
        $path = Join-Path $Instance $name
        if (Test-Path -LiteralPath $path -PathType Container) { return $path }
    }
    throw 'Prism Minecraft directory not found'
}
function Assert-CivStopped([string]$Game) {
    foreach ($process in @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'")) {
        if ($process.CommandLine -and $process.CommandLine.Replace('/','\').IndexOf($Game.Replace('/','\'), [StringComparison]::OrdinalIgnoreCase) -ge 0) { throw 'Close this Minecraft instance before updating' }
    }
}
function Invoke-CivUpdate([string]$Instance) {
    if (!$Instance -or !(Test-Path -LiteralPath (Join-Path $Instance 'mmc-pack.json'))) { throw 'Expected a Prism instance directory' }
    $game = Get-CivGameDirectory $Instance
    New-Item -ItemType Directory -Path (Join-Path $game 'civilization-updater') -Force | Out-Null
    $lockPath = Join-Path $game 'civilization-updater/update.lock'
    $guard = [IO.File]::Open($lockPath, 'OpenOrCreate', 'ReadWrite', 'None')
    try { Invoke-CivUpdateLocked $Instance } finally { $guard.Dispose() }
}
function Invoke-CivUpdateLocked([string]$Instance) {
    if (!$Instance -or !(Test-Path -LiteralPath (Join-Path $Instance 'mmc-pack.json'))) { throw 'Expected a Prism instance directory' }
    $Instance = (Resolve-Path -LiteralPath $Instance).Path
    $game = Get-CivGameDirectory $Instance
    $homeDir = Join-Path $game 'civilization-updater'
    $mods = Join-Path $game 'mods'
    $jars = @(Get-ChildItem -LiteralPath $mods -Filter 'civilization-*.jar' -File)
    if ($jars.Count -ne 1) { throw 'Expected exactly one Civilization JAR; resolve missing or duplicate mods first' }
    $current = $jars[0].FullName
    $version = Get-CivJarVersion $current
    Assert-CivStopped $game
    # A failed network check leaves the verified installed mod available for offline play.
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $releases = @(Get-CivJson 'https://api.github.com/repos/stephenmccranie/civilization/releases?per_page=100')
        $release = $releases | Where-Object { !$_.draft -and $_.tag_name -match '^playtest-\d+\.\d+\.\d+-\d+$' -and @($_.assets | Where-Object name -eq 'civilization-update.json').Count -eq 1 } | Sort-Object published_at -Descending | Select-Object -First 1
        if (!$release) { Write-Host 'Civilization: no approved updater release yet.'; return }
        $asset = $release.assets | Where-Object name -eq 'civilization-update.json'
        Assert-CivUrl $asset.browser_download_url $release.tag_name
        $manifest = Get-CivJson $asset.browser_download_url
        if ($manifest.schemaVersion -ne 1 -or $manifest.channel -ne 'playtest' -or $manifest.releaseTag -ne $release.tag_name -or $manifest.version -notmatch '^\d+\.\d+\.\d+-dev$') { throw 'Unsupported update manifest' }
        if ([version]($manifest.version -replace '-dev$','') -le [version]($version -replace '-dev$','')) { Write-Host "Civilization: keeping $version."; return }
        $pack = Get-Content -LiteralPath (Join-Path $Instance 'mmc-pack.json') -Raw | ConvertFrom-Json
        $mc = @($pack.components | Where-Object uid -eq 'net.minecraft')[0].version
        $nf = @($pack.components | Where-Object uid -eq 'net.neoforged')[0].version
        if ($mc -ne $manifest.minecraftVersion -or [version]$nf -lt [version]$manifest.minimumNeoForgeVersion -or $manifest.protocol -ne '3/0.35.0-dev') {
            Write-Warning 'This release needs a full client-pack upgrade. Keeping the installed mod.'; return
        }
        Assert-CivUrl $manifest.downloadUrl $release.tag_name
        if ($manifest.sha256 -notmatch '^[0-9a-f]{64}$' -or $manifest.size -le 0 -or $manifest.size -gt 67108864) { throw 'Invalid artifact metadata' }
        $partial = Join-Path $homeDir 'download.partial'
        Open-CivProgress 'Updating Civilization' "$version -> $($manifest.version)" $manifest.size
        Save-CivDownload $manifest.downloadUrl $partial
        Open-CivProgress 'Updating Civilization' 'Verifying the downloaded mod...'
        if ((Get-Item -LiteralPath $partial).Length -ne $manifest.size -or (Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash.ToLowerInvariant() -ne $manifest.sha256) { throw 'Update checksum or size mismatch' }
        if ((Get-CivJarVersion $partial) -ne $manifest.version) { throw 'Update version mismatch' }
        Assert-CivStopped $game
        $lock = [IO.File]::Open($current, 'Open', 'ReadWrite', 'None'); $lock.Dispose()
        # Same-volume atomic replacement; the previous mod stays outside the mods folder.
        [IO.File]::Replace($partial, $current, (Join-Path $homeDir 'previous.jar'), $true)
        Write-Host "Civilization updated: $version -> $($manifest.version)"
    } catch {
        Write-Warning "Civilization update skipped: $($_.Exception.Message). Keeping $version."
        if ($script:CivProgress) { Show-CivFailure "The update could not finish. Keeping $version and continuing launch.`n`n$($_.Exception.Message)" }
    }
    finally {
        Close-CivProgress
        $partial = Join-Path $homeDir 'download.partial'
        if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial }
    }
}
if ($MyInvocation.InvocationName -ne '.') {
    $script:CivShowProgress=$true
    try { Invoke-CivUpdate $InstancePath } catch { Show-CivFailure $_.Exception.Message; Write-Error $_; exit 1 } finally { Close-CivProgress }
}
