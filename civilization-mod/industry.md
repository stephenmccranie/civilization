# Industry

Deposits, the oil refinery, industrial liquids, engines and bulk storage. An engine drives the **Oil Derrick** that drills a well into an oil body and pumps it; the crude passes through a **Fired Heater**, **Distillation Column** and **Air-Cooled Condenser** to become refined fuel, with lubricating oil and sulfur as coproducts. The [Coal Engine](#coal-engine) burns Coal; refined fuel runs Coal Drills, [Hot-Bulb Engines](#hot-bulb-engine) and the [boats](vehicle-physics.md#boats) they drive. Controllers need Machine Parts from [manufacturing](manufacturing.md); hand mining of huge coal veins is in [coal mining](coal-mining.md); shared machine rules are in [machines](machines.md).

## Deposits

Three resources generate naturally: **dense coal seams**, **oil reservoirs** (with surface clues) and [uranium](geography.md#frontier-uranium). Sulfur is a refinery coproduct; steel, parts and finished liquids are manufactured. Vanilla ores keep their normal generation, and ordinary coal ore is hand-mined for Coal.

### Prospecting

Assemble a **Prospecting Rod** at the Assembly Bench from 1 Iron Rod + 2 Sticks and right-click the ground. It reports the closest large deposit within 192 horizontal blocks: its horizontal center, underground Y range and whether the inspected spot is within extraction reach. It describes the generated site, not remaining stock, so an emptied site stays discoverable. It has a one-second cooldown, never loads chunks and does not mark the Survey Table.

### Where deposits form

Coal and oil each select deterministic **1,024×1,024-block districts**. The biome at a candidate center's natural surface gives its permanent climate from Minecraft's biome temperature index: **cold below 0.3, temperate from 0.3 to below 0.9, hot from 0.9**. Weather, altitude and player heat do not matter, and modded biomes follow the same rule.

| District opportunity | Cold | Temperate | Hot |
| --- | --- | --- | --- |
| Coal seam | 1 in 8 | 1 in 16 | none |
| Oil field | none | 1 in 32 | 1 in 16 |

Inside a 2,048-block town radius this gives about 1.57 cold-coal, 0.79 temperate-coal or hot-oil, or 0.39 temperate-oil opportunities before terrain rejection; these are frequencies, not quotas. Each district places at most one field per resource, searching its sixteen 256×256 cells in seed-stable order with up to four candidates per cell. A mixed district rolls once per resource and evaluates each candidate's own climate. Coal and oil use independent seed salts; if both choose a cell, coal keeps it and oil moves on. Candidate centers stay at least 80 blocks from their cell edges, so a field never leaves its cell.

Terrain checks sample the center and eight edge points. Oil needs dry natural surface and host rock at the body's center height at all nine; coal needs a dry center, host rock at the center and at least six of nine body samples, so single river or cave cuts are allowed. There is no flatness cutoff.

- **Coal seam:** elongated and gently irregular, **141–161 × 99–113 blocks**, 2–3 blocks thick (up to 5 near the middle) with a slight tilt. For three fifths of candidates the generator aligns the seam with a natural rock or riverbed exposure inside the footprint where a dry-centered seam fits; otherwise it lies about 24 blocks below the center surface. Exposed dense coal is the seam itself.
- **Oil field:** a tapered pocket **69–73 × 55–59 blocks** centered about 32 blocks below its seep, one oil block per column at the rim and up to six near the center under a two-block air cap. One crude-oil source sits on the dark Oil Seep block at the center and slowly spreads over terrain as a visible clue; it cannot be bucketed and is not reservoir stock, so removing it changes nothing below.
- The long axis runs east–west or north–south. Generation replaces natural rock only, so caves lower actual stock. Stock is the generated blocks themselves; there is no separate counter. Each chunk generates only its own slice.

Dense seam, seep and reservoir blocks cannot be mined in Survival, drop nothing and resist explosions and pistons. Swimming through crude slows movement. Sites and empty districts are cached per level (4,096 cells); sampling never loads chunks or creates stock.

### Extraction reach

An **Oil Derrick** may stand at any height above the oil body's top with its well, under the walking beam's head five blocks behind the controller, horizontally inside the footprint, and [drills that well](#oil-well) down to the body. A **Coal Drill** stands over the seam within sixteen blocks of its center surface height and needs no shaft. Neither machine quarries overburden: they remove only matching resource blocks in the body below, scanning from the field center outward and down each column, at most 1,024 cells per ten-tick work step. A loaded slice can be worked while distant chunks are unloaded; an unloaded slice never counts as depleted, and extraction never loads chunks.

Coal extraction replaces the dense block with air and puts **16 Coal** into the machine. Oil has eight height steps per block: the derrick drains **125 mB** steps from the top of the nearest column downward, then outward, and empty cells become air. Reservoir oil never spreads, equalizes or regenerates. All machines draw on the same blocks, so nothing pays twice, and breaking or rebuilding a controller restores nothing.

## Refinery

| Machine | Shape and connections |
| --- | --- |
| Oil Derrick | Fully modeled tapered timber tower with five X-braced bays, cobblestone footings, middle and crown galleries and a crown sheave; inside the open front bay a band wheel on two bearings cranks the walking beam over the well, beside the cable drum of the bull wheel. 9×10×26 blocks with the ground-level controller, the band wheel, at the front middle and the well five blocks behind it. Crude Refinery Port immediately to its right |
| Fired Heater | Five-wide brick hearth, riveted steel jacket, stepped crown and narrow rear flue; eight blocks tall; crude intake left, heated output right |
| Distillation Column | Five-wide, eleven-high clipped-corner vessel with tapered crown; intake low on the left, vapor outlet high on the right, lubricating-oil outlet low on the right |
| Air-Cooled Condenser | Seven-wide horizontal vessel with rounded edges, cooling-grille faces and steel cradles; vapor intake left, fuel outlet right |
| Coal Drill | Extraction frame; fuel intake at back, lubricant intake at left, coal hopper output below |
| Liquid Tank | Single block holding 16,000 mB of one fluid; every side accepts or supplies it |

Sides are as seen facing the controller's front. The heater, column and condenser are built with the ordinary [construction guide](machines.md#building-with-a-controller) from Industrial Casing, Cooling Grilles, Refinery Flues, bricks, saw-cut pieces and Refinery Ports; automatically placed ports face outward, and empty-hand right-click on a port names its machine and role. Required parts must stay intact for processing and remote fluid access. Industrial Guardrails are decorative only.

### Oil Derrick construction

Place the Oil Derrick controller at ground level (holding it previews the whole tower where a right-click would put it) and **right-click it with construction materials in hand**. Only the held stack is used; a click adds as many matching sections as it can afford, working upward, and leftovers stay in hand. The guide shows the missing model sections, remaining materials and progress, with red outlines on blocked cells; it follows the [guide performance contract](machines.md#guide-performance-contract).

The bill is **165 planks, 7 stone or cobblestone, 4 Iron Plates, 2 Machine Parts and 1 Refinery Port** across 31 sections: footings (7 stone), 20 frame panels (6 planks each), the middle gallery's two pairs of sides (8 planks each), the crown gallery's two pairs of sides (5 and 4 planks), floor (8 planks), walking beam (6 planks), bull wheel (6 planks), crown block and drill line (4 Iron Plates), band wheel (2 Machine Parts) and output port. Sections may use different plank species, each from one held stack, without changing the timber texture. Each section checks loaded space, build limits, claims, placement permission and entities; blocked sections consume nothing. Placement labor applies (356 kcal for the body before modifiers, plus the port). Creative construction is free.

Completed sections are solid immediately. **Breaking a frame section or the port removes only that section**, refunds its paid materials once and stops the derrick until it is repaired with the same held-item interaction. Breaking the controller dismantles the derrick, refunding the remaining materials once and dropping its inventory and controller; the well and its casing stay in the ground and a new derrick drills afresh; Creative-built sections refund nothing. Partial progress and payments are saved. Drain the derrick's liquid before removing the controller.

### Oil well

An [engine](#engines) within eight blocks of the controller drives the derrick through its band wheel, and the walking beam does all its work. First it **drills one well** straight down from under the beam's head: each block takes **5 full-power seconds**, and each new four blocks of well first take **one Iron Plate of casing** from the controller's two slots (screen or hopper). The bit passes caves, liquids and the oil seep, and stops at bedrock, the bottom of the world or ground claimed by another owner. The hole is not dug out: nothing is mined and nothing drops. When the bit reaches the oil body the derrick has **struck oil** and the same beam **pumps** it, one 125 mB oil step per **2.5 full-power seconds**, from the [physical oil](#extraction-reach) as before. A Coal Engine gives half power, so it drills a block in 10 seconds and pumps 25 mB/s; a Hot-Bulb Engine gives full power.

The derrick asks only for the work its current block or step still needs and only while it can use it, so an engine idles while the casing, output or deposit stops the derrick. The beam rocks while work flows. The screen shows the bit's depth and cased depth, or the well's depth once it has struck oil, and the status names what is missing: casing, an engine in range, the engine's work, or the reason the bit stopped.

The tower renderer stays drawn when the controller's chunk section leaves view (within a 128-block controller distance), samples ambient light around the tower so shaded timber stays readable under shaders, and uses cached collision shapes per section and orientation.

### Recipes

All are ten-second [Assembly Bench](manufacturing.md#assembly-bench) jobs. Structural blocks are extra; controller tooltips and guides give each shell's bill.

| Item | Ingredients / yield |
| --- | --- |
| Oil Derrick | 4 Iron Plates, 3 Copper Plates, 2 Machine Parts |
| Coal Engine | 3 Iron Plates, 2 Machine Parts |
| Fired Heater | 2 Iron Plates, 3 Copper Plates, 3 bricks, 1 Machine Parts |
| Distillation Column | 4 Iron Plates, 4 Copper Plates, 1 Machine Parts |
| Air-Cooled Condenser | 4 Iron Plates, 2 Copper Plates, 2 iron bars, 1 Machine Parts |
| Coal Drill | 5 Iron Plates, 2 Copper Plates, 1 iron pickaxe, 1 Machine Parts |
| Liquid Tank | 4 Iron Plates, 4 Copper Plates |
| Industrial Casing ×8 | 4 Steel Plates, 4 Copper Plates |
| Cooling Grille ×3 | 3 Industrial Casing, 6 Iron Fasteners |
| Refinery Flue ×4 | 4 Industrial Casing |
| Refinery Port ×2 | 4 Iron Plates, 1 Copper Plate |
| Fluid Pipe ×8 | 4 Iron Fasteners, 2 Copper Plates |
| Industrial Guardrail ×16 | 6 Copper Rods, 10 Iron Rods |
| Empty Canister | 5 Iron Plates |
| Hot-Bulb Engine | 4 Steel Plates, 2 Machine Parts, 2 Copper Plates, 4 Iron Plates |
| Coal Bunker | 4 Steel Plates, 1 Machine Parts |
| Cargo Tank | 4 Steel Plates, 1 Machine Parts, 2 Glass |

### Process and tuning

| Machine | Completed batch | Time at 20 TPS |
| --- | --- | --- |
| Oil Derrick | One block of well drilled, or one oil height step → 125 mB crude | 5 or 2.5 seconds at full power |
| Fired Heater | 250 mB crude → 250 mB heated crude | 5 seconds |
| Distillation Column | 1,000 mB heated crude → 800 mB distillate vapor + 200 mB lubricating oil + 1 Sulfur | 20 seconds |
| Air-Cooled Condenser | 200 mB vapor → 200 mB refined fuel | 5 seconds |
| Coal Drill | 80 mB refined fuel → one dense coal block removed → 16 Coal | 5 seconds lubricated; up to 25 seconds dry |

One Oil Derrick driven at full power is the throughput unit: **50 mB/s crude** feeds one heater and one column, then one condenser at **40 mB/s refined fuel**, plus **10 mB/s lubricant and one Sulfur per 20 seconds**. Two lubricated Coal Drills use 32 mB/s together, leaving 8 mB/s for engines. These are loaded rates with coal supplied and outputs clear; a Coal Engine halves the derrick's rate; startup fills batch buffers first and downstream flow pulses with completed batches. Crude lines reach the 50 mB/s pipe capacity. Empty search stretches, depleted or unloaded deposits, missing coal or backed-up coproducts interrupt supply, so keep lubricant and sulfur moving as well as fuel.

The Coal Engine and Heater burn Coal (250 mB pumped or heated per Coal drawn; [coal budget](machines.md#coal-budget)), after [warming up](machines.md#coal-fires) on about 2 and 11 Coal. Eight Coal drawn plus crude stock yields 800 mB fuel, 200 mB lubricant and one Sulfur; eight lubricated drill batches spend 640 mB of that fuel and 80 mB lubricant to extract 128 Coal from a separate finite seam. This is a geological bootstrap, not renewable energy. Charcoal, biomass and raw crude cannot replace the required fuels.

The products have distinct uses: **fuel powers machines, oil lubricates moving machinery, sulfur makes [fertilizer](machines.md#fertilizer-furnace)**. Heated crude and vapor are internal streams. Condensation is air-cooled: no water, pressure or electrical network. This is deliberately simplified petroleum processing: sulfur recovery and lubricant finishing are folded into the column, and ratios are game tuning rather than a chemical mass balance.

**Lubrication:** the Coal Drill starts fully lubricated. Each completed dry batch lowers its speed condition by one percentage point, down to 20%; it never breaks. Idle, unloaded or blocked time causes no wear. With at least 10 mB oil available, the next batch runs at full speed, consumes 10 mB and restores condition to 100%. No other refinery machine needs lubrication.

### Controls

Controllers show liquid gauges, progress, status and their slots in the [cabinet interface](machines.md#cabinet-interface). Feed coal to the Coal Engine and Heater, and casing plates to the Derrick, through their screens or a hopper above or beside; drill coal and column sulfur leave through a hopper below. The heater shows a warm inspection slit while processing and smokes from its actual flue. Completed batches and coal use are logged in the energy journal.

## Liquids and pipes

Every process buffer holds **4,000 mB**; a Liquid Tank holds **16,000 mB** of one registered industrial fluid and never mixes. No Survival item places crude.

Connect pipes between flanged ports, tanks and extractor connections; they turn corners and run vertically. Pipes are always open, with no valves. Each pipe segment has a shared **25 mB per ten-tick step (50 mB/s)** budget spent only by fluid actually delivered and shared by every route through it, across pushing, pulling, branches and fluid types. Unused capacity does not accumulate and is not saved. Direct adjacent-port operations move at most 25 mB per call. Tanks are passive buffers and do not push to each other.

Routing explores at most **128 pipes per network** and never loads chunks; an oversized network transfers nothing, so split long runs with buffer tanks. Loops cannot duplicate delivery. Endpoints are visited in deterministic order without fair branch scheduling. Pipes hold no fluid inventory; separate lines are recommended for legibility but not required. Clear inspection windows show confirmed transfers — brown crude, orange heated crude, pale vapor, amber refined fuel, olive lubricating oil — with moving highlights for direction, clearing 0.8 seconds after the last transfer; mixed traffic shows the latest fluid per arm.

Remote ports expose only their assigned controller buffer, on the outward face, while the controller and whole structure are present; a port cannot serve two vessels.

**Canisters** occupy one slot and move **exactly 1,000 mB** by right-clicking a controller or tank. Crude, refined fuel and lubricating oil have canisters; heated crude and vapor do not. An empty canister withdraws a portable output first, then lubricant, then input. The drill takes lubricant into its separate buffer. A wrong liquid or insufficient room or amount changes nothing. Canisters have no automated fluid-item capability.

**Claims:** every automatic transfer requires both ends unclaimed or owned by the same owner, and remote ports also check the controller-to-port boundary. Manual use honors owner and whitelist access. Extraction checks the actual coal or oil block: unclaimed resources are open, claimed resources need a controller claim with the same owner. Owning a controller does not reserve the deposit.

## Stops and limits

A missing structure, wrong site, depleted or unloaded deposit, protected resource, missing input, coal, casing or engine, or full output pauses processing. Blocked batches consume no deposit stock, draw no heat and reset partial progress; a lit fire keeps the machine hot. Work heat is drawn on completion. A coal block always yields 16 Coal and an oil step 125 mB; nothing is produced once the physical resource is gone.

Inventory, fluid buffers, lubrication, the fire and its stored heat, in-progress work, the well's depth, casing and strike, and the search cursor are saved in the block entity; depletion is saved as removed blocks and changed oil heights in ordinary chunks. Rebuilding starts a bounded search through what remains. **Breaking a controller drops its items but discards its fluids and heat — drain it first.** A tank item never carries liquid. There is no offline production, forced chunk loading or crash-atomic saving across player and resource chunks.

## Engines

An engine drives one machine within **eight blocks** of its controller; today that is the [Oil Derrick](#oil-well), which uses the nearest engine on its side of every claim boundary. Every engine supplies work the same way: each game tick it gives the machine up to the share of its full power that machine asks for, and burns its own fuel for exactly the work it gave. Engines differ only in fuel and power. The server accepts one request per engine per tick and checks range, loaded ends, structure and claims at that moment. It is not an electric grid or shaft network.

## Coal Engine

A small oilfield steam engine, **2 blocks wide, 3 long and 4 tall**: a vertical boiler with brass bands, gauge glass and safety valve stands on its own red-brick firebox, and a brass steam pipe feeds a horizontal single cylinder on oak sills whose crosshead and rod turn a big flywheel. It is the lower tier, before refined fuel.

**Construction.** The controller comes from the Assembly Bench ([recipes](#recipes)) and is the firebox at the engine's front corner; the engine extends one block to the controller's left as you face it and two behind. It is built in sections by right-clicking the controller with each section's material: **Firebox** 4 bricks, **Boiler** 6 Iron Plates, **Stack** 2 Iron Plates, **Engine bed** 6 planks, **Cylinder** 4 Iron Plates, **Running gear** 2 Machine Parts, **Flywheel** 6 Iron Rods and **Steam piping** 2 Copper Plates.

**Operation.** Coal goes into the firebox's two slots through the screen or a hopper, and the fire is lit and kept like every [coal fire](machines.md#coal-fires); rain does not reach it behind the firedoor. Its four bricks of masonry warm up on about 2 Coal. At working heat it gives up to **half power**, drawing from the firebox's stored heat exactly the work it gives: one Coal for every five full-power seconds, ten seconds of its own half power. The firedoor glows and the stack smokes while the fire burns; while it drives a machine the flywheel turns and the stack puffs with every stroke. It does not drive boats.

## Hot-Bulb Engine

A twin-cylinder vertical marine hot-bulb oil engine, **2 blocks wide, 4 long and 3 tall**, after period Bolinder-type engines: a two-rail bedplate, two cylinders on cast crankcase columns crowned by drum-shaped hot-bulb heads with lamp ports on cast bosses, a silencer drum and stack on a cast standard, a dished flywheel at the back and the clutch drum, handwheel, starting lever, sight-feed lubricator and gauge at the front. It drives [boats](vehicle-physics.md#boats) and, at full power, an [Oil Derrick](#oil-well).

**Construction.** The controller comes from the Assembly Bench ([recipes](#recipes)) and is the engine's front corner on its control side; the engine extends one block to the controller's left as you face it and three behind. Like the [other modelled machines](machines.md#building-with-a-controller), it is built in sections by right-clicking the controller with each section's material, any amount at a time: **Bedplate** 8 Industrial Casing, **Crankcase** 8 Iron Plates, **Cylinders** 6 Steel Plates, **Running gear** 4 Machine Parts, **Flywheel** 8 Steel Rods, **Exhaust** 4 Iron Plates and **Controls** 2 Copper Plates. Breaking any part of a section removes that section and refunds what was paid for it.

**Supply.** The controller front accepts refined fuel and its underside lubricating oil (route the lubricant pipe beneath the bed); capacities are **4,000 mB fuel / 2,000 mB oil**. A filled canister inserts 1,000 mB; an empty canister withdraws fuel, or oil when sneaking, and needs a whole 1,000 mB. Crude and wrong liquids are rejected. The screen has named gauges and **Start/Stop**.

**Operation.** Start spends **10 mB fuel once** and heats the bulbs for **5 loaded seconds**; with less than 10 mB it waits without spending. While they heat, both lamp ports glow and a blowlamp roars at them. Then the engine kicks its flywheel over in a burst of black smoke and runs until it is stopped or runs dry: the lamps go dark and the roar fades as it catches, the flywheel turns, and the stack knocks twice a turn over the clatter of the running gear, each knock puffing smoke that rises in a drifting column. Running with nothing to drive it idles on **0.2 mB fuel/s** and no oil; at full load it uses **2 mB fuel/s and 0.1 mB oil/s**. Dry running gradually reduces delivered power to 20%; supplied lubricant restores it. Stop shuts it down, its knocks spreading apart as the flywheel coasts to rest, and a later cold start costs fuel again.

**State and limits.** Tanks, fractional work credit, lubrication, enabled state and completed warm-up are saved. No offline work or forced chunks. A missing section stops the engine and requires reheating. The server accepts at most one work request per engine per game tick and checks range, loaded endpoints, structure and claims; physics substeps never spend fuel independently. Breaking the controller returns it empty: **stored fluids and work credit are lost**, so drain it first.

## Bulk freight storage

Two controller-backed land stores share the casing construction guide and cabinet screen. Both fit a **3 wide × 5 deep × 3 high** envelope with the controller at bottom front center, built from Industrial Casing and its saw-cut forms; the tank also needs one glass window. Storing cargo needs no fuel.

| Store | Capacity | Manual access | Automation |
| --- | --- | --- | --- |
| Coal Bunker | 8,192 Coal | Right-click with Coal; shift-click Coal from its screen; Store 32 / Take 32 | Hoppers at the controller at ordinary hopper cadence |
| Cargo Tank | 64,000 mB of crude oil, refined fuel **or** lubricating oil | Right-click with a canister, or **Use canister** in the screen with one in the selected hotbar slot; exactly 1,000 mB per exchange | Pipes at the controller: automatic intake from the front, capability access on other exposed faces |

The bunker shows a rising coal pile and the tank's window its fill height and fluid color; the screen gives exact stock. Empty the tank before changing liquid; water and intermediate streams are rejected. Hopper adapters expose bounded views of one stock balance, and pipes keep their 50 mB/s budget.

An incomplete or unloaded shell stops all transfers but keeps stock. Mining a stocked controller is refused, even in Creative; forced removal or explosion destroys its contents. Menus recheck distance and claim access, and automation rechecks the claim boundary on each transfer. Both stores can ride aboard boats.

## Save compatibility

- **Retrofit oil in New Surv.** A world-root `civilization-retrofit-oil.flag` opts a saved world's already generated chunks into a one-time oil backfill as they load. It uses the same deposit sites as new terrain, adds the seep and source at an unobstructed natural surface, and replaces only natural underground rock with oil or the reservoir cavity, leaving ores, tunnels and construction (player-placed plain stone may be replaced). Each processed chunk saves a completion marker, so later visits and extraction never refill it. Only the New Surv save carries the flag; other old worlds gain no physical oil.
- **Generated sites.** Deposit placement belongs to the world generator, so terrain generated before a placement change keeps its old bodies while prospecting and extractor lookup follow the current rules. Keep generator and deposit rules fixed within a world; evaluate changes in a fresh world.
- **Authored deposits.** A map may define deposit geometry in vanilla command storage `civilization:authored_deposits`: a list per dimension ID of up to 64 compounds with `x`, `y`, `z`, `kind` (0 coal, 1 oil) and `radius` (8–32). Authored sites take precedence where they contain a position; they define where machines search, never quantities — stock is still the actual blocks. Ordinary worlds have no entries.
