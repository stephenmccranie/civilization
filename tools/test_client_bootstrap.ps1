# Offline first-launch integration checks with ZIP fixtures and real hash verification.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. "$root/modpack/overrides/civilization-updater/Start-Civilization.ps1"
$fixture=Join-Path $root ('.tools/bootstrap-tests/'+[guid]::NewGuid().ToString('N'))
$game=Join-Path $fixture 'minecraft'
$homeDir=Join-Path $game 'civilization-updater'
New-Item -ItemType Directory -Path "$game/mods",$homeDir -Force | Out-Null
function New-TestArchive([string]$Path,[string]$Content) {
    $stream=[IO.File]::Create($Path)
    $zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
    $writer=[IO.StreamWriter]::new($zip.CreateEntry('META-INF/neoforge.mods.toml').Open())
    $writer.Write($Content); $writer.Dispose(); $zip.Dispose(); $stream.Dispose()
}
$civ="$game/mods/civilization-0.35.4-dev.jar"
New-TestArchive $civ "[[mods]]`nmodId=`"civilization`"`nversion=`"0.35.4-dev`"`n"
$dependency="$fixture/dependency.jar"
New-TestArchive $dependency 'fixture dependency'
function New-Entry([string]$Path,[string]$File,[string]$Url) {
    @{path=$Path; fileSize=(Get-Item $File).Length; hashes=@{sha1=(Get-FileHash $File -Algorithm SHA1).Hash.ToLowerInvariant();sha512=(Get-FileHash $File -Algorithm SHA512).Hash.ToLowerInvariant()};downloads=@($Url)}
}
$manifest=@{formatVersion=1;game='minecraft';versionId='playtest-0.35.4-1';files=@(
    (New-Entry 'mods/civilization-0.35.4-dev.jar' $civ 'https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.4-1/civilization-0.35.4-dev.jar'),
    (New-Entry 'mods/dependency.jar' $dependency 'https://cdn.modrinth.com/data/fixture.jar')
)}
function Write-Manifest { [IO.File]::WriteAllText("$homeDir/client-files.json",($manifest | ConvertTo-Json -Depth 8)) }
$script:downloads=0; $script:corrupt=$true; $script:offline=$false
function Save-CivDownload([string]$Url,[string]$Path) {
    $script:downloads++
    if ($script:offline) { throw 'Offline fixture' }
    if ($script:corrupt) { [IO.File]::WriteAllText($Path,'truncated') } else { Copy-Item -LiteralPath $dependency -Destination $Path }
}
function Expect-Failure([scriptblock]$Action) {
    $failed=$false; try { & $Action } catch { $failed=$true }
    if (!$failed) { throw 'Expected bootstrap rejection' }
}
Write-Manifest
Expect-Failure { Initialize-CivClient $game }
if ((Test-Path "$game/mods/dependency.jar") -or (Test-Path "$homeDir/installed.txt") -or (Test-Path "$homeDir/bootstrap.partial")) { throw 'Partial install escaped verification' }
$script:corrupt=$false; $script:offline=$true
Expect-Failure { Initialize-CivClient $game }
$script:offline=$false
'personal setting' | Set-Content "$game/options.txt"
Initialize-CivClient $game
if (!(Test-CivPinnedFile "$game/mods/dependency.jar" ([pscustomobject]$manifest.files[1]))) { throw 'Installed dependency mismatch' }
$count=$script:downloads; $script:offline=$true
Initialize-CivClient $game
if ($script:downloads -ne $count) { throw 'Complete installation needed network again' }
# A future automatic mod update must survive the pinned first-launch baseline.
New-TestArchive $civ "[[mods]]`nmodId=`"civilization`"`nversion=`"0.35.5-dev`"`n"
Initialize-CivClient $game
if ((Get-CivJarVersion $civ) -ne '0.35.5-dev') { throw 'Bootstrap downgraded Civilization' }
$script:offline=$false
Remove-Item -LiteralPath "$game/mods/dependency.jar"
Initialize-CivClient $game
if (!(Test-Path "$game/mods/dependency.jar")) { throw 'Missing dependency not repaired' }
$manifest.files[1].path='mods/../../escape.jar'; Write-Manifest
Expect-Failure { Initialize-CivClient $game }
$manifest.files[1].path='mods/dependency.jar'; $manifest.files[1].downloads=@('https://example.com/evil.jar'); Write-Manifest
Expect-Failure { Initialize-CivClient $game }
if ((Get-Content "$game/options.txt" -Raw).Trim() -ne 'personal setting') { throw 'Personal config changed' }
Write-Host 'PASS: first install, failed/corrupt download, retry, offline repeat launch, no downgrade, missing-file repair, traversal/host rejection, personal settings preservation.'
