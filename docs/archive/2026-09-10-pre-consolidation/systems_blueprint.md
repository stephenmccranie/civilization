# Whole-mod systems blueprint

## Current planning approach

Current broad planning focus: [first_hour.md](first_hour.md) traces solo startup through food, materials, a site, claim power and trade. Unclaimed-land gathering/building/facility use without claim protection is now agreed. Spawn, starting items and numerical bootstrap costs remain open; older land-failure work below stays in the backlog rather than automatically becoming the next task.

Latest social-system decision: use solo ownership and small trusted groups with full member access to shared assets. [groups.md](groups.md) records the baseline; no custom permission hierarchy or eight-player cap is implied. Detailed administration remains open. Land-controller edge cases remain tracked but need not monopolize the planning sequence.

Confirmed request: plan the interconnected mod before resuming coding. Work broadly, resolve low-dependency decisions first, and progressively make the systems implementable. Do not get trapped perfecting one machine, land mechanism or endgame feature in isolation.

This is the design index. [design_direction.md](design_direction.md) records commitments; [development_plan.md](development_plan.md) tracks implementation work; individual specifications carry detail. The original philosophy remains in [status_power_civilization_game_design.md](status_power_civilization_game_design.md).

The coverage map below is a planning checklist, not a commitment to implement every candidate. “Open” means undecided. Proposals remain proposals until accepted. Industry and physics implementation remain deferred.

**Confirmed design principle:** we may disable or substantially rework vanilla systems as long as the result is simple and intuitive. Preserve useful interactions, not compatibility for its own sake. No specific removal follows automatically from this latitude.

**Confirmed construction principle:** material conversion should more closely respect physical block volume. One log becomes one plank block, not four. Other recipes need a volume-aware review, including partial blocks and early necessities. This changes demand for timber and freight; it is not implemented yet.

**Confirmed logistics foundation:** standard inventories with item-specific stack limits represent carrying bulk. Heavy/large goods have low stack sizes, and bulk Tier 2 resources should be impractical to move in player inventories. Specify packaging, alternate item forms and freight capacity consistently; exact numbers remain open. See [everyday_play.md](everyday_play.md).

The agreed ordinary stack baseline is now **32 instead of 64**, including building blocks. Lower industrial limits and treatment of existing 16-stack items remain open; the inventory layout stays standard. The stack changes are not implemented yet.

Storage rules are also agreed: more slots provide warehouse/vehicle capacity, vehicle cargo moves physically, carried container items must be empty, filled-container nesting is prohibited, and shared remote ender storage is disabled. Ordinary chests support early warehouses. Ordinary death now leaves all carried items/equipment in an owner-only recovery container near the death location, persistent across logout/restart; respawn preserves calories and possessions require physical recovery. Household cooking uses a simple coal-fired station, while farming and uncooked food support subsistence. These changes are unimplemented. Recovery edge cases, vehicle types, broader travel rules and cooking recipes remain open. Next low-dependency discussion: renewable versus finite resources.

## 1. The experience we are building

Latest Pass A decision: food, timber and biological materials are renewable; metals, industrial fuels and strategic mineral deposits are finite. Common building materials should be plentiful through extraction. Renewable metal/fuel acquisition routes must be removed; automation may process finite stocks but not create unlimited replacements. See [everyday_play.md](everyday_play.md) for qualifications and the pending vanilla audit. Next broad planning step: describe an interconnected region (Pass B), keeping remaining travel and world-lifespan choices explicit.

**Energy → surplus → capability → dependency → power → status.**

Players start with personal labor and expand the amount of matter and territory they can influence. Food supports people; finite coal, oil and uranium support increasing industrial scale. Useful surplus supports trade, infrastructure and extraordinary places. Control of valuable assets and relationships supports hierarchy.

The world connects frontier and agriculture to industrial cities and elite sky institutions. These are overlapping ways to live, not mandatory player classes. Ground-level life needs its own appeal. The apex must be impressive enough to motivate production, organization and competition.

No renewable industrial energy or biomass fuel. No formal jurisdictions. Land allocation should face market pressure toward valuable use with generous leeway; a particular lease, auction or valuation scheme is not approved. Trade and politics are primary conflict, with optional organized PvP. Target up to 200 concurrent players with one human maintainer and assistant support.

## 2. Coverage map

