# Industry and agriculture — implemented recipe foundation (0.8 onward)

Release reference: this preserves the original recipe foundation and its verification evidence. [Current implementation](../../../civilization-mod/README.md) and [multiblock layouts](../../../civilization-mod/multiblock-builds.md) govern the current build; planned gameplay rules live in [game rules](../../../design_direction.md).

For current assembly/UI and work order, use the links above and the [active development plan](../../../development_plan.md). Names and rates below are historical.

Mining supplies finite industrial fuel. Kilns make construction materials. Kiln-made bricks enable a second machine, the Fertilizer Retort, which supplies higher-yield farming. Both machines use server-owned inventories and share the existing furnace processing implementation, while retaining separate recipe types, block entities, menus, visuals, and audit actions.

## Recipes and rates

| Operation | Inputs | Output | Time |
| --- | --- | --- | --- |
| Craft kiln | 8 cobblestone around a furnace | 1 Brick Kiln | Crafting |
| Fire clay | 1 clay block | 4 brick items | 200 ticks / 10 seconds |
| Craft retort | 4 brick blocks + 4 copper ingots + 1 furnace | 1 Fertilizer Retort | Crafting |
| Mix feedstock | 1 clay ball + 1 gravel | 1 Raw Mineral Blend | Shapeless crafting |
| Process fertilizer | 1 Raw Mineral Blend | 4 Mineral Fertilizer | 400 ticks / 20 seconds |

Both machines accept **Mineral Coal only**: 1,600 loaded server ticks per piece. Continuously supplied, a kiln produces 32 bricks or a retort produces 16 fertilizer per fuel. Times assume 20 TPS. Lit fuel keeps burning while idle or output-blocked; new fuel ignites only with a valid recipe and room for a complete batch. Unloaded machines do no work and consume no fuel.

Retort crafting-table layout:

```text
Brick block    Copper ingot    Brick block
Copper ingot   Furnace         Copper ingot
Brick block    Copper ingot    Brick block
```

Four brick blocks consume sixteen brick items. The recipe unlocks when a brick item enters the inventory; raw blend unlocks on acquiring a clay ball. The retort has copper casing and a blast-furnace-style face. Its inventory uses familiar furnace controls and shows recipe/fuel hints over empty slots. Top hoppers supply blend, side hoppers supply fuel, and bottom hoppers collect fertilizer. Shift-click routes matching inputs automatically. A kiln cannot process blend; a retort cannot fire clay blocks.

## Brick production gate

The vanilla `minecraft:brick` furnace recipe is disabled through a NeoForge false condition. Villager offers producing brick items are filtered when offers are accessed, covering both new and saved villagers. Other villager trades are preserved. Restart with this version to load the recipe change.

Brick blocks, stairs, slabs, walls, flower pots and other ordinary recipes using existing bricks remain. Existing inventories and naturally generated brick structures or pottery are not erased: they are finite salvage, not renewable brick manufacturing. Nether/stone/mud bricks are different material families and are unaffected. Additional mods/data packs could reintroduce production routes and need review when added.

## Fertilizer and balance

One retort fuel and four blends yield sixteen fertilizer. Each fertilized wheat plot yields three wheat instead of one at maturity, giving **32 additional wheat**, equivalent to about **5,333 kcal of additional bread potential** at the current bread value. This is extra harvest potential, not instant food or player calories. It requires growing time, applying each dose, harvesting, baking and distribution. Planting, fertilizing and harvesting still incur the existing manual calorie costs. Subsistence farming still works without fertilizer.

The clay/gravel blend is a **provisional gameplay feedstock, not a chemical or nitrogen-production model**. Separating the retort recipe from the machine lets us replace it later with a richer mineral/nitrogen chain without rewriting inventories, synchronization, persistence or fuel handling. Biomass is not a machine fuel. The prototype deliberately uses a small number of existing raw materials instead of adding ore generation during this update.

## Logging and persistence

The existing world-local JSONL journal records `retort_fuel` at ignition and `retort_batch` at completion, including consumed input and produced fertilizer quantities. New machine records also include the block registry ID in `machine`; old kiln log records lack this optional field. Player calorie records are unchanged. Machine records have no player calorie balance and no fabricated player attribution while operating unattended.

Both machines persist partial work, remaining heat and inventory through normal saves/reloads. Breaking one drops the appropriate machine and its stored inventory, losing active heat and unfinished work. The retort uses the same finite-fuel and full-output checks as the kiln. Machine ownership and deposit/withdrawal auditing are not yet implemented.

## Verification and development direction

The suite now contains **31 Minecraft GameTests and 19 unit tests**, covering the existing calorie/farming/kiln mechanics plus furnace gate enforcement, saved/new villager offers, retort construction, one-fuel fertilizer yield through actual crop use, recipe isolation, retort menu routing, save/reload and exact drops. Client startup is checked for registration/resource failures. These checks are not a 200-concurrent-player load test.

Work in coherent larger increments. Manual play sessions inform balance and feel without blocking each addition. The previously suggested construction machine is deferred following the user's correction. See `../development_plan.md` for the proposed regional-economy milestone, ownership/exchange work, server readiness and creative direction.
