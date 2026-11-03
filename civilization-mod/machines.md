# Machines

A machine is a controller block plus a structure the player builds around it. This page owns what every machine shares — construction guides, the cabinet interface, coal fires, the coal budget, production XP and JEI — and the two first machines, the Kiln and Fertilizer Furnace. The Foundry and town workshops are in [manufacturing](manufacturing.md), the oil machines in [industry](industry.md), the stoves in [cooking](cooking.md).

## Building with a controller

1. Craft the controller and place it where the structure's bottom/front center belongs; its front faces you. Its tooltip lists the additional structural blocks, and holding it shows the whole structure first (see [placement preview](#placement-preview)).
2. Look at the controller within eight blocks. Translucent ghosts of the missing pieces appear, using the real block models and resource-pack textures; correctly filled positions disappear. The top-left HUD lists remaining quantities by material and piece size, including pieces behind obstructions (air requirements count as obstructions but need no item).
3. Holding an accepted variant previews it (stone bricks, aged or waxed copper, a downward hopper for a hatch) in the guide's orientation and connections. Red outlines mark blocks that must be replaced and chambers that must be emptied; wrongly occupied positions show the real block with a correction outline. The directly aimed requirement is outlined in cyan and named in the HUD; real blocks stop target selection. Declared shared-cell pieces (such as Smithy table feet and tops) each keep their own guide, and excess cells or wrong materials are marked.
4. Right-click the controller with a matching stack to place as many missing pieces as that stack covers, with exact cut shapes and facing, or place them yourself. Full blocks need full items; halves, quarter beams and eighth cubes need saw-cut items or native slabs — **the controller never cuts or recuts pieces**. Already-correct pieces cost nothing; existing blocks, liquids and inaccessible or unloaded spaces are never replaced. Empty-hand right-click opens the controller.

The guide and HUD share one visibility decision: they show only while you look at the selected unfinished area (0.1-block retention margin, 150 ms release delay to avoid edge flicker) and hide immediately when the build completes, a menu opens or the HUD is hidden. One controller is selected at a time; walking more than 32 blocks away clears it. There is no toggle key or build screen. Guides are client-only and never load terrain.

### Placement preview

Holding a block that fills more than one cell or brings a structure (every controller with a construction guide, including the Oil Derrick, plus the Assembly Bench, Small Boat Helm and Baking Oven) and aiming at a block face shows where a right-click would put it, at the exact position and facing, before anything is placed. The preview asks the same placement code the click runs. The cells the click fills are textured with a cyan outline and the HUD names the block with **Right-click to place here**. A refused spot (an occupied cell, an entity in the way or missing support) is outlined in red without texture and reads **Can't place here: blocked or unsupported**. A controller's structure follows as the ordinary guide, so parts that would need clearing are already red, and the HUD counts the missing and obstructed blocks and lists the materials still needed. Claims are checked only by the server, so a spot inside someone else's claim can look valid and still be refused. Aiming at a block that opens on right-click, such as a chest or a machine, opens it instead; crouch to place against it.

Ghosts draw after the world's shader effects with their own neutral brightness (they do not inherit shader-pack lighting), test world depth without writing it, and keep the whole build's textures while drawing at most 64 hard outlines that are not hidden by nearer guide surfaces or real blocks. Client settings persist in `config/civilization-client.toml` and need no server permission:

- `/civilization preview textured` (default) or `/civilization preview outline`
- `/civilization preview opacity 0.32` (range 0.05–0.60)

### Guide performance contract

This contract binds every construction guide: `MachinePreview` for block assemblies and held placements, `DerrickPreview` for the Oil Derrick's native sections, and the Builder's Line preview. A held placement comes from `PlacementPlan`, recomputed each client tick from the aimed face; a changed plan refreshes at once, like a changed selection. New assemblies reuse these renderers; a custom renderer follows the same policy.

- Cache static positions and bounds per world, controller and orientation; cache missing-section geometry per construction state; clear on selection or world change.
- Prepare missing parts, obstruction states and material counts outside rendering. Refresh immediately on a changed selection, construction key or relevant held stack; poll other world changes only through `GuidePerformance.Refresh` (every 10 client ticks). Do not add separate polling intervals or budget constants.
- Render cached geometry. Never build collision unions or query structural world state in the frame loop; only camera aim and visibility update per frame.
- Cull off-screen parts before choosing outlines. `GuidePerformance.outlines` caps outlines at 64, including the aimed visible part; `outlineSteps` sets outline sampling. The translucent preview stays complete.
- Read only loaded terrain.

## Structure rules

