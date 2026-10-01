# User-startable editor; development profile is separate from personal Blockbench settings.
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$editor = Join-Path $root '.tools/modeling/Blockbench_5.1.6_portable.exe'
if (-not (Test-Path -LiteralPath $editor)) { throw 'Run tools/modeling/setup.py first.' }
$profile = Join-Path $root '.tools/modeling/profile'
Start-Process -FilePath $editor -ArgumentList "--userData `"$profile`"" -WindowStyle Hidden
