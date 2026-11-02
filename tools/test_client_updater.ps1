# Focused offline failure tests; all fixtures stay in ignored scratch storage.
$ErrorActionPreference='Stop'
$root = Split-Path $PSScriptRoot -Parent
$source = Join-Path $root 'modpack/overrides/civilization-updater'
. "$source/Update-Civilization.ps1"
$fixture = Join-Path $root ('.tools/updater-tests/' + [guid]::NewGuid().ToString('N'))
$game = Join-Path $fixture 'minecraft'
New-Item -ItemType Directory -Path "$game/mods","$game/civilization-updater" -Force | Out-Null
'{"components":[{"uid":"net.minecraft","version":"1.21.1"},{"uid":"net.neoforged","version":"21.1.265"}]}' | Set-Content "$fixture/mmc-pack.json"
function New-TestJar([string]$Path,[string]$Version) {
    $stream=[IO.File]::Create($Path)
    $zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
    $writer=[IO.StreamWriter]::new($zip.CreateEntry('META-INF/neoforge.mods.toml').Open())
    $writer.Write("[[mods]]`nmodId=`"civilization`"`nversion=`"$Version`"`n")
    $writer.Dispose(); $zip.Dispose(); $stream.Dispose()
}
$current="$game/mods/civilization-0.35.0-dev.jar"
$candidate="$fixture/candidate.jar"
New-TestJar $current '0.35.0-dev'
New-TestJar $candidate '0.35.4-dev'
$original=(Get-FileHash $current).Hash
$script:manifest=@{schemaVersion=1;channel='playtest';releaseTag='playtest-0.35.4-1';version='0.35.4-dev';protocol='3/0.35.0-dev';minecraftVersion='1.21.1';minimumNeoForgeVersion='21.1.265';downloadUrl='https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.4-1/civilization-0.35.4-dev.jar';sha256=(Get-FileHash $candidate).Hash.ToLowerInvariant();size=(Get-Item $candidate).Length}
$script:offline=$false; $script:corrupt=$false; $script:running=$false; $script:downloads=0
function Assert-CivStopped([string]$Game) { if ($script:running) { throw 'Running fixture' } }
function Get-CivJson([string]$Url) {
    if ($script:offline) { throw 'Offline fixture' }
    if ($Url -like 'https://api.github.com/*') {
        return @([pscustomobject]@{draft=$false;tag_name='playtest-0.35.4-1';published_at='2026-11-02';assets=@([pscustomobject]@{name='civilization-update.json';browser_download_url='https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.4-1/civilization-update.json'})})
    }
    return [pscustomobject]$script:manifest
}
function Save-CivDownload([string]$Url,[string]$Path) {
    $script:downloads++
    if ($script:corrupt) { [IO.File]::WriteAllText($Path,'truncated') } else { Copy-Item -LiteralPath $candidate -Destination $Path }
}
function Assert-Original { if ((Get-FileHash $current).Hash -ne $original) { throw 'Installed mod changed unexpectedly' } }
$script:offline=$true; Invoke-CivUpdate $fixture; Assert-Original
$script:offline=$false; $script:corrupt=$true; Invoke-CivUpdate $fixture; Assert-Original
$script:corrupt=$false; $script:manifest.protocol='future'; Invoke-CivUpdate $fixture; Assert-Original
$script:manifest.protocol='3/0.35.0-dev'; $script:manifest.downloadUrl='https://example.com/evil.jar'; Invoke-CivUpdate $fixture; Assert-Original
$script:manifest.downloadUrl='https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.4-1/civilization-0.35.4-dev.jar'
$script:manifest.minimumNeoForgeVersion='21.1.999'; Invoke-CivUpdate $fixture; Assert-Original
$script:manifest.minimumNeoForgeVersion='21.1.265'
$held=[IO.File]::Open($current,'Open','Read','Read')
try { Invoke-CivUpdate $fixture } finally { $held.Dispose() }
Assert-Original
$held=[IO.File]::Open("$game/civilization-updater/update.lock",'Open','ReadWrite','None')
try {
    try { Invoke-CivUpdate $fixture; throw 'Concurrent update accepted' } catch { if ($_.Exception.Message -eq 'Concurrent update accepted') { throw } }
} finally { $held.Dispose() }
$script:running=$true
try { Invoke-CivUpdate $fixture; throw 'Running update accepted' } catch { if ($_.Exception.Message -eq 'Running update accepted') { throw } }
$script:running=$false
Copy-Item $current "$game/mods/civilization-duplicate.jar"
try { Invoke-CivUpdate $fixture; throw 'Duplicate accepted' } catch { if ($_.Exception.Message -eq 'Duplicate accepted') { throw } }
Remove-Item -LiteralPath "$game/mods/civilization-duplicate.jar"
Invoke-CivUpdate $fixture
if ((Get-CivJarVersion $current) -ne '0.35.4-dev' -or (Get-FileHash "$game/civilization-updater/previous.jar").Hash -ne $original) { throw 'Update/rollback verification failed' }
$before=$script:downloads; Invoke-CivUpdate $fixture
if ($script:downloads -ne $before) { throw 'Same version redownloaded' }
$script:manifest.version='0.35.3-dev'; Invoke-CivUpdate $fixture
if ($script:downloads -ne $before) { throw 'Downgrade attempted' }
Write-Host 'PASS: offline, corrupt download, protocol, loader, URL, locked JAR, concurrent updater, running game, duplicate, atomic update/rollback, same version, downgrade.'
