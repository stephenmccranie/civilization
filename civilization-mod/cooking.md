# Cooking stove prototype

The experimental **Prototype Stove** tests the first Tier 2 sensory cooking loop. The kitchen's later multiblock stations remain [proposals](../open_decisions.md#tier-2-kitchen-workshop). This slice uses a dedicated cast-iron stovetop on an open steel stand, with one exposed skillet, one rotary heat dial and four narrow ember vents. It has no oven door or baking cavity; the separate oven remains later kitchen work. Original materials use 64 pixels per block face in a 512×256 model atlas. Quiet metal fills have component-specific seams, steel fasteners, a fitted vent surround, dial calibration ticks and restrained skillet-lip wear; the marks are mapped to measured faces rather than repeated as a generic tile. Ingredient loading and serving are built into its menu.

## Try it

Obtain the block from the Civilization Creative tab or `/give @s civilization:prototype_stove`. There is no Survival construction recipe yet. Place it under cover so its coal fire can be lit normally.

1. Right-click to open the stove. Add **2 raw potatoes, 2 carrots and 1 bread**, plus Mineral Coal in the fuel slot.
2. Click **Load batch**, then the flint-and-steel button to light the fire. Accepted strikes use the shared ignition rule; a spark can fail, with a one-second retry cooldown.
3. Drag the **round dial** to any heat setting. Left/right arrows adjust it finely. Close the menu to watch and listen to the pan in the world.
4. While looking at the stove, **crouch + scroll** adjusts heat; hold the sprint key as well for finer adjustment. The physical pointer turns with the setting.
5. **Crouch + right-click with an empty hand** serves an edible batch, or use **Serve** in the menu. Collect the four portions from the output slot. A batch cannot be loaded until that slot is empty.

Lower heat gives a longer timing window at the same maximum quality. The food gradually changes from pale to golden to brown and charred. Wet cooking, dry sizzling and crackling sound layers blend with cooking progress; activity also follows the dial. There is no live quality number, countdown or perfect-state lamp.

## Current trial values

One server-owned cooking coordinate advances at `heat^0.7` full-heat work ticks per game tick. Zero heat pauses progress, without extinguishing the fire. The maximum-quality plateau spans 800â€“1,000 work ticks. Quality uses smooth curves before and after it; serving locks in the **present** result, never the best previously reached. Before 400 ticks, serving is rejected and leaves the batch intact. Progress caps at 1,800 ticks.

| Example dial position | Enter optimum | Time in optimum |
| --- | ---: | ---: |
| 30% | About 93 seconds | About 23 seconds |
| 60% | About 57 seconds | About 14 seconds |
| 100% | 40 seconds | 10 seconds |

These are examples along the continuous range. All assume uninterrupted cooking at that setting and 20 ticks/second.

The batch reserves its ingredients exactly once. Its calorie ceiling is calculated at loading from configured food values for **two baked potatoes, two carrots and one bread** (default 1,600 kcal). Raw potato inputs deliberately use the existing cooked-potato allowance. Each of four portions receives 80â€“100% of the per-portion ceiling as quality rises: defaults **320â€“400 kcal**. The stored ceiling survives configuration changes during cooking and the finished portions retain their saved values through trade/storage.

An eaten portion gives a **10â€“15% discount on active work calorie costs** for **30 minutes online**, scaling with its quality. Mining, placing, movement/ascent, rowing, jumping, melee, ignition and fertilizer labor use it. Sleep, natural healing, hunger effects, cold exposure and machine fuel do not. Eating another portion replaces its strength and refreshes its duration, without stacking. The benefit persists through logout/restart and the existing calorie-state death clone; offline time does not reduce it. Eating displays its strength and duration, and meal tooltips show quality, calories and work benefit. There is no cooking XP stat.

## Fuel and persistence

The stove uses the [shared finite coal fire](industry.md#continuous-coal-fires), including manual ignition, wet-work-face protection, low idle burn and environmental heat accounting. Productive fuel spending follows cooking progress, with a saved fractional prepayment remainder: changing heat does not mint work or consume another set of ingredients. Reaching optimum spends about two neutral-efficiency coal budgets, independently of the dial; overcooking and idle time spend more. No fuel pauses the batch and an exhausted fire requires relighting.

Both cooking stoves emit 2.5% of industrial waste heat: 325 local heat units per coal, about 16.25 units/second at full productive burn. This keeps the cook warm without furnace-strength heat beside the pan. Cooking speed and fuel work credit are unchanged; lower dial settings also lower productive room heat. Already-loaded fuel uses the reduced emission, while warmth previously stored in the room dissipates normally. Actual temperature still depends on climate, enclosure and operating time.

Dial, reserved batch, recipe calorie ceiling, cooking progress, fuel and fractional spending persist across normal chunk saves. Unloaded chunks do not advance cooking. Breaking the stove drops its inventory and returns the reserved raw ingredients of an unfinished batch. Finished output is produced once and cannot be served again.

## Prototype limits

This is one dish and one cooking position, with fixed cookware. Multiple kitchen multiblocks, recipe specialties, ingredient substitutions, moving hot pots, residual heat and oven cooking are later work. The color/sound cues and throughput need ordinary player playtesting; automated checks establish operation and conservation, not whether cooks enjoy or master them. No claim is made for cooking aboard moving vessels.

The [asset brief](../art/assets/prototype_stove/asset.json) owns editable Blockbench geometry, original material references, generated concept provenance and reproducible sound sources. The [meal asset](../art/assets/vegetable_skillet/asset.json) owns its transparent master and native sprite export.
