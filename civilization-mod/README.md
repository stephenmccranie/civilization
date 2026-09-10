# Civilization — implemented build

**0.9.2-dev · Minecraft 1.21.1 · NeoForge 21.1.250 · JDK 21**

This file describes the working mod. [Game rules](../design_direction.md) describe the intended game; [development plan](../development_plan.md) separates shipped work from future features. The planning consolidation changes no code.

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

Build runs unit tests. Deploy selects the versioned JAR from `gradle.properties`, refuses a running matching Prism game, archives previous Civilization JARs under instance `mod-backups`, copies the new JAR and verifies its hash. Client, Server and GameTest use development environments separate from Prism saves. A normal dedicated server requires owner-controlled EULA/setup work.

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

Currently bone meal works, ordinary stack limits remain, and crops have no regional gate. Planned changes must not be mistaken for shipped behavior.

## Multiblocks

Craft a kiln controller from a furnace surrounded by eight cobblestone. Craft the Fertilizer Works controller from four brick blocks, four copper ingots and a furnace in the alternating pattern.

Additional shells: kiln uses 25 cobblestone/stone bricks; Fertilizer Works uses 21 brick blocks and 5 full copper blocks. Both have a hollow chamber; a downward input hopper can replace a specified wall position. See [multiblock layouts](multiblock-builds.md) for exact placement and ports.

Controllers show required materials and actual block textures as translucent placement previews. Correct positions disappear. Contextual HUD/guides show only while looking at the unfinished area; no Build screen or G toggle. Held accepted block variants appear in the preview.

Lit heat burns while idle/blocked; new fuel ignites only for valid work with full output room. Unloaded machines pause without forced loading. A broken/blocked structure extinguishes heat and unfinished work while preserving inventory. Removing the controller drops its inventory; shell blocks remain. Registry IDs and `retort_*` audit names remain stable despite the Fertilizer Works display name.

## Audit and verification

World-local `civilization-energy/energy-current.jsonl` contains player calorie/production events and separate machine records. Movement is aggregated; machine records do not invent player attribution. The asynchronous journal is bounded (8,192 queued entries, 16 MiB files, eight backups) and best effort. Crash/overflow/disk failures can lose records. It is not a persistent inventory/payment/ownership ledger.

Recorded evidence: 19 unit tests in the latest build; 36 GameTests on the multiblock foundation, with later preview changes built separately. User play confirmed the calorie/farming/industry loop and multiblock guidance; not every later visual revision has human verification. No tests were rerun for this documentation-only review.

Older version details: [farming release note](farming-prototype.md), [kiln release note](kiln-prototype.md), [industry release note](industry-progression.md). Their tests and pending-work statements describe those releases. Pre-consolidation full documentation is [archived](../docs/archive/2026-09-10-pre-consolidation/README.md).

No regional system, cooking station, land controller, groups, shops, recovery container, map or vehicle implementation exists yet. Dedicated multi-client behavior and the 200-concurrent target remain unverified.
