# Cooking stove prototype

The experimental **Prototype Stove** tests the first Tier 2 sensory cooking loop. The kitchen's later multiblock stations remain [proposals](../open_decisions.md#tier-2-kitchen-workshop). This slice uses a dedicated cast-iron stovetop on an open steel stand, with one removable exposed skillet, one rotary heat dial and four narrow ember vents. It has no oven door or baking cavity; the separate oven remains later kitchen work. Original materials use 64 pixels per block face in a 512×256 model atlas. Quiet metal fills have component-specific seams, steel fasteners, a fitted vent surround, dial calibration ticks and restrained skillet-lip wear; the marks are mapped to measured faces rather than repeated as a generic tile. Ingredient loading and serving are built into its menu.

## Try it

Obtain the block from the Civilization Creative tab or `/give @s civilization:prototype_stove`. There is no Survival construction recipe yet. Place it under cover so its coal fire can be lit normally.

1. Right-click to open the stove. Add **2 raw potatoes, 2 carrots and 1 bread**, plus Mineral Coal in the fuel slot.
2. Click **Load batch**, then the flint-and-steel button to light the fire. Accepted strikes use the shared ignition rule; a spark can fail, with a one-second retry cooldown.
3. Drag the **round dial** to any heat setting. Left/right arrows adjust it finely. Close the menu to watch and listen to the pan in the world.
4. While looking at the stove, **crouch + scroll** adjusts heat; hold the sprint key as well for finer adjustment. The physical pointer turns with the setting.
5. **Crouch + right-click with both hands empty** lifts the skillet, keeping its food and paid heat. Use the held skillet on the top of a solid countertop to rest it, or on an empty Prototype Stove to return it. Each supported block holds one skillet; placement targets are broad and have no precision score.
6. **Right-click a resting skillet with an empty main hand** to serve four portions into inventory (overflow drops beside you). Crouch + right-click with both hands empty lifts it again. The stove menu still has **Serve**, which puts four portions in its output slot. Loading requires a mounted empty skillet and an empty output slot.

Lower heat gives a longer timing window at the same maximum quality. The food gradually changes from pale to golden to brown and charred. Wet cooking, dry sizzling and crackling sound layers blend with cooking progress; activity also follows the dial. There is no live quality number, countdown or perfect-state lamp.

## Current trial values

The continuous dial supplies `dial^0.7` paid heat units per game tick. One server-owned vessel reserve holds at most **160 work-tick units**. Each tick it releases `reserve / 160` units into cooking progress and gains only the heat actually supplied by the finite stove fire. A cold vessel gradually warms toward the dial setting, with an eight-second response time. Turning down or removing it changes the trend rather than stopping cooking instantly. Zero dial stops productive heat input without extinguishing the fire; stored heat continues cooking as it fades. The maximum-quality plateau spans 800â€“1,000 work ticks. Quality uses smooth curves before and after it; serving locks in the **present** result, never the best previously reached. Before 400 ticks, serving is rejected and leaves the batch intact. Progress caps at 1,800 ticks.

| Example dial position | Enter optimum | Time in optimum |
| --- | ---: | ---: |
| 30% | About 101 seconds | About 23 seconds |
| 60% | About 65 seconds | About 14 seconds |
| 100% | About 48 seconds | 10 seconds |

These are examples along the continuous range. All assume a cold skillet, uninterrupted cooking at that setting and 20 ticks/second. Warm cookware starts faster. The quality curve and broad optimum are unchanged.

The batch reserves its ingredients exactly once. Its calorie ceiling is calculated at loading from configured food values for **two baked potatoes, two carrots and one bread** (default 1,600 kcal). Raw potato inputs deliberately use the existing cooked-potato allowance. Each of four portions receives 80â€“100% of the per-portion ceiling as quality rises: defaults **320â€“400 kcal**. The stored ceiling survives configuration changes during cooking and the finished portions retain their saved values through trade/storage.

