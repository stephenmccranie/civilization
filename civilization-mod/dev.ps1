[CmdletBinding()]
param(
    [ValidateSet('Build', 'Deploy', 'Client', 'Server', 'GameTest')]
    [string]$Task = 'Build'
)

$ErrorActionPreference = 'Stop'
$configFile = Join-Path $PSScriptRoot 'dev.local.json'
if (-not (Test-Path -LiteralPath $configFile)) {
    throw 'Copy dev.local.example.json to dev.local.json and set your JDK and Prism instance paths.'
}
$devConfig = Get-Content -LiteralPath $configFile -Raw | ConvertFrom-Json
$compiler = Join-Path $devConfig.javaHome 'bin/javac.exe'
if (-not (Test-Path -LiteralPath $compiler)) {
    throw "JDK compiler not found: $compiler"
}
$compilerVersion = & $compiler -version 2>&1
if ($LASTEXITCODE -ne 0 -or "$compilerVersion" -notmatch '^javac 21(?:\.|$)') {
    throw "This project requires JDK 21; found: $compilerVersion"
}

$previousJavaHome = $env:JAVA_HOME
Push-Location $PSScriptRoot
try {
    $env:JAVA_HOME = $devConfig.javaHome
    $gradleTask = switch ($Task) {
        'Client' { 'runClient' }
        'Server' { 'runServer' }
        'GameTest' { 'runGameTestServer' }
        default { 'build' }
    }

    if ($Task -eq 'Server') {
        $serverDirectory = Join-Path $PSScriptRoot 'runs/server'
        New-Item -ItemType Directory -Path $serverDirectory -Force | Out-Null
        $serverProperties = Join-Path $serverDirectory 'server.properties'
        if (-not (Test-Path -LiteralPath $serverProperties)) {
            Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'server.local.example.properties') -Destination $serverProperties
        }
        Write-Host 'Dedicated development server: localhost:25566; saves in runs/server. Type stop to shut down.'
    }

    if ($Task -eq 'Deploy') {
        $instancePath = (Resolve-Path -LiteralPath $devConfig.prismInstance).Path
        $pack = Get-Content -LiteralPath (Join-Path $instancePath 'mmc-pack.json') -Raw | ConvertFrom-Json
        $mc = $pack.components | Where-Object uid -eq 'net.minecraft'
        $neo = $pack.components | Where-Object uid -eq 'net.neoforged'
        $properties = ConvertFrom-StringData (Get-Content -LiteralPath 'gradle.properties' -Raw)
        if ($mc.version -ne $properties.minecraft_version -or $neo.version -ne $properties.neo_version) {
            throw 'Prism Minecraft/NeoForge versions do not match gradle.properties.'
        }
        $gamePath = Join-Path $instancePath 'minecraft'
        if (-not (Test-Path -LiteralPath $gamePath)) {
            throw "Launch the Prism instance once first: $gamePath"
        }
        $runningGame = Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" |
            Where-Object {
                $_.CommandLine -and
                $_.CommandLine.Replace('/', '\').IndexOf($instancePath, [StringComparison]::OrdinalIgnoreCase) -ge 0
            }
        if ($runningGame) {
            throw 'Close the Civilization Dev game before deploying. Prism itself may stay open.'
        }
    }

    & (Join-Path $PSScriptRoot 'gradlew.bat') $gradleTask --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle $gradleTask failed ($LASTEXITCODE)." }

    if ($Task -eq 'Deploy') {
        $jarName = "$($properties.mod_id)-$($properties.mod_version).jar"
        $artifact = Join-Path $PSScriptRoot "build/libs/$jarName"
        if (-not (Test-Path -LiteralPath $artifact)) { throw "Missing artifact: $artifact" }
        $modsPath = Join-Path $gamePath 'mods'
        New-Item -ItemType Directory -Path $modsPath -Force | Out-Null
        # Only replace our own artifacts; archive previous builds outside the loaded mods folder.
        $existing = @(Get-ChildItem -LiteralPath $modsPath -Filter "$($properties.mod_id)-*.jar" -File)
        if ($existing.Count -gt 0) {
            $backupPath = Join-Path $instancePath ('mod-backups/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
            New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
            foreach ($oldJar in $existing) {
                Move-Item -LiteralPath $oldJar.FullName -Destination $backupPath
            }
        }
        $destination = Join-Path $modsPath $jarName
        Copy-Item -LiteralPath $artifact -Destination $destination
        if ((Get-FileHash -LiteralPath $artifact).Hash -ne (Get-FileHash -LiteralPath $destination).Hash) {
            throw 'Deployed JAR checksum does not match the build.'
        }
        Write-Host "Installed $destination"
        Write-Host 'Launch Civilization Dev, open a world, and run /civilization status.'
    }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
