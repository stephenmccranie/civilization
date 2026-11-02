# Civilization playtest client

[Download the Prism pack](https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.0-1/Civilization-playtest-0.35.0-1.mrpack) · [Release and checksums](https://github.com/stephenmccranie/civilization/releases/tag/playtest-0.35.0-1)

This release includes shared parts and hand assembly, wheelbarrow/pallet hauling, permanent grass wear and thermal improvements. Existing worlds are retained; see [manufacturing migration](../civilization-mod/manufacturing.md#existing-saves-and-limits) and [block recovery](../civilization-mod/physics.md#personal-block-carrying).

This is a **client pack for the Civilization server**, pinned to Civilization **0.35.0-dev**, Minecraft **1.21.1** and NeoForge **21.1.250**. The current server accepts this 0.35.0 client through its stable network protocol; server-only balance updates do not require a client upgrade. [Compatibility details](../civilization-mod/README.md#server-connections). Ask the host privately for the server address; it is not included in this download or repository.

## Install in Prism Launcher

1. Install [Prism Launcher](https://prismlauncher.org/download/) and add your Minecraft account. Use **Java 21**; Prism can manage the Java installation.
2. Download the `.mrpack` above. In Prism choose **Add Instance → Import**, select that file, and complete the import. Allow its pinned downloads to finish. Import it as a new instance rather than copying an old development instance.
3. Open **Edit → Settings → Java / Memory** for the new instance. Start with **8,192 MiB maximum memory** on a computer with at least 16 GB of RAM. Keep Prism's normal Java arguments; no Java agent or Terrain Diffusion model download is needed to join.
4. Launch the instance. Photon and Patrix should already be selected. In **Multiplayer → Add Server**, enter the address the host gives you privately.

The pack is a small [Modrinth-format manifest](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack), which [Prism imports directly](https://prismlauncher.org/wiki/getting-started/download-modpacks/). Third-party mods, the shader and resource pack download from their original Modrinth hosts with pinned SHA-1/SHA-512 hashes. They are **not bundled or rehosted by this repository**. Only our Civilization JAR is served from this GitHub release. An Internet connection is needed for installation.

## Starting settings

These are client defaults, not forced server settings. Adjust them for your hardware after trying the pack.

| Setting | Pack default |
| --- | --- |
| GUI scale / FOV | 4× / 90° |
| Grassier Grass blade particles / grass plants as blades | Off / Off |
| Grassier Grass blade height / height variation | 0.65 / 0.35; permanent server-saved trampling |
| DH distance | 256 chunks |
| DH finest horizontal resolution | Two Blocks |
| DH horizontal / vertical quality | Low / Medium |
| DH LOD biome blending | 0 |
| Vanilla biome blending | 0 |
| DH generation / sync request limits | 64 each; the server may impose a lower limit |
| DH per-player bandwidth | 1 MB/s (`1024` KB/s in DH) |
| DH Pause If Moving Quickly | Off |
| DH generation plan | Chunks Only; read pre-existing chunks rather than generating a local world |
| DH CPU | Balanced defaults: half the logical processors, full thread runtime; no fixed count shipped |
| DH database synchronization | Normal |
| DH automatic updates | Off, to retain the tested version |
| Vanilla render / simulation distance | 12 / 5 chunks |
| Shader | Photon v1.3b, Medium profile |
| Shader materials | Normal and specular mapping on for Patrix; handheld lighting on |
| Shader clouds | Existing high-terrain cloud altitudes retained; vanilla clouds off |
| Resource pack | Patrix 32x Basic for Minecraft 1.21 / 1.21.1, release 72 |

The current server separately caps generation requests at 64 and synchronization requests at 50; DH negotiates the smaller client/server limit. Bandwidth is per player, not a global server budget. Turning down **horizontal** quality does not mean selecting the overall Low quality preset: that would also change the Two Blocks / Medium values. DH's CPU preset is derived from hardware; the pack deliberately omits machine-specific thread counts.

Photon's Medium profile is expanded into its actual shader option values so it applies on first launch. Patrix's material mapping and the existing high-altitude cloud settings are additional overrides. [Photon](https://modrinth.com/shader/photon-shader) and [Patrix](https://modrinth.com/resourcepack/patrix-32x) remain their authors' work and retain their original licenses. This includes only the freely available **32x Basic** pack, not paid resolutions or optional Patrix add-ons.

## Automatic Civilization updates

The next **local, unpublished** pack candidate is `playtest-0.35.4-1`, with Civilization 0.35.4-dev, Minecraft 1.21.1 and NeoForge 21.1.265. The download at the top still points to the current published pack, which does not contain the updater.

The updater is part of the pack, outside the mod. Windows/Prism players enable it once: close Minecraft and Prism, open the instance's Minecraft folder, and run `civilization-updater/Install-Client-Updates.cmd`. Then start Prism normally. For an existing instance, extract the Windows updater ZIP into its Minecraft folder first. Installation preserves Java arguments and personal settings; it refuses to replace an existing custom launch command. Other launchers/platforms still use manual installation.

Before each game launch, the updater checks this repository's published `playtest-*` GitHub Releases, including prereleases. Only Releases with a `civilization-update.json` asset participate. It installs a strictly newer Civilization version after verifying the owned GitHub URL, file size, SHA-256 and embedded mod identity/version. The replacement is atomic; one previous JAR stays in `civilization-updater/previous.jar`, outside `mods`. The installed filename may retain its original version; the embedded mod version is authoritative. Duplicate Civilization JARs are rejected.

Only the Civilization JAR changes. Personal controls, DH bandwidth/quality, other mods and saved data are preserved. Network/download failures retain the verified installed JAR and allow launch. An incompatible Minecraft/NeoForge baseline or protocol requires an announced full-pack upgrade; the updater does not change the loader. It never downgrades or replaces a same-version private build. GitHub is the release trust boundary; the public feed needs no account or token. API rate limits or offline access simply skip that check.

To roll back, close Minecraft, turn off the instance pre-launch command, move the single Civilization JAR out of `mods`, and copy `previous.jar` into `mods` as `civilization-client.jar`. Leave only one Civilization JAR. A later automatic check would otherwise reinstall the newer approved version. Updater-script changes require a new one-time installer or full pack; scripts do not update themselves.

## Included files and updates

The [manifest](modrinth.index.json) is the authoritative list of all 21 pinned downloads: Civilization, GeckoLib, Sable, Jade, JEI, JourneyMap, Kiwi, Snow! Real Magic!, DH, Sodium, Iris, Lithium, FerriteCore, C2ME, Lithostitched, Grassier Grass, Continuity, **Entity Texture Features** and **Entity Model Features**, plus Photon and Patrix. ETF 7.2.6 and EMF 3.3.12 match the development instance and support Patrix's entity textures/models. Dependencies embedded by their authors, such as JEI's MezzConfig, remain inside the original downloaded JARs.

This is a snapshot of the reviewed development client, not a minimal mandatory-mod list. The client-side performance and presentation mods can differ from the server; C2ME and Lithostitched are retained for parity with that instance, not required to join. Terrain Diffusion world generation remains on the server. The download contains no worlds, LOD caches, waypoints, saved servers, credentials, launcher account data or machine-specific Java settings.

Keep this as its own playtest instance. Leave third-party mod versions pinned together. Approved compatible Civilization releases can update through the pre-launch hook above. When the host announces a new Minecraft/loader/dependency baseline, import the full pack into a new instance and retain the old one until the replacement works; enable its updater once.

The earlier 0.34.54 pack revision 2 added GUI scale 4× and FOV 90°, and disabled Grassier Grass's blade particles and “grass plants as blades.” Existing revision-1 installations can change those four settings in-game without downloading mods again. Minecraft stores 90° as `fov:0.5` in `options.txt`; Grassier Grass stores `bladeParticles=false` and `grassPlantsAsBlades=false` in the `[rendering]` section of `config/grassiergrass-client.toml`.

If the machine struggles, disable shaders first or reduce DH distance while keeping the other tuned settings. Photon supports Iris on NVIDIA/AMD/Intel; its author does not support Apple Metal GPUs. If LOD retrieval stops filling nearby terrain, reconnect once; this pack uses unmodified DH and does not include the experimental retrieval-queue patch. Do not delete caches as a first step.

## Maintaining the release

Only reviewed public manifest/settings files live in this folder. Never export the whole development instance. The builder accepts exactly eight override files (five settings files and three updater/installer scripts) and rejects IPv4 address literals, machine paths and unreviewed download hosts; it never reads a user's instance or saved-server list.

From the repository root, with Python 3.11 or later:

```text
python tools/build_player_pack.py --check-downloads
```

This verifies downloaded file sizes, both hashes and ZIP integrity, then writes a deterministic `.mrpack` and SHA-256 sidecar under ignored `.tools/player-pack-build/`. Before publishing a new Civilization JAR, add `--local-civilization path/to/civilization-version.jar` to verify that candidate locally. After publishing, run without that argument using a fresh cache to verify the public GitHub asset too.

Keep small pack and gameplay adjustments local with the current development batch. Tags identify source commits; **GitHub Releases distribute approved client updates**. Existing playtest Releases are prereleases, so the updater lists Releases explicitly instead of using GitHub's stable-only `latest` endpoint. Creating a tag alone never triggers an update.

For an authorized substantial release:

1. Finish local validation, merge the approved source batch, and choose `playtest-X.Y.Z-N` on the verified published commit. Increase the mod version for code changes; increase `N` for pack-only revisions.
2. Update the manifest's versioned Civilization URL, hashes and loader baseline. Prepare assets locally with `python tools/prepare_client_release.py --jar path/to/civilization-version.jar --check-downloads`. This verifies all 21 downloads and creates the pack, Civilization JAR, Windows installer ZIP, update manifest and SHA-256 sidecars under ignored `.tools/client-release/`. The builder never publishes.
3. Create a **draft prerelease** for the tag. Upload those assets, verify the asset set and checksums, and write release notes describing player changes, compatibility and any migration. Upload no third-party archives.
4. Publish the draft last. The `civilization-update.json` asset makes that published Release eligible for automatic client updates. Server-only changes need no client Release/manifest. Do not delete or silently replace published binary assets; fix them with a new version.
5. Verify the public downloads with a fresh builder cache, then update the public download links. Import the pack in Prism and check the hook, remote join and appearance before declaring the distribution proven. Archive/hash checks do not prove these player behaviors.

Run focused offline Windows checks with `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/test_client_updater.ps1`. No gameplay source changes are involved in updater-only changes. Never publish a draft until its complete asset set has been reviewed.
