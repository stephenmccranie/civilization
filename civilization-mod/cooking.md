# Cooking

Food is cooked on the **Cooking Stove**, a household kiln-family machine. The **Prototype Stove** adds a sensory skillet loop whose meals reduce work costs, and the **Baking Oven** is a physical preview. Calorie values and food rules are in [survival](survival.md#food).

## Cooking Stove

A single block with no shell or guide. Craft it with three copper ingots across the top and six cobblestone below. It uses the [cabinet interface](machines.md#cabinet-interface) with two ingredient, two Coal and two output slots; hoppers feed ingredients from above and coal from the sides and take output below. It burns Coal in a [shared coal fire](machines.md#coal-fires): one Coal gives 20 loaded seconds of heat and each item cooks in 10 seconds, so two items per Coal. Unloading pauses work; partial cooking and heat are saved. Breaking the stove drops its inventory and the stove.

| Input | Output | Default edible energy |
| --- | --- | --- |
| Raw beef, pork, chicken, mutton, rabbit, cod or salmon | The cooked food | Calorie catalog |
| Potato / kelp | Baked potato / dried kelp | 400 / 50 kcal |
| Bread Dough | Bread | 300 raw → 500 baked |
| Cookie Dough | Cookie | 25 raw → 100 baked |
| Unbaked Pumpkin Pie | Pumpkin pie | 200 raw → 600 baked |
| Cake Batter | Cake | Batter is not edible; cake keeps its slice rules |

The familiar bread, cookie, pie and cake crafting recipes make these preparations in the same quantities; cake preparation returns the three buckets. Raw dough, raw foods and foraging keep subsistence possible without fuel.

Furnaces, smokers and campfires cook no food. Burning animals, including fire-aspect kills, drop raw meat and fish. Villagers do not sell cooked or baked foods or make bread at composters. Cold crafting (bowl recipes, golden foods), finite loot, existing food and placed cakes are unchanged.

## Prototype Stove

A cast-iron stovetop on an open steel stand with one removable skillet, a rotary heat dial and four ember vents. Get it from the Civilization creative tab or `/give @s civilization:prototype_stove`; there is no Survival recipe. Place it under cover so its fire can be lit.

1. Open the stove and add **2 raw potatoes, 2 carrots and 1 bread**, plus Coal.
2. Click **Load batch**, then the striker to light the fire ([ignition rules](machines.md#coal-fires)).
3. Drag the **round dial** to any heat; left/right arrows adjust it finely. Close the menu to watch and listen to the pan.
4. While looking at the stove, **crouch + scroll** adjusts heat; add the sprint key for fine steps. The physical pointer turns with the setting.
5. **Crouch + right-click with both hands empty** lifts the skillet with its food and paid heat. Use the held skillet on top of a solid block to rest it, or on an empty Prototype Stove to return it. Each supporting block holds one skillet.
6. **Right-click a resting skillet with an empty main hand** to serve four portions into your inventory (overflow drops beside you); crouch + right-click with both hands empty lifts it again. The stove menu's **Serve** puts four portions in its output slot. Loading needs a mounted empty skillet and an empty output slot.

Food color moves from pale through golden to brown and charred. A continuous frying hiss plays during cooking; a crisp crackle blends in over 600–800 work ticks and holds across the best-quality plateau; after 1,000 ticks a coarse sputter replaces it, fully by 1,400. The three loops keep constant combined loudness, which follows stored pan heat; they keep a gentle left/right cue (at most about 27° from center) and fade linearly to silence at 16 blocks. There is no quality number, countdown or ready lamp.

### Heat and quality

The dial supplies `dial^0.7` paid heat units per tick. The skillet's reserve holds at most **160 work-tick units** and releases `reserve / 160` units into cooking each tick, gaining only heat the fire actually supplies, so a cold pan approaches the dial setting with an eight-second response and turning down changes the trend rather than stopping at once. Zero dial stops new heat without putting out the fire. Quality peaks on a plateau from **800 to 1,000 work ticks** with smooth curves either side; serving locks in the **present** result. Serving before 400 ticks is refused, and progress caps at 1,800.

| Dial | Reach the plateau | Time on the plateau |
| --- | ---: | ---: |
| 30% | About 101 seconds | About 23 seconds |
| 60% | About 65 seconds | About 14 seconds |
| 100% | About 48 seconds | 10 seconds |

These assume a cold skillet and uninterrupted cooking at 20 TPS; a warm skillet starts faster. Productive coal follows supplied heat with a saved fractional remainder: reaching the plateau from cold spends about 2.17, 2.28 and 2.40 Coal budgets at 30%, 60% and 100%, part of which stays in the skillet. Heat is paid once, so moving cookware never creates fuel credit. Without coal, stored heat simply dissipates. Both stoves warm the room ([heat sources](heat.md#sources)).

The batch reserves its ingredients once. Its calorie ceiling is set at loading from the configured values of **two baked potatoes, two carrots and one bread** (1,600 kcal by default), and each of the four portions receives 80–100% of a quarter of it by quality: **320–400 kcal** by default. Portions keep their values through storage and trade.

### Resting

A fully warmed skillet holds at most eight seconds of full-heat cooking; lower settings store less. Off the stove the reserve decays smoothly while the food keeps changing and the sounds fade with it, including after the fire goes out and while carried in either hand. Taking the pan off near golden lets it finish gently; slightly early removal leaves a batch that can go back for more heat. Stored or dropped skillets settle elapsed world-time cooling when next handled, so a chest cannot freeze their heat. Unloaded stoves and resting skillets do not cook.

Lifting frees the stove for another empty skillet (`civilization:skillet` from Creative). Removing the supporting block drops the actual skillet with its contents. Breaking the stove drops its inventory and skillet, keeping unfinished food rather than refunding ingredients. Dial, mounted skillet, batch, calorie ceiling, progress, skillet heat, fuel and fractional spending are saved; finished output is served once.

### Meal benefit

An eaten portion gives a **10–15% discount on active work calorie costs** for **30 online minutes**, scaled by quality. Mining, placing, movement and ascent, rowing, jumping, melee, ignition and fertilizer labor use it; sleep, natural healing, hunger effects, cold exposure and machine fuel do not. Eating another portion replaces its strength and refreshes its duration without stacking. The benefit survives logout, restart and death; offline time does not count down. Eating shows strength and duration, and meal tooltips show quality, calories and benefit.

One dish, one heated position and one pan per resting block; no cooking XP, spills or cooking aboard moving vessels.

## Baking Oven

A physical preview: Civilization creative tab or `/give @s civilization:baking_oven`. Place it on a solid 2×2 base with three blocks of clear space above; it faces you and rejects obstructed or unauthorized placement. The cast-iron body fits in 2×2×2 blocks and stands 1.8 blocks high, with a rear flue to about 2.3 blocks. Leave room in front for the door.

- Empty-hand right-click opens or closes the main door; the leaf swings outward over 0.8 seconds and the window is transparent.
- Crouch + scroll while looking at it turns the continuous draft dial (sprint key for fine steps); the pointer moves, the action bar shows the percentage and the setting is saved.
- The shell and closed door have cached collision; opening clears the chamber entrance.
- All occupied cells belong to one oven: breaking any part removes it and drops one oven in Survival.

The chamber holds a fixed rack and the firebox door is fixed. The oven has no fuel, heat, tray handling, recipes or meals.
