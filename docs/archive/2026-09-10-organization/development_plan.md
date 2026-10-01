# Civilization — implementation backlog

Current build: **0.11.5-dev**, Minecraft **1.21.1**, NeoForge **21.1.250**, JDK **21**. Version in [gradle.properties](civilization-mod/gradle.properties). Everyday materials and household cooking are implemented; all custom items now have a Civilization creative tab.

Read [game rules](design_direction.md) for agreed behavior and [open decisions](open_decisions.md) for unsettled choices. This file owns work order and implementation status. Original checklist IDs are retained for traceability.

## 1. Implemented foundation

| IDs | Completed scope |
| --- | --- |
| F01 | Development build, test and Prism deployment loop |
| F02–F04 | Persistent calorie reserve/HUD, explicit work costs and depleted recovery |
| F05 | Wheat/fertilizer yield loop, food catalog and calorie-neutral field rations |
| F06 | Mineral Coal from ore and one-way conversion to vanilla coal; custom machines reject renewable fuel |
| F07–F08 | Kiln bricks, vanilla brick production gate and Fertilizer Furnace |
| F09–F11 | Physical multiblocks, contextual textured build guides, inventories/hoppers and interruption/save behavior |
| F12 | Bounded asynchronous player/production and machine event logs |
| F13 | Verification: 47 passing GameTests, including the new everyday package; 19 unit tests in the build |
| F14 | User play evidence for gathering, building, calorie recovery, farming, fertilizer manufacture and multiblock assembly |

Current recipes and limitations: [mod README](civilization-mod/README.md). Normal dedicated multiplayer and 200-player capacity remain unverified. Logs are diagnostic, not a durable transaction database.

