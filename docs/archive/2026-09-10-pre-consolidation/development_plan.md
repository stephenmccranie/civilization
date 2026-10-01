# Civilization — development plan and checklist

Updated September 10, 2026. Baseline: **0.9.1-dev**, Minecraft 1.21.1 / NeoForge 21.1.250 / JDK 21.

This is the current work plan. The original design supplies the philosophy; `design_direction.md` records the design commitments; the mod README and implementation notes describe shipped behavior. Future work below is proposed unless explicitly marked confirmed. This planning revision does not authorize or implement a new gameplay feature.

**Current priority: whole-mod planning before coding resumes.** Use [systems_blueprint.md](systems_blueprint.md) as the system coverage map and planning sequence. Work through low-dependency decisions and connections before specializing in industry or land allocation. The implementation checklist below remains a backlog; its section order is not the current discussion order.

Pass A draft: [everyday_play.md](everyday_play.md). Review its simple baseline, then resolve death and travel together, followed by cooking and resource boundaries. Drafting the sheet does not approve its proposed changes.

Pass A progress: stack/storage constraints, ordinary death recovery, household cooking direction and the renewable/finite resource boundary are now agreed. Exact implementation and the vanilla audit remain open, as do passenger travel, dimensions and world lifespan. Proceed with the illustrative region in Pass B while tracking these dependencies; do not treat Pass A as fully complete.

## 1. What we are building

**A civilization server where control of useful production, scarce resources, routes and institutions creates power, and power becomes visible as status.**

The original chain remains the organizing principle:

**Energy → surplus → capability → dependency → power → status.**

We have implemented the beginnings of energy, surplus and capability. We have not yet implemented the property, geography and exchange systems that turn them into durable relationships between players. Another faster building tool would expand capability without addressing that gap. The construction machine is therefore **deferred, not the next committed feature**.

Target: up to 200 concurrent players, maintained by one human with assistant support. This is a capacity target, not a demonstrated result.

### Confirmed constraints

- Organize energy, food, materials and productive capability into approximately three tiers, with calories at the base. Higher-tier resources come from large, rarer deposits requiring extraction infrastructure. The agreed industrial ladder is **Tier 1 coal → Tier 2 oil → Tier 3 uranium**, with calories supporting all three. Current Mineral Coal and its machines belong to Tier 1. See [tier_framework.md](tier_framework.md); detailed recipes, extraction mechanics and other material assignments remain proposals.

- Hierarchy, exclusion, concentration of power and status competition are intended possibilities. No prestige score, assigned social caste, or guaranteed equal outcome.
- Ordinary life should be viable without pursuing elite power. Farms, mines, towns and industry should be places worth inhabiting.
- Renewable food supports manual labor. Industrial power comes from finite resources; biomass and renewable generators do not supply it.
- Trade rivalry and politics are primary conflict; PvP is optional and organized. Unrestricted destructive raiding is not the baseline.
- Land allocation should work through a market, with generous leeway, favoring those who can make the best (most profitable) use of a site. This is an acquisition/retention mechanism, not just a trade interface. No formal jurisdictions. Define tenure, economic pressure and transfer treatment in [property_and_authority.md](property_and_authority.md) before implementing claims; wider exchange features remain proposals.
- Progression increases the scale of useful action: hand tools → machines → facilities → networks → regional instruments.
- The sky is an aspirational apex, supported by the world below. Its precise advantages and operating rules remain to be designed.
- Most gameplay is custom-developed. Physics/airships come later; external technical foundations may be used where justified.
- Machines are physical multiblock builds. Craftable controllers list materials; contextual world outlines and the top-left HUD explain construction. No separate layer-guide screen or hide key.
- Take coherent, substantial development steps. Automated verification continues; a manual playtest is not a prerequisite for every machine or feature.

## 2. Three systems, not a simulation of everything

