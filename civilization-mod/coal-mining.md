# Coal mining

Huge coal veins are worked by hand: pick → loose coal → shovel → rail cart → hand preparation → Coal. A complete vein block prepares into **16 Coal**. Ordinary coal ore still mines normally and drops Coal directly, and the [Coal Drill](industry.md#refinery) still extracts dense seams with refined fuel. The quantities below are prototype tuning.

## Equipment

Start with hand-mined Coal and the Kiln, Foundry and Smithy, then make the kit:

| Item | Route | Materials |
| --- | --- | --- |
| Coal Mining Pick | Smithy + Assembly Bench | Iron Pickaxe Head + Tool Handle |
| Coal Loading Shovel | Smithy + Assembly Bench | Iron Shovel Head + Tool Handle |
| Coal Minecart | Assembly Bench, 10 seconds | 4 planks + 2 Iron Rods + 1 Machine Parts + 4 Fasteners |
| Coal Preparation Screen | Assembly Bench, 10 seconds | 4 planks + 2 Iron Plates + 2 Sticks |

An iron head takes 25 seconds and 1.25 Coal at base efficiency (3 ingots for the pick, 1 for the shovel); final assembly is five seconds of hand work. Tools receive a grade and use Assembly Bench repairs ([manufacturing](manufacturing.md#repairs)). Mixed planks work for the cart and screen. Nothing here needs oil.

## Working a vein

The pick works naturally generated dense coal-seam blocks. The Creative tab also supplies a **Huge Coal Vein** workface for demonstrations; it has no Survival recipe. Lay ordinary rails and place a screen near the worksite, expose the seam and:

1. **Mine.** Hold the pick and click exposed coal. Each click commits one swing; holding left-click repeats after each full recovery, and releasing stops the next swing while the committed stroke finishes. Menus and pause prevent new swings, and looking around never strikes. The pick head follows camera yaw and pitch with bounded weighted carry, then settles back; the swing winds up beside the shoulder, strikes forward and down toward the aimed coal in one plane, and recovers to a ready pose. There is no forced camera shake or extra stock from faster turns. The server resolves your current aim at contact, within normal reach capped at 4.5 blocks.
2. **Notch.** Each contact removes an exposed 2×2 patch one quarter-cell deep on the block's 4×4×4 grid. Collision, outline and mesh follow the remaining material; work through the notch to go deeper, and the last portion clears the block.
3. **Collect.** Each strike drops its stock as one persistent chunk at the contact point with a small outward push. Chunks fall, roll off small ledges and settle into piles on supported empty floor cells. Walking over piles gives nothing; right-click a pile with the shovel to scoop up to 16 raw units, carried visibly on the shovel.
4. **Haul.** Place the Coal Minecart on rails and right-click it with the loaded shovel; only available space transfers. Empty-hand right-click pushes the cart along the rails in your viewing direction.
5. **Prepare.** Park the cart within 2.5 blocks of the screen. Empty-hand right-click the screen to take cart stock and prepare one batch; repeat after its short work interval. Crouch-right-click collects ready Coal into inventory.

| Stock or action | Value |
| --- | --- |
| Raw units | Each removed quarter-cell holds 1; 4 prepare into 1 Coal |
| Full vein block | 64 raw units, 16 Coal, normally 16 successful contacts |
| Ground pile | 64 raw units maximum; four visible height stages |
| Shovel | 16 raw units (4 Coal) |
| Cart | 4,096 raw units (1,024 Coal) |
| Screen | 256 raw input units and 64 ready Coal; no idle production |
| Pick | Contact at tick 12, recovery through tick 32; twice as slow when depleted |
| Successful pick contact | 1 durability and 2 base kcal; misses and rejected contacts are free |
| Scoop, accepted push, preparation batch | 1 base kcal each, with comfort and meal adjustments |
| Preparation | 4 raw units per Coal; at least 10 ticks between batches; blocked when depleted |
| Coal collection | Up to 32 per crouch-click, limited by inventory space |

The pick has 768 base durability; scooping adds no shovel wear.

## Persistence and protection

Remaining vein masks, falling batches, piles, shovel loads, cart cargo and screen stock are saved; shovel loads follow the item through switching, dropping and storage. Destroying a cart spills its raw cargo (Creative included); breaking a screen spills its stock and drops the screen. A pile's support follows the actual remaining coal surface, so a small remnant cannot hold a full pile; changing a workface notifies piles above it, and unsupported piles fall again. A chunk that cannot settle checks eight nearby downhill exits every ten ticks and rolls toward open space; walls still block it. Falling stock never despawns or becomes an item, keeps its quantity when fully blocked, and waits at unloaded chunk borders without loading terrain.

Mining rechecks the held tool, slot, world, reach, loaded terrain and claim access at contact; canceled breaks, changed targets and failed drops grant nothing and cost no wear. Scooping, cart placement, loading and pushing and screen use respect claims. Work on boats and airships is refused. Piles, carts and screens expose no hopper or cargo adapter.

Untouched seams keep their ordinary block model; only worked faces gain an unticked block entity and opt out of Minecraft's static shape cache, so collision, suffocation and pile support follow the saved mask. Mask collision and mesh geometry use bounded 512-entry caches; settled piles and screens do not tick, and a falling batch is one entity per strike. Coal textures follow the active resource pack and the cart keeps its native body.
