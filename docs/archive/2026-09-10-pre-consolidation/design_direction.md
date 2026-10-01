# Design direction: energy, hierarchy, and the sky

Current design brief — September 10, 2026. Implementation baseline: 0.9.1-dev. See [development_plan.md](development_plan.md) for the active checklist and proposed work order; it supersedes older next-step sections.

Platform decision: **Java Edition**. After reviewing Bedrock, the user prioritizes advanced physics and distant rendering, including interest in Distant Horizons, over native mobile cross-play. `bedrock_feasibility_research.md` remains background research, not the current implementation recommendation. The Prism development client is installed; a dedicated server is not set up yet.

Development strategy: **almost all gameplay will be custom-developed for this project**. Use a small number of external technical foundations where justified, rather than assemble a large gameplay modpack. The initial development environment is now **Minecraft 1.21.1 / NeoForge 21.1.250 / JDK 21**, matching the user's Prism instance. Physics and rendering dependencies remain deferred. Mentioning a project here does not establish its compatibility or capacity.

Physics scope clarified: primarily **player-built airships**, with **Create Aeronautics** the user's preferred candidate backbone. General structural collapse is not the current requirement. Official project pages checked September 10, 2026 list Aeronautics for **Minecraft 1.21.1 / NeoForge**, requiring **Create and Sable**. This is the candidate platform for the technical test; exact dependency versions are not pinned. [Aeronautics](https://modrinth.com/mod/create-aeronautics)

