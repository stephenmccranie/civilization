[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
$oldJava=$env:JAVA_HOME
Push-Location $PSScriptRoot
try {
 $env:JAVA_HOME=$config.javaHome
 $server=Join-Path $PSScriptRoot 'runs/boat-server'
 New-Item -ItemType Directory -Force $server | Out-Null
 if(-not(Test-Path -LiteralPath "$server/server.properties")){[IO.File]::WriteAllText("$server/server.properties","level-name=boat-check`nserver-ip=127.0.0.1`nserver-port=25568`nonline-mode=false`nview-distance=4`nsimulation-distance=4`nspawn-protection=0`ngamemode=creative`ndifficulty=peaceful`n")}
 # Reuse the development-server EULA acknowledgement documented by the existing laboratory setup.
 [IO.File]::WriteAllText("$server/eula.txt","eula=true`n")
 foreach($phase in @('seed','reload')){
  & ./gradlew.bat --console=plain "-PboatPhase=$phase" runBoatServer | Tee-Object -FilePath "$server/check-$phase.log"
  if($LASTEXITCODE -ne 0 -or -not(Select-String -LiteralPath "$server/check-$phase.log" -Pattern "BOAT_$($phase.ToUpper())_PASS" -Quiet)){throw "Boat persistence check failed: $phase"}
 }
}finally{$env:JAVA_HOME=$oldJava;Pop-Location}