| System | What belongs in it | What it must make possible |
| --- | --- | --- |
| **Production and energy** | Calories, food, finite extraction, recipes, multiblock industry, operating inputs | Produce useful surplus; shortages have concrete effects |
| **Ownership, organization and physical trade** | Land/facility control, shared permissions, local exchange, storage and delivery | Decide who can use an asset; cooperate, bargain, exclude and compete |
| **Infrastructure and visible scale** | Better transport, large facilities, regional services, impressive ground and sky architecture | Use surplus to extend reach and create things other players care about |

World geography supports all three. Persistence, auditing, performance and server operations support all three. They do not need to become additional player-facing currencies or systems.

For each proposed feature, identify its user, inputs, useful output, who can control it, who might depend on it, and its recurring maintenance burden. Add machines when they serve a distinct purpose, rather than to fill out a conventional technology tree.

## 3. Completed foundation

Checked means implemented at the stated scope, not final balance or MMO validation.

- [x] **F01 — Development loop:** Java/NeoForge project, Prism deployment, versioned builds and automated tests.
- [x] **F02 — Calories:** replace hunger/saturation/exhaustion with a persistent reserve and Minecraft-style HUD; food tooltips show kcal.
- [x] **F03 — Work costs:** movement, sprinting, jumping, attacks, mining, placement and natural healing have explicit costs. Sprinting costs more per distance. This is not yet a complete audit of every vanilla physical action.
- [x] **F04 — Recovery:** depletion penalties and manual foraging permit recovery without a permanent action lock.
- [x] **F05 — Farming:** subsistence wheat, one-use fertilizer yield bonus, reliable replanting, bread and packed field rations.
- [x] **F06 — Finite custom fuel:** Mineral Coal from coal ore; one-way conversion to ordinary coal; custom industry rejects renewable fuels.
- [x] **F07 — Brick production:** kiln batches; remove vanilla brick smelting and villager brick sales. Existing stock and finite salvage remain.
- [x] **F08 — Fertilizer production:** Raw Mineral Blend → Fertilizer Works → usable fertilizer, fueled by Mineral Coal.
- [x] **F09 — Physical machines:** stone kiln shell; brick/copper Fertilizer Works; validate shape, chamber, oxidation and optional hopper hatch.
- [x] **F10 — Building UX:** material counts, contextual 3D guides, missing/wrong-block hints; removed Build button and G toggle.
- [x] **F11 — Machine lifecycle:** inventories, hoppers, partial progress, save/reload, blocked output and structure-break behavior.
- [x] **F12 — Audit foundation:** player calorie/production records and machine fuel/batch records with bounded asynchronous logging.
- [x] **F13 — Functional verification:** 36 Minecraft GameTests passed on the multiblock foundation; 19 unit tests passed in the latest build. The 0.9.1 change was client UX cleanup, not another full GameTest run.
- [x] **F14 — Human evidence:** the user has mined, built, farmed, assembled machines and manufactured fertilizer in ordinary play. Multiblock guides were confirmed working; final UX polish remains open to feedback.

### What this does not establish

There is no implemented regional resource economy, claim/organization system, secure exchange, industrial logistics service, sky gameplay or airship integration. A normal dedicated server with multiple connected clients has not been validated. There is no 200-player capacity evidence.

Vanilla fuels and recipes outside our custom chain remain. The universal energy policy is therefore **partly implemented**, not complete. Clay/gravel fertilizer feedstock is a gameplay placeholder, not a nitrogen model. Fuel burn ticks are an operating budget, not an implemented multi-form energy conversion network. Existing machine logs do not establish ownership or track every inventory transfer.

## 4. Recommended next milestone: a first multiplayer regional economy

**Outcome:** a farm, an extraction operation and a workshop can be controlled by different players, exchange actual goods, and have a practical reason to do so. Common ownership and vertical integration remain legitimate; do not force specialization through classes or prohibit successful organizations from expanding.

This is a larger development milestone with three connected work packages. Choose its rules first, then implement the packages without stopping for manual approval between individual features.

**Current planning focus:** the whole-mod design pass in [systems_blueprint.md](systems_blueprint.md), beginning with everyday play and vanilla boundaries, then geography and demand, then property/payment and later industry. [property_and_authority.md](property_and_authority.md) is a candidate specification; its particular land mechanisms are not approved. Oil-chain design is deferred during this broader planning work.

