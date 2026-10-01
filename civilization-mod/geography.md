# Geography

The first regional system gives farming and forestry a shared, server-owned location query. It uses existing natural biome geography; it does not replace terrain generation.

## In play

Inspection uses short, direct wording: **Can farm here | Trees grow fast**. Unsuitable sites say **Can't farm here**, with the river or height requirement where relevant; non-woodland sites say **Trees grow slowly**.

**Crouch-right-click with any hoe** to inspect the selected ground without tilling it, spending calories or wearing the tool. Clicking a crop inspects the block underneath it. `/civilization geography` reports the ground under your feet without operator permission.

| Property | Starting behavior |
| --- | --- |
| River farmland | Within 48 blocks of a river or frozen-river biome, measured on the four-block biome grid |
| Height | Ground must be at Y58–70, inclusive: four blocks below to eight above the standard river water surface at Y62 |
| Eligible crops | Wheat, fertilized wheat, carrots, potatoes, beetroot, melon/pumpkin stems, torchflower/pitcher crops, sweet berries, sugar cane and cocoa |
| Woodland | Forest, taiga and jungle biome families, cherry groves and mangrove swamps |
| Trees | Saplings retain vanilla random-tick speed in woodland; outside woodland they receive one quarter as many attempts |

Vanilla light, plant support, space and species requirements still apply. Eligible farmland now supplies the 50% river baseline automatically; regional rain fills its water reserve for 100% growth. See [weather and farming](weather.md) for covered crops, timing, fertilizer and explicit exceptions. Eligibility does not guarantee that a tree fits. Woodland and river farmland can overlap. Removing trees does not erase woodland suitability; planting a plantation does not create it.

Players can plant outside the river band for decoration, but receive a warning and crops do not advance through natural random ticks. Existing crops are not destroyed: mature crops can be harvested, immature crops remain dormant. Fertilizer refuses unsuitable ground without consuming an item or charging calories. Already-fertilized crops retain their dose and report that they are already fertilized.

Placed water cannot create eligibility or give farmland the rain bonus. Non-farmland species retain their native water/support requirements. Building high above a river or deep beneath it does not qualify. The Nether and End cannot grow these crops. Emergency hand-foraging is unchanged. Other renewable plants—such as bamboo, cactus, kelp, mushrooms and Nether wart—retain their existing rules pending the broader resource audit. Bone meal remains globally disabled.

## Representation and limits

`Geography.inspect(level, soilPos)` returns overlapping river-band, height and woodland properties plus a readable failure reason. `canFarm` accepts the **ground** position; growth checks use the block immediately below the ticking crop. For multi-block sugar cane, each ticking segment must remain within the height band. `woodland` is a cheap independent query for tree growth.

The generator's biome source is sampled at the configured river water level, on four-block cells. Distance is circular between cell centers, so boundaries have four-block steps rather than exact block-level distance from a shoreline. The geographic query uses no block scans, chunk loads or background simulation. The separate weather/farming layer persists sparse soil and crop-production state in active farm chunks. Edits to water, trees or `/fillbiome` do not change the generator's geographic properties.

This first representation uses a **river-biome corridor**, not detection of naturally generated water blocks. Dry stretches within a river biome can qualify; oceans, lakes and player canals do not create bands. It assumes the standard Overworld water elevation rather than inferring local river heights. Natural, ceiling-free custom dimensions use the same rule and configuration. Tectonic's supplied Overworld noise settings retain sea level 63, but this alone does not establish that every generated river or underground stream is suitable farmland. Elevated/local river levels and navigable channel geometry still need a separate terrain survey before choosing a permanent generator.

Each active level has bounded access-ordered caches: up to 32,768 sampled biome cells and 8,192 derived columns. A cold 48-block query considers at most 625 candidate cells; neighboring fields reuse samples and repeated cells reuse the result. Tree checks sample only their own cell. Height-ineligible growth exits before the river scan. Cache entries are disposable and regenerated deterministically. Level unload/server stop clears them; server tag reloads and radius/water-height changes invalidate them. This is bounded implementation work, not a 200-player performance certification.

