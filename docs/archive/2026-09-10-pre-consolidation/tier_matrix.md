# Tier matrix — working dependency design

Status: coal → oil → uranium is the agreed industrial progression. Civilization-scale ambitions are already established. Material families, extraction methods, processed fuel products and detailed recipes remain proposed specifications, not implemented features.

Companion documents: [tier_framework.md](tier_framework.md), [design_direction.md](design_direction.md), [development_plan.md](development_plan.md).

## 1. Begin with the documented Tier 3 capabilities

Tier 3's broad purpose is already established. We are specifying what supports it, not asking again what the endgame should be.

| Established direction | Capital requirements proposed for the matrix | Recurring requirements | Why the lower world still matters |
| --- | --- | --- | --- |
| Monumental construction | Heavy structural components, advanced drive assemblies, substantial foundations and a supplied work site | Actual construction materials, industrial power, deliveries and operators | Machinery places or moves supplied matter; quarries, foundries, carriers and builders remain necessary |
| Regional weather control | Atmospheric instrument, specialized assemblies, supporting station and service access | Advanced energy, bounded operating time and replenishment | Farms can depend on a service whose operator depends on extraction and industry; no resource creation |
| Sky freight and airships | Cargo terminals, lifting/propulsion equipment, structural components and advanced assemblies | Energy for the chosen lift/propulsion model, actual cargo, loading and maintenance provisions | Routes connect productive sites to consumers; cargo is not conjured or globally delivered by a market |
| Elite sky civilization | Extraordinary player-built architecture, access infrastructure, regional instruments and supply facilities | Fuel and material deliveries, staffed operations, provisions for residents and workers | The apex concentrates control and costly display while depending on ground production |

The exact weather effect, construction mechanism, airship integration and sky-support fiction are unresolved. This matrix neither selects teleporting freight nor promises simulated floating cities. Lift versus propulsion energy costs remain open. A building's failure state is not assumed to be collapse.

## 2. Three industrial tiers

**Agreed progression: coal → oil → uranium.** Calories support people at every stage; they are not a fourth industrial tier. This is a gameplay progression with overlapping fuel uses, not a claim that one source completely replaced another historically.

| Category | Tier 1 — Coal | Tier 2 — Oil | Tier 3 — Uranium |
| --- | --- | --- | --- |
| Reach | Manual work and first industrial sites | Mechanized production, transport and developed cities | Industrial networks and regional instruments |
| Labor energy | Calories | Calories | Calories |
| Industrial energy | Finite hand-accessible coal for heat and first machinery | Crude oil processed into useful engine and industrial fuels | Processed uranium fuel used in reactors; delivered power for major installations |
| Resource access | Common materials, base metals and hand-mined coal | Large, rare oil reservoirs requiring drilling, pumping and handling | Rare large uranium deposits requiring industrial extraction and fuel processing |
| Proposed material outputs | Ordinary construction materials, bricks, fertilizer and bootstrap machine parts | Bulk metals, steel, structural components and industrial chemicals | Advanced alloy and precision assemblies |
| Facilities | Farms, workshops, kiln, Fertilizer Works and first machinery | Oil extraction, storage, refinery, developed mines and fabrication | Uranium extraction, fuel processing, reactors and regional infrastructure |
| Food role | Simple sustaining meals | Prepared provisions convenient to supply and carry | Expedition provisions and luxury cuisine; still ordinary kcal |
| Relationship to power | Work, knowledge, useful stock and early production | Control of productive sites, transport and reliable output | Control of networks, instruments and institutions |

Fuel identities are agreed. Material assignments, machinery and food progression remain proposals. Food tiers need not correspond one-to-one with industrial fuel requirements: existing field rations and fertilizer already work before oil.

The labels describe productive capability, not mandatory player ranks. A sky institution can buy basic grain and ordinary stone; an independent miner can possess advanced goods without owning their production facilities.

**Earlier energy remains useful.** Coal can continue serving process heat and early facilities. Oil retains a role in engines, freight and mobile equipment after reactors exist. Uranium is reactor fuel for major installations, not a universal replacement item for furnaces or airships. Exact airship power remains open.

Electricity is a delivery form, not inherently Tier 3. Define generation output, conversion losses, transfer capacity and facility requirements when introducing derived power. Advanced capability should depend on appropriate infrastructure, materials and operating requirements; it must not reduce to compressing coal into uranium. No biomass, renewable industrial generation or calorie-to-power route.

## 3. Keep the material vocabulary small

| Material family | Proposed role | Avoid |
| --- | --- | --- |
| Existing iron and copper | First tools, fittings and machinery | Replacing every vanilla material unnecessarily |
| Steel | Larger industrial frames and substantial structural parts | Requiring oil-only steel to build the first oil facility |
| Structural components | Batch item representing fabricated industrial structure | Separate recipes for every bolt, plate and beam |
| Advanced alloy | Scarce material for high-capability equipment | Obtaining it solely by compressing common metal |
| Precision assemblies | Advanced actuators and instruments using alloy plus ordinary inputs | Requiring them in every cosmetic object |
| Refined oil fuel | Useful carrier produced from crude oil | Treating raw crude as every engine's finished fuel |
| Processed uranium fuel | Consumable for a reactor with a defined energy yield | Burning uranium directly in ordinary machines |

The alloy mineral, fertilizer feedstock and exact fuel products remain design choices. Uranium's identity is settled; its game processing chain is not. Keep it manageable rather than introducing a detailed chemistry simulation.

