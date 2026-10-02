# Civilization — implementation backlog

[Status](docs/status.md) owns the current build and validation; [design](design_direction.md) owns agreed rules; [open decisions](open_decisions.md) owns unresolved choices. This file owns work order and remaining deliverables. Original checklist IDs are retained.

Current priority: Tier 2 cooking, then coal extraction. The requested full Oil Derrick redesign is implemented with progressive held-material construction, contextual guides and local section repair; [industry](civilization-mod/industry.md#build-and-supply-machines) owns its construction and migration. Flight optimization is deferred; the user will build the demonstration area later, so W01 is not a prerequisite.

### Tier 2 cooking — first prototype plan

**Implemented:** one Creative/admin stove with continuous heat, sensory quality, portioned work meals, removable skillet and paid carryover. Three imported audio loops retain a frying bed throughout, with plateau crackle and overcooking sputter; cue recognition still needs ordinary play. The preparation table is deferred; full kitchen multiblocks remain later work. [Cooking](civilization-mod/cooking.md) owns controls, recipes, timing, fuel and persistence; [testing](civilization-mod/testing.md) owns checks.

**Oven physical pass implemented:** the native cast-iron model is Creative-placeable with a 2x2x2 body envelope, player-height casing, a flue above that envelope and chimney clearance, opening main door, transparent window and continuous draft dial. [Cooking](civilization-mod/cooking.md#baking-oven-physical-first-pass) owns placement, controls and collision limits; the [local asset](art/assets/baking_oven/asset.json) owns native sources and visual evidence. Fuel, heat, trays, recipes and Survival construction remain deferred.

**Next:** try the physical oven in an ordinary kitchen, then choose its first baking batch. Continue ordinary stove play to assess sensory timing, carryover, warmth and rewards. Preparation remains deferred; there is no cooking XP or chopping mechanic.

### Builder's Line — implemented package

The offhand reel marks two endpoints with right-click and immediately starts axis-aligned building or held-tool demolition on the second click. The first pass includes cached previews, cancellation, server-authoritative hits and work, normal stock/labor/tool/drop accounting, conservative full-block coverage and a Smithy recipe. [Construction](civilization-mod/cutting.md#builders-line) owns controls, costs and limits; [status](docs/status.md) owns actual validation/deployment; the [asset](art/assets/builders_line/asset.json) owns original sources and inspected visual receipts.

Next: ordinary Survival building/demolition play to judge length, pace and reach. Cut pieces, functional blocks and vessels remain [follow-up choices](open_decisions.md#builders-line--follow-up-choices), rather than silently broadening this item. Cooking and coal work retain their existing order after this requested tool.

## 1. Implemented foundation

| IDs | Completed scope |
| --- | --- |
| F01 | Development build, test and Prism deployment loop |
| F02–F04 | Persistent calorie reserve/HUD, explicit work costs and depleted recovery |
| F05 | Food catalog, calorie-neutral field rations, regional rain/soil storage and two-hour farmland cycles with shared fertilizer yields; [coverage and exceptions](civilization-mod/weather.md) |
| F06 | Unified Coal from finite ore/salvage; vanilla coal conversion, charcoal and coal-block production retired |
| F07–F08 | Kiln bricks, vanilla brick production gate and Fertilizer Furnace |
| F09–F11 | Physical multiblocks, contextual textured build guides with a [shared performance contract](civilization-mod/multiblock-builds.md#guide-performance-contract), [tier-appropriate machine storage](civilization-mod/README.md#machine-item-storage), hoppers and interruption/save behavior |
| F12 | Bounded asynchronous player/production and machine event logs |
| F13 | Automated gameplay/unit tests and focused rendering checks; current results in project status |
| F14 | User play evidence for gathering, building, calorie recovery, farming, fertilizer manufacture and multiblock assembly |
| F15 | Everyday material rules, cooking gates, machine feedback and dedicated Street Paver movement bonus |
| F16 | Unified half-grid cutting, mixed-cell placement, volume-based calorie charge and crafting recombination; vanilla and Street Pavers slab crafting plus vanilla slab stonecutting retired in favor of the saw |
| F17 | Original texture pipeline and compact [asset workflow](tools/modeling/README.md#complete-asset-pipeline), with reusable materials, declared density, visual proofs and retained legacy families |
| F18 | Native 60-minute day/night default with saved admin overrides |
| F19 | Shared biome geography, river crop eligibility, woodland growth and hoe inspection |
| F20 | Three-tier powered claims, chest-style coal storage, per-claim whitelists, held takeovers/refunds and local trade counters |
| F21 | 2×2 Survey Table, automatic local terrain survey, public claim/counter markers and inspection |
| F23 | Pipe-connected fired heater, tall distillation column and air-cooled condenser, rebuilt with original reference-driven textures, rounded cut-block shells, supported condenser, cooling grilles, narrow flues and animated sight-glass pipes; fuel/oil/sulfur outputs, drill lubrication and sulfur-enriched fertilizer |
| F22 | Physical regional coal/oil deposits, block-removing drills, visibly draining reservoirs, flowing oil-seep surface clues, vertically flexible derrick siting, prospecting, shared liquids/tanks/canisters and the coal → crude → refined fuel → industrial coal loop |

## 2. Proposed coherent packages

Advance connected player loops. Basic operational readiness proceeds alongside development and before persistent player investment.

| Package | Remaining deliverable / dependency |
| --- | --- |
| Regional foundation | Navigable sample terrain and representative reserves; resolve travel, river elevation and world lifespan (D03/D04/D12) |
| Ownership and recovery | Real multi-client/protection audit plus owner-only death recovery; settle inaccessible-claim and vehicle custody rules (D08–D10) |
| Trade and discovery | External storage, item search/pins and actual competing-client transactions (D05/D11) |
| River freight | Powered docking, practical routes and vessel lifecycle; depends on regions, permissions and fuel/cargo policy |
| Economy evidence | Repeat voluntary farm/forest/mine → carrier → buyer exchanges; measure labor, food, fuel, stock, travel and useful demand |
| Later scale | Broader oil/uranium industry, rail, large construction and sky/weather services after useful ground demand |

### Regional weather and farming — implemented package

[Weather and farming](civilization-mod/weather.md) owns shipped rain, soil, crops, fertilizer and prototype Rain Caller behavior. Next: [non-farmland habitat and food-economy audit](open_decisions.md#regional-weather-and-farming-package), especially berries and supplied animal food. Tower construction, Survival energy/access and offline production remain unsettled.

### Town workshops — initial package implemented

[Industry](civilization-mod/industry.md#town-workshops) owns current station recipes, equipment grades/repairs, acquisition gates and shared cabinet behavior. Next: play repair/replacement economics and finish [special-equipment/acquisition coverage](open_decisions.md#town-workshop-implementation-details). Supplied husbandry, powered manufacturing and maker customization follow separately; settlement density still needs economic evidence.

### Shelter and heat — initial foundation

[Heat](civilization-mod/heat.md) owns the field, comfort, rain/shelter, waste heat and diagnostics. Next: review actual-world workshop equilibrium, roof/chimney layouts, stored-hotspot cooling and solver age. Section-local scheduling and representative load remain open; clothing, moving-vessel heat, deliberate heat transport and internal machine temperatures need separate decisions.

### Unified physical foundation — material/geometry stage implemented

[Physics](civilization-mod/physics.md) implements shared material/geometry properties, the inspector and a pinned-Sable static-property proof. Next: dynamic per-position mass hooks and domain-aware thermal state on moving vessels, then explicit fuel/work accounting and individually approved physical consequences. Inventory/tank mass requires contents policies. [Architecture and acceptance](open_decisions.md#unified-physical-foundation--proposed-implementation-plan).

### Bulk freight — planned implementation package

**Implemented:** land Coal Bunker/Cargo Tank, freeform hulls, physical Hot-Bulb Engines and vessel-mounted bulk stores. [Industry](civilization-mod/industry.md#bulk-freight-storage) and [vehicles](civilization-mod/vehicle-physics.md#freeform-motor-vessels) own their rules.

**Remaining sequence:**

1. Stationary mooring and engine-driven loading on land, with explicit port-to-port transfers. Verify disconnect/movement, full target, lost access, chunk unload and competing transfers preserve stock.
2. One extraction → loaded voyage → unloading loop on a representative river. Demonstrate useful freight capacity, credible fuel cost and graceful reload; test peer docking/access before claiming multiplayer readiness.
3. Finish the relevant assembly/feedback review through the [asset pipeline](tools/modeling/README.md).

Reuse existing guides, cabinet controls, fluid/stock adapters, claim checks and engine accounting. Capability access must obey the same stock bounds. Keep the coal-fired oil bootstrap; no universal grid, shaft simulation, boilers, new fuel grades, automatic trade or crew framework is part of this package. [Unresolved dock and lifecycle rules](open_decisions.md#bulk-freight-implementation-details).

### Airships — active planning package

The [controller flight prototype](civilization-mod/vehicle-physics.md#airship-prototype) is implemented; Survival nuclear flight remains unfinished. Flight performance tuning is deferred. [Agreed scope](design_direction.md#controller-built-antigravity-airships); [remaining mechanics](open_decisions.md#controller-built-airships--proposed-implementation-plan).

| Stage | Remaining acceptance |
| --- | --- |
| 1. Flight/custody proof | Broader landing/contact coverage; retain exact hull, cargo and identity through assembly, disassembly and reload |
| 2. Dynamic mass/control | Per-position material/cargo mass, center of mass/inertia and finite stabilization; load/edit/reload invalidation |
| 3. Aerodynamics/wind | Calibrate exterior geometry, drag and regional wind using equal-mass hulls, load changes and head/tail/crosswinds |
| 4. Survival nuclear loop | Resolve processing, fuel packaging, power limits and recovery; verify explorer → processing → flight → refuel with exact finite consumption and visible lift margin |
| 5. Travel/presentation | Real-peer access/reconnect, walking aboard, safe terrain crossings/braking, bounded load, refined controls and controller art |

Use targeted conservation/lifecycle checks and focused flight scenes; long voyages do not substitute for explicit acceptance. No custom vessel model is required.

## 3. Checklist

Entries remain open at their full scope unless explicitly marked complete. Partial implementations are described rather than treated as finished systems.

| ID | Remaining deliverable |
| --- | --- |
| E00 | Coal → crude → refined fuel → industrial coal bootstrap implemented; extend the non-circular progression to later industry/uranium |
| E01 | Initial coal/oil geography, discovery and physical block/fluid depletion implemented; balance distribution/lifespan and add later resources |
| E02 | Live-world lifespan and reserve/expansion/reset policy |
| E03 | Three-stage oil refinery, basic pipes, lubrication and sulfur fertilizer implemented in [industry](civilization-mod/industry.md); tune labor, freight and permanent-world throughput with play evidence |
| E04 | Cooking, bone-meal growth and ordinary material rules implemented. [Thermal/coal consolidation](civilization-mod/industry.md#thermal-processing-and-retired-vanilla-systems) implemented; finish the [remaining vanilla audit](open_decisions.md#vanilla-replacement-audit) |
| E05 | Advanced animal husbandry remains deferred. Current fertilizer is sulfur + gravel only; the clay route is removed. Revisit any animal-derived fertilizer role before implementation |
| E06 | Production coal costs raised 4× through a shared budget; [rates](civilization-mod/industry.md#production-coal-balance). Evaluate town demand against extraction before further tuning; identify useful outputs and customers before adding machines |
| E07 | Finish passenger/dimension/flight rules; enforce already-agreed storage/freight restrictions |
| E08 | Initial metalworking bridge implemented: kiln bricks → Foundry → ingots/steel → Machine Parts and steel casings for T2 controllers. See [industry](civilization-mod/industry.md). Initial [town workshops](civilization-mod/industry.md#town-workshops), wool/hide inputs, smithy equipment/repairs and catalog acquisition gates are implemented; extend special-equipment and alternate-resource coverage |
| E09 | Later maker customization: selectable equipment color accents and pixel-drawn marks/signatures; grid size around 6×6 remains a candidate. Resolve representation and UI before implementation |
| C01 | Owner/whitelist lifecycle, chest-style coal inventory, attached native-style controls, and three claim tiers with switching fees/cooldown implemented |
| C02 | Full-access claim whitelists, owner-only list administration and operator land bypass implemented; production administration policy remains |
| C03 | Common block/container/environmental and vanilla automation protection implemented; broader vanilla/mod/vehicle audit remains |
| C04 | Local item-for-item counter with typed batch quantities, shared chest storage and original block textures implemented; initial physical map discovery implemented; item search, destination pins and external storage attachment remain |
| C05 | Current ownership, escrow and stock persist in SavedData; historical transaction audit and crash recovery remain |
| C06 | Initial takeover, no-grace loss/refund, anchored obligations and whitelist revocation implemented; private death recovery remains |
| W01 | Sample navigable region and coherent production/settlement opportunities |
| W02 | Regional crop/forestry growth and coal/oil deposits with finite accounting implemented; new worlds are the development target, broader world generation remains |
| W03 | Agreed production changes and later useful industrial machines; explore [powered work at increasing scale](design_direction.md#emergent-construction-and-increasing-scale) |
| W04 | Initial small refined-fuel/oil boat implemented with stable handling, bounded construction and chest/barrel cargo; large hulls, crew custody, decommissioning and broader shore/multiplayer checks remain |
| W05 | Understandable solo and trusted-group arrival path without basic actions requiring a claim |
| W06 | Economic scenario measuring actual voluntary exchange and supplier interruption |
| T01 | Completed: local Git repository and pre-update checkpoint; retain checkpoints during development |
| T02 | Separate local development server configuration and startup verified; production EULA/hosting setup remains |
| T03 | Real multi-client verification of state, permissions, transfers and reconnect/restart |
| T04 | Consistent backups and a demonstrated restore before public investment |
| T05 | Compact diagnostics for energy, production, transfer failures and server performance |
| T06 | Hosting/maintenance budget and active-chunk, entity and machine limits |
| T07 | Representative growing-load tests: exploration, crowded hubs, factories, freight, saving and backups |
| T08 | Pin dependencies; keep versions, saves, configuration and implementation docs aligned |
| V01 | Short original setting statement, name and tone |
| V02 | Immersive daily life and useful architecture: shelter/exposure, heat and physical access. Begin with the [heat design discussion](open_decisions.md#emergent-architecture-and-heat); broader opportunities remain deferred |
| V03 | Reusable architecture, signage, materials, lighting and sound/effect vocabulary; begin by treating existing farming and machines before adding bespoke atmosphere systems |
| V04 | One compelling prestige destination and the experience of owning/visiting/supplying it |
| V05 | Visible working sides of beautiful places, a single great sky transmitter and majestic regional receiving towers; one active beam target at a time |
| V06 | Plain player-facing names and contextual interfaces |
| L01 | Advanced-capability dependencies: central sky machine, one receiver at a time, monumental rain towers, and explicit switching/buffer rules |
| L02 | One sky asset's construction, access, energy clock and shortage behavior |
| L03 | Later airship/distant-rendering integration, including extremely fast prestige airships, their energy demand and safe high-speed travel limits |
| L04 | Optional organized PvP participation, stakes and protection boundary; implement the deferred [pickpocketing / wearable PvP flag](design_direction.md#pickpocketing-and-wearable-pvp-flag--planned) system after resolving its interaction rules |
| L06 | The [Oil Derrick](civilization-mod/industry.md#build-and-supply-machines) is a complete native timber tower with progressive held-material construction, two walkable galleries, an animated crosshead and local section repair. Every guided multiblock accepts held structural stacks and shows remaining materials; all guides follow the [shared performance contract](civilization-mod/multiblock-builds.md#guide-performance-contract). Next overhaul the Coal Drill, review derrick construction cost and load, then resolve [machine-centered resource-site sieges](open_decisions.md#extraction-installation-sieges) |
| L05 | Public access staged by operational and performance evidence |

Keep the original distinction: a successful functional test does not prove an economy, and a small economy test does not prove 200-player capacity.

## 4. Common implementation contracts

Apply these once across features, not as a repeated warning in every design document:

- **Conservation:** actual stock, fuel and reserved payments cannot be spent twice; transformations explicitly consume inputs.
- **Authority:** the server resolves owner/whitelist access at action time, including automation and already-open menus.
- **Persistence:** claims, inventories, reservations and deadlines commit consistently and survive retries/restart. Diagnostic JSONL is not their source of truth.
- **Bounded work:** region inspection, map listings and claims do not force-load the world. Define active/unloaded/offline behavior per consumer. Do not make every object share an unsuitable universal clock.
- **Migration:** future live-world changes need an explicit migration policy. Use a fresh-world baseline only when explicitly selected by the user.
- **Feedback:** show the useful requirement, state and failure reason; keep implementation identifiers out of ordinary play.

A ready package has a concrete interaction, agreed rules, data ownership, lifecycle, bounded work and observable acceptance cases. Prices and rates can stay tunable; unresolved ownership or conservation cannot.

## 5. Scope discipline

Keep advanced industry, global power networks, elaborate contracts, NPC labor, detailed ecology/chemistry and universal wear outside the current cooking slice. Sable is the chosen vehicle backend; [remaining dependency questions](open_decisions.md#vehicle-physics-dependency-research--2026-09-10) and Survival mass/flight rules remain separate work.

A functional test does not prove an economy or 200-player capacity. Advance by whole player loops, keep decisions in their owning documents, and mark completion only with evidence.