## Configuration and data

World configuration: `serverconfig/civilization-geography.toml`. Stop the world before editing. Radius, water elevation, vertical allowance and outside-woodland divisor are tunable. Radius is capped at 64 blocks to bound query work. Configuration affects existing fields immediately on the next world load; it does not move crops or rewrite chunks.

Data-pack tags in the `civilization` namespace:

- Biomes: `river_regions`, `woodland_regions`; the industry layer adds `coal_regions` and `oil_regions`, and frontier ore uses `uranium_regions`.
- Blocks: `river_crops`, `woodland_saplings` (the latter includes vanilla's saplings tag).

Use tag replacement deliberately when changing coverage. Tags control the existing eligibility gate; the explicitly enumerated farmland crops additionally use the soil clock instead of native random growth. Adding another block to the tag does not automatically adapt its growth stages or harvests. Scripted growth, commands and other mods directly changing block states are not universally intercepted.

Existing worlds need no chunk regeneration. Geography is derived rather than saved separately: changing the generator, biome tags or these settings may change suitability. Public-world migrations must preserve or explicitly migrate those inputs. The separate [deposit and industry layer](industry.md) now adds biome-tagged coal/oil placement, prospecting and finite extraction; that phase targets fresh worlds. Navigable river generation, broader habitat/spawn rules and monumental rain towers remain future work; regional weather and the prototype Rain Caller are described separately.

## Frontier uranium and accessories

The first Tier 3 frontier resource is a small, physical Uranium Ore deposit, generated by the vanilla ore feature in stone in the tagged `civilization:uranium_regions` biomes: windswept hills and forest, rocky peaks and shore, badlands variants, mountain meadows/groves/snowy slopes/cherry groves, windswept savanna/savanna plateau, and both old-growth taigas. Plains, rivers and oceans are excluded. Placement starts at one two-block vein attempt per eight eligible chunks, between Y=16 and Y=192; actual yield varies with host stone and terrain. The wider vertical range lets highland prospectors reach ore from mountain slopes. It targets new chunks. Iron-or-better pickaxes yield one Raw Uranium per ore block. Raw Uranium is portable and currently has no fuel or processing recipe. Actual generated-terrain abundance remains to be measured before changing the attempt rate.

The three accessory slots occupy the previously empty right column beside the player portrait at menu coordinates `(77,8)`, `(77,26)` and `(77,44)`, with offhand unchanged at `(77,62)`. Only accessory items can be equipped there; the Geiger Counter is the first. It uses a loaded-section ore check once per second, within 48 blocks of the wearer. Its wearer alone hears a click every 1 second at the edge, rising to four per second at the ore. It does not reveal direction or coordinates, force-load chunks, spend a battery or detect raw uranium items. Standard survival death drops include equipped accessories; keep-inventory preserves them. Creative inventory does not show these slots, so switch to survival to equip during testing. No radiation effects are implemented.

The counter's shaped crafting recipe is glass pane above redstone between two copper ingots, with Machine Parts beneath. Its T2 components make it a town-made instrument for frontier explorers. Other role-specific accessories are future content, not active bonuses.

## Verification

Unit checks cover circular and negative-coordinate boundaries, vertical limits, overlapping properties, cache reuse, eviction and deterministic reconstruction. Server checks use isolated river/forest/plains dimensions to exercise real crop dispatch, fertilizer acceptance/refusal, natural wheat growth, sky exclusion, forestry gating and queries that do not load chunks. Existing fertilizer/industry tests now apply their manufactured fertilizer on eligible river ground.

The isolated dimensions are embedded in the development-only flat preset because Minecraft's GameTest server discards standalone dimension definitions. They are excluded from the shipped mod JAR. The preset is a test fixture, not the planned navigable sample region. Current validation and deployment results live in [project status](../docs/status.md).
