# Civilization development

For the current project roadmap and formal checklist, see [development_plan.md](../development_plan.md). This README documents the implemented build.

Minecraft **1.21.1**, NeoForge **21.1.250**, **64-bit JDK 21**. Custom mod **Civilization 0.9.2-dev**.

The food system uses one server-owned calorie reserve. Hunger, saturation, and exhaustion no longer determine energy, eating, sprint eligibility, or natural healing. Physics and airships remain deferred.

## Multiblock machines and building preview (0.9)

Both machines now require physical multiblock structures. The old machine items are controllers with the same recipes and saved registry IDs; their stored inventories remain. The retort's player-facing name is now **Fertilizer Works**.

- **Kiln:** controller + 25 cobblestone/stone bricks; hollow 3×3×3 structure.
- **Fertilizer Works:** controller + 21 brick blocks + 5 full copper blocks; 3×3×3 chamber with a central copper chimney, 4 blocks tall. Oxidized and waxed full copper blocks are accepted.
- The controller tooltip lists additional materials. Place or look at a controller to select it. Its transparent preview and HUD appear only when looking at its unfinished build area within eight blocks. Previews use translucent actual block models/textures; a red outline means replace/clear. Holding an accepted block variant previews that variant. Both disappear on completion.
- The controller GUI shows structure status. The Build button, layer-plan screen and G toggle were removed in 0.9.1; use the world preview and contextual HUD.
- Incomplete builds cannot operate. A breach loses active heat and partial work but retains stored items. Missing neighboring chunks pause processing without forcing chunk loads.

See [multiblock-builds.md](multiblock-builds.md) for placement, hopper ports, compatibility and verification. Earlier single-block descriptions below are superseded by these assembly requirements.

## Farming update (0.6)

See [farming-prototype.md](farming-prototype.md) for the food catalog, lighter vegetation costs, wheat/fertilizer loop, field-ration recipe, production logs, and tests. Reapplying fertilizer now explicitly says the wheat is already fertilized, without spending fertilizer or calories.

## Brick kiln (0.7)

See [kiln-prototype.md](kiln-prototype.md) for recipes, finite mineral fuel, inventory behavior, logging, and test coverage. Craft a furnace surrounded by eight cobblestone into a kiln. Mine coal ore for Mineral Coal, then feed the kiln clay blocks. One fuel fires eight batches of four bricks.

## Fertilizer Retort and brick gate (0.8)

Brick smelting in vanilla furnaces is disabled, and villager brick sales are removed, including saved offers. Existing bricks and finite world salvage remain usable.

Build a Fertilizer Retort from four brick blocks, four copper ingots and a furnace. Craft one clay ball plus one gravel into Raw Mineral Blend. The retort consumes one blend to produce four Mineral Fertilizer in 20 seconds; one Mineral Coal powers four batches (16 fertilizer). The kiln and retort have separate recipes, shared furnace-style controls, hopper access, persistence and machine audit records. See [industry-progression.md](industry-progression.md) for the complete chain and current balance.

## Calorie system

| Rule | Initial value |
| --- | --- |
| Capacity and new-player reserve | 2,400 kcal |
| Walking / crouching | 0.10 kcal per block |
| Sprinting | 0.30 kcal per block (3x per distance) |
| Breaking a heavy block / light vegetation / crop | 4 / 0.25 / 1 kcal |
| Placing a block / planting a crop | 2 / 1 kcal |
| Jumping | 2 kcal, plus horizontal travel |
| Successful melee attack | 3 kcal |
| Natural healing | 40 kcal per health point (half heart), every 4 seconds |
| Minimum reserve to sprint | 100 kcal |
| Hunger I status effect | 2 kcal per second; scales with effect level |

These are tunable game kcal, not a claim of realistic nutrition. No passive idle or offline drain. Walking remains possible at zero for recovery. Basic breaking and placement remain allowed even when their cost cannot be fully paid; they spend only the remaining calories and never create debt. Empty-calorie mining is slower. Creative, Spectator, and invulnerable players do not spend energy. Peaceful still uses calories, with no starvation damage or free automatic food/healing refill.