An eaten portion gives a **10â€“15% discount on active work calorie costs** for **30 minutes online**, scaling with its quality. Mining, placing, movement/ascent, rowing, jumping, melee, ignition and fertilizer labor use it. Sleep, natural healing, hunger effects, cold exposure and machine fuel do not. Eating another portion replaces its strength and refreshes its duration, without stacking. The benefit persists through logout/restart and the existing calorie-state death clone; offline time does not reduce it. Eating displays its strength and duration, and meal tooltips show quality, calories and work benefit. There is no cooking XP stat.

## Resting and carryover

A fully warmed vessel contains at most eight seconds of full-heat cooking work; lower settings store proportionally less. Off the stove, that finite reserve decays smoothly. Food color continues changing while wet/sizzle/crackle layers fade with actual vessel activity, including after the stove fire goes out. Removing a skillet near golden readiness lets it finish gently on the counter. Slightly early removal leaves an intact batch that can be returned for more heat. Handling itself awards no quality or calories.

The same approved pan and live food are visible mounted, resting, carried and in inventory. Item tooltips offer plain food/cooling observations without a live quality number. Stored or dropped items settle elapsed world-time cooling when next handled; putting hot cookware in a chest cannot freeze its heat. This includes time while the player is offline if that world continues ticking. Fully unloaded placed stoves/resting blocks do not simulate cooking until loaded again.

Lifting frees the stove. It can accept another empty skillet (available from Creative as `civilization:skillet`) and cook the next batch while the first rests. The one-skillet prototype does not require this second vessel. Removing the countertop drops the actual vessel with its contents; it does not refund another ingredient batch.

## Fuel and persistence

The stove uses the [shared finite coal fire](industry.md#continuous-coal-fires), including manual ignition, wet-work-face protection, low idle burn and environmental heat accounting. Productive fuel spending follows supplied vessel heat, with a saved fractional prepayment remainder: changing heat does not mint work or consume another set of ingredients. From a cold vessel, reaching optimum on the stove spends about 2.17 / 2.28 / 2.40 neutral-efficiency coal budgets at 30% / 60% / 100% dial: part of that energy is still in the skillet and can finish the batch off heat or help warm the next batch. Heat already paid for is carried once; moving cookware cannot mint fuel credit. Overcooking and idle time spend more. No fuel stops new heat input; the finite stored heat finishes dissipating, and an exhausted fire requires relighting.

Both cooking stoves emit 2.5% of industrial waste heat: 325 local heat units per coal, about 16.25 units/second at full productive burn. This keeps the cook warm without furnace-strength heat beside the pan. Fuel work credit and waste-heat factors are unchanged; lower dial settings also lower productive room heat. Already-loaded fuel uses the reduced emission, while warmth previously stored in the room dissipates normally. Actual temperature still depends on climate, enclosure and operating time.

Dial, mounted-vessel presence, reserved batch, recipe calorie ceiling, cooking progress, vessel heat, fuel and fractional spending persist across normal chunk saves. Breaking the stove drops its inventory and actual skillet, preserving unfinished food rather than also refunding raw ingredients. Old stove saves without the new fields retain their mounted skillet and existing batch, starting with zero stored vessel heat. Finished output is produced once and cannot be served again.

## Prototype limits

This is one dish, one heated position, removable cookware and one pan per supported resting block. Multiple kitchen multiblocks, recipe specialties, ingredient substitutions and oven cooking are later work. There is no preparation-table requirement, precision handling, physics spill or cooking XP. The color/sound cues and throughput need ordinary player playtesting; automated checks establish operation and conservation, not whether cooks enjoy or master them. No claim is made for cooking aboard moving vessels.

The [asset brief](../art/assets/prototype_stove/asset.json) owns editable Blockbench geometry, original material references, generated concept provenance and reproducible sound sources. The [meal asset](../art/assets/vegetable_skillet/asset.json) owns its transparent master and native sprite export.
