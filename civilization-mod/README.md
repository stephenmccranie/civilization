# Civilization — implemented build

[Current build and validation](../docs/status.md) · [Runtime versions](gradle.properties)

This file describes the working mod. [Game rules](../design_direction.md) describe the intended game; [development plan](../development_plan.md) separates shipped work from future features.

All custom items are in the **Civilization** creative tab, marked with the kiln icon: machines first, then ingredients and foods. New items in the mod's namespace are included automatically. They also remain discoverable through creative search.

Town manufacturing: [Tannery, Textile Workshop and Smithy](industry.md#town-workshops) connect hides, wool and Foundry metals to equipment production and repairs.

The [stove cooking prototype](cooking.md) adds one exposed skillet, a continuous heat dial, evolving color/sound cues, and quality-dependent calorie/work benefits. Ingredient loading and serving happen at the stove. Obtain **Prototype Stove** from Creative or `/give @s civilization:prototype_stove`; the full kitchen multiblocks remain future work.

[Bulk freight storage](industry.md#bulk-freight-storage) adds a coal bunker and single-liquid cargo tank for land industry, with visible contents, construction guides and local hopper/pipe access.

Shared [physical properties and inspector](physics.md) expose material mass, heat capacity, geometry and the Sable boundary. The [Airship Controller prototype](vehicle-physics.md#airship-prototype) assembles player-built flying machines and accepts typed numeric test power. Optional [speed-adaptive DH detail](vehicle-physics.md#speed-adaptive-distant-horizons-detail) reduces distant rendering work during fast travel and restores quality after slowing down.

[JEI processing recipes](industry.md#jei-recipe-discovery) make machine production, repairs, refining, cutting and canister transfers discoverable alongside ordinary crafting.

## Development

GeckoLib is now a required pinned runtime dependency alongside Sable. Gradle resolves it for development; Deploy copies the exact resolved JAR and verifies its hash, refusing a conflicting installed GeckoLib version. Blockbench and MCP are authoring-only tools: [setup and model workflow](../tools/modeling/README.md). Existing engine art remains unchanged while that workflow is established.

JEI is an optional compile-only integration. Gradle resolves the version in `gradle.properties` from the JEI Maven repository, or uses the matching `.dev-libs/jei-<minecraft>-neoforge-<version>.jar` for offline development. Do not bundle this jar in Civilization. For the `jei` visual scene, put the matching JEI jar in ignored `run/mods`; the visual runtime copies it normally. Deployment leaves the user's installed JEI untouched.

Local settings in ignored `dev.local.json` provide `javaHome` and `prismInstance`; use `dev.local.example.json` as the template. The configured Prism instance is Civilization Dev. Its game directory is `minecraft`.

Run from this folder:

```powershell
.\dev.ps1 Build
.\dev.ps1 Deploy
.\dev.ps1 Client
.\dev.ps1 Server
.\dev.ps1 GameTest
.\dev.ps1 Verify
.\dev.ps1 Verify -Scope Gameplay
.\dev.ps1 Verify -Scope Visual -Scene modular
```

Build runs unit tests. Deploy selects the versioned JAR from `gradle.properties`, refuses a running matching Prism game, archives previous Civilization JARs under instance `mod-backups`, copies the new JAR and verifies its hash. Client uses `run`, Server uses `runs/server`, and GameTest uses `runs/gametest`; all are separate from Prism saves. The previous test world in `run/world` is left in place.

Use the [change-specific testing workflow](testing.md). `Verify` defaults to the fast build; `-Scope Gameplay`, `Visual`, or `Full` adds only the requested runtime checks in one Gradle invocation. Visual checks default to one focused daylight scene; the longer day/night/outline tour requires `-FullVisual`. Successful checks are not repeated without a relevant change.

`Server` copies `server.local.example.properties` only if no local server properties exist. It binds to localhost:25566, with online authentication and eight slots for development. Run it in an interactive terminal and type `stop` to shut down. NeoForge's development runtime bypasses the EULA prompt; no production EULA file is accepted by this script. A packaged/public server still needs owner-controlled EULA and hosting setup. Git/deployment status is recorded in the project status document; deployment does not create a commit.

For GameTests, check the explicit all-required-tests-passed output, not merely the process exit code. Back up saves before changing mods/configuration.

## Source map

| Location | Responsibility |
| --- | --- |
| `src/main/java/dev/civilization/` | Registration, server rules, calories, farming, machines, cutting, clock setup and journals |
| `src/main/java/dev/civilization/client/` | HUD, menus, previews and baked rendering |
| `src/main/java/dev/civilization/mixin/` | Narrow vanilla integrations; native APIs take priority where available |
| `src/main/resources/` | Assets, recipes, language, data and mixin declarations |
| `src/gameTest/` | Dedicated gameplay tests and development-only hidden visual fixtures |
| `src/test/` | Fast unit checks |
| `../art/textures/` | Authoring pipeline and provenance; runtime exports go to resources |

Keep runtime/development-only hooks out of the shipped JAR. Source layout stays stable during documentation cleanup; feature references explain behavior without duplicating code.

## Day and night

A complete Overworld day/night cycle defaults to **60 real minutes at 20 TPS**, with vanilla daylight/twilight/night proportions. NeoForge's native fractional clock runs at one-third speed; simulation time still advances normally. Machines, movement, crop random ticks and other tick timers keep their existing rates. Sun/moon lighting, daily schedules and moon phases follow the longer day. Server lag stretches real duration as it does in vanilla.

The default is applied once per world, without changing the current time or enabling a paused daylight cycle. Existing explicit NeoForge clock settings are respected. NeoForge saves and synchronizes the rate; no per-tick correction loop, clock-rewinding, custom network packets or new mixins are needed. Sleeping and `/time set` retain their normal behavior.

Operators can inspect `/neoforge day` or change the full cycle using `/neoforge day length set 60`. `/neoforge day length set default` restores vanilla's 20-minute cycle. Later admin changes survive reloads. The small `civilization_world_defaults` saved-data marker prevents reapplying the initial default.

## Ownership and trade

Craft a Land Controller to protect a bounded area with Coal, or a Trade Counter for item-for-item offers with two ghost selectors and one shared chest inventory. Payment defaults to Coal, but any item can be used. Numeric fields set exact per-click trade batches, including multiple stacks. Both blocks have original 32×32 texture families and face the player when placed. Claims share full access through owner-managed username whitelists; no formal groups are needed. Takeovers hold payment for a one-week handover and refund the buyer if claim power fails. See [ownership and trade](ownership-trade.md) for chest-style coal storage, three claim tiers, recipes, whitelist controls and current protection limits.

## Deposits and oil industry

Kiln bricks now unlock the coal-fired Foundry. Smelt metals, turn iron into steel, and craft steel/copper Machine Parts to construct T2 machinery. Vanilla metal smelting is disabled; catalog equipment production and repairs use the town workshops described in the industry reference. Damageable gear now receives a percentage grade; Smithy repairs lower that grade, while black unavailable capacity and an inline cyan bonus at the right end of the durability bar reveal grades below or above 100%. Prospect for finite regional oil fields and dense coal seams. Their fresh-world placement now checks surface biomes, dry ground, relief and buried geology, including under Tectonic terrain. The coal-powered wooden Oil Derrick and Fired Heater feed a tall Distillation Column and Air-Cooled Condenser through pipes. Outputs are refined fuel, lubricating oil and fertilizer sulfur; Coal Drills spend fuel and benefit from lubrication while removing actual dense coal blocks. Oil reservoirs exist underground and visibly lower as pumps extract their fluid; marker-only invisible reserves have been replaced. Build the riveted steel vessels with the existing guides (Industrial Casing, Cooling Grilles and Refinery Flues), connect their flanged ports and Liquid Tanks with sight-glass pipes, or use canisters for portable finished liquids. New worlds are the normal target; the New Surv save has an [opt-in oil backfill](industry.md#find-a-site) for its already generated chunks. See [industry](industry.md) for recipes, grade behavior, exact yields, site reach, liquid handling and current limitations.

## Frontier uranium and accessories

Rare [Uranium Ore](geography.md#frontier-uranium-and-accessories) generates in rocky, upland and old-growth frontier biomes of new worlds, between Y=16 and Y=192. An iron-or-better pickaxe yields one portable Raw Uranium. The Geiger Counter is crafted from glass, copper, redstone and Machine Parts; equip it in any of the three accessory slots to the right of the player portrait, above the offhand slot. It clicks faster within 48 blocks of nearby loaded ore. It reveals no coordinates, consumes no fuel and does not detect ore in unloaded chunks. Exposed ore, uranium held by a player and nearby chests storing Raw Uranium or Uranium Ore emit occasional fine silver tracks that travel roughly 2½–3½ blocks outward from their edges. Launched trails stay in place as their sources move and taper at their tips. These cues are visual only. The accessory slots take wearable accessories only, keep their contents across ordinary saves and dimension changes, and follow normal survival death drops (or keep-inventory). Raw Uranium has no processing or power use yet.

## Geography

Natural river bands govern crop growth, and woodland grows trees faster. Farmland averages two hours to harvest when rain-soaked and four when river-fed; regional rain, soil storage, all covered harvests and Rain Caller controls are in [weather and farming](weather.md). Crouch-use a hoe on ground to inspect it, or run `/civilization geography`. Existing crops remain intact; unsuitable ground cannot consume fertilizer. See [geography](geography.md) for eligible plants, tunable distances/heights, existing-world behavior and limits.

## Calories and recovery

One server-owned calorie reserve replaces vanilla hunger, saturation and exhaustion. Tunable current defaults:

| Rule | Value |
| --- | ---: |
| Capacity / initial reserve | 2,400 kcal |
| Walking/crouching | 0.10 kcal per block |
| Sprinting | 0.30 kcal per block (3× walking) |
| Rowing a vanilla boat/chest boat | 0.05 kcal per horizontal block while paddling |
| Break ordinary block / light vegetation / crop | 4 / 0.25 / 1 kcal |
| Place block / plant crop / apply fertilizer | 2 / 1 / 1 kcal |
| Place cut slab / quarter beam / eighth cube | 1 / 0.5 / 0.25 kcal |
| Sleep | 50 kcal per in-game hour actually slept, including skipped hours |
| Jump | 2 kcal plus horizontal movement |
| Successful melee attack | 3 kcal |
| Accepted coal-machine ignition strike | 1 kcal; failed spark still costs labor |
| Natural healing | 40 kcal per half-heart, every 4 seconds |
| Minimum reserve to sprint | 100 kcal |
| Hunger effect | 2 kcal/second per level |
| Cold exposure | Up to 0.20 kcal/second at or below 35.6°F felt temperature; begins below 64.4°F |
| Depletion recovery threshold | 200 kcal |

Successful cut-piece placement uses the same configurable full-block placement rate multiplied by piece volume; native slabs also count as half blocks. Sleep costs the configured `sleepKcalPerGameHour` for in-game time spent in bed and for hours advanced when the night skips. It is charged on waking, has no offline drain, and appears as `sleep` in the per-player energy journal. Other labor costs retain their temperature comfort modifier.

Ordinary level-ground travel now settles at **3.2 blocks/s walking** and **4.6 blocks/s sprinting** at 20 TPS. Sneaking retains its proportional slowdown. Jump height remains native. Airborne acceleration follows the adjusted walking/sprinting gait and the forward sprint-jump impulse is tuned for a 20% speed bonus. Repeated sprint jumping measures about **5.52 blocks/s**, roughly **20% faster than sprinting** on ordinary level ground. This is a modest momentum bonus, not a hard velocity cap. Ice, slopes, effects, equipment and server tick rate can alter these figures. Creative flight and vehicles retain their own controls/speeds; mobs keep their existing speeds. Walking and sprinting have fixed per-block calorie rates of 0.1 and 0.3 respectively; jumps separately cost 2 kcal. Existing player walking abilities update on load.

**Street paving:** craft 2 Street Pavers from 1 vanilla brick block and 1 stone-brick block; the recipe preserves block volume and depends on Kiln brick production. Street Pavers, their slabs/stairs, and exposed cut-piece surfaces give **+25% ground movement speed**: about **4.00 blocks/s walking** and **5.75 sprinting**. Ordinary red bricks, their slabs/stairs, and plain stone bricks no longer qualify. Mixed cut assemblies check the actual supporting surface; buried paving does not help. The pavers' worn running-bond top is one world-aligned 8×8-block painting at 64 pixels per block; matching sides and item faces come from that painting. The bonus modifies ground acceleration without changing friction, jump height, FOV or calorie cost per block. It adds no airborne, swimming, vehicle or creative-flight buff; existing momentum naturally carries off the road. The block tag `civilization:road_paving` owns eligible materials.

Travel costs **distance × the gait’s rate**: 100 blocks costs **10 kcal walking**, **30 kcal sprinting**, or **5 kcal rowing**, whether moving slowly or quickly. Speed bonuses never multiply the per-block charge. Ordinary movement and rowing count horizontal distance; swimming counts 3D distance. Only the controlling player of a vanilla boat or chest boat pays rowing calories while its paddles are active; passengers and drifting boats do not. Powered Civilization vessels are not rowboats and remain excluded. All upward personal travel additionally costs 1 kcal per vertical block, independent of walking/sprinting, including stairs, ladders, swimming and the rising portion of jumps. Descending refunds nothing. Jumping still separately costs 2 kcal, on top of ascent. Other riding, creative flight, elytra travel and teleports retain their existing exclusions. `walkKcalPerBlock` defaults to 0.1, `sprintMultiplier` to 3, `rowKcalPerBlock` to 0.05, and `ascentKcalPerBlock` to 1; warmth discounts travel. Rowing has its own distance counter and `row` entry in the energy journal. The sprint setting is restored for worlds that loaded the brief shared-rate version.

There is no baseline idle or offline drain. While heat simulation is enabled, cold felt temperature adds a bounded passive cost once per online second in Survival. Felt temperature follows torso air over several seconds rather than changing instantly. It rises quadratically from zero below 64.4°F to 0.20 kcal/s at 35.6°F or colder (0.05 kcal/s at 50°F), and is zero when warm, heat is disabled, or the player is Creative/Spectator. Flames and shelter reduce the cost by warming that air and then the player; there is no separate proximity bonus. Cold does not create calorie debt at zero. The `cold_exposure` action appears in the per-player energy journal. Calories and depletion persist through death, dimension changes and relog. At zero, basic actions remain possible without debt, mining is 25% speed, sprinting/natural healing stop, and starvation alone stops at three hearts. Other damage remains dangerous. Enough food to reach 200 kcal clears depletion.

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
| Fertilizer application | One dose on an immature covered crop or detached fruit stem; yield only, no stacking; see [weather and farming](weather.md) |
| Field ration | 3 bread → 1 ration; exactly three times configured bread kcal, normally 1,500 |
| Sulfur Blend | 1 sulfur + 1 gravel → 1 blend |
| Kiln | 1 clay block → 4 brick items in 200 ticks |
| Fertilizer Furnace | 1 Sulfur Blend → 8 fertilizer in 400 ticks |
| Coal | 400 loaded ticks per piece in either machine |

Bread defaults to 500 kcal, cooked beef/pork 700. Cows, mooshrooms, pigs, hoglins, sheep, chickens and rabbits now drop one raw meat each, without a Looting meat bonus; hide, wool and other drops are unchanged. This reduces wild meat supply without changing calories per serving. Crop seeds support replanting; fortune does not multiply governed mature yields. Replanting clears fertilizer but retains soil water. Fertilizer requires refinery sulfur and gravel. The clay/gravel route and firing old Raw Mineral Blend are removed; existing old blend remains an inert legacy item. Feed-supported husbandry yield is still deferred (development plan E05).

Coal (`civilization:mineral_coal`) is the single supported coal item for machines, claims and ordinary crafting. Ore drops preserve Fortune/Silk Touch; silk-touched coal ore is processed in the Foundry. Finite vanilla coal loot yields this item, while wither skeletons no longer drop coal. Vanilla coal, charcoal and coal blocks have no production/conversion recipes and are hidden from creative listings. Existing legacy stock is not converted. See [thermal processing and retired vanilla systems](industry.md#thermal-processing-and-retired-vanilla-systems).

Ordinary 64-stacks now hold 32, including custom items; existing 16-stacks and unstackable items retain their limits. One log/wood/stem/hyphae block makes one plank, including stripped variants; one bamboo block also makes one plank. No oversized-stack migration is provided, as requested: older over-limit stacks may clamp when handled by containers. Partial blocks use the integrated [cutting and recombination system](cutting.md); conflicting two-slab decorative recipes use their full-block equivalent.

Bone meal cannot grow crops, trees, land or underwater vegetation. Manual use explains the rule; dispensers and villagers cannot apply it. Bone meal can still exist and be used in crafting. Mineral Fertilizer works on eligible river farmland; see [geography](geography.md) for regional growth rules.

## Household cooking

The **Cooking Stove** is a single block, with no shell or building preview. Craft it with three copper ingots across the top, three cobblestone in the middle, and three cobblestone across the bottom. It uses the shared cabinet layout: two ingredient slots, two fuel slots and two output slots. Inputs enter from above, fuel from the sides, and outputs leave below via hoppers.

Only **Coal** is accepted. One piece supplies 20 loaded seconds of heat; each item cooks in 10 seconds, giving two items if kept working. Manually light it with the controller striker; it automatically feeds Coal afterward and burns at 10% speed while idle or blocked. Exhaustion requires manual relighting. See [continuous coal fires](industry.md#continuous-coal-fires). Unloading pauses work; partial cooking and heat survive saving. Breaking the stove drops its inventory and the stove item.

| Input | Output | Default edible energy |
| --- | --- | --- |
| Raw beef / pork / chicken / mutton / rabbit / cod / salmon | Corresponding cooked food | Existing calorie catalog |
| Potato / kelp | Baked potato / dried kelp | 400 / 50 kcal |
| Bread Dough | Bread | 300 raw → 500 baked |
| Cookie Dough | Cookie | 25 raw → 100 baked |
| Unbaked Pumpkin Pie | Pumpkin pie | 200 raw → 600 baked |
| Cake Batter | Cake | Batter is not edible; cake retains its slice rules |

The familiar bread, cookie, pie and cake crafting ingredients now produce the uncooked preparation in the same quantities. Cake preparation returns the three buckets. Food values remain configurable through the calorie overrides. Three baked bread still pack into one 1,500-kcal field ration without adding energy. Raw dough, raw foods and hand-foraging keep subsistence available without fuel.

Food recipes in furnaces, smokers and campfires are disabled. Burning animals drops raw meat/fish, including fire-aspect kills; villagers cannot sell these cooked/baked foods or manufacture bread at composters. Nonfood heat recipes now use the Kiln/Foundry; cold crafting (including bowl recipes and golden foods), finite loot, already-existing food and placed cakes remain. This is the household cooking boundary, not the complete future audit of vanilla fuels, loot, livestock and automation.

The stove and food preparations use original 32×32 textures.

## Multiblocks

Craft a kiln controller from nine cobblestone in a full 3×3 grid. Craft the Fertilizer Furnace controller from four brick blocks, four copper ingots and cobblestone in the center of the alternating pattern.

Additional shells: kiln uses 13 stone blocks, 4 slabs, 4 beams and 4 eighth cubes; Fertilizer Furnace uses 13 bricks, 4 brick slabs, 4 brick beams, 1 copper slab and 4 copper eighth cubes. Both have a hollow chamber; a downward input hopper can replace a specified wall position. See [multiblock layouts](multiblock-builds.md) for exact placement and ports.

Controllers show required materials and actual block textures as translucent placement previews. Correct positions disappear. Contextual HUD/guides show only while looking at the unfinished area; no Build screen or G toggle. Held accepted block variants appear in the preview.

Previews draw after the world's shader effects with consistent brightness and adjustable transparency. They test world depth but never write it. Local settings persist in `config/civilization-client.toml`: `/civilization preview textured` (default), `/civilization preview outline` (fallback), and `/civilization preview opacity 0.32` (range 0.05–0.60). No server permission is needed. The guides intentionally do not inherit shader-pack lighting. Multiblock textures retain the full shape while outlines hide behind nearer guide surfaces. The directly aimed requirement is highlighted in cyan; real blocks block its selection.

### Texture direction

New Civilization artwork targets **64×64 pixels per standard block face and item icon**. Street Pavers are the first 64×64 family; previously shipped 32×32 families remain until reviewed and migrated. Existing special-purpose atlases/UI use their own declared dimensions. Faithful 32x is the companion pack for vanilla assets; custom artwork belongs in the mod's `civilization` namespace and must not depend on Faithful being installed. Photon remains the primary shader target.

Use readable silhouettes, restrained material palettes and deliberate pixel clusters. Ground-level industry uses stone, soot, iron and worn copper; food preparations must look distinct from their cooked outputs. Item icons use transparent backgrounds. Block faces use flat, orthographic textures with consistent edges; active machine variants preserve the same structure and change only working details. Avoid baked-in bloom or dramatic shadows that conflict with world lighting.

Image generation supplies original artwork and visual exploration. Every shipping texture must then be checked against its declared native dimensions, clean alpha, pixel readability at inventory size, and appearance on its in-game model. An enlarged pixel-art concept is not a finished texture. Keep generation prompts and source artwork outside runtime assets; package only verified game textures.

The food/preparation family now uses original 32×32 sprites with pale raw dough, a terracotta batter bowl, shallow unbaked pie, cloth-wrapped bread ration and woodland morsel. The early machine trio also has original 32×32 artwork. Each machine uses a unified front/side/top/hot texture family derived from one master image. The kiln references Minecraft's cobblestone directly, so its casing follows the active resource pack and matches its shell; Fertilizer Furnace uses copper and iron, and the Cooking Stove uses dark iron. Artwork sources, exact generation prompts and export workflow are in [texture artwork](../art/textures/README.md). Bowls, tins and ration wrapping in the artwork are visual presentation, not additional recipe ingredients or returned containers.

Run `./dev.ps1 Visual` for one daylight screenshot of assembled machines, or choose a focused `-Scene` from [testing.md](testing.md). Add `-FullVisual` for the extended day/night/outline tour. It uses a disposable world in `runs/visual`, an invisible, unfocused 1920×1080 window, and development-only hooks that prevent mouse grabbing and recentering. A three-minute task timeout bounds failed runs. Screenshots are in `runs/visual/screenshots`. Normal interactive play does not load these hooks, and they are excluded from the release JAR.

### Machine item storage

The normal Survival/Adventure player inventory uses the same tan enamel, dark metal frame and brass corner pins, with inset slots, a recessed live character preview and a brass Crafting label. Its recipe-book backdrop shares the cabinet skin. Armor/offhand hints, the 2×2 crafting grid, native recipe-book controls, status-effect display, item tooltips and click/drag/shift-click retain vanilla behavior. Crafting tables and their recipe-book backdrop use the same skin, preserving the native 3×3 grid, output, recipe controls and item handling. Single and double chests also share the cabinet skin and brass title while retaining their native 27/54-slot layout. Other vanilla row-based containers using the same screen, including barrels and trapped chests, inherit it as well. Creative inventory tabs and specialized containers retain their own layouts. This is a client-side presentation change, with no new slots or gameplay rules.

Current machines and civic controllers use a shared tan/brass cabinet UI. Coal machines have two fuel slots; T1 machines have two material inputs and two outputs except the Smithy, which has four interchangeable ingredient slots and one output. The Coal Drill and Distillation Column retain four item output slots. Finished items remain in their output slots when another slot is emptied. Workshop recipes can run automatically from materials in any input slot or be selected in the searchable drawer; ghosts show missing requirements without becoming inventory. Ambiguous recipes wait for a choice. Liquid gauges retain exact names/amounts on hover. Recipe behavior, heat policies and old-slot migration are detailed in [shared controller interfaces](industry.md#shared-controller-interfaces).

Stone pickaxes recover stone and metal machine controllers, including the Smithy, extraction/refinery machinery, the Oil Engine, land controller and bulk stores. Stone axes recover wooden controllers such as the Trade Counter, Survey Table and boat helm. Industrial casing, pipes and engine pieces also accept stone pickaxes. Stocked bulk-store controllers still require emptying before removal; claim and vehicle protections still apply.

### Shader support

**Photon is our recommended shader pack and primary visual development/test target.** Tune new visual effects for Photon first, while keeping the game usable with shaders disabled. Prefer shared rendering fixes that work across packs; maintain broader compatibility where practical without committing to support every pack or setting. Bliss and Complementary are secondary compatibility checks.

The current tested Photon version is **1.3b**, not a permanent version pin. Visual checks used Iris 1.8.14-beta.1 and Sodium 0.8.13 with Photon 1.3b, Bliss 2.1.2 and Complementary Unbound r5.9. Textured transparency works in the isolated test scene and with shaders disabled; Bliss and Complementary also passed nighttime, outline and completed-build checks. This is a tested baseline, not a guarantee for every shader setting or mod combination; Distant Horizons was not included in these checks.

Manually struck fires automatically feed Coal while lit, with a low idle/blocked burn. Exhaustion requires relighting; see [continuous coal fires](industry.md#continuous-coal-fires). Unloaded machines pause without forced loading. A broken/blocked structure extinguishes heat and unfinished work while preserving inventory. Removing the controller drops its inventory; shell blocks remain. Registry IDs and `retort_*` audit names remain stable despite the Fertilizer Furnace display name.

Lit controller/stove models retain normal ambient shading so the entire casing does not switch to flat lighting when fuel ignites. Fire-chamber textures and normal emitted light remain; surrounding blocks still receive the glow.

All three machines now distinguish heat from active work. Firelight and quiet crackle follow the lit fire; smoke (industrial machines) or steam (stove) follows actual processing. Ignition and completion have brief nearby sounds. Menus report working, missing ingredients/fuel, wrong input, blocked output or ready to collect. Incomplete industrial structures retain their construction feedback. Client particles are local and use normal particle settings; sounds use the Blocks volume setting. Dense-factory audiovisual tuning is not yet play-verified.

## Audit and verification

World-local `civilization-energy/energy-current.jsonl` contains player calorie/production events and separate machine records. Movement is aggregated; machine records do not invent player attribution. The asynchronous journal is bounded (8,192 queued entries, 16 MiB files, eight backups) and best effort. Crash/overflow/disk failures can lose records. It is not a persistent inventory/payment/ownership ledger.

Older version details: [farming release note](../docs/history/releases/farming-prototype.md), [kiln release note](../docs/history/releases/kiln-prototype.md), [industry release note](../docs/history/releases/industry-progression.md). Their tests and pending-work statements describe those releases. Pre-consolidation full documentation is [archived](../docs/archive/2026-09-10-pre-consolidation/README.md).

Regional crop/forestry queries and inspection are implemented. Land controllers, per-claim whitelists and local trade counters are implemented. The 2×2 Survey Table automatically surveys nearby terrain and shows public claims/counter offers on its tabletop and in a closer inspection view; see [ownership and trade](ownership-trade.md). Recovery containers remain unimplemented; the small motorboat is described in [vehicle physics](vehicle-physics.md). Broad dedicated multiplayer acceptance and the 200-concurrent target remain unverified.

Fertilizer Furnace retains its older registry IDs for save compatibility. The kiln layers custom iron ports over the active resource pack's cobblestone. All machine texture families use a shared brighter tone curve to keep cold ports and hardware readable in inventory and world lighting.

## Block cutting

Slabs, beams and cubes now share all eight half-grid cells freely, including mixed materials and non-rectangular shapes. Previews show the added piece, and breaking returns recombinable material pieces. Duplicate generic slab entries are hidden; existing custom halves normalize to vanilla slabs in player inventories.

Craft two matching pieces in any two slots to recombine: eighths → beam → slab → full block. No saw required; the player crafting grid works. Conflicting two-slab decorative recipes now use one full source block.

Stone, iron and diamond saws cut full blocks into slabs (½), slabs into beams (¼), and beams into small cubes (⅛). Every shape uses a 2×2×2 half-block grid. Existing vanilla slab items are the half stage and gain vertical placement and previews. Horizontal halves use vanilla slab blocks. The kiln and Fertilizer Furnace use quarter-beam supports, slab roof edges and eighth-cube corners. See [cutting](cutting.md) for controls, recipes and saved-build changes.

## Optional vehicle physics laboratory

An isolated Sable-only development profile now contains a controllable cargo-platform example and server restart/hidden client checks. It is excluded from the normal JAR and is not installed into Prism. See [vehicle physics integration](vehicle-physics.md) for setup and executable API examples. Boats, airship gameplay and production integration remain unimplemented.

## Freeform motor vessels

Build a connected hull around one two-block Small Boat Helm at the waterline, then launch it. There is no hull template or build band: owners can keep building from any attached vessel face and remove anything except the Helm. Build and start physical Hot-Bulb Engines aboard; every running engine contributes thrust, while Sable block mass slows the vessel. The two-wide console keeps the familiar wheel and four-position lever: W/S shift gear, A/D steer, Shift leaves without changing throttle, and neutral stops. Chests, barrels, cut blocks, Hot-Bulb Engines and the bulk freight stores are supported aboard. See [boats and vehicle physics](vehicle-physics.md) for assembly limits, controls and current custody/lifecycle limits. Sable 2.0.5 is required and installed by Deploy; Create is not required.

## Standalone Oil Engine

The Hot-Bulb Engine is buildable at the Smithy, with guided steel-bed/cylinder/flywheel construction, refined fuel and lubricant service tanks, five-second startup and a ten-second commissioning test. Complete running engines now power freeform boats directly. [Recipes, controls, consumption and limits](industry.md#oil-engine).

## Environmental heat

Fuelled machines warm a persistent local thermal field, venting most waste heat above their tops/chimneys. Regional rain gradually cools the air, with a lingering effect after it stops; heated multiblocks need a sheltered work face to ignite and process. Shelter/materials retain heat; comfort improves human labor and influences thermal machine fuel efficiency. Equip the creative **Thermal Survey Helmet** to inspect temperatures. [Rules, controls and prototype limits](heat.md).