| Area | Player-facing purpose | Already established or present | Next planning deliverable | Main connections |
| --- | --- | --- | --- | --- |
| Body, survival and recovery | Work, eat, recover and continue playing | Calorie reserve, action costs, depletion recovery implemented | Death/respawn, healing, travel and remaining action rules | Food demand, labor, transport, combat |
| Food and agriculture | Feed oneself or provision other people | Basic wheat/fertilizer loop and rations implemented; food tiers intended | Compact food roles and crop/animal/automation boundary | Calories, fertilizer, land, freight |
| Ordinary materials and crafting | Build homes, tools and first facilities | Vanilla base plus kiln and fertilizer chain | Keep/change/remove list for vanilla crafting and resource paths | Bootstrap, construction, finite-resource policy |
| Geography and exploration | Discover useful sites and choose where to live | Rare large higher-tier deposits require infrastructure | One illustrative region, discovery rules and travel targets | Land value, supply routes, settlement, depletion |
| Land and buildings | Invest in a place and retain useful control | Market allocation with generous leeway; no jurisdictions | Acquisition/retention alternatives tested against common scenarios | Currency, construction investment, access, abandonment |
| Organizations and access | Combine assets and delegate work | Hierarchy intended; concrete organization model proposed | Small rights table and asset/leadership lifecycle | Land, storage, machinery, trade, succession |
| Exchange and payment | Find buyers, obtain supplies and value land | Physical trade intended; currency and interface open | Payment creation/circulation rules and first exchange method | Land bids, food sales, freight, asset transfer |
| Storage and logistics | Accumulate stock and move real goods | Inventories/hoppers present; airships later | Early carrying, warehouses, routes and vanilla bypass rules | Trade geography, machine interfaces, custody, energy |
| Production and power | Turn limited inputs into larger capabilities | Coal → oil → uranium accepted; two coal machines implemented | Common input/output and energy rules; detailed machines later | Resources, logistics, food, permissions, scale |
| Construction and infrastructure | Build larger settlements and useful networks | Physical multiblocks and guides implemented; monumental construction intended | Building progression and supplied-work job behavior | Materials, labor, property, power, prestige |
| Regional instruments | Influence productive conditions over a region | Weather control is an established ambition | Bounded service specification: effect, access, range, duration, overlap, shortage | Agriculture, energy, ownership, customer value |
| Sky places and mobility | Inhabit and operate the aspirational apex | Sky institutions/freight intended; physics deferred | One destination's lived experience and supply diagram | Architecture, services, access, airships, industrial scale |
| Conflict and loss | Compete with understandable stakes | Economic rivalry; optional organized PvP | Protection rules, consensual stakes and death/cargo outcomes | Property, organizations, transport, recovery |
| Continuity and discovery UI | Make the world understandable and durable | Contextual machine guidance and bounded audit logs exist | New-arrival path, owner absence, shortages and world-lifespan rules | Every persistent asset, finite reserves, onboarding |
| Server operations | Keep the world reliable and maintainable | Dev client/build/tests exist; target capacity unproven | Persistence, transactions, backup/restore and performance budgets | All systems; especially moving craft, inventories and world generation |

These are aspects to cover, not fifteen isolated subsystems to build. Prefer shared machinery beneath them.

## 3. Proposed common building blocks

- **Resources and quantities:** food, materials, fuel and actual stored inventory. Value and location matter; avoid a separate token for each mechanic.
- **Places and rights:** bounded locations, owners, organizations and scoped permissions. Reuse for fields, homes, factories and terminals.
- **Work and services:** a job has inputs, a rate, energy use, outputs and an interruption state. Reuse where practical for processing, construction and regional services.
- **Transfers and obligations:** move stock, payment, title or time-limited rights with explicit completion and failure behavior. Use only agreement types the server can evaluate.

This is an architectural direction, not a requirement to build a universal framework first. Extract common behavior when concrete use cases justify it. Every system must state what happens offline and while its chunks are unloaded; ownership or a listing should not force chunks to stay loaded.

## 4. Planning sequence: easy decisions before dependent mechanisms

### Pass A — Everyday play and the vanilla boundary

Start with existing behavior and inexpensive decisions that affect many systems. Deliver one concise rules sheet answering:

- What can a new player accomplish alone during a normal session?
- What changes on death, logout and return? Which current calorie/recovery rules remain?
- Which ordinary food, cooking, tools, blocks and crafting paths remain available?
- How do inventory, portable storage, portals, teleportation and respawn interact with physical freight?
- Which mob, villager and automated production paths would bypass energy or scarcity?

Evaluate each vanilla system by its role in the intended game. Keep it, replace it with a coherent simpler rule, or remove it if unnecessary; retaining vanilla behavior is not the default requirement. Retain the working calorie/recovery baseline while evaluating connections. Particular replacements and removals remain proposals until settled.

### Pass B — Places, demand and the world's appeal