Use the [change-specific testing workflow](civilization-mod/testing.md): quick incremental builds for routine edits, server tests for gameplay, and focused visual scenes for rendering. Photon is the primary visual target; add shaders-disabled or secondary-pack checks when the changed behavior affects compatibility, under the [shader support policy](civilization-mod/README.md#shader-support). The extended visual tour is opt-in.

Block texture work must use the [reference-driven block-family pipeline](art/textures/README.md#block-families-required-workflow): one master per block, explicit image edits/references for other faces, shared palette, protected casing regions and assembled-block review. Independent face generation from text prompts is rejected.

**Added in 0.10.0:** 32 ordinary stacks, one-log/one-plank, global bone-meal growth disable, single-block Cooking Stove and uncooked preparations, vanilla cooking/fire-drop/villager bypass gates, machine state feedback and local effects. Git checkpoints are established. Client and separate dedicated development server startup are verified. GameTest worlds are separated; production server EULA/hosting and multi-client validation remain pending.

**Not implemented:** recovery containers, region rules, groups, powered claims/takeovers, shop/currency interfaces, map, boat, oil/uranium industry, trains, airships and regional/sky instruments. The broader vanilla resource/fuel audit remains open.

## 2. Proposed coherent packages

This is a dependency order. Each package needs only its relevant unresolved decisions; completed parts are recorded explicitly.

| Package | Player-visible result | Dependencies / acceptance |
| --- | --- | --- |
| A. Everyday material rules — implemented | Stacks, timber conversion, bone-meal boundary, cooking and existing-machine feedback | No oversized-stack preservation requested for this development update. Coal-free food recovery is tested; normal balance/play feedback remains |
| B. Regional foundation | Terrain-linked farming/forestry, prospecting groundwork and a navigable sample region | D03/D04/D12. Boundaries remain consistent across reload and do not require repeated terrain scans |
| C. Shared ownership and protection | Solo/group assets and powered land controllers with complete takeover behavior | D06–D10 plus multiplayer persistence. Test full-access groups, zero-power loss and paid handover together |
| Recovery, alongside C | Owner-only persistent death container | D10 and inaccessible-claim behavior; separated from A so custody decisions do not block materials/cooking |
| D. Local trade and discovery | Physical fuel-priced shops, funded orders and map-visible offers | C, D05/D11. Two clients trade safely; offline owner works; no remote goods or stock duplication |
| E. River freight | Load a coal boat, carry a useful shipment and unload at a bank | B–D, D03/D05/D10. Fuel exhaustion/restart preserves cargo; actual route works in both directions |
| F. Regional economy evidence | A farm/forest/mine, carrier and buyer voluntarily exchange repeatedly | A–E. Measure labor, food, fuel, stock, travel and useful demand; integrated ownership remains legitimate |
| Later. Industrial and regional scale | Oil/uranium, rail, large construction, weather and sky services | Useful ground economy, settled process inputs and appropriate server/physics tests |

Basic server readiness proceeds before persistent player investment and alongside these packages. Coherent larger development steps are preferred; appropriate automated checks do not require a manual playtest between every feature.

## 3. Checklist

Entries remain open at their full scope unless explicitly marked complete. Partial implementations are described rather than treated as finished systems.

| ID | Remaining deliverable |
| --- | --- |
| E00 | Complete material/energy input-output table and non-circular coal → oil → uranium bootstrap when returning to industry |
| E01 | Resource geography, discovery, reserve sizes and extraction behavior |
| E02 | Live-world lifespan and reserve/expansion/reset policy |
| E03 | Quantified first production table: capital, inputs, yield, time, energy and labor |
| E04 | Cooking, bone-meal growth and ordinary material rules implemented; finish the broader vanilla resource/fuel/automation audit |
| E05 | Final fertilizer feedstock or deliberate retention of the clay/gravel placeholder |
| E06 | Identify useful outputs and customers before adding machines |
| E07 | Finish passenger/dimension/flight rules; enforce already-agreed storage/freight restrictions |
| C01 | Solo/full-access group lifecycle and bounded controller claims |
| C02 | Shared access enforcement and administrative authority; no custom member role tree |
| C03 | Protection across multiblocks, inventories, automation and environmental effects; explicit adoption of existing assets |
| C04 | Physical-fuel trade counter with sell/funded-buy offers and map discovery; no currency-issuance engine |
| C05 | Persistent ownership/transfer records and bounded diagnostic attribution |
| C06 | Complete monthly takeover, zero-power loss, block removal, relocation, group departure and recovery behavior |
| W01 | Sample navigable region and coherent production/settlement opportunities |
| W02 | Regional growth/deposit implementation with finite accounting and existing-world migration |
| W03 | Agreed production changes and later useful industrial machines |
| W04 | Coal cargo boat and local stockpiles; lifecycle and loading rules |
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
| V02 | Immersive daily life at every scale: place, physical interaction, readable work and visible consequence; see the proposed treatment in open decisions |
| V03 | Reusable architecture, signage, materials, lighting and sound/effect vocabulary; begin by treating existing farming and machines before adding bespoke atmosphere systems |
| V04 | One compelling prestige destination and the experience of owning/visiting/supplying it |
| V05 | Visible working sides of beautiful places, a single great sky transmitter and majestic regional receiving towers; one active beam target at a time |
| V06 | Plain player-facing names and contextual interfaces |
| L01 | Advanced-capability dependencies: central sky machine, one receiver at a time, monumental rain towers, and explicit switching/buffer rules |
| L02 | One sky asset's construction, access, energy clock and shortage behavior |
| L03 | Later airship/distant-rendering integration, including extremely fast prestige airships, their energy demand and safe high-speed travel limits |
| L04 | Optional organized PvP participation, stakes and protection boundary |
| L05 | Public access staged by operational and performance evidence |

Keep the original distinction: a successful functional test does not prove an economy, and a small economy test does not prove 200-player capacity.

## 4. Common implementation contracts

Apply these once across features, not as a repeated warning in every design document:

- **Conservation:** actual stock, fuel and reserved payments cannot be spent twice; transformations explicitly consume inputs.
- **Authority:** the server resolves owner/group access at action time, including automation and already-open menus.
- **Persistence:** claims, inventories, reservations and deadlines commit consistently and survive retries/restart. Diagnostic JSONL is not their source of truth.
- **Bounded work:** region inspection, map listings and claims do not force-load the world. Define active/unloaded/offline behavior per consumer. Do not make every object share an unsuitable universal clock.
- **Migration:** future live-world changes need an explicit migration policy. For the 0.10.0 development stack reduction, the user explicitly waived preservation of over-limit items; no migration was built.
- **Feedback:** show the useful requirement, state and failure reason; keep implementation identifiers out of ordinary play.

A ready package has a concrete interaction, agreed rules, data ownership, lifecycle, bounded work and observable acceptance cases. Prices and rates can stay tunable; unresolved ownership or conservation cannot.

## 5. Scope discipline

Keep physics/airships, advanced industry, global power networks, elaborate contracts, NPC labor, detailed ecology/chemistry and universal wear out of the immediate implementation queue. This defers mechanisms, not the agreed high-ceiling civilization vision.

Create Aeronautics is the preferred airship candidate; Distant Horizons is a rendering candidate. No compatible dependency set or 200-player result is established. Recheck official sources and APIs when that integration work begins; dated platform research remains in the archive.

Advance by whole player loops, not a new system for every noun. Maintain design decisions in one place; mark implementation complete only with evidence.

### Shipped: shaped construction (0.11.3)

Matching pieces also recombine in any two crafting slots without a saw, through eighth → quarter → half → whole. Two-slab decorative recipes use one full source block to avoid conflicts.

Durable saws cut full blocks → slabs → quarter beams → eighth cubes on a 2×2×2 grid. Vanilla slab items and horizontal blocks are integrated, with vertical placement and previews. Joining conserves material. Machines use beam supports and eighth-cube roof corners. See [cutting](civilization-mod/cutting.md). All eight cells can now be filled independently with mixed sizes and materials, including non-rectangular assemblies. Collision and previews follow the occupied cells, and breaking returns the constituent material volumes.

### Shipped: longer world days (0.11.5)

Full Overworld cycle defaults to 60 minutes using NeoForge’s native fractional day clock, with unchanged simulation tick speed and vanilla phase proportions. One-time world initialization preserves later admin overrides. See [implemented clock rules](civilization-mod/README.md#day-and-night).