### A. Set the economic and world rules

- [ ] **E00 — Tier matrix first:** specify the three tiers' energy sources, deposit/extraction requirements, material outputs, food roles and desirable capabilities. Trace a non-circular bootstrap path and continuing lower-tier dependencies. Coal → oil → uranium and current Mineral Coal placement in Tier 1 are agreed. Next specify the first coal-powered oil extraction/refining chain, its construction materials and operating costs. Use `tier_matrix.md` and `tier_framework.md` as the working specification.

- [ ] **E01 — Resource map:** identify the small initial set of strategic inputs and where they occur; distinguish common subsistence materials from industrial concentrations. Specify deposit size, extraction behavior, discovery information and exhaustion.
- [ ] **E02 — World lifespan:** select campaign/reset, persistent core plus managed frontier, or a provisioned long-lived finite world. Define what happens when an area runs short. No regenerating fuel deposits disguised as finite fuel.
- [ ] **E03 — Production table:** write one table of actual inputs, output quantities, processing time, fuel budget, capital cost and labor for the first economy. Include vanilla metal processing and cooking, not only our two machines.
- [ ] **E04 — Vanilla boundary:** explicitly allow, replace or disable relevant vanilla fuel, food, villager, mob-farm and resource-conversion paths. Define the household cooking exception, if any. Review both manual and automated bypasses.
- [ ] **E05 — Fertilizer feedstock:** decide whether the current blend stays for this milestone or becomes a more meaningful mineral input. Do not add a chemistry simulation merely to justify the recipe.
- [ ] **E06 — Progression purpose:** specify what the first workshop sells, who wants it, and why establishing another workshop has a meaningful cost. Choose any additional machine from this need; a foundry is a candidate, not a decision.
- [ ] **E07 — Transport boundary:** define allowed early travel and storage, including portals, ender storage, portable containers and respawn. Start with physical delivery and known inventory limits; no global market that teleports goods.

**Definition of done:** a concise rules sheet, an input/output table, and a small resource/settlement layout explain the complete first economy. Rates may remain tunable; reserve policy and allowed energy paths must be explicit. These decisions describe the prototype map before commissioning a large permanent world.

### B. Establish control and exchange

- [ ] **C01 — Ownership model:** choose the land/facility boundary and one persistent organization primitive usable as a company, town or guild. Specify creation, invitations, owner transfer, departure and disbanding.
- [ ] **C02 — Permissions:** distinguish building/breaking, machine use, input deposits, output withdrawals and member management. No automatic rights from a decorative title.
- [ ] **C03 — Protection coverage:** apply rules to the whole multiblock and its inventories, including hopper transfers and relevant environmental damage. Decide how pre-existing unowned machines are adopted without silently assigning them to the wrong player.
- [ ] **C04 — Market system:** specify initial offer types, payment medium, listing visibility and physical fulfillment, then implement a coherent first exchange method. Goods, property sales, rentals/access and delivery orders are candidate offer types, not all committed launch scope. Currency versus barter remains open. Prevent duplication/loss under simultaneous use, disconnects and restart; enforce supported agreements without a moderator contract court.
- [ ] **C05 — Audit attribution:** record owner/organization changes and material transfers. Retain unattended machine events separately from player calorie balances. Keep logs bounded and useful for diagnosis.
- [ ] **C06 — Owner and maintainer operations:** define revocation, abandoned assets, disputed access and administrative recovery so ordinary economic competition does not require a daily moderator court.

**Definition of done:** two real clients can grant/revoke access, run separate facilities, exchange goods and reconnect after a restart without losing property state or duplicating stock. An excluded player cannot take goods through an unguarded automation path.

### C. Put the economy in a useful geography

