# Brick kiln prototype — 0.7

Release reference: single-block placement and verification statements below describe the stated versions. Use [current implementation](../../../civilization-mod/README.md) and [multiblock layouts](../../../civilization-mod/multiblock-builds.md) for today's build. Historical next-step instructions are not the active work order.

The first stationary industrial machine turns a bulky construction input into finished materials with better fuel efficiency. It uses Minecraft's furnace inventory, progress arrow, fuel flame, sided hopper access, save format, and batch completion logic. Its exterior has brick sides and a furnace mouth; the coal icon is a temporary shared vanilla asset.

## Playtest

1. Craft a furnace, then surround it with eight cobblestone in a crafting table to make a Brick Kiln. The recipe unlocks when a furnace enters your inventory.
2. Mine ordinary or deepslate coal ore. It now drops **Mineral Coal**. Existing ore in already-generated chunks works; existing ordinary coal stays ordinary coal.
3. Craft four clay balls into one vanilla clay block. Put clay blocks in the kiln's upper slot and Mineral Coal in the lower slot.
4. Each clay block becomes **four brick items in 200 loaded server ticks** (10 seconds at 20 TPS). One Mineral Coal burns for **1,600 ticks**, enough for **eight batches / 32 bricks** under continuous supply. Compared with converting that same fuel to ordinary coal and smelting individual clay balls, this is four times the brick throughput and four times the bricks per fuel, with no material multiplication.
5. Take the bricks and use normal brick-block recipes. Check whether the saved fuel/time feels worth obtaining and keeping a kiln supplied.

Quick test commands, if desired:

```mcfunction
/give @s civilization:brick_kiln
/give @s civilization:mineral_coal 4
/give @s minecraft:clay 32
```

## Fuel boundary

Mineral Coal comes from coal ore loot or smelting/blasting a coal ore block. Fortune and Silk Touch behavior remain inherited from the vanilla ore loot table. Mining uses the existing normal block-breaking calorie cost, not an additional industrial charge.

Mineral Coal can be crafted one-for-one into ordinary coal for torches, cooking, and other vanilla uses. **There is no reverse recipe.** Wither-skeleton coal, charcoal, wood, bamboo, blaze rods, kelp, coal blocks, and lava cannot power this kiln. Both manual fuel insertion and hopper insertion reject them; even forced fuel inventory contents cannot burn.

As of 0.8, vanilla brick smelting and villager brick sales are disabled. Saved brick offers are removed when accessed as well. Existing bricks and finite world salvage remain usable. This establishes finite fuel for custom industry, not a completed economy-wide renewable-energy ban: other vanilla furnace recipes and fuels still exist. World expansion, resource resets, and total fuel lifespan remain design decisions. No new ore world generation or retro-generation is needed.

## Inventory, persistence, and maintenance

- Output must have room for all four bricks before a batch can start/finish. An incompatible output also blocks work.
- An already-lit kiln burns continuously, including while empty or output-blocked. It does not ignite a new piece of fuel unless a recipe has output space. Blocked work resets like a vanilla furnace; fuel already spent is not refunded.
- Shift-click routes matching recipe inputs and Mineral Coal. Hoppers insert ingredients from above, fuel from the sides, and extract output below. A single machine ticking on the server owns all mutations.
- Contents and partial work/remaining fuel persist through normal world saves and reloads. Unloaded chunks and a stopped server do no work and use no fuel; there is no forced chunk loading or offline catch-up.
- Breaking a kiln drops the machine and stored items. Already-burned fuel and partial firing progress are lost. No custom ownership/access-control layer exists yet.
- Recipes are data-pack recipes of type `civilization:kiln`. Fuel duration is currently the single constant `KilnBlockEntity.FUEL_TICKS`; balance can become server configuration once playtesting warrants it.
- The implementation is a small extension of vanilla furnace classes, not a second inventory framework. No pipes, power network, moving machinery, global scan, or physics integration is added.

## Audit records

The existing asynchronous world energy journal gains records with `record_type: "machine"`:

- `kiln_fuel`: consumption of one Mineral Coal at ignition.
- `kiln_batch`: consumption of one clay block and production of four bricks at completion.

They include run/sequence, UTC, server tick, dimension and machine coordinates. They intentionally contain **no player calorie balance** and no invented player attribution for unattended operation. Consumers must branch on `record_type` before reading calorie fields; historical/player records have no `record_type`. Machine position is an audit location, not a persistent ownership identifier. Tracking who deposits/withdraws goods and machine ownership is later work. Existing queue, rotation and best-effort durability limits still apply.

## Validation

Six added Minecraft GameTests exercise exact eight-batch fuel yield, rejection of renewable fuels through manual/hopper/burn paths, blocked output and stack limits, partial-work save/reload, ore drops/one-way fuel conversion, shift-click routing and exact inventory drops on destruction. Together with the existing tests: **25 Minecraft GameTests and 19 JUnit tests**. Client startup is checked separately; in-world appearance and feel still need a human playtest. No claim of 200-player load validation.

The 0.8 update adds a separate Fertilizer Retort, built using kiln-made bricks. See `industry-progression.md`. Development can proceed in larger coherent steps; automated verification continues without requiring a manual playtest between individual additions.
