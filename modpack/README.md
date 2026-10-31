# Civilization playtest client

[Download the Prism pack](https://github.com/stephenmccranie/civilization/releases/download/playtest-0.34.54-2/Civilization-playtest-0.34.54-2.mrpack) · [Release and checksums](https://github.com/stephenmccranie/civilization/releases/tag/playtest-0.34.54-2)

This is a **client pack for the Civilization server**, pinned to Civilization **0.34.54-dev**, Minecraft **1.21.1** and NeoForge **21.1.250**. The server rejects other Civilization versions. Ask the host privately for the server address; it is not included in this download or repository.

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

## Included files and updates

The [manifest](modrinth.index.json) is the authoritative list of all 21 pinned downloads: Civilization, GeckoLib, Sable, Jade, JEI, JourneyMap, Kiwi, Snow! Real Magic!, DH, Sodium, Iris, Lithium, FerriteCore, C2ME, Lithostitched, Grassier Grass, Continuity, **Entity Texture Features** and **Entity Model Features**, plus Photon and Patrix. ETF 7.2.6 and EMF 3.3.12 match the development instance and support Patrix's entity textures/models. Dependencies embedded by their authors, such as JEI's MezzConfig, remain inside the original downloaded JARs.

This is a snapshot of the reviewed development client, not a minimal mandatory-mod list. The client-side performance and presentation mods can differ from the server; C2ME and Lithostitched are retained for parity with that instance, not required to join. Terrain Diffusion world generation remains on the server. The download contains no worlds, LOD caches, waypoints, saved servers, credentials, launcher account data or machine-specific Java settings.

Keep this as its own playtest instance. Do not update every mod independently: DH/Iris/Sodium versions and the exact Civilization version are pinned together. When the host announces a new pack, import that release into a new instance and retain the old one until the replacement works. Personal controls and hardware choices are not overwritten by an automatic updater.

Pack revision 2 adds GUI scale 4× and FOV 90°. Existing revision-1 installations can set these two values in Minecraft's Video Settings and Options menus without downloading mods again. Minecraft stores 90° as `fov:0.5` in `options.txt`.

If the machine struggles, disable shaders first or reduce DH distance while keeping the other tuned settings. Photon supports Iris on NVIDIA/AMD/Intel; its author does not support Apple Metal GPUs. If LOD retrieval stops filling nearby terrain, reconnect once; this pack uses unmodified DH and does not include the experimental retrieval-queue patch. Do not delete caches as a first step.

## Maintaining the release

Only reviewed public manifest/settings files live in this folder. Never export the whole development instance. The builder accepts exactly four override files and rejects IPv4 address literals, machine paths and unreviewed download hosts; it never reads a user's instance or saved-server list.

From the repository root, with Python 3.11 or later:

```text
python tools/build_player_pack.py --check-downloads
```

This verifies downloaded file sizes, both hashes and ZIP integrity, then writes a deterministic `.mrpack` and SHA-256 sidecar under ignored `.tools/player-pack-build/`. Before publishing a new Civilization JAR, add `--local-civilization path/to/civilization-version.jar` to verify that candidate locally. After publishing, run without that argument using a fresh cache to verify the public GitHub asset too.

For a new release, update the versioned manifest URLs/hashes and settings together, validate, merge the task, and publish the `.mrpack`, its checksum and our matching Civilization JAR as assets on that release tag. Upload no third-party archives. Source-only changes to this pack do not change the mod version. A real Prism import, remote join, shader appearance and performance still require a player check; hash/archive verification alone does not prove those behaviors.