An active machine validates its structure on every processing tick (26 positions for the Kiln, 27 for the Fertilizer Furnace). Idle empty controllers stagger checks once per second, and opening a menu checks immediately. Validation reads only nearby loaded chunks and never scans globally or force-loads.

Removing or replacing a required block, or obstructing the chamber, stops processing before the next batch completes: the fire goes out, unfinished work resets, no further coal is spent and materials are kept. Repairing allows operation with new coal. If a required neighboring chunk is unloaded, the machine waits with heat and work preserved. Validity is recomputed after load. Oxidation does not invalidate copper fittings.

Inventories live only in the controller. Breaking a shell block scatters nothing; breaking the controller drops its inventory and itself, and shell blocks stay as ordinary blocks. Decorative blocks outside required positions are allowed. Unloaded machines pause; there is no offline production or forced loading.

**Hoppers:** kiln-family machines take ingredients from above, coal from the sides or front and give output below. In the Kiln and Fertilizer Furnace the wall block directly above the controller (the hatch) may be a downward hopper feeding the ingredient slot; supply it from outside the front. A hopper underneath the controller extracts output (dig below or raise the build). No casing inventories or hidden transfers exist.

**Removing controllers:** stone pickaxes recover stone and metal controllers (the Smithy, extraction and refinery machinery, Hot-Bulb Engine, Land Controller, bulk stores) and Industrial Casing, pipes and engine pieces. Stone axes recover wooden controllers such as the Trade Counter, Survey Table and Small Boat Helm. Stocked bulk stores must be emptied first; claim and vehicle protections still apply.

## Cabinet interface

