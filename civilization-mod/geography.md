# Geography and weather

Natural river bands decide where crops grow and woodland decides how fast trees grow; cut trees fall from the cut up; regional weather waters fields; frontier uranium is scattered through the Overworld. Geography is derived from the world generator's biomes and rivers, so it changes no terrain and is not saved separately. Rivers are fresh water: squid spawn in the sea, never in river or frozen-river biomes. Deposits of coal and oil are in [industry](industry.md#deposits).

## Farmland and woodland

**Crouch-right-click with any hoe** to inspect ground without tilling it, spending calories or wearing the tool (clicking a crop inspects the soil beneath). Inspection reads **Can farm here | Trees grow fast**, or **Can't farm here** with the river or height requirement, or **Trees grow slowly**; on farmland it also shows **River-fed: 50% growth** or **Rain-soaked: 100% growth**, stored-water minutes and fertilizer status. `/civilization geography` reports the ground under your feet without operator permission.

| Property | Rule |
| --- | --- |
| River farmland | Within 48 blocks of a river or frozen-river biome, measured on the four-block biome grid |
| Height | Four blocks below to eight above the nearest natural river's top water block. Supported Terrain Diffusion rivers use their local elevation, including high valleys; other generators use the configured Y62 reference (Y58–70 soil) |
| Eligible crops | Wheat, carrots, potatoes, beetroot, melon and pumpkin stems, torchflower and pitcher crops, sweet berries, sugar cane and cocoa |
| Woodland | Forest, taiga and jungle families, cherry groves and mangrove swamps |
| Trees | Saplings keep vanilla random-tick speed in woodland and get one quarter as many attempts elsewhere |

Vanilla light, support, space and species requirements still apply. River farmland and woodland can overlap; felling trees does not remove woodland and planting does not create it. Crops planted outside the band show a warning and do not grow; existing crops are never destroyed, so mature ones can be harvested and immature ones stay dormant. Placed water creates no eligibility. The Nether and End cannot grow these crops.

### Tree felling

When a Survival or Adventure player breaks a log of a tree, every log of that tree at the cut's height or above breaks too; logs below the cut stay as a stump and leaves are left to decay. Creative breaks only the clicked log.

