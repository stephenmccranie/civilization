# Development checks

Use the smallest check that covers the changed behavior. Do not run the whole battery after every edit, and do not repeat successful checks without a relevant code change or unresolved failure.

| Change | Check |
| --- | --- |
| Documentation or local configuration | Inspect the edit; no Minecraft launch |
| Routine code, text, or item registration | `./dev.ps1 Verify` (build + fast unit tests) |
| Recipes, inventory, placement, persistence, production, calories | `./dev.ps1 Verify -Scope Gameplay` |
| Rendering, shaders, HUD, textures | `./dev.ps1 Verify -Scope Visual -Scene <scene>` |
| Changes spanning client and server, or release checkpoint | `./dev.ps1 Verify -Scope Full -Scene <scene>` |

Verification runs its Gradle tasks in one invocation. Gradle reuses unchanged compilation and unit-test results. Deployment also uses this incremental build and verifies the installed JAR checksum; there is no need for a separate Build or duplicate installed-hash check after a successful Deploy.

The two September 28 gameplay checks each took about **66 seconds** overall. In the 230-test run, the server reported tests starting at 02:46:05 and all passing at 02:46:51; its first batch took about 29 seconds and included a 28.5-second server-overload warning. Long-running thermal simulations are a likely contributor, but individual test costs have not been profiled. Keep the prescribed gameplay check for recipe and production changes; use Quick for routine code and avoid repeating a successful check without another relevant edit. The script reports actual Gradle elapsed time, which can vary by cache state and machine load.

For a narrow change, inspect the owning code and focused reference, keep searches and command output bounded, make one coherent edit, run its check once, then deploy once if the user needs the build in Prism. Update only affected documents and use `tools/check_docs.py` after documentation changes. Profile slow tests before shortening any simulation or reducing coverage.

## Focused visual checks

`models` is a development-only GeckoLib integration scene beside the Oil Engine. First create and publish the native editor fixture using the [model toolset](../tools/modeling/README.md). The fixture source and native exports are now included, with successful editor round-trip and Photon motion-frame review. The ordinary `engine` scene continues to test the production renderer; success there only verifies that the added dependency coexists with the current graphics setup.

`./dev.ps1 Visual -Scene machines` is the short default: assembled machines in daylight, one screenshot, then exit. The previous tour also waited through night, outline mode, and several piece previews. Request that only when those modes are relevant:

```powershell
./dev.ps1 Visual -Scene machines -FullVisual
```

The engine scene also switches between held controller, cylinder and flywheel on an east-facing incomplete build, checking that guide geometry stays fixed. Server placement coverage uses real item placement from all four approach directions for all four controller facings.

The `canisters` scene is a short art-only check of all four canisters in item frames and inventory, plus fuel/lubricant held views. It does not rerun fluid-transfer gameplay coverage.

Other scenes:

- `derrick-guide`: a completely unbuilt Oil Derrick blueprint in a focused Photon/Faithful view. Captures base and middle views, confirms all 671 missing pieces reached the client, and logs FPS with the guide hidden, outline-only, and textured from a fixed mid-height camera. FPS is a local comparison, not a server-scale benchmark.
- `uranium`: exposed ore, a chest stocked with Raw Uranium and Raw Uranium in the first-person hand under Photon/Faithful. The short scene captures several moments because streak launches are intentionally irregular; one frame can be quiet. It checks visual origin and client chest synchronization, not long-session animation cadence or multiplayer load.
- `bulk`: land Coal Bunker and Cargo Tank assembled, filled and empty views, synchronized 64,000 mB menu, real coal withdrawal, and incomplete construction guide under Photon/Faithful. Server checks cover rotations, capacity, canister conservation, fluid mixing, persistence, incomplete shells, cached capabilities, mining policy, actual hopper insertion/extraction, pipe budgets and claim revocation.

- `airship`: numeric controller input, freeform assembly, actual client flight/climb/turn controls, pilot retention across late heartbeat/rider separation, physical rider attachment during high-speed acceleration/turns and release on Shift, gradual thrust/reversal momentum, extended hover braking and settled unpiloted position hold in wind, extreme/zero-power behavior and cargo under Photon, followed by the real Disassemble button, cargo preservation and reopening the editable controller. With DH 3.3.1 installed in the disposable visual runtime, also checks actual transformed fixed near/far detail levels, warp activation, quality recovery, warp terrain draw/mesh suppression and restoration, near-clip adjustment, Photon fade override/recovery with a downward warp screenshot, and absence of tree-transition errors; without DH, checks optional-integration absence. With the pinned C2ME installed in the disposable runtime, the flight also requires actual source iterators to use directional ordering; unit coverage verifies all 16 headings, full square coverage, reversal, bounded fallback, independent cursors and world isolation. `./airship-check.ps1` separately checks exact power, owner, hull/cargo and vessel identity across two dedicated server processes.

