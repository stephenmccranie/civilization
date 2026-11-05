[CmdletBinding()]
param(
    [ValidateSet('Build', 'GameTest', 'Visual', 'Perf', 'Fixture', 'Deploy', 'Client', 'Server')]
    [string]$Task = 'Build',
    [string]$Scene = 'machines',
    [switch]$Full,
    [ValidateSet('boat', 'airship', 'thermal')]
    [string]$Name = 'boat',
    [string]$Phase
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$configFile = Join-Path $root 'dev.local.json'
if (-not (Test-Path -LiteralPath $configFile)) {
    throw 'Copy dev.local.example.json to dev.local.json and set your JDK and Prism instance paths.'
}
$devConfig = Get-Content -LiteralPath $configFile -Raw | ConvertFrom-Json
$compilerVersion = & (Join-Path $devConfig.javaHome 'bin/javac.exe') -version 2>&1
if ("$compilerVersion" -notmatch '^javac 21(?:\.|$)') { throw "This project requires JDK 21; found: $compilerVersion" }
$properties = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $root 'gradle.properties') -Raw)
$sableJarName = "sable-neoforge-$($properties.minecraft_version)-$($properties.sable_version).jar"
$sableUrl = "https://cdn.modrinth.com/data/T9PomCSv/versions/U678xqle/$sableJarName"
$sableSha512 = 'bf3d8c87bcc5efb99afffd50305fc978086ed48a63e106816e3a8a3901f8052f4480ea527dbd7d4003f7775ed4d020529098034f4e307aefac9cc42df2b4c19a'
# Phases each fixture runs against one fresh world; -Phase runs a single phase instead (thermal: physics, manual).
$fixturePhases = @{ boat = 'seed', 'reload'; airship = 'seed', 'reload'; thermal = 'check', 'reload' }
$peerScenes = 'gladiator', 'weather'

function Invoke-Gradle([string[]]$Arguments, [string]$Log) {
    # Gradle and javac report warnings on stderr; only the exit code decides success.
    $ErrorActionPreference = 'Continue'
    if ($Log) { & (Join-Path $root 'gradlew.bat') @Arguments --console=plain 2>&1 | Tee-Object -FilePath $Log }
    else { & (Join-Path $root 'gradlew.bat') @Arguments --console=plain }
    if ($LASTEXITCODE -ne 0) { throw "Gradle $($Arguments -join ' ') failed ($LASTEXITCODE)." }
}

# Run directories are disposable: every run starts from a clean copy.
function Reset-Directory([string]$path) {
    if (Test-Path -LiteralPath $path) { cmd /c rd /s /q $path }
    New-Item -ItemType Directory -Force $path | Out-Null
}

# The pinned Sable release and the libraries it bundles, downloaded once into .dev-libs.
function Install-Sable {
    $libs = Join-Path $root '.dev-libs'
    $jar = Join-Path $libs $sableJarName
    if (Test-Path -LiteralPath $jar) { return }
    New-Item -ItemType Directory -Force $libs | Out-Null
    $download = "$jar.download"
    Invoke-WebRequest $sableUrl -OutFile $download
    if ((Get-FileHash -LiteralPath $download -Algorithm SHA512).Hash -ne $sableSha512) {
        Remove-Item -LiteralPath $download
        throw 'Sable download does not match the pinned SHA-512.'
    }
    Move-Item -LiteralPath $download -Destination $jar
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($entry in $zip.Entries | Where-Object FullName -like 'META-INF/jarjar/*.jar') {
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $libs $entry.Name), $true)
        }
    }
    finally { $zip.Dispose() }
}

# LambDynamicLights as the player pack pins it, downloaded once into .dev-libs; test clients and Deploy install it.
function Install-DynamicLights {
    $item = (Get-Content -LiteralPath (Join-Path $root '../modpack/client-files.json') -Raw | ConvertFrom-Json).files | Where-Object id -eq 'modrinth-yBW8D80W'
    $jar = Join-Path $root ".dev-libs/$(Split-Path $item.path -Leaf)"
    if (-not (Test-Path -LiteralPath $jar)) {
        $download = "$jar.download"
        Invoke-WebRequest $item.downloads[0] -OutFile $download
        if ((Get-FileHash -LiteralPath $download -Algorithm SHA512).Hash -ne $item.hashes.sha512) {
            Remove-Item -LiteralPath $download
            throw 'LambDynamicLights download does not match the pack manifest SHA-512.'
        }
        Move-Item -LiteralPath $download -Destination $jar
    }
    $jar
}

