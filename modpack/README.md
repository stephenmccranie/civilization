# Civilization playtest client

[Download the Prism pack](https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.7-1/Civilization-playtest-0.35.7-1-Prism-Windows.zip) · [Release notes and checksums](https://github.com/stephenmccranie/civilization/releases/tag/playtest-0.35.7-1)

The playtest uses Civilization **0.35.7-dev**, Minecraft **1.21.1** and NeoForge **21.1.265**. It includes automatic managed-pack updates with visible progress, easier inventory handling, shared crafting-table ingredients, numeric player ping, improved daily temperature timing and resource tuning. DH starts at **128 chunks / 100 KB/s per player**. Distribution uses a Windows/Prism instance ZIP; new builds no longer produce `.mrpack` files or a separate updater installer.

Server-only balance updates do not require matching client versions; see [connection compatibility](../civilization-mod/README.md#server-connections). Ask the host privately for the server address; it is not included in the package.

## Install in Prism Launcher

1. Install [Prism Launcher](https://prismlauncher.org/download/) and add your Minecraft account. Use **Java 21**; Prism can manage the Java installation.
2. Download the announced `Civilization-...-Prism-Windows.zip`. Choose **Add Instance → Import**, select the ZIP and finish the import. Import a new instance; do not merge it into a development instance. The launch hook is already configured, with no script to install or settings to enable manually.
3. The instance starts with **8,192 MiB maximum memory**, suitable for a computer with at least 16 GB RAM. Adjust that in Prism if needed. Java arguments and Java selection inherit normal launcher settings; no Java agent or Terrain Diffusion model is needed to join.
4. Click **Play**. On first launch the hook downloads pinned third-party files from their original hosts, verifies their sizes and SHA-1/SHA-512 hashes, then checks approved pack updates before Minecraft starts. A visible Civilization Launcher window shows the current file, download progress and verification; it closes automatically when setup finishes. A failed or corrupt initial download shows an error and stops launch; click Play again to retry.
5. Photon and Patrix should already be selected. In **Multiplayer → Add Server**, enter the address the host gives you privately.

For an older installation without this launcher hook, import the new ZIP once as a separate instance. Existing updater-enabled instances on the same Minecraft/NeoForge baseline can receive the release automatically. Settings migrations reduce only the former shipped DH defaults (256 chunks / 1024 KB/s); custom values are preserved.

The ZIP contains Prism's `instance.cfg` and `mmc-pack.json`, our own Civilization JAR, the download manifest, three launch scripts and reviewed settings. Third-party mods, shader and resource-pack archives are **not bundled or rehosted**. Internet access is required for the first installation. Once installed, network failures during the update check leave the installed mod available. The scripts use built-in Windows PowerShell; Python is only a maintainer build dependency.

## Starting settings

These are starting client defaults; adjust them for your hardware. The server also enforces the DH bandwidth cap below.

| Setting | Pack default |
| --- | --- |
| GUI scale / FOV | 4× / 90° |
| Grassier Grass blade particles / grass plants as blades | Off / Off |
| Grassier Grass blade height / height variation | 0.65 / 0.35; permanent server-saved trampling |
| DH distance | 128 chunks |
| DH finest horizontal resolution | Two Blocks |
| DH horizontal / vertical quality | Low / Medium |
| DH LOD biome blending | 0 |
| Vanilla biome blending | 0 |
| DH generation / sync request limits | 64 each; the server may impose a lower limit |
| DH per-player bandwidth | 100 KB/s (per player; also enforced by the server) |
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

The server uses a 128-chunk real-time LOD update radius. DH 3.3.3 has a hard 256-chunk minimum for generation and synchronization request-distance caps; both server caps are set to that minimum (synchronization was previously 4096). Client rendering remains 128 chunks; the server does not render LODs or erase cached terrain. The current server separately caps generation requests at 64 and synchronization requests at 50; DH negotiates the smaller client/server limit. The server caps each player at 100 KB/s for outside-network playtesting, including existing clients with higher local settings. Clients can choose a lower limit. This is per player, not a global server budget; LOD downloads will take longer at this cap. Turning down **horizontal** quality does not mean selecting the overall Low quality preset: that would also change the Two Blocks / Medium values. DH's CPU preset is derived from hardware; the pack deliberately omits machine-specific thread counts.

Photon's Medium profile is expanded into its actual shader option values so it applies on first launch. Patrix's material mapping and the existing high-altitude cloud settings are additional overrides. [Photon](https://modrinth.com/shader/photon-shader) and [Patrix](https://modrinth.com/resourcepack/patrix-32x) remain their authors' work and retain their original licenses. This includes only the freely available **32x Basic** pack, not paid resolutions or optional Patrix add-ons.

## Automatic Civilization updates

The preconfigured Prism hook manages the complete tested pack outside the running game: Civilization, Sodium and the other third-party mods, Photon and Patrix. Each published release with a `civilization-pack.json` asset describes the complete managed file set, stable file identities, checksums, supported Minecraft/NeoForge baseline and explicit configuration migrations. Pack revisions such as `playtest-0.35.4-2` can update dependencies without changing Civilization's mod version. A tag alone does not trigger an update. Published playtest prereleases participate; drafts do not.

The updater downloads and verifies every changed archive before applying the pack. A visible window shows file/byte progress and verification. The managed receipt records installed paths, so old managed filenames are retired when replaced and removed mods leave the instance. Extra player-added mods are preserved. A same-path unmanaged file or a locally changed managed file stops that update instead of being overwritten. Lower pack revisions and newer private Civilization builds are not downgraded.

Applying an update uses a durable local transaction journal and backups of changed files. A failed apply restores the previous set; an interrupted apply recovers before the next launch. The current and previous transaction backups remain outside the mods folder. If recovery cannot safely finish, launch stops for repair rather than starting a mixed pack. Missing files are repaired from the current managed receipt, never from an older ZIP's pinned baseline. No updater backup contains worlds or credentials.

Routine updates preserve personal controls, DH bandwidth/quality and saved data. Optional `configMigrations` change individual existing values in flat `config/*.toml` files or `options.txt` only when the current value equals the specified prior shipped value. Custom values and missing keys/files are preserved. For example, a migration can set the `server` section's `playerBandwidthLimit` from `1024` to `100` without resetting the rest of the file. Each migration contains `path`, `section` (empty for options), `key`, `from` and `value`; scalar values are strings containing their literal TOML/options representation. Paths, keys and migration values are validated; arbitrary scripts and whole-configuration replacement are not supported.

GitHub is the release trust boundary, with no account/token required. Third-party files still download from their original hosts. An offline/rate-limited release check keeps a complete installed pack launchable. Failed initial installation or missing-file repair stops launch and can be retried with Play. A newer release with a different Minecraft/NeoForge baseline or protocol needs a new Prism ZIP. Java, launcher settings and updater-script changes also remain explicit installation upgrades.

## Included files and updates

The [manifest](client-files.json) is the authoritative list of all 21 pinned files: Civilization, GeckoLib, Sable, Jade, JEI, JourneyMap, Kiwi, Snow! Real Magic!, DH, Sodium, Iris, Lithium, FerriteCore, C2ME, Lithostitched, Grassier Grass, Continuity, **Entity Texture Features** and **Entity Model Features**, plus Photon and Patrix. ETF 7.2.6 and EMF 3.3.12 match the development instance and support Patrix's entity textures/models. Dependencies embedded by their authors, such as JEI's MezzConfig, remain inside the original downloaded JARs.

This is a snapshot of the reviewed development client, not a minimal mandatory-mod list. The client-side performance and presentation mods can differ from the server; C2ME and Lithostitched are retained for parity with that instance, not required to join. Terrain Diffusion world generation remains on the server. The ZIP contains no worlds, LOD caches, waypoints, saved servers, credentials, launcher account data or machine-specific Java settings.

Keep this as its own playtest instance. Leave third-party mod versions pinned together. Approved compatible pack releases update through the pre-launch hook above, including third-party mods. When the host announces a new Minecraft/loader/dependency baseline, import the new Prism ZIP into a separate instance and retain the old one until the replacement works. Its updater is already enabled.

The earlier 0.34.54 pack revision 2 added GUI scale 4× and FOV 90°, and disabled Grassier Grass's blade particles and “grass plants as blades.” Existing revision-1 installations can change those four settings in-game without downloading mods again. Minecraft stores 90° as `fov:0.5` in `options.txt`; Grassier Grass stores `bladeParticles=false` and `grassPlantsAsBlades=false` in the `[rendering]` section of `config/grassiergrass-client.toml`.

If the machine struggles, disable shaders first or reduce DH distance while keeping the other tuned settings. Photon supports Iris on NVIDIA/AMD/Intel; its author does not support Apple Metal GPUs. If LOD retrieval stops filling nearby terrain, reconnect once; this pack uses unmodified DH and does not include the experimental retrieval-queue patch. Do not delete caches as a first step.

## Maintaining the release

Only reviewed public inputs live in this folder. Never export the whole development instance. The builder accepts exactly eight client override files (five settings files and three launch scripts), the pinned download manifest and the Prism instance template. It rejects IPv4 address literals, machine paths and unreviewed download hosts; it never reads a user's instance or saved-server list. Loader metadata is generated from the manifest dependencies.

From the repository root, with Python 3.11 or later:

```text
python tools/build_player_pack.py --check-downloads --local-civilization path/to/civilization-version.jar
```

This verifies downloaded file sizes, both hashes and ZIP integrity, then writes a deterministic native Prism ZIP and SHA-256 sidecar under ignored `.tools/prism-pack-build/`. Our verified Civilization JAR is the only bundled archive; the other 20 files remain original-host downloads. After publishing, run without the local-JAR argument using a fresh cache to verify the public GitHub asset too.

Keep small pack and gameplay adjustments local with the current development batch. Tags identify source commits; **GitHub Releases distribute approved complete pack updates**. Existing playtest Releases are prereleases, so the updater lists Releases explicitly instead of using GitHub's stable-only `latest` endpoint. Creating a tag alone never triggers an update.

For an authorized substantial release:

1. Finish local validation, merge the approved source batch, and choose `playtest-X.Y.Z-N` on the verified published commit. Increase the mod version for Civilization code changes; increase `N` for pack-only revisions such as Sodium updates. Keep each file's stable `id` when changing its versioned path. Add/remove manifest entries to add/retire managed files; describe any targeted configuration migrations explicitly.
2. Update the manifest's versioned Civilization URL, hashes and loader baseline. Prepare assets locally with `python tools/prepare_client_release.py --jar path/to/civilization-version.jar --check-downloads`. This verifies all 21 files and creates the Prism instance ZIP, Civilization JAR, update manifest and SHA-256 sidecars under ignored `.tools/prism-release/`. The builder never publishes.
3. Create a **draft prerelease** for the tag. Upload those assets, verify the asset set and checksums, and write release notes describing player changes, compatibility and any migration. Upload no third-party archives or legacy `.mrpack`, standalone-installer or single-mod update-manifest assets.
4. Publish the draft last. The complete `civilization-pack.json` asset makes that published Release eligible for automatic pack updates. Server-only changes need no client Release/manifest. Do not delete or silently replace published binary assets; fix them with a new version.
5. Verify the public downloads with a fresh builder cache, then update the public download links. Import the pack in Prism and check the hook, remote join and appearance before declaring the distribution proven. Archive/hash checks do not prove these player behaviors.

Run focused offline Windows checks with `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/test_client_bootstrap.ps1` and the same command for `tools/test_client_updater.ps1`. No gameplay source changes are involved in updater-only changes. Never publish a draft until its complete asset set has been reviewed.
