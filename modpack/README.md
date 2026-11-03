# Civilization playtest client

The client is a Windows Prism Launcher instance ZIP for Minecraft 1.21.1 with NeoForge; the ZIP sets the exact loader version. Download the newest `Civilization-...-Prism-Windows.zip` from [Releases](https://github.com/stephenmccranie/civilization/releases); its release notes list player changes and checksums.

## Install in Prism Launcher

1. Install [Prism Launcher](https://prismlauncher.org/download/) and add your Minecraft account. Use **Java 21**; Prism can manage the Java installation.
2. Choose **Add Instance → Import**, select the ZIP and finish the import as a new instance; do not merge it into another instance. The launch hook is already configured.
3. The instance starts with **8,192 MiB maximum memory**, suited to a computer with at least 16 GB RAM; adjust it in Prism if needed. Java arguments and selection follow normal launcher settings.
4. Click **Play**. On first launch a visible Civilization Launcher window downloads the pinned third-party files from their original hosts, checks their sizes and SHA-1/SHA-512 hashes, then checks for approved pack updates before Minecraft starts. A failed or corrupt download stops launch with an error; click Play again to retry.
5. Photon and Patrix are already selected. In **Multiplayer → Add Server**, enter the address the host gives you privately; the pack contains none.

An older installation without the launch hook needs this new ZIP imported once as a separate instance; from then on it updates itself.

The ZIP holds Prism's `instance.cfg` and `mmc-pack.json`, the Civilization JAR, the download manifest, the launch scripts and the reviewed settings. **Third-party mods, the shader and the resource pack are never bundled or rehosted**; they download from their authors' hosts, so the first launch needs internet access. The scripts use built-in Windows PowerShell.

## Starting settings

These are starting defaults; adjust them for your hardware.

| Setting | Pack default |
| --- | --- |
| GUI scale / FOV | 4× / 90° |
| Chat text size | 50%, so twice as much fits per line while the rest of the GUI stays 4× |
| Grassier Grass blade particles / grass plants as blades | Off / Off |
| Grassier Grass blade height / height variation | 0.65 / 0.35 |
| DH distance | 128 chunks |
| DH finest horizontal resolution | Two Blocks |
| DH horizontal / vertical quality | Low / Medium |
| DH LOD biome blending / vanilla biome blending | 0 / 0 |
| DH generation / sync request rate limits | 64 each |
| DH per-player bandwidth | 100 KB/s |
| DH Pause If Moving Quickly | Off |
| DH generation plan | Chunks Only; reads existing chunks instead of generating a local world |
| DH CPU | Balanced defaults (half the logical processors); no fixed thread count |
| DH database synchronization | Normal |
| DH automatic updates | Off, to keep the tested version |
| Vanilla render / simulation distance | 12 / 5 chunks |
| Shader | Photon v1.3b, Medium profile |
| Shader materials | Normal and specular mapping on for Patrix; handheld lighting on |
| Shader clouds | High-terrain cloud altitudes kept; vanilla clouds off |
| Resource pack | Patrix 32x Basic for Minecraft 1.21 / 1.21.1 |

Lowering DH **horizontal** quality is not the same as choosing the overall Low preset, which would also change the Two Blocks and Medium values. Photon's Medium profile is expanded into its actual option values so it applies on first launch.

**Server side.** The server caps Distant Horizons at 100 KB/s per player, the same as the pack default, including clients with higher local settings. It uses a 128-chunk real-time LOD update radius. Its generation and synchronisation request-distance caps sit at 256 chunks, the minimum DH 3.3.3 allows. DH always uses the smaller of the client and server limits, so a client may choose lower values. The bandwidth cap is per player, not a server-wide budget, so distant terrain fills in gradually.

[Photon](https://modrinth.com/shader/photon-shader) and [Patrix](https://modrinth.com/resourcepack/patrix-32x) remain their authors' work under their own licenses. The pack uses only the free **32x Basic** Patrix pack.

## Automatic updates

The pre-launch hook manages the complete tested pack outside the running game: Civilization, Sodium and the other third-party mods, Photon and Patrix. Each published playtest release that carries a `civilization-pack.json` asset describes the full managed file set: stable file identities, checksums, the supported Minecraft/NeoForge baseline and any configuration migrations. A pack-only revision can update dependencies without a new Civilization version. Only published releases count: tags, drafts, local checkpoints and server-only changes never trigger a download (see the [compatibility rule](../design_direction.md#vision)).

- Every changed archive is downloaded and verified before anything is applied, with visible file and byte progress.
- Old managed files are retired when replaced, and removed mods leave the instance. Player-added mods are kept. A same-path unmanaged file or a locally modified managed file stops the update rather than being overwritten. Nothing is downgraded.
- Updates apply as a durable journaled transaction with backups of changed files, kept outside the mods folder. A failed apply restores the previous set; an interrupted one recovers before the next launch; if recovery cannot finish safely, launch stops rather than start a mixed pack. Missing files are repaired from the current managed receipt. Backups never contain worlds or credentials.
- Personal controls, DH settings and saved data are preserved. A configuration migration changes one existing value in a flat `config/*.toml` file or `options.txt` only when it still equals the stated previous default, so custom values survive. Each migration names `path`, `section` (empty for options), `key`, `from` and `value`, all as literal strings; arbitrary scripts and whole-file replacement are not supported.
- GitHub is the release trust boundary; no account or token is needed. An offline or rate-limited check leaves the installed pack launchable.
- A different Minecraft/NeoForge baseline, Java, launcher-settings or updater-script change needs a new Prism ZIP. Import it as a separate instance and keep the old one until the new one works.

## Included files

The [manifest](client-files.json) is the authoritative list of pinned files: Civilization, GeckoLib, Sable, Jade, JEI, JourneyMap, Kiwi, Snow! Real Magic!, Distant Horizons, Sodium, Iris, Lithium, FerriteCore, C2ME, Lithostitched, Grassier Grass, Continuity, Entity Texture Features and Entity Model Features (both needed for Patrix's entity textures and models), plus Photon and Patrix. Dependencies embedded by their authors stay inside the original JARs.

This is a snapshot of the reviewed development client, not a minimal list: client performance and presentation mods may differ from the server's, and C2ME and Lithostitched are kept for parity rather than needed to join. Terrain generation runs only on the server. The ZIP contains no worlds, LOD caches, waypoints, saved servers, credentials, account data or machine-specific Java settings. Keep the third-party versions pinned together.

## Troubleshooting

If the machine struggles, disable shaders first or lower DH distance while keeping the other settings. Photon supports Iris on NVIDIA, AMD and Intel GPUs; its author does not support Apple Metal. If distant terrain stops filling in nearby, reconnect once; do not delete caches as a first step.

## Maintaining the release

This folder holds only reviewed public inputs: the pinned manifest, the Prism instance template and the client override files (settings and launch scripts). Never export a whole development instance. The builder rejects IP address literals, machine paths and unreviewed download hosts, never reads a player's instance or saved-server list, and generates loader metadata from the manifest.

To build and check a pack locally (Python 3.11+, from the repository root):

```text
python tools/build_player_pack.py --check-downloads --local-civilization path/to/civilization-version.jar
```

It verifies every download's size, hashes and ZIP integrity, then writes a deterministic Prism ZIP and SHA-256 sidecar under ignored `.tools/prism-pack-build/`. The Civilization JAR is the only bundled archive.

Playtest releases are GitHub prereleases, so the updater lists releases explicitly rather than using the stable-only `latest` endpoint.

1. Finish validation, merge the approved batch and choose the tag `playtest-X.Y.Z-N` on the published commit. Raise the mod version for Civilization code changes; raise `N` for pack-only revisions. Keep each file's stable `id` when its versioned path changes; add or remove manifest entries to add or retire managed files; state any configuration migrations explicitly.
2. Update the manifest's Civilization URL, hashes and loader baseline, then run `python tools/prepare_client_release.py --jar path/to/civilization-version.jar --check-downloads`. It verifies every pinned file and writes the Prism ZIP, Civilization JAR, `civilization-pack.json` and SHA-256 sidecars under ignored `.tools/prism-release/`. It never publishes.
3. Create a **draft prerelease** for the tag, upload exactly those assets, verify the set and checksums, and write release notes covering player changes, compatibility and migrations. Never upload third-party archives.
4. Publish the draft last; the complete `civilization-pack.json` makes it eligible for automatic updates. **Never delete or replace a published asset — fix it with a new version.**
5. Verify the public downloads with a fresh builder cache (run the builder without `--local-civilization`). Import the pack in Prism and check the hook, a remote join and the appearance before calling the release proven; archive and hash checks do not prove those.

Offline updater checks: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/test_client_bootstrap.ps1`, and the same for `tools/test_client_updater.ps1`.
