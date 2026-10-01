# Farming prototype — Civilization 0.6.0-dev

Release reference: behavior and verification statements below describe the stated versions. Use [current implementation](../../../civilization-mod/README.md) for today's build and [game rules](../../../design_direction.md) for planned changes. Historical “next” instructions are not the active work order. At the time of this release, bone-meal and stack changes were not yet implemented.

The first loop is **wheat → bread → field rations**, with optional single-harvest mineral fertilizer. Subsistence requires no fertilizer or industrial fuel. The kiln and fertilizer manufacturing are the next industrial work, not part of this build.

## Food and labor cleanup

Every vanilla edible item has an explicit default in FoodCatalog.java. Existing per-item server overrides remain authoritative, so customized values are not overwritten during upgrade. Unknown modded foods still use the configurable fallback. Cooked beef and pork now both default to **700 kcal**. Bread remains **500 kcal**. Magical side effects remain separate from energy value.

| Action | kcal |
| --- | ---: |
| Clear flowers, grass, leaves, saplings, mushrooms, and tagged light vegetation | 0.25 |
| Break a crop (mature or immature) | 1 |
| Plant a crop | 1 |
| Apply fertilizer successfully | 1 |
| Break other blocks, including stone and logs | 4 |
| Place other blocks, including torches | 2 |

Tag `civilization:light_vegetation` is data-driven. Crops are classified before the vegetation tag. Wheat, carrots, potatoes, and beetroot use the CropBlock category; this does not yet classify every modded plant or every vanilla vine/nether-wart interaction. Existing depletion and recovery behavior remains. The current harvest logger measures drops, not ownership of crops.

## Wheat

Plant ordinary wheat seeds on farmland. Hydration, growth time, light, and bone meal work as before. Harvest drops now have a predictable baseline:

| Crop | Mature yield | Immature yield |
| --- | --- | --- |
| Ordinary wheat | 1 wheat + 2 seeds | 1 seed |
| Fertilized wheat | 3 wheat + 2 seeds | 1 seed |

Both crops guarantee enough seeds to replant and expand. These explicit loot tables replace vanilla wheat's random seed/fortune behavior. Fortune does not increase either yield in this prototype. No soil nutrients, soil depletion, or crop rotation are simulated.

## Mineral fertilizer

Right-click **growing, unfertilized wheat** with Mineral Fertilizer. One item is consumed, plus 1 kcal of manual work, and a message/particles confirm application. The crop's age is preserved; fertilizer increases final yield rather than instantly growing it. It cannot stack on an already-fertilized crop, and mature crops reject it. Breaking early forfeits the fertilizer.

Fertilized wheat is a separate registered crop block, so its state persists in normal chunk saves without a global crop-position database or extra simulation loop. Harvesting removes that crop; replanting with its seeds makes ordinary wheat. Vanilla bone meal can accelerate growth but does not provide the fertilizer yield bonus. Bees can grow the crop; full villager/automated-farm compatibility has not been established.

As of **0.8**, fertilizer has a survival production chain: craft one clay ball and one gravel into Raw Mineral Blend, then process it in a Fertilizer Retort using Mineral Coal. Each blend produces four fertilizer in 20 seconds; one fuel supports sixteen fertilizer. See `industry-progression.md`. Bone meal remains a different item, despite the temporary shared vanilla powder icon.

## Field rations

Craft **3 bread → 1 Field Ration**, shapeless. It works in the inventory crafting grid or a crafting table. The recipe book unlocks when bread is acquired. One ration provides three times the server's bread calorie value: **1,500 kcal by default**. Both stack to 64, so rations carry three times the calories per occupied slot. Packing creates no extra food energy and consumes no industrial fuel. Excess calories above body capacity are still wasted when eaten.

Ration calories are derived from bread, so changing the bread override keeps packing energy-neutral. A ration-specific entry in foodKcal does not override this invariant. There is no unpacking recipe in this version. Ration and fertilizer icons currently reuse vanilla bread/bone-meal textures; the item names and tooltips identify them.

## Initial numerical check

Nine ordinary plots produce 9 wheat → 3 bread → 1 ration = **1,500 kcal**. Planting and harvesting cost **18 kcal**; the crop yields 18 seeds, of which 9 replant the plots.

Nine fertilized plots produce 27 wheat → 9 bread → 3 rations = **4,500 kcal**. Planting, harvesting, and applying fertilizer cost **27 kcal**, plus **9 fertilizer items**. Seeds are the same as above.

These are per-harvest material/energy figures, not profit or production-per-hour claims. They exclude field preparation, travel, waiting, distribution, and the as-yet-unimplemented cost of manufacturing fertilizer. Crafting/tilling have no extra calorie charge yet. Farm profitability still needs measured play sessions and a finite-fuel fertilizer cost.

## Production logs

The existing world-local energy JSONL adds a `production` object to production events. It contains `input_resource`, `input_units`, `output_resource`, `output_units`, and `potential_food_kcal`. Producing food changes inventory, not the player's body calories: its ordinary `delta_kcal` is zero. Actual eating remains an `eat` event.

- `crop_plant`: seed item and quantity consumed. A separate `place_block` event records labor cost.
- `fertilizer_apply`: fertilizer consumed and crop position. `fertilizer_labor` separately records manual energy.
- `crop_output`: actual dropped resource/quantity, position, crop identity, and mature/immature status. Seed drops have zero food potential. Wheat uses bread-value / 3 as its potential after processing.
- `food_produced`: manual crafting/processing output. For bread, tracked input is 3 wheat per loaf; for rations, 3 bread per ration. The hook uses the actual crafted amount, including bulk result removal, rather than the visible result-slot count.

Do not sum wheat food potential, bread food potential, and ration food potential together: they describe the same energy at different processing stages. Compare crop output against planting stock and labor, then track processing input/output separately. The before/after body-calorie ledger remains independent.

Per-player attribution requires a player breaker/crafter. Water, pistons, machines, and unattended processing do not yet produce a complete player-attributed production ledger. Later listeners or other mods that mutate drops may require integration. No ownership/trade accounting is implied.

## Try it

1. Plant two small wheat patches.
2. Manufacture fertilizer using the retort chain in `industry-progression.md`. Creative → Ingredients or `/give @s civilization:fertilizer 16` remain shortcuts.
3. Apply it to one patch while the wheat is still growing. Leave the other patch ordinary.
4. Harvest both when mature. Normal wheat yields 1 grain; fertilized wheat yields 3; both yield 2 seeds.
5. Make bread, then combine three loaves anywhere in a crafting grid to make a field ration.

For an accelerated mechanical check, bone meal can mature crops normally. For an economic measurement, let them grow at the server's ordinary tick rate and include field preparation/travel. The logger will capture manual production without debug commands.

## Verification

19 existing unit tests passed. **19 Minecraft GameTests passed**, including full vanilla-food catalog coverage, vegetation/crop cost categories, actual seed use and charging, single-use fertilizer and mature/double-application rejection, age preservation through growth and block-state save/load, deterministic mature/immature loot, exact ration recipe and actual eating, and real fertilized harvesting. Runtime JSONL was checked for one-seed input, one-fertilizer input, 3-wheat/2-seed harvest output, and 3-bread-to-1-ration production without a body-calorie gain.

The new crop rendering and inventory tooltips have not yet been visually verified in-game. This is a small gameplay prototype, not validation at 200 concurrent players.