# A hidden test client built from the development client in run/; never from the owner's Prism instance.
function New-TestClient([string]$path) {
    Reset-Directory $path
    foreach ($dir in 'mods', 'shaderpacks', 'resourcepacks', 'config') {
        New-Item -ItemType Directory -Force (Join-Path $path $dir) | Out-Null
        $source = Join-Path $root "run/$dir"
        if ($dir -ne 'config' -and (Test-Path -LiteralPath $source)) {
            Get-ChildItem -LiteralPath $source -File | Copy-Item -Destination (Join-Path $path $dir)
        }
    }
    Copy-Item -LiteralPath $dynamicLights -Destination (Join-Path $path 'mods')
    $iris = Join-Path $root 'run/config/iris.properties'
    if (Test-Path -LiteralPath $iris) { Copy-Item -LiteralPath $iris -Destination (Join-Path $path 'config') }
    # Nobody can dismiss Distant Horizons' update prompt or NeoForge's separate splash window here.
    [IO.File]::WriteAllText((Join-Path $path 'config/DistantHorizons.toml'), "[client.advanced.autoUpdater]`nenableAutoUpdater = false`n")
    [IO.File]::WriteAllText((Join-Path $path 'config/fml.toml'), "earlyWindowControl = false`nearlyWindowWidth = 1920`nearlyWindowHeight = 1080`n")
    $options = @(Get-Content -LiteralPath (Join-Path $root 'run/options.txt') | Where-Object { $_ -notmatch '^(fullscreen|overrideWidth|overrideHeight|pauseOnLostFocus):' })
    $options += 'fullscreen:false', 'overrideWidth:1920', 'overrideHeight:1080', 'pauseOnLostFocus:false'
    [IO.File]::WriteAllLines((Join-Path $path 'options.txt'), $options)
}

# The player pack as players get it, in runs/perf: its pinned mods, Patrix and Photon and its settings, from the verified
# download cache, at 2560x1440. This checkout supplies the Civilization build under test, GeckoLib and Sable. Each
# Distant Horizons setting in `$changes` is replaced, from the pack's line to the given line.
function New-PerfClient([hashtable]$changes) {
    $perf = Join-Path $root 'runs/perf'
    Reset-Directory $perf
    & python (Join-Path $root '../tools/build_player_pack.py') --client $perf --skip civilization- geckolib- sable-
    if ($LASTEXITCODE -ne 0) { throw 'Could not lay out the player pack.' }
    [IO.File]::WriteAllText((Join-Path $perf 'config/fml.toml'), "earlyWindowControl = false`nearlyWindowWidth = 2560`nearlyWindowHeight = 1440`n")
    $dh = Join-Path $perf 'config/DistantHorizons.toml'
    $settings = [IO.File]::ReadAllText($dh)
    foreach ($from in $changes.Keys) {
        if (-not $settings.Contains($from)) { throw "The pack's Distant Horizons settings no longer hold $from." }
        $settings = $settings.Replace($from, $changes[$from])
    }
    [IO.File]::WriteAllText($dh, $settings)
    $options = @(Get-Content -LiteralPath (Join-Path $perf 'options.txt'))
    $options += 'fullscreen:false', 'overrideWidth:2560', 'overrideHeight:1440', 'pauseOnLostFocus:false', 'onboardAccessibility:false', 'tutorialStep:none', 'joinedFirstServer:true'
    [IO.File]::WriteAllLines((Join-Path $perf 'options.txt'), $options)
}

