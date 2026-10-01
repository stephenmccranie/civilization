# Heat and thermal comfort



## Temporary disable switch

`config/civilization-heat.toml` contains `enabled = true` by default. It is a per-instance/common configuration used by the authoritative server; restart after changing it. Civilization Dev is enabled again after the airship performance trial so cold exposure can be tested there. While disabled, source discovery/emission and diffusion stop; new fuel waste heat is discarded instead of building a catch-up queue. Existing saved heat and pending budgets remain intact. Comfort bonuses, cold drain, rain pauses on heated work faces and thermal machine efficiency adjustments are neutral, the survey is empty, and `/civilization heat` reports that heat is disabled. Restoring `true` and restarting resumes the saved field and rebuilds source discovery from loaded chunks.

## Initial thermal field

Overworld climate comes from biome, altitude, the 60-minute day and regional rain. Rain approaches up to a 6Ã‚Â°C cooler district baseline over five real minutes and recovers over forty minutes after it stops; the biome profile scales or excludes this direct rain response. The regional clock owns this background; no block-level temperature history is created merely by weather or daylight changing. The local field stores thermal energy relative to the current background, so an unheated cell immediately reads the current regional climate. At ordinary elevation, a river biome is about 63Ã‚Â°F at noon and 45Ã‚Â°F at midnight. This is a game-tuned boundary condition, not a simulation of ground thermal inertia.

### Biome climate profiles

Vanilla's biome temperature is a gameplay index rather than an air temperature. For ordinary land, the index supplies a mean climate and the temperate day/night swing is Ã‚Â±5Ã‚Â°C. Negative indices stay distinct: frozen peaks are colder than snowy plains. Biome families adjust this small shared rule:

| Climate | Clear-weather behavior at Y=64 |
| --- | --- |
| Forests, birch woods, cherry groves and meadows | Cool shared woodland mean with a small vanilla-index distinction, Ã‚Â±5Ã‚Â°C. Ordinary forest: 62.4Ã‚Â°F noon / 44.4Ã‚Â°F midnight. |
| Plains and sunflower plains | 12.2Ã‚Â°C mean, Ã‚Â±5Ã‚Â°C: 63.0Ã‚Â°F noon / 45.0Ã‚Â°F midnight. |
| River | 12Ã‚Â°C mean, Ã‚Â±5Ã‚Â°C: 62.6Ã‚Â°F noon / 44.6Ã‚Â°F midnight. Frozen river keeps its cold profile. |
| Other temperate land | Index-based mean, Ã‚Â±5Ã‚Â°C (18Ã‚Â°F peak to trough). |
| Desert and badlands | Hot index-based mean, Ã‚Â±9Ã‚Â°C. Desert: 102.2Ã‚Â°F / 69.8Ã‚Â°F. |
| Savanna | 25Ã‚Â°C mean, Ã‚Â±7Ã‚Â°C; windswept savanna has a 24Ã‚Â°C mean. Ordinary savanna: 89.6Ã‚Â°F / 64.4Ã‚Â°F. |
| Non-snowy taiga | 12Ã‚Â°C mean, Ã‚Â±5Ã‚Â°C: 62.6Ã‚Â°F / 44.6Ã‚Â°F. Snowy taiga retains its colder index. |
| Jungle and swamp | Index-based mean plus 4Ã‚Â°C, Ã‚Â±3Ã‚Â°C. Jungle: 81Ã‚Â°F / 70.2Ã‚Â°F. |
| Oceans and coasts | Ã‚Â±3Ã‚Â°C. Ocean means are explicitly frozen Ã¢Ë†â€™8Ã‚Â°C, cold 8Ã‚Â°C, ordinary 16Ã‚Â°C, lukewarm 23Ã‚Â°C, warm 27Ã‚Â°C; beaches and mushroom fields keep their index-based mean. Deep variants follow the matching surface profile even though vanilla gives them nearly identical indices. |
| Caves | Lush 16Ã‚Â°C, dripstone 14Ã‚Â°C, deep dark 10Ã‚Â°C, without a direct surface day/night or rain change. |
| Stony peaks | Index-based mean minus 7Ã‚Â°C, then the ordinary altitude cooling. |

