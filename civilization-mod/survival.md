# Survival

Day length, calories, movement, food, inventory limits and the player-facing interface. Crops live in [geography](geography.md#crops-and-harvests), cooking in [cooking](cooking.md), and carrying building blocks in [physics](physics.md#personal-block-carrying).

## Day and night

A full Overworld day/night cycle lasts **60 real minutes at 20 TPS**, with vanilla daylight/twilight/night proportions. NeoForge's fractional clock runs at one-third speed while simulation time advances normally, so machines, movement, crop ticks and other timers keep their rates. Sun, moon, daily schedules and moon phases follow the longer day; server lag stretches it as in vanilla. Sleeping and `/time set` behave normally.

The default is applied once per world without changing the current time; the `civilization_world_defaults` saved-data marker prevents reapplying it, and explicit NeoForge clock settings are respected. NeoForge saves and synchronizes the rate. Inspect it with `/neoforge day`, change it with `/neoforge day length set 60`, and restore vanilla's 20 minutes with `/neoforge day length set default`.

## Calories

One server-owned calorie reserve replaces vanilla hunger, saturation and exhaustion. Defaults:

| Rule | Value |
| --- | ---: |
| Capacity / initial reserve | 2,400 kcal |
| Walking/crouching | 0.10 kcal per block |
| Sprinting | 0.30 kcal per block (3× walking) |
| Rowing a vanilla boat/chest boat | 0.05 kcal per horizontal block while paddling |
| Wheelbarrow handling | Walking rate: 0.10 kcal per horizontal block traveled at the handles, including turns |
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
| Cold exposure | Up to 0.20 kcal/second ([rule](heat.md#comfort-and-cold)) |
| Depletion recovery threshold | 200 kcal |

- **Travel** costs distance × the gait's rate, whatever the speed: 100 blocks costs 10 kcal walking, 30 sprinting or 5 rowing. Speed bonuses never multiply the charge. Walking, sprinting and rowing count horizontal distance; swimming counts 3D distance. Every block of upward personal travel adds `ascentKcalPerBlock` (1 kcal), including stairs, ladders, swimming and the rising part of a jump; descending refunds nothing. Movement is measured from server-accepted displacement.
- **Rowing** is charged only to the controlling player of a vanilla boat or chest boat while its paddles are active; passengers, drifting boats and powered Civilization vessels pay nothing. Rowing has its own distance counter.
- **Wheelbarrow handling** charges the walking rate for actual handle travel, including reversing and turning, plus normal ascent; idle or blocked drive is free.
- **Not charged:** other riding, creative flight, elytra, teleports and abnormal large movement deltas; tool transformations, buckets, ranged attacks and automated or fake-player work.
- **Placement** of a cut piece costs the full-block rate × its volume; native slabs count as half blocks.
- **Sleep** charges `sleepKcalPerGameHour` on waking, for in-game time in bed including hours skipped when the night passes. There is no idle or offline drain.
- **Discounts:** [thermal comfort](heat.md#comfort-and-cold) and [cooked meals](cooking.md#meal-benefit) reduce labor and travel costs.
- **Depletion:** at zero, basic actions continue without debt, mining runs at 25% speed, sprinting and natural healing stop, and starvation alone stops at three hearts; other damage remains dangerous. Eating up to 200 kcal clears depletion. The reserve and depletion persist through death, dimension changes and relog.
- **Healing** needs enough calories, no depletion and the `naturalRegeneration` game rule. Magical healing keeps its effects, saturation grants no energy, and Peaceful does not refill food or health.
- Creative, Spectator and invulnerable players pay nothing.

**Emergency foraging:** empty both hands, crouch-right-click eligible natural ground and stay still and crouched for 10 seconds to receive a 25-kcal Foraged Morsel. Moving, changing equipment or any interruption cancels. Foraging is recovery, not efficient provisioning.

The ten-segment calorie HUD fills from right to left, shows the number and thresholds, and hides with F1.

Settings are in the world's `serverconfig/civilization-server.toml` (stop the world before editing); costs, thresholds and food values are synchronized to clients. `/civilization status` and `/civilization calories` inspect state; operator `/civilization calories set` and `reset` are development tools.

## Movement

Ordinary level-ground travel settles at **3.2 blocks/s walking** and **4.6 blocks/s sprinting** at 20 TPS. Sneaking keeps its proportional slowdown and jump height is native. Airborne acceleration follows the walking/sprinting gait, and the forward sprint-jump impulse gives repeated sprint-jumping about **5.52 blocks/s**, roughly 20% faster than sprinting: a momentum bonus, not a velocity cap. Ice, slopes, effects, equipment and tick rate change these figures. Creative flight, vehicles and mobs keep their own speeds.

### Street paving

Craft 2 **Street Pavers** from 1 brick block and 1 stone-brick block (shapeless, volume-preserving); pavers depend on Kiln bricks. Street Pavers, their slabs and stairs, and exposed cut-piece surfaces of them give **+25% ground movement speed**: about **4.00 blocks/s walking** and **5.75 sprinting**. Mixed cut assemblies check the actual supporting surface, and buried paving does not count. The bonus changes ground acceleration only: friction, jump height, FOV and calories per block are unchanged, and it adds nothing airborne, swimming, in vehicles or in creative flight. Momentum carries naturally off the road. The block tag `civilization:road_paving` owns the eligible materials. Paver slabs come from the saw ([cutting](cutting.md)). The paver texture is one world-aligned 8×8-block painting at 64 pixels per block.

### Trampled grass

With Grassier Grass installed on the client ([version gate](README.md#optional-mods)), walking wears down its grass blades:

- Player travel adds a footfall every 0.8 blocks along grass blocks, including continuous walking and sprint-jumping. Ordinary hops keep the distance counter and sample the grass beneath the airborne route, up to 1.6 blocks above the first physical surface and 20 airborne ticks after ground contact.
- Every four footfalls on the same grass block shorten its blades by another 10% of their configured height, reaching 20% after 32 footfalls. Wear never recovers.
- Standing still, flight, swimming, riding, large teleports, bridges, fluid, unloaded terrain and high falls add no wear. Vanilla grass blocks, plants, collision and movement speed are unchanged.
- Civilization also disables Grassier Grass's automatic white snow tint on blades, including in cold or high areas in clear weather and when rendering through snow is enabled. Blades keep their normal biome tint; snow blocks, snowfall, accumulation, biome temperature and the mod's visibility settings are unchanged.

The server stores sparse counts in chunk attachments (at most 16,384 grass positions per chunk, eight bytes per saved cell before NBT/compression; capped cells keep their wear without further writes) and sends visible wear stages to players tracking the chunk, including when they start watching it. Partial counts cause no network or mesh update. Stage changes are batched per chunk and tick and dirty only the affected vertical sections through Grassier Grass's asynchronous build budget; height is applied when cached geometry is built, with no per-frame scan. Both sides need Civilization. Grassier Grass is optional client rendering and is not bundled; the client mixins check its version and snow/mesh/cache signatures and leave an unverified version unchanged with a log warning.

## Food

Bread is worth 500 kcal and cooked beef or pork 700 by default. Every vanilla food has an explicit value; server overrides take precedence, and unknown mod food uses nutrition × `fallbackKcalPerNutrition` (100). A cake slice is `cakeSliceKcal` (200). Food tooltips show kcal; food effects and eating animations remain. Excess above capacity is discarded.

Three bread craft one **Field Ration** worth exactly three times the configured bread value (1,500 kcal by default).

Cows, mooshrooms, pigs, hoglins, sheep, chickens and rabbits drop one raw meat each, with no Looting bonus; hides, wool and other drops are unchanged. Raw meat is cooked on a [Cooking Stove](cooking.md#cooking-stove).

## Inventory

Ordinary 64-item stacks hold **32**, including custom items; 16-item stacks and unstackable items keep their limits. Building blocks are carried in a single offhand stack ([personal block carrying](physics.md#personal-block-carrying)). One log, wood, stem or hyphae block (stripped or not) makes one plank, as does one bamboo block.

## Interface

All custom items are in the **Civilization** creative tab, marked with the kiln icon: machines first, then ingredients and foods. New items in the mod's namespace join it automatically and remain in creative search. Item tooltips give short purpose, control and safety hints; construction bills and processing details belong to the placed guides, machine screens and [JEI](machines.md#jei).

### Welcome message

On every join the server sends a short framed getting-started card in chat: offhand block carrying, that furnaces and campfires are retired, five numbered steps from the Stone Saw through building and lighting the [Kiln](machines.md#kiln) to bricks for the Foundry, raw food, JEI's **R** lookup, and an invitation to give feedback in ordinary chat. It fits the ten lines closed chat shows, without wrapping at the pack's 4× GUI scale. It is plain server chat, so any client shows it; the text lives in `Welcome.java`.

### Player-list ping

Hold **Tab** to see each player's round-trip latency beside their name, such as `42 ms`. The server sends one native ping probe per second and shows the mean of replies received during the last five seconds; client frame stalls and queued traffic count. A new connection shows `-- ms` until a reply arrives, and a probe unanswered after five seconds shows a lower bound such as `≥ 6000 ms`. Samples are bounded, expire automatically and clear on disconnect or server stop. Team formatting and custom tab names are kept. This uses Minecraft's ping/pong packets and tab display-name updates, so protocol-compatible clients need no update; native connection timeouts are unchanged.

## Energy journal

The world-local `civilization-energy/energy-current.jsonl` records player calorie and production events (such as `sleep`, `row`, `push_wheelbarrow` and `cold_exposure`) and separate machine records (such as `coal_fire_fuel` and structure breaks). Movement is aggregated, and machine records carry no invented player attribution. The asynchronous journal is bounded (8,192 queued entries, 16 MiB files, eight backups) and best effort: crashes, overflow or disk failures can lose records. It is a diagnostic log, not an inventory, payment or ownership ledger.

## Save compatibility

Over-limit non-block stacks in older saves are not migrated. Extra block stacks and filled portable containers go to the `/civilization recoverblocks` balance ([physics](physics.md#personal-block-carrying)).
