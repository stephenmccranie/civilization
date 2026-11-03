# Development checks

Run the smallest check that covers the change, once. Repeat a passing check only after a relevant code change or to diagnose a failure. Documentation and local configuration changes need no Minecraft launch.

| Change | Check (from `civilization-mod/`) |
| --- | --- |
| Documentation or paths | `python tools/check_docs.py` from the repository root |
| Routine code, text or registration | `.\dev.ps1 Build`: compile and unit tests |
| Recipes, inventory, placement, persistence, production, calories, networking | `.\dev.ps1 GameTest`: every server GameTest |
| Rendering, shaders, HUD, textures, client input | `.\dev.ps1 Visual -Scene <scene>` |
| Vehicle state across a server restart | `.\dev.ps1 Fixture -Name boat` or `-Name airship` |
| Heat solver | `.\dev.ps1 Fixture -Name thermal`, and `-Phase physics` for the reference physics checks |
| Client and server together | `GameTest` plus the relevant scene |

## How runs work

Every run starts from a clean directory that `dev.ps1` rebuilds: `runs/gametest`, `runs/visual`, `runs/peer` and `runs/fixture`. Two directories persist: `runs/server` is the development server world, and `run/` is the development client. Visual runs copy that client's `saves/preview-compatibility` world, mods, shader and resource packs, `options.txt` and Iris settings, so install the optional client mods (Photon, Distant Horizons, JEI) there.

GameTest and visual runs pass on a zero exit code; a check fails by throwing. Each fixture phase must also print `<NAME>_<PHASE>_PASS`. Fixtures copy the EULA acceptance from `runs/server`, so run `.\dev.ps1 Server` once first.

Visual clients are hidden, unfocused, 1920x1080 and cannot grab the mouse (`VisualTestGuard` and `civilization.visual.mixins.json`). They never use the owner's Prism instance. Screenshots land in `runs/visual/screenshots` (and `runs/peer/screenshots`); look at them whenever appearance changes. `-Full` extends a scene with its optional night, outline or placement tour.

GameTest templates must contain their whole fixture: the suite places tests in a randomized grid, so a fixture that writes outside its declared bounds breaks its neighbours. Multiblock guide changes keep `GuidePerformanceTest` coverage.

## Visual scenes

| Scene | Shows |
| --- | --- |
| `machines` | Kiln, Fertilizer Furnace and Cooking Stove controllers and shells, and cut-block placement previews |
| `textures` | Item and controller display |
| `guide` | Textured multiblock build guide, including a wrong block and its removal |
| `placement` | Held Kiln, Smithy, Oil Derrick, Assembly Bench and Baking Oven previews facing three ways, mostly from the offhand as survival carries them: a refused bench, an obstructed shell and a controller held up to a finished shell. Each real right-click must place exactly the previewed cells, or nothing |
| `modular` | Mixed-material and irregular cut-block assemblies |
| `welcome` | The join welcome card with the pack's Auto GUI scale and chat settings, at 1080p and 1440p (closed and open chat) |
| `material-sync` | Delayed material packets: received identities and before/after rendering |
| `machine-lighting` | Fueled and cold machines in daylight and at night |
| `thermal-art` | Cold and lit Kiln, Foundry, Cooking Stove, Fertilizer Furnace and Smithy, and their icons |
| `thermal` | Survey helmet temperature cells and readout, comfort HUD and inspection tooltip |
| `workshops` | Tannery, Textile Workshop and Smithy, with manual ignition, and the recipe drawer's filters, search, scrollbar drag and track clicks |
| `assembly` | Assembly Bench forging and assembly through the real menu |
| `storage` | Paired material, fuel and output banks in machine menus |
| `bulk` | Coal Bunker and Cargo Tank construction, fill states and menu withdrawal |
| `pipes` | Empty and flowing glass pipes, connections and flanges |
| `industry` | Refinery, Oil Derrick, construction cabinet and industrial icons |
| `derrick-guide` | Oil Derrick section guides placed by real clicks, day and night |
| `engine` | Oil Engine states, cabinet and wheel animation |
| `models` | GeckoLib model integration beside the Oil Engine |
| `deposits` | Generated coal and oil bodies across chunk borders, before and after extraction |
| `uranium` | Exposed ore, stocked chest and held Raw Uranium |
| `coal-mining` | Manual coal workface, partial masks and standing inside a mined notch |
| `builders-line` | Builder's Line clicks, automatic building and mining, and cancel |
| `wheelbarrow` | Wheelbarrow first-person view, walking bob, A/D steering with a free mouse look, a hill climb and descent, the hitbox and walking around the wheel's point, loading and rendering |
| `backpack` | Cooking Stoves, a chest and stairs on a Pallet, a backpack in its accessory slot, binding it in a Pallet menu, building that tops up the hand and pack from the store with stone conserved, the pack worn from behind, three-quarters and the side, its menu and Supply switch, building beyond reach from the pack alone, the held item, and the Pack panel beside a chest menu |
| `pallet` | Pallet stacks at each fill level up to two blocks high, one under a block, mixed goods and pieces, an overloaded pre-limit pallet, a 3x3 yard, right-click stacking, shared menus and loaded wheelbarrows |
| `road` | Street Paver surface variants and forms |
| `grassier-grass` | Grass at low and high altitude with Grassier Grass installed |
| `crafting` | Crafting table output, ingredient use, recipe book and saved shared inputs |
| `chests` | Single and double chest menus with shift-click conservation |
| `inventory` | Player inventory, equipment, offhand, pickup and recipe book |
| `jei` | JEI categories, catalysts and recipe parity (needs JEI in `run/mods`) |
| `civic` | Land controller and trade counter menus, whitelist and quantity input |
| `kitchen` | Prototype stove, food doneness, rotary menu input and serving |
| `oven` | Baking Oven placement and animated door |
| `canisters` | All four canisters in frames, inventory and hand |
| `cloth`, `sulfur`, `parts`, `supplies`, `foods`, `manufactured`, `components` | Item art in frames, inventory slots and hand |
| `paterson` | Paterson revolver input, reload, hammer and recoil |
| `boat` | Freeform boat placement, gears, steering and throttle |
| `airship` | Airship assembly, flight controls and rider retention |
| `weather` | Two clients: local rain beside a dry neighbouring district |
| `gladiator` | Two clients: arena menus, stakes, knockout and winnings |

`weather` and `gladiator` start a second hidden client in `runs/peer` that joins once the host writes `runs/peer/ready.txt`. An unknown scene name fails the run.

## Fixtures

A fixture runs its phases as separate server processes on one fresh flat world in `runs/fixture`.

- `boat`: `seed` builds and fuels a loaded boat, then `reload` confirms its engine fuel, oil, condition and cargo after a restart.
- `airship`: `seed` assembles a loaded airship, then `reload` confirms its power and cargo after a restart.
- `thermal`: the [thermal laboratory](heat.md). `check` records heating and cooling at reference stations into `measurements.csv`, and `reload` confirms the field survives a restart. `-Phase physics` runs the reference physics checks. `-Phase manual` opens the laboratory as an interactive server on `localhost:25567` with `/thermal_lab` commands.