Rain cooling is 80% of the district value in dry/savanna profiles, 60% in humid profiles, 50% along coasts and oceans, zero in cave profiles, and full strength elsewhere. Above Y=80, air cools 0.045Ã‚Â°C per block of elevation; this does not replace the biome's mountain profile. Unrecognized modded biomes use their own index and the temperate swing. The [earlier all-54-biome audit](../docs/history/biome-climate-audit-2026-09-27.md) records the prior climate tuning; the current woodland, plains and river values are in the table above. Local fires, buildings and heat transfer then alter those background values.

Each active block cell has energy and a material-dependent heat capacity; its displayed temperature is background plus energy divided by capacity. Sources, air, walls and player-built chimney pieces all occupy the same one-block lattice. Partial blocks use their actual eight cut-piece materials for capacity, and the occupied pieces on each 2Ãƒâ€”2 face determine how much air can pass through it. Closed full blocks conduct without airflow; open doors and empty space let air mix. No room, duct or named chimney is detected. Placing or breaking a block changes that cell's capacity and interfaces, so it changes the resulting temperature and flow.

A frozen field snapshot exchanges energy between neighboring cells and publishes the complete result. Indoor air mixes 15% of a difference per quarter step, or 19% upward if lower air is hotter, with 4% additional buoyant rise. Air in a sky-exposed column mixes 10% sideways and carries 16% of its warm excess upward. Both paths transfer heat between adjacent cells without creating it, and their combined per-step outward rates remain below one. This favors upward exhaust through an open shaft without stripping all warmth from an enclosed room. Solid-to-solid conduction is slower (3% of the lower conductance), while a hot solid face transfers to adjacent air at 10% of the square root of conductance. Exposed air exchanges about 1.8% of its excess with the regional atmosphere per simulated second. A tiny exposed-air difference below 0.05Ã‚Â°C and numerical residuals are retired to an atmosphere ledger. These rates are deliberately coarse rather than real fluid dynamics. A cut-block shaft works by material conduction, face openings and ordinary air transfer; it is not granted a hidden outlet bonus.

A coal machine emits environmental heat as its purchased reserve is spent. One Mineral Coal carries 13,000 game heat units for industrial sources, independent of the work recipe. Household and prototype cooking stoves emit 2.5% of that local waste heat (325 units per coal; about 16.25 units/second at full productive burn), so working beside a stove is moderately warm. Their fuel work credit and processing times are unchanged. This stove factor is applied when the reserve is spent, including already-loaded coal; existing room warmth dissipates normally. A continuously productive industrial coal fire therefore releases about 650 units per loaded second; ordinary idle burn releases one tenth of that. The coal burn rate, machine work credit and processing times have not changed. The Smithy's diamond job runs four times as long as iron at the same productive burn rate, so its total coal use and environmental heat are four times higher, without multiplying heat a second time. The source queue limits release to 800 units per simulated second and retains any remainder for later. Industrial batch releases can therefore trail actual completion. Existing saved fire reserves retain their already-purchased remaining heat; new coal uses the new amount. Old saved fire reserves with no emission ledger do not create a second emission.

A registered machine allocates the *same finite release* to two physical entries: 80% at actual open top outlets and 20% evenly across all matching structural blocks, including the controller. The outlet receives four times the casing's heat budget. Each open outlet spreads its share across up to eight consecutive air cells above it, approximating one second of rising exhaust instead of concentrating it in one stationary cell; a roof stops the path. Every part of the shell can warm adjacent room air through ordinary face exchange. If all top outlets are blocked, their share remains in the machine body rather than being deposited into a solid roof; heat never jumps outside. A player-built enclosed shaft channels rising hot air through its physical openings, while a machine exhausting into an unvented room gradually overheats that space. No named chimney or room is recognized. Torches supply 6 units per loaded second, lit campfires 24, and lava is a maintained hot reservoir limited to 48 units per exposed face per second. The Oil Engine releases heat from fuel it actually consumes. All player comfort still comes from the temperature of the air at torso height, with a separate felt-temperature response over 2.5 seconds; there is no proximity-only warmth bonus.