- `storage`: short synchronized-menu check and screenshots of paired material/fuel/output banks in a Kiln and Textile Workshop. Server tests cover spillover, whole batches split across partial outputs without moving existing stacks, 32-item limits, save/load, legacy overflow, hopper access and the Smithy's single output pausing until collected.

- `jei`: requires the pinned optional JEI jar in `run/mods`. Checks all 15 processing categories and station catalysts, iron output/raw-iron uses, Foundry recipe parity, absence of retired iron smelting, and cut-piece subtype isolation. Captures actual Foundry, column, Smithy, canister and cutting pages. The server suite runs without JEI, verifying optional linkage.

- `pipes`: short patterned-wall comparison of empty/flowing octagonal glass tubes, telemetry expiry, vertical/corner connections, flange fit and item appearance under Photon.

- `thermal-art`: cold/lit Kiln, Foundry, Cooking Stove, Fertilizer Furnace and Smithy assembled views, rear material continuity and native inventory icons under Photon/Faithful.
- `road`: a broad Street Paver surface with independent texture variants, nearby ordinary red bricks, and slab/stair forms under Photon/Faithful; captures wide and close views.

- `industry`: connected refinery, casing/cooling surfaces, the wooden Oil Derrick front/rear and its small moving crosshead, derrick guide, column guide and all six industrial controller faces in world and inventory. Coal fixtures explicitly start lit; ignition behavior remains covered separately.

- `manufactured`: Steel Ingot and Prospecting Rod beside Machine Parts and vanilla iron/copper, including the held rod.

- `foods`: six kitchen food/preparation icons beside Cloth and fertilizer in native inventory slots and item frames under Photon.

- `supplies`: frontier resource family in seven item frames and inventory slots: Raw Hide, Cloth, both mineral blends, fertilizer, sulfur and Machine Parts.

- `cloth` / `sulfur` / `parts`: shared short art-only item-frame, native inventory and held-item review under Photon. Cloth is compared with wool/leather/Coal; sulfur with Coal/gold/Cloth; Machine Parts with steel/sulfur/Cloth.

- `engine`: standalone Oil Engine stopped/startup/running views, rear burner/exhaust, synchronized fuel/oil cabinet with fitted part icons, whole-wheel animation frames and incomplete build guide under Photon/Faithful. The disposable scene clears local regional rain and old overhead test blocks for reproducible art review.

- `crafting`: native crafting-table 3Ã—3 recipe, real output pickup and exact ingredient consumption, plus recipe-book open/closed positioning under Photon/Faithful.

- `chests`: native single/double chest menus, synchronized first/last slots and shift-click conservation, with the shared cabinet skin under Photon/Faithful.

- `inventory`: normal player inventory with equipment/offhand and an active effect, native item pickup/crafting/output custody, and the recipe book open/closed. Uses the shared cabinet skin in a hidden Photon/Faithful client.

- `workshops`: dedicated front captures for the partial-block Tannery and Textile Workshop, the complete three-workshop lineup, manual ignition rest/strike/lit views and real button interaction, synchronized menus, repaired grade and recipe ghosts under Photon/Faithful, plus real ghost-click custody, Automatic selection, focused search-key handling and filtered recipe selection. Server checks cover every catalog job, productive coal, grade-based repair wear, enchantment and per-item seed retention, upgrade grade/damage/seed, saves, blocked/broken machines, all four structure rotations, repair-menu boundaries, acquisition gates, mob equipment custody and controller stock drops. The `inventory` scene includes two same-grade items with different saved wear seeds and under-/over-100% equipment for black/gray/green/cyan inline bar and icon-overlay review.

- `weather`: local rain and a dry neighboring district under Photon. Use `./weather-check.ps1` to coordinate two hidden clients automatically (disposable LAN port 25569, test accounts, isolated saves). It copies the existing visual runtime graphics setup, resets the peer receipt, waits for the host port, checks independent wet/dry snapshots and sky strength, and saves screenshots in `runs/visual/screenshots` and `runs/weather-peer/screenshots`. The standalone weather scene expects that second client. Server checks accelerate farm time rather than waiting two hours. Only these disposable test accounts use an unauthenticated LAN connection; production configuration is untouched.

