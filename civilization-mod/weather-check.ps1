# Two disposable clients, different simultaneous weather, no mouse capture.
$ErrorActionPreference = 'Stop'
$peerPath = Join-Path $PSScriptRoot 'runs/weather-peer'
$visualPath = Join-Path $PSScriptRoot 'runs/visual'
New-Item -ItemType Directory -Force $peerPath | Out-Null
foreach ($name in @('config', 'mods', 'shaderpacks', 'resourcepacks')) {
    New-Item -ItemType Directory -Force (Join-Path $peerPath $name) | Out-Null
    Get-ChildItem -LiteralPath (Join-Path $visualPath $name) | Copy-Item -Destination (Join-Path $peerPath $name) -Recurse -Force
}
Copy-Item -LiteralPath (Join-Path $visualPath 'options.txt') -Destination $peerPath -Force
Set-Content -LiteralPath (Join-Path $peerPath 'verified.txt') -Value 'pending'
$hostLog = Join-Path $peerPath 'host.log'
Set-Content -LiteralPath $hostLog -Value ''
$hostJob = Start-Job -ArgumentList $PSScriptRoot,$hostLog -ScriptBlock {
    param($root,$log)
    & (Join-Path $root 'dev.ps1') Visual -Scene weather *> $log
    if ($LASTEXITCODE -ne 0) { throw 'Weather host failed; see host.log' }
}
$previousJavaHome = $env:JAVA_HOME
try {
    $deadline = [DateTime]::UtcNow.AddMinutes(2)
    while (-not (Select-String -LiteralPath $hostLog -Pattern 'Started serving on 25569' -Quiet)) {
        if ($hostJob.State -in @('Completed','Failed') -or [DateTime]::UtcNow -gt $deadline) { throw 'Weather host did not open its test port; see runs/weather-peer/host.log' }
        Start-Sleep -Milliseconds 250
    }
    $config = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
    $env:JAVA_HOME = $config.javaHome
    Push-Location $PSScriptRoot
    try { & ./gradlew.bat runWeatherPeer --console=plain; if ($LASTEXITCODE -ne 0) { throw 'Weather peer failed' } }
    finally { Pop-Location }
    Receive-Job $hostJob -Wait -ErrorAction Stop
    if (-not (Select-String -LiteralPath $hostLog -Pattern 'REGIONAL WEATHER TWO CLIENTS VERIFIED' -Quiet)) { throw 'Host did not verify the peer' }
    Write-Host 'Two-client regional weather verified. Screenshots: runs/visual/screenshots and runs/weather-peer/screenshots.'
} finally {
    $env:JAVA_HOME = $previousJavaHome
    if ($hostJob.State -eq 'Running') { Stop-Job $hostJob }
    Remove-Job $hostJob
}
