# Current project status

Last reviewed: **2026-11-01**. Current build: **0.35.0-dev**. [Playtest release](https://github.com/stephenmccranie/civilization/releases/tag/playtest-0.35.0-1) includes the matching mod JAR and Prism client pack. Clients and server must use the same Civilization version.

## Implemented

| Area | Current scope and reference |
| --- | --- |
| Food and labor | Calorie reserve, work costs, recovery, movement/rowing/sleep accounting and material gates: [mod reference](../civilization-mod/README.md) |
| Tier 2 cooking | Sensory stove, work meals, removable skillet/carryover and physical Creative oven: [cooking](../civilization-mod/cooking.md) |
| Land and trade | Powered claim tiers, whitelists, held takeovers/refunds, barter counters and Survey Table: [ownership and trade](../civilization-mod/ownership-trade.md) |
| Geography and farming | Shared regional queries, rain districts, soil water, crop cycles, fertilizer and prototype Rain Caller: [geography](../civilization-mod/geography.md), [weather](../civilization-mod/weather.md) |
| Manual coal prototype | Survival Smithy tools, craftable handling infrastructure, partial workfaces, camera-shaped pick swings, saved loose stock, shovel loads, rail carts and hand preparation: [coal mining](../civilization-mod/coal-mining.md) |
| Industry | Finite coal/oil fields, prospecting, physical extraction, refinery, pipes, workshops, graded equipment/repairs and bulk cargo stores: [industry](../civilization-mod/industry.md) |
| Construction | Half-grid cutting, straight-line offhand building/demolition, contextual multiblock guides and actual structural-item placement: [cutting](../civilization-mod/cutting.md), [multiblocks](../civilization-mod/multiblock-builds.md) |
| Gladiator Pit | Exactly centered masonry oval, opposed prep rooms, two-fighter knockout matches and physical item wagers: [arena reference](../civilization-mod/gladiator-pit.md) |
| Firearms | Creative/admin Colt Paterson with cocking, reload custody and server-side projectiles: [firearms](../civilization-mod/firearms.md) |
| Heat | Persistent local heat, material exchange, finite waste heat, shelter/rain effects, felt temperature, comfort and survey helmet: [heat](../civilization-mod/heat.md) |
| Vehicles | Freeform engine-powered boats and a development-power airship prototype with assembly/disassembly and terrain safety: [vehicles](../civilization-mod/vehicle-physics.md) |
| Physical foundation | Shared material/geometry properties, inspectors, exact stone/cut-piece cargo accounting and static Sable integration; personal offhand block carrying and mass-only wheelbarrow/stockpile cargo; ordinary chest migration and dynamic Sable contents mass remain pending: [physics](../civilization-mod/physics.md) |
| Presentation | Shared cabinet interfaces, JEI recipe guidance and native asset pipeline: [mod reference](../civilization-mod/README.md), [modeling](../tools/modeling/README.md) |

## Validation and limits

- **Release checkpoint:** build, **83 fast tests** and all **377 required server GameTests** passed on the combined manufacturing, hauling and thermal sources. The guarded `wheelbarrow` client passed; loaded Photon/shaders-disabled views, pallet cargo and Hand UI were inspected. The guarded `assembly` client also passed; actual bench, component icons and working/finished menus were inspected. The 80-second heating/40-second shutdown thermal check and separate-process reload passed, retaining all 41,915 cells with 0.45-second peak recorded solver age.
- **Distribution:** all **21 pinned downloads** passed hash, size and archive checks using the local release JAR. The deterministic pack contains only its manifest and five reviewed settings files. The distribution JAR excludes development fixtures. SHA-256: `636fcab9e21522a378a1064e706031b5eb1153045951f6f1de92fd3f7bc8ff93`.
- **Manufacturing:** shared forged parts, the two-block Assembly Bench, hand labor, equipment/repairs and industrial acquisition are implemented. Prior native and guarded client checks covered all-facing placement, canceled rollback, claimed extensions, single-inventory dismantling, worker contention, exact charges, stock conservation and grade/identity preservation. Existing narrow benches retain their stock and visual until replaced. [Behavior and migration](../civilization-mod/manufacturing.md).
- **Hauling:** wheelbarrows and side-connected pallets retain exact saved stock, enforce mass capacity and support partial transfers. Personal building blocks use one offhand stack of up to 32; extra legacy stock remains recoverable with `/civilization recoverblocks`. Creative keeps its native inventory mapping. Loaded balance, larger pallet groups, remote contention, actual cross-chunk restart cycles, broader container adapters and supply-fed building remain unverified or unfinished. [Rules](../civilization-mod/physics.md).
- **Thermal field:** timestamped sources, fixed physical time, actual-outlet exhaust, maintained lava boundaries and half-second exchanges are implemented. Earlier isolated checks passed 181 reference physics assertions and the scheduled source/shutdown/reload loop. Controlled local measurements showed lower compute/allocation cost; coarse air transport is intentionally approximate. Historical geometry/weather replay, regional sleeping and representative city load remain open. [Thermal behavior and experiments](../civilization-mod/heat.md).
- **Other included fixes:** permanent server-saved grass wear, partial-coal collision/settlement, derrick ambient lighting and rare dispersed uranium. Their focused guarded client/native checks passed at their local checkpoints. No world regeneration or old-chunk uranium repopulation is required. [Grass](../civilization-mod/README.md#grassier-grass-compatibility), [coal](../civilization-mod/coal-mining.md), [derrick](../civilization-mod/industry.md#build-and-supply-machines), [uranium](../civilization-mod/geography.md#frontier-uranium-and-accessories).
- **Acceptance limits:** fresh Prism import, a real remote join, ordinary Survival/economy balance, crash recovery and representative 200-player load remain player/integration checks. Automated fixtures do not establish those outcomes.

## Local runtime

Civilization Dev and the dedicated server now have the released **0.35.0-dev** JAR, matching the SHA-256 above. Previous JARs are backed up; the server also has a complete offline world/configuration backup. The server reached normal startup and answered the Minecraft status protocol. Existing worlds and settings are retained, and the previously stopped world-generation service remains stopped. Relaunch Civilization Dev to load the upgrade; a real player join remains untested.

The dedicated terrain is the selected Alesrr Terrain Diffusion scale-4 Candidate 5 world. Elevated rivers and deposit integration still need broad-world playtesting. The separate Compact Campus and development fixtures remain local; the Lacertan modeling study is not a runtime feature. Original artwork, editable models and visual-review history stay outside GitHub.

## Work direction

The [development plan](../development_plan.md) owns remaining work: review loaded hauling and acquisition balance, add supply-fed local building, then extend the powered freight loop. Manual coal still needs broader custody/Survival review and drill retirement; cooking/mob follow-ups remain queued and flight optimization is deferred. [Open decisions](../open_decisions.md) owns unsettled rules. Older published evidence remains in the [history index](history/README.md).