Sable supplies interactive moving block structures and the physics pipeline; Aeronautics supplies vehicle-building gameplay on top. Its maintainers warn of extensive compatibility interactions. Extend this foundation with our own energy, ownership, freight and progression systems; a stable extension API has not yet been established by code inspection. [Sable](https://modrinth.com/mod/sable), [Project source](https://github.com/Creators-of-Aeronautics/Simulated-Project)

Distant Horizons is a separate rendering candidate. Its documentation supports distant terrain/player structures and multiplayer LOD delivery, but this does not establish visibility of moving airships at those distances or compatibility with Sable. Test moving craft separately. Aeronautics currently lists visual issues with Iris shaders; begin without shaders. [Distant Horizons](https://modrinth.com/mod/distanthorizons), [Aeronautics compatibility note](https://modrinth.com/mod/create-aeronautics)

First airship integration checks: passenger interaction, cargo containers, docking, fuel exhaustion, logout/reconnect, chunk transitions, and server restart. Then test multiple active craft and distant scenery together. Audit lift and propulsion mechanics against our no-renewable-industrial-power rule, including passive lift and any renewable drive path. Keeping Aeronautics as a foundation does not imply adopting its entire default economy. Decide whether lift, propulsion, or both consume fuel before changing mechanics. Treat cargo-dependent mass as a requirement to investigate, not an established feature. Do not infer support for giant mobile cities or 200 concurrent players from a small airship test.

Read this first, then the original `status_power_civilization_game_design.md` for the philosophy. This brief supersedes conflicting recommendations in `modpack_server_design_workshop.md`. Confirmed decisions and proposed mechanics are separated below.

## Confirmed intent

- Unclaimed land permits ordinary gathering, building and facility use, without claim-based protection. A controller protects a site rather than unlocking ordinary gameplay. Normal regional/resource rules still apply; separately protected assets such as owner-only death recovery retain their specific rules. See [first_hour.md](first_hour.md) for the proposed startup walkthrough.

- Social baseline: solo players and trusted groups typically around 2–8 people. Group members have full access to shared assets, avoiding per-member permission trees. The range is not a hard cap. Administrative powers and personal-versus-shared ownership details remain to be specified; see [groups.md](groups.md).

- Land-controller power failure has no grace period: an ordinary claim becomes unclaimed and loses claim-based protection immediately at zero energy. Physical structures/goods are not automatically deleted. Reactivation, separate inventory rights and exhaustion during a funded takeover remain to be specified.

- Latest land clarification: buildings, installed equipment and goods may be removed during the move-out week; takeover primarily purchases the land. This supersedes earlier structure-retention rules. The controller's energy is explicitly excluded: withdrawals remain locked, normal upkeep continues, and the remaining reserve transfers to the buyer. Controller removal cannot release the reserve or cancel a funded land transfer.

- A funded land takeover allows **one week (7 days)** for the outgoing owner to move before handover, replacing the proposed two weeks. The takeover window schedule, subsequent eligibility and server-outage clock handling remain open.

- Land takeover valuation uses actual stored energy, not capacity. Full buyer payment fixes the price from the reserve at that moment and begins relocation. Payment is held; reserve withdrawals are locked; normal claim upkeep continues. At the deadline ownership and payment transfer together, with structures and remaining energy going to the buyer. The monthly schedule, relocation duration, premium and failure cases remain open.

- Land allocation: craftable controller costing meaningful coal and other resources, consuming ongoing power scaled with claimed land, with bounded vertical reach. Once a month another player can take over by paying the incumbent a significant premium over the energy they can store in the controller. Provide generous relocation time; structures remain. Exact valuation basis, fuel representation, timing, power-failure and handover rules remain open. [land_controller.md](land_controller.md) supersedes earlier lease/auction candidates; no claim system is implemented.

- Map visibility: terrain is revealed through exploration; public shops/offers appear even beyond explored terrain without revealing the surroundings. Players can inspect property boundaries and ownership. Deposits require prospecting and discoveries remain private unless shared. No public live tracking of players or shipments. Search goods, inspect an offer, mark its destination and travel physically. Detailed UI and sharing mechanisms remain open; map features are unimplemented.

- Shops use a trade counter attached to physical storage, supporting sell offers and funded buy offers, one fuel denomination per offer, offline-owner operation and atomic goods/payment exchange subject to available stock and receiving space. Customers cannot directly access owner storage. Shops and their offers appear on the map; goods still require physical collection/delivery. Map implementation, visibility coverage and detailed controls remain open. These features are not implemented.

- Currency carrier decision: Mineral Coal serves ordinary transactions; oil and uranium serve higher-level transactions as more energy-dense currencies. Payments remain physical fuel, not newly minted abstract credits. Exact oil/uranium forms, denominations and exchange mechanisms remain open; fixed exchange rates are not established. Reconcile higher purchasing power per carried payment with low personal capacity for bulk industrial shipments.

- Currency should be a form of energy. The exact energy form/carrier and whether transactions exchange it directly or transfer claims on stored energy remain open. Abstract starting-money issuance is superseded as the working direction. Conservation, physical freight and distinct coal/oil/uranium capabilities must remain consistent; no fixed inter-fuel conversion rates or energy-backed banking system are approved.

- Exchange direction: physical stocked shops with seller-set prices, local collection, operation while owners are offline, and funded purchase orders for deliveries. Shared currency is the current design topic; representation, issuance and the destination of land payments remain undecided. See [currency_and_exchange.md](currency_and_exchange.md). No currency or shop implementation exists yet.

- First freight craft: a simple coal-powered cargo boat built from early materials, with a fuel slot and large cargo inventory. Load, pilot and unload directly at banks/simple landings without port machinery. Fuel exhaustion stops propulsion and retains cargo. Compact steam-launch/workboat direction; no detailed boiler-pressure or water-management system. Connected navigable rivers and landing opportunities are world requirements. Capacity, consumption, dimensions and implementation remain open; no boat is implemented yet.

- Transport hierarchy: rivers provide early freight; ground freight uses trains, comes later and is always more expensive than river transport. Airships are the most expensive, fastest and highest-capacity option. Exact vehicle designs, energy sources, comparative cost basis and capacities remain open. See [transport.md](transport.md); this is planned behavior, not an installed transport stack.

- Regional feedback: terrain provides the main visual clues; a simple land-inspection interaction explains crop and forestry suitability, and failed planting explains its specific cause. No permanent overlay or complex menu is needed. Buried deposits require separate prospecting rather than being revealed by ordinary land inspection. Controls and prospecting mechanics remain open; these features are unimplemented.

- Forestry: trees grow on suitable ground outside woodland, but substantially faster within woodland regions. Kept livestock has no additional regional restriction; production depends on supplied farm feed and ordinary local requirements. Exact growth rates, yields and planting conditions remain open; these rules are not implemented.

- Globally disable bone meal use, rather than patching only tree growth. Bone/item recipe cleanup remains unspecified; custom Mineral Fertilizer retains its separate role. Develop a shared regional/biome foundation for growth, spawning and deposits; see [region_system.md](region_system.md) for the proposed architecture. Exact regional assignments and tree-growth restrictions are not yet decided.

- Construction-material conversions should better reflect physical block volume. One log yields one plank block instead of four, increasing the timber requirement for construction. Audit other building recipes by material volume rather than universally requiring one item per item. The log conversion is agreed but unimplemented; other recipe changes remain to be specified.

- Regional specialization should be legible from terrain. Farming works only within a certain distance of a river; exact distance, vertical reach and crop coverage remain open. This is an agreed geographic rule, not implemented behavior. See [regional_geography.md](regional_geography.md) for proposed complementary regions and the implications for subsistence, land value and trade.
- Agreed regional pattern: river valleys support agriculture, rocky uplands mining/quarrying, forests timber, and crossings/route junctions towns and storage. Farming uses a natural floodplain band with bounded vertical reach; placed water irrigates eligible fields but cannot create farmland elsewhere, and fertilizer improves output within the band. Exact boundaries and complementary food mechanics remain open.
- Agreed food roles: river crops supply staple calories; livestock consumes farm-produced feed for meat and other foods; fishing and wild gathering support local subsistence and variety. Model livestock feed at breeding/production interactions instead of continuous animal hunger. Exact yields, interaction coverage and resulting farm demand remain unverified; these changes are not implemented.

- Use standard item inventories with item-specific stack limits to represent bulk and weight. Large/heavy goods have low stack sizes; carrying large quantities of Tier 2 resources personally should be impractical. Exact limits, packaging and freight capacities remain open. Use this approach instead of introducing a separate weight meter.
- Halve the ordinary default stack limit from 64 to 32, including common building blocks. Keep the standard inventory layout and use lower limits for bulky industrial goods. Existing 16-stack items require a separate decision; unstackable items remain unstackable. This is planned, not implemented.
- Storage and freight: warehouses and vehicles gain capacity through more slots; vehicle cargo moves physically. Containers must be emptied before being carried as inventory items, with no filled-container nesting. Disable shared remote ender storage. Ordinary chests can serve early warehouses. These rules are agreed but unimplemented; vehicle details and safe migration of existing stored goods remain open.
- Ordinary death leaves all carried items, including equipment, in an owner-only recovery container near the death location, persistent across logout and restart. Respawn at bed/spawn preserves calorie and depletion state; possessions require physical recovery. The recovery system is agreed but unimplemented. Hazardous placement, protected-land access, repeated deaths, cleanup, XP and organized PvP stakes remain to be specified.
- Household cooking uses one simple station burning small amounts of coal. Farming and uncooked food remain sufficient for subsistence; cooking improves food value and preparation options. This direction is agreed but unimplemented; recipes, station construction, fuel rates and replacement of vanilla cooking paths remain open.
- Resource boundary: food, timber and other biological materials are renewable; metals, industrial fuels and strategic mineral deposits are finite. Common building materials remain plentiful through extraction. Remove renewable metal/fuel outputs from mob farms, trading and other alternate sources. Automation may gather/process finite stocks but not replenish them indefinitely. Exact acquisition-path changes, nonmetal generation rules and world lifespan remain open; the boundary is not yet fully implemented.

- Vanilla systems may be disabled or substantially reworked when the result is simple and intuitive. Preserving vanilla mechanics is not a design goal in itself; prefer coherent rules over layers of exceptions. Specific removals and replacements still require a design decision.

- Build a Minecraft modpack and custom server targeting up to 200 concurrent players, maintained by one human with assistant support.
- Java client mods are acceptable and expected. Advanced physics and distant rendering are priorities; the scope of simulated moving structures remains to be specified. Most gameplay systems will be custom-developed.
- Status and competition for real power are central. Hierarchy is a desired outcome of the game.
- Material production, control of infrastructure, and organization underpin power. No universal prestige score or assigned elite class.
- The top should be compelling and awe-inspiring, without being everyone's preferred destination.
- Powerful players determine access to the assets and institutions they control. Universal access, guaranteed upward mobility, and equality of outcomes are not design goals. Moderation rules and technical limits are separate from economic competition.
- Society should be able to develop spatial layers: wilderness and extraction, farmland and processing, concentrated urban power, and an extraordinary elite sky world.
- BioShock and BioShock Infinite inform the contrast between material production and spectacular elite environments. The final architectural style and fictional explanation remain open.
- Trade rivalry and politics are primary conflict; PvP is optional and organized. Economic coercion does not automatically authorize destructive raiding.
- Progression increases the scale of matter and territory players can affect: hand tools, machines, facilities, networks, then regional instruments. The user endorsed monumental construction, weather control, and sky freight as the leading manifestations. Specific mechanics remain proposals.

## Energy direction

### Tier organizing principle

The agreed industrial progression is **Tier 1 coal → Tier 2 oil → Tier 3 uranium**, with calories supporting human labor at every stage. Tiers also organize foods, materials and productive capability without assigning social classes or requiring three versions of every item. Higher-tier resources occur in large, rarer deposits requiring extraction infrastructure. Current hand-mined Mineral Coal, the kiln and Fertilizer Works belong to Tier 1. Coal-stage industry bootstraps oil extraction and refining; oil-stage industry bootstraps uranium extraction, fuel processing and reactors. Earlier fuels retain useful roles, especially oil for mobile machinery and freight after reactors. Uranium supplies reactors rather than acting as ordinary furnace fuel. See [tier_framework.md](tier_framework.md) and [tier_matrix.md](tier_matrix.md); oil, uranium and infrastructure-gated extraction remain unimplemented.

Confirmed scope: universal energy costs with **renewable food for player labor, but no renewable industrial power and no biomass fuel**. Basic subsistence should be relatively accessible; ambitious industrial activity requires harder-to-obtain resources. Calorie costs must be meaningful and commercial farming must be economically viable.

All physical work should draw on an energy budget. Calories support player actions; fuel and derived power support machinery and infrastructure. Forms can convert through specific processes at less than 100% efficiency. Universal accounting does not imply unrestricted conversion between all forms.

Do not substitute a generic upkeep currency for this material chain. Energy should have sources, physical carriers, facilities, conversion losses, and consumers.

### Resolved: food is the renewable exception

Food supports calories and manual work. It cannot be converted into industrial fuel. Wood, charcoal, crop fuel, biofuel, and equivalent biomass routes are excluded from industrial energy generation. Trees may remain building-material sources. Renewable generators are excluded as well.

Manual work remains renewable by design. It can extract finite fuel but must not generate industrial power through a crank, animal drive, or equivalent conversion. Advanced processes require an industrial energy input; repetition of free manual operations must not bypass that requirement. Exact allowed primitive processes, including cooking, still need specification.

### Proposal: subsistence farming and commercial agriculture

Use one crop system with two economic modes, not a farmer class or a farm-size lock:

- **Subsistence:** ordinary crops grow without purchased fertilizer. Modest yields and manual tending let players feed themselves. The farming cycle must return more calories than cultivation and harvesting consume.
- **Commercial production:** fertilizer increases yield per plot or harvest; machinery reduces labor per unit; processing creates compact, calorie-dense provisions. These are investments whose benefit must exceed their input costs, not mandatory upgrades for every household.

Fertilizer is a nutrient input, not an energy unit. Real nitrogen can enter agriculture through biological fixation as well as purchased fertilizers; legumes and crop rotations provide examples ([University of Minnesota Extension](https://extension.umn.edu/agriculture/crop-production/forages/legume-life-cycles-and-characteristics)). An industrial fertilizer requirement is a game-design choice for intensive output, not a claim that all real farming requires mined nitrogen.

First prototype proposal: one fertilizer item, consumed for a bounded harvest bonus. Its production consumes finite industrial energy and any chosen material inputs. Avoid simulating nitrogen chemistry, soil depletion, and runoff. The exact crop integration is an implementation investigation, not an assumed mod capability.

The economic loop becomes: farms feed miners and builders; extraction fuels industry; industry supplies fertilizer and farm equipment; farms supply larger workforces. Food cannot feed a generator, so greater crop output does not create a renewable industrial-energy loop.

Calorie accounting should distinguish light activity, travel, and sustained productive work. Heavy mining and construction should consume appreciably more than a quiet session. Charge valid work rather than failed action attempts. Prefer sufficient food capacity and convenient meals over incessant eating. Offline players do not consume calories in the proposed model.

**Implemented direction, September 10:** replace vanilla hunger, saturation, and exhaustion with one calorie reserve and a numeric calorie-bar HUD. Food adds directly to that reserve; labor, travel, and natural healing spend it directly. Sprinting costs more per distance than walking (initial default 3x). Food items display kcal. Reserve persists through death and logout. The 0.3 prototype uses 2,400 kcal capacity, 0.10 kcal/block walking and 0.30 sprinting; these are tuning defaults, not final balance. See `civilization-mod/README.md` for the complete rules, migration, test commands, and known limitations. The older exhaustion-overlay prototype is superseded.

**Recovery rule, implemented in 0.5:** zero calories triggers depletion rather than a permanent action lock. Walking, basic interactions, and manual labor remain available without debt; mining is four times slower, sprinting/healing stop, and starvation alone stops at three hearts. Eat back to 200 kcal to recover normal work. Empty-handed crouch-right-click foraging on common natural terrain takes 10 seconds and yields a 25-kcal morsel. It supplies a slow recovery route without tools, purchased food, or industrial fuel. Depletion persists through death/relog. Costs and thresholds are prototype defaults; this emergency manual-work exception does not authorize free powered machinery.

**Farming and industry, implemented through 0.9.1:** ordinary wheat yields 1 grain plus 2 seeds. One mineral fertilizer applied during growth changes the next mature harvest to 3 grain plus 2 seeds; replanting returns ordinary wheat. Three bread pack into one calorie-neutral field ration (1,500 kcal by default). Fertilizer now has a survival production chain: clay/gravel blend plus Mineral Coal in the multiblock Fertilizer Works. A multiblock kiln makes bricks; furnace brick smelting and villager brick sales are disabled. Crop, food and machine production have audit records. Vegetation costs 0.25 kcal to break, crop harvest/planting 1 kcal, and stone/logs remain 4 kcal. See the mod README and linked farming/industry notes. The user has manufactured fertilizer in play; geography, ownership and commercial viability remain separate unfinished work.

Before balancing, define a standard play session and measure its work. Provisional relative test targets: light play consumes 1 ration-equivalent; sustained heavy work consumes 3–4. These are test hypotheses, not real nutritional values or final balance. Define a ration-equivalent as a unit of useful food energy, independent of item count.

Measure a farm's saleable surplus as edible output minus its workers' food use and retained planting/operating stock. Economic profit separately subtracts fertilizer, fuel, transport, and labor opportunity cost. Population appetite caps the staple market: increased yields alone do not guarantee a viable industry. Test with different numbers of active workers and competing farms.

Do not require spoilage, daily consumption while offline, or arbitrary food quotas to create demand. First test actual work costs, farm labor, delivery convenience, and batch provisions. Check alternative foods, automation, crop multipliers, and death/respawn food resets before claiming the market works.

### Unresolved: world lifespan

A finite world with finite industrial energy reserves eventually runs out of industrial fuel. Renewable subsistence remains possible. Conversion losses accelerate depletion; they do not resolve it.

Choose an explicit policy: a finite campaign with an ending/reset, a persistent core with deliberate frontier expansion, or reserves provisioned for a chosen operating horizon. Regenerating deposits would change the no-renewables premise. Frontier expansion adds reserves and world-management work; it is not an infinite maintenance-free solution.

Size reserves from observed consumption during prototyping, including the fraction players can realistically discover and recover. Stockpiling remains legitimate material wealth; sustained extraction and delivery remain sources of power.

## Three systems to implement

### 1. Energy and production

Calories, extracted fuel, and derived industrial power form a small conversion graph. Start with one fuel, one generator or processor, and one industrial output. Additional fuel tiers must provide a specific strategic choice.

Implementation status: custom calorie accounting replaces hunger/saturation/exhaustion, with explicit tunable action costs and bounded movement logging. Custom machine recipes consume finite Mineral Coal. A broader vanilla-action/fuel audit and any shared derived-power conversion model remain open; neither is established by the current two-machine chain.

### 2. Ownership and physical logistics

Claims and permissions establish actual control of deposits, workshops, terminals, and residences. Goods move through physical routes. Players negotiate prices, access, employment, and alliances.

**Confirmed social direction: land allocation should work through a market, with generous leeway, favoring those who can make the best (most profitable) use of a site. No formal jurisdictions.** This concerns the rules for acquiring and retaining land, not merely a trading interface. Power comes from assets and negotiated relationships; organizations do not impose territorial laws or taxes on independent holdings. The allocation mechanism, holding costs, tenure, buildings at transfer and payment medium remain open. Wider market offer types are assistant proposals, not confirmed scope. See [property_and_authority.md](property_and_authority.md).

Monopolies, exclusion, and unequal bargaining positions are possible outcomes, not automatic balance failures. Whether rivals can find alternatives follows the world and its rules; do not guarantee fallback recipes merely to neutralize power. Keep routine ownership and exchange mechanically enforceable so the maintainer does not become a contract court.

### 3. Concentrated power and costly display

Players combine supply chains into cities, headquarters, castles, towers, and sky estates. More ambitious assets require greater construction inputs and, where appropriate, ongoing energy flows.

The social layers are outcomes, not mandatory jobs or progression ranks. A mine owner may be more powerful than a sky resident. Architecture displays surplus and association; actual dependencies determine leverage.

Proposal: a sky district has a small number of shared energy consumers rather than a physics simulation for every floating block. Decide what the supply enables, who owns access, how reserves are displayed, and what a shortage disables. Suspension, transport, services, and expansion are candidate uses, not approved mechanics. Falling cities, automatic repossession, and public visitor access are not assumed.

## Creative direction: civilization in vertical section

Working image: dark mineral country and productive farmland feed smoky industrial cities; above them rise immaculate gardens, monumental halls, and extraordinary residences. The supply connection should be visible through freight terminals, elevators, cables, pipes, or other infrastructure consistent with the chosen fiction.

The sky should offer a distinct experience and convincing reasons to desire it: space, views, architecture, exclusivity, institutional proximity, and potentially special infrastructure. Mechanical advantages remain to be designed. Avoid making the setting depend solely on a floating house cosmetic.

Ground settlements can be beautiful, powerful, or independent. There is no predetermined moral alignment for any layer. Players decide what their institutions become.

## Progression through scale

**Hand tools → machines → facilities → networks → regional instruments.**

The progression changes the scope of action, not just action speed. A beginner alters blocks; an established operator runs machinery; an organization builds networks; a sky civilization can change regional productive conditions. These are capability descriptions, not assigned player ranks.

The sky is the apex of material reach as well as an expression of wealth. Its instruments amplify and direct the productive base below, consuming its fuel, materials, and services. They do not conjure resources or eliminate that base.

Three leading capabilities:

| Capability | Fantasy | Smallest proposed experiment |
| --- | --- | --- |
| Monumental construction | Commission canals, viaducts, districts, and enormous structures | A bounded construction job consumes supplied blocks and finite energy, building gradually from a plan |
| Weather control | Sell productive weather windows and influence regional agriculture | One fuel-powered instrument grants a timed agricultural rain bonus within a defined area |
| Sky freight | Move industrial quantities through privately controlled elevated terminals | A paid-in-energy shipment moves existing cargo between two authorized terminals |

These experiments are proposed abstractions, not verified mod features. Their physical presentation should make the work visible: a developing structure, a weather instrument activating overhead, or freight ascending toward the sky. A local agricultural effect need not imply a full climate simulation.

Construction must account for materials, excavation outputs, ownership permissions, and bounded work rates. Freight must preserve cargo through interruption and restart. Weather must have a defined radius, duration, fuel cost, and overlap rule. None should require scanning the entire world continuously or loading unlimited territory.

Destructive weather is not part of the initial proposal, consistent with optional organized PvP. Terrain alteration follows build permissions. Access to instruments and services belongs to their owners; public service is not assumed.

Other possible later capabilities include deep prospecting and regional power delivery. Keep them deferred until the three leading capabilities prove useful.

Creative work should define the appeal, architecture and supply dependencies of a first prestige destination, with the weather observatory and freight terminal retained as references. Large construction remains a later candidate, not the next development step. The user rejected automatically proceeding to a construction machine. Exact costs from `first_playable_economy.md` remain historical placeholders.

## Current checkpoint and updated plan

**Latest work direction:** flesh out the whole interconnected mod before resuming coding, working through low-hanging decisions and logical dependencies. [systems_blueprint.md](systems_blueprint.md) is the planning map; it supersedes earlier immediate-next-task recommendations. Land allocation remains open, and industry expansion remains deferred. The existing regional-economy milestone below remains a development candidate, not the immediate planning agenda.

The working build is 0.9.1-dev. Calories, recovery, wheat/fertilizer, field rations, Mineral Coal, kiln production and Fertilizer Works production are implemented. Machines require physical multiblocks. Controllers list structural quantities and select transparent guides that disappear as correct blocks are placed. The 0.9.1 UI shows previews and top-left information only while looking at the unfinished build area; the extra Build screen and G toggle were removed. Existing machine identities and inventories are preserved.

The user has confirmed producing fertilizer in play. This establishes the survival production path, not a multiplayer economy or 200-player performance. Mineral Coal is finite for custom industry, but other vanilla energy paths are not comprehensively governed yet. Raw Mineral Blend remains provisional feedstock.

Development preference: take larger coherent implementation steps, including additional machines when useful. Continue appropriate automated checks without making every addition wait on a manual playtest.

The proposed next milestone is a **first multiplayer regional economy**: explicit resource/energy rules, enforceable ownership, local exchange and useful physical delivery between production sites. Its immediate design input is the three-tier matrix in [tier_framework.md](tier_framework.md): energy sources, deposits, extraction requirements, materials, food roles and bootstrap dependencies. Dedicated-server correctness, recoverable operations and creative development proceed alongside it. The construction machine is deferred; no next machine has been selected.

Use [development_plan.md](development_plan.md) for the formal completed-work checklist, proposed work packages, definitions of done, open decisions and later milestones. Review it before implementing the next feature. Preserve the core chain: energy and surplus must support meaningful capability, dependency, power and visible status.
