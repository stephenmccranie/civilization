[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
$oldJava=$env:JAVA_HOME
Push-Location $PSScriptRoot
try {
 $env:JAVA_HOME=$config.javaHome
 $server=Join-Path $PSScriptRoot 'runs/airship-server'
 New-Item -ItemType Directory -Force $server | Out-Null
 if(-not(Test-Path -LiteralPath "$server/server.properties")){[IO.File]::WriteAllText("$server/server.properties","level-name=airship-check`nserver-ip=127.0.0.1`nserver-port=25570`nonline-mode=false`nview-distance=4`nsimulation-distance=4`nspawn-protection=0`ngamemode=creative`ndifficulty=peaceful`n")}
 # Same development-only EULA acknowledgement as the boat/laboratory harness.
 [IO.File]::WriteAllText("$server/eula.txt","eula=true`n")
 foreach($phase in @('seed','reload')){
  & ./gradlew.bat --console=plain "-PairshipPhase=$phase" runAirshipServer | Tee-Object -FilePath "$server/check-$phase.log"
  if($LASTEXITCODE -ne 0 -or -not(Select-String -LiteralPath "$server/check-$phase.log" -Pattern "AIRSHIP_$($phase.ToUpper())_PASS" -Quiet)){throw "Airship persistence check failed: $phase"}
 }
}finally{$env:JAVA_HOME=$oldJava;Pop-Location}
