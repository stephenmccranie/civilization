[CmdletBinding()]
param(
    [switch]$Prepare,
    [ValidateSet('stream','airship')][string]$Mode='stream',
    [int[]]$Speeds=@(200,1000,5000),
    [ValidateRange(1,10)][int]$Repeats=2,
    [ValidateRange(1,120)][int]$Seconds=20,
    [ValidateRange(0,30)][int]$Warmup=5,
    [ValidateSet('directional','spiral')][string]$Order='directional',
    [ValidatePattern('^[A-Za-z0-9_-]+$')][string]$Label='baseline',
    [string]$Python='python'
)
$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
$oldJava=$env:JAVA_HOME
$benchRoot=Join-Path $PSScriptRoot 'runs/flight-benchmark'
$toolRoot=Join-Path (Split-Path $PSScriptRoot -Parent) 'tools/flight-benchmark'
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME=$config.javaHome
    if($Prepare){
        $seedRoot=Join-Path $benchRoot 'seed'
        New-Item -ItemType Directory -Force $seedRoot | Out-Null
        if(-not(Test-Path -LiteralPath (Join-Path $seedRoot 'benchmark-template/flat-template.nbt'))){
            $settings='{"biome":"minecraft:plains","features":false,"lakes":false,"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":124},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"structure_overrides":[]}'
            [IO.File]::WriteAllText((Join-Path $seedRoot 'eula.txt'),"eula=true`n")
            [IO.File]::WriteAllText((Join-Path $seedRoot 'server.properties'),"level-name=benchmark-template`nlevel-seed=192506`nlevel-type=minecraft:flat`ngenerator-settings=$settings`ngenerate-structures=false`nserver-ip=127.0.0.1`nserver-port=25579`nonline-mode=false`ngamemode=creative`ndifficulty=peaceful`nview-distance=4`nsimulation-distance=4`nspawn-protection=0`nmax-tick-time=180000`n")
            & ./gradlew.bat --console=plain runFlightBenchmarkSeed
            if($LASTEXITCODE -ne 0){throw 'Benchmark template creation failed'}
        }
        & $Python (Join-Path $toolRoot 'corridor.py')
        if($LASTEXITCODE -ne 0){throw 'Corridor preparation failed'}
        & $Python (Join-Path $toolRoot 'lod_cache.py')
        if($LASTEXITCODE -ne 0){throw 'LOD preparation failed'}
    }else{
        if(-not(Test-Path -LiteralPath (Join-Path $benchRoot 'baseline/CORRIDOR_READY.json'))){throw 'Run flight-benchmark.ps1 -Prepare first'}
        $direction=if($Order -eq 'directional'){'on'}else{'off'}
        & $Python (Join-Path $toolRoot 'run.py') --mode $Mode --speeds ($Speeds -join ',') --repeats $Repeats --seconds $Seconds --warmup $Warmup --directional $direction --label $Label
        if($LASTEXITCODE -ne 0){throw 'Benchmark failed; inspect runs/flight-benchmark/results logs'}
    }
}finally{$env:JAVA_HOME=$oldJava;Pop-Location}