Natural healing requires enough calories, a non-depleted state, and `naturalRegeneration`. At zero, starvation checks run every four seconds and stop at three hearts on Easy, Normal, and Hard. They cannot kill a player or push health below that floor; other damage still can. Healing potions, regeneration effects, and other explicitly magical health sources retain their existing effects. The Saturation status effect provides no energy; Hunger has an explicit calorie cost.

Movement is charged from the server's movement-statistics hook using accepted displacement, not elapsed sprinting time. Sprint-jump horizontal travel counts. Swimming includes vertical travel; climbing includes ascent. Mounts, elytra flight, creative flight, pure downward freefall, and teleports do not incur walking bills. Knockback/current-driven horizontal movement can count; this is not yet a biomechanical effort model. Deltas over 16 blocks per call are ignored as abnormal movement. Future transport/physics integration must define its own costs.

## Depletion and hand-foraging (0.5)

Reaching zero enters a persistent depleted state. It clears only after eating back to **200 kcal** (configurable, capped at capacity), so one nibble does not instantly remove the penalty. Dropping below 200 while otherwise healthy does not itself cause depletion; entry is at zero. Once recovered, normal calorie rules resume.

While depleted:

- Walking and basic jumping remain available at normal movement speed. Sprinting and natural healing are disabled.
- Mining runs at **25% normal break speed**. Existing tool requirements, protected areas, and vanilla block drops still apply. Blocks/crops with zero break time remain instant; a speed multiplier cannot slow an instant break.
- Basic block breaking, placement, crafting, planting, containers, eating, and cooking remain available. When a cost exceeds the remaining reserve, only that reserve is spent; no debt or hidden energy is created.
- Starvation alone stops at **three hearts** on all non-Peaceful difficulties, including Hard. This does not heal anyone already below the floor. Mobs, falls, drowning, and other damage remain dangerous.
- Depletion persists through relog and death. The HUD labels it **Depleted**, highlights the recovery threshold, and the chat explains the recovery controls. Future powered tools must keep their own industrial-energy requirements; none are implemented yet.

### Forage controls

1. Empty both hands, including the offhand.
2. Crouch and right-click nearby natural ground: dirt/grass, sand, gravel, stone, terracotta, mud, moss, or snow. Netherrack, soul terrain, and end stone are supported too.
3. Remain crouched and still for **10 seconds**. The action-bar text shows progress. You do not need to hold right-click.
4. One **Foraged Morsel** appears in your inventory, or drops at your feet if full. Eat it for **25 kcal**.
5. Repeat deliberately as needed. Each right-click starts one attempt; there is no automatic repeat. Eight morsels supply 200 kcal if you incur no intervening costs.

Moving more than 0.4 blocks, releasing crouch, equipping an item, dying, logging out, changing dimension, or losing access to suitable ground interrupts the attempt. Foraging consumes time, requires no tools/fuel/calories, and does not damage terrain. It is available at any calorie level, but produces only 150 kcal per minute before eating time, far below good provisions. It is a renewable food route, not industrial fuel.

A custom morsel item uses the existing vanilla brown-mushroom texture as a prototype icon and appears in Food & Drinks. Its calorie value is controlled by `foragedMorselKcal`, not the general food fallback or override list. The biome coverage comes from a data-driven `civilization:forage_ground` block tag; suitable terrain must still be reachable. Sealed artificial rooms, void spaces, and arbitrary third-party protection rules are not guaranteed recovery locations.

Server settings: `depletedRecoveryKcal` (200), `depletedMiningSpeedMultiplier` (0.25), `starvationHealthFloor` (6 health points), `forageDurationTicks` (200), `foragedMorselKcal` (25). Forage duration follows simulation ticks, so lag can make it take longer. Client and server must both update to 0.5 because the calorie packet now carries depletion state.

Quick test: `/civilization calories set 0`, empty both hands, crouch-right-click dirt or sand, stay still, and eat the result. `/civilization calories set 200` can verify recovery immediately. These admin controls require cheats/operator privileges; foraging itself does not.

Verification: **19 unit tests and 12 Minecraft GameTests passed**, including a full eight-forage/eat recovery sequence from zero, interruption without food payout, slower/restored mining, saved depletion, actual zero-calorie block breaking, healing after recovery, and fractional starvation damage stopping exactly at the floor. The new HUD label and client interactions still need visual in-game verification; previous desktop capture was unavailable.
## Food and HUD

