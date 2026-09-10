# Civilization — game rules

This is the single source of agreed game-design rules. “Agreed” means the direction is settled, not that it is implemented. Current build status and work packages live in [development_plan.md](development_plan.md); unresolved choices and recommendations live in [open_decisions.md](open_decisions.md). The original philosophy and previous drafts are reference material, not additional requirements.

## 1. The point of the game

**Energy → surplus → capability → dependency → power → status.**

Start with personal labor and expand the scale of matter and territory you can influence. Build productive places, control useful resources and routes, and use their surplus to create extraordinary institutions. Hierarchy, exclusion and concentrated power are intended outcomes. Status emerges from what people can build, supply and control, without a universal prestige score or assigned social class.

Support solo play and trusted groups, typically 2–8 people; that is not a membership cap. Large institutions can emerge, but ordinary farming, building, exploration and trade should be worthwhile lives. No formal jurisdictions. Trade rivalry and politics are central; PvP is optional and organized, not unrestricted destructive raiding.

Target up to 200 concurrent players, maintained by one human with assistant support. Most gameplay is custom Java development. Vanilla systems may be reworked or removed when the resulting rules are simpler and more intuitive.

## 2. Four connected systems

| System | Simple rule | What it creates |
| --- | --- | --- |
| **Land and resources** | Terrain determines productive opportunities; deposits hold finite reserves | Places worth discovering, developing and buying |
| **Energy and production** | People spend calories; industry spends finite fuel to transform real inputs | Food demand, useful surplus and increasing capability |
| **Storage, freight and trade** | Goods and fuel occupy inventory space and move physically | Markets, carriers, routes and supply relationships |
| **Ownership and access** | Powered claims protect places; trusted groups share assets; land can change hands through payment | Investment, cooperation and competition for control |

These are ways to organize the design, not four mandatory code frameworks. The same physical fuel supports production, commerce and claims. The same inventories support households, warehouses, shops and vehicles. Regional information also supports the map and local feedback.

## 3. People, food and recovery

- One calorie reserve replaces hunger, saturation and exhaustion. Work, movement and healing have explicit costs; sprinting costs more per distance than walking. No idle or offline calorie drain.
- At depletion, walking and basic actions remain possible; mining slows, sprinting and natural healing stop. Starvation alone has a health floor. Deliberate hand-foraging provides a slow recovery route. Death/relog do not refill calories or clear depletion.
- Ordinary farming and uncooked food can sustain a person without industrial fuel. River crops supply staple calories. Livestock consumes farm feed at breeding/production interactions, with no continuous animal-hunger simulation or additional regional restriction for kept animals.
- Fishing and wild gathering support subsistence and variety; their effort and yields must leave purchased provisions useful.
- Household cooking uses one simple coal-fired station. Cooking improves food value and preparation options. Packing provisions conserves calories; food tiers express convenience, carrying density and luxury within one calorie scale.
- Globally disable bone meal use. Custom Mineral Fertilizer remains a separate input that improves eligible agricultural output.
- Ordinary death leaves all carried possessions, including equipment, in an owner-only recovery container near the death location. It persists through logout/restart. Respawn at bed/spawn preserves calorie state; players return to recover goods.

Current calorie values and working recipes are documented with the implementation, not duplicated here.

## 4. Regional land and materials

Terrain must make productive opportunities understandable. Use a shared region/biome foundation for growth, natural spawning and deposit placement. Regions can overlap.

| Landscape/activity | Rule |
| --- | --- |
| River floodplain | Farming requires a natural river band with bounded vertical reach. Terraces can qualify; sky platforms do not inherit eligibility merely by being above a river |
| Irrigation/fertilizer | Placed water irrigates already eligible fields; it does not create new agricultural regions. Fertilizer improves output within the band |
| Woodland | Trees grow on suitable ground elsewhere, but substantially faster in woodland |
| Rocky mineral country | Mining/quarrying opportunities; exact deposit distribution remains open |
| Livestock | Suitable sites with supplied farm feed; no special livestock region |
| Crossings and route junctions | Attractive places for towns, processing and storage because supplies meet there, not because of a city-biome bonus |

Food, timber and other biological materials are renewable. Metals, industrial fuels and strategic mineral deposits are finite. Remove renewable metal/fuel sources from loot, trading and other alternate acquisition paths. Ordinary building materials remain plentiful through extraction. Automation may gather/process finite stock but cannot replenish it indefinitely.

