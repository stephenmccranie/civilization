[CmdletBinding()]
param(
    [ValidateSet('Build', 'Deploy', 'Client', 'Server', 'GameTest', 'Visual', 'Verify')]
    [string]$Task = 'Build',
    [ValidateSet('Quick', 'Gameplay', 'Visual', 'Full')]
    [string]$Scope = 'Quick',
    [ValidateSet('machines', 'machine-lighting', 'thermal-art', 'modular', 'material-sync', 'guide', 'textures', 'civic', 'industry', 'pipes', 'jei', 'bulk', 'storage', 'deposits', 'boat', 'airship', 'weather', 'workshops', 'inventory', 'chests', 'crafting', 'engine', 'models', 'canisters','cloth','sulfur','parts','supplies','foods','manufactured','thermal','road','uranium','derrick-guide','kitchen','oven','paterson')]
    [string]$Scene = 'machines',
    [switch]$FullVisual
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
    & ./sable.ps1 Prepare
    $gradleTasks = @(switch ($Task) {
        'Client' { 'runClient' }
        'Server' { 'runServer' }
        'GameTest' { 'runGameTestServer' }
        'Visual' { 'runVisualClient' }
        'Verify' {
            'build'
            if ($Scope -in @('Gameplay', 'Full')) { 'runGameTestServer' }
            if ($Scope -in @('Visual', 'Full')) { 'runVisualClient' }
        }
        default { 'build' }
    })
    $needsVisual = $gradleTasks -contains 'runVisualClient'
    if ($Task -eq 'Deploy') { $gradleTasks += 'prepareRuntimeMods' }
    $gradleArguments = @('--console=plain')
    if ($needsVisual) {
        $gradleArguments += "-PvisualScene=$Scene"
        $gradleArguments += "-PvisualFull=$($FullVisual.IsPresent.ToString().ToLowerInvariant())"
    }

    if ($needsVisual) {
        $visualPath = Join-Path $PSScriptRoot 'runs/visual'
        foreach ($subdir in @('config', 'mods', 'shaderpacks', 'resourcepacks', 'saves')) {
            New-Item -ItemType Directory -Force (Join-Path $visualPath $subdir) | Out-Null
        }
        $visualWorld = Join-Path $visualPath 'saves/preview-compatibility'
        if (-not (Test-Path -LiteralPath $visualWorld)) {
            $sourceWorld = Join-Path $PSScriptRoot 'run/saves/preview-compatibility'
            if (-not (Test-Path -LiteralPath $sourceWorld)) { $sourceWorld = Join-Path $PSScriptRoot 'runs/server/development-world' }
            if (-not (Test-Path -LiteralPath $sourceWorld)) { throw 'Visual tests need a disposable development world first.' }
            Copy-Item -LiteralPath $sourceWorld -Destination $visualWorld -Recurse
        }
        # Copy development-only graphics inputs; never edit the user's Prism instance.
        foreach ($subdir in @('mods', 'shaderpacks', 'resourcepacks')) {
            $sourceDir = Join-Path $PSScriptRoot "run/$subdir"
            if (Test-Path -LiteralPath $sourceDir) {
                Get-ChildItem -LiteralPath $sourceDir -File | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $visualPath $subdir) }
            }
        }
        $optionsSource = Join-Path $PSScriptRoot 'run/options.txt'
        if (Test-Path -LiteralPath $optionsSource) {
            $options = @(Get-Content -LiteralPath $optionsSource | Where-Object { $_ -notmatch '^(fullscreen|overrideWidth|overrideHeight|pauseOnLostFocus):' })
            $options += @('fullscreen:false', 'overrideWidth:1920', 'overrideHeight:1080', 'pauseOnLostFocus:false')
            [IO.File]::WriteAllLines((Join-Path $visualPath 'options.txt'), $options)
        }
        $irisSource = Join-Path $PSScriptRoot 'run/config/iris.properties'
        if (Test-Path -LiteralPath $irisSource) { Copy-Item -LiteralPath $irisSource -Destination (Join-Path $visualPath 'config/iris.properties') }
        # The disposable client has no one to dismiss a Distant Horizons update prompt.
        # Its default auto-updater was blocking the title screen and looked like a Photon stall.
        [IO.File]::WriteAllText((Join-Path $visualPath 'config/DistantHorizons.toml'), "[client.advanced.autoUpdater]`nenableAutoUpdater = false`n")
        # Disable NeoForge's separate early splash window before any native window is created.
        [IO.File]::WriteAllText((Join-Path $visualPath 'config/fml.toml'), "earlyWindowControl = false`nearlyWindowWidth = 1920`nearlyWindowHeight = 1080`n")
        Write-Host "Hidden 1920x1080 visual check: $Scene (extended tour: $($FullVisual.IsPresent)). Mouse capture disabled. Output: runs/visual/screenshots."
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

    $verificationTimer = [Diagnostics.Stopwatch]::StartNew()
    & (Join-Path $PSScriptRoot 'gradlew.bat') @gradleTasks @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle $($gradleTasks -join ', ') failed ($LASTEXITCODE)." }
    Write-Host ("Completed {0} in {1:N1}s." -f ($gradleTasks -join ', '), $verificationTimer.Elapsed.TotalSeconds)

    if ($Task -eq 'Deploy') {
        $jarName = "$($properties.mod_id)-$($properties.mod_version).jar"
        $artifact = Join-Path $PSScriptRoot "build/libs/$jarName"
        if (-not (Test-Path -LiteralPath $artifact)) { throw "Missing artifact: $artifact" }
        $modsPath = Join-Path $gamePath 'mods'
        New-Item -ItemType Directory -Path $modsPath -Force | Out-Null
        $geckoName = "geckolib-neoforge-$($properties.minecraft_version)-$($properties.geckolib_version).jar"
        $geckoSource = Join-Path $PSScriptRoot "build/runtimeMods/$geckoName"
        if (-not (Test-Path -LiteralPath $geckoSource)) { throw "Missing resolved GeckoLib artifact: $geckoSource" }
        $otherGecko = @(Get-ChildItem -LiteralPath $modsPath -Filter 'geckolib-*.jar' -File | Where-Object Name -ne $geckoName)
        if ($otherGecko.Count) { throw 'A different GeckoLib version is installed; resolve the duplicate before deployment.' }
        $geckoDestination = Join-Path $modsPath $geckoName
        Copy-Item -LiteralPath $geckoSource -Destination $geckoDestination -Force
        if ((Get-FileHash -LiteralPath $geckoSource).Hash -ne (Get-FileHash -LiteralPath $geckoDestination).Hash) { throw 'GeckoLib deployment hash mismatch' }
        $sableJar = Join-Path $PSScriptRoot ".dev-libs/sable-neoforge-1.21.1-2.0.5.jar"
        $sableDestination = Join-Path $modsPath "sable-neoforge-1.21.1-2.0.5.jar"
        Copy-Item -LiteralPath $sableJar -Destination $sableDestination -Force
        if ((Get-FileHash -LiteralPath $sableJar -Algorithm SHA512).Hash -ne (Get-FileHash -LiteralPath $sableDestination -Algorithm SHA512).Hash) { throw "Sable deployment hash mismatch" }
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