The shared [region system draft](region_system.md) now connects growth, natural spawning and deposit generation. Bone meal is globally disabled in the planned rules. Trees grow more slowly outside woodland; kept livestock has no additional regional gate beyond feed and ordinary local requirements. These directions are agreed but unimplemented.

The first region/activity matrix is drafted, with a wooded river valley at the foot of mineral uplands as an overlap example. Exact forestry rates and natural-water fishing eligibility remain open. Next specify concise land-inspection feedback while keeping prospecting distinct.

Confirmed direction: terrain-legible regional specialization, including river-dependent farming. The first [regional geography draft](regional_geography.md) separates this rule from proposed resource landscapes, river detection and crop coverage. Define the sample region around visible productive geography.

Describe a small region with a homestead, commercial farm, resource site, workshop, town and route to an aspirational destination. Give each place an activity worth doing, something it needs and something others value. Draw the physical supply relationships without assigning exact prices.

Develop the look and daily experience alongside the economics: materials, silhouettes, sounds, public spaces, private spaces and visible freight. Specify the sky destination now as an experience; implement its physics later.

This establishes why land and services have value before selecting an allocation mechanism. It does not require completing the oil chain.

### Pass C — Property, payment and cooperation

Use those places to choose land allocation, payment medium, ownership boundaries and a minimal organization/permission model together. A land bid mechanism depends on how payment enters and leaves the economy; do not design either in isolation.

Test a remote homestead, developing factory, desirable estate and scarce resource site. Include a richer bidder, an absent owner, a tenant and a completed building. Market pressure and generous leeway are confirmed; renewable competitive leases are only one candidate.

Define the first exchange and delivery interactions. Avoid broad employment, banking or arbitrary legal contract simulation unless an essential play loop demands them.

### Pass D — Scale and productive services

Return to the tier matrix after the social and spatial requirements are clearer. Trace resource → extraction → processing → transport → customer. Specify construction, weather and sky operations through the same questions about inputs, permissions, service and interruption.

Choose a small material vocabulary and useful capabilities per stage. Detailed industrial recipes and physics investigations belong here, not at the front of the current planning queue.

### Pass E — Whole-world lifecycle and implementation packages

World lifespan is a cross-cutting question to track from the start; resolve it before fixing reserve quantities or provisioning a permanent world. Also finalize owner absence, abandonment, organization succession, failed deliveries, PvP stakes and protected assets.

Walk through complete sessions and longer-term changes across the whole map. Convert coherent groups of decisions into implementation packages with acceptance checks. Continue basic server readiness work when coding resumes; heavy gameplay investment depends on multiplayer correctness.

These passes are revisitable. If a choice depends on an unresolved system, name the dependency and move to another useful decision instead of inventing a rule to close the page.

## 5. When a feature is ready to code

A short specification should answer:

1. **Experience:** who uses it, why, and one concrete interaction.
2. **Rules:** inputs, outputs, permissions, limits and visible feedback.
3. **Connections:** resources or other systems it consumes and exposes.
4. **Lifecycle:** success, interruption, loss, offline/unloaded behavior and restart.
5. **Cost:** tunable defaults, bounded server work and likely maintenance burden.
6. **Acceptance:** observable examples demonstrating correct behavior and the important bypasses to test.

Separate decisions from tuning: we can choose that freight requires physical pickup while leaving route lengths adjustable. A feature is not ready if its unresolved questions change ownership, conservation of resources or the fundamental player interaction. No need to design every recipe before coding a settled package.

## 6. Working checklist

- [x] Create whole-mod coverage map and identify shared building blocks as proposals.
- [x] A: Create the first [everyday play and vanilla-boundary draft](everyday_play.md), with current behavior separated from proposals.
- [ ] A: Resolve baseline rules, death/travel, cooking and resource boundaries; complete the keep/change/defer table.
- [ ] B: Describe one interconnected region and its ground-to-sky appeal.
- [ ] C: Resolve land, payment, ownership and organization rules against that region.
- [ ] D: Specify progression and regional services, then resume detailed industry design.
- [ ] E: Reconcile world lifecycle, failure cases and server limits across the design.
- [ ] Produce ordered implementation packages from settled specifications.

**Current planning step: land-controller failure handling.** Zero energy ends ordinary claim protection immediately, with no grace period. A funded takeover otherwise fixes the price, locks the controller reserve and provides one week to relocate before handover. Resolve exhaustion during that funded interval and subsequent refueling/reclaim behavior; monthly scheduling and numerical costs remain open. See [land_controller.md](land_controller.md). No land-allocation code is implemented.