The local field and pending fuel heat are saved in format v2. Loading the older format resets its transient heat and pending queue because the old temperature-reference cells could fill the field with stale cold ground. It does not alter blocks, inventories, machine fuel or player data. Save the old heat file before updating an established world. Unloaded chunks are neither forced to load nor simulated; their stored warmth and pending machine emissions wait for reload. Existing source discovery is chunk-indexed and bounded, and its emission queue spreads work fairly across ticks.

The current solver still performs a whole-field exchange in bounded batches under a 3 ms soft budget per server tick. It copies the heat snapshot over multiple ticks before exchange reads and publishes atomically; incoming machine heat is journaled and merged once. Capturing the starting cell keys, some map growth, individual world reads and JVM pauses can still exceed that soft budget. The v2 cap is 262,144 active cells and 4,096 pending machine-source locations per dimension. Weak settled cells may be retired to the atmosphere to make room; if a field is saturated during an active exchange, new machine heat stays queued until capacity is available. The helmet and `/civilization heat` show solver age, so a stalled temperature reading is visible. Section-local scheduling, per-section memory quotas, stale-data handling by individual district, and representative 200-player profiling remain open; this build does **not** claim that scale.

Lit normal and soul campfires, ordinary and soul torches, and exposed natural or placed lava warm loaded neighboring cells through the same field. Extinguishing or removing a source ends new emission while stored warmth decays. Redstone torches have no combustion heat. Flame/lava discovery and emission have separate bounded queues; broad lava fields can take time to index. There is no wind, smoke, indoor room classifier, radiant player shortcut, offline city simulation or sky-city thermal guarantee.
## People and machines



Comfort follows a Gaussian curve centered on 70Ã‚Â°F with an 8Ã‚Â°F spread: 100% at 70Ã‚Â°F, about 46% at 60Ã‚Â°F or 80Ã‚Â°F, and about 2% at 48Ã‚Â°F. Felt temperature below 64.4Ã‚Â°F also spends calories once per online second in Survival: a quadratic ramp reaching 0.20 kcal/s at 35.6Ã‚Â°F or colder, with 0.05 kcal/s at 50Ã‚Â°F. `coldExposureMaxKcalPerSecond` in the calorie server config sets the cap. Heat disabled, unloaded/unsupported locations, Creative/Spectator and offline time have no cold charge; the reserve cannot go below zero. This is energy demand, not direct temperature damage. At full comfort, active survival players gain up to 20% cheaper block labor and travel, 15% faster block breaking, and 5% movement speed. Supported jump/attack calorie actions use the same discount. Healing and hunger-effect costs stay unchanged; depletion retains its existing restrictions. Sprint remains three times walking per block at equal comfort. The server updates air and felt temperature once per second; comfort and cold costs use felt temperature.



Kiln-family furnaces and workshops buy a temperature-adjusted work budget with each new coal. From 32Ã‚Â°F through 78.8Ã‚Â°F the base work budget is unchanged; warmer surroundings add at most 10%. Below 32Ã‚Â°F cold reduces it by up to 30%, with overhead shelter halving the cold penalty. The Fired Heater instead pays a correspondingly adjusted batch heat cost, checking sufficient physical coal before committing. Processing times and output quantities stay unchanged. Pump/engine work efficiency and claim upkeep are unchanged. Existing purchased work credit is retained.

Direct rain on the block immediately in front of a heated controller prevents ignition and pauses productive work in Kilns, Foundries, Cooking Stoves, Fertilizer Furnaces, town workshops and the Fired Heater. The existing regional `isRainingAt` query includes sky exposure and biome precipitation, so a roof over the work face is enough; an exposed chimney outlet may remain outdoors. Already-lit coal fires continue at the lower idle rate and resume their saved work when sheltered or dry. Oil Pumps, Coal Drills, the distillation column, condenser and Hot-Bulb Engines are not rain-paused by this rule. Rain still affects their surrounding air through district climate. The controller screen names the rain obstruction; the common ignition button rejects a wet heated face.



