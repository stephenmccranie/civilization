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
    # PowerShell 5.1 streams a REST array as one object unless returned explicitly.
    $result=Invoke-RestMethod -Uri $Url -Headers @{ 'User-Agent'='Civilization-updater/1' } -TimeoutSec 15
    return $result
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
