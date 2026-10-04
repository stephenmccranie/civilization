# Development-only companion for the guarded gladiator visual scene.
$ErrorActionPreference = 'Stop'
$arenaPeerRoot = Join-Path $PSScriptRoot 'runs/arena-peer'
$arenaExitFile = Join-Path $arenaPeerRoot 'peer-exit.txt'
try {
    $arenaReadyFile = Join-Path $arenaPeerRoot 'ready.txt'
    $arenaTimer = [Diagnostics.Stopwatch]::StartNew()
    while (-not (Test-Path -LiteralPath $arenaReadyFile)) {
        if ($arenaTimer.Elapsed.TotalSeconds -gt 180) { throw 'Arena host never published its disposable test world.' }
        Start-Sleep -Milliseconds 500
    }
    $arenaConfig = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
    $env:JAVA_HOME = $arenaConfig.javaHome
    Push-Location $PSScriptRoot
    try {
        & (Join-Path $PSScriptRoot 'gradlew.bat') runArenaPeer --console=plain *> (Join-Path $PSScriptRoot '../.tools/arena-peer-auto.log')
        $arenaCode = $LASTEXITCODE
    } finally { Pop-Location }
    [IO.File]::WriteAllText($arenaExitFile, [string]$arenaCode)
    exit $arenaCode
} catch {
    [IO.File]::WriteAllText($arenaExitFile, '1')
    Write-Error $_
    exit 1
}