- `boat`: real client freeform placement beyond the former hull band, keyboard gear selection, held gears, fast/reverse movement, steering, held throttle after exit, neutral braking, physical Hot-Bulb Engine fuel/oil consumption and chest cargo under Photon. Old fixture vessels are cleared only within this disposable scene before its basin is rebuilt. `./boat-check.ps1` separately verifies cargo and physical engine state across two server processes.

- `thermal-art`: cold/lit Kiln, Foundry, Cooking Stove, Fertilizer Furnace and Smithy assembled views, rear material continuity and native inventory icons under Photon/Faithful.

- `industry`: connected heater/column/condenser delivery into a fuel tank, synchronized gauges/canister icons, custom-textured clipped-corner vessels, maintenance deck, grille/flue geometry and contextual construction guide, followed by the working Foundry menu and brick structure.
- `deposits`: freshly generated physical coal/oil bodies across chunk boundaries, exact machine output against removed blocks/fluid volume, and Photon/Faithful before/after views of excavated coal and lowered oil in a disposable cutaway.

- `civic`: synchronized land-controller and trade-counter menus, whitelist panel, typed quantity submission, real mouse interaction with a ghost selector, the assembled Survey Table with public marker inspection, in-world rendering and an incomplete construction guide, and the civic block texture display.

- `machine-lighting`: real fueled/cold kiln, Fertilizer Furnace and Cooking Stove comparisons in daylight and at night.

- `modular`: mixed-material and irregular cut-block assemblies.
- `material-sync`: delayed material packets, checking received identities and before/after rendering.
- `guide`: textured multiblock guide.
- `textures`: item/controller display.

Keep the hidden 1920x1080 window, mouse-capture guard, and three-minute timeout. `dev.ps1 -Task Visual` disables Distant Horizons auto-update only in the disposable visual client: its update prompt otherwise blocks the title screen and can look like a Photon shader stall. Photon remains enabled from `run/config/iris.properties`; check the log for `Using shaderpack` and review the screenshot. A successful process exit alone does not prove the image is correct. Usually one post-fix visual run is sufficient; reproduce a pre-fix image only when needed to establish the bug.

Use the longer visual tour independently of scope: `-Scope Full` selects server plus visual checks, while `-FullVisual` selects the extended scene sequence. Modular and material-sync scenes already have focused sequences and do not add an extended tour.

The `kitchen` scene reviews the prototype stove beside the existing Cooking Stove, pale/golden/charred food, a running fire, actual rotary menu input reaching the server, serving and inventory appearance, plus actual crouch lift, countertop placement, carried food, return and resting serve. The player is explicitly Creative with flight disabled for these physical interactions. Server-task failures propagate to the render thread before a successful receipt. It clears dropped items only inside its disposable fixture area so repeat reviews do not obscure the cookware. Server stove checks cover fuel/batch conservation, persistence, input validation and meal-benefit accounting, plus paid carryover, early recovery, real Creative placement custody, resting save/serve/lift, support-loss drops and legacy migration.

## Documentation checks

From the project root, run `python tools/check_docs.py` after documentation/path changes. It checks local Markdown link targets and the current status version against Gradle. Frozen archives are excluded; no Minecraft launch is needed. Current test counts live in [project status](../docs/status.md).

## Optional Sable laboratory

Use `./sable.ps1 Verify` for its dedicated movement/save/restart check and `./sable.ps1 Visual [-Graphics]` for baseline or installed graphics compatibility. These commands are separate from routine verification and never add Sable to production. Setup, working API examples and limitations: [vehicle physics laboratory](vehicle-physics.md).

## User testing campus

**Civilization Compact Campus** is the current separate superflat save in Civilization Dev. Spawn is **0, 64, 4**, Creative with cheats. Six labeled wall buttons control Creative/Survival, calories and day/night; command blocks are concealed inside the wall. The earlier Civilization Test Campus and player-built boats are preserved in their existing save.