The drumsticks are replaced by a compact calorie bar and a numeric kcal label. In 0.3.1, the bar fills from right to left to match vanilla food indicators. Its black pixel outline, ten shaded golden segments, and unscaled shadowed Minecraft text replace the translucent panel and reduced-size text. The sprint marker is mirrored to the right end as well. It turns amber below 25%, red below 10%; a small red marker indicates the sprint threshold. The HUD reserves space for air and mount-health indicators. It hides in Creative/Spectator and with F1. Food tooltips show the energy per item.

Explicit food values initially include apple 200, bread 500, baked potato 400, cooked beef 700, carrot 150, golden carrot 400, cookie 100, dried kelp 50. Cake supplies 200 per slice. All vanilla edible items now have deliberate defaults in FoodCatalog.java; cooked beef and pork both supply 700 kcal. Server food overrides take precedence. Unknown modded foods use nutrition times 100, ignoring saturation. This fallback is a temporary catalog rule, not a second energy system. Consumption retains vanilla effects, eating time, containers, and animations. Eating fills the one reserve up to capacity; excess is discarded. You can eat whenever the reserve is below capacity. Always-edible special foods retain that behavior.

Settings live in `<world>/serverconfig/civilization-server.toml`. In Prism, worlds are under `minecraft/saves/<world>`. Close the world before editing. All costs, capacity, sprint minimum, cake, fallback, and per-item overrides are configurable. `sprintMultiplier` must be greater than one. Keep the sprint minimum below capacity. Clients receive server config and calorie updates; they cannot submit calorie values.

### Commands

- `/civilization status`: version and system status.
- `/civilization calories`: exact reserve, work counts, walked/sprinted distances, and calorie totals.
- `/civilization calories reset`: resets only measurement counters.
- `/civilization calories set 500`: sets your own reserve for quick tests; requires cheats/operator permission level 2. Clamps to configured capacity.

Counters report energy actually paid/absorbed, including clamping at empty/full. Calorie HUD updates at most four times per second when values change, with immediate updates on login, respawn, dimension change, and the test command.

### Persistence and migration

Calories and measurement counters survive saving, logout, dimension changes, and death. Respawning cannot refill calories. Existing 0.2 worlds receive one full reserve on their first load under 0.3; the old hunger, saturation, exhaustion, and debt are discarded. This is an intentional development migration. Returning to an older mod version does not preserve the new model. Back up established worlds before mod upgrades.

The replacement FoodData exposes a 0–20 projection for vanilla food scoreboard/health packets, with saturation and exhaustion fixed at zero. This projection is never used as stored energy and incoming vanilla food updates cannot mint calories. Other mods that replace FoodData or depend on vanilla exhaustion require explicit integration.

Block completion still uses final cancellation and world state at end of tick. Same-position replacement within a tick can undercount work. Basic labor is no longer denied for insufficient energy. The audit records nominal cost and actual cost, including zero-cost recovery work. Tool transformations, buckets, ranged attacks, and automated/fake-player machines need explicit rules before public economy enforcement. This build is not a complete energy economy or a 200-player capacity validation.

## Per-player energy logs (0.4)

Logging starts automatically with each integrated or dedicated server. Files are local to the world:

`<world>/civilization-energy/energy-current.jsonl`

For Prism single-player: `minecraft/saves/<world>/civilization-energy/energy-current.jsonl`. All players share a structured JSON Lines file, with a UUID and display name on every record; filter by UUID for reliable per-player history. This avoids opening hundreds of separate files. No client can submit audit records.

Each line includes schema version, server-run UUID, emission sequence, UTC start/end timestamps, server ticks, player UUID/name, action, detail, dimension, player position, requested calorie delta, actual delta, and before/after balance. Positive deltas add energy; negative deltas spend it. Food near capacity records both nominal kcal and the smaller actual gain. Details identify the food item or block ID and block position. Position is the player's position when logged (end position for an aggregate), not a complete movement trace.

Recorded events: walking/sprinting (including swimming/climbing context), block breaking/placement, jumping, successful melee hits, eating/cake, natural healing, Hunger-effect drain, attempted starvation damage, capacity clamps, admin calorie changes, debug-counter resets, login/logout, respawn/clone, dimension changes, orderly server-stop snapshots, depletion entry/recovery, and foraging start/completion/cancellation. Acquiring a morsel is a zero-delta inventory event; eating it is the calorie gain.