- [ ] **W01 — Prototype world layout:** place farms, industrial inputs and settlement opportunities so real delivery routes exist. Avoid scaling travel into empty waiting time.
- [ ] **W02 — Extraction implementation:** implement the selected deposits and their accounting; choose compatibility/migration for existing dev terrain. Keep the prototype fuel ledger separate from creative/admin grants.
- [ ] **W03 — Industrial implementation:** finish the agreed production chain, including new multiblock machinery if justified by E03/E06. Reuse shared inventory and building-preview behavior.
- [ ] **W04 — Freight baseline:** support local stockpiles and a basic physical delivery route. Choose the initial transport mode before adding freight machinery; do not assume airships or item teleportation.
- [ ] **W05 — Participation:** make the route from arriving, feeding oneself and finding useful work understandable through game behavior and concise UI. Include farming, mining, hauling, manufacturing and building without assigned professions.
- [ ] **W06 — Economic evidence:** run a bounded scenario with separate producers, a buyer and a carrier. Record inputs, food costs, capital, travel and stock buffers; observe whether buying or hiring is attractive without quest payouts or spawned supplies.

**Definition of done:** there is at least one repeated voluntary supply relationship and an understandable consequence when a supplier stops. This is evidence to gather, not a promise that recipes alone will make players trade. If trade is unattractive, examine self-supply costs, usefulness and geography before adding more tiers.

## 5. Server and maintenance work alongside that milestone

- [ ] **T01 — Version control:** establish a repository and recoverable checkpoints. The project folder currently has no `.git` directory.
- [ ] **T02 — Dedicated development server:** create a separate server directory and disposable multiplayer test world, pin the client/server build, document startup and configuration. Complete owner-controlled hosting/EULA setup as needed.
- [ ] **T03 — Real multiplayer:** verify calorie synchronization, permissions, simultaneous inventory use, join/leave, unloaded borders and restart with multiple clients. GameTests do not replace this.
- [ ] **T04 — Backup and restore:** version world/config/mod backups and successfully restore a copy before public access; define upgrade and rollback procedure.
- [ ] **T05 — Repeatable diagnostics:** add a compact report for player calorie use, machine input/output, fuel utilization where measurable, journal failures and server tick/memory metrics. Do not infer utilization from completed batches alone.
- [ ] **T06 — Operating budget:** choose hosting hardware/budget, maintenance expectations, simulation distance and explicit limits for persistent chunk loading, entities and active machinery.
- [ ] **T07 — Growing-load profiles:** test exploration, clustered factories, farms, inventory traffic, saving and backups together. Set measurable tick-latency/memory targets and then test toward 200 concurrent players.
- [ ] **T08 — Content and version discipline:** keep gameplay decisions, recipes, config defaults, save compatibility and release notes aligned. Recheck external dependencies when actually selecting/installing them.

These tasks proceed alongside gameplay development. Basic multiplayer correctness should precede heavy investment in permission-dependent systems; representative load profiling should precede committing to their largest scale.

## 6. Creative direction as an active workstream

The existing art already suggests a useful contrast: productive farmland and mineral country, dark brick/iron/copper industry, monumental urban institutions, and ivory/turquoise sky architecture. Treat that as a working visual vocabulary, not a finalized lore bible.

- [ ] **V01 — World identity:** write a short setting statement, working world name, tone and vocabulary. Define what makes this civilization distinctive beyond its BioShock influences.
- [ ] **V02 — Four inhabited layers:** specify a believable place and enjoyable daily activity for frontier workers, commercial agriculture, urban institutions and sky residents. Ground-level power and beauty remain possible.
- [ ] **V03 — Architectural language:** establish materials, silhouettes, machine scale, signage and lighting for each layer. Prefer reusable block-built motifs over a large bespoke art workload.
- [ ] **V04 — First prestige destination:** describe one impressive estate or institution and the experience of owning, supplying, visiting or being excluded from it. Choose what makes it desirable before assigning upkeep numbers.
- [ ] **V05 — Visible dependencies:** show how freight, fuel and workers support impressive places. Describe shortage behavior and reserves as part of the experience.
- [ ] **V06 — Naming and UX:** use understandable player-facing machine names and contextual guidance. Keep implementation identifiers out of normal play.