The compact layout has stocked workshops, the three-stage refinery, civic blocks/map, farming, a nearby timber grove, assembly bays, all custom items and a smaller deep basin with two user-owned boats (fueled/empty). Coal and oil are **on campus**, with real finite bodies, glass viewing galleries and ladders. Its original compact Oil Pump and pipe connection predate the wooden Oil Derrick; rebuild the derrick and reconnect its ground-level output port before using that line. Empty full outputs and replenish fuel normally. Keep Inventory is on, natural monster spawning is off, and the normal day/night cycle remains active.

The saved map uses two explicit deposit geometries in vanilla command storage (`civilization:authored_deposits`, list keyed by dimension). The production lookup accepts up to 64 entries per dimension with x/y/z, kind (0 coal, 1 oil), and radius 8â€“12; absent entries leave natural generation unchanged. These define where machines search, never resource quantities: extraction still removes actual coal blocks/oil heights. A world-local datapack adds the flat river biome to woodland regions so the nearby grove grows normally. This testing override applies throughout this save; it does not change other worlds or production geography rules.

[CompactCampus.java](src/gameTest/java/dev/civilization/CompactCampus.java) generates the layout using shared [campus helpers](src/gameTest/java/dev/civilization/TestCampusBuilder.java). From the project root, `python tools/prepare_compact_campus.py` prepares a fresh `runs/test-campus/compact-campus`; then run `./gradlew.bat runCampusServer -PcampusCompact=true` from the mod directory. Preparation refuses an existing completed world. The configured fixture owner is the user's account UUID. Development classes are excluded from the production JAR.

`runCampusClient` reviews a separate copy named `compact-campus` under `runs/campus-preview/saves`, with hidden-window/mouse guards. It checks persisted local sites, farm/woodland eligibility, complete structures, wall controls, extraction and refinery output after reload, plus screenshots. The clean source remains in ignored `runs/test-campus/compact-campus`; install it only into a new save folder after the source server stops. Preserve `sublevels`, world data/command storage and the bundled datapack. No development-only classes are needed to play the installed save.

The `thermal` visual scene equips/removes the survey helmet, moves one block to inspect world-grid anchoring, and captures depth-tested temperature cells and the readout, then survival comfort/road HUD, inventory inspection tooltip and cold/off-road appearance. It also holds cold, comfortable and hot felt-temperature samples long enough to capture the smooth whole-frame color wash and its effect over the HUD. Thermal GameTests cover insulating walls, open doors/glass, bounded comfort/efficiency, finite distributed source emissions, front/rear Foundry heating, enclosed-air mixing/conservation, material differences, climate-relative save migration and unloaded-cell behavior.

## Controlled fast-flight benchmark

Use [flight-benchmark.ps1](flight-benchmark.ps1) for repeatable streaming/airship experiments. It never opens a user save or the immutable baseline for gameplay. Runtime data stays under ignored `runs/flight-benchmark/` and the benchmark classes live only in `src/gameTest`; no benchmark controls ship in the release mod.

```powershell
./flight-benchmark.ps1 -Prepare
./flight-benchmark.ps1 -Mode stream -Speeds 200,1000,5000 -Repeats 2 -Label baseline
./flight-benchmark.ps1 -Mode airship -Speeds 200,5000 -Repeats 2 -Label baseline
./flight-benchmark.ps1 -Mode stream -Speeds 1000 -Order spiral -Repeats 2 -Label spiral
```

`-Python` can select an explicit Python executable. Defaults are 10 seconds stationary settling, 5 seconds moving warmup and 20 seconds measured flight per fresh JVM. `-Warmup` and `-Seconds` adjust the latter two; the runner rejects routes exceeding 250,000 blocks or targets above 5,000 blocks/s. Repeat with reversed A/B order when evaluating a change. Do not run competing game/profiling workloads concurrently. The source, mod and configuration fingerprints in each manifest must match across an intended comparison except for the chosen independent variable.

### World and isolation

The baseline is structure-free, feature-free plains superflat, seed 192506, grass surface at y=63 and flight altitude near y=128. Its useful eastbound route is 262,144 blocks long, with 1,024-block end margins and 2,048-block width. Minecraft creates a fully generated, lit reference chunk; [corridor.py](../tools/flight-benchmark/corridor.py) duplicates the identical flat sections, biomes, heightmaps and lighting while rewriting absolute chunk coordinates and resetting time/history fields. It validates empty structures/block entities/tick queues and FULL/light status, atomically writes whole Anvil regions, and reads back both corner chunks of every region. A specification marker prevents resuming with a different template. The baseline is never force-loaded as a whole.