Higher-tier materials and energy occur in large, rarer deposits requiring infrastructure. More hand mining cannot substitute for required extraction equipment. Discovering or breaking a marker cannot duplicate or relocate a reserve.

Construction conversions should respect physical block volume: **one log → one plank block**. Partial blocks and other recipes need their own sensible ratios, not a blanket one-item-in/one-item-out requirement.

## 5. Energy, industry and scale

| Industrial tier | Energy and access | Capability direction |
| --- | --- | --- |
| **1 — Coal** | Hand-accessible Mineral Coal | First machinery, kiln, fertilizer and household cooking; first river freight |
| **2 — Oil** | Rare large deposits requiring extraction and refining | Larger industry and transport |
| **3 — Uranium** | Rare deposits, industrial extraction/fuel processing and reactors | Major installations, regional instruments and sky civilization |

Calories support people throughout; they are not a fourth industrial tier. Lower-tier goods and fuels remain useful after advancement. Oil remains useful for mobile work even when reactors exist.

No renewable industrial generation or biomass fuel. Food, cranks and animal labor cannot become industrial power. Allowed energy conversions have explicit inputs and losses. Uranium fuels reactors rather than ordinary furnaces; no universal conversion between fuel types or fixed market exchange rate is implied.

Each stage must construct and start the next using already available materials and energy. Coal industry opens oil; oil industry opens uranium. Avoid circular recipes. A short material vocabulary is preferable; steel, structural components, advanced alloy and precision assemblies remain candidates, not a committed catalog.

Machines have physical multiblock construction where specified. Controllers explain required materials with contextual textured ghost previews; guides disappear when complete or not being inspected. No separate build screen or hide key is needed. This does not require every future cooking station or vehicle to be a multiblock.

## 6. Carrying and transport

Standard inventory slots represent carrying capacity. Ordinary 64-stacks become **32**, including building blocks. Bulky industrial goods stack lower, making large Tier 2 shipments impractical to carry personally. Existing 16-stack items stay unchanged in the first implementation; unstackable items remain unstackable.

Warehouses and vehicles gain capacity through more slots. Ordinary chests can serve early warehouses. Containers must be empty to become portable inventory items; filled-container nesting cannot bypass capacity. Disable shared remote ender storage. An assembled cargo vehicle can physically move its inventory.

| Mode | Agreed position |
| --- | --- |
| Rivers | Early, cheapest freight |
| Trains | Later ground freight; always more expensive than river transport |
| Airships | Most expensive, fastest and greatest bulk capacity |

The first craft is a simple coal-powered cargo boat, built from early materials, with a fuel slot and large cargo inventory. Load, pilot and unload directly at a bank or simple landing. No required port machine, boiler-pressure system or water-management simulation. Fuel exhaustion stops propulsion and leaves cargo aboard. Compact steam launch/workboat is the visual direction.

World terrain must provide connected rivers wide/deep enough for useful routes, turning and landing opportunities. Not every tributary must be navigable. Train details, airship physics, precise speeds/capacities and comparative cost definitions remain open.

## 7. Fuel currency and shops

Mineral Coal is ordinary currency; oil and uranium are denser currencies for higher-level transactions. Payment consists of physical fuel that can later be consumed in its appropriate industrial process. Exact oil/uranium forms remain open.

One trade counter connects to physical storage:

- **Sell:** stocked goods for a stated fuel quantity.
- **Buy:** delivered goods for a reserved fuel quantity.

Each offer specifies one fuel form and quantity. No automatic conversion; players set prices and can exchange fuel types through offers. Customers access the offer, not the owner's inventory. Trades require available goods, payment and receiving space, and complete together. Shops operate while owners are offline.

Shops/offers appear on the map, including beyond explored terrain. Information is remote; purchases, collection and delivery remain physical. Visible stock is not a reservation.

## 8. Ownership and groups

Unclaimed land allows gathering, building and facility use under ordinary regional/tool/energy rules, without claim protection. A controller secures a place rather than unlocking basic gameplay. The owner-only death container retains its specific rule; other separate asset rights still need definition.

Trusted group members have full access to shared land, machinery, storage, shops and vehicles. No member role tree is needed. Full access still obeys global rules, including takeover energy locks. Group administration, personal/shared assignment and departure details remain open.

### Land controller

