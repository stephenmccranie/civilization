# Civilization — implemented build

**0.10.1-dev · Minecraft 1.21.1 · NeoForge 21.1.250 · JDK 21**

This file describes the working mod. [Game rules](../design_direction.md) describe the intended game; [development plan](../development_plan.md) separates shipped work from future features.

All custom items are in the **Civilization** creative tab, marked with the kiln icon: machines first, then ingredients and foods. New items in the mod's namespace are included automatically. They also remain discoverable through creative search.

## Development

Local settings in ignored `dev.local.json` provide `javaHome` and `prismInstance`; use `dev.local.example.json` as the template. The configured Prism instance is Civilization Dev. Its game directory is `minecraft`.

Run from this folder:

```powershell
.\dev.ps1 Build
.\dev.ps1 Deploy
.\dev.ps1 Client
.\dev.ps1 Server
.\dev.ps1 GameTest
```

Build runs unit tests. Deploy selects the versioned JAR from `gradle.properties`, refuses a running matching Prism game, archives previous Civilization JARs under instance `mod-backups`, copies the new JAR and verifies its hash. Client uses `run`, Server uses `runs/server`, and GameTest uses `runs/gametest`; all are separate from Prism saves. The previous test world in `run/world` is left in place.

`Server` copies `server.local.example.properties` only if no local server properties exist. It binds to localhost:25566, with online authentication and eight slots for development. Run it in an interactive terminal and type `stop` to shut down. NeoForge's development runtime bypasses the EULA prompt; no production EULA file is accepted by this script. A packaged/public server still needs owner-controlled EULA and hosting setup. Git now records the pre-update checkpoint and development changes.

For GameTests, check the explicit all-required-tests-passed output, not merely the process exit code. Back up saves before changing mods/configuration.

## Calories and recovery

One server-owned calorie reserve replaces vanilla hunger, saturation and exhaustion. Tunable current defaults:

| Rule | Value |
| --- | ---: |
| Capacity / initial reserve | 2,400 kcal |
| Walking/crouching | 0.10 kcal per block |
| Sprinting | 0.30 kcal per block |
| Break ordinary block / light vegetation / crop | 4 / 0.25 / 1 kcal |
| Place block / plant crop / apply fertilizer | 2 / 1 / 1 kcal |
| Jump | 2 kcal plus horizontal movement |
| Successful melee attack | 3 kcal |
| Natural healing | 40 kcal per half-heart, every 4 seconds |
| Minimum reserve to sprint | 100 kcal |
| Hunger effect | 2 kcal/second per level |
| Depletion recovery threshold | 200 kcal |

No idle/offline drain. Calories and depletion persist through death, dimension changes and relog. At zero, basic actions remain possible without debt, mining is 25% speed, sprinting/natural healing stop, and starvation alone stops at three hearts. Other damage remains dangerous. Enough food to reach 200 kcal clears depletion.

For emergency food: empty both hands, crouch-right-click eligible natural ground and stay still/crouched for 10 seconds. Receive a 25-kcal morsel. Movement, equipment changes or interruption cancel; each attempt is deliberate. Foraging is recovery, not efficient normal provisioning.

Natural healing requires sufficient calories, a non-depleted state and `naturalRegeneration`. Magical healing retains its effects. Saturation grants no energy. Peaceful does not refill food/health freely. Creative/Spectator/invulnerable players are exempt from costs.

Movement uses accepted server displacement. Swimming/climbing are accounted for; mounts, elytra, teleports and abnormal large deltas are not walking charges. Tool transformations, buckets, ranged actions and automated/fake-player work still need explicit coverage. This is not yet universal action accounting.

The ten-segment calorie HUD fills from right to left, includes a number/threshold feedback and hides with F1. Food tooltips show kcal; food effects and eating animations remain. Excess food above capacity is discarded. All vanilla foods have explicit defaults; server overrides take precedence. Unknown mod food uses nutrition × 100 as a temporary fallback.

World settings: `serverconfig/civilization-server.toml`. Stop the world before editing. Costs, thresholds and food values are configurable and synchronized to clients. `/civilization status` and `/civilization calories` inspect state; operator calorie-setting/reset commands are for development.

## Farming and production

| Process | Implemented result |
| --- | --- |
| Mature ordinary wheat | 1 wheat + 2 seeds |
| Mature fertilized wheat | 3 wheat + 2 seeds |
| Fertilizer application | One dose on growing unfertilized wheat; preserves age, no stacking, mature crops rejected |
| Field ration | 3 bread → 1 ration; exactly three times configured bread kcal, normally 1,500 |
| Raw Mineral Blend | 1 clay ball + 1 gravel → 1 blend |
| Kiln | 1 clay block → 4 brick items in 200 ticks |
| Fertilizer Works | 1 blend → 4 fertilizer in 400 ticks |
| Mineral Coal | 1,600 loaded ticks per piece in either machine |

Bread defaults to 500 kcal, cooked beef/pork 700. Crop seeds support replanting; fortune does not multiply the custom wheat yields. Replanting restores ordinary wheat. The clay/gravel feedstock is provisional.

Mineral Coal replaces coal ore drops and ore smelting/blasting output, retaining fortune/silk behavior. It converts one-way to ordinary coal. Custom machines accept Mineral Coal only. Vanilla brick smelting and villager brick sales are disabled; existing stock and finite salvage remain. Other vanilla fuel/resource paths still exist.

