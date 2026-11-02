param([string]$InstancePath)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/Update-Civilization.ps1" -InstancePath $InstancePath
if (!$InstancePath) { $InstancePath = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent }
if (!(Test-Path -LiteralPath (Join-Path $InstancePath 'mmc-pack.json'))) { throw 'Run inside a Prism instance or supply -InstancePath' }
$InstancePath = (Resolve-Path -LiteralPath $InstancePath).Path
$game = Get-CivGameDirectory $InstancePath
Assert-CivStopped $game
$cfg = Join-Path $InstancePath 'instance.cfg'
$lines = [Collections.Generic.List[string]]::new()
$lines.AddRange([string[]][IO.File]::ReadAllLines($cfg))
$start = $lines.IndexOf('[General]')
if ($start -lt 0) { throw 'Missing General settings section' }
$end = $start + 1
while ($end -lt $lines.Count -and $lines[$end] -notmatch '^\[') { $end++ }
$settings = @{}
for ($i=$start+1; $i -lt $end; $i++) { if ($lines[$i] -match '^([^=]+)=(.*)$') { $settings[$Matches[1]]=$Matches[2] } }
if ($settings['ConfigVersion'] -ne '1.3') { throw 'Open and save this instance in current Prism before installing' }
if ($settings['PreLaunchCommand'] -and $settings['PreLaunchCommand'] -notmatch 'civilization-updater/Update-Civilization.ps1') { throw 'Existing pre-launch command: integrate it manually before installing' }
# Instance overrides must not silently shadow launcher-wide custom commands.
$globalCfg = Join-Path (Split-Path (Split-Path $InstancePath -Parent) -Parent) 'prismlauncher.cfg'
if ($settings['OverrideCommands'] -ne 'true' -and (Test-Path -LiteralPath $globalCfg)) {
    if ([IO.File]::ReadAllText($globalCfg) -match '(?m)^(PreLoadCommand|PreLaunchCommand|PostExitCommand|WrapperCommand)=(?!"?\s*$).+') { throw 'Launcher-wide custom commands exist: integrate the updater manually' }
}
$target = Join-Path $game 'civilization-updater'
New-Item -ItemType Directory -Path $target -Force | Out-Null
foreach ($name in @('Update-Civilization.ps1','Install-CivilizationUpdater.ps1','Install-Client-Updates.cmd')) {
    $source = Join-Path $PSScriptRoot $name
    $dest = Join-Path $target $name
    if ([IO.Path]::GetFullPath($source) -ne [IO.Path]::GetFullPath($dest)) { Copy-Item -LiteralPath $source -Destination $dest -Force }
}
$values = @{
    OverrideCommands='true'
    PreLaunchCommand='"powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File \"$INST_MC_DIR/civilization-updater/Update-Civilization.ps1\" -InstancePath \"$INST_DIR\""'
}
foreach ($key in $values.Keys) {
    $found=$false
    for ($i=$start+1; $i -lt $end; $i++) { if ($lines[$i].StartsWith($key+'=')) { $lines[$i]=$key+'='+$values[$key]; $found=$true } }
    if (!$found) { $lines.Insert($end, $key+'='+$values[$key]); $end++ }
}
[IO.File]::WriteAllLines($cfg, $lines, [Text.UTF8Encoding]::new($false))
Write-Host 'Installed. Prism will check approved Civilization releases before launch.'
