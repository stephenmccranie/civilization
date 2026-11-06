# Heat

Every loaded block cell has a temperature: the regional climate plus local thermal energy stored in the cell. Fires, lava and machines add energy; materials, openings and the open sky move it. Players feel the air at torso height together with the warmth or chill of what they can see, tempered by what they wear, which sets their comfort and cold calorie cost, and temperature changes how much work machine coal buys. Player-facing temperatures are Fahrenheit; simulation and saves use Celsius and game heat units.

## Switch

`config/civilization-heat.toml` holds `enabled = true` by default. It is a common config read by the authoritative server; restart after changing it. While disabled, source discovery, emission and diffusion stop and new machine waste heat is discarded rather than queued; saved heat and pending budgets stay intact. Comfort bonuses, cold drain, rain pauses on heated work faces and machine efficiency are neutral, the helmet survey is empty and `/civilization heat` reports that heat is disabled. Re-enabling resumes the saved field and rediscovers sources in loaded chunks.

## Climate

Overworld background temperature comes from biome, altitude, the 60-minute day and regional rain. Air reaches its daily low near dawn (time 0, about 06:00) and its high in mid-afternoon (time 9000, about 15:00), on two smooth half-cosine curves that keep each biome's daily high, low and mean; at the default day length the peak comes about 7.5 real minutes after noon. A clear river biome at ordinary elevation:

| In-game time | Outdoor temperature |
| --- | --- |
| Dawn, 06:00 | 44.6°F / 7°C |
| Noon, 12:00 | 58.1°F / 14.5°C |
| Afternoon peak, 15:00 | 62.6°F / 17°C |
| Sunset, 18:00 | 60.9°F / 16°C |
| Midnight, 00:00 | 50.8°F / 10.5°C |

Vanilla's biome temperature is a gameplay index, not an air temperature. For ordinary land it supplies a mean with a temperate daily swing of ±5°C; negative indices stay distinct (frozen peaks are colder than snowy plains). Biome families adjust that rule:

| Climate | Clear-weather daily high / dawn low at Y=64 |
| --- | --- |
| Forests, birch woods, cherry groves and meadows | Shared woodland mean with a small index distinction, ±5°C. Ordinary forest: 62.4°F / 44.4°F |
| Plains and sunflower plains | 12.2°C mean, ±5°C: 63.0°F / 45.0°F |
| River | 12°C mean, ±5°C: 62.6°F / 44.6°F. Frozen river keeps its cold profile |
| Other temperate land | Index-based mean, ±5°C |
| Desert and badlands | Hot index-based mean, ±9°C. Desert: 102.2°F / 69.8°F |
| Savanna | 25°C mean (windswept 24°C), ±7°C. Savanna: 89.6°F / 64.4°F |
| Non-snowy taiga | 12°C mean, ±5°C: 62.6°F / 44.6°F. Snowy taiga keeps its colder index |
| Jungle and swamp | Index-based mean plus 4°C, ±3°C. Jungle: 81°F / 70.2°F |
| Oceans and coasts | ±3°C. Means: frozen −8°C, cold 8°C, ordinary 16°C, lukewarm 23°C, warm 27°C; beaches and mushroom fields use their index mean; deep variants follow their surface profile |
| Caves | Lush 16°C, dripstone 14°C, deep dark 10°C, with no daily or rain change |
| Stony peaks | Index-based mean minus 7°C, then altitude cooling |

Rain moves the district baseline toward 6°C cooler over five real minutes and recovers over forty minutes after it stops; the response is 80% in dry and savanna profiles, 60% in humid ones, 50% on coasts and oceans, zero in caves and full elsewhere. Above Y=80 air cools 0.045°C per block. Unrecognized modded biomes use their own index and the temperate swing. Time commands and sleeping select the climate of the resulting world time.

## Thermal field

