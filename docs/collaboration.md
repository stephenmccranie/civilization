# Working on a separate helicopter branch

The development handoff is a source snapshot of the working files, including changes not yet committed in the owner's repository. `HANDOFF-MANIFEST.json` in the exported archive records its version and SHA-256 file hashes. It is not a launcher instance or a ready-to-run modpack. It includes original artwork, concepts, tools and documentation, but excludes `.git`, local configuration, downloaded dependencies, caches, logs and worlds.

## Start from the archive

Extract the ZIP into its own folder, outside any existing repository. Open that folder in Codex or your editor. Install Git and JDK 21. No GitHub account is necessary for local branches.

Before changing files, establish a baseline in the extracted project root:

```powershell
git init -b handoff/base
```

If Git has no configured author, set your own actual name and email locally with `git config user.name` and `git config user.email`. Do not copy the original developer's identity or invent one. Then:

```powershell
git add .
git commit -m "Import Civilization development handoff"
git tag handoff-base
git switch -c codex/helicopters
```

Keep the baseline commit and tag unchanged. Only helicopter changes should follow that commit. The owner retains the original snapshot for comparison, so unrelated ongoing work can be preserved when integrating your changes.

## Build and run

The exact Minecraft, NeoForge, GeckoLib and JEI versions are in [gradle.properties](../civilization-mod/gradle.properties); Sable is pinned by [sable.ps1](../civilization-mod/sable.ps1). Current target is Minecraft 1.21.1, NeoForge 21.1.250, JDK 21, GeckoLib 4.9.3 and Sable 2.0.5.

From the extracted project root:

```powershell
Copy-Item civilization-mod/dev.local.example.json civilization-mod/dev.local.json
```

Edit the copied JSON to point `javaHome` at your JDK 21 and `prismInstance` at your own Prism instance folder. Build-only work does not need a usable Prism instance path, but the configuration file must exist. Then:

```powershell
./civilization-mod/dev.ps1 Verify
./civilization-mod/dev.ps1 Client
```

The scripts download and hash-check pinned Sable, extract its bundled compile dependencies, and let Gradle resolve NeoForge/GeckoLib/JEI. The first build needs internet access and will take longer than subsequent builds. Do not copy the owner's absolute paths. Shader packs and Faithful are not included; install your own for visual reviews. The hidden visual fixtures need a disposable local development world first; see [testing](../civilization-mod/testing.md). Use a fresh world for helicopter work.

For Prism deployment, create an instance with the versions above, launch it once, close Minecraft, and run `./civilization-mod/dev.ps1 Deploy`. Python 3 and Pillow are useful for the art pipeline; Blockbench tooling has its own [setup](../tools/modeling/README.md). None is required merely to compile the Java mod.

## Helicopter starting points

- Read [AGENTS.md](../AGENTS.md), [current status](status.md), [design direction](../design_direction.md) and [vehicle integration](../civilization-mod/vehicle-physics.md).
- The two helicopter images are proposals, not approved mechanics: [Kestrel](../concept_art/helicopters/kestrel.png), [Meridian](../concept_art/helicopters/meridian.png), [brief](../concept_art/helicopters/brief.md), and [generation/review record](../concept_art/helicopters/receipt.json).
- Start with `BoatSystem`, `BoatEngine`, `BoatPayload`, `BoatProtection` and `SablePhysicalBridge` in `civilization-mod/src/main/java/dev/civilization`. Existing boats use Sable; helicopter aerodynamics are not implemented.
- Reuse the [physical-material foundation](../civilization-mod/physics.md), [oil fuel/lubrication](../civilization-mod/industry.md#oil-engine), shared UI and [art skill](../.agents/skills/civilization-art/SKILL.md). Keep flight/control logic separate from boats where their behavior differs.
- Preserve simple controls, server-owned fuel/physics, persistence, claim boundaries and the late-industrial/Tesla material style. Choose the aircraft design with the owner before treating either concept as final. Airship/rotorcraft progression, cargo, speed and recipes remain to settle.
- Keep changes focused on helicopters and shared adapters they actually need. Follow the existing change-specific tests and update the affected documentation. Do not rewrite unrelated balance or regenerate every art family.

## Send changes back without hosting

Commit your finished work on `codex/helicopters`, then create a Git bundle outside the project folder:

```powershell
git status --short
git bundle create ../civilization-helicopters.bundle codex/helicopters handoff-base
git bundle verify ../civilization-helicopters.bundle
```

Send the bundle and a short summary of controls, testing and limitations. This bundle includes the baseline and may be large because original artwork is included. For a smaller return, export only the binary-safe change set instead:

```powershell
git diff --binary --full-index --output=../civilization-helicopters.patch handoff-base HEAD
```

The patch includes committed changes and binary assets; it does not include uncommitted or untracked work. The receiving assistant should compare it against the retained source archive and integrate in an isolated checkout. For a bundle, import its refs and apply only commits after `handoff-base`; do not merge the unrelated initial import commit into the owner's existing history. Do not overwrite the owner's live project with the returned folder.

## Creating a fresh handoff

From the owner's project, run `python tools/package_handoff.py`. It packages existing tracked and non-ignored untracked working files without staging, committing, changing branches or reading runtime worlds. Output goes under ignored `.tools/handoffs`; each archive has a SHA-256 manifest and a separate ZIP checksum. Build checks are independent; the package records the current version without claiming a fresh-machine build has been tested.
