# Civilization — implementation backlog

Current build, completed scope and validation: [project status](docs/status.md). Runtime versions: [gradle.properties](civilization-mod/gradle.properties).

Read [game rules](design_direction.md) for agreed behavior and [open decisions](open_decisions.md) for unsettled choices. This file owns work order and implementation status. Original checklist IDs are retained for traceability.

Current design/art priority: [Tier 2 kitchen workshop](open_decisions.md#tier-2-kitchen-workshop), ahead of coal-extraction redesign. The [room mockup](art/assets/t2_kitchen/mockup-04.png) and [four-station sheet](art/assets/t2_kitchen/mockup-03.png) remain planning art. The [first prototype package](#tier-2-cooking--first-prototype-plan) now implements one exposed skillet stove with continuous heat, sensory timing and quality-dependent food benefits. The full multiblock [workshop proposal](open_decisions.md#tier-2-kitchen-workshop) remains later work; ordinary cooking playtests are the next step. Existing asset-family migration and freight work remain backlog.

Art workflow now follows the approved [visual direction](design_direction.md#setting-and-material-style), distinct reference roles, contextual family review and recorded pixel cleanup; see the [pipeline](tools/modeling/README.md#complete-asset-pipeline). Existing art is not automatically regenerated. Machine Parts now have a reviewed original bearing-assembly sprite; [asset record](art/assets/machine_parts/asset.json).

Equipment processing duration now scales with material, causing proportional total coal use at a constant productive burn rate; see [town workshops](civilization-mod/industry.md#town-workshops).

Oil/coal field balancing now gives oil one suitable site per 4×4 generation-cell region, keeps coal's terrain-aware sampling, and uses a compact tapered physical reservoir with an 80 mB Coal Drill batch. The geometry-plus-throughput target is roughly two coal fields extracted per oil field with one fifth of refined fuel reserved for engines; [field and process rules](civilization-mod/industry.md#find-a-site) own the implementation, and [remaining world-scale validation](open_decisions.md#coal-and-oil-balance-validation) remains open.

Surface oil now schedules its initial flow during generation and on loading older one-block seeps; [field behavior](civilization-mod/industry.md#find-a-site) owns the rule.

The engine modeling study is complete: reusable native assembly/UV/camera helpers, a refined engine with a full 3×3 bed, elongated cylinder, continuous cast bed and integrated controls and actual Photon review. Follow [native modeling practice](tools/modeling/MODELING.md) for future shaped assets; the [active engine record](art/assets/oil_engine/asset.json) owns its sources and evidence.

Existing asset art backlog: unify existing assets by family before returning to freight. Frontier supplies, the six food/preparation icons, Steel, the Prospecting Rod and the town workshop assemblies now follow the reviewed pipeline. The Tannery and Textile Workshop have redesigned partial-block structures and original controller fronts; the Smithy retains its reviewed broad forge/bench assembly. Shared industrial steel, cooling grilles and all six industrial controllers now have a [coherent original material pass](art/assets/industrial_steel/asset.json). The [civic cabinets](art/assets/civic_cabinets/asset.json) now use readable ownership/exchange emblems. The [T1 thermal controller family](art/assets/thermal_controllers/asset.json) now includes original kiln/foundry/stove/fertilizer fronts and the Smithy gate. Remaining family audit: coal, saw and prototype equipment art. Review each family with its neighbors; retain already successful Cloth, sulfur, Machine Parts, Oil Engine and canisters unless comparison reveals a specific mismatch.

Texture migration backlog: migrate the remaining custom textures from 32×32 to the new 64×64 target by coherent family, using Street Pavers as the first reviewed example. Update the shared item/block exporters to accept declared native sizes before converting each family, review old and new assets side by side under Photon/Faithful, and preserve pack-aware borrowed vanilla surfaces and separately sized UI/model atlases. Do not enlarge existing PNGs as a substitute for revisiting their source masters and material detail.

Pipes now have a [thicker octagonal steel design with narrow glass inspection strips](art/assets/glass_pipes/asset.json), clear idle interiors and internal directional flow. One Oil Derrick now matches one complete refinery line; pipes share a real per-segment transfer budget. Rates and limits remain in the [pipe reference](civilization-mod/industry.md#liquids-and-pipes).

Torches, campfires, coal machines and lava warm players by heating local cells in the shared environmental field. The v2 field now separates regional climate from stored local heat, models open partial-block faces and buoyant air transfer, accounts for fuel-derived heat and atmospheric loss, and reports solver age. [Current behavior and bounds](civilization-mod/heat.md#initial-thermal-field); [remaining architecture and scale targets](open_decisions.md#thermal-field-redesign--proposal-2026-09-27).

Cold felt temperature now incurs a small, bounded online calorie drain. It follows local air over several seconds. The [heat reference](civilization-mod/heat.md#people-and-machines) owns its tuning and exclusions.

Cut-piece placement now spends calories by added volume, and sleep spends time-based calories including skipped night hours; [current costs](civilization-mod/README.md#calories-and-recovery) and [cut placement](civilization-mod/cutting.md#placement-and-joining) own the details.

Vanilla boat and chest-boat rowing now spends calories per paddled horizontal block for the controlling player; passive travel and powered vessels remain free of rowing labor. [Current rate and exclusions](civilization-mod/README.md#calories-and-recovery).

Shared coal-machine ignition now spends a modest calorie charge per accepted manual strike, including unlucky sparks; rejected clicks are free. [Fire lifecycle](civilization-mod/industry.md#continuous-coal-fires).

JEI now exposes the complete current machine recipe chain plus repairs, cutting/recombining and canister transfers; [coverage and limits](civilization-mod/industry.md#jei-recipe-discovery). New processing systems must add their recipe-viewer adapter alongside production.

Paired T1 material storage is implemented: two input/two output stacks except the Smithy's four ingredients and single output, with unchanged T2 banks. Output collection leaves other stacks in place. [Storage](civilization-mod/industry.md#shared-controller-interfaces).

Elevation gain now has a separate configurable calorie cost and journal entry, retaining the jump charge; see [movement rules](civilization-mod/README.md).

Production XP is restored for kiln-family recipes and added to town workshop output collection; [rates and collection rules](civilization-mod/industry.md#production-experience).

Fertilizer now requires sulfur + gravel; equipment production/repair coal demand scales with material. Both are implemented with recipe-viewer guidance; [current recipes and costs](civilization-mod/industry.md).

Speed-adaptive distant detail is implemented as optional Civilization-side DH 3.3.1 compatibility; build/unit and DH-present/absent flight checks passed. Warp-only ordinary-terrain rendering/mesh suppression passed DH-enabled flight and DH-absent rendering checks. Photon near-LOD fade compatibility is implemented to fill the vanilla-render gap during warp. Warp recovery is one second; warp quality now requests two fixed near/far levels around 32 chunks, with boundary hysteresis. Corridor-first C2ME request ordering is implemented without changing chunk coverage; narrowing the footprint remains a [separate proposal](open_decisions.md#directional-chunk-loading-for-warp-travel). Initial user playtest reports smoother flight and roughly half the CPU at 200 blocks/s; a controlled comparison remains. [Policy and boundaries](civilization-mod/vehicle-physics.md#speed-adaptive-distant-horizons-detail).

A controlled superflat flight benchmark is implemented: a long saved corridor with prepared DH terrain, isolated runs matching the main client's enabled mods/shaders/settings, frame/tick/CPU telemetry and JFR, and separate streaming versus physical-airship trials. Use the [benchmark protocol](civilization-mod/testing.md#controlled-fast-flight-benchmark) before attributing performance changes to scheduling or LOD policy. Forward snapshot coverage, bounded ahead preparation and anticipatory braking now replace the repeated snapshot-bound stop/reaccelerate behavior. A swept-hull terrain guard addresses the thin-wall failure found during validation. Primitive-long collections now remove the measured readiness hash-probe bottleneck. Larger-buffer/governor and reduced-DH-worker experiments were rejected for poor troughs, missing terrain or memory failure; see the [optimization evidence](docs/history/2026-09-20-flight-loading-optimization.md). A first/repeat retained-DH-database trial showed only a 1.1% speed difference and nearly unchanged FPS; first-pass hash coverage remains partial. Request-to-ready tracing now identifies lighting-queue completion as the immediate readiness bottleneck (71% queued LIGHT, 15% waiting to enter LIGHT, 12% publication in the final capture). Three bounded lighting scheduling trials were screened and reverted: batch/priority changes failed tail-latency acceptance, while a drain prompt improved readiness but coincided with poor FPS. Turn-aware incremental preparation now narrows the straight corridor, retains momentum coverage and adds a bounded curved forecast, extending preparation within the same ticket budget. See the [terrain implementation](civilization-mod/vehicle-physics.md#airship-prototype). The user considers steady roughly 1,000-block/s flight sufficient for now; further performance tuning is deferred. Controller disassembly now restores editable world blocks with destination preflight and cargo preservation. Preserve dependencies, FULL completion and immutable physics readiness in later performance work. DH mesh/conversion work remains a separate CPU target. Governor-driven fluctuations remain confirmed by [explicit brake telemetry](docs/history/2026-09-20-readiness-brake-benchmark.md). Fresh generation is outside this optimization target. Measure remaining client frame stalls separately; dense-terrain behavior and overlapping ships remain later validation. Compare one variable at a time using actual airship trials. Spectator streaming alone does not establish smooth flight. See the [diagnostic evidence](docs/history/2026-09-20-airship-safety-hitches.md).

### Tier 2 cooking — first prototype plan

Status: **first stove prototype implemented**. Latest scope: skip the preparation table and build the stove. A dedicated open-stand stovetop with quiet original 64px materials and fitted component-specific surface details, one exposed skillet, continuous dial, evolving color/sound, present-quality serving, portioned calories and one generic work benefit is available in Creative/admin playtests. This is a narrow proof before the full kitchen's multiblock construction.

[Cooking reference](civilization-mod/cooking.md) owns the recipe, actual controls, timing calibration, calorie budget, work benefit, finite fuel and persistence. The [stove asset](art/assets/prototype_stove/asset.json) owns generated direction, editable native model and actual review evidence; the [served meal asset](art/assets/vegetable_skillet/asset.json) owns its original 64px sprite.

Automated acceptance covers a continuous rate/quality plateau, ingredient reservation and blocked output, current-quality serving exactly once, finite fuel and cold pause, reduced stove room heat from saved fuel, an adjacent-torso kitchen heat check, saved batch/dial/budget, input validation, actual meal consumption, nonstacking work benefit and excluded sleep costs. The hidden `kitchen` scene exercises actual rotary drag to the server, working food/pointer/fire, serving, menu and inventory appearance under Photon/Faithful. Exact build/test/deployment results live in [status](docs/status.md).

Next: play repeated gentle and strong-heat batches and judge cue recognition and useful throughput. Player learning from color alone and sound alone remains unverified. Tune trial defaults from that evidence before adding more dishes, specialties, cooking positions, residual heat or other kitchen stations. There is no cooking XP or chopping mechanic.

## 1. Implemented foundation

| IDs | Completed scope |
| --- | --- |
| F01 | Development build, test and Prism deployment loop |
| F02–F04 | Persistent calorie reserve/HUD, explicit work costs and depleted recovery |
| F05 | Food catalog, calorie-neutral field rations, regional rain/soil storage and two-hour farmland cycles with shared fertilizer yields; [coverage and exceptions](civilization-mod/weather.md) |
| F06 | Unified Coal from finite ore/salvage; vanilla coal conversion, charcoal and coal-block production retired |
| F07–F08 | Kiln bricks, vanilla brick production gate and Fertilizer Furnace |
| F09–F11 | Physical multiblocks, contextual textured build guides, [tier-appropriate machine storage](civilization-mod/README.md#machine-item-storage), hoppers and interruption/save behavior |
| F12 | Bounded asynchronous player/production and machine event logs |
| F13 | Automated gameplay/unit tests and focused rendering checks; current results in project status |
| F14 | User play evidence for gathering, building, calorie recovery, farming, fertilizer manufacture and multiblock assembly |
| F15 | Everyday material rules, cooking gates, machine feedback and dedicated Street Paver movement bonus |
| F16 | Unified half-grid cutting, mixed-cell placement, volume-based calorie charge and crafting recombination; vanilla and Street Pavers slab crafting plus vanilla slab stonecutting retired in favor of the saw |
| F17 | Original texture pipeline, now adding 64×64 road paving alongside existing 32×32 families; resource-pack-aware materials and renderer synchronization |
| F18 | Native 60-minute day/night default with saved admin overrides |
| F19 | Shared biome geography, river crop eligibility, woodland growth and hoe inspection |
| F20 | Three-tier powered claims, chest-style coal storage, per-claim whitelists, held takeovers/refunds and local trade counters |
| F21 | 2×2 Survey Table, automatic local terrain survey, public claim/counter markers and inspection |
| F23 | Pipe-connected fired heater, tall distillation column and air-cooled condenser, rebuilt with original reference-driven textures, rounded cut-block shells, supported condenser, cooling grilles, narrow flues and animated sight-glass pipes; fuel/oil/sulfur outputs, drill lubrication and sulfur-enriched fertilizer |
| F22 | Physical regional coal/oil deposits, block-removing drills, visibly draining reservoirs, flowing oil-seep surface clues, vertically flexible derrick siting, prospecting, shared liquids/tanks/canisters and the coal → crude → refined fuel → industrial coal loop |

Persistent server-owned recipe selection now controls kiln-family output, including the clay/bricks/terracotta overlap; see [recipe selection](civilization-mod/industry.md#shared-controller-interfaces).

Shared paced manual ignition, continuous coal feeding and low idle burning are implemented across all coal machines; see [fire lifecycle](civilization-mod/industry.md#continuous-coal-fires).

Coal-fire environmental heat now follows actual productive or idle reserve spending instead of coal insertion; see [heat accounting](civilization-mod/heat.md#initial-thermal-field). The broader fuel/work/heat transaction remains in the [physics architecture](design_direction.md#shared-physical-foundation).

Current recipes and limitations: [mod README](civilization-mod/README.md). Normal dedicated multiplayer and 200-player capacity remain unverified. Logs are diagnostic, not a durable transaction database.

Use the [change-specific testing workflow](civilization-mod/testing.md): quick incremental builds for routine edits, server tests for gameplay, and focused visual scenes for rendering. Photon is the primary visual target; add shaders-disabled or secondary-pack checks when the changed behavior affects compatibility, under the [shader support policy](civilization-mod/README.md#shader-support). The extended visual tour is opt-in.

Block texture work must use the [reference-driven block-family pipeline](art/textures/README.md#block-families-required-workflow): one master per block, explicit image edits/references for other faces, shared palette, protected casing regions and assembled-block review. Independent face generation from text prompts is rejected.

Unimplemented systems and operational limits are summarized in [project status](docs/status.md); their work items follow below.

## 2. Proposed coherent packages

This is a dependency order. Each package needs only its relevant unresolved decisions; completed parts are recorded explicitly. The current priority is to connect working systems across the progression. The user will handle demonstration-area construction later; W01 is not a prerequisite for continuing system implementation.

| Package | Player-visible result | Dependencies / acceptance |
| --- | --- | --- |
| A. Everyday material rules — implemented | Stacks, timber conversion, bone-meal boundary, cooking and existing-machine feedback | No oversized-stack preservation requested for this development update. Coal-free food recovery is tested; normal balance/play feedback remains |
| B. Regional foundation — partially implemented | Shared geography, river farming, woodland growth, prospecting and broad terrain-aware coal/oil fields are implemented; navigable sample terrain remains | Current field shapes and extraction behavior are in [industry](civilization-mod/industry.md). D03/D04/D12 still govern transport-ready world generation, local river elevations and world lifespan |
| C. Shared ownership and protection — initial system implemented | Owner/whitelist controllers, three generous powered claim tiers, takeover handover/refund and common protection hooks | [Ownership/trade reference](civilization-mod/ownership-trade.md). Real multi-client and broader automation/grief hardening remain; recovery is separate |
| Recovery, alongside C | Owner-only persistent death container | D10 and inaccessible-claim behavior; separated from A so custody decisions do not block materials/cooking |
| D. Local trade and discovery — counters and initial map implemented | Single-offer barter counters with ghost selectors, shared chest storage and offline-owner operation; physical table displays nearby terrain, claims and counter offers | D05/D11. Server tests cover conservation and competing buyers; actual multi-client transactions remain to verify |
| E. River freight — freeform vessel foundation implemented | Connected player-built vessels with two-wide wheel/gear console, physical Hot-Bulb Engines, mass-scaled handling and supported bulk/chest cargo; practical bank-to-bank routes and docking remain | B–D, D03/D05/D10. Engine exhaustion/restart preserves cargo; actual route works in both directions |
| F. Regional economy evidence | A farm/forest/mine, carrier and buyer voluntarily exchange repeatedly | A–E. Measure labor, food, fuel, stock, travel and useful demand; integrated ownership remains legitimate |
| Later. Industrial and regional scale | Oil bootstrap implemented; broader oil/uranium industry, rail, large construction, monumental weather control and sky services | Useful ground economy, settled process inputs and appropriate server/physics tests |

Basic server readiness proceeds before persistent player investment and alongside these packages. Coherent larger development steps are preferred; appropriate automated checks do not require a manual playtest between every feature.

### Regional weather and farming — implemented package

Implemented: persisted random regional weather, the prototype Rain Caller, capped soil water, normalized farmland growth/repeat fruit, one-dose fertilizer yields, hoe feedback and Survey Table district inspection. Rules live in the [weather design](design_direction.md#regional-weather-and-farming-package); behavior and limits live in [weather and farming](civilization-mod/weather.md).

Acceptance covers all eight farmland crop types, active-time water/growth, persistence, roof exclusion, district borders, duplicate calls, manual harvests of both pitcher halves, repeat fruit and replacement-fruit yield conservation. Two hidden clients verified simultaneous wet/dry districts under Photon. Exact build/deployment results live in [status](docs/status.md).

Next agricultural work is the [non-farmland habitat and food-economy audit](open_decisions.md#regional-weather-and-farming-package), especially berries/animal food versus the two-hour field economy. Tower construction, survival rain access, energy costs and offline production remain later decisions. Do not treat the current crop matrix as a complete biological-resource audit.

### Town workshops — initial package implemented

Shared interface package implemented, including compact icon/name recipe rows, contextual category/material filters and mockup-matched cabinet frame/nameplate detailing: [cabinet assets, common controls, two coal slots and recipe guidance/automatic matching](civilization-mod/industry.md#shared-controller-interfaces) across the current machine and civic controller screens, with the same visual family on the normal player inventory, standard chest inventories and crafting tables. The centered fire is animated without an obscuring hover tooltip. Specialized extraction, repair and fuel policies remain explicit; future machines reuse these components. Focus next on playing the workshop loop and the outstanding coverage below.

Direction: [town workshops and the rural–urban production loop](design_direction.md#town-workshops-and-the-ruralurban-production-loop). The existing Foundry now feeds Tannery, wool-based Textile Workshop and Smithy multiblocks. [Workshop recipes and limits](civilization-mod/industry.md#town-workshops) own shipped quantities, equipment coverage, repair wear, menus, fuel, persistence and acquisition gates.

Completed: Raw Hide/Cloth, productive coal processing, distinct shaped guides, basic metal equipment and netherite upgrades, graded damageable equipment with a 75% mean/20-point spread, one grade-based repair-wear rule and a fixed-width black/gray/green/cyan durability bar, saved individual wear patterns, no repair XP charge, identity/enchantment retention, Mending disable and catalog crafting/repair/trade/spawned-gear gates. The [workshop reference](civilization-mod/industry.md#equipment-grade-and-repairs) owns the current distribution, stat effects, bar display and Faithful 32× item wear; build/server/visual results live in [status](docs/status.md).

Controller cleanup completed: workshop jobs persist by stable ID with legacy index fallback; coal machines use one shared fire host contract and named menu status values. Industrial formation now shares the bounded part validator and distinguishes unloaded from broken shells in one check. [Workshop persistence and fire lifecycle](civilization-mod/industry.md#town-workshops) remain the behavior reference. Profile civic saves and a representative thermal-helmet population before changing those broader systems further. Preserve existing-world migration until an explicit new-world baseline is chosen.

Repository hygiene: inventory the pre-existing untracked source and assets with their owners before making an intentional Git checkpoint. The current pass did not stage, delete or commit unrelated work. Avoid a package-wide move or formatting rewrite; keep the next cleanup tied to measured cost or a concrete maintenance problem.

Current equipment progression pass:

- [x] Make gold tools and armor a durable, effective tier between iron and diamond, including iron-level harvesting.
- [x] Scale and verify grade on armor defense, toughness and knockback resistance, component-defined tool/weapon stats and fired projectile damage.
- [x] Cut the Tannery's iron cost so tanning and leather armor precede the Smithy.
- [x] Reduce tanning to 10 seconds and 0.25 Coal per Leather; [current workshop costs](civilization-mod/industry.md#town-workshops).
- [x] Match weaving to 10 seconds and 0.25 Coal per two Cloth; [current workshop costs](civilization-mod/industry.md#town-workshops).
- [x] Move the Shield to the Smithy and remove its vanilla crafting route; [workshop catalog](civilization-mod/industry.md#town-workshops).
- [x] Make stone tools recover every controller and the related industrial construction blocks; [mining rules](civilization-mod/README.md#machine-item-storage).
- [x] Give 100%+ armor automatic random vanilla trims and close the vanilla Smithing Table trim route.

Next: review commercial repair/replacement economics and extend the [remaining acquisition/special-equipment audit](open_decisions.md#town-workshop-implementation-details). Workshop assembly art is complete for this pass: the Smithy has its broad forge/bench/anvil form, the Tannery a low open vat and rack, and the Textile Workshop a low bench/open loom with one copper roller. Tannery/Textile/Smithy controller faces are original; construction remains resource-pack aware. Supplied husbandry/fertilizer production, powered machine works and maker customization follow separately. Natural density remains an economy hypothesis to validate through freight, stocking and trade; no population bonus forces it. The user handles town/demo construction.

### Shelter and heat — initial foundation

Structural performance work includes bounded discovery/emission/diffusion, atomic publication and save safety, chunk-indexed sources, coordinate hashing and bounded caches. The v2 field removes persistent per-cell climate references and increases active capacity; snapshot copying is now sliced across ticks, while helmet samples are spatially filtered and sent every two seconds in compact packets. Section-local scheduling and a representative 200-player benchmark are still required. Chimney exhaust distribution, bell-shaped comfort and the [historical vanilla biome-family climate audit](docs/history/biome-climate-audit-2026-09-27.md) have passed fixture checks; common woodland, rivers and plains now receive mildly cool daylight. [The heat reference](civilization-mod/heat.md#initial-thermal-field) owns current tuning. Civilization Dev's heat switch remains on. Next: assess warmth, stored-hotspot cooling and solver age in the user's actual world.

The [approved heat direction](design_direction.md#shared-heat-and-thermal-comfort) now has a [first implementation](civilization-mod/heat.md): persistent local heat, material resistance/mass, finite machine waste heat, regional rain cooling and lingering recovery, roof-sensitive heated work, chimney-weighted exhaust with stored burner/shell warmth, comfort benefits, bounded thermal-machine fuel adjustment and a development thermal-camera helmet. A compact survival thermometer and inventory bonus tooltip expose felt temperature, local air, comfort and active road benefits; a whole-frame temperature wash adds visual feedback. Next: review small-workshop equilibrium, roof/chimney layouts and cooling rates in a survival world, then resolve detailed internal machine temperatures, clothing, moving vessels and deliberate heat transport. Broader architecture/access and larger-scale powered work remain future development. Bulk freight remains planned below.

### Unified physical foundation — material/geometry stage implemented

[Proposed architecture and acceptance](open_decisions.md#unified-physical-foundation--proposed-implementation-plan): common material/geometry properties → safe world/vessel thermal state → explicit fuel/work accounting → individually approved physical consequences. The [material resolver, geometry snapshot, inspector and pinned-Sable static-property proof](civilization-mod/physics.md) are implemented with existing balance preserved. Next prove dynamic per-position mass hooks and domain-aware thermal state on a moving vessel; inventory/tank mass remains excluded until explicit contents policies exist. No cargo/machine penalties are enabled by this foundation.

### Bulk freight — planned implementation package

The [reusable model authoring toolset](tools/modeling/README.md) now supports the complete art workflow. The Hot-Bulb Engine was rebuilt through mockup refinement, native Blockbench components, a shared atlas and whole-wheel GeckoLib animation; its [asset record](art/assets/oil_engine/asset.json) owns source and review evidence. The canister and cargo-store families use the same pipeline. Cylinder/flywheel placement aligns to the controller and held components preserve guide orientation. The freeform vessel foundation and engine-to-boat integration are implemented; powered dock transfer is next.

**Status: land cargo stores, freeform vessels and physical engine propulsion implemented; powered docking remains planned.** The vessel has no preset hull or cabin. Players build any connected form around the Helm and choose how many Hot-Bulb Engines its physical mass requires. [Agreed scope](design_direction.md#bulk-freight-structures), [remaining dock rules](open_decisions.md#bulk-freight-implementation-details), [implemented vessel reference](civilization-mod/vehicle-physics.md).

| Step | Deliverable | Completion evidence |
| --- | --- | --- |
| 1. Freeform vessel boundary — implemented | Connected scan around one Helm, 4,096-block safety cap, supported block/entity policy and transformed interaction coordinates | No shape envelope; terrain/dock capture and unsupported machinery refused; engine/store interaction follows the moving vessel |
| 2. Land storage foundation — implemented | [Coal bunker and cargo tank](civilization-mod/industry.md#bulk-freight-storage), shared authoritative stock/adapters, guides, cabinet screens, manual and local hopper/pipe access | Correct capacity and partial acceptance, no mixing/duplication, full/empty stops, permission revocation, rotated/broken/unloaded structures and save/load retain exact stock |
| 3. Generic Hot-Bulb Engine — vessel integration implemented | [Controller, parts, startup/test operation and shared work accounting](civilization-mod/industry.md#oil-engine); each complete running onboard engine supplies additive thrust | Work/fuel/lubrication conservation, no neutral work charge, shortage/startup/save behavior and no physics-substep double charging |
| 4. Freeform mass-scaled boat — implemented | Existing Helm controls and persistent throttle drive any connected hull; Sable block mass reduces acceleration, speed and steering response | One or several physical engines propel the vessel; cabins, engines, freeform extensions and service fluids survive graceful reload |
| 5. Ship cargo and docking — vessel mounting implemented, docking pending | Bulk stores are allowed aboard; add stationary mooring and explicit port-to-port loading with an engine-driven loading consumer on land | Physical contents arrive intact from shore to boat to shore; disconnect/movement, full target, lost access, chunk unload and competing transfers cannot create or silently erase stock |
| 6. Unified art and feedback | Selected concept converted to buildable cut-block geometry; shared casing/master texture family; engine flywheel/exhaust/audio, coal pile stages and tank sight strip | Reference-driven 32×32 export; readable material/amount, no floating supports or mismatched faces; one focused Photon/Faithful freight scene |
| 7. Integrated balance and release | One extraction → loaded voyage → unloading loop; update recipes/controls/limits and install verified build | Useful freight advantage over pocket/chest carrying, navigable representative river and credible fuel cost; coherent release docs, no promise of untested 200-player load |

Steps 2–5 may land in meaningful implementation batches without requiring the user to test between every machine. Do not retrofit every existing industrial machine to external engines in this package; retain coal-fired oil bootstrap and existing refinery behavior. Add no universal electric grid, mechanical shaft simulation, boilers, new refinery fuel grades, automatic trade, crew framework or nuclear machinery here.

Engineering reuse: extend existing machine guides, cabinet controls, fluid identities/ports, claim checks and BoatEngine work accounting. Keep one controller as owner of cargo/engine state and a small explicit adapter for each consumer. Standard NeoForge item/fluid capability access must not expose more stock than the shared store or bypass transfer bounds. Avoid rewriting all machine processing to enable two stores.

Verification follows [testing](civilization-mod/testing.md): targeted new conservation/lifecycle cases in the fast server suite, one focused hidden freight scene for placement/transforms/UI, and a graceful save/restart check. Test a real peer for docking/access if feasible before claiming multiplayer readiness. No long simulated voyages or broad repeated graphical tours. Document abrupt-crash limitations separately from graceful persistence. Update implementation references and status only as each behavior actually ships.

### Airships — active planning package

The first [controller flight prototype](civilization-mod/vehicle-physics.md#airship-prototype) is implemented: freeform bounded assembly, typed numeric power, powered hover/steering/climb (including wind-compensated resting-position hold), coarse directional drag, gentle test wind and an unloaded-terrain boundary without a configured speed cap. Existing controller art is reused. [Agreed scope](design_direction.md#controller-built-antigravity-airships) replaces prefab airships; [mechanics and remaining decisions](open_decisions.md#controller-built-airships--proposed-implementation-plan) own the detailed design. The initial stage-1 flight and persistence proof is implemented with explicit limits; landing/contact behavior still needs broader coverage; stages 2 and 4 remain future work, and stages 3/5 have only initial approximations. Existing freight work remains incomplete.

| Stage | Deliverable | Focused acceptance |
| --- | --- | --- |
| 1. Controller flight proof | Bounded arbitrary hull assembly, custody, Sable force/torque control and development power source | Assemble, hover, steer, land and save/reload exact blocks, cargo and identity without terrain capture |
| 2. Dynamic mass and control | Verify per-position material/cargo mass, center of mass/inertia and finite stabilization | Greater mass reduces acceleration at equal force; shifted loads alter turning; load/edit/reload invalidate properties correctly |
| 3. Aerodynamics and wind | Cached exterior geometry, directional drag, regional wind and finite-power propulsion | Compare equal-mass slim/broad hulls, empty/loaded hull, head/tail/crosswind and powered hover; expected force/energy trends without oscillation |
| 4. Survival nuclear loop | Small hand-recoverable uranium ore, Geiger survey, accessory slots and visual radiation cues implemented; resolve processing, fuel packaging, power limits and recovery | Explorer find → oil-era processing → flight → return/refuel; exact finite consumption, no substep double charge, visible lift margin and verified shortage/restart behavior |
| 5. Endgame travel and presentation | High-speed terrain/collision policy, refined controls and controller art | Real-peer access/reconnect, walking aboard, safe chunk crossings, braking, bounded performance and focused Photon review |

Use targeted physics/conservation/lifecycle checks and one focused flight scene, not long voyages or broad repeated tests. Controller art follows the existing art skill when implementation begins; no custom vessel model is needed. Update shipped references/status only when behavior or validation changes.

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
| L06 | The [Oil Derrick](civilization-mod/industry.md#build-and-supply-machines) has a tall guide-built timber frame, two walkable galleries, a small animated crosshead and a ground-level crude port, exported from the [isometric block plan](tools/modeling/README.md#isometric-block-plans). Every guided multiblock accepts held structural stacks and shows remaining material counts in its hint. Large-guide rendering now culls off-screen ghosts and bounds hard outlines; next overhaul the Coal Drill, review derrick construction cost and load, then resolve [machine-centered resource-site sieges](open_decisions.md#extraction-installation-sieges) |
| L05 | Public access staged by operational and performance evidence |

Keep the original distinction: a successful functional test does not prove an economy, and a small economy test does not prove 200-player capacity.

## 4. Common implementation contracts

Apply these once across features, not as a repeated warning in every design document:

- **Conservation:** actual stock, fuel and reserved payments cannot be spent twice; transformations explicitly consume inputs.
- **Authority:** the server resolves owner/whitelist access at action time, including automation and already-open menus.
- **Persistence:** claims, inventories, reservations and deadlines commit consistently and survive retries/restart. Diagnostic JSONL is not their source of truth.
- **Bounded work:** region inspection, map listings and claims do not force-load the world. Define active/unloaded/offline behavior per consumer. Do not make every object share an unsuitable universal clock.
- **Migration:** future live-world changes need an explicit migration policy. For the 0.10.0 development stack reduction, the user explicitly waived preservation of over-limit items; no migration was built.
- **Feedback:** show the useful requirement, state and failure reason; keep implementation identifiers out of ordinary play.

A ready package has a concrete interaction, agreed rules, data ownership, lifecycle, bounded work and observable acceptance cases. Prices and rates can stay tunable; unresolved ownership or conservation cannot.

## 5. Scope discipline

Airship work is active under the package above; the controller flight proof is implemented and next needs dynamic mass, calibrated aerodynamics and survival rules. Keep unrelated advanced industry, global power networks, elaborate contracts, NPC labor, detailed ecology/chemistry and universal wear outside that package.

The preferred vehicle direction is custom Civilization gameplay on Sable, the independent physics backend used by Create Aeronautics, without requiring Create gameplay. The [current dependency research](open_decisions.md#vehicle-physics-dependency-research--2026-09-10) identifies standalone/version support, a license clarification, and the proposed integration test. The [optional vehicle laboratory](civilization-mod/vehicle-physics.md) now tests an initial Sable-only dependency set, controllable platform and cargo persistence. Production compatibility and 200-player capacity remain unverified; the airship package must prove dynamic mass, flight forces and safe travel before survival release.

Advance by whole player loops, not a new system for every noun. Maintain design decisions in one place; mark implementation complete only with evidence.
