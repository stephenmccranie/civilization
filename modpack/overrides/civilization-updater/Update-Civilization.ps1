param([string]$InstancePath = $env:INST_DIR)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression

function Get-CivJson([string]$Url) {
    Invoke-RestMethod -Uri $Url -Headers @{ 'User-Agent'='Civilization-updater/1' } -TimeoutSec 15
}
function Save-CivDownload([string]$Url, [string]$Path) {
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Path -TimeoutSec 90
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
        Save-CivDownload $manifest.downloadUrl $partial
        if ((Get-Item -LiteralPath $partial).Length -ne $manifest.size -or (Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash.ToLowerInvariant() -ne $manifest.sha256) { throw 'Update checksum or size mismatch' }
        if ((Get-CivJarVersion $partial) -ne $manifest.version) { throw 'Update version mismatch' }
        Assert-CivStopped $game
        $lock = [IO.File]::Open($current, 'Open', 'ReadWrite', 'None'); $lock.Dispose()
        # Same-volume atomic replacement; the previous mod stays outside the mods folder.
        [IO.File]::Replace($partial, $current, (Join-Path $homeDir 'previous.jar'), $true)
        Write-Host "Civilization updated: $version -> $($manifest.version)"
    } catch { Write-Warning "Civilization update skipped: $($_.Exception.Message). Keeping $version." }
    finally {
        $partial = Join-Path $homeDir 'download.partial'
        if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial }
    }
}
if ($MyInvocation.InvocationName -ne '.') {
    try { Invoke-CivUpdate $InstancePath } catch { Write-Error $_; exit 1 }
}
