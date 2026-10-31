[CmdletBinding()]
param([ValidateSet('Seed','Server','Check','Reload','Visual','Physics','Bench','Batching')][string]$Task='Seed',
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunName='physics')
$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
$previousJava=$env:JAVA_HOME
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME=$config.javaHome
    & ./sable.ps1 Prepare
    if(-not $?){throw 'Dependency preparation failed'}
    $labRoot=Join-Path $PSScriptRoot 'runs/thermal-lab'
    $directory=if($Task -in @('Check','Reload')){'runs/thermal-lab/check'}else{'runs/thermal-lab/server'}
    if($Task -in @('Physics','Bench','Batching')){$directory="runs/thermal-lab/$RunName"}
    if($Task -in @('Check','Reload') -and $RunName -ne 'physics'){$directory="runs/thermal-lab/$RunName"}
    $serverPath=Join-Path $PSScriptRoot $directory
    New-Item -ItemType Directory -Force $serverPath | Out-Null
    $properties=Join-Path $serverPath 'server.properties'
    if(-not(Test-Path -LiteralPath $properties)){
        [IO.File]::WriteAllText($properties,"level-name=thermal-laboratory`nlevel-seed=54321987`nlevel-type=minecraft:flat`ngenerator-settings={`"biome`":`"minecraft:plains`",`"layers`":[{`"block`":`"minecraft:bedrock`",`"height`":1},{`"block`":`"minecraft:stone`",`"height`":2}]} `nserver-ip=127.0.0.1`nserver-port=25573`nonline-mode=true`nview-distance=7`nsimulation-distance=7`nspawn-protection=0`ngamemode=creative`nforce-gamemode=true`ndifficulty=peaceful`n")
    }
    # Reuse the owner's existing local development-server EULA acknowledgement.
    $eula=Join-Path $PSScriptRoot 'runs/server/eula.txt'
    if(-not(Test-Path -LiteralPath $eula)){throw "Existing development EULA acknowledgement required: $eula"}
    Copy-Item -LiteralPath $eula -Destination (Join-Path $serverPath 'eula.txt') -Force
    if($Task -eq 'Visual'){
        $world=Join-Path $labRoot 'server/thermal-laboratory'
        if(-not(Test-Path -LiteralPath (Join-Path $world 'thermal-lab-v1.txt'))){throw 'Run thermal-lab.ps1 Seed first'}
        $client=Join-Path $labRoot 'client'
        New-Item -ItemType Directory -Force (Join-Path $client 'saves'),(Join-Path $client 'config') | Out-Null
        $copy=Join-Path $client 'saves/thermal-laboratory'
        if(-not(Test-Path -LiteralPath $copy)){Copy-Item -LiteralPath $world -Destination $copy -Recurse}
        [IO.File]::WriteAllText((Join-Path $client 'config/fml.toml'),"earlyWindowControl=false`n")
        [IO.File]::WriteAllText((Join-Path $client 'options.txt'),"fullscreen:false`noverrideWidth:1920`noverrideHeight:1080`npauseOnLostFocus:false`nrenderDistance:9`nsimulationDistance:7`nguiScale:3`n")
        & ./gradlew.bat --console=plain runThermalLabClient | Tee-Object -FilePath (Join-Path $labRoot 'visual.log')
        if($LASTEXITCODE -ne 0 -or -not(Select-String -LiteralPath (Join-Path $labRoot 'visual.log') -Pattern 'THERMAL_LAB_VISUAL_PASS' -Quiet)){throw 'Laboratory visual check failed'}
    }else{
        $phase=if($Task -eq 'Server'){'manual'}else{$Task.ToLowerInvariant()}
        $log=Join-Path $labRoot "$phase.log"
        & ./gradlew.bat --console=plain "-PthermalLabDirectory=$directory" "-PthermalLabPhase=$phase" runThermalLabServer | Tee-Object -FilePath $log
        if($LASTEXITCODE -ne 0){throw "Thermal laboratory failed: $phase"}
        $pass=if($phase -eq 'physics'){'THERMAL_PHYSICS_PASS'}elseif($phase -eq 'bench'){'THERMAL_BENCH_PASS'}elseif($phase -eq 'batching'){'THERMAL_BATCHING_PASS'}else{"THERMAL_LAB_$($phase.ToUpper())_PASS"}
        if($phase -ne 'manual' -and -not(Select-String -LiteralPath $log -Pattern $pass -Quiet)){throw "Missing explicit $phase pass"}
    }
}finally{$env:JAVA_HOME=$previousJava;Pop-Location}
