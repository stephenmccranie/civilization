# Civilization

A custom Minecraft civilization MMO: personal labor grows into industry, trade, concentrated power, and extraordinary sky settlements. Hierarchy and visible status are part of the competition. Target: up to 200 concurrent players, maintained by one human with assistant support.

## Start here

| Need | Document |
| --- | --- |
| What works today, build and validation | [Current status](docs/status.md) |
| Agreed vision and game rules | [Design](design_direction.md) |
| What gets built next | [Development plan](development_plan.md) |
| Choices still open | [Open decisions](open_decisions.md) |
| Recipes, controls and development commands | [Mod reference](civilization-mod/README.md) |
| All references and historical material | [Documentation index](docs/README.md) |

Java + NeoForge; Photon is the recommended shader. New Civilization artwork targets 64×64 pixels per standard block face/item, beginning with Street Pavers; existing 32×32 art will migrate by family. Faithful 32x remains the companion pack for vanilla assets. Exact runtime versions live in [gradle.properties](civilization-mod/gradle.properties). The concurrency target is a design goal, not verified capacity.

## Project layout

- `civilization-mod/src/`: production code/resources, unit tests, and isolated GameTest/visual fixtures.
- `civilization-mod/dev.ps1`: build, change-specific verification and Prism deployment.
- `art/textures/`: original masters, manifests, export tools, receipts and review sheets.
- `art/construction/`: selected in-game construction/renderer evidence.
- `concept_art/`: world/vibe references and prompts.
- `docs/history/`: old release notes and bounded playtest observations.
- `docs/archive/`: frozen document snapshots.
- `tools/`: small project-maintenance tools.
- Ignored local state: `.tools/`, Gradle/build caches, mod `run/` and `runs/`, and `dev.local.json`. These are not additional source trees.

Each rule has one home. Update the relevant reference with the code or decision; keep proposals separate from shipped behavior. [Contributor instructions](AGENTS.md) make this part of the normal workflow.

## Source control

The project is maintained in the private GitHub repository `stephenmccranie/civilization`. Continue development in this local checkout; completed, validated changes are committed and pushed under the [GitHub workflow](AGENTS.md#github-workflow). Original art, editable models, evidence, tools and documentation belong in the repository. Worlds, authentication, machine-local configuration, downloaded dependencies and build caches remain local.