- **A tree** is the logs (`#minecraft:logs`, so stems and wood blocks too) joined to the cut through faces, edges or corners without passing below the cut's height, so acacia and dark-oak branches count and a log joined only through the stump does not. It counts only if one of those logs touches natural foliage: leaves a tree grew (not persistent, so never player-placed) or nether and warped wart blocks. Log houses and walls therefore stand.
- **Each log breaks as the player would break it by hand:** the same claim, whitelist and spawn-protection checks at that position (protected logs stay), the ordinary breaking calories, one point of tool wear and the tool's normal loot (Silk Touch and Fortune apply), with the drop landing at the log's own position and following the [block-carrying](physics.md#personal-block-carrying) pickup rules. Any tool or a bare hand fells; mining speed already makes the cut itself slow without an axe.
- **Tool wear:** felling stops before the tool could break, keeping enough durability for the cut, so a tool can break only on the cut itself. The logs nearest the cut fall first; the rest stay standing.
- **Limits:** the search reads only loaded blocks and at most 256 logs, enough for a giant jungle tree. A larger connected structure fells nothing beyond the cut log. Every felled log shows its normal break particles and sound, to the cutting player as to everyone nearby.

## Weather

Weather is local to fixed **512×512-block Overworld districts** and never changes river eligibility or claims.

- Natural storms last 25–35 minutes. Each following clear interval is drawn from an exponential distribution with a 12-hour mean: no dry cooldown, maximum drought or guaranteed rain (about 13.5% of intervals exceed 24 hours and 1.8% exceed 48).
- A **Rain Caller** (Civilization creative tab; no Survival recipe or cost) starts 30 minutes of rain in your district; calling during rain does nothing.
- Weather time runs at 20 ticks per second while the server runs, independently of the day; sleeping does not clear storms, shutdown pauses it and lag slows it. Unvisited districts start their schedule when first queried and catch up lazily without loading terrain.
- Natural destructive thunder is disabled. The server checks local precipitation for exposed wetness, fire and precipitation ticks, including snow and cauldron filling. Other mods that read the global weather flag are not regionalized.

Clients receive the nine nearby districts once per second. Rain and snow particles follow their positions, and sky strength fades over two seconds and blends near the viewer's district boundary. Fog and shader wetness use viewer-centered shader uniforms tuned for Photon; there is no simulated moving storm front.

**Clouds.** Each district's cloud cover drifts between random amounts over one to four hours of weather time, and rain thickens it to overcast as the rain's cooling sets in, over its first few minutes, then lets it thin as that cooling fades after the rain stops. One cloud field, fixed by block position and weather time and drifting with Photon's default wind, turns that cover into individual clouds; the server shades players from the sun with it, and clients receive the nine districts' cover and the weather clock once per second. Under Photon the mod rewrites the pack's cumulus coverage to draw that field, blended between district centres, and its ground cloud shadows to come from that layer alone; its other cloud layers stay its own and are only scenery. Other packs, other Photon versions and players with no shader draw their own clouds, while the server still shades by its field, and the log names any Photon file that was not recognised. Photon settings far from the pack's defaults (cloud scale, altitude, wind, sun path) move the drawn clouds away from the server's, and clouds beyond the nine nearby districts show the outer district's cover. The [Survey Table](ownership-trade.md#survey-table) inspection view shows district lines and the viewer's district weather; there is no weather HUD. Rain also cools the air ([heat](heat.md#climate)).

## Crops and harvests

Eligible farmland stays farmland without water trenches and grows at **50%** from the river alone. Exposed rain grows crops at **100%** and stores four seconds of water per second, up to 120 minutes (an empty field fills in 30 minutes); stored water then drains one second per second, even when the field is empty or mature. Roofs block recharge; harvesting and replanting keep the reserve, breaking the soil discards it. Snow does not recharge soil. Wet soil uses the resource pack's dark farmland texture.

Each covered crop cycle averages **two hours at full water or four at the river baseline**, ±10%; row layouts and vanilla growth multipliers do not change it. Melon and pumpkin stems need a full interval for each fruit and pause while a fruit is attached or no space is free. **Soil and crops advance only in actively simulated chunks**: unloaded soil freezes, keeps its reserve and misses rain, with no catch-up or forced loading. `randomTickSpeed=0` pauses farming; higher values do not speed it. Keep crops lit for night growth.

| Mature harvest | Ordinary | Fertilized |
| --- | --- | --- |
| Wheat | 1 wheat + 2 seeds | 3 wheat + 2 seeds |
| Carrots | 4 carrots | 10 carrots |
| Potatoes | 4 potatoes | 10 potatoes |
| Beetroot | 1 beetroot + 2 seeds | 3 beetroot + 2 seeds |
| Torchflower | 1 flower | 3 flowers |
| Pitcher crop | 1 plant | 3 plants |
| Melon fruit | 3 slices | 9 slices |
| Pumpkin fruit | 1 pumpkin | 3 pumpkins |

Carrots and potatoes reserve one planting item before tripling the rest. **Mineral Fertilizer** ([made in the Fertilizer Furnace](machines.md#fertilizer-furnace)) gives one dose to an immature crop or detached fruit stem on eligible ground; it changes yield only, never growth speed or moisture, and cannot stack. Fruit consumes the stem's dose and records the fruit's yield until harvest. Replanting loses the dose but keeps water. Unsuitable ground refuses fertilizer without consuming it or charging calories, and already-fertilized crops say so. Either half of a mature pitcher crop gives one harvest. Fortune and Silk Touch do not change governed mature yields; immature drops keep their normal rules.

### Other plants

| Source | Treatment |
| --- | --- |
| The eight farmland crops above | Shared soil, timing and fertilizer rules |
| Sweet berry bushes | River and height eligibility and the shared soil clock on whatever ground they grow: the ground keeps a water reserve, and each picking takes a full crop cycle to ripen again. Native picking yield; no fertilizer |
| Sugar cane, cocoa | River and height eligibility plus native requirements, timing and yields; no water reserve or fertilizer. Each sugar-cane segment must be inside the height band |
| Saplings | Woodland speed rule; no water reserve |
| Bamboo, cactus, kelp, mushrooms, vines and Nether wart | Native growth |
| Grass, flowers, moss and other spread | Native behavior |
| Animal food, fishing and emergency foraging | Separate systems |

**Bone meal cannot grow** crops, trees, land or underwater vegetation; manual use explains why, and dispensers and villagers cannot apply it. It still works as a crafting ingredient.

## Commands and configuration

- `/civilization weather` inspects your district, including its cloud cover, without permission; `/civilization weather rain` or `clear` (operator level 2) targets the command position. In the Overworld `/weather rain` and `/weather clear` use the regional control (rain is always 30 minutes; a duration argument is ignored with a note) and `/weather thunder` is rejected; other dimensions keep vanilla commands. `doWeatherCycle=false` freezes the schedule; existing rain still wets active fields.
- `serverconfig/civilization-geography.toml`: `riverRadius` (48, capped at 64), `riverWaterY` (62), `farmlandBelowWater` (4), `farmlandAboveWater` (8) and `outsideWoodlandDivisor` (4). Supported Terrain Diffusion rivers replace `riverWaterY` with their local surface.
- `serverconfig/civilization-weather.toml`: `meanClearHours` (12) and `cropSeconds` (7200; applies to newly started cycles). The soil water cap stays 120 minutes.
- Stop the world before editing either file; changes apply on the next load without moving crops or rewriting chunks.
- Datapack tags in the `civilization` namespace: biomes `river_regions`, `woodland_regions` and `uranium_regions` (which includes `#minecraft:is_overworld`); blocks `river_crops` and `woodland_saplings` (which includes vanilla's saplings tag). Tags control eligibility; only the enumerated farmland crops use the soil clock, so tagging another block does not adapt its growth stages or harvests. Scripted growth, commands and other mods setting block states directly are not intercepted.

## Generator integration

Biomes are sampled on four-block cells, so band edges step in four-block increments. With Terrain Diffusion's surface-water API, river-tagged cells also need a generated surface-water column, whose local water elevation sets the height band; underground reaches and dry corridors do not qualify, and biomes are sampled above the terrain so a high river or forest is not mistaken for its cave biome. Other generators, or Terrain Diffusion without that API, use the river-biome corridor and `riverWaterY`, so dry stretches of river biome qualify and oceans, lakes and canals do not.

Each level keeps bounded access-ordered caches of up to 32,768 sampled biome cells and 8,192 derived columns; a cold 48-block query checks at most 625 cells, tree checks sample only their own cell, and height-ineligible growth exits before the river scan. Caches are deterministic, clear on level unload or server stop, and invalidate on tag reloads or setting changes. Queries never generate chunks, add tickets or scan placed water (the Terrain Diffusion lookup may fill the generator's own terrain-model cache, as biome lookup does). `SoilChunk` keeps sparse per-soil state in chunk attachments and `SoilSystem` advances registered fields once per active second, without a block entity per plot; `RegionalWeather` owns saved district schedules, and natural rain and the Rain Caller share its `call`.

## Frontier uranium

Rare, tiny pockets of **Uranium Ore** generate in new chunks throughout the Overworld (the `civilization:uranium_regions` tag covers every Overworld biome). Placement makes one size-two pocket attempt per eight eligible chunks with no district or climate gate, in stone-family ore-replaceable rock, sandstone and red sandstone, at 4 to 80 blocks of cover below the local surface (concentrated near 40) rather than a fixed Y band. Actual finds are fewer than attempts because a pocket must hit compatible rock; some are within reach of the surface, others need cliffs, caves or digging. An iron-or-better pickaxe yields one **Raw Uranium** per ore. Raw Uranium is portable and has no processing or power use.

Exposed ore, uranium carried by a player and nearby chests holding Raw Uranium or Uranium Ore emit occasional fine silver tracks that travel about 2½–3½ blocks outward, stay where launched as their source moves and taper at their tips. These cues are visual only.

**Accessory slots:** three slots to the right of the player portrait, above the offhand slot (menu coordinates `(77,8)`, `(77,26)` and `(77,44)`), take only accessory items. They keep their contents through saves and dimension changes and follow normal death drops or keep-inventory. Creative inventory does not show them.

The **Geiger Counter** is a ten-second Assembly Bench job from 1 Glass Pane, 2 Copper Plates, 1 Redstone and 1 Machine Parts. Worn in an accessory slot, it checks loaded sections within 48 blocks once per second; only the wearer hears it, from one click per second at the edge to four per second at the ore. It reveals no direction or coordinates, uses no battery, does not detect raw uranium items and ignores unloaded chunks. No radiation effects exist.

## Save compatibility

- Geography is derived, not saved: changing the generator, biome tags or these settings changes suitability for existing worlds, which need no regeneration.
- The uranium feature keeps its ID `civilization:uranium`; ore generated in older chunks stays.
- Partially grown crops from older saves keep their visible age while the soil timer catches up.
