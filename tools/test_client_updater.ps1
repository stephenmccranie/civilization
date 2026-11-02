# Focused whole-pack transaction tests; no game launch, GUI, or live services.
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. "$root/modpack/overrides/civilization-updater/Start-Civilization.ps1"
# Model Invoke-RestMethod's real Windows PowerShell 5.1 non-enumerated REST array.
function Invoke-RestMethod {
 param($Uri,$Headers,$TimeoutSec)
 if ($Uri -eq 'fixture:array') { Write-Output -NoEnumerate @([pscustomobject]@{tag_name='first'},[pscustomobject]@{tag_name='second'}) }
 else { [pscustomobject]@{versionId='fixture'} }
}
$jsonArray=@(Get-CivJson 'fixture:array')
if ($jsonArray.Count -ne 2 -or $jsonArray[0].tag_name -ne 'first') { throw 'REST release arrays must enumerate into individual releases' }
if ((Get-CivJson 'fixture:object').versionId -ne 'fixture') { throw 'REST manifest objects must remain scalar' }
Remove-Item Function:Invoke-RestMethod
$fixture=Join-Path $root ('.tools/pack-update-tests/'+[guid]::NewGuid().ToString('N'))
$game=Join-Path $fixture 'minecraft'
$homeDir=Join-Path $game 'civilization-updater'
New-Item -ItemType Directory -Path "$game/mods","$game/config",$homeDir -Force | Out-Null
[IO.File]::WriteAllText("$fixture/mmc-pack.json",'{"components":[{"uid":"net.minecraft","version":"1.21.1"},{"uid":"net.neoforged","version":"21.1.265"}]}')
function New-Archive([string]$Name,[string]$Text) {
 $path=Join-Path $fixture $Name
 $stream=[IO.File]::Create($path);$zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
 $writer=[IO.StreamWriter]::new($zip.CreateEntry('META-INF/neoforge.mods.toml').Open());$writer.Write($Text);$writer.Dispose();$zip.Dispose();$stream.Dispose()
 return $path
}
function Entry([string]$Id,[string]$Path,[string]$Source) {
 [pscustomobject]@{id=$Id;path=$Path;fileSize=(Get-Item $Source).Length;hashes=@{sha1=(Get-FileHash $Source -Algorithm SHA1).Hash.ToLowerInvariant();sha512=(Get-CivHash $Source)};downloads=@('https://cdn.modrinth.com/data/fixture/'+(Split-Path $Source -Leaf))}
}
function Clone($Value) { $Value | ConvertTo-Json -Depth 15 | ConvertFrom-Json }
function Must-Fail([scriptblock]$Action) { $failed=$false;try { & $Action } catch { $failed=$true };if (!$failed) { throw 'Expected transaction rejection' } }
function Assert-File([string]$Path,[string]$Source) { if ((Get-CivHash $Path) -ne (Get-CivHash $Source)) { throw "Unexpected file: $Path" } }
$civ=New-Archive 'civic.jar' "[[mods]]`nmodId=`"civilization`"`nversion=`"0.35.4-dev`"`n"
$a1=New-Archive 'a1.jar' 'first';$a2=New-Archive 'a2.jar' 'second';$a3=New-Archive 'a3.jar' 'third'
$b1=New-Archive 'b1.jar' 'retired';$c1=New-Archive 'c1.jar' 'added';$c2=New-Archive 'c2.jar' 'changed'
$old=[pscustomobject]@{formatVersion=1;game='minecraft';channel='playtest';protocol='3/0.35.0-dev';versionId='playtest-0.35.4-1';dependencies=@{minecraft='1.21.1';neoforge='21.1.265'};configMigrations=@();files=@((Entry 'civilization' 'mods/civilization-0.35.4-dev.jar' $civ),(Entry 'alpha' 'mods/alpha-1.jar' $a1),(Entry 'retired' 'mods/retired.jar' $b1))}
foreach ($item in $old.files) { Copy-Item -LiteralPath (Join-Path $fixture ([uri]$item.downloads[0]).Segments[-1]) -Destination (Join-Path $game $item.path) }
[IO.File]::WriteAllText("$homeDir/managed-pack.json",($old | ConvertTo-Json -Depth 15))
$next=Clone $old;$next.versionId='playtest-0.35.4-2';$next.files=@($next.files[0],(Entry 'alpha' 'mods/alpha-2.jar' $a2),(Entry 'added' 'mods/added.jar' $c1))
$next.configMigrations=@([pscustomobject]@{path='config/DistantHorizons.toml';section='server';key='playerBandwidthLimit';from='1024';value='100'},[pscustomobject]@{path='options.txt';section='';key='fov';from='0.5';value='0.25'})
[IO.File]::WriteAllText("$game/config/DistantHorizons.toml","[server]`r`nplayerBandwidthLimit = 1024`r`ncustom = 17`r`n")
[IO.File]::WriteAllText("$game/options.txt","fov:0.75`r`n")
[IO.File]::WriteAllText("$game/mods/personal.jar",'personal mod')
$script:downloads=0;$script:corrupt=$false;$script:events=[Collections.Generic.List[string]]::new()
function Open-CivProgress([string]$Title,[string]$Detail,[long]$ExpectedBytes=0) { $script:events.Add("$Title|$Detail") }
function Assert-CivStopped([string]$Game) {}
function Save-CivDownload([string]$Url,[string]$Path) {
 $script:downloads++
 if ($script:corrupt) { [IO.File]::WriteAllText($Path,'truncated') } else { Copy-Item -LiteralPath (Join-Path $fixture ([uri]$Url).Segments[-1]) -Destination $Path }
}
Install-CivPack $game $old $next
Assert-File "$game/mods/alpha-2.jar" $a2;Assert-File "$game/mods/added.jar" $c1
if ((Test-Path "$game/mods/alpha-1.jar") -or (Test-Path "$game/mods/retired.jar")) { throw 'Old managed files retained' }
if ([IO.File]::ReadAllText("$game/mods/personal.jar") -ne 'personal mod' -or [IO.File]::ReadAllText("$game/options.txt") -ne "fov:0.75`r`n") { throw 'Personal files modified' }
if ([IO.File]::ReadAllText("$game/config/DistantHorizons.toml") -ne "[server]`r`nplayerBandwidthLimit = 100`r`ncustom = 17`r`n") { throw 'Config migration changed unrelated settings or newlines' }
$receipt=[IO.File]::ReadAllText("$homeDir/managed-pack.json")
$third=Clone $next;$third.versionId='playtest-0.35.4-3';$third.files=@($third.files[0],(Entry 'alpha' 'mods/alpha-2.jar' $a3),(Entry 'added' 'mods/added.jar' $c2));$third.configMigrations=@()
$script:corrupt=$true;Must-Fail { Install-CivPack $game $next $third };$script:corrupt=$false
Assert-File "$game/mods/alpha-2.jar" $a2
if ([IO.File]::ReadAllText("$homeDir/managed-pack.json") -ne $receipt) { throw 'Failed staging changed receipt' }
# Inject a failure after the first file has been replaced; require whole-pack rollback.
$script:applyCount=0;$script:failAt=2
function Set-CivTransactionFile([string]$Source,[string]$Target) {
 $script:applyCount++;if ($script:applyCount -eq $script:failAt) { throw 'Injected apply failure' }
 if (Test-Path -LiteralPath $Target) { [IO.File]::Replace($Source,$Target,[NullString]::Value) } else { [IO.File]::Move($Source,$Target) }
}
Must-Fail { Install-CivPack $game $next $third }
Assert-File "$game/mods/alpha-2.jar" $a2;Assert-File "$game/mods/added.jar" $c1
if ([IO.File]::ReadAllText("$homeDir/managed-pack.json") -ne $receipt) { throw 'Rollback changed managed receipt' }
$third.configMigrations=@([pscustomobject]@{path='config/DistantHorizons.toml';section='server';key='playerBandwidthLimit';from='100';value='50'})
$script:applyCount=0;$script:failAt=4
Must-Fail { Install-CivPack $game $next $third }
if ([IO.File]::ReadAllText("$game/config/DistantHorizons.toml") -notmatch 'playerBandwidthLimit = 100') { throw 'Config migration did not roll back' }
Assert-File "$game/mods/alpha-2.jar" $a2;Assert-File "$game/mods/added.jar" $c1
$third.configMigrations=@();$script:failAt=-1
# Model a process dying after replacement but before the commit marker.
$folder="$homeDir/transaction";New-Item -ItemType Directory -Path $folder | Out-Null
Copy-Item $a2 "$folder/0.old";Copy-Item $a3 "$game/mods/alpha-2.jar" -Force
$journal=@([pscustomobject]@{number=0;path='mods/alpha-2.jar';existed=$true;oldHash=(Get-CivHash $a2);newHash=(Get-CivHash $a3)})
[IO.File]::WriteAllText("$folder/journal.json",(ConvertTo-Json -InputObject $journal))
Restore-CivTransaction $game;Restore-CivTransaction $game
Assert-File "$game/mods/alpha-2.jar" $a2
$collision=Clone $third;$collision.files[2].path='mods/personal.jar'
Must-Fail { Install-CivPack $game $next $collision }
if ([IO.File]::ReadAllText("$game/mods/personal.jar") -ne 'personal mod') { throw 'Unmanaged collision overwritten' }
# Published pack-only revisions participate even when the mod version is unchanged.
$script:releaseTag='playtest-0.35.4-2';$script:offline=$false
function Get-CivJson([string]$Url) {
 if ($script:offline) { throw 'Offline fixture' }
 if ($Url -like 'https://api.github.com/*') { return @([pscustomobject]@{draft=$false;tag_name=$script:releaseTag;assets=@([pscustomobject]@{name='civilization-pack.json';browser_download_url="https://github.com/stephenmccranie/civilization/releases/download/$script:releaseTag/civilization-pack.json"})}) }
 return $third
}
$count=$script:downloads;Invoke-CivPackUpdate $fixture
$script:releaseTag='playtest-0.35.4-1';Invoke-CivPackUpdate $fixture
$script:offline=$true;Invoke-CivPackUpdate $fixture;$script:offline=$false
if ($script:downloads -ne $count) { throw 'Same/older/offline pack redownloaded files' }
$script:releaseTag='playtest-0.35.4-3';$third.dependencies.neoforge='99.0.0';Invoke-CivPackUpdate $fixture
if ($script:downloads -ne $count) { throw 'Incompatible loader downloaded files' }
$third.dependencies.neoforge='21.1.265';Invoke-CivPackUpdate $fixture
Assert-File "$game/mods/alpha-2.jar" $a3;Assert-File "$game/mods/added.jar" $c2
if ((Get-Content "$homeDir/managed-pack.json" -Raw | ConvertFrom-Json).versionId -ne 'playtest-0.35.4-3') { throw 'Published pack-only revision was not adopted' }
Remove-Item -LiteralPath "$game/mods/added.jar"
$script:releaseTag='playtest-0.35.4-2';Invoke-CivPackUpdate $fixture
Assert-File "$game/mods/added.jar" $c2
if ((Get-CivPackVersion 'playtest-0.35.4-3') -le (Get-CivPackVersion 'playtest-0.35.4-2')) { throw 'Pack-only revisions do not order correctly' }
if (!($script:events | Where-Object { $_ -match 'Updating Civilization pack\|File' })) { throw 'Pack update progress missing' }
Write-Host 'PASS: pack-only revisions, staged updates/additions/removals, config migration, personal mods/settings, bad download, partial-apply rollback, crash recovery, collisions, offline/no-downgrade, loader mismatch, visible progress stages.'