Every machine and civic screen uses one cabinet: soft tan enamel, a dark metal frame with brass corner pins, a raised brass nameplate and inset native slots and controls. Land, trade and survey controllers keep their specialized layouts inside it. The Survival/Adventure player inventory (with its live character preview and brass Crafting label), its recipe book, crafting tables, single and double chests, barrels and other row-based vanilla containers use the same skin with their native slot layouts; Creative tabs and specialized containers keep their own. The [Hand well](physics.md#personal-block-carrying) sits left of the hotbar. The boat helm is an in-world control, not a screen.

- **Slots.** Coal machines have two coal slots on the left of a centered fire window, with the striker on its right. The Kiln, Fertilizer Furnace, Cooking Stove, Foundry, Tannery and Textile Workshop have two inputs and two outputs; the Smithy and Assembly Bench have four interchangeable ingredient slots and one output; the Assembly Bench has no fuel. The Coal Drill's coal and the Distillation Column's sulfur use a 2×2 output bank. Stacks hold 32, so a paired bank holds 64. Kiln-family input drains the first slot before refilling from the second; new output fills the first compatible slot and splits across both; finished stacks stay in place when another is collected. Click, drag, shift-click and sided hoppers use whole banks; extra slots never raise processing speed.
- **Recipe drawer.** Open or collapse it with the side button, search by name and scroll item rows with the mouse wheel, Page Up/Down or the scrollbar beside them (drag its thumb, or click the track to jump there). Assembly Bench category tabs filter Tools, Weapons, Armor and Components, and material chips filter Iron, Gold, Diamond and Netherite; netherite rows are labeled as upgrades; Repair and Automatic are icon controls with hover labels. Filters never hide recipes because materials are missing. Selecting a recipe shows ghost requirements and output; ghosts never reserve or create stock. Selections are server-validated and saved with the machine; a selection restricts production to that output, incompatible ingredients wait, and switching resets partial progress without consuming ingredients.
- **Automatic matching.** Kiln-family machines match in recipe-ID order (clay makes bricks; select Terracotta explicitly to fire clay into terracotta). Workshops accept an ingredient in any input slot, combine split stacks, keep extra quantities for later batches and block on unrelated ingredients; Automatic runs only one unambiguous match, so shared materials (such as axe and pickaxe heads' ingots) need a selection. Repair and upgrade identity come from the actual source item regardless of slot order.
- **Feedback.** The fire window animates while lit and has no hover tooltip. A slim bar beside it shows the remaining energy of the last consumed Coal, draining at the actual idle or productive rate (hover for the percentage); the Oil Derrick and Fired Heater charge per batch, so their bar steps at completion. Liquid gauges show full fluid names and exact mB on hover, including the expected fluid in an empty buffer; color is supplemental. Menus report working, missing ingredients or fuel, wrong input, blocked output or ready to collect.

`MachineUi` and `MachineScreen` own rendering and recipe browsing, and `CivicWidget` reuses the frame and controls. `MachineMenus` owns shared slot placement and shift-click routing, `MachineInventory` fuel-bank and output operations, and `MachineWork` read-only workshop matching and consumption quotes for both server and UI; only the server commits fuel and materials.

## Coal fires

Every coal-burning machine — Kiln, Fertilizer Furnace, Cooking Stove, Prototype Stove, Foundry, Tannery, Textile Workshop, Smithy, Oil Derrick and Fired Heater — burns only Coal and shares one fire lifecycle:

- **Lighting.** Add Coal, complete the structure and click the striker. Each accepted strike costs `machineIgnitionKcal` (1 kcal at neutral comfort) and lights with a one-in-three chance; the server accepts at most one strike per second. An unlucky strike costs labor but no coal. Rejected clicks (cooldown, already lit, wet, incomplete or no coal) are free. Sparks and the native flint sound acknowledge a strike; the button disables while lit. Direct rain on the work face blocks ignition and pauses work ([heat](heat.md#machines)).
- **Burning.** A lit fire feeds itself from both coal slots, even without materials or with blocked output. Idle burn is one Coal per 200 loaded seconds at neutral temperature — 10% of the kiln/workshop active rate; the Derrick and Heater use the same idle lifetime. Idle burn makes nothing.
- **Going out.** When the burning reserve and all coal are spent the fire goes out, and added coal needs a new strike. There is no stop button: remove spare coal and let the reserve burn out, or dismantle the structure. Breaking the structure discards the burning reserve; unloading pauses it without catch-up. Lit state, reserve, fractional idle drain and strike cooldown are saved. Each coal purchase logs `coal_fire_fuel` in the [energy journal](survival.md#energy-journal).

Firelight and a quiet crackle follow the lit fire; smoke (industrial machines) or steam (stoves) follows actual processing; ignition and completion play brief sounds. Particles are local and follow particle settings; sounds use the Blocks volume. Lit models keep normal ambient shading, and their fire textures and emitted light still light the surroundings.

## Coal budget

`ProductionEnergy` owns one budget: one Coal buys **400 work units** at neutral temperature. Kilns and standard workshop jobs spend one unit per work tick (20 seconds per Coal); the Tannery and Textile Workshop spend one unit per two ticks; pumping and heating spend eight units per 5 mB. Uninterrupted productive work per Coal:

| Machine | One Coal makes |
| --- | --- |
| Kiln | 8 bricks (two batches) |
| Foundry | 2 ingots; steel takes 2 Coal per ingot |
| Cooking Stove | 2 cooked items |
| Fertilizer Furnace | 8 Mineral Fertilizer (one batch) |
| Tannery / Textile Workshop | 4 Leather / 8 Cloth from 4 wool |
| Oil Derrick / Fired Heater | 250 mB pumped / 250 mB heated |

Equipment forging takes longer by material, which scales its coal. Lit idle time spends coal as above. [Temperature](heat.md#machines) changes the work each new Coal buys; recipe times and yields do not change. Claim upkeep, crafting and refined-fuel consumption are separate. Unspent heat credit is saved.

## Production XP

Kiln-family machines (Kiln, Cooking Stove, Foundry and Fertilizer Furnace) accrue recipe XP with native furnace collection: taking either output awards the accumulated XP, hopper extraction leaves it banked, and breaking the controller releases it. Vanilla-equivalent outputs use vanilla smelting rates (cooked fish and meat 0.35, iron and copper 0.7, gold 1 XP per batch); a four-brick batch awards 1.2 XP; baking, steel and fertilizer award 0.35. The Tannery, Textile Workshop and Smithy accrue 1 XP per completed batch, banked through hopper extraction and released on player collection or controller removal. Fractional awards use vanilla probabilistic rounding. Hand assembly, upgrades, repairs, idle burning, blocked jobs, transfers, extraction and fluid processing award no XP.

## JEI

With Just Enough Items installed, recipe lookup (default **R**) and uses (default **U**) include the Kiln, Foundry, Fertilizer Furnace, Cooking Stove, Tannery, Textile Workshop, Smithy, Assembly Bench, Oil Derrick, Fired Heater, Distillation Column, Air-Cooled Condenser, Coal Drill, canister filling and emptying, saw cutting and recombining. Each category shows its controller or tool as the catalyst; ordinary crafting stays in JEI's Crafting category. The Foundry has an information page about the metal-free brick bootstrap.

Pages show ingredient alternatives and counts, coproducts, baseline seconds, fuel or tool requirements and operating notes. Coal appears as a fuel supply rather than a fixed per-recipe charge, because idle burn and temperature apply. Drill oil is marked optional with its dry slowdown explained. Repair pages are half-damaged examples; actual material use and resulting durability depend on the item. Retired furnace fuel pages and forbidden anvil/grindstone equipment repairs are hidden, and retired items are removed from the ingredient list.

Kiln-family pages come from the synchronized recipe manager, workshop and repair pages from the production catalog, refinery pages from shared `IndustrialProcess` batches, and cutting examples from the actual recipe match/assemble methods. Clicking the progress arrow in a kiln-family or workshop screen opens its JEI category; JEI keeps clear of the recipe drawer. The integration is read-only: it never fills machine inventories or changes the selected recipe. JEI is optional and unnecessary on servers.

## Kiln

Craft the **Kiln Controller** from nine cobblestone. The shell adds **13 cobblestone or stone-brick blocks, 4 slabs, 4 quarter beams and 4 eighth cubes** (stone variants may be mixed) and uses no bricks, so it can be built before the first brick exists.

The body extends one block left and right, two behind and two above the controller. The bottom layer is a full 3×3 plinth with the controller at front center. The middle layer has four inset upright quarter-beam corners (each in the inward corner along both horizontal axes), full side and hatch walls and an empty central chamber. The roof has four eighth-cube corners, four half-slab edge centers and one full central crown; roof pieces sit at the bottom of their cells.

| Input | Output | Time |
| --- | --- | --- |
| 1 clay block | 4 bricks | 10 seconds |
| Smelts-to-glass items | Glass | 10 seconds |
| Cobblestone, stone, sandstones, quartz block, basalt, cobbled deepslate | Stone, smooth variants, deepslate | 10 seconds each |
| Stone/deepslate/nether/blackstone bricks and deepslate tiles | Cracked variants | 10 seconds each |
| Clay block (selected) and the 16 colored terracottas | Terracotta and glazed terracotta | 10 seconds each |
| Cactus / sea pickle | Green / lime dye | 10 seconds |
| Wet sponge | Sponge (no water is collected) | 10 seconds |
| Netherrack / chorus fruit | Nether brick / popped chorus fruit | 10 seconds |

## Fertilizer Furnace

Craft the **Fertilizer Furnace Controller** from four brick blocks in the corners, four copper ingots on the edges and cobblestone in the center. The shell adds **13 brick blocks, 4 brick slabs, 4 brick quarter beams, 1 copper slab and 4 copper eighth cubes**. It follows the Kiln's plinth, inset supports and stepped roof; its roof corners are copper eighth cubes, and a copper half slab above the crown forms a fourth-layer chimney cap. Copper may be fresh, aged or waxed; matching vanilla slabs are accepted.

Craft **1 Sulfur + 1 Gravel → 1 Sulfur Blend**, then fire **1 Sulfur Blend → 8 Mineral Fertilizer** in 20 seconds, one Coal per batch at base efficiency. This is the only fertilizer route; sulfur comes from the [Distillation Column](industry.md#process-and-tuning), and applying fertilizer is covered in [geography](geography.md#crops-and-harvests). The Kiln cannot make fertilizer and the furnace cannot fire bricks.

## Retired vanilla processing

**Coal** (`civilization:mineral_coal`) is the single coal item for machines, claims, torches, soul torches, campfires, fire charges and crafting. Coal ore drops keep Fortune and Silk Touch, finite loot yields this Coal, and wither skeletons drop none. Vanilla coal, charcoal and coal blocks have no production or conversion recipes, are hidden from creative listings, power no machine and never convert; mined coal blocks drop nothing.

Vanilla furnace, blast-furnace and smoker processing is disabled, including on existing blocks, and their recipes and drops are removed, as are furnace-minecart crafting and propulsion. Campfire cooking is disabled; campfire smoke and bee utility remain. Nonmetal heat recipes run in the Kiln, ores and metals in the [Foundry](manufacturing.md#foundry), food on the [Cooking Stove](cooking.md#cooking-stove). Retired items are hidden from creative tabs and search, and villager offers involving them are removed when accessed. Crafting tables, banners, naming, enchanted books, disenchanting and armor trims keep their vanilla roles, within the [equipment limits](manufacturing.md#acquisition-limits). This is a curated recipe boundary; a datapack can still reintroduce recipes.

## Save compatibility

- The Kiln and Fertilizer Furnace keep the registry IDs `civilization:brick_kiln` and `civilization:fertilizer_retort`, and the furnace's journal events keep their `retort_*` names (for example `retort_structure_broken`).
- Sulfur Blend keeps the ID `civilization:enriched_mineral_blend`. Raw Mineral Blend (`civilization:mineral_blend`) is an inert legacy item: it cannot be crafted or fired and is hidden from creative and JEI.
- Legacy vanilla coal and charcoal stay loadable but power nothing. Old furnace minecarts roll without propulsion, and retired processors keep their contents accessible.
