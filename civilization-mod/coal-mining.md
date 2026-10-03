# Manual coal mining prototype

The first Creative/admin loop implements pick → loose coal → shovel → rail cart → hand preparation → existing usable Coal. Ordinary coal ore still uses normal mining and yields usable Coal directly. Huge-vein work is richer: a complete prototype vein block prepares into **16 Coal**. These are trial values, not final Survival balance. The Coal Drill remains available until the replacement has Survival acquisition and migration.

## Try it

Use the Civilization Creative tab to obtain **Coal Mining Pick**, **Coal Loading Shovel**, **Coal Minecart**, **Coal Preparation Screen** and **Huge Coal Vein**. Place a workface above a solid floor, ordinary rails beside it, and a screen beside the destination rail. Return to Survival to try paid work; these items have no Survival recipes yet. The special pick also starts manual excavation of existing dense geological coal-seam blocks.

1. Hold the pick and click the exposed coal. A click commits one swing; hold left-click to repeat after each full recovery. Releasing stops the next swing, while the committed stroke finishes. Menus and pause prevent new swings; camera movement alone never strikes. Turn your view to pull the pick around: it trails camera yaw/pitch with weight, then settles back into alignment when you stop. The close ready pose raises the grip beside the shoulder and draws the head back over the player, then brings the working point forward/down toward the aimed coal. The strike and downward follow-through stay in one plane; only camera movement changes the plane. The tool recovers into a forward-ready pose. The camera remains freely responsive; there is no forced shake or extra stock from faster turns. The inertial first-person pose also responds between strikes; looking alone never mines. The server resolves the actual current aim at contact, within normal reach capped at 4.5 blocks.
2. Each successful contact removes an exposed 2×2 patch one quarter-cell deep in a 4×4×4 block grid. Collision, outline and chunk mesh follow the saved remaining material. Work through the notch to reach deeper layers. The final portion clears the block.
3. Loosened stock falls toward the miner and settles into persistent piles on supported, empty floor cells. Walking over it gives no items. Right-click a pile with the shovel to scoop up to 16 raw units, visibly carried on that actual shovel.
4. Place the Coal Minecart on rails; right-click it with the loaded shovel. Only available space transfers. Empty-hand right-click pushes the cart in your viewing direction using native rail movement.
5. Park within 2.5 blocks of the screen. With an empty hand, right-click the screen to receive cart stock and prepare one batch; repeat after its short work interval. Crouch-right-click collects ready Coal into available inventory space. It is the existing fuel used by machines and claims.

## Trial quantities and work

| Stock or action | Prototype value |
| --- | --- |
| Raw units | Each removed quarter-cell owns 1; 4 prepare into 1 Coal |
| Full vein block | 64 raw units, 16 Coal, normally 16 successful contacts |
| Ground pile | 64 raw units maximum; four visible height stages |
| Shovel | 16 raw units, equivalent to 4 Coal |
| Cart | 4,096 raw units, equivalent to 1,024 Coal |
| Screen | 256 raw input units and 64 ready Coal; no idle production |
| Pick | Contact at tick 12, recovery through tick 32; depleted players take twice as long |
| Successful pick contact | 1 durability and 2 base kcal; misses/rejected contacts are free |
| Scoop, accepted push, preparation batch | 1 base kcal each; normal comfort/meal adjustments apply |
| Preparation | 4 raw units per Coal; at least 10 ticks between accepted batches; blocked when depleted |
| Coal collection | Up to 32 per crouch-click, stopping at inventory capacity |

The pick has 768 base durability; shovel durability/repair tuning is unfinished and scooping currently adds no wear. The screen is manually worked for this prototype. Its final power, acquisition and throughput remain [open decisions](../open_decisions.md#manual-coal-mining--proposed-implementation-plan).

## Persistence, protection and limits

Remaining vein masks, falling batches, piles, shovel components, cart cargo and screen input/output are saved. Shovel loads follow the item through switching, dropping and ordinary inventory persistence. Native cart destruction spills raw cargo, including Creative destruction; breaking a screen spills stock and drops its controller. Unsupported piles become falling batches. Falling stock does not despawn or become an ordinary pickup, and a blocked landing retains its saved quantity. It pauses at unloaded chunk boundaries without forcing terrain.

Mining rechecks held-tool identity, slot, world, reach, loaded terrain and current claim access at contact. Canceled break hooks, changed targets and failed carrier creation cannot grant stock or charge wear. Scooping, cart placement/loading/pushing and screen operation respect current claims. Work on assembled boats/airships is rejected. Raw piles/carts/screens expose no hopper inventory or general cargo adapter.

This is a placed-face and short flat-route prototype. Regional vein shapes, irregular fractures, uphill haulage, moving screen/cargo art, multiplayer contention, separate-process restart/death recovery, vessel use, comprehensive external-hook cancellation and a busy-face performance sample remain later work. Administrative deletion can still destroy stock; no crash-transaction guarantee is claimed. Ordinary mining, legacy seam/drill drops and the drill recipe are retained during this transition. Do not treat those legacy paths as proof of the final huge-vein extraction boundary.

Untouched geological seams keep their ordinary block model. Only worked faces gain an unticked block entity. Mask collision and chunk mesh geometry use bounded 512-entry caches; settled piles and screens have no continuous ticker. A falling batch aggregates a strike's material rather than creating an entity for every quarter-cell. These bounds do not establish multiplayer capacity.

The original 64px timber/steel tools and timber/iron screen use the [local coal asset](../art/assets/coal_mining/asset.json). Coal inherits the active resource pack; the cart retains its native body and icon. Native model views and the hidden Photon/Faithful `coal-mining` scene cover held/inventory views, camera-shaped swings, notches with shaders disabled, loading, rail travel, preparation and consumption in an existing cooking station. [Testing](testing.md) owns the checks; [status](../docs/status.md) owns current results and publication limits.