[lod_cache.py](../tools/flight-benchmark/lod_cache.py) also builds a flat DH 3.3.1 database from a game-produced uniform section. It validates every decoded interior/edge column, full generation flags, block/biome mapping, template identity and pinned DH artifact hash before replicating SQL positions at each detail level. A uniform flat column is unchanged by horizontal downsampling. LOD coverage extends 4,096 blocks to either side and beyond the route; SQLite integrity and database copy hashes are checked. This provides renderable LOD coverage, but the fixture has no `ChunkHash` entries. DH still checks loaded chunks and can rebuild their LOD data on first traversal; this is not a fully warmed DH cache benchmark. Run manifests record FullData/ChunkHash row counts before launch and `dh-cache-after.json` records them after shutdown. Nonzero hash counts alone do not establish matches with the current world. The main client's DH generator setting is preserved. Rebuild/review the fixture if DH or the flat terrain changes.

Streaming trials copy the required route prefix plus margins into their own named saves. Physical-airship trials copy the entire baseline because the numeric-power estimate can produce speeds well above its nominal target. No region files are hardlinked to the baseline. A new JVM and save copy, including a prepared DH database, are used for each trial; operating-system disk cache and global shader/mod caches are not forcibly cleared. This is a **pregenerated loading/streaming** benchmark, not a fresh-world-generation benchmark. Count newly generated chunks and reject a supposedly pregenerated measurement if that count is nonzero. The empty flat landscape provides controlled loading but cannot predict forest/structure-heavy geometry costs.

The runner copies every enabled JAR from the main Prism client's `mods` directory, excluding the Civilization code under test and Sable/GeckoLib already provided by the development runtime. It verifies Sable/GeckoLib against the main-client artifact hashes. Disabled JARs remain disabled. Main-client configurations, shaders (including shader option files), resource packs and options are copied, preserving Lithium, Sodium, Iris, DH, map/HUD mods, frame cap, VSync and render/simulation distances. Only hidden/unfocused window behavior, fixed 1920Ã—1080 resolution, pause-on-focus-loss and tutorial state differ. Actual loaded mod versions, Java/VM arguments and input artifact hashes are written per run. The development JVM uses the configured project JDK and the main client's configured maximum heap, read from its instance/global Prism settings (4 GiB fallback when unset). `--heap-gib` provides an explicit experiment override. It does not inherit other Prism JVM flags or its Java installation. Earlier runs used a fixed 6 GiB heap; inspect each runtime/manifest instead of assuming equivalence. These runtime differences are recorded and should be held constant for A/B tests. No main-client config is edited.

### Modes and measurements

- **stream:** a spectator travels east by target speed / 20 per server tick. It exercises server/client chunk streaming, C2ME ordering, DH and shaders independently of airship collision/acceleration limits. The test publishes synthetic velocity hints for C2ME when enabled. This does not certify physical airship speed.
- **airship:** a fixed 5Ã—7 plank deck and controller use real assembly, piloting, thrust ramp, drag, mass, chunk safety checks and numeric power. Power is estimated from target speed and frontal area; actual speed is the result, not forced. No trajectory teleporting or velocity injection occurs after setup. A requested speed may be exceeded or not reached; use measured speed for comparisons. The production readiness governor remains active.
- **Order:** `directional` uses production ordering; `spiral` disables only the hint publication through a development-only mixin. Coverage, concurrency and physics remain the same. Existing jobs finish normally.

Each result directory contains `manifest.json`, `loaded-mods.json`, `runtime.json`, `ticks.csv`, `frames.csv`, `safety.csv`, `profile.jfr`, `console.log`, a screenshot and `summary.json`. Tick rows include actual wall-clock and game-time speed, body speed where applicable, tick work/intervals, emergency safety holds, gradual readiness-brake state, available forward distance and checked/ready snapshot counts, pilot attachment, normalized Java process CPU, host CPU, heap, loaded/generated chunk counts and requested sweep coverage. Frame rows include frame intervals/render duration, camera position/speed, warp state and actual client chunk-cache coverage (never `ClientLevel.hasChunk`, which is always true). The development-only safety observer records every real physics clearance result and classifies the same immutable snapshot used by production: boundary, missing chunks, height, invalid numerics or collision readiness. It neither cancels checks nor changes motion/loading. Collision-shortened steps count as accepted motion, not emergency holds.

