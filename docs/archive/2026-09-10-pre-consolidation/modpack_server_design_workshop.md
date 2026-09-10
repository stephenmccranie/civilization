# Minecraft civilization server: design workshop

Archived exploratory proposal, September 10, 2026. Current work order and checklist: [development_plan.md](development_plan.md). Earlier mod choices, low progression ceiling, and next-experiment recommendations are not current commitments. This translates `status_power_civilization_game_design.md` into a small playable design. Recommendations below are hypotheses for discussion and prototyping, not settled requirements.

**Revision note:** Read `design_direction.md` first. The user has clarified that emergent hierarchy and status are central, with an awe-inspiring elite sky layer and a proposed nonrenewable energy economy. That brief governs the next prototype; the four settings below remain earlier creative alternatives.

## The experience to build

**Build a place other people have a reason to visit, buy from, work with, or compete against.**

The original document gives us a strong philosophy: capability creates dependency, and dependency creates power. We should implement the conditions that produce that relationship rather than build a simulation that measures it.

The desired story is: “Our town built the reliable freight service. The northern foundry became our largest customer. Now they are funding a rival route.” It should be understandable by looking at buildings, shipments, and people.

An ordinary player must also have a good evening without running an institution. Building a cottage, selling a load of timber, decorating a shop, exploring a pass, or helping finish a station must all belong in the game. Universal political ambition would make a poor population model.

Confirmed direction: **up to 200 simultaneous players**, with **trade rivalry and politics plus optional organized PvP**. Protected settlements and commercial rivalry are therefore the baseline. No capacity claim is made here; 200 concurrent players is an engineering acceptance target, not something established by selecting mods.

## Three core systems

### 1. Regional production: everyone can live, specialists can prosper

Use three recognizable economic regions in one compact, connected world. Basic food, shelter, tools, and building materials remain accessible to everyone. Regional advantages apply to a small set of industrial inputs and desirable building goods.

Prototype candidates:

- Lowlands: a regional plant produces resin for industrial recipes and distinctive finishes.
- Highlands: a regional deposit supplies an alloy ingredient and distinctive masonry.
- Coast or basin: a regional deposit supplies a mineral used in processing and decorative glass.

These are recipe-design placeholders, not claims about existing mods. Prefer fixed deposits and simple recipes over a new climate simulation. If plants are used, portable seeds must not erase the regional distinction.

Start with only a handful of traded goods and two or three processing steps per chain. A useful test recipe might combine a local metal, imported resin, and imported mineral into a batch of industrial components. Those components support expansion and selected production processes. Do not make every household object require imports.

Resource distribution and substitution determine how much leverage owners possess. Do not guarantee alternative suppliers or fallback recipes solely to prevent monopoly power. Prototype the resulting bargaining relationships before choosing deposit density.

The current energy direction is no renewable sources. Food, biomass, finite reserves, and world lifespan require explicit treatment; see `design_direction.md`. The earlier recommendation for renewable industrial feedstocks is withdrawn.

**Why this creates power:** a well-run producer saves other people time and makes ambitious projects possible.

**Main failure:** one large group produces everything internally. Geography alone will not prevent that. Test whether establishing and operating a distant outpost is enough effort that buying is attractive. Do not respond by adding arbitrary class locks immediately.

### 2. Physical freight: routes are a player business

Goods are purchased or exchanged at their physical location. No global market that delivers items instantly. Start with familiar inventory limits; do not build a universal weight system.

Choose one primary freight mode for the setting: boats, roads, or rail. Walking with a shipment must remain a viable early alternative. Bulk transport should earn its place through convenience and throughput.

Set provisional map distances so a basic commercial trip takes roughly 5–10 minutes, then test whether that feels satisfying. Route construction should visibly improve the journey. Do not make people spend an evening crossing empty terrain.

For the prototype, omit routine player teleportation. Before launch, review every bypass: homes, spawn travel, death and graves, ender storage, portable storage, flight, portals, remote inventories, and modded item transport. Either accept its economic effect or configure it consistently. A custom “empty inventory teleport” rule is additional code, especially around nested inventories, so it is not a free convenience feature.

Do not enforce bridge tolls through moderator rulings. A transport operator can charge for delivery or access to its own service. If anyone can build a competing route, that is healthy competition. Do not promise physical chokepoints unless terrain and protection rules actually make them work in Minecraft.

**Why this creates power:** reliable carriers and useful junctions connect producers to customers.

**Main failure:** moving goods is tedious, or inventories make specialized transport unnecessary. Test both before adding freight machinery or more distance.

### 3. Player towns: secure places to invest and cooperate

Use one existing claim/team system for permissions and shared property. The same organization primitive represents a town, guild, or company; titles and constitutions can live in player-written books.

Prototype private stalls with an existing exchange mechanism or simple barter. Automated local shops are a candidate after compatibility and transaction safety are verified. Do not create a custom banking or contract engine.

