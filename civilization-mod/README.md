# Civilization — implemented build

[Runtime versions](gradle.properties)

This folder holds the working mod. [Game rules](../design_direction.md) describe the intended game; the [development plan](../development_plan.md) orders future work. The references below describe shipped behavior.

## Reference

- [Survival](survival.md): day length, calories, movement, food, inventory limits, interface and the energy journal.
- [Machines](machines.md): construction guides, cabinet interface, coal fires and coal budget, production XP, JEI, Kiln and Fertilizer Furnace.
- [Manufacturing](manufacturing.md): Foundry, town workshops, Assembly Bench, parts, equipment grades and repairs.
- [Industry](industry.md): coal and oil deposits, the refinery, liquids and pipes, Hot-Bulb Engine and bulk storage.
- [Coal mining](coal-mining.md): hand mining of huge coal veins.
- [Cutting](cutting.md): saws, slabs, beams and eighth cubes, and the Builder's Line.
- [Cooking](cooking.md): Cooking Stove, Prototype Stove and Baking Oven.
- [Heat](heat.md): climate, the thermal field, comfort and cold, and the Thermal Survey Helmet.
- [Geography and weather](geography.md): farmland and woodland, river fish, tree felling, regional weather, crops and frontier uranium.
- [Physical materials and cargo](physics.md): materials, inspectors, wheelbarrow, pallets, block carrying and the side column, the backpack, nearby stores and shared crafting and enchanting tables.
- [Vehicles](vehicle-physics.md): boats, the airship prototype and fast-flight integrations.
- [Ownership and trade](ownership-trade.md): land claims, takeovers, whitelists, Trade Counters and the Survey Table.
- [Gladiator Pit](gladiator-pit.md): the arena, knockout fights and wagers.
- [Firearms](firearms.md): the Colt Paterson.
- [Development checks](testing.md): which checks a change needs.

## Optional mods

Civilization runs without these. Each version-gated integration activates only for the verified version and its expected hook signatures; any other version keeps its own behavior and Civilization logs a warning.

| Mod | Verified version | Side | Enables |
| --- | --- | --- | --- |
| Just Enough Items | Pinned in `gradle.properties` | Client | [Machine recipe categories](machines.md#jei) and [crafting placement](physics.md#personal-block-carrying) |
| Grassier Grass | 1.4.5 | Client | [Trampled grass and no snow tint](survival.md#trampled-grass) |
| Distant Horizons | 3.3.1 | Client | [Speed-adaptive LOD detail](vehicle-physics.md#distant-horizons-detail) |
| Sodium, with DH 3.3.1 | 0.8.13 | Client | [Warp terrain suppression](vehicle-physics.md#warp-terrain-rendering) |
| Iris | 1.8.14 | Client | [Machine water shaded as water](manufacturing.md#town-workshops) |
| Iris, with DH 3.3.1 | 1.8.14 | Client | Warp bypass of the DH fade in shader programs |
| C2ME (`c2me_notickvd` module) | 0.4.0-alpha.0.122+1.21.1 | Server | [Directional chunk loading](vehicle-physics.md#directional-chunk-loading) |
| Terrain Diffusion | The owner's fork with its surface-water and climate APIs (`TerrainWater`, `TerrainClimate`); a build without them stops Civilization at startup | Server | [Local river elevations](geography.md#generator-integration), [air](heat.md#climate) |

Photon is the primary shader target; Bliss and Complementary are secondary checks.

## Development

### Server connections

Clients and servers need compatible Civilization protocols, not identical builds. The required core channel uses the protocol ID `15/0.35.32-dev`, the identity advertised by the 0.35.32 client, whose Fired Heater, Fluid Pipe and Small Boat Helm blocks must match the server's. Server-only generation, balance and settings changes keep that ID, so existing clients connect without an update. Change it only when packet formats or synchronized content become incompatible; NeoForge then rejects older protocols before play. Client-side fixes still reach players only through a client update. The pack manifest's own `protocol` field is the updater's format and stays `3/0.35.0-dev`, so installed updaters keep accepting new releases.

This is a compatibility gate, not authentication or anti-cheat: a modified client can imitate the channel. Account authentication and server access controls are separate. Players install the [Prism playtest client](../modpack/README.md); the host supplies the server address privately.

### Setup

Install JDK 21 and a Prism instance named Civilization Dev, then copy `dev.local.example.json` to the ignored `dev.local.json` and set `javaHome` and `prismInstance`.

Gradle resolves GeckoLib (required at runtime), and JEI, Iris and the LambDynamicLights API (optional and compile-only; never bundle them). `dev.ps1` downloads LambDynamicLights as the [player pack](../modpack/client-files.json) pins it into `.dev-libs` and adds it to test clients and Civilization Dev. `dev.ps1` downloads the pinned Sable release into the ignored `.dev-libs` folder: its version is in `gradle.properties` and its SHA-512 is pinned in `dev.ps1`.

`run/` is the development client. Visual checks copy its world, mods, shader and resource packs and options, so optional client mods for those checks (JEI, Iris with Photon, Distant Horizons) go in `run/mods`. Blockbench and its MCP server are authoring tools only: see the [modeling workflow](../tools/modeling/README.md).

### Commands

Run from this folder:

```powershell
.\dev.ps1 Build                                    # compile and unit tests
.\dev.ps1 GameTest                                 # every server GameTest, fresh world
.\dev.ps1 Visual -Scene machines [-Full]           # hidden client scene
.\dev.ps1 Perf                                     # performance benchmark in the player pack
.\dev.ps1 Fixture -Name boat|airship|coal|thermal [-Phase physics|manual]
.\dev.ps1 Deploy                                   # build and install into Civilization Dev
.\dev.ps1 Client                                   # development client in run/
.\dev.ps1 Server                                   # development server on localhost:25566
```

[Development checks](testing.md) says which check a change needs and lists every scene and fixture.

Deploy refuses while the Civilization Dev game is running or when Prism's Minecraft or NeoForge version differs from `gradle.properties`. It installs the resolved GeckoLib, Sable and LambDynamicLights jars, refusing a conflicting GeckoLib version, moves earlier Civilization jars to the instance's `mod-backups` folder and installs the new jar. Other mods, including JEI, are left untouched.

Server keeps its world in `runs/server` and copies `server.local.example.properties` on first start. Run it in an interactive terminal and type `stop` to shut down.

## Source map

| Location | Responsibility |
| --- | --- |
| `src/main/java/dev/civilization/` | Registration, server rules, calories, farming, machines, cutting, clock setup and journals |
| `src/main/java/dev/civilization/client/` | HUD, menus, previews and baked rendering |
| `src/main/java/dev/civilization/mixin/` | Narrow vanilla integrations; native APIs take priority where available |
| `src/main/resources/` | Assets, recipes, language, data and mixin declarations |
| `src/gameTest/` | GameTests, hidden visual scenes and server fixtures; never in the shipped jar |
| `src/test/` | Unit tests |