Food retains one calorie meter. Preparation, carrying density, expedition use and luxury distinguish foods. Packing conserves calories unless additional ingredients supply them. Existing field rations demonstrate convenient concentration without requiring oil or a new energy currency.

## 4. Extraction and the bootstrap path

**Higher-tier deposits are large, rare and infrastructure-gated.** Ordinary pickaxe swings cannot extract an oil reservoir or replace an industrial uranium operation. Breaking a deposit marker must not drop its industrial resource or relocate the reserve. Deposit representation, depletion storage and prospecting UI remain technical specifications.

1. **Manual work establishes coal industry.** Gather food, stone, base metals and hand-mineable Mineral Coal. Existing kilns and Fertilizer Works belong here. Specify the first metal/component production path using these available inputs.
2. **Coal industry opens oil.** Construct the first drilling, pumping, storage and refining equipment using coal-stage materials. Coal supplies its initial operating energy. Neither construction nor first production can require oil-only outputs.
3. **Oil expands industrial scale.** Refining supplies fuels for larger machinery and physical transport. Larger extraction, processing and fabrication facilities support cities and the next tier. Exact machinery and recipes remain proposals.
4. **Oil industry opens uranium.** Oil-stage equipment and power establish uranium extraction and fuel processing. Initial reactor construction must use available materials; it cannot require reactor-only production.
5. **Reactors enable major installations.** Processed uranium produces power through a reactor with explicit fuel consumption and conversion accounting. Advanced materials and assemblies support monumental construction, regional weather instruments and sky infrastructure. Their exact recipes and delivery systems remain open.
6. **Demand flows back down the chain.** Advanced consumers continue needing fuel, parts, ordinary construction materials, food and deliveries. No closed loop creates more industrial energy than it consumes.

Current Mineral Coal is the agreed Tier 1 fuel. Its hand-mineability is consistent with the higher-tier resource rule because that rule gates oil and uranium. No separate starter-fuel item or inventory migration is required by this decision. Larger coal mines may later improve throughput without making a first oil operation depend on oil.

## 5. Dependency table — proposed processes, not numerical recipes

| Process | Input | Initial operating energy | Required capital | Output |
| --- | --- | --- | --- | --- |
| Existing kiln | Clay blocks | Mineral Coal | Kiln multiblock | Bricks |
| Existing fertilizer manufacture | Current clay/gravel blend; eventual feedstock unresolved | Mineral Coal | Fertilizer Works | Fertilizer |
| Bootstrap metal/component production | Available base metals and chosen process inputs | Coal | Equipment constructible from existing materials | Parts sufficient to build the first oil chain |
| Oil extraction | Valid finite oil reservoir | Coal-derived power | Drill/pump and handling equipment built from Tier 1 outputs | Crude oil |
| Oil refining | Crude oil | Coal-derived heat/power for startup | Refinery and storage built without oil-only inputs | Refined oil fuel |
| Larger fabrication and extraction | Metal feedstocks and process inputs | Process-appropriate coal or oil energy | Developed sites and machinery | Bulk materials, proposed steel and structural components |
| Uranium extraction | Valid finite uranium deposit | Oil-stage power | Industrial extraction and handling equipment | Uranium feedstock |
| Uranium fuel processing | Uranium feedstock and chosen inputs | Oil-stage power for startup | Processing facility built from Tier 2 outputs | Reactor fuel |
| Reactor operation | Processed uranium fuel and defined operating inputs | Startup requirements supported by Tier 2 | Reactor built without reactor-only materials | Usable power with explicit fuel consumption and losses |
| Advanced material/assembly production | Rare material feedstock and ordinary industrial inputs | Process-specific power, with a non-circular first-production route | Processing and assembly facilities | Advanced alloy and precision assemblies |
| Regional operation | Actual supplied cargo/materials where applicable | Facility-specific energy | Construction works, weather station or freight infrastructure | Useful service, not free raw resources |

Only the existing kiln and fertilizer rows describe implemented machines. No new numerical rates are assigned here. Quantify time, reserve consumption, idle behavior, transport and capital using a common operating-energy accounting scheme. Heat, electricity and fuel require explicit conversions. Extraction infrastructure also needs bounded server work; do not require continuous simulation of every block in a deposit.

## 6. Decisions and next specification

**Established:** three-tier organization; coal → oil → uranium; calories supporting all stages; existing Mineral Coal and machines in Tier 1; rare infrastructure-gated higher resources; continuing lower-tier dependencies; Tier 3's documented construction, weather, freight and sky ambitions.

**Proposed:** the short steel/components/alloy/assemblies vocabulary; particular extraction and processing facilities; food roles; precise material assignments.

**Still open:** deposit geography, reserve sizes and world lifespan; extraction mechanics; metal and cooking boundaries; fertilizer and alloy feedstocks; refined fuel products; power delivery and conversions; construction recipes and operating rates; exact regional machine mechanisms.

**Next design package: the coal-to-oil chain.** Specify one oil deposit model and a compact extraction/refining setup, including construction materials, coal-powered startup, crude and fuel storage, throughput, depletion and the first useful oil-powered capability. Trace every input back to available Tier 1 production before implementation.

This records the accepted energy ladder. It does not implement oil or uranium, reopen Tier 3's broad purpose or select construction automation as the next feature.