Protected homes and workshops make long-term construction plausible. Start with generous but bounded claim allowances, a starter area, and a clear route for newcomers to settle independently. Claim count is not an adequate proxy for server load; simulation budgets are separate.

Preserve the original idea of maintained power through **operating inputs**, not universal decay. Selected advanced output consumes materials when it is produced. Empty inputs stop production; they do not destroy the factory or remove home protection. Only one such sink needs to exist in the first experiment.

Do not simulate offline production in custom code. Any production while players are absent must use the selected platform's explicitly budgeted chunk-loading behavior. Test what happens to routes and machines when chunks unload.

Large monuments, public stations, attractive markets, supplier relationships, and player histories provide recognition. Start provenance with signed books, named items, and plaques. A complete ownership ledger for every object is unnecessary.

**Why this creates power:** players coordinate productive assets and build places others choose to use.

**Operational concern:** vacant organizations and unclear ownership can require maintainer intervention. Define permissions and inactivity rules explicitly. Exclusion by active owners is not itself a design failure; control and hierarchy are intended outcomes.

## What to retain, simplify, or remove

| Original ambition | Practical first version |
| --- | --- |
| Energy, matter, information, organization | Recipes, physical goods, player knowledge, teams |
| Complex production economy | A few regional inputs with short processing chains |
| Stocks versus flows | Reliable production and operating inputs; no separate metric |
| Universal maintenance and decay | Consumption during selected industrial production |
| Dependency graphs and centrality | Observe actual trade and player behavior |
| Banks, loans, insurance, leases | Player agreements; no administrative enforcement service |
| Simulated workers and population needs | Actual players; ordinary survival |
| Multiple forms of government | One permission system and player-defined institutions |
| Historical provenance | Books, named objects, buildings, and recorded milestones |
| Logistics-based warfare | Defer; trade must work before conquest is layered onto it |
| Endless progression and technology eras | A deliberately low ceiling with horizontal expansion |

Food alone is a weak candidate for anchoring the whole economy: test surplus and storage behavior instead of assuming hunger creates durable trade. Similarly, repetitive upkeep can become a login obligation. Industrial demand should come from useful activity and projects, not punishment for taking a holiday.

## Four creative directions

These are alternatives using the same systems. Each should change the map, daily activity, architecture, and the meaning of success—not just the textures.

### A. The Estuary Republics

**Image:** foggy harbors, tiled counting houses, painted warehouses, canal locks, lantern-lit market streets, distant foundry smoke.

Several independent towns grow around one inland sea and its river mouths. The geography makes destinations legible and boats useful. Players become carriers, mill owners, builders, merchants, and harbor organizers.

The characteristic story is two rival ports competing to serve the same hinterland. Wealth appears in busy quays, civic buildings, ornamental bridges, and warehouses with recognizable merchant marks.

Keep the tone warm, industrious, and slightly strange: a mercantile fairy tale with practical machinery. Avoid generic medieval kingdoms and an unrestricted march toward futuristic technology.

**Maintenance fit:** strongest initial candidate, because ordinary boats and static terrain can express the setting. Start without simulated tides, water levels, or complex moving ships. Rail can be evaluated later.

### B. The Last Railway

**Image:** amber station windows in dark pine forests, rust-red freight cars, snow sheds, brass signs, repair yards, enormous abandoned viaducts.

Players restore connections between isolated valleys after an unexplained collapse. Stations are civic centers. An engineer who reliably connects three towns becomes a public figure.

The characteristic story is a cooperative reopening a mountain route while another company builds a cheaper line. Old-world relics furnish museums, council chambers, and railway hotels.

**Maintenance fit:** compelling if the pack has one proven rail solution. Vehicle behavior, unattended routes, chunk loading, and performance are early technical gates. No simulated NPC passenger economy or procedural collapse events.

### C. The Lantern Marches

**Image:** immense trees, dark marshes, pale ruins, green glass, communal kilns, yellow lanterns, walled gardens around luminous workshops.

A gentle industrial civilization settles the edge of an uncanny wilderness. Regional alchemical materials replace generic ore tiers. Carriers connect workshops, gardens, and remote gathering settlements.

The characteristic story is a town becoming famous for the glass that lights every settlement along a road. Power has a ritual and communal aesthetic, even though the recipes remain simple.

**Maintenance fit:** mostly feasible through art direction, recipes, and restrained world generation. An expanding protective light field sounds thematic but creates a new claim/safety system; omit it initially. No spell research tree or custom invading AI.

### D. The Salt Road

**Image:** white salt flats, ochre caravanserais, turquoise courtyards, indigo banners, cliff towns, shaded exchanges, abandoned survey towers.

Settlements occupy a chain of oases and mineral basins. Trade routes connect several sources of industrial minerals and agricultural products. Basic water access stays available; “own the only well” is not the onboarding experience.

