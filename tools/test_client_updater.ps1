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
$old=[pscustomobject]@{formatVersion=1;game='minecraft';channel='playtest';protocol='3/0.35.0-dev';versionId='playtest-0.35.4-1';dependencies=@{minecraft='1.21.1';neoforge='21.1.265'};migrations=@();files=@((Entry 'civilization' 'mods/civilization-0.35.4-dev.jar' $civ),(Entry 'alpha' 'mods/alpha-1.jar' $a1),(Entry 'retired' 'mods/retired.jar' $b1))}
foreach ($item in $old.files) { Copy-Item -LiteralPath (Join-Path $fixture ([uri]$item.downloads[0]).Segments[-1]) -Destination (Join-Path $game $item.path) }
[IO.File]::WriteAllText("$homeDir/managed-pack.json",($old | ConvertTo-Json -Depth 15))
$next=Clone $old;$next.versionId='playtest-0.35.4-2';$next.files=@($next.files[0],(Entry 'alpha' 'mods/alpha-2.jar' $a2),(Entry 'added' 'mods/added.jar' $c1))
$next.migrations=@([pscustomobject]@{path='config/DistantHorizons.toml';section='server';key='playerBandwidthLimit';from='1024';value='100'},[pscustomobject]@{path='options.txt';section='';key='fov';from='0.5';value='0.25'})
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
$third=Clone $next;$third.versionId='playtest-0.35.4-3';$third.files=@($third.files[0],(Entry 'alpha' 'mods/alpha-2.jar' $a3),(Entry 'added' 'mods/added.jar' $c2));$third.migrations=@()
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
$third.migrations=@([pscustomobject]@{path='config/DistantHorizons.toml';section='server';key='playerBandwidthLimit';from='100';value='50'})
$script:applyCount=0;$script:failAt=4
Must-Fail { Install-CivPack $game $next $third }
if ([IO.File]::ReadAllText("$game/config/DistantHorizons.toml") -notmatch 'playerBandwidthLimit = 100') { throw 'Config migration did not roll back' }
Assert-File "$game/mods/alpha-2.jar" $a2;Assert-File "$game/mods/added.jar" $c1
$third.migrations=@();$script:failAt=-1
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
# The updater updates its own scripts as managed files, with the same verification and rollback.
function UpdaterEntry([string]$Tag) {
 $source=Join-Path $fixture 'Fixture-Updater.ps1'
 [pscustomobject]@{id='updater-fixture';path='civilization-updater/Fixture-Updater.ps1';fileSize=(Get-Item $source).Length;hashes=@{sha1=(Get-FileHash $source -Algorithm SHA1).Hash.ToLowerInvariant();sha512=(Get-CivHash $source)};downloads=@("https://github.com/stephenmccranie/civilization/releases/download/$Tag/Fixture-Updater.ps1")}
}
function Next-Pack($Previous,[int]$Revision) { $pack=Clone $Previous;$pack.versionId="playtest-0.35.4-$Revision";$pack.updaterFiles=@(UpdaterEntry $pack.versionId);return $pack }
$current=Get-Content "$homeDir/managed-pack.json" -Raw | ConvertFrom-Json
[IO.File]::WriteAllText("$fixture/Fixture-Updater.ps1",'# updater one')
$self1=Clone $current;$self1.versionId='playtest-0.35.4-4';$self1 | Add-Member -NotePropertyName updaterFiles -NotePropertyValue @(UpdaterEntry 'playtest-0.35.4-4')
Install-CivPack $game $current $self1
if ([IO.File]::ReadAllText("$homeDir/Fixture-Updater.ps1") -ne '# updater one') { throw 'Updater script was not installed' }
[IO.File]::WriteAllText("$fixture/Fixture-Updater.ps1",'# updater two');$self2=Next-Pack $self1 5
$script:applyCount=0;$script:failAt=2
Must-Fail { Install-CivPack $game $self1 $self2 }
if ([IO.File]::ReadAllText("$homeDir/Fixture-Updater.ps1") -ne '# updater one') { throw 'Updater script did not roll back' }
$script:failAt=-1;Install-CivPack $game $self1 $self2
if ([IO.File]::ReadAllText("$homeDir/Fixture-Updater.ps1") -ne '# updater two') { throw 'Updater script did not update itself' }
$dropped=Clone $self2;$dropped.versionId='playtest-0.35.4-9';$dropped.PSObject.Properties.Remove('updaterFiles')
Must-Fail { Install-CivPack $game $self2 $dropped }
if ([IO.File]::ReadAllText("$homeDir/Fixture-Updater.ps1") -ne '# updater two') { throw 'An update removed the updater' }
$foreign=Clone $self2;$foreign.updaterFiles[0].downloads=@('https://cdn.modrinth.com/data/fixture/Fixture-Updater.ps1')
Must-Fail { Assert-CivPack $foreign }
# Each migration applies once: a missing setting waits until the game writes it, and a player who restores the old value keeps it.
$chat=[pscustomobject]@{path='options.txt';section='';key='chatScale';from='1.0';value='0.75'}
$once1=Next-Pack $self2 6;$once1.migrations=@($chat)
$count=$script:downloads;Install-CivPack $game $self2 $once1
if ($script:downloads -ne $count) { throw 'Unchanged updater scripts were downloaded again' }
if ([IO.File]::ReadAllText("$game/options.txt") -ne "fov:0.75`r`n" -or (Get-CivAppliedMigrations $game) -contains (Get-CivMigrationId $chat)) { throw 'A missing setting must stay pending' }
[IO.File]::WriteAllText("$game/options.txt","fov:0.75`r`nchatScale:1.0`r`n")
$once2=Next-Pack $once1 7;Install-CivPack $game $once1 $once2
if ([IO.File]::ReadAllText("$game/options.txt") -ne "fov:0.75`r`nchatScale:0.75`r`n") { throw 'A pending migration did not apply once its setting existed' }
[IO.File]::WriteAllText("$game/options.txt","fov:0.75`r`nchatScale:1.0`r`n")
$once3=Next-Pack $once2 8;Install-CivPack $game $once2 $once3
if ([IO.File]::ReadAllText("$game/options.txt") -ne "fov:0.75`r`nchatScale:1.0`r`n") { throw 'An applied migration overrode the player''s later choice' }
$fresh=Join-Path $fixture 'fresh';New-Item -ItemType Directory -Path "$fresh/civilization-updater" | Out-Null
Save-CivInitialReceipt "$fresh/civilization-updater" $once3
if (@(Get-CivAppliedMigrations $fresh) -notcontains (Get-CivMigrationId $chat)) { throw 'A fresh import must count the migrations its settings already hold' }
# Due migrations from the installed receipt apply at launch without a new release, including key bindings.
$keys=[pscustomobject]@{path='options.txt';section='';key='key_key.journeymap.create_waypoint';from='key.keyboard.b';value='key.keyboard.v'}
$bound=Clone $once3;$bound.migrations=@($chat,$keys);[IO.File]::WriteAllText("$homeDir/managed-pack.json",($bound | ConvertTo-Json -Depth 15))
[IO.File]::WriteAllText("$game/options.txt","fov:0.75`r`nchatScale:1.0`r`nkey_key.journeymap.fullscreen_create_waypoint:key.keyboard.b`r`nkey_key.journeymap.create_waypoint:key.keyboard.b`r`n")
$count=$script:downloads;$script:offline=$true;Invoke-CivPackUpdate $fixture;$script:offline=$false
if ([IO.File]::ReadAllText("$game/options.txt") -ne "fov:0.75`r`nchatScale:1.0`r`nkey_key.journeymap.fullscreen_create_waypoint:key.keyboard.b`r`nkey_key.journeymap.create_waypoint:key.keyboard.v`r`n") { throw 'A due key-binding migration did not apply at launch' }
if ($script:downloads -ne $count -or @(Get-CivAppliedMigrations $game) -notcontains (Get-CivMigrationId $keys)) { throw 'Applying a migration at launch must record it without downloading' }
Write-Host 'PASS: pack-only revisions, staged updates/additions/removals, config migration, personal mods/settings, bad download, partial-apply rollback, crash recovery, collisions, offline/no-downgrade, loader mismatch, visible progress stages, updater self-update and rollback, migrations applied once and at launch.'
