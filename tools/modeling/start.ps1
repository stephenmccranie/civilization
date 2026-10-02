# User-startable editor; development profile is separate from personal Blockbench settings.
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$editor = Join-Path $root '.tools/modeling/Blockbench_5.1.6_portable.exe'
if (-not (Test-Path -LiteralPath $editor)) { throw 'Run tools/modeling/setup.py first.' }
$profile = Join-Path $root '.tools/modeling/profile'
$startupLog = Join-Path $root '.tools/modeling/startup.log'
function Test-ModelServer {
    $session = $null
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:3000/bb-mcp' -Method Post -ContentType 'application/json' -Headers @{ Accept = 'application/json, text/event-stream'; 'MCP-Protocol-Version' = '2024-11-05' } -Body '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"civilization-startup","version":"1"}}}' -TimeoutSec 3
        $session = $response.Headers['Mcp-Session-Id']
        $body = [string]$response.Content
        if ($response.Headers['Content-Type'] -match 'text/event-stream') {
            $body = (($body -split '\r?\n') | Where-Object { $_ -match '^data:' } | ForEach-Object { $_ -replace '^data:\s*', '' }) -join "`n"
        }
        $message = $body | ConvertFrom-Json -ErrorAction Stop
        return ($message.id -eq 1 -and $null -eq $message.error -and [bool]$message.result.protocolVersion)
    } catch { return $false }
    finally {
        if ($session) {
            try { Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:3000/bb-mcp' -Method Delete -Headers @{ 'Mcp-Session-Id' = [string]$session } -TimeoutSec 3 | Out-Null }
            catch { } # Session cleanup must not turn a successful handshake into a startup failure.
        }
    }
}
if (Test-ModelServer) { Write-Host 'Blockbench modeling server is already ready.'; return }
$running = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -like '*Blockbench*.exe' -and $_.CommandLine -and $_.CommandLine.IndexOf($profile, [StringComparison]::OrdinalIgnoreCase) -ge 0
}
if (-not $running) {
    Start-Process -FilePath $editor -ArgumentList @('--userData', "`"$profile`"", '--enable-logging=file', "--log-file=`"$startupLog`"") -WindowStyle Hidden
} else {
    Write-Host 'Waiting for the existing project editor; open projects will be preserved.'
}
$deadline = [DateTime]::UtcNow.AddSeconds(45)
do {
    if (Test-ModelServer) { Write-Host 'Blockbench modeling server is ready.'; return }
    Start-Sleep -Milliseconds 500
} while ([DateTime]::UtcNow -lt $deadline)
throw "Blockbench started but its modeling server is not ready. Inspect $startupLog; preserve open projects before restarting the editor."