**Deliverable:** a short world/style brief and one grounded-to-sky scene specification. Work on these while the regional economy is built; do not postpone the vibe until all systems are finished.

## 7. Later milestones and dependencies

| Milestone | What it adds | What must be known first |
| --- | --- | --- |
| Infrastructure businesses | Better freight, larger production facilities, route services and paid access | Useful goods, control, transaction safety, transport costs |
| Regional capability | One chosen instrument such as agricultural weather influence | Owner/access rules, bounded region, fuel accounting, overlap and shortage behavior |
| Sky civilization | A desirable place/institution with material supply and operating dependencies | V04/V05, delivery model, permissions, acceptable failure behavior |
| Airships and distant rendering | Physical transport and visual scale using a tested technical foundation | Stable ground economy/server, current dependency investigation, cargo/lift/propulsion budgets and lifecycle tests |
| Broader public server | Sustained economy, organized competition and up to the target population | Recovery operations, clear rules, load evidence and realistic maintenance budget |

- [ ] **L01 — Specify dependencies and implementation order for the documented advanced capabilities.** Monumental construction, regional weather influence, sky freight and elite sky institutions already establish Tier 3's broad purpose. Use `tier_matrix.md` to trace their inputs and operating requirements; do not reopen that purpose or treat construction automation as the next committed feature.
- [ ] **L02 — Define one sky asset's construction, access, operating clock, fuel reserve and shortage behavior.** Expensive lighting alone is not an established reward; automatic falling cities are not approved.
- [ ] **L03 — Investigate and integrate airships later.** Create Aeronautics remains a preferred candidate, not an installed or validated dependency. Validate moving cargo, docking, ownership, chunk boundaries, passengers and restart.
- [ ] **L04 — Define optional organized PvP rules** before combat can affect protected facilities, routes or territory.
- [ ] **L05 — Stage public access by operational and performance evidence.** Small economic tests and capacity tests answer different questions.

## 8. Decisions still open

| Decision | Why it matters | Resolve by |
| --- | --- | --- |
| Live-world fuel lifespan and expansion/reset policy | Finite fuel eventually exhausts accessible reserves | Before permanent world provisioning |
| Primitive cooking vs industrial processing rules | Current vanilla paths conflict with a blanket energy claim | E04 |
| Deposit geography and first strategic inputs | Determines exploration, trade and replacement difficulty | E01/W01 |
| Claim boundaries, transfer and inactivity | Determines enforceable control and maintainer burden | C01/C06 |
| Initial exchange medium | Changes trading UX and the need for monetary infrastructure | C04 |
| First transport mode and storage bypasses | Determines whether location and carriers matter | E07/W04 |
| Actual appeal and fiction of the sky | Determines what the productive base ultimately supports | V04, before L02 |
| Hosting and operational budget | Constrains population and simulation scope | T06 |

The recommended ordering above is a proposal, not a claim these decisions have been made. Do not silently turn an earlier suggestion into a confirmed requirement.

## 9. Explicitly outside the immediate queue

Automatic construction machine; weather-control implementation; sky service cores; physics/airships; global power cables; universal weight simulation; NPC workforce; detailed nitrogen/soil chemistry; spoilage and blanket building decay; banks, debt and contract enforcement; conquest; prestige scores; elaborate hidden dependency-centrality simulation.

Several are valid future directions. They are deferred because the next milestone needs meaningful production relationships and control, rather than every ambition in the original document at once. This is not a requirement to keep all future increments small.

## 10. Keeping this checklist useful

Each implementation step should name the checklist IDs it advances, its player-visible result, dependencies, and appropriate checks. Mark a task done with evidence in release notes or its implementation document. Record changed decisions here and in `design_direction.md`; leave the original philosophy intact. Do not check off a whole system because one recipe works, and do not leave completed work permanently labeled a future prototype.

Read order: this plan → `design_direction.md` → original design for philosophy → `civilization-mod/README.md` and linked implementation details. `first_playable_economy.md` is a historical numerical experiment, not live balance. `modpack_server_design_workshop.md` is an exploratory archive, not the active schedule. Concept images are visual references, not implemented features.