Each active cell stores energy relative to the current background and has a material heat capacity; its temperature is background + energy ÷ capacity. Sources, air, walls and chimneys share one block lattice — no room, duct or chimney is recognized. Partial blocks use their eight cut-piece materials for capacity, and the occupied pieces on each 2×2 face set how much air passes. Closed full blocks conduct without airflow; open doors, open trapdoors and empty space let air mix. Joined native double slabs behave as their full material; single slabs combine half material with half air. Placing or breaking a block changes that cell's capacity and faces. Material lookup goes through the [shared material policy](physics.md#materials); legacy heat capacities stay game units.

Air mixes with neighbors, with extra upward transfer when lower air is hotter, limited so uniform warm air never piles up artificially at a ceiling. Sky-exposed columns carry warm excess upward with matched inflow and outflow and an atmosphere boundary at world height, so an open shaft exhausts while an enclosed room keeps its warmth. Solid-to-solid conduction is slow and hot surfaces warm adjacent air faster than they conduct. Exposed air sheds about 1.8% of its excess to the atmosphere per simulated second. Tiny residuals are retired to an atmosphere ledger. Timber warms through sooner than cold brick but passes less heat at equal surface temperature.

### Sources

| Source | Heat |
| --- | --- |
| Coal in an industrial machine | 13,000 units per Coal, released as work draws it from the store (about 650 units/s at a Kiln's full draw) and as hot masonry leaks (about 1.7 units/s per block of full masonry); heat still being stored releases nothing yet |
| Coal in the Cooking Stove or Prototype Stove | 5% of industrial: 650 units per Coal (about 32.5 units/s at full draw) |
| Hot-Bulb Engine | From fuel actually consumed |
| Torch, soul torch | 6 units per loaded second |
| Campfire, soul campfire, fire, soul fire | 24 units per loaded second |
| Lava (natural or placed) | Maintained 1,000°C reservoir exchanging up to 48 units per exposed face per second, warming colder and absorbing from hotter neighbors |

Redstone torches give no heat. Extinguishing or removing a source ends emission while stored warmth decays. A machine releases 80% of its heat into the first air cell above each open top outlet and 20% evenly across its structural blocks, including the controller; blocked outlets keep their share in the body, so an unvented room gradually overheats. The release queue limits machine heat to 800 units per simulated second and keeps the remainder for later, so batch heat can trail completion.

### Solver

- A whole-field exchange runs every half second of game time in bounded batches under a **5 ms soft budget per server tick** and at most 65,536 operations per call; an overdue step can start immediately within that budget. Releases due in each interval are staged from a quarter-second timeline, so energy is never lost or doubled.
- At equal simulated time the field is identical however the work is sliced, including across a mid-step save. Overload delays the field rather than changing it; the helmet and `/civilization heat` report solver age.
- Per dimension: 262,144 active cells, 262,144 timed-release entries and 4,096 pending machine-source locations. Machine batches wait for release-queue room; natural inputs that do not fit, and weak settled cells, are retired to the atmosphere ledger.
- Heat reads only fully loaded chunks: unloaded chunks, and chunks still loading or generating, are neither loaded nor simulated, and heat never waits for one; their warmth and pending emissions wait for reload. Offline and heat-disabled time is not simulated. Flame and lava discovery is chunk-indexed with bounded queues.
- Exchanges use dense numeric arrays with section-local indices; block materials and face geometry are resolved once per exchange and nothing is cached across exchanges.
- Not modelled: wind, smoke, radiation between blocks (only bodies feel radiant warmth), oxygen, phase change, internal machine temperatures and moving vessels. Rates are deliberately coarse, not CFD or SI calibration.

The thermal laboratory is a server fixture with physics reference checks (energy conservation, conduction, door and roof airflow, source budgets, room warm-up and cool-down, vented and closed Foundries). Run it with `dev.ps1 Fixture -Name thermal` after changing the solver or thermal rules ([development checks](testing.md)).

## Comfort and cold

The server updates air and felt temperature once per second. Felt temperature follows, with a 2.5-second response, a blend of 60% torso-height air and 40% the radiant temperature of what the player sees within 8 blocks. 32 evenly spread sight lines each take the temperature of the first block they meet beyond the one the torso is in, so a ladder, open door or scaffold the player stands in hides nothing; one that ends in open air takes the sky if it points upward under open sky, otherwise that air. Flames and the surface of lava with a clear line to the torso add their warmth, falling off with distance, and so does direct sun. On a 59°F evening a lit campfire or fire beside you feels about 88°F, about 73°F two blocks away and 66°F three away; a torch adds a few degrees close up, and lava is much hotter (capped). Walls, closed doors and water block that warmth, and warm or cold masonry makes a room feel warmer or cooler than its air.

**Clothing** pulls that blend toward skin temperature (91.4°F, 33°C) by the wearer's insulation share. A full leather suit is 20%; metal armor of any kind — chainmail, iron, gold, diamond, netherite and the Thermal Helmet — is padded with the same leather and cloth and gives 10% for a full set. Each piece counts by how much of the body it covers: chest 40%, legs 30%, helmet and boots 15% each, so mixed sets add up piece by piece. A 50°F evening feels about 58°F in leather and 54°F in metal armor. Below skin temperature clothing warms, so heavy clothing makes warm weather hotter; above it, clothing shields a little from the heat. Turtle shells, elytra, pumpkins and heads are not clothing, and grade and wear do not change insulation.

**Sun and sky.** Climate temperatures are shade air; sun and sky are felt on top. The sun follows the time of day on Photon's default tilted path (35°), so shade lines up with the shadows Photon draws; a sight line 64 blocks toward it finds whether any block beyond the torso's own shades it, and unloaded ground counts as open. Direct sun thins toward the horizon and stops when the sun is within 3° of it. At midday in the open the sun adds about 13°F to felt temperature. A clear sky is much colder than the air, offset by day by its scattered light, so open shade on a clear day feels about like the air while a clear night outdoors feels about 6°F colder than under a roof. Cloud makes the sky radiate like the air in proportion to the district's [cloud cover](geography.md#weather), and the sun is hidden while one of the server's clouds lies where the line to the sun crosses the cloud layer (Y 363), the same clouds Photon draws; as in Photon, cloud shade fades out as the sun drops low. Being seen never changes the heat field. Only loaded chunks are read: where any chunk within 8 blocks is not loaded, felt temperature follows the air alone. `ThermalRules` owns the tuning.

**Comfort** follows a Gaussian curve centered on 70°F with an 8°F spread: 100% at 70°F, about 46% at 60°F or 80°F and about 2% at 48°F. At full comfort an active Survival player gets up to 20% cheaper block labor, travel, jumping and attacks, 15% faster block breaking and 5% more movement speed. Healing and hunger-effect costs are unchanged, depletion suspends the benefits, and sprinting stays three times walking per block.

**Cold:** felt temperature below 64.4°F (18°C) costs calories once per online second in Survival, rising quadratically to `coldExposureMaxKcalPerSecond` at 35.6°F (2°C) or colder — a quarter of that at 50°F. The [calorie table](survival.md#calories) gives the default. There is no charge while heat is disabled, at unloaded or unsupported locations, in Creative or Spectator, or offline, and the reserve never goes below zero. The journal action is `cold_exposure`. This is energy demand, not temperature damage.

## Machines

A hot machine's masonry leaks at a temperature-adjusted rate: unchanged from 32°F through 78.8°F, so an unfed machine stays hot up to 10% longer when warmer and up to 30% less long below 32°F, with overhead shelter halving the cold penalty. This applies to every coal-fired machine; work draws, times and yields never change, and engines and claim upkeep are unaffected. Neighboring field temperatures are sampled around the controller once a second. `ThermalRules` owns the tuning.

**Rain on the work face:** direct rain on the block in front of a heated controller blocks ignition and pauses productive work in Kilns, Foundries, Cooking Stoves, Fertilizer Furnaces, town workshops and the Fired Heater. The regional `isRainingAt` query includes sky exposure and biome precipitation, so a roof over the work face is enough and a chimney may stay outdoors. A lit fire keeps the masonry hot and work resumes when sheltered or dry; the screen names the obstruction. Oil Derricks, Coal Drills, the Distillation Column, Condenser and Hot-Bulb Engines are not rain-paused.

## Player display

A small brass-and-glass thermometer between the health and calorie bars shows felt temperature in Fahrenheit. Its glass shifts continuously from muted blue (cold) through sage green (comfortable) to amber-red (hot), with a four-second breathing highlight that strengthens with actual comfort bonuses and disappears while depleted. A small brick chevron appears while [street paving](survival.md#street-paving) is speeding you. The display follows Survival HUD visibility and hides with F1, in Creative or Spectator and when data is missing or stale. Hover the thermometer in the inventory for air and surroundings temperatures, the share of direct sun reaching you while the sun is up, what your clothing adds or takes away, felt temperature, comfort, current cold drain, labor and travel savings, mining speed, comfort movement and the separate road bonus (not added together).

Felt temperature also shows on screen, every effect settling over a few seconds and fading out on stale samples. A translucent color wash covers the finished world and interface, including screens and tooltips: warm golden tan near 70°F, deepening to blue below about 53°F and to red above about 87°F. Over the world view, below about 61°F the screen edges glow icy white and from about 55°F frost creeps in from the corners; above about 78°F the edges glow ember-orange and throb with a lub-dub heartbeat that quickens with heat. Below about 53°F the camera gives a quick shiver every 12 to 15 seconds, every 6 to 9 seconds in the hardest cold; above about 87°F it sways slowly and the view throbs slightly with the heartbeat. Everything reaches full strength at 36°F or 104°F. Camera motion also follows the vanilla Distortion Effects setting. The wash and edges are drawn after shader rendering and do not change shader settings. `config/civilization-temperature-wash.toml` has client `enabled` (default true) and `intensity` (default 1, range 0–2), which scale all of it.

`/civilization heat` reports local air and surroundings temperatures, direct sun, the clothing shift, felt temperature, comfort, thermal efficiency, solver age and timed-release count.

## Thermal Survey Helmet

A Creative inspection instrument (Civilization creative tab or `/give @s civilization:thermal_helmet`; no Survival recipe) that reuses the iron helmet model. Worn in the head slot, it tints visible nearby block faces by their sampled temperature on a fixed, deliberately nonlinear Fahrenheit palette from blue through tan and amber to red and pale extreme heat, with a labeled legend. Full blocks and partial shapes are tinted on exposed faces; solid foreground geometry hides what is behind it. The block under the crosshair gets an outline, its approximate temperature and its difference from local air. Up to 14 well-spaced arrows estimate the strongest nearby heat flows from sampled temperatures and the shared exchange rule (without partial-face or buoyancy terms). The readout shows time since the last completed field step and highlights delays over three seconds.

The sampling lattice is fixed to world coordinates while the window follows the player: cells within 10 blocks of the torso, sampled once every two seconds per wearer, sent as offsets from one position with temperatures to 0.1°C. Fluid surfaces and animated model geometry are not reconstructed. Removing the helmet removes the overlay.

## Save compatibility

The local field, pending machine heat and timed releases are saved in heat **format v3**. Version 2 files load with their heat and queues and start a new timeline at the current server time; version 1 absolute-temperature data resets. Partial results from an exchange in progress are discarded on restart and its queued inputs replayed once; the clock rebases without simulating offline time. **Back up the heat file before upgrading an established world: an older JAR cannot keep the v3 timed-release history if you downgrade.**
