# Development checks

Run the smallest check that covers the change, once. Repeat a passing check only after a relevant code change or to diagnose a failure. Documentation and local configuration changes need no Minecraft launch.

| Change | Check (from `civilization-mod/`) |
| --- | --- |
| Documentation or paths | `python tools/check_docs.py` from the repository root |
| Routine code, text or registration | `.\dev.ps1 Build`: compile and unit tests |
| Recipes, inventory, placement, persistence, production, calories, networking | `.\dev.ps1 GameTest`: every server GameTest |
| Rendering, shaders, HUD, textures, client input | `.\dev.ps1 Visual -Scene <scene>` |
| Vehicle state across a server restart | `.\dev.ps1 Fixture -Name boat` or `-Name airship` |
| Coal stock across a server restart or chunk unload | `.\dev.ps1 Fixture -Name coal` |
| Heat solver | `.\dev.ps1 Fixture -Name thermal`, and `-Phase physics` for the reference physics checks |
| Client and server together | `GameTest` plus the relevant scene |
| Rendering cost or frame pacing | `.\dev.ps1 Perf`, before and after the change ([performance](#performance)) |

## How runs work

Every run starts from a clean directory that `dev.ps1` rebuilds: `runs/gametest`, `runs/visual`, `runs/peer` and `runs/fixture`. Two directories persist: `runs/server` is the development server world, and `run/` is the development client. Visual runs copy that client's `saves/preview-compatibility` world, mods, shader and resource packs, `options.txt` and Iris settings, so install the optional client mods (Photon, Distant Horizons, JEI) there.

GameTest and visual runs pass on a zero exit code; a check fails by throwing. A GameTest server that fails to start also exits cleanly, so `GameTest` passes only when the server reports its required tests passed. Each fixture phase must also print `<NAME>_<PHASE>_PASS`. Fixtures copy the EULA acceptance from `runs/server`, so run `.\dev.ps1 Server` once first.

Visual clients are hidden, unfocused, silent, 1920x1080 and cannot grab the mouse; their sounds still play, so scenes can check them, but OpenAL output is muted (`VisualTestGuard` and `civilization.visual.mixins.json`). They never use the owner's Prism instance. Screenshots land in `runs/visual/screenshots` (and `runs/peer/screenshots`); look at them whenever appearance changes. `-Full` extends a scene with its optional night, outline or placement tour.

GameTest templates must contain their whole fixture: the suite places tests in a randomized grid, so a fixture that writes outside its declared bounds breaks its neighbours. Multiblock guide changes keep `GuidePerformanceTest` coverage.

## Visual scenes

| Scene | Shows |
| --- | --- |
| `machines` | Kiln, Fertilizer Furnace and Cooking Stove controllers and shells, and cut-block placement previews |
| `textures` | Item and controller display |
| `guide` | Textured multiblock build guide, including a wrong block and its removal |
| `placement` | Held Kiln, Fertilizer Furnace, Smithy, Oil Derrick, Assembly Bench and Baking Oven previews facing three ways, mostly from the offhand as survival carries them: a refused bench, an obstructed shell and a controller held up to a finished shell. Each real right-click must place exactly the previewed cells, or nothing |
| `modular` | Mixed-material and irregular cut-block assemblies |
| `welcome` | The join welcome card with the pack's Auto GUI scale and chat settings at 1080p, with GUI scale 2 there, in a 768p window and at 1440p (closed and open chat); fails if the chat text is not the expected size, a line would wrap or the card outgrows the closed chat |
| `material-sync` | Delayed material packets: received identities and before/after rendering |
| `machine-lighting` | Fueled and cold machines in daylight and at night |
| `thermal-art` | Cold and lit Kiln, Foundry, Cooking Stove, Fertilizer Furnace and Smithy, their icons, and the ground at the foot of a controller seen from above |
| `clouds` | Photon drawing the server's clouds: the ground darker when the server puts a cloud on the line to the sun than when it reports a gap, then partly cloudy and overcast skies; fails if either Photon file was not patched |
| `thermal` | Survey helmet temperature cells and readout, comfort HUD and inspection tooltip, and the cold, comfortable, hot and cool temperature screen effects |
| `workshops` | Tannery with a loaded pit, Textile Workshop, and Smithy with manual ignition, and the recipe drawer's filters, search, scrollbar drag and track clicks; then the steeping pit three seconds on, its bark heap close up, the loom being worked from its front left and the pit without shaders |
| `assembly` | Assembly Bench forging and assembly through the real menu |
| `storage` | Paired material, fuel and output banks in machine menus; rough metal glowing to cold in the hotbar beside the Stone Anvil, iron anvil and water cauldron; iron ingots through every glow colour in the hotbar, held white-hot and cherry red in first person and cherry red in third, lighting the floor around them, then the same night without shaders and dropped white-yellow, cherry red and dull red, by day and night |
| `bulk` | Coal Bunker and Cargo Tank construction, fill states and menu withdrawal |
| `pipes` | Empty and flowing glass pipes, connections and flanges |
| `industry` | Refinery, an Oil Derrick pumping with a Coal Engine driving it, construction cabinet, industrial icons, and a boat helm and guardrail run |
| `derrick-guide` | Oil Derrick section guides placed by real clicks, its beam working from a Coal Engine, day and night |
| `engine` | Hot-Bulb Engine stopped, heating with lit lamp ports and the blowlamp sounding, kicked over and running with the flywheel turning and the stack knocking, its menu and its section guide |
| `deposits` | Generated coal and oil bodies across chunk borders, before and after extraction |
| `uranium` | Exposed ore, stocked chest and held Raw Uranium |
| `coal-mining` | Manual coal workface, partial masks and standing inside a mined notch |
| `builders-line` | Builder's Line clicks, automatic building and mining, and cancel |
| `wheelbarrow` | Wheelbarrow first-person view, walking bob, A/D steering with a free mouse look, a hill climb and descent, the hitbox and walking around the wheel's point, loading and rendering; the side column beside a chest menu, and beside crafting and Kiln menus with their recipe panels open at 1080p, GUI scale 2, 768p and 1440p, standing in the same place; in Creative, the same beside a Kiln menu with its recipe drawer open and closed, the Hand taking a full stack by real clicks |
| `backpack` | Cooking Stoves, a chest and stairs on a Pallet, a backpack in its accessory slot, binding it in a Pallet menu, building that tops up the hand and pack from the store with stone conserved, the pack worn from behind, three-quarters and the side, the Pack panel in the inventory, building beyond reach from the pack alone, the held item, the Pack and Nearby panels beside a chest menu, taking a stack from Nearby and putting it back by real clicks, and unbinding with the Pack panel's button |
| `pallet` | Pallet stacks at each fill level up to two blocks high, one under a block, mixed goods and pieces, an overloaded pre-limit pallet, a 3x3 yard, right-click stacking, shared menus and loaded wheelbarrows |
| `road` | Street Paver surface variants and forms |
| `grassier-grass` | Grass at low and high altitude with Grassier Grass installed |
| `crafting` | Crafting table output, ingredient use, recipe book and saved shared inputs; a shift-click crafting six logs onto a nearby Pallet past a full Hand, with the action bar naming it; an enchanting table keeping its book and lapis across reopening |
| `chests` | Single and double chest menus with shift-click conservation |
| `inventory` | Player inventory, equipment, offhand, pickup and recipe book; graded ingots dealt by a real mouse drag in the order crossed, and stone from the Pack swapped into a Hand holding torches |
| `jei` | JEI categories, catalysts and recipe parity, and the **+** placing a crafting recipe through our own transfer handler (needs JEI in `run/mods`) |
| `civic` | Land controller and trade counter menus, whitelist and quantity input |
| `kitchen` | Prototype stove, food doneness, rotary menu input and serving |
| `oven` | Baking Oven placement and animated door |
| `canisters` | All four canisters in frames, inventory and hand |
| `cloth`, `sulfur`, `parts`, `supplies`, `foods`, `manufactured`, `components`, `tools`, `minerals`, `buckets` | Item art in frames, inventory slots and hand |
| `paterson` | Paterson revolver input, reload, hammer and recoil |
| `boat` | Freeform boat placement, gears, steering and throttle |
| `airship` | Airship assembly, flight controls and rider retention |
| `weather` | Two clients: local rain beside a dry neighbouring district |
| `gladiator` | Two clients: arena menus, stakes, knockout and winnings |

`weather` and `gladiator` start a second hidden client in `runs/peer` that joins once the host writes `runs/peer/ready.txt`. An unknown scene name fails the run.

## Performance

`.\dev.ps1 Perf` runs the benchmark in the player pack exactly as players get it: every mod, Patrix 32x, Photon on the pack's Medium settings and the pack's options, laid out from `modpack/client-files.json` and its verified download cache on the pack's NeoForge, with this checkout's Civilization build. The client is hidden at 2560x1440 with vsync off. Drivers throttle a hidden window's buffer swaps, so it presents nothing and instead keeps at most two frames in flight on the GPU, as a visible window's swap chain does. Distant Horizons draws its stored terrain at the pack's 128 chunks and quality settings, which the report lists, and generates none locally, as for players, whose distant terrain comes from the server.

The benchmark has one world, generated on the first run and kept in `.tools/perf/world`: a fixed seed, a town site on open plains, every chunk the walk loads, and Distant Horizons terrain built 136 chunks around the town by its own generator. Each run copies it and builds the town on a grass lawn: three yards of every custom model, the Oil Derrick, Kiln, Foundry and Fertilizer Furnace built and burning, the Baking Oven and Cooking Stove, loaded pallets and wheelbarrows. It waits until the world has finished loading (the threads that build chunk meshes, grass blades and Distant Horizons' terrain have gone quiet), then walks one route of about 100 blocks at walking pace in three legs (`approach` across the meadow, `yards` along the lane between the first two yards, `outskirts` out into the country). The first walk is not measured, because chunk meshes and grass blades are built as the route first brings them near; the second is uncapped and the third capped at the 120 fps target, each measured per leg:

- **Pacing:** frame rate, 1% and 0.1% lows, frame-time percentiles, the share of frames over the 8.33 ms budget, frames over 16.7 and 33 ms, and frame-to-frame jitter. The capped pass is the pacing players get.
- **Cost:** every profiler section of the frame, Minecraft's, Sodium's, Iris's (including its shadow pass, shader passes and final pass) and Distant Horizons', timed on the CPU and on the GPU with timestamp queries; each draw by render type and texture; each block entity and entity render by type on the CPU. Tables give self time per frame by section and summed by name.
- **Slowest frames:** the ten slowest uncapped frames with the sections that ran furthest over their usual time.
- **Java profile:** a flight recording of every thread during the uncapped walk, summed per leg by thread, by owning mod and by method, with garbage-collection pauses.

The report is `runs/perf/report/report.md`, with `report.json` for comparing runs, the recording (`walk.jfr`) and a screenshot at the start of each leg; each run's report and screenshots are also kept in `.tools/perf/<time>-<commit>`. The run fails if a chunk is generated after the town is built; delete `.tools/perf/world` after changing the seed, site or route so the next run generates the world again. Compare a change against a run of its base on the same quiet machine: other load on the desktop, a Minecraft client or another session, shifts the numbers.

## Fixtures

A fixture runs its phases as separate server processes on one fresh flat world in `runs/fixture`.

- `boat`: `seed` builds and fuels a loaded boat, then `reload` confirms its engine fuel, oil, condition and cargo after a restart.
- `airship`: `seed` assembles a loaded airship, then `reload` confirms its power and cargo after a restart.
- `coal`: `seed` leaves raw coal in a struck face, piles, a falling and a blocked batch, a cart, a screen and a stored shovel, stopping the server while a chunk is still falling and an undercut pile's fall is still scheduled; `reload` counts every unit after the restart and again after the chunk has unloaded and loaded.
- `thermal`: the [thermal laboratory](heat.md). `check` records heating and cooling at reference stations into `measurements.csv`, and `reload` confirms the field survives a restart. `-Phase physics` runs the reference physics checks. `-Phase manual` opens the laboratory as an interactive server on `localhost:25567` with `/thermal_lab` commands.