Craft and place a management block using a meaningful minimum of coal and other resources. Claim reach is bounded above and below; exact geometry is open. Ongoing power cost increases with the amount of land. **At zero energy an ordinary claim immediately becomes unclaimed and loses claim protection, with no grace period.** Structures and goods are not automatically deleted.

Once a month, a buyer can take over by paying the owner a significant premium over **actual stored controller energy**, not empty storage capacity. The exact calendar/window and premium are open.

The agreed transaction is:

1. Buyer deposits the full required payment during an eligible opportunity.
2. Snapshot actual stored energy and fix the price. Hold payment pending handover.
3. Give the owner **one week** to move. Reserve withdrawals are locked; normal claim upkeep continues without repricing.
4. At the deadline, transfer ownership and payment together. The buyer receives the remaining controller energy.

The seller may remove buildings, machines, goods and vehicles during the week. This buys land, not guaranteed structures. Controller energy is the exception: it remains locked and transfers to the buyer. Removing a block must not erase the pending transaction or release locked stock.

Power exhaustion during an already funded takeover is unresolved; no exception to the no-grace rule has been approved.

## 9. Information and interface

Terrain gives the main clues. A simple local inspection explains crop/forestry suitability; failed planting explains its cause. Prospecting discovers buried deposits separately.

The map reveals terrain through exploration, shows public shop locations/offers without revealing their surroundings, and lets players inspect property boundaries/ownership. Deposit discoveries remain private unless shared. No public live tracking of players or shipments.

The basic interaction is **search goods → inspect offer → mark destination → travel**. Controls, sharing and map technology remain implementation choices. Avoid turning inspection into a mandatory permanent dashboard.

## 10. The apex and its visual identity

**Extremely fast airships are a core sky-civilization status symbol and energy consumer.** Owners can pour substantial energy into exceptional speed, gaining useful reach and a visible expression of wealth. This is an agreed endgame direction. Exact speed ranges, energy curves, craft configurations and technical limits remain open; no flight system is implemented.

The intended scale is **hand tools → machines → facilities → networks → regional instruments**. Monumental construction, regional weather control, sky freight and elite sky institutions are established ambitions. They consume supplied materials/energy and depend on the ground economy; precise mechanisms remain open.

Working art direction: productive river valleys and mineral country; weathered brick, iron and copper industrial cities; ivory/turquoise Art Deco sky estates, gardens and observatories. Freight and service infrastructure make the dependency visible. The sky offers awe, access and extraordinary reach; beautiful and powerful ground settlements remain possible. No automatic falling-city simulation or destructive weather weapon is approved.

The four [concept images](concept_art/prompts.md) express this direction, not implemented capabilities. Preserve the aspirational scale while choosing a maintainable mechanism for each feature.

The accepted creative direction gives every layer its own prosperity: warm river homes and painted warehouses, substantial forest halls, dramatic stone mining settlements, busy industrial cities and serene sky gardens. Buildings have attractive inhabited spaces and a visible working side—loading yards, fuel stores, service walks and machinery. Copper gorges, pale river country and misty forest valleys are visual references, not additional committed resource chains. Named airships, recognizable silhouettes and engine sounds express reputation; formal race mechanics remain a future candidate.

**Sky-to-ground wireless power:** a single great machine is the sky civilization's central transmitter. It directs supplied energy to **one regional tower at a time**, rather than broadcasting to several simultaneously. Regional observatories are majestic, enormous towers that dominate their landscapes. They receive power to bring rain and potentially other useful effects. Draw on Tesla-inspired wireless-power imagery and ancient-monument transmitter stories as fictional engineering; pyramids are not required. Target selection, switching, ownership and effect mechanics remain open; no wireless network is implemented.

Wireless transmission transfers supplied energy with losses; it is not a renewable source or item-transport system. Regional services retain a material supply chain and finite operating costs.

## 11. Immersion throughout the world

Immersion is a core goal for every activity and stage: farming, mining, building, trade, travel and sky civilization. Ground-level work needs its own satisfying experience, not merely an obligation on the way to the endgame. Treat atmosphere, physical interaction and understandable consequences as part of system design, rather than a final visual polish pass.

Specific sensory treatments and interaction proposals are in [open decisions](open_decisions.md#immersion-direction--proposals). This goal does not by itself approve new survival meters, mandatory minigames, slower actions or additional simulations.
