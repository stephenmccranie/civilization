# Civilization

A Minecraft civilization game for a shared server. Personal labour grows into farming, industry, trade, concentrated power and extraordinary sky settlements, in an alternate history set in Nikola Tesla's lifetime. Food is fuel for people, coal, oil and uranium are fuel for industry and money for trade, goods move physically along rivers and roads, and land is held by powered claims that can change hands. Hierarchy and visible status are part of the competition.

The game is a NeoForge mod for Minecraft Java Edition.

## Play

The client is a self-updating Windows pack for [Prism Launcher](https://prismlauncher.org/download/), published on [Releases](https://github.com/stephenmccranie/civilization/releases). Installation, joining, settings and troubleshooting are in [modpack/README.md](modpack/README.md).

## Documentation

| Need | Document |
| --- | --- |
| The game's rules, setting and open questions | [design_direction.md](design_direction.md) |
| What is deployed, known limits and what is built next | [development_plan.md](development_plan.md) |
| Development setup, commands and the per-system mod docs | [civilization-mod/README.md](civilization-mod/README.md) |
| Which checks to run for a change | [civilization-mod/testing.md](civilization-mod/testing.md) |
| Player pack, client and server settings, releases | [modpack/README.md](modpack/README.md) |
| Modelling and art pipeline | [tools/modeling/README.md](tools/modeling/README.md) |
| Working rules for contributors and assistants | [CLAUDE.md](CLAUDE.md) |

## Layout

- `civilization-mod/` — the mod: source, resources, tests, `dev.ps1` and the system docs.
- `modpack/` — the public, download-only Prism pack manifest and reviewed client settings.
- `tools/` — maintenance, release, benchmark and art tools.
- `art/` and `concept_art/` — original art masters and concepts. They stay local and are not part of this repository; finished textures and models ship inside the mod.
