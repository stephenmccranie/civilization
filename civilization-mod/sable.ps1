[CmdletBinding()]
param([ValidateSet('Prepare','Compile','Verify','Server','Visual')][string]$Task='Verify',[switch]$Graphics)
$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dev.local.json') -Raw | ConvertFrom-Json
$oldJava=$env:JAVA_HOME
Push-Location $PSScriptRoot
try {
 $env:JAVA_HOME=$config.javaHome
 $libs=Join-Path $PSScriptRoot '.dev-libs'
 New-Item -ItemType Directory -Force $libs | Out-Null
 $jar=Join-Path $libs 'sable-neoforge-1.21.1-2.0.5.jar'
 $hash='bf3d8c87bcc5efb99afffd50305fc978086ed48a63e106816e3a8a3901f8052f4480ea527dbd7d4003f7775ed4d020529098034f4e307aefac9cc42df2b4c19a'
 if(-not(Test-Path -LiteralPath $jar)){Invoke-WebRequest 'https://cdn.modrinth.com/data/T9PomCSv/versions/U678xqle/sable-neoforge-1.21.1-2.0.5.jar' -OutFile $jar}
 if((Get-FileHash -LiteralPath $jar -Algorithm SHA512).Hash -ne $hash){throw 'Sable release hash mismatch'}
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 $zip=[IO.Compression.ZipFile]::OpenRead($jar)
 try {foreach($entry in $zip.Entries){if($entry.FullName -like 'META-INF/jarjar/*.jar'){[IO.Compression.ZipFileExtensions]::ExtractToFile($entry,(Join-Path $libs $entry.Name),$true)}}} finally {$zip.Dispose()}
 if($Task -eq 'Prepare'){Write-Host 'Pinned Sable 2.0.5 prepared and hash verified.';return}
 $server=Join-Path $PSScriptRoot 'runs/sable-server'
 New-Item -ItemType Directory -Force $server | Out-Null
 if(-not(Test-Path -LiteralPath "$server/server.properties")){
  [IO.File]::WriteAllText("$server/server.properties","level-name=sable-lab`nserver-ip=127.0.0.1`nserver-port=25567`nonline-mode=false`nview-distance=6`nsimulation-distance=6`nspawn-protection=0`ngamemode=creative`ndifficulty=peaceful`nmax-tick-time=60000`n")
 }
 # Isolated development server uses the same local EULA acknowledgement as the existing dev server.
 $eula=Join-Path $PSScriptRoot 'runs/server/eula.txt'
 if(Test-Path -LiteralPath $eula){Copy-Item -LiteralPath $eula -Destination "$server/eula.txt"}
 $tasks=@('compileSableLabJava')
 if($Task -eq 'Verify'){$tasks=@('seed','reload')}
 if($Task -eq 'Server'){$tasks=@('manual')}
 if($Task -eq 'Visual'){
  $visual=Join-Path $PSScriptRoot ('runs/sable-visual'+$(if($Graphics){'-graphics'}else{''}))
  # Gradle gets the exact isolated visual directory; baseline and graphics runs never share mods.
  foreach($dir in @('config','saves','mods','resourcepacks','shaderpacks')){New-Item -ItemType Directory -Force "$visual/$dir" | Out-Null}
  if(-not(Test-Path -LiteralPath "$server/sable-lab")){throw "Run sable.ps1 Verify first to create the fixture world"}
  if(-not(Test-Path -LiteralPath "$visual/saves/preview-compatibility")){Copy-Item -LiteralPath "$server/sable-lab" -Destination "$visual/saves/preview-compatibility" -Recurse}
  [IO.File]::WriteAllText("$visual/config/fml.toml","earlyWindowControl=false`n")
  $options=@('fullscreen:false','overrideWidth:1920','overrideHeight:1080','pauseOnLostFocus:false','renderDistance:8','simulationDistance:6','guiScale:3')
  if($Graphics){
   $game=Join-Path $config.prismInstance 'minecraft'
   foreach($pattern in @('sodium-*.jar','iris-*.jar','DistantHorizons-*.jar')){Get-ChildItem -LiteralPath "$game/mods" -Filter $pattern | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination "$visual/mods"}}
   foreach($dir in @('resourcepacks','shaderpacks')){Get-ChildItem -LiteralPath "$game/$dir" -File | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination "$visual/$dir"}}
   if(Test-Path -LiteralPath "$game/config/iris.properties"){Copy-Item -LiteralPath "$game/config/iris.properties" -Destination "$visual/config/iris.properties"}
   $options+=@(Get-Content -LiteralPath "$game/options.txt" | Where-Object {$_ -match '^(resourcePacks|incompatibleResourcePacks):'})
  }
  [IO.File]::WriteAllLines("$visual/options.txt",$options)
  & ./gradlew.bat --console=plain '-PsableLab=true' "-PsableVisualDir=$visual" runSableClient | Tee-Object -FilePath "$visual/check.log"
  if($LASTEXITCODE -ne 0 -or -not(Select-String -LiteralPath "$visual/check.log" -Pattern 'CIV_SABLE_VISUAL_PASS' -Quiet)){throw 'Sable visual check failed'}
  return
 }
 foreach($phase in $tasks){
  if($phase -eq 'compileSableLabJava'){& ./gradlew.bat --console=plain '-PsableLab=true' compileSableLabJava}
  else {& ./gradlew.bat --console=plain '-PsableLab=true' "-PsablePhase=$phase" runSableServer | Tee-Object -FilePath "$server/check-$phase.log"}
  if($LASTEXITCODE -ne 0){throw "Sable lab failed: $phase"}
  if($phase -in @('seed','reload')){if(-not(Select-String -LiteralPath "$server/check-$phase.log" -Pattern "CIV_SABLE_$($phase.ToUpper())_PASS" -Quiet)){throw "Missing explicit pass result: $phase"}}
 }
} finally {$env:JAVA_HOME=$oldJava;Pop-Location}
