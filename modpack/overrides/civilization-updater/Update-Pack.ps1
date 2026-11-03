# Loaded by Start-Civilization.ps1, under the instance's exclusive update lock.
function Assert-CivPackPath([string]$Path, [switch]$Internal) {
    if ($Internal -and $Path -match '^civilization-updater/(managed-pack|applied-migrations)\.json$') { return }
    if ($Path -match '^(mods|shaderpacks|resourcepacks)/[a-zA-Z0-9_.+() -]+\.(jar|zip)$') { return }
    if ($Path -match '^civilization-updater/[a-zA-Z-]+\.ps1$') { return }
    throw "Unsafe managed path: $Path"
}
# The updater's own scripts travel in updaterFiles, which updaters from before self-update ignore.
function Get-CivManagedFiles($Manifest) {
    if ($Manifest.PSObject.Properties['updaterFiles']) { return @($Manifest.files) + @($Manifest.updaterFiles) }
    return @($Manifest.files)
}
function Assert-CivPack($Manifest) {
    if ($Manifest.formatVersion -ne 1 -or $Manifest.game -ne 'minecraft' -or $Manifest.channel -ne 'playtest' -or $Manifest.protocol -ne '3/0.35.0-dev' -or $Manifest.versionId -notmatch '^playtest-\d+\.\d+\.\d+-\d+$') { throw 'Unsupported pack manifest' }
    $paths=@{}; $ids=@{}
    if (@($Manifest.files).Count -lt 1 -or @($Manifest.files).Count -gt 100) { throw 'Unexpected managed file count' }
    foreach ($item in @(Get-CivManagedFiles $Manifest)) {
        Assert-CivPackPath $item.path
        if ($paths.ContainsKey($item.path) -or $item.id -notmatch '^[a-zA-Z0-9_-]+$' -or $ids.ContainsKey($item.id)) { throw 'Duplicate managed path or identity' }
        $paths[$item.path]=$true; $ids[$item.id]=$true
        if ($item.hashes.sha1 -notmatch '^[0-9a-f]{40}$' -or $item.hashes.sha512 -notmatch '^[0-9a-f]{128}$' -or $item.fileSize -le 0 -or $item.fileSize -gt 536870912 -or @($item.downloads).Count -ne 1) { throw 'Invalid managed artifact metadata' }
        $uri=[uri]$item.downloads[0]
        if ($uri.Scheme -ne 'https' -or !$uri.IsDefaultPort -or $uri.UserInfo -or $uri.Fragment) { throw 'Unsafe managed artifact URL' }
        if ($uri.Host -eq 'github.com') { Assert-CivUrl $uri.AbsoluteUri $Manifest.versionId }
        elseif ($uri.Host -ne 'cdn.modrinth.com' -or $item.path -like '*.ps1') { throw 'Unapproved managed artifact host' }
    }
    $civ=@($Manifest.files | Where-Object id -eq 'civilization')
    if ($civ.Count -ne 1 -or $civ[0].path -notmatch '^mods/civilization-(\d+\.\d+\.\d+)-dev.jar$' -or !$Manifest.versionId.StartsWith('playtest-'+$Matches[1]+'-')) { throw 'Pack/Civilization version mismatch' }
    $migrationKeys=@{}
    foreach ($patch in $Manifest.configMigrations) {
        if ($patch.path -ne 'options.txt' -and $patch.path -notmatch '^config/[a-zA-Z0-9_-]+\.toml$') { throw 'Unsafe configuration migration path' }
        if ($patch.key -notmatch '^[a-zA-Z0-9_]+$' -or $patch.section -notmatch '^[a-zA-Z0-9_.]*$' -or $patch.from -match '[\r\n]' -or $patch.value -match '[\r\n]') { throw 'Unsafe configuration migration' }
        $key=$patch.path+'|'+$patch.section+'|'+$patch.key
        if ($migrationKeys.ContainsKey($key)) { throw 'Duplicate configuration migration' }; $migrationKeys[$key]=$true
        if ($patch.path -eq 'options.txt' -and $patch.section) { throw 'Options migration cannot have a section' }
        if ($patch.path -ne 'options.txt') {
            foreach ($value in @($patch.from,$patch.value)) {
                if ($value -notmatch '^(true|false|-?\d+(\.\d+)?|"[^"\r\n]*")$') { throw 'Configuration migration needs a scalar TOML value' }
            }
        }
    }
}
function Get-CivPackVersion([string]$Tag) {
    if ($Tag -notmatch '^playtest-(\d+)\.(\d+)\.(\d+)-(\d+)$') { throw 'Invalid pack version' }
    return [version](($Matches[1..4]) -join '.')
}
function Get-CivHash([string]$Path) { (Get-FileHash -LiteralPath $Path -Algorithm SHA512).Hash.ToLowerInvariant() }
function Get-CivMigrationId($Patch) { return ($Patch.path,$Patch.section,$Patch.key,$Patch.from,$Patch.value) -join '|' }
function Get-CivAppliedMigrations([string]$Game) {
    $path=Join-Path $Game 'civilization-updater/applied-migrations.json'
    if (!(Test-Path -LiteralPath $path)) { return @() }
    # Windows PowerShell 5.1 emits a parsed JSON array as one object; enumerate it explicitly.
    return @(ConvertFrom-Json -InputObject ([IO.File]::ReadAllText($path)) | ForEach-Object { $_ })
}
function Save-CivInitialReceipt([string]$Folder,$Manifest) {
    Assert-CivPack $Manifest
    # A fresh import's settings already hold every migration its manifest lists.
    $applied=@($Manifest.configMigrations | ForEach-Object { Get-CivMigrationId $_ })
    [IO.File]::WriteAllText((Join-Path $Folder 'applied-migrations.json'),(ConvertTo-Json -InputObject $applied),[Text.UTF8Encoding]::new($false))
    $temporary=Join-Path $Folder 'managed-pack.pending'
    [IO.File]::WriteAllText($temporary,($Manifest | ConvertTo-Json -Depth 15),[Text.UTF8Encoding]::new($false))
    [IO.File]::Move($temporary,(Join-Path $Folder 'managed-pack.json'))
}
function Clear-CivTransaction([string]$Folder) {
    # Only our fixed flat scratch files; never recursively delete an instance directory.
    if (!(Test-Path -LiteralPath $Folder)) { return }
    foreach ($file in Get-ChildItem -LiteralPath $Folder -Force) {
        if ($file.PSIsContainer -or $file.Name -notmatch '^(\d+\.(new|old)|journal\.(json|pending)|committed(\.pending)?)$') { throw 'Unexpected transaction scratch file' }
        Remove-Item -LiteralPath $file.FullName
    }
    Remove-Item -LiteralPath $Folder
}
function Restore-CivTransaction([string]$Game) {
    $folder=Join-Path $Game 'civilization-updater/transaction'
    $journal=Join-Path $folder 'journal.json'
    if (!(Test-Path -LiteralPath $journal)) { Clear-CivTransaction $folder; return }
    if (Test-Path -LiteralPath (Join-Path $folder 'committed')) { return }
    $entries=[object[]](ConvertFrom-Json -InputObject ([IO.File]::ReadAllText($journal)))
    [array]::Reverse($entries)
    foreach ($entry in $entries) {
        if ($entry.path -match '^config/[a-zA-Z0-9_-]+\.toml$' -or $entry.path -eq 'options.txt') { }
        else { Assert-CivPackPath $entry.path -Internal }
        if ($entry.number -notmatch '^\d+$') { throw 'Invalid recovery entry' }
        $target=Join-Path $Game $entry.path
        if (Test-Path -LiteralPath $target) {
            $hash=Get-CivHash $target
            if ($hash -ne $entry.oldHash -and $hash -ne $entry.newHash) { throw "Recovery found a changed file: $($entry.path). Keep the instance closed for repair." }
            if ($entry.existed -and $hash -eq $entry.oldHash) { continue }
        }
        if ($entry.existed) {
            $backup=Join-Path $folder "$($entry.number).old"
            if ((Get-CivHash $backup) -ne $entry.oldHash) { throw 'Recovery backup checksum mismatch' }
            Copy-Item -LiteralPath $backup -Destination $target -Force
        } elseif (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target }
    }
    Clear-CivTransaction $folder
    Write-Host 'Restored the previous complete client pack.'
}
function Set-CivTransactionFile([string]$Source,[string]$Target) {
    if (Test-Path -LiteralPath $Target) { [IO.File]::Replace($Source,$Target,[NullString]::Value) }
    else { [IO.File]::Move($Source,$Target) }
}
function Add-CivPlanEntry($Plan,[string]$Game,[string]$Folder,[string]$Path,[string]$Staged) {
    $target=Join-Path $Game $Path; $number=$Plan.Count
    $exists=Test-Path -LiteralPath $target
    $oldHash=''; $newHash=''
    if ($exists) { $oldHash=Get-CivHash $target; Copy-Item -LiteralPath $target -Destination (Join-Path $Folder "$number.old") }
    if ($Staged) { $newHash=Get-CivHash $Staged; [IO.File]::Move($Staged,(Join-Path $Folder "$number.new")) }
    $Plan.Add([pscustomobject]@{path=$Path;number=$number;existed=$exists;oldHash=$oldHash;newHash=$newHash})
}
function Get-CivMigratedConfig([string]$Text,$Patch) {
    $separator=if ($Patch.path -eq 'options.txt') { ':' } else { '=' }
    $section=''; $found=0
    $parts=[regex]::Split($Text,'(\r?\n)')
    for ($i=0; $i -lt $parts.Count; $i+=2) {
        if ($parts[$i] -match '^\s*\[([^\]]+)\]\s*$') { $section=$Matches[1] }
        if ($section -eq $Patch.section -and $parts[$i] -match ('^(\s*'+[regex]::Escape($Patch.key)+'\s*'+$separator+'\s*)(.*?)(\s*)$')) {
            $found++
            # Only migrate the prior shipped value; keep the player's custom value.
            if ($Matches[2] -ceq $Patch.from) { $parts[$i]=$Matches[1]+$Patch.value+$Matches[3] }
        }
    }
    if ($found -gt 1) { throw 'Ambiguous configuration migration' }
    return [pscustomobject]@{Text=$parts -join '';Found=$found -eq 1}
}
function Install-CivPack([string]$Game,$Old,$Next) {
    Assert-CivPack $Old; Assert-CivPack $Next
    $folder=Join-Path $Game 'civilization-updater/transaction'
    Restore-CivTransaction $Game
    $previous=Join-Path $Game 'civilization-updater/previous-pack'
    if (Test-Path -LiteralPath $folder) {
        $root=[IO.Path]::GetFullPath((Join-Path $Game 'civilization-updater'))
        if ([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($folder)) -ne $root -or [IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($previous)) -ne $root) { throw 'Transaction directories escaped the updater folder' }
        Clear-CivTransaction $previous
        Move-Item -LiteralPath $folder -Destination $previous
    }
    New-Item -ItemType Directory -Path $folder | Out-Null
    $plan=[Collections.Generic.List[object]]::new(); $oldPaths=@{}; $nextPaths=@{}
    $oldFiles=@(Get-CivManagedFiles $Old); $nextFiles=@(Get-CivManagedFiles $Next)
    foreach ($item in $oldFiles) { $oldPaths[$item.path]=$item }
    foreach ($item in $nextFiles) { $nextPaths[$item.path]=$true }
    try {
        $position=0
        foreach ($item in $nextFiles) {
            $position++; $target=Join-Path $Game $item.path
            if (Test-CivPinnedFile $target $item) { continue }
            if (Test-Path -LiteralPath $target) {
                if (!$oldPaths.ContainsKey($item.path) -or !(Test-CivPinnedFile $target $oldPaths[$item.path])) { throw "Managed file conflicts with a local change: $($item.path)" }
            }
            Open-CivProgress 'Updating Civilization pack' "File $position of $($nextFiles.Count): $(Split-Path $item.path -Leaf)" $item.fileSize
            $staged=Join-Path $folder "$($plan.Count).new"
            Save-CivDownload $item.downloads[0] $staged
            Open-CivProgress 'Updating Civilization pack' "Verifying $(Split-Path $item.path -Leaf)..."
            if (!(Test-CivPinnedFile $staged $item)) { throw 'Pack download checksum or size mismatch' }
            if ($item.path -match '\.(jar|zip)$') { $zip=[IO.Compression.ZipFile]::OpenRead($staged); $zip.Dispose() }
            if ($item.id -eq 'civilization' -and (Get-CivJarVersion $staged) -ne (($Next.versionId -replace '^playtest-','') -replace '-\d+$','-dev')) { throw 'Civilization version does not match pack' }
            # Add-CivPlanEntry moves from a separate name so the staged destination is unique.
            [IO.File]::Move($staged,(Join-Path $folder 'journal.pending'))
            Add-CivPlanEntry $plan $Game $folder $item.path (Join-Path $folder 'journal.pending')
        }
        foreach ($item in $oldFiles) {
            if (!$nextPaths.ContainsKey($item.path) -and (Test-Path -LiteralPath (Join-Path $Game $item.path))) {
                if (!(Test-CivPinnedFile (Join-Path $Game $item.path) $item)) { throw "Retired managed file was changed locally: $($item.path)" }
                Add-CivPlanEntry $plan $Game $folder $item.path ''
            }
        }
        # Each migration applies once, so a player who later picks the old value keeps it. A setting the game
        # has not written yet stays pending until it exists.
        $applied=[Collections.Generic.List[string]]::new([string[]]@(Get-CivAppliedMigrations $Game))
        $appliedCount=$applied.Count
        $configs=@{}
        foreach ($patch in $Next.configMigrations) {
            $id=Get-CivMigrationId $patch
            if ($applied.Contains($id)) { continue }
            $path=Join-Path $Game $patch.path
            if (!(Test-Path -LiteralPath $path)) { continue }
            if (!$configs.ContainsKey($patch.path)) { $configs[$patch.path]=[IO.File]::ReadAllText($path) }
            $result=Get-CivMigratedConfig $configs[$patch.path] $patch
            $configs[$patch.path]=$result.Text
            if ($result.Found) { $applied.Add($id) }
        }
        foreach ($path in $configs.Keys) {
            if ($configs[$path] -ceq [IO.File]::ReadAllText((Join-Path $Game $path))) { continue }
            $staged=Join-Path $folder 'journal.pending'
            [IO.File]::WriteAllText($staged,$configs[$path],[Text.UTF8Encoding]::new($false))
            Add-CivPlanEntry $plan $Game $folder $path $staged
        }
        if ($applied.Count -ne $appliedCount) {
            $staged=Join-Path $folder 'journal.pending'
            [IO.File]::WriteAllText($staged,(ConvertTo-Json -InputObject @($applied.ToArray())),[Text.UTF8Encoding]::new($false))
            Add-CivPlanEntry $plan $Game $folder 'civilization-updater/applied-migrations.json' $staged
        }
        $staged=Join-Path $folder 'journal.pending'
        [IO.File]::WriteAllText($staged,($Next | ConvertTo-Json -Depth 15),[Text.UTF8Encoding]::new($false))
        Add-CivPlanEntry $plan $Game $folder 'civilization-updater/managed-pack.json' $staged
        [IO.File]::WriteAllText($staged,(ConvertTo-Json -InputObject @($plan.ToArray()) -Depth 5))
        [IO.File]::Move($staged,(Join-Path $folder 'journal.json'))
        Assert-CivStopped $Game
        Open-CivProgress 'Updating Civilization pack' 'Applying the verified pack...'
        foreach ($entry in $plan) {
            $target=Join-Path $Game $entry.path
            New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force | Out-Null
            if ($entry.existed -and (Get-CivHash $target) -ne $entry.oldHash) { throw 'A file changed while preparing the pack update' }
            if (!$entry.existed -and (Test-Path -LiteralPath $target)) { throw 'An unmanaged file appeared during pack preparation' }
            if ($entry.newHash) { Set-CivTransactionFile (Join-Path $folder "$($entry.number).new") $target }
            else { Remove-Item -LiteralPath $target }
        }
        [IO.File]::WriteAllText((Join-Path $folder 'committed.pending'),'complete')
        [IO.File]::Move((Join-Path $folder 'committed.pending'),(Join-Path $folder 'committed'))
        Write-Host "Client pack updated to $($Next.versionId)"
    } catch { Restore-CivTransaction $Game; throw }
    finally { Close-CivProgress }
}
function Invoke-CivPackUpdate([string]$Instance) {
    $game=Get-CivGameDirectory $Instance
    $receipt=Join-Path $game 'civilization-updater/managed-pack.json'
    $old=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
    Assert-CivPack $old
    $foundNew=$false
    try {
        $releases=@(Get-CivJson 'https://api.github.com/repos/stephenmccranie/civilization/releases?per_page=100')
        $release=$releases | Where-Object { !$_.draft -and $_.tag_name -match '^playtest-\d+\.\d+\.\d+-\d+$' -and @($_.assets | Where-Object name -eq 'civilization-pack.json').Count -eq 1 } | Sort-Object { Get-CivPackVersion $_.tag_name } -Descending | Select-Object -First 1
        if ($release -and (Get-CivPackVersion $release.tag_name) -gt (Get-CivPackVersion $old.versionId)) {
        $foundNew=$true
        $asset=$release.assets | Where-Object name -eq 'civilization-pack.json'
        Assert-CivUrl $asset.browser_download_url $release.tag_name
        $next=Get-CivJson $asset.browser_download_url
        Assert-CivPack $next
        if ($next.versionId -ne $release.tag_name) { throw 'Release/pack manifest mismatch' }
        $prism=Get-Content -LiteralPath (Join-Path $Instance 'mmc-pack.json') -Raw | ConvertFrom-Json
        $mc=@($prism.components | Where-Object uid -eq 'net.minecraft')[0].version
        $nf=@($prism.components | Where-Object uid -eq 'net.neoforged')[0].version
        if ($mc -ne $next.dependencies.minecraft -or $nf -ne $next.dependencies.neoforge) { throw 'This release needs a new Prism ZIP for its Minecraft/NeoForge baseline' }
        $civ=@(Get-ChildItem -LiteralPath (Join-Path $game 'mods') -Filter 'civilization-*.jar' -File)
        if ($civ.Count -ne 1) { throw 'Resolve duplicate or missing Civilization JARs' }
        if ([version]((Get-CivJarVersion $civ[0].FullName) -replace '-dev$','') -gt [version](($next.versionId -replace '^playtest-','') -replace '-\d+$','')) { throw 'Keeping the newer private Civilization build' }
        Install-CivPack $game $old $next
        }
    } catch {
        Write-Warning "Pack update skipped: $($_.Exception.Message)"
        if ($foundNew) { Show-CivFailure "The pack update could not finish. Keeping the previous pack.`n`n$($_.Exception.Message)" }
    }
    Restore-CivTransaction $game
    # Missing managed files must be repaired from the CURRENT receipt, never the old ZIP baseline.
    $current=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
    if (@(Get-CivManagedFiles $current | Where-Object { !(Test-Path -LiteralPath (Join-Path $game $_.path)) }).Count -gt 0) { Install-CivPack $game $current $current }
}
