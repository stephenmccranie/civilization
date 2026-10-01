# Current project status

Last reviewed: **2026-10-01**. Current build: **0.34.25-dev**, installed in Civilization Dev. Build and installed SHA-256: `DE3D1A8215A2EC43F81E8C234397C19238E7DAE71D938388F866C2CE6E0DBECF`.

## Implemented

| Area | Current scope and reference |
| --- | --- |
| Food and labor | Calorie reserve, work costs, recovery, movement/rowing/sleep accounting and material gates: [mod reference](../civilization-mod/README.md) |
| Tier 2 cooking | Creative/admin stove, continuous heat, sensory timing, quality-based meals, removable skillet and paid carryover: [cooking](../civilization-mod/cooking.md) |
| Land and trade | Powered claim tiers, whitelists, held takeovers/refunds, barter counters and Survey Table: [ownership and trade](../civilization-mod/ownership-trade.md) |
| Geography and farming | Shared regional queries, rain districts, soil water, crop cycles, fertilizer and prototype Rain Caller: [geography](../civilization-mod/geography.md), [weather](../civilization-mod/weather.md) |
| Industry | Finite coal/oil fields, prospecting, physical extraction, refinery, pipes, workshops, graded equipment/repairs and bulk cargo stores: [industry](../civilization-mod/industry.md) |
| Construction | Half-grid cutting, contextual multiblock guides and actual structural-item placement: [cutting](../civilization-mod/cutting.md), [multiblocks](../civilization-mod/multiblock-builds.md) |
| Heat | Persistent local heat, material exchange, finite waste heat, shelter/rain effects, felt temperature, comfort and survey helmet: [heat](../civilization-mod/heat.md) |
| Vehicles | Freeform engine-powered boats and a development-power airship prototype with assembly/disassembly and terrain safety: [vehicles](../civilization-mod/vehicle-physics.md) |
| Physical foundation | Shared material/geometry properties, inspector and static Sable integration; dynamic contents mass is still pending: [physics](../civilization-mod/physics.md) |
| Presentation | Shared cabinet interfaces, JEI recipe guidance and native asset pipeline: [mod reference](../civilization-mod/README.md), [modeling](../tools/modeling/README.md) |

## Validation and limits

- **Latest tooltip check:** 0.34.25-dev build and fast unit tests passed; deployed JAR checksum verified. Item descriptions now use short purpose/control/warning hints, without full construction bills. No in-game tooltip review was run for this text-only change.
- **Latest gameplay check:** all **258 server GameTests** passed in 0.34.24-dev. The complete modeled Oil Derrick covers material-fed construction, four facings, collision, payment/labor, persistence and exact refunds through controller/brace/port dismantling. The refinery fixture now explicitly supplies its required ticking area.
- **Latest derrick client check:** actual Assemble button, complete native timber tower, moving crosshead, synchronized cabinet and a real player standing on the middle gallery passed under Photon/Faithful. Native asset validation is complete; original art and editable Blockbench source remain local.
- **Latest kitchen client check:** 0.34.22-dev passed under Photon/Faithful, including actual lift/rest/return/serve and a held skillet staying steady through repeated cooling updates. Earlier cookware extraction passed complete asset validation. The subsequent stove heat tuning remains validated by its two-minute enclosed-room test (10–30°F adjacent rise); it has not had a manual Survival review.
- Dedicated local startup and selected graceful vehicle restart checks are verified. Real multi-client transactions/protection, crash recovery across separate saves, restore drills and representative 200-player load remain unverified. Diagnostic logs are not a transaction database.
- Survival kitchen construction, powered docks, cargo-content mass, vehicle lifecycle/crew rules, death recovery, map item search/external counter storage, nuclear flight and sky/weather machinery remain incomplete. Unresolved choices belong in [open decisions](../open_decisions.md).
- Tectonic is a development terrain candidate. Selected fresh-world coal/oil sites passed compatibility checks; navigable rivers, representative routes, generation cost and permanent-world policy remain open. Terrain Diffusion is not adopted.
- **Civilization Compact Campus** is the separate testing save; user worlds remain intact. Its older pump frames remain preserved and need manual recovery followed by controller material assembly. [Campus setup and limits](../civilization-mod/testing.md#user-testing-campus).

## Work direction

Next: ordinary gentle/strong-heat cooking batches to judge cue recognition, carryover, throughput and warmth. Tune from that play before adding dishes, specialties or kitchen stations. Cooking precedes coal-extraction redesign; existing drills/refinery remain implemented. Flight optimization is deferred at the user’s request. The [development plan](../development_plan.md) owns package order and remaining work.

Completed code/docs/runtime resources are committed and pushed under the [GitHub workflow](../AGENTS.md#github-workflow); original art and visual-review history remain local. Older release hashes, experiments and session observations are preserved in the [development record](history/releases/development-through-0.34.23.md) and [history index](history/README.md).