CPU percent is process CPU time divided by wall time and logical processor count, sampled approximately every 250 ms; host utilization is separate. TPS comes from observed tick intervals. FPS comes from frame intervals, and 1% low FPS is the reciprocal of the mean slowest 1% of intervals. Summaries also report body-speed percentiles/range, tick-end hold episodes, long frame counts and per-physics-check rejection reasons. Readiness braking has separate tick-end sample prevalence and episode metrics; older captures without this field report unknown, not zero. Neither metric captures every substep. The legacy `hold_time_pct` field weights the tick-end hold flag by tick interval; it is sampled hold prevalence, not a precise measurement of stopped duration. Averages alone cannot establish smooth physical flight. Warmup and settling rows are retained but excluded from summaries. JFR profiling and telemetry are enabled consistently; their overhead is part of every measured trial. Shader VSync and the frame cap can limit reported FPS.

Run `python ../tools/flight-benchmark/report.py --label baseline` to produce linked HTML/JSON reports under the benchmark directory. Reports keep streaming and airship results distinct and retain per-repeat values. Unit checks: `python ../tools/flight-benchmark/test_corridor.py`; runtime validation requires a completed end-to-end trial with zero newly generated route chunks and inspection of the loaded-mod manifest and screenshot for actual terrain coverage. Current observations belong in [status](../docs/status.md), not in this protocol.

A focused obstruction check is available through `python ../tools/flight-benchmark/run.py --mode airship --speeds 5000 --warmup 10 --seconds 20 --obstacle 16384 --label wall`. It builds a one-block-thick bedrock wall across the route, spanning the world height, before timing. It fails if the controller crosses the wall or if the ship never approaches it at speed; `COLLISION_PASS.txt` records the result and approach peak. This is a collision regression check, not a cruising-performance sample.

For an isolated worker-contention experiment, the Python runner accepts `--dh-threads 2` (1-16; default 0 preserves the main configuration). It edits only the disposable client copy and records the explicit override plus actual configuration hashes in the manifest. Lower thread counts can leave DH terrain missing at high speed; inspect screenshots before accepting apparent speed/FPS gains. Do not mix these runs with unchanged-settings comparisons.


For a first/repeat DH-cache comparison, use `python ../tools/flight-benchmark/run.py --mode airship --speeds 5000 --warmup 10 --seconds 15 --repeats 1 --cache-pair --label dh-cache-pair`. The second trial starts a fresh JVM and fresh Minecraft baseline, retaining only the first completed trial's DH database through SQLite backup. Both fly east from the same start with the same power and timing; actual distances can differ under the readiness governor. `dh_cache_from` identifies the first trial in the second manifest. This isolates retained DH data from retained Minecraft chunk state/JIT/GPU meshes, but OS disk caches remain uncontrolled. Compare source/mod/config fingerprints, route overlap, host/process CPU, hash counts, JFR conversion work and screenshots before interpreting the result. A retained hash entry is not proof that DH accepts it as unchanged.


Airship benchmarks also write `chunk-latency.csv`, `chunk-blockers.csv` and `chunk-latency-limits.txt`, plus `light-batches.csv` for whole lighting-batch elapsed times. Development-only mixins observe corridor requests, updated NBT read completion, deserialization, lighting/spawn steps, C2ME FULL conversion/publication and first inclusion in the existing physics snapshot. They do not alter returned futures, tickets or readiness decisions. Blocker rows inspect missing cells just beyond the measured ready distance. Run `python ../tools/flight-benchmark/latency_report.py <result-directory>` for stage quantiles and blocker-state counts. Records are bounded to 131,072 coordinate traces and 20,000 blocker observations; check the limits file. Timestamps are first observations per coordinate, so reloads are not independently tracked; pre-existing loads, missing endpoints and tick-sampled snapshots must be accounted for. Different stage quantiles cannot be summed. These hooks are excluded from the production JAR and the detailed publication hooks target the pinned C2ME build.

The airship runner accepts `--turn-pulse` for two opposing 0.2-second steering inputs, beginning 2.0 and 2.4 seconds into measurement. Use a 1,000-block/s target for this bounded route check; inspect actual x/z displacement and generated-chunk count because the fixture is a finite-width corridor. Legacy eastbound coverage columns are not a curved-path clearance measurement; physics safety telemetry still uses actual velocity. Corridor request timestamps now record admitted tickets after the update, excluding predictions not yet admitted by the per-tick batch limit.
