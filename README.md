# Civilization

A custom Minecraft civilization MMO: personal labor grows into industry, trade, concentrated power, and extraordinary sky settlements. Hierarchy and visible status are part of the competition. Target: up to 200 concurrent players, maintained by one human with assistant support.

## Playtest client — Prism Launcher

The current playtest is **0.35.7-dev**, distributed as a **Windows/Prism instance ZIP** with automatic managed-pack updates enabled. [Download the Prism pack](https://github.com/stephenmccranie/civilization/releases/download/playtest-0.35.7-2/Civilization-playtest-0.35.7-2-Prism-Windows.zip) or view the [release notes and checksums](https://github.com/stephenmccranie/civilization/releases/tag/playtest-0.35.7-2).

In [Prism Launcher](https://prismlauncher.org/download/), choose **Add Instance → Import** and select the announced `Civilization-...-Prism-Windows.zip`. Use **Java 21**. The instance starts with **8 GB maximum memory** on a computer with at least 16 GB RAM. Click Play: first launch downloads pinned third-party files from their original hosts, then starts Minecraft; later launches automatically update the complete managed pack from approved releases, including third-party mods. No separate updater installation is needed.

The pack includes tuned Distant Horizons settings (**100 KB/s per player**), Photon **Medium**, Patrix **32x**, ETF and EMF. Only our own Civilization JAR is bundled; third-party mod/shader/texture archives download from their authors' hosts. Personal settings are retained by routine pack updates; explicit setting migrations apply only to matching prior defaults. Server-only balance changes do not require matching client builds; see [connection compatibility](civilization-mod/README.md#server-connections).

Add the server address supplied **privately by the host**; no address is included. [Full installation, settings, versions and troubleshooting](modpack/README.md). Check [runtime status](docs/status.md#local-runtime) for the active server build. The published 0.35.0 client can still connect but keeps its old small-placeable inventory restrictions.

## Start here

| Need | Document |
| --- | --- |
| What works today, build and validation | [Current status](docs/status.md) |
| Agreed vision and game rules | [Design](design_direction.md) |
| What gets built next | [Development plan](development_plan.md) |
| Choices still open | [Open decisions](open_decisions.md) |
| Recipes, controls and development commands | [Mod reference](civilization-mod/README.md) |
| All references and historical material | [Documentation index](docs/README.md) |

Java + NeoForge; Photon is the recommended shader. New Civilization artwork targets 64×64 pixels per standard block face/item, beginning with Street Pavers; existing 32×32 art will migrate by family. The published playtest client uses Patrix 32x for vanilla assets; Faithful remains the development art-review companion. Exact runtime versions live in [gradle.properties](civilization-mod/gradle.properties). The concurrency target is a design goal, not verified capacity.

## Project layout

- `civilization-mod/src/`: production code/resources, unit tests, and isolated GameTest/visual fixtures.
- `civilization-mod/dev.ps1`: build, change-specific verification and Prism deployment.
- `art/textures/`: original masters, manifests, export tools, receipts and review sheets.
- `art/construction/`: selected in-game construction/renderer evidence.
- `concept_art/`: world/vibe references and prompts.
- `docs/history/`: old release notes and bounded playtest observations.
- `docs/archive/`: frozen document snapshots.
- `tools/`: small project-maintenance tools.
- `modpack/`: public, download-only Prism client manifest and reviewed starting settings.
- Ignored local state: `.tools/`, Gradle/build caches, mod `run/` and `runs/`, and `dev.local.json`. These are not additional source trees.

Each rule has one home. Update the relevant reference with the code or decision; keep proposals separate from shipped behavior. [Contributor instructions](AGENTS.md) make this part of the normal workflow.

## Source control

The project is maintained in the public GitHub repository `stephenmccranie/civilization`. The [GitHub workflow](AGENTS.md#github-workflow) is the authoritative contributor policy: carefully stage and validate work locally, then group related changes into substantial published updates. A completed small change does not trigger a push, PR or release. Publish only on an explicit request or an agreed milestone; once authorized, carry the batch through PR checks, squash merge and safe cleanup without repeated confirmation. `main` stays the verified published version; pending development checkpoints stay on their local task/batch branch. Historical commits already on `main` are preserved.

The repository contains code, runtime resources, documentation and reusable tools. Original art, editable art models and visual review history in `art/` and `concept_art/` stay local, along with worlds, authentication, machine-local configuration, downloaded dependencies and build caches. Documentation links to those local art sources require the local artwork folders. Full source archives use the separate [collaboration handoff](docs/collaboration.md); their artwork-containing baseline must not enter the public repository.