This is a bounded initial loss approximation, not a full internal machine-temperature/ignition/cooling simulation. Neighboring ambient/field temperatures are sampled around the controller. A roof is the initial direct shelter cue; actual wall insulation also affects the surrounding heat field. Tuning lives in `ThermalRules`; material lookup delegates to the [shared physical material compatibility policy](physics.md). Its legacy heat capacity remains distinct from physical mass and J/K capacity.



## Survival temperature HUD



A small brass/glass thermometer sits between the health and calorie bars, with local Fahrenheit temperature above it. It shows the player's felt temperature; hover in inventory to compare it with the measured torso air. No helmet is required. It follows survival HUD visibility and hides with F1, Creative/Spectator, or missing/stale temperature data.



Cold glass is muted blue; comfortable glass sage green; hot glass amber-red, with continuous interpolation by comfort. A restrained four-second breathing highlight strengthens with actual comfort bonuses and disappears while depleted. There is no extra meter or permanent percentage text. A tiny brick-pattern foot/chevron appears only when the existing brick-road contact rule is active, including supported cut-block surfaces; it disappears off-road or airborne. Neither display changes any bonus calculations.

A temperature color wash is drawn over the finished world and HUD, including open screens and tooltips. It is blue in cold conditions, distinctly warm golden tan near 70Ã‚Â°F, then eases toward red as heat reduces comfort. Changes settle over a few seconds rather than flashing when entering warm air. It uses the same server-sent felt temperature as the thermometer; unsupported or stale samples fade out. This is a client-side translucent color effect after shader rendering, not a mutation of Photon's internal white-balance setting or a change to heat simulation. `config/civilization-temperature-wash.toml` has local `enabled` and `intensity` controls (default on, strength 1; 0Ã¢â‚¬â€œ2 range). The effect intentionally covers interface colors as well as the world.



Open the normal inventory and hover the matching thermometer above its panel for labeled air and felt temperatures, comfort, any current cold calorie drain, work/travel calorie saving, mining speed, comfort movement and the separate road movement bonus. Depletion explicitly reports paused comfort benefits. Road and comfort percentages are not added together. The Thermal Survey Helmet retains its separate diagnostic thermal camera.



## Thermal Survey Helmet



Use `/civilization heat` for local air and felt temperatures, comfort and thermal efficiency.



Equip **Thermal Survey Helmet** in the head armor slot. Available in the Civilization creative tab or `/give @s civilization:thermal_helmet`; no survival recipe yet. It temporarily reuses the native iron helmet model/armor texture.



While worn, visible nearby block faces receive a strong translucent color from their actual sampled block temperature. The fixed Fahrenheit palette runs from blue cold surfaces through tan comfort, amber/orange heat and red/pale extreme heat; its labeled legend is deliberately nonlinear to give useful visual room near human temperatures while retaining an absolute meaning. Full blocks and physical partial-block shapes are tinted on their exposed faces. The block under the crosshair also gets an outline and approximate Fahrenheit temperature and difference from local air. At most 14 well-spaced arrows estimate the strongest nearby heat-transfer vectors from sampled temperatures and the shared exchange-rate rule; the overlay does not yet include the solver's partial-face or buoyancy terms. The readout shows age since the last completed field exchange and highlights delays above three seconds, so stale data is visible. Solid foreground geometry hides surfaces and arrows behind it. All player-facing temperatures use Fahrenheit; internal simulation and save units are unchanged. Remove the helmet to remove the overlay. This is a development inspection instrument, not an approved survival progression item. The underlying 1Ãƒâ€”1Ãƒâ€”1 sampling lattice stays fixed to world coordinates while the bounded viewing window follows the player. Only cells within 10 blocks of the player's torso are sampled, once every two seconds per wearer; the compact packet stores offsets from one world position and temperatures to 0.1Ã‚Â°C. The once-per-second comfort update remains separate. Fluid surfaces and purely animated model geometry are not yet reconstructed as thermal meshes.



## Follow-ups



Detailed internal machine temperature and meaningful cooldown/startup; enclosure/exposure and circulation tuning; clothing; thermal ducts/exchangers; crop rules; moving ships; climate/material balance and representative loaded-city profiling. Maintain energy conservation and predictable unloaded behavior when extending the system.