$previousJavaHome = $env:JAVA_HOME
$peer = $null
Push-Location $root
try {
    $env:JAVA_HOME = $devConfig.javaHome
    Install-Sable
    $dynamicLights = Install-DynamicLights
    switch ($Task) {
        'Build' { Invoke-Gradle build }
        'Client' { Invoke-Gradle runClient }
        'Server' {
            $server = Join-Path $root 'runs/server'
            New-Item -ItemType Directory -Force $server | Out-Null
            $serverProperties = Join-Path $server 'server.properties'
            if (-not (Test-Path -LiteralPath $serverProperties)) { Copy-Item -LiteralPath (Join-Path $root 'server.local.example.properties') -Destination $serverProperties }
            Write-Host 'Development server on localhost:25566, saved in runs/server. Type stop to shut down.'
            Invoke-Gradle runServer
        }
        'GameTest' {
            Reset-Directory (Join-Path $root 'runs/gametest')
            Invoke-Gradle runGameTestServer
        }
        'Visual' {
            $visual = Join-Path $root 'runs/visual'
            New-TestClient $visual
            Copy-Item -LiteralPath (Join-Path $root 'run/saves/preview-compatibility') -Destination (Join-Path $visual 'saves/preview-compatibility') -Recurse
            if ($Scene -in $peerScenes) {
                New-TestClient (Join-Path $root 'runs/peer')
                # The peer joins once the host has opened its test port and written runs/peer/ready.txt.
                $peer = Start-Job -ArgumentList $root, $Scene -ScriptBlock {
                    param($root, $scene)
                    Set-Location -LiteralPath $root
                    $deadline = (Get-Date).AddMinutes(3)
                    while (-not (Test-Path -LiteralPath 'runs/peer/ready.txt')) {
                        if ((Get-Date) -gt $deadline) { throw 'The host never opened its test port.' }
                        Start-Sleep -Milliseconds 500
                    }
                    & ./gradlew.bat runPeerClient "-PpeerScene=$scene-peer" --console=plain *> 'runs/peer/peer.log'
                    if ($LASTEXITCODE -ne 0) { throw "Peer client failed ($LASTEXITCODE); see runs/peer/peer.log." }
                }
            }
            Write-Host "Hidden 1920x1080 visual check: $Scene. Screenshots: runs/visual/screenshots."
            Invoke-Gradle runVisualClient, "-PvisualScene=$Scene", "-PvisualFull=$($Full.IsPresent.ToString().ToLowerInvariant())"
            if ($peer) { Receive-Job $peer -Wait -ErrorAction Stop }
        }
        'Perf' {
            $perf = Join-Path $root 'runs/perf'
            $world = Join-Path $root '../.tools/perf/world'
            $packNeoForge = (Get-Content -LiteralPath (Join-Path $root '../modpack/client-files.json') -Raw | ConvertFrom-Json).dependencies.neoforge
            if (-not (Test-Path -LiteralPath $world)) {
                # Made once and kept: the benchmark creates its world when it finds none, and Distant Horizons generates
                # its terrain around the town with its own generator, to the pack's 128 chunks plus the walk's reach.
                New-PerfClient @{
                    'chunkGeneratorMode = "PRE_EXISTING_ONLY"' = "chunkGeneratorMode = `"FEATURES`"`nshowGenerationProgress = `"LOG`""
                    'lodChunkRenderDistanceRadius = 128' = 'lodChunkRenderDistanceRadius = 136'
                }
                Write-Host 'Generating the benchmark world and its Distant Horizons terrain, once; it is kept in .tools/perf/world.'
                Invoke-Gradle runPerfClient, "-Pneo_version=$packNeoForge"
                New-Item -ItemType Directory -Force (Split-Path $world) | Out-Null
                Move-Item -LiteralPath (Join-Path $perf 'saves/benchmark') -Destination $world
            }
            # Players' distant terrain comes from the server; a singleplayer client would generate it itself.
            New-PerfClient @{ 'generatorPlan = "CHUNKS_ONLY"' = 'generatorPlan = "DISABLED"' }
            Copy-Item -LiteralPath $world -Destination (Join-Path $perf 'saves/benchmark') -Recurse
            Write-Host "Hidden 2560x1440 performance benchmark in the player pack on NeoForge $packNeoForge. Report: runs/perf/report."
            Invoke-Gradle runPerfClient, "-Pneo_version=$packNeoForge"
            # Keep each report for comparison; runs/perf is rebuilt by the next run.
            $label = "$(Get-Date -Format 'yyyyMMdd-HHmmss')-$(git -C $root rev-parse --short HEAD)"
            $archive = Join-Path $root "../.tools/perf/$label"
            New-Item -ItemType Directory -Force $archive | Out-Null
            Copy-Item -Path (Join-Path $perf 'report/*') -Destination $archive -Exclude '*.jfr'
            Write-Host "Report kept in .tools/perf/$label"
        }
        'Fixture' {
            $eula = Join-Path $root 'runs/server/eula.txt'
            if (-not (Test-Path -LiteralPath $eula)) { throw 'Run dev.ps1 Server once and accept the Minecraft EULA first.' }
            $fixture = Join-Path $root 'runs/fixture'
            Reset-Directory $fixture
            Copy-Item -LiteralPath $eula -Destination $fixture
            $flat = '{"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":2}]}'
            [IO.File]::WriteAllText((Join-Path $fixture 'server.properties'), "level-name=fixture`nlevel-seed=54321987`nlevel-type=minecraft:flat`ngenerator-settings=$flat`nserver-ip=127.0.0.1`nserver-port=25567`nonline-mode=false`nview-distance=6`nsimulation-distance=6`nspawn-protection=0`ngamemode=creative`nforce-gamemode=true`ndifficulty=peaceful`n")
            $phases = if ($Phase) { @($Phase) } else { $fixturePhases[$Name] }
            foreach ($current in $phases) {
                $log = Join-Path $fixture "$current.log"
                Invoke-Gradle runFixtureServer, "-Pfixture=$Name", "-PfixturePhase=$current" -Log $log
                $pass = "$($Name.ToUpperInvariant())_$($current.ToUpperInvariant())_PASS"
                if ($current -ne 'manual' -and -not (Select-String -LiteralPath $log -Pattern $pass -SimpleMatch -Quiet)) { throw "Fixture $Name $current did not report $pass." }
            }
        }
        'Deploy' {
            $instancePath = (Resolve-Path -LiteralPath $devConfig.prismInstance).Path
            $pack = Get-Content -LiteralPath (Join-Path $instancePath 'mmc-pack.json') -Raw | ConvertFrom-Json
            $minecraft = $pack.components | Where-Object uid -eq 'net.minecraft'
            $neoForge = $pack.components | Where-Object uid -eq 'net.neoforged'
            if ($minecraft.version -ne $properties.minecraft_version -or $neoForge.version -ne $properties.neo_version) {
                throw 'Prism Minecraft/NeoForge versions do not match gradle.properties.'
            }
            $modsPath = Join-Path $instancePath 'minecraft/mods'
            if (-not (Test-Path -LiteralPath $modsPath)) { throw "Launch the Prism instance once first: $modsPath" }
            $runningGame = Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" |
                Where-Object { $_.CommandLine -and $_.CommandLine.Replace('/', '\').IndexOf($instancePath, [StringComparison]::OrdinalIgnoreCase) -ge 0 }
            if ($runningGame) { throw 'Close the Civilization Dev game before deploying. Prism itself may stay open.' }
            Invoke-Gradle build, prepareRuntimeMods
            $geckoName = "geckolib-neoforge-$($properties.minecraft_version)-$($properties.geckolib_version).jar"
            if (Get-ChildItem -LiteralPath $modsPath -Filter 'geckolib-*.jar' -File | Where-Object Name -ne $geckoName) {
                throw 'A different GeckoLib version is installed; resolve the duplicate before deploying.'
            }
            Copy-Item -LiteralPath (Join-Path $root "build/runtimeMods/$geckoName") -Destination $modsPath -Force
            Copy-Item -LiteralPath (Join-Path $root ".dev-libs/$sableJarName") -Destination $modsPath -Force
            Copy-Item -LiteralPath $dynamicLights -Destination $modsPath -Force
            # Replace only our own JAR, archiving previous builds outside the loaded mods folder.
            $existing = @(Get-ChildItem -LiteralPath $modsPath -Filter "$($properties.mod_id)-*.jar" -File)
            if ($existing.Count) {
                $backupPath = Join-Path $instancePath ('mod-backups/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
                New-Item -ItemType Directory -Force $backupPath | Out-Null
                $existing | Move-Item -Destination $backupPath
            }
            $jarName = "$($properties.mod_id)-$($properties.mod_version).jar"
            Copy-Item -LiteralPath (Join-Path $root "build/libs/$jarName") -Destination $modsPath
            Write-Host "Installed $jarName. Launch Civilization Dev, open a world, and run /civilization status."
        }
    }
}
finally {
    if ($peer) { Stop-Job $peer; Remove-Job $peer -Force }
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