Consecutive movement or Hunger events of the same kind for one player aggregate for up to one second. A different action flushes that player's pending entry first, preserving per-player balance order. `count` is the number of merged calls; movement `quantity` is total blocks, while other events use one per call. Movement entries are not individual footsteps or exact path tracking. Activity interleaving can produce shorter aggregates. Global `sequence` is emission order; use timestamps to compare occurrences across players.

An asynchronous writer flushes about once per second. Expect roughly 1–2 seconds before an aggregate is readable, with additional delay under load. A single bounded queue holds 8,192 entries. Files rotate at 16 MiB, keeping eight backups (`energy-1.jsonl` newest through `energy-8.jsonl` oldest), approximately 144 MiB per world. Older records are deliberately discarded by rotation; retention is by size, not days. Orderly shutdown drains queued/pending entries, waiting up to five seconds for the writer.

This is a diagnostic audit trail, not a transactional accounting database: abrupt crashes, full disks, or queue overflow can lose records. Failures/record drops are reported in the normal server log instead of blocking gameplay. No 200-player load test has been performed. Actions without implemented calorie costs and third-party mods that mutate the reserve directly are outside this logger's coverage. Future machine-energy systems will need their own hooks. Nothing reconstructs sessions before installation.

Verification: 16 unit tests passed, including live readability, shutdown drain/order, rotation/retention, and restart/Unicode behavior. Seven Minecraft GameTests passed. The generated runtime JSONL was parsed and checked for balance consistency, a 500-requested/100-absorbed food event, and 100-block walking/sprinting aggregates costing 10/30 kcal.
## Build, test, deploy

`dev.local.json` supplies the installed JDK and Prism instance and is excluded from Git. Copy `dev.local.example.json` for another machine.

```powershell
.\dev.ps1 Build
.\dev.ps1 GameTest
.\dev.ps1 Deploy
```

Build runs JUnit tests. GameTest runs Minecraft's isolated automated server tests; test classes and the empty structure fixture are in `src/gameTest` and excluded from the distributable JAR. Require an explicit `All ... required tests passed` log line: the Minecraft launcher can exit successfully when test discovery fails.

Deploy requires the Prism game to be closed; Prism itself may remain open. It verifies Minecraft/NeoForge versions, builds, archives older Civilization JARs under the instance's `mod-backups`, copies `build/libs/civilization-0.6.0-dev.jar`, and verifies SHA-256.

```powershell
.\dev.ps1 Client
.\dev.ps1 Server
```

These launch development environments separate from Prism's worlds. A normal dedicated development server requires the owner to review/accept Minecraft's EULA. Keep test servers private.

## Verification (September 10, 2026)

- 12 unit tests: one capped reserve, fractional costs, overflow/underflow, resizing/restoration, healing affordability, equal-distance sprint/walk costs, diagonal/partitioned movement, sprint jumping, swimming/climbing, and abnormal deltas.
- 6 Minecraft GameTests passed: replacement and actual bread consumption/save-load, calorie-funded healing, actual ServerPlayer movement hook, death clone, cake/jump hooks, Hunger effect and counter reset.
- Development client started and completed resource loading with the mixins enabled. Windows screenshot capture failed with `SetIsBorderRequired: No such interface supported`; the HUD has not been visually verified in-world. Multiplayer packet delivery and layout at different GUI scales still need an in-world check.

A quick UI check needs no survival grind: use a cheats-enabled test world, `/civilization calories set 500`, inspect the amber bar, hover bread for its 500-kcal tooltip, eat, then inspect the reserve. Set 50 to inspect the red bar and sprint restriction. Test underwater/mounted layouts when available.

## Source and tooling

Core files are `CalorieFoodData`, `CalorieReserve`, `CalorieConfig`, `FoodCalories`, `CalorieSystem`, `CaloriePayload`, `client/CalorieHud`, and the focused mixins. Uses Mojang mappings. NeoForge MDK bootstrap commit `30cafee9cd8d7f46427ec88fa8579d49c146df9a`; retained licensing in `TEMPLATE_LICENSE.txt`. ModDevGradle 2.0.146; Gradle wrapper 9.2.1.




