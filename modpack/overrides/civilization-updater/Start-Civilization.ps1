param([string]$InstancePath = $env:INST_DIR)
. "$PSScriptRoot/Update-Civilization.ps1" -InstancePath $InstancePath

function Test-CivPinnedFile([string]$Path, $Item) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    return (Get-Item -LiteralPath $Path).Length -eq $Item.fileSize -and
        (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant() -eq $Item.hashes.sha1 -and
        (Get-FileHash -LiteralPath $Path -Algorithm SHA512).Hash.ToLowerInvariant() -eq $Item.hashes.sha512
}
function Initialize-CivClient([string]$Game) {
    $homeDir = Join-Path $Game 'civilization-updater'
    $manifestPath = Join-Path $homeDir 'client-files.json'
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.formatVersion -ne 1 -or $manifest.game -ne 'minecraft' -or $manifest.versionId -notmatch '^playtest-\d+\.\d+\.\d+-\d+$') { throw 'Unsupported client manifest' }
    $paths = @{}
    foreach ($item in $manifest.files) {
        if ($item.path -notmatch '^(mods|shaderpacks|resourcepacks)/[a-zA-Z0-9_.+() -]+\.(jar|zip)$' -or $paths.ContainsKey($item.path)) { throw 'Unsafe or duplicate client file path' }
        $paths[$item.path]=$true
        if ($item.hashes.sha1 -notmatch '^[0-9a-f]{40}$' -or $item.hashes.sha512 -notmatch '^[0-9a-f]{128}$' -or $item.fileSize -le 0 -or $item.fileSize -gt 536870912 -or @($item.downloads).Count -ne 1) { throw 'Invalid pinned download metadata' }
        $uri = [uri]$item.downloads[0]
        if ($uri.Scheme -ne 'https' -or $uri.UserInfo -or !$uri.IsDefaultPort -or $uri.Fragment) { throw 'Unsafe client download URL' }
        if ($uri.Host -eq 'github.com') { Assert-CivUrl $uri.AbsoluteUri $manifest.versionId }
        elseif ($uri.Host -ne 'cdn.modrinth.com') { throw 'Unapproved client download host' }
    }
    $civItems = @($manifest.files | Where-Object { $_.path -match '^mods/civilization-.*\.jar$' })
    if ($civItems.Count -ne 1) { throw 'Expected one pinned Civilization mod' }
    if ($civItems[0].path -notmatch '^mods/civilization-(\d+\.\d+\.\d+)-dev.jar$') { throw 'Invalid pinned Civilization version' }
    $baselineVersion = [version]$Matches[1]
    $mods = Join-Path $Game 'mods'
    New-Item -ItemType Directory -Path $mods -Force | Out-Null
    $civJars = @(Get-ChildItem -LiteralPath $mods -Filter 'civilization-*.jar' -File)
    if ($civJars.Count -gt 1) { throw 'Resolve duplicate Civilization JARs before launch' }
    $keepCiv = $false
    if ($civJars.Count -eq 1) {
        $installedVersion = [version]((Get-CivJarVersion $civJars[0].FullName) -replace '-dev$','')
        $keepCiv = $installedVersion -ge $baselineVersion
        if (!$keepCiv -and $civJars[0].Name -ne (Split-Path $civItems[0].path -Leaf)) { throw 'Use a fresh instance for this pack baseline' }
    }
    $stamp = (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash
    $receipt = Join-Path $homeDir 'installed.txt'
    $missing = @($manifest.files | Where-Object { $_ -ne $civItems[0] -and !(Test-Path -LiteralPath (Join-Path $Game $_.path) -PathType Leaf) })
    if ($keepCiv -and $missing.Count -eq 0 -and (Test-Path -LiteralPath $receipt) -and [IO.File]::ReadAllText($receipt) -eq $stamp) { return }
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $partial = Join-Path $homeDir 'bootstrap.partial'
    try {
        foreach ($item in $manifest.files) {
            if ($item -eq $civItems[0] -and $keepCiv) { continue }
            $destination = Join-Path $Game $item.path
            if (Test-CivPinnedFile $destination $item) { continue }
            Write-Host "Installing $($item.path)..."
            Save-CivDownload $item.downloads[0] $partial
            if (!(Test-CivPinnedFile $partial $item)) { throw "Download verification failed: $($item.path)" }
            $zip = [IO.Compression.ZipFile]::OpenRead($partial); $zip.Dispose()
            New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
            if (Test-Path -LiteralPath $destination) { [IO.File]::Replace($partial, $destination, $null) }
            else { [IO.File]::Move($partial, $destination) }
        }
        [IO.File]::WriteAllText($receipt, $stamp)
        Write-Host 'Civilization client files are ready.'
    } finally { if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial } }
}
function Start-CivClient([string]$Instance) {
    if (!$Instance -or !(Test-Path -LiteralPath (Join-Path $Instance 'mmc-pack.json'))) { throw 'Expected a Prism instance directory' }
    $game = Get-CivGameDirectory $Instance
    Assert-CivStopped $game
    $guard = [IO.File]::Open((Join-Path $game 'civilization-updater/update.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    try {
        Initialize-CivClient $game
        Invoke-CivUpdateLocked $Instance
    } finally { $guard.Dispose() }
}
if ($MyInvocation.InvocationName -ne '.') {
    try { Start-CivClient $InstancePath } catch { Write-Error $_; exit 1 }
}