The characteristic story is a small caravan town outcompeting an established exchange through better service and a new route. Success looks like a thriving courtyard market or a monumental public cistern.

**Maintenance fit:** good if expressed through static geography and familiar transport. Simulated thirst, moving dunes, and dynamic desertification would add work without proving the core loop.

**Earlier preference, now superseded:** the Estuary Republics. The current creative direction emphasizes the vertical relationship between extraction, productive countryside, urban power, and elite sky environments. Harbor and railway motifs may still support it.

## Implementation boundaries

Candidate capabilities checked against official project documentation on September 10, 2026:

- [Create](https://github.com/Creators-of-Create/Create) provides building, decoration, and visible automation. Evaluate it as the single machinery backbone if the theme needs one.
- [FTB Chunks](https://docs.feed-the-beast.com/mod-docs/mods/suite/Chunks/) provides claims and chunk loading. Protection and permission interactions still require testing with the chosen machines and inventories.
- [KubeJS](https://kubejs.com/) supports items, recipes, and scripting. Prefer data and small recipe changes over a large custom gameplay framework.
- [spark](https://spark.lucko.me/docs) provides documentation for profiling and server performance investigation. Use measurements to set limits.

These are candidates, not a compatible modpack manifest. Choose the Minecraft version and loader only after checking the full set of required releases together. Do not assume a server-plugin/mod hybrid is necessary or supported.

Set a custom-code budget: regional inputs, a few recipes, and onboarding first. Any new persistent state machine needs a demonstrated gameplay need. Avoid per-tick scans, global dependency calculations, and individual wear tracking.

At the confirmed 200-concurrent target, use static infrastructure, ordinary boats, and simple processing as the baseline. Test machinery and rail in a separate candidate build before making them essential to the design. Budget active chunks, machines, entities, and unattended production explicitly; do not grant unlimited simulation simply because someone has more claims or accounts. Pre-generate the bounded playable world and test both crowded hubs and geographically distributed activity. No amount of attractive design substitutes for this capacity test.

Optional PvP should initially use a designated arena or event space with explicit participation and no effect on settlement protection. The commercial loop must work when nobody organizes an event. Defer wagers, territorial rewards, siege engines, and automated tournament logic until their value is demonstrated. Trade disputes remain player decisions; harassment and rule violations remain moderation matters.

Keep configuration and scripts versioned; test changes on a staging copy; pin releases. Establish backups and prove a restore before persistent player investment. Keep starter instructions short enough that joining does not require reading this document.

One human maintainer also owns social operations. Default rules should be mechanically enforceable: permissions, protected builds, and explicit PvP boundaries. Theft disputes, contract litigation, bespoke war arbitration, and daily GM events are recurring labor. Assistant help can reduce technical work but does not remove the need for human judgment or availability.

## First playable experiment

Build one small bay with three settlement sites and several distributed sources of each regional input. Include starter tools, protected settlement space, one short industrial chain, basic local exchange, and boat-accessible routes. Use placeholders before commissioning extensive terrain or art.

Invite 6–12 testers for several sessions. This tests the loop, not production capacity. Later admit a larger cohort to test sustained economic behavior; scale technical load separately toward the actual concurrent-player target.

Look for observable results:

- A newcomer can understand opportunities to participate in the actual economy; independence from powerful groups is not a required outcome.
- At least two groups repeatedly exchange goods voluntarily after introductory prompts end.
- A route or public facility becomes useful enough that someone maintains or improves it.
- Interrupting a supplier creates understandable consequences and meaningful leverage; observe reserves, alternatives, bargaining, and production stoppages without requiring guaranteed recovery.
- Builders and casual players find useful activity without needing an executive role.
- The server runs through the experiment without the maintainer spawning economic inputs or settling recurring rule disputes.

If players avoid trade, fix that before adding currencies, war, professions, or additional recipe tiers. If they trade only because a quest pays them, the player economy is not yet demonstrated.

For the scale test, reproduce the expensive real behaviors: simultaneous exploration, concentrated towns, active factories, entities, freight, saving, and backups. Measure tick latency, memory, and player experience with headroom. Test chunk-unloading behavior and backup restoration. Do not infer support for 200 concurrent players from an empty server or a small social test. If the target fails, reduce simulation or population per world before designing a complicated network of servers.

## Decisions for the next revision

1. Confirmed: up to 200 simultaneous players. Hardware and measured simulation budgets remain open.
2. Confirmed: commercial rivalry and politics with optional organized PvP. Event format remains open.
3. Which setting would you enjoy building and inhabiting for a year?
4. Is operating machinery the main fun, or should machinery mostly support building, travel, and politics?
5. What recurring maintenance time and hosting budget are acceptable?

Resolve these before selecting a large mod list. They determine how much simulation and operational burden the project can support.