Ordinary 64-stacks now hold 32, including custom items; existing 16-stacks and unstackable items retain their limits. One log/wood/stem/hyphae block makes one plank, including stripped variants; one bamboo block also makes one plank. No oversized-stack migration is provided, as requested: older over-limit stacks may clamp when handled by containers. Recipes for slabs and other partial blocks are unchanged.

Bone meal cannot grow crops, trees, land or underwater vegetation. Manual use explains the rule; dispensers and villagers cannot apply it. Bone meal can still exist and be used in crafting. Mineral Fertilizer continues to work. Crops still have no regional gate.

## Household cooking

The **Cooking Stove** is a single block, with no shell or building preview. Craft it with three copper ingots across the top, cobblestone / furnace / cobblestone in the middle, and three cobblestone across the bottom. It uses the familiar input, fuel and output layout. Inputs enter from above, fuel from the sides, and outputs leave below via hoppers.

Only **Mineral Coal** is accepted. One piece supplies 80 loaded seconds of heat; each item cooks in 10 seconds, giving eight items if kept working. Already-lit fuel burns while idle or blocked. New coal does not ignite without a valid input and output space. Unloading pauses work; partial cooking and heat survive saving. Breaking the stove drops its inventory and the stove item.

| Input | Output | Default edible energy |
| --- | --- | --- |
| Raw beef / pork / chicken / mutton / rabbit / cod / salmon | Corresponding cooked food | Existing calorie catalog |
| Potato / kelp | Baked potato / dried kelp | 400 / 50 kcal |
| Bread Dough | Bread | 300 raw → 500 baked |
| Cookie Dough | Cookie | 25 raw → 100 baked |
| Unbaked Pumpkin Pie | Pumpkin pie | 200 raw → 600 baked |
| Cake Batter | Cake | Batter is not edible; cake retains its slice rules |

The familiar bread, cookie, pie and cake crafting ingredients now produce the uncooked preparation in the same quantities. Cake preparation returns the three buckets. Food values remain configurable through the calorie overrides. Three baked bread still pack into one 1,500-kcal field ration without adding energy. Raw dough, raw foods and hand-foraging keep subsistence available without fuel.

Food recipes in furnaces, smokers and campfires are disabled. Burning animals drops raw meat/fish, including fire-aspect kills; villagers cannot sell these cooked/baked foods or manufacture bread at composters. Unrelated smelting, cold crafting (including bowl recipes and golden foods), finite loot, already-existing food and placed cakes remain. This is the household cooking boundary, not the complete future audit of vanilla fuels, loot, livestock and automation.

The stove and preparations currently reuse vanilla textures. Their names and tooltips identify the new use; bespoke food art is later work.

## Multiblocks

Craft a kiln controller from a furnace surrounded by eight cobblestone. Craft the Fertilizer Works controller from four brick blocks, four copper ingots and a furnace in the alternating pattern.

Additional shells: kiln uses 25 cobblestone/stone bricks; Fertilizer Works uses 21 brick blocks and 5 full copper blocks. Both have a hollow chamber; a downward input hopper can replace a specified wall position. See [multiblock layouts](multiblock-builds.md) for exact placement and ports.

Controllers show required materials and actual block textures as translucent placement previews. Correct positions disappear. Contextual HUD/guides show only while looking at the unfinished area; no Build screen or G toggle. Held accepted block variants appear in the preview.

Lit heat burns while idle/blocked; new fuel ignites only for valid work with full output room. Unloaded machines pause without forced loading. A broken/blocked structure extinguishes heat and unfinished work while preserving inventory. Removing the controller drops its inventory; shell blocks remain. Registry IDs and `retort_*` audit names remain stable despite the Fertilizer Works display name.

All three machines now distinguish heat from active work. Firelight and quiet crackle follow remaining heat; smoke (industrial machines) or steam (stove) follows actual processing. Ignition and completion have brief nearby sounds. Menus report working, missing ingredients/fuel, wrong input, blocked output or ready to collect. Incomplete industrial structures retain their construction feedback. Client particles are local and use normal particle settings; sounds use the Blocks volume setting. Dense-factory audiovisual tuning is not yet play-verified.

## Audit and verification

World-local `civilization-energy/energy-current.jsonl` contains player calorie/production events and separate machine records. Movement is aggregated; machine records do not invent player attribution. The asynchronous journal is bounded (8,192 queued entries, 16 MiB files, eight backups) and best effort. Crash/overflow/disk failures can lose records. It is not a persistent inventory/payment/ownership ledger.

Verification for 0.10.0: 47 GameTests pass, including actual inventory splitting, all log variants, manual/dispenser bone meal, cooking recipes and villager/fire bypasses, coal budgets, real hopper transport, output limits, save/reload and block drops. Existing calorie/recovery/farming/multiblock cases remain in that suite. Build runs the 19 unit tests. The development client loads its resource and sound atlases without mod-specific errors; the separate dedicated development server reaches ready on localhost:25566. This update's balance and audiovisual treatment still need normal play feedback; startup and automated results do not establish multiplayer correctness or 200-player capacity.

Older version details: [farming release note](farming-prototype.md), [kiln release note](kiln-prototype.md), [industry release note](industry-progression.md). Their tests and pending-work statements describe those releases. Pre-consolidation full documentation is [archived](../docs/archive/2026-09-10-pre-consolidation/README.md).

No regional system, land controller, groups, shops, recovery container, map or vehicle implementation exists yet. Dedicated multi-client behavior and the 200-concurrent target remain unverified.
