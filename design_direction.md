# Civilization — game rules

The agreed rules and setting, one section per system. "Agreed" means settled, not necessarily built: [development_plan.md](development_plan.md) says what is deployed and what comes next, and the system docs in `civilization-mod/` own shipped behaviour and tuning values. Each section ends with its unresolved questions; an Open item stays a proposal until the owner decides it.

## Vision

**Energy → surplus → capability → dependency → power → status.** Status is the social shadow of real power. Players start with personal labour and expand the matter, territory and energy they command, build productive places, control resources and routes, and spend surplus on extraordinary institutions. Hierarchy, exclusion and concentrated power are intended outcomes; status emerges from what people build, supply and control, never from a prestige score or assigned class.

- Solo play and trusted groups of typically 2–8 (not a cap) are supported. Large institutions can emerge, but farming, building, exploring and trading stay worthwhile lives.
- No formal jurisdictions. Trade rivalry and politics are central; PvP is optional — sieges, the Gladiator Pit or the wearable PvP flag — never unrestricted raiding.
- The design target is up to 200 concurrent players, maintained by one human with assistant support. It is a goal, not verified capacity.
- Gameplay is mostly custom Java; vanilla systems may be reworked or removed when the result is simpler and more intuitive.
- Immersion matters in every activity: atmosphere, physical interaction and understandable consequences are part of system design, not polish. This alone approves no new meters, minigames, slower actions or simulations.
- Out of scope for now: global power networks, elaborate contracts, NPC labour, detailed ecology or chemistry and universal wear.

**Joining.** A client needs a compatible Civilization network protocol, not an identical build; server-only generation, balance and settings changes never force a client update. The protocol version changes only when packets or synchronised content become incompatible, and missing or incompatible installations are rejected at the mod handshake. This is compatibility, not client authentication; client-only graphics mods may differ. The client ships as a Prism instance with a pre-launch updater ([modpack/README.md](modpack/README.md)).

**Open**
- Setting statement, world name and tone.
- Hosting budget and active chunk, entity and machine limits before any capacity promise.
- Deferred ambitions: research progression, provenance, detailed wear, NPC labour, banking, general contracts.
- Immersion treatment (proposal): world cues first, exact numbers on inspection; a small shared sound and material vocabulary tied to real state; working, stopped and complete states readable without menus; prices, deadlines and protection never hidden for atmosphere.

## Setting and material style

The reference is **Nikola Tesla's lifetime (1856–1943)**: a late-nineteenth-century industrial foundation developing toward monumental electrical modernism, as one coherent alternate history. The fantastic is Tesla's physics made real — wireless power, resonance, radiant energy, antigravity — built from the same materials as everything else: wonder shows in what machines do, never in ornament, runes or magic. Nuclear technology and sky machinery keep physical materials and legible instruments.

| World layer | Material and form language |
| --- | --- |
| Farms and frontier | American Western: sun-weathered timber, saddle leather, wool, ceramics, rough masonry, hand-forged iron; practical, repairable, visibly hand-assembled |
| Town industry | Black steel, brick, enamel, restrained brass; substantial mechanisms and readable physical controls |
| Advanced infrastructure | Black steel frames, porcelain insulators, copper conductors, coils, glass instruments; precise functional connections |
| Sky civilization | Pale stone and metal, ivory and turquoise accents, monumental symmetry, restrained Art Deco; enormous electrical apparatus |

These are visual stages, not resource tiers. Precision, scale and command over energy connect them; wealth reads through craftsmanship, finish and infrastructure, not gold trim beyond livery pinstripes. Beautiful ground settlements stay possible at every stage.

- **Black steel** is the signature industrial material: blackened iron and steel in charcoal with a faint cool cast, never flat black. Its edges wear to bright, scratched steel only where hands and tools touch, and rust appears only through neglect. Iron stays bare, forged gray so it always reads apart from steel.
- Brass for bearings, fittings, instruments and contacts — things touched or read; copper where heat or electricity explains it; porcelain for insulation; blue-white light only for electrical energy. No decorative gears, lightning emblems or generic steampunk clutter. Raw materials look like their substances.
- Textures are dark, clean pixel art: deep values, large deliberate colour clusters, few tones per material and worn-edge highlights that articulate dark metal.
- **Livery** is the signature, after nineteenth-century sewing machines, locomotives and safes. Finished machines and devices are japanned black steel with a fine pale-gold pinstripe along their main panel edges and at most one small painted decal without lettering. Frontier goods carry simple geometric brands and occasional painted colour, while wheels stay natural timber and iron; sacks and crates carry one stencilled mark. Raw materials, metal stock, plain panels, sides and tops stay undecorated.
- New textures take their style from the approved [board](style_references/board.png), [materials](style_references/materials.png), [items](style_references/items.png) and [machine faces](style_references/machines.png). The machine sheet's warm specks along black steel frame edges and its spool-like coil block are defects, not style.
- Material behaviour stays distinct — wool weave, timber grain, fractured minerals, cast-metal wear — never one generic surface.
- New block and item art is **64 pixels per block face**, starting with Street Pavers. Existing 32px families stay valid until migrated family by family; Faithful 32x may supply vanilla materials without being copied into the mod.
- Models are editable Blockbench sources, animated with GeckoLib 4 where they move. Production follows the `civilization-art` skill and [tools/modeling/README.md](tools/modeling/README.md).
- The first firearm, the Colt Paterson (1836), is an explicit earlier addition to the setting.
- Grassier Grass (optional client mod) never whitens blades from cold or altitude alone. Blades look like taller wild grass with restrained height variation. Cumulative footsteps permanently shorten blades along travelled routes, shared and saved with the world; standing, flying and teleporting add no wear. Visual only.

## People: calories, movement and recovery

- One calorie reserve replaces hunger, saturation and exhaustion. Work, movement, healing and sleep have explicit costs; cut-piece placement scales with volume, and sleep pays for elapsed in-game time. No idle or offline drain; cold adds a bounded passive cost while an active Survival player is online.
- On level ground, walking is 3.2 blocks/s and sprinting 4.6, with jumps scaled to match; sprint-jumping gives about 20% over sprinting. Sprinting costs three times walking per block, at a constant rate per gait. Ascent costs 1 kcal per vertical block, including the rise of a jump, which has no charge of its own; there is no descent refund.
- Rowing a vanilla boat costs less per block than sprinting but is a real effort: sustained paddling at twice walking's rate per block, and a boat floating up a river step or rapid pays ascent like any climb. Only the paddling player pays.
- **Street Pavers** — gray, muted red and brown long-paver brick, distinct from structural bricks — give 25% faster ground movement, including their slabs, stairs and cut pieces; ordinary bricks do not. No stored buff, airborne boost or calorie change.
- At depletion, walking and basic actions work; mining slows, sprinting and natural healing stop. Starvation alone has a health floor. Hand-foraging is a slow recovery route. Death and relog never refill calories.
- Death leaves everything carried in an owner-only recovery container near the death location, persisting through restarts. Respawn keeps calorie state.
- The full day/night cycle is 60 minutes with vanilla proportions; the tick rate is unchanged.

**Open**
- Labor and food in real energy: labor is game-scaled (assembly's 5 kcal/s is about 21 kW, far above real human output), while food comes fast, since a whole cow is about 1,000 MJ in two seconds. Real metabolic labor would need a slower food supply.
- Spawn, starting items and subsistence: a fresh solo player must reach food, tools and claim inputs without existing buyers.
- Death recovery in hazardous or inaccessible places, repeated deaths, XP and respawn health.

## Food and cooking

- Farming and uncooked food sustain a person without industrial fuel; river crops supply staple calories. Fishing and gathering add subsistence and variety while purchased provisions stay useful.
- Livestock kills yield one meat portion, with no Looting bonus.
- Raw meat and fish give about half their stove-cooked value or less, so the full starting reserve plus hunting, roasting and foraging carries a new player to a Cooking Stove without starving, while farming and cooking stay clearly better than foraging.
- Future husbandry uses farm feed at breeding and production, with no animal-hunger simulation or livestock region.
- Household cooking uses one coal-fired station; a campfire also roasts raw food, three times slower and for half the calories the stove adds. Cooking raises food value; packed provisions conserve calories; food tiers express convenience, density and luxury on one calorie scale.
- **Tier 2 cooking** grows into a kitchen workshop of distinct multiblocks. Mastery is the player learning — no cooking XP — and pays off in higher-calorie meals and stronger work bonuses. Chopping is not a mastery mechanic.
- Mastery means reading colour and sound cues that change continuously, with a forgiving optimal plateau. A frying sound runs under every active phase, with distinct textures for the plateau and overcooking; loudness follows pan heat.
- Heat is a continuous physical dial, no presets. Stronger heat cooks faster and narrows the optimal window.
- The first station is a dedicated stovetop with loading and serving in its interface; standing beside it is warmer than comfort but far below furnace heat. Cookware can rest on a countertop and return with its batch intact; already-paid heat keeps cooking gradually; handling awards no quality.
- The baking oven is a nineteenth-century professional oven: cast-iron exterior, refractory brick inside, a real opening door, a draft dial, a 2×2×2 body and a flue above.

**Open**
- The rest of the kitchen: stations and dimensions, Survival construction, recipes, batches, specialties (mining speed is a candidate), packaging, eating at full reserve. Oven fuel, heat, trays and baking; the deferred preparation table.
- Station proposals: preparation bench, cooking range (browning then simmering), oven (preheat, trays, a generous browned window), portioning bench (only if it earns a role); freely arranged, no room detection. Cues should work by colour or sound alone; no hidden quality rolls or perfect-state lamp. Recipe sets specialties; batch quality sets calories and bonus strength, capped by ingredient energy.
- Husbandry products, domestic versus wild yields, feed-supported wool and hides, and any animal role in fertilizer.
- Cheap foods such as berries must not undercut farmland.
- **Proposal: spoiling and salting.** Raw meat and fish carry a spoil time, an absolute world game time stamped on the stack when it drops (about two in-game days); merged stacks keep the earlier time, so merging never extends life. Past it the stack becomes Spoiled Meat, a poor emergency food. Salting at a crafting table, raw meat plus Salt, makes Salted Meat that never spoils and cooks like the raw cut; Salt would come from boiling seawater in a pan over a fire or from a rock-salt deposit. The stack owns its deadline, so chests, stores, trade counters and death recovery carry it with no per-container code, and spoilage is settled only when a stack is shown, eaten, cooked, merged or moved: no ticking containers, world scans or loading. To decide: lifetimes, whether cooked food and crops spoil, the salt source, and whether cold air slows spoiling.

## Farming and weather

- **Eligibility.** Farmland follows the local natural river elevation, including high valleys, within a bounded reach of the water surface. Sea level is not a ceiling; sky platforms gain nothing from being above a river; placed water creates neither eligibility nor the rain bonus.
- **Growth.** Soil holds the water: river soil gives 50% growth, stored rainwater 100%. Full-speed planting to harvest averages two hours (four at baseline), normalised by growth stage, without vanilla moisture or row multipliers.
- **Rain reserve.** 30 minutes of open-sky rain charges a farmland block's reserve to 120 minutes; it grows at full speed and recharges while rain falls, then drains a minute per minute. Roofs block rain; the crop does not. No lateral spread or pipe irrigation. Breaking soil cannot refill it; replanting keeps it.
- Governed crops: wheat, carrots, potatoes, beetroot, melon and pumpkin stems, torchflowers and pitcher plants. Each later melon or pumpkin needs the full production interval. Sweet berry bushes ripen on the same clock and water reserve wherever they grow, a full interval per picking, with their native picking yield and no fertilizer.
- **Fertilizer** raises yield, not speed: one non-stacking dose per cycle tripling harvest output after replanting material (wheat keeps its 1→3). Fortune and alternative paths cannot bypass yields. Mineral Fertilizer comes only from refinery sulfur plus gravel. Bone meal is disabled.
- Trees grow anywhere suitable, substantially faster in woodland. Every other growable needs its own habitat policy rather than inheriting wheat rules.
- **Basic felling.** Cutting a log of a natural tree drops every log of that tree at and above the cut, each as if broken by hand; the stump stays and leaves decay. Log buildings never fall. It answers the strongest playtest complaint, chopping one log at a time. The cut takes longer for each log it will fell, a quarter of a log's breaking time per log, so a big tree is real work yet far quicker than chopping it log by log.
- **Regional weather.** Weather runs in fixed 512×512-block districts — atmosphere, not claims — through one authoritative server query that natural rain, the Rain Caller and later towers all feed. Overworld only. Sleeping changes time, not weather.
- Natural rain lasts 25–35 minutes. Clear spells end with a constant chance over time (mean 12 real hours) and no minimum or maximum, so back-to-back storms and multi-day droughts both happen. Rain only: no lightning, wind, floods or destructive storms; snow is not irrigation.
- Crops and soil advance only while loaded; district schedules advance with server time; no offline catch-up or forced loading.
- The **Rain Caller** is a creative/admin prototype with no Survival recipe, since a pocket item would bypass the towers' energy investment. It starts a 30-minute rain in the user's district, or reports that it is already raining. Rain benefits the whole district, with no rain ownership. Tower service will charge fuel only for rain it actually starts or sustains.
- Wet soil darkens; hoe inspection reports suitability, growth rate and stored water; failed planting explains why. Photon is the primary shader target.

**Open**
- Habitat policy for cocoa, sugar cane, bamboo, cactus, kelp, mushrooms, vines, Nether wart and spreaders.
- Meltwater and cold-region farming.
- Advanced felling and timber hauling, to be designed later; basic felling stands in until then.
- The Rain Caller's eventual form; tower energy and Survival access; offline production.
- Weather fronts that carry rain and cloud across districts.

## Land, deposits and geography

Terrain makes opportunity understandable. One shared region/biome foundation drives growth, spawning and deposits; regions overlap.

- Livestock needs suitable sites and feed, not a special region. Crossings and junctions attract towns because supplies meet there, not through a city bonus.
- The dedicated server uses [the owner's Terrain Diffusion fork](https://github.com/stephenmccranie/terra-diffusion) at **world scale 4**. Temperature regions stay coherent but small enough that cold and hot country usually lie a few thousand blocks apart. The land's large shape rises to use the world's height while local slopes stay as buildable as the model made them and ridges and summits are steepened; climate follows the model's real heights, while trees and snow follow the air of the raised ground: forest stops at a real tree line, and the treeless ground above it carries snow only where the snow outlasts the summer. Rivers are fewer and sized by the water their climate supplies: a river is as many blocks wide as a real river carrying that water is metres. It is the evaluation baseline, not proof that river farming or freight works.
- Food, timber and biological materials are renewable; metals, industrial fuels and strategic minerals are finite. Renewable metal and fuel sources are removed from loot and trading. Building materials stay plentiful. Automation may process finite stock but never replenish it.
- **Climate bands** from the natural surface biome: coal never in hot country and twice as frequent in cold as temperate; oil never in cold country and twice as frequent in hot as temperate (halved from the first implementation); uranium equally rare everywhere. Climate changes frequency, not size. A town catchment of about 2,048 blocks' radius holds one or two cold-country coal opportunities and half that for hot-country oil; temperate country half of each. Towns may lack a resource and need trade. Sites use physical host-rock and cover checks, not flatness.
- **Physical deposits.** Mining removes real coal and pumping lowers real oil. An oil seep above the centre reveals it but holds no reserve. Coal has no clue block: river cuts, cliffs and caves expose some seams. Derricks may stand anywhere above the oil footprint.
- Ordinary small coal stays hand-mineable for usable Coal; huge veins use [manual mining](#manual-coal-mining).
- **Uranium** is rare, small, hand-mined ore scattered through suitable frontier rock. Explorers wear a Geiger counter that clicks faster nearby and carry finds personally; value comes after processing. Exposed ore, uranium in hand and chests holding it show sparse silver radiation tracks — a visual cue only.
- Three accessory slots beside the player portrait hold wearable role tools and one backpack, never a power ladder.
- Deposit geography is developed against fresh worlds; existing natural sites need no migration during development.

**Open**
- Navigable river width, depth and continuity, local river elevations and permanent-world generation policy.
- Realised stock, sites per climate, generation cost and depletion in representative worlds.
- How fresh uranium frontier stays available.
- World lifespan: reserve horizon and expansion or reset policy before a permanent world, since nothing replenishes finite stock.

## Energy and industry

| Tier | Energy and access | Capability |
| --- | --- | --- |
| **1 — Coal** | Hand-accessible Coal | First machinery, kiln, fertilizer, cooking; first river freight |
| **2 — Oil** | Rare large deposits needing extraction and refining | Larger industry and transport |
| **3 — Uranium** | Small frontier finds, fuel processing, reactors | Major installations, regional instruments, sky civilization |

Calories support people throughout and are not a tier. Lower-tier goods and fuels stay useful; oil stays useful for mobile work after reactors.

- **Energy is measured in joules.** Fuel, food, stored heat and machine work share one unit (kJ, MJ, GJ; power in kW), with real-world values: coal 25 MJ/kg, refined fuel about 43 MJ/kg, crude about 42 MJ/kg, and food its real energy, which players read in kcal (1 kcal is 4.184 kJ). A **Coal** item is a 10 kg scuttle of 250 MJ; a dense coal block prepares into 16 Coal, the 160 kg of fuel-grade coal screened from its rock partings and dust. A common unit measures and compares; it never converts one fuel into another.
- No renewable industrial power or biomass fuel; food, cranks and animals never become industrial energy. Conversions have explicit inputs and losses. Uranium fuels reactors, not furnaces. No universal fuel conversion or fixed exchange rate.
- Coal is the heat fuel; oil the mobile mechanical fuel and source of lubricant and sulfur. Per mass, petroleum should eventually hold only moderately more energy than coal.
- Each stage builds the next from what is already available, with no circular recipes: ordinary coal starts industry, coal opens oil, oil processes hand-found uranium.
- **One Coal** serves crafting, fuel and claim reserves; charcoal, renewable coal and coal blocks are removed. Kilns fire nonmetals, Foundries metals and ore, stoves food; vanilla furnaces, blast furnaces and smokers retire, and no controller needs one.
- **Fired machines store heat.** Every coal machine is lit by hand with a paced, random-chance flint-and-steel button (each accepted strike costs a little labour), then stokes itself from two fuel slots. The fire heats the machine's masonry first: warming a cold machine is the big cost, about one dense block of Coal for a Foundry and less for smaller machines, over about two minutes. Work needs working heat and draws a little per job; a hot machine leaks slowly into the air around it, so unfed it stays hot for about twenty minutes, longer when sheltered and walled, shorter in cold or rain. Heat once and work a lot: a busy shared furnace pays one warm-up.
- Only an open fire minds rain: the Smithy's forge cannot ignite or work while rain reaches its work face, and a roof shelters it. Fires behind a door, and machines without a fire, work in the rain.
- **Engines drive oil wells.** An Oil Derrick is one machine that an engine within eight blocks drives through its band wheel: first its walking beam drills one well straight down at the derrick's centre, then the same beam pumps it. Drilling takes five seconds of full-power work per block, and every four blocks of depth line the hole with one Iron Plate of casing; the bit passes caves and liquids and stops at bedrock or protected ground. Once it strikes the oil body the derrick pumps crude at 50 mB/s at full power, from the same physical oil steps. Every engine supplies work the same way and differs only in fuel and power: the **Coal Engine**, a small steam engine with its own firebox and a lower tier than oil, burns Coal through the coal fire at half power; the **Hot-Bulb Engine** burns refined fuel at full power and also drives boats. An engine left running with nothing to drive still burns a little fuel.
- **Refining.** Coal powers the first engine and heater. Heater, distillation and condenser multiblocks connect through basic pipes, balanced one-to-one with an extractor, producing refined fuel, lubricating oil and sulfur. One extractor fills a basic pipe; branching never multiplies capacity. Lubrication wears with work, not idle time; running dry gradually cuts efficiency. Pipes have no valves; a sight glass on every fourth block of a straight run shows the moving fluid.
- Refined fuel drives transport and machinery and is not needed for coal. **No oil dependency may block building the first oil chain.**
- The bucket is a cooper's wooden pail, so water work such as tanning needs no metal; lava would burn it.
- **Equipment needs production**: stone tools and Coal → kiln bricks → a brick, coal-fired Foundry buildable without metal → ingots → steel and Machine Parts → T2 machinery. The material vocabulary stays short.

**Open**
- Oil and uranium processing beyond the refinery; uranium fuel packaging and yield; oil freight quantities.
- The remaining vanilla boundary: resource farms, special equipment and loot, enchanting, food shortcuts, mobs, redstone, automation, and storage or travel that bypasses freight; stonecutter overlap with saws.
- Coal and oil balance rebuilt from mining, hauling and transport rather than the retired drill ratio. With real energy per item, what goods cost is set by how fast coal and oil are mined, drilled and hauled: a dense block yields 4 GJ, so mining and drilling rates per block need rethinking alongside heat-and-hammer refining.
- How powers are shown while the day runs 24 times faster than real time, so real energies per job meet compressed hours.

## Manual coal mining

Manual mining unlocks huge coal veins and replaces the Coal Drill: special tools → chip the vein → shovel loose raw coal → minecart → surface preparation → usable Coal. One raw-coal material; no coke or coal grades.

- Huge veins yield more usable Coal per block than ordinary deposits.
- A special pick chips away visible portions: blocks show real local removal, and loosened chunks fall to supported ground. Worked faces never repel players invisibly or hold floating piles.
- Clicking commits a swing; holding repeats after full recovery. Camera movement shapes the stroke, but looking around never strikes. The pick feels heavy and somewhat detached from the camera: a close overhead wind-up, the head following the view with weight, and a forward-downward strike in one plane — never a dipped head or sideways spin.
- A special shovel loads minecarts that are hauled to the preparation machine.
- The full Survival loop must work before the drill retires.

**Open**
- Per-block yield, extraction time, labour and route throughput, tuned together.
- Further controls, fragment geometry, missed-swing costs; shovel custody (a loaded shovel destroyed as a dropped item loses its load) and scoop size; cart capacity, uphill haulage and cross-claim access.
- Preparation power (coal or hand; never mandatory oil), throughput and waste.
- Keeping Fortune, explosions, pistons, hoppers and Builder's Line from bypassing extraction; drill legacy IDs and saves.
- Not agreed: cave-ins, gas, ventilation, mandatory supports, elevators, locomotives, conveyors.

## Workshops, parts and equipment

Towns concentrate production, customers and services: rural farms, mines and forests supply inputs, and town workshops turn them into goods sold back, later into ships and sky infrastructure.

| Workplace | Input → output |
| --- | --- |
| Foundry | Ore + Coal → metals and steel; refining |
| Tannery | Raw Hide + Tanbark → Leather, steeped in a water bark pit by time alone; brick, stone and wood, no iron or fuel |
| Textile Workshop | Wool → Cloth, woven by hand at a timber loom for labour; no fuel |
| Smithy | Metals → forged heads, Plates, Rods, Fasteners |
| Assembly Bench | Parts, timber, Leather, Cloth → finished goods and repairs |

- **Cloth is wool only** — no plant fibres.
- Heat and machines transform materials; benches and building sites join parts. Coal belongs in smelting and forging, never in fitting a handle. Intermediates stay few and recognisable; workshops make a selected product without skill grinds or minigames.
- **Shared parts.** Ingredients that always travel together become one part: the Tool Handle (wood, leather grip, binding) serves pickaxe, axe and shovel. Core parts: Tool Handles, Fasteners, Plates, Rods, forged heads and Wheels, alongside Leather, Cloth and Machine Parts. Each part needs several customers; most recipes use two to four ingredient types.
- The **Assembly Bench** is a timber-only two-block bench with one inventory. Assembly costs 5 kcal per working second; idle costs nothing.
- Large structures are assembled on site through construction guides, never crafted into portable items.
- **Forging cost.** Metal parts scale duration and total coal by material — iron 1×, gold 2×, diamond 4×, netherite 8× — at a constant coal rate per second. Assembly and repairs use labour, not Coal, with the same duration multiplier.
- Gold is a real tier between iron and diamond, keeping its enchantability.
- Coal-era workshops make iron equipment and everything for the first oil chain; later powered works use fuel and lubricant for throughput. Lubricant services machines; it is never a recipe ingredient.
- **Density** comes from shared landings and warehouses, short transfers and concentrated customers — no city designation, proximity buff or forced residence.
- **Grade.** Grade belongs to each unit of metal. Smelting rolls every unit of rough metal, a 10 kg bar's worth, a permanent grade, to a tenth of a percent, from a normal distribution centred on 75% (10-point deviation, no limits); each hammering always raises it, by a random step that shrinks as grade rises. Steel keeps its iron's grade, and diamonds are cut rather than hammered. Parts take the mean grade of the metal they use and equipment the mean of its metal parts. Gear without graded material is standard 75%. Stacks mix grades, show their average and keep each unit's grade, handing out their best units first. Storage blocks and nuggets are one-way: a placed block keeps no grade, so they never craft back into ingots or diamonds. Grade scales equipment's base stats; 100% equals vanilla. Grade is the single permanent wear measure: use never drains it, repairs lower it. The durability bar shows remaining, spent, grade-lost and exceptional capacity; a persistent wear pattern appears below 100%, and 100%+ items get an automatic trim (Smithing Table trims are disabled).
- **Repairs** at the Assembly Bench consume materials and lower grade, keeping names and enchantments, with no XP or prior-work penalty. No idle decay.
- Mending and villager equipment trades are disabled. Completed production grants modest XP; hand assembly and repairs grant none. Crafting, repair and other acquisition routes may not bypass workshop production, though finite exploration finds stay usable.
- **Maker's marks** (approved, deferred): colour accents and player-drawn pixel signatures on equipment.

- **Smelt, hammer and quench.** Every metal is made hot and finished by hand. The Foundry smelts ore into rough metal, which iron never melts into but copper and gold are cast as: **Iron Bloom**, **Blister Copper** and **Gold Doré**; cemented iron ingots become **Blister Steel**. Metal stays at full heat while it sits in a hot Foundry and cools over about a minute after leaving it, wherever it is; its glow, from white-yellow through orange, bright cherry red and dull red to nothing, is the only gauge of its temperature, on the item wherever it is shown. A few slow, heavy blows at an anvil beat a hot stack of rough metal into ingots, and hammering a hot ingot refines it; every hammering is the player's own labor, paid for each unit, and raises each unit one random grade step. Water quenches hot metal at once. The ideal moment is bright cherry red, the same for every metal and learnt by eye or by counting from the fire; hammering does not change how fast metal cools, and each heat takes one hammering. Mastery pays: quenching freshly hammered metal within a second of it adds up to another hammering's step to every unit, roughly halving the heats to a high grade; a forgiving few seconds either side neither help nor harm, and quenching further off costs every unit up to 10 grade points along a bell curve, and metal left to cool in air loses the full 10. Each quench says, to a tenth of a second, how early or late it was. Cold metal goes back into the Foundry, which heats a whole load for the same coal, so full loads are cheapest. The first anvil is a **Stone Anvil**, a heavy anvil stone seated on a log stump, as early smiths worked blooms on stone; it takes more blows than the iron anvil that its first ingots make. Diamonds burn rather than forge, so they are cut at a grindstone with the same slow labor and no heat. High grades cost a great deal of both coal and food, so mines and kitchens both serve metalworkers, and a fire tender and a hammerer can work together.

**Open**
- Whether the Smithy should also repair equipment, which players look for there; today only the Assembly Bench repairs.
- Smelt-and-hammer tuning: coal per load, load size, blows per anvil and kcal per blow, cooling time, and whether hammering wears the anvil.
- A powered hammer that takes metal past the Foundry without hand labor; until one exists, hot metal leaves the Foundry only by hand.
- Gates for ranged and special equipment, horse armour and tools; renewable treasure and other metal sources.
- Where coal smithing ends and oil-powered manufacturing begins.
- Head and handle damage or disassembly; separate woodworking or assembly sites at scale; further product families.
- Non-numeric grade effects and nonpositive grades.
- Grade in trade offers.
- Maker's mark details (a 6×6 grid is a candidate).

## Construction

- Construction respects volume: **one log → one plank block**. Saws cut halves, quarter beams and eighth cubes on a 2×2×2 grid, and are the only source of slabs. Pieces place freely, cost labour by volume and recombine without loss.
- **Multiblocks.** Machines are native Blockbench models, each a new design, built from their controller in sections: holding a controller shows the whole machine where a right-click would put it, and a placed one shows the missing sections while inspected. Right-clicking the machine with material in either hand, including the offhand that carries building material, pays toward the sections in order, any amount at a time, and a section is built once paid in full; each section consumes real items and labour, needs clear, accessible space and is refunded once when broken, so damage stays local and repairable. Guides show remaining materials and obstructions, and stay cheap to render. Machines not yet redrawn keep block structures built from full blocks and saw cuts, fed through the same controller or placed by hand.
- Placed crafting tables keep a shared nine-slot grid that persists when closed, so players can fetch more and cooperate.
- **Builder's Line** is a wooden mason's cord reel. With blocks in the other hand it builds; with a tool, it mines. Two right-clicks mark endpoints along one world axis and immediately work the whole line (up to 16 full blocks, four per second, 16-block reach); crouch-right-click cancels. Normal costs and wear apply, and work stops at the first obstruction. It comes from the Smithy.

**Open**
- Builder's Line length and pace in play, a powered speed boost, shaped pieces, functional blocks and vessels.

## Storage, carrying and hauling

Progression expands the quantities, distances and areas a player handles: personal work → assisted handling → powered work → regional industry → extraordinary engineering. Each step enables bigger projects while small jobs stay convenient; a first home still fits in an ordinary session.

- Material volume, storage volume and mass are distinct and separate from energy value. A stone block is 1 m³; its cut pieces preserve it.
- **One packing density.** Carried and stored building goods pack 64 to a block of space, so every capacity is the carrier's real interior at that density: a hand holds an armful of 8, a wheelbarrow 64 and a pallet 128 in a stack two blocks high.
- **Cargo stores.** Every store holds a fixed volume of goods, drawn one for one as quarter-size blocks, with unlimited-count matching stacks. Stores take every block the hand carries: pieces by volume, any other block as one block of space, and right-click puts held goods onto a store. A wheelbarrow's load heaps above its tray rim. A pallet's stack fills its block, then rises into the block above while that block is empty. Pallets touching horizontally share one menu and summed capacity; each keeps its own stack, filled one pallet at a time and sorted by item.
- **Personal carrying.** Building material travels as one offhand stack of up to 8 blocks or cut pieces, and buckets, which do not stack, one at a time, or in a worn backpack; matching pickups fill it. Tools, seeds, crops, small placeables and empty pallets use normal inventory. Old extra stock is recovered, never deleted.
- **Backpack.** An early leather pack worn in an accessory slot adds the same room on every pack, ungraded: 8 slots and 24 building blocks. It comes off only empty.
- A chest or barrel holds at most **8 building blocks in total**, one armful, the same goods the hand carries; tools, food and components keep 32-item stacks, and building stock belongs in stockpiles and cargo spaces. A chest already over the limit keeps everything and can be emptied but not added to. Containers must be empty to carry; ender storage is disabled.
- The **wheelbarrow** is pushed over the ground at walking or sprinting speed, paying handling calories and climbing one-block rises, carrying eight hands: half a pallet. The coal minecart keeps its separate rail role.
- **Supply-fed building.** Placement draws only from explicitly selected nearby supplies: a backpack bound to one wheelbarrow or Pallet group keeps the hand, and the block stacks already in the pack, topped up from it while the wearer builds within 16 blocks. It never adds new kinds of block. A bound pack supplies until it is unbound; the binding is shown beside the hotbar and checks access at every pull; out of reach, the pack alone refills the hand.
- **Menus.** Every menu has one column on its right, in the same place: the Hand, the worn pack and the stores within reach, whose goods can be taken and stacked from there. Crafting, by recipe book or JEI, draws on all of them.
- New limits follow an audit and explicit migration; inventories are never shrunk or discarded.

**Open**
- Wheelbarrow acquisition and loaded handling.
- Supply-fed building in play: the 16-block reach, competing builders on one store and backpack size.
- Universal enforcement across containers, machines, hoppers and recipes; unknown goods are never weightless.
- The first powered loading upgrade; hoists, cranes and rail are examples only.
- Journey times (proposal): everyday supplies minutes away, early resource trips 5–10 minutes, regional freight 10–20 minutes.
- Fuel value versus pocket freight: stack limits, payloads and consumption tuned together.

## Heat, shelter and comfort

One environmental temperature field connects buildings, machines and people.

- Climate, altitude, weather and time set ambient air, with a mid-afternoon peak, warm evenings and a dawn low. On Terrain Diffusion worlds the generator's own sea-level air cools 6.5°C per kilometre of the world's height, so climbing is steadily colder and biome borders do not step; biomes set the daily swing and rain response.
- Blocks slow heat without stopping it; glass and doors pass more, openings more again. Wood insulates, masonry stores, air mixes. Heat eventually escapes.
- Burning machines heat their shell and exhaust, warming nearby air; enclosure and insulation still matter. Torches give modest heat, campfires more, lava much more. Passive exchange conserves heat and never invents hotspots.
- **Comfort.** Players feel the air around them blended with the warmth or chill of what they can see — fires, lava, warm or cold walls, the sun and a sky that is colder when clear; walls and roofs block it. Body temperature changes at a rate that grows faster than its gap to what the player feels: mild differences drift over a minute or more, so walking past a fire or a torch barely moves it, while extreme ones such as open lava or deep cold move it within seconds. Clothing pulls felt temperature toward skin temperature: leather insulates most and padded metal armor somewhat less, enough to matter as much as shelter in the cold, so it warms the cold and adds to warm weather. Comfort peaks at 70°F, lowering calorie costs and speeding work, with a smaller movement benefit. Ordinary daylight is mildly cool, so shelter helps; hot biomes are their own challenge. Cold adds a capped passive calorie cost, never damage.
- A brass thermometer between health and calories shows comfort and road speed; felt temperature shows on screen as a tint, frost or glowing edges and a shiver or sway, with a disable option. Mild temperatures from 60°F to 80°F felt read as comfortable; colour and effects build only beyond that band.
- Cold, exposed machines lose energy; sheltered, warm workshops are efficient. Recovered waste heat comes only from consumed fuel and never duplicates energy used for work; engines make waste heat without gaining furnace efficiency. Architectural heat reuse comes before ducts.
- Cloud cover is server weather per district, the same for every player; shaders draw those clouds and their shadows, which shade players from the sun.
- Cheap, readable heat beats precise prediction: batching and approximation are fine while warmth, insulation and cooling stay understandable and fuel accounting stays finite.
- Architecture matters through physical relationships — shelter, heat, access, scale — not room types or proximity bonuses; outdoor life stays viable.

**Open**
- Cadence (owner direction, still a proposal): about one update per second where changing, two to five seconds where settled, sleeping when negligible. Unverified targets: under 2 ms per tick and at most 3 s behind at 200 players.
- Heat transport, greenhouses, internal machine temperatures, radiant exchange between blocks and oxygen-limited burning.
- Lava and flames warm the air only while among the 4,096 of each indexed per dimension, which Terrain Diffusion's underground lava fills from about 70 chunks; bodies still feel every source they see. Proposal: index and emit only sources near players, the only place their air warmth is felt.
- Architecture proposals: workshops warming homes, greenhouses on waste heat, ventilated engine rooms; heat aboard moving vessels.

## Physical foundation

Mass, heat capacity and stored fuel energy are separate quantities drawn from shared material and shape properties, all in real units; heat and fuel energy are joules, and the heat field stores joules from the real material data. Machine work credit is separate again. Sable owns rigid-body motion; Civilization supplies narrow adapters, energy accounting and gameplay policy. Controls stay simple, with no hidden engineering chores, and the foundation comes before any cargo penalty or machine-temperature rule.

**Open**
- Dynamic cargo mass and heat aboard moving vessels.
- Cargo penalties and machine-temperature rules each need approval.

## Transport

| Mode | Position |
| --- | --- |
| Rivers | Primary passenger and bulk-freight network; earliest and cheapest |
| Trains | Later ground freight; always dearer than rivers |
| Airships | Most expensive, fastest, greatest capacity |

Sable is the vehicle physics backend. Controls stay simple and stable, without linkages or balance engineering.

- **Rivers** connect farms, mines, workshops and towns to each other and the sea. Mouths, confluences and landings create ports and markets through real deliveries, not assigned bonuses. Terrain must offer connected sea-to-inland routes judged by loaded journeys both ways; not every tributary must be navigable. There is no hydrology simulation.
- **Offshore oil** is the first ocean resource: finite reserves, physical crude delivered inland by river. It never replaces onshore oil as the first route to refined fuel.
- **Boats** are freeform vessels built around one Helm, with no prefab hull or size band and a 4,096-block safety cap. Owners extend them freely; only the Helm is fixed.
- Every running **Hot-Bulb Engine** aboard adds fixed thrust, and block mass slows the ship, so larger vessels need more engines. Each engine burns its own refined fuel and oil; the Helm never burns cargo. The engine is a twin-cylinder vertical marine hot-bulb engine of about 1910, not steam or diesel, and runs on refined fuel by design to keep refining in demand. One engine serves boats and land.
- Freight hulls, bunkers and tanks use structural steel with timber decks and player-built cabins.
- The **Coal Bunker** and **Cargo Tank** are reusable 3×5×3 stores that work on land and aboard; later bulk materials extend the same bunker. Incomplete shells keep stock; stocked controllers resist mining, and forced destruction loses contents.
- **Airships** are player-built around a controller — no prefab hulls. The endgame is nuclear-powered antigravity with realistic Sable motion: mass, aerodynamics and wind matter. Antigravity keeps inertia; powered hover holds position against wind; acceleration, braking and reversing build gradually.
- **There is no airship speed ceiling**: power and drag set speed. Terrain preparation follows the predicted path and braking keeps the ship inside ready terrain, with collision protection at any speed. Pilots stay attached until they leave the controls. Stopped ships disassemble back into world blocks without overwriting terrain or losing cargo.
- Fast flight lowers distant-terrain detail through Distant Horizons (one warp threshold at 80 blocks/s, two coarse LOD bands) and loads chunks along the flight path first. Rendering changes, speed and world data do not.
- Airships express the most refined layer: precise metalwork, ivory enamel, restrained turquoise, porcelain and glass. Speed and capacity are tradeoffs.

**Open**
- Which other land machines take engine work (proposal: fuel-powered machines such as the Coal Drill take an engine's work instead of burning fuel themselves).
- Portals, mounts, elytra and teleport commands, before final distances.
- A real ocean → river → inland route on the server terrain; offshore reserves and extraction; one vessel or transfers at the coast; useful return cargo.
- Docking (proposal): a short port-to-port connection with Load/Unload and visible progress while moored, around 128 coal/s or 1,000 mB/s.
- Cargo mass and draft, dismantling, damage, crew custody and vessel access.
- Airship controls, hover assistance, live editing and disconnects; lift capacity and cargo mass; emergency reserve, damage and recovery; vessel size and count budgets; nuclear fuel processing and packaging; how extreme speed costs energy.
- Proposed flight model: finite antigravity lift, mass with centre and inertia, propulsion with force and power limits, quadratic drag against wind-relative airspeed, smooth regional wind. A fixed thrust ceiling may not act as a hidden speed cap.
- Sable's PolyForm Shield license question is deferred by the owner.

## Currency and trade

- Coal is ordinary currency; oil and uranium are denser currencies. Payment is physical fuel, later burned.
- A **trade counter** presents one exchange: receive this item and quantity, pay that item and quantity. Any item can be payment; Coal is the default. Typed quantities can span several stacks; one click trades one batch. One chest holds stock and takings.
- No buy/sell modes or conversions: reversing the pair reverses the trade. Customers cannot touch the owner's stock; both sides must have the goods and the space, and the trade completes together. Shops run while their owners are offline.
- Offers appear on the map beyond explored terrain. Information is remote; goods move physically. Visible stock is not a reservation.

**Open**
- Oil and uranium payment forms; external counter storage.

## Ownership and access

- Unclaimed land allows gathering, building and use without protection; a claim secures a place rather than unlocking gameplay.
- No formal groups. Each claim has one owner and an owner-managed username whitelist with full access.
- The **land controller**, crafted from coal and other resources, holds real coal like a chest. Tiers of 64×64, 128×128 and 256×256 blocks (64 tall) cost 1, 4 and 16 coal per real hour. Changing tier costs 24 hours of the new upkeep and starts a 24-hour cooldown, and is refused if it would empty the reserve.
- **At zero energy a claim instantly becomes unclaimed.** Structures and goods remain.
- **Takeover.** Once a month a buyer can pay the owner a significant premium over the claim's stored energy. The full payment is deposited and the price fixed; the owner then has **one week** to move, with withdrawals and resizing locked and upkeep continuing. At the deadline ownership and payment transfer together, and the buyer receives the remaining energy. The seller may remove buildings, machines and goods — land is bought, not structures — but controller energy stays locked. If power runs out mid-takeover, protection ends, the takeover is cancelled and the buyer is fully refunded; the seller gets nothing.

**Open**
- The takeover calendar and premium; claim costs and capacity.
- A small protection boundary for assets outside claims, keeping death containers recoverable.
- Vehicle access; the automation and protection audit; public-server abuse policy.

## Information and interface

- Terrain gives the main clues. Inspection explains crop suitability and failed planting; prospecting finds buried deposits.
- **Map.** The map lives on a physical 2×2 Survey Table — no handheld map, map key or minimap. Nearby terrain is revealed automatically and shared at the table. It shows public shops and property; deposit finds stay private unless shared; nobody is tracked live. The loop is **search goods → inspect offer → mark destination → travel**, without a mandatory dashboard.
- **Cabinet interface.** Machines and civic controllers share one warm tan cabinet with brass and dark metal, native slots, a clear input-to-output flow and an optional recipe drawer. Coal machines show an animated fire; liquid machines show sight glasses.
- Choosing a recipe shows ghost requirements, never real stock; inserting materials directly works too. Workshops run only an unambiguous recipe and never burn fuel waiting. Extra slots never multiply speed.
- The Tab list shows each player's ping, averaged over five seconds, with waiting and overdue states rather than zero.

**Open**
- Item search, destination pins and sharing; recipe-drawer discoverability.

## Conflict

- **Sieges.** Large resource installations are the objectives of organised sieges, with player-built defences and a decisive encounter at the site's control point — never raids on ordinary claims. The Oil Derrick is the oil control point; it is built section by section from fed materials, and damage removes only the hit section. The coal site's control point must be redesigned now that the drill is retiring.
- **Gladiator Pit.** A stone-brick oval arena with two opposed prep rooms where two consenting players fight. Fighters escrow optional item stakes and must both accept the terms; spectators match opposing identical item wagers, with no house cut, odds or currency. A would-be lethal hit is a knockout without death drops, and the winner takes both stakes. Combat is allowed only between the two fighters inside the arena. Unfinished matches refund rather than forfeit.
- **Pickpocketing and the PvP flag** (approved, not built): wearing a designated item opts into PvP with other flagged players and prevents pickpocketing; unflagged players can be pickpocketed from behind.
- **Firearms.** The Colt Paterson prototype: left-click fires; tap R cocks or reloads when empty, hold R partly reloads; five chambers, slight recoil, server-side projectiles under existing claim and damage rules. Each chamber takes one complete .36 ammunition item.

**Open**
- Sieges: who may start one, defence, notice, unpowered sites, what success transfers, relation to claims and mining. Proposed: reaching the control point ends the encounter.
- Arena forfeits, rankings, loadouts and wider betting.
- Pickpocketing item, range, eligible items, cooldown, flag switching and interaction with claims and death.
- Firearm crafting, balance and penetration.

## Creatures

The first custom mob is the **Lacertan**. Its appearance is approved; its gameplay is not.

- A robust, upright, tailless reptilian humanoid about 2.1 blocks tall: broad but not clunky, strong forearms, five useful fingers, olive skin with brown mottling, ochre ventral scales and loose charcoal hip cloth.
- A clearly reptilian head: heavy scaled brow, short wedge muzzle, cheek scutes, central facial plates, a wide low mouth and no human features. Green slit eyes look alert; the resting expression is neutral.
- Uncovered dorsal thermal plates run down its back. It basks with a relaxed forward lean.
- It is fiction built in Minecraft's angular cuboid style with substantial detail.

**Open** (proposals)
- Name and role: "the Deep Listener", an observer from an older civilization below.
- Encounters: initially neutral and rare near warm cave mouths; observe, warn, then strike or retreat; basking at rest. Health, damage and spawning unset.
- Warmth benefits, drops and progression role.

## Sky civilization

- The scale runs **hand tools → machines → facilities → networks → regional instruments**. Monumental works, regional weather control, sky freight and elite institutions consume supplied energy and depend on the ground economy. No falling cities or weather weapons.
- **Extremely fast airships are a core sky status symbol and energy sink.** Named ships, silhouettes and engine sounds express reputation.
- Every layer has its own prosperity: river homes, forest halls, mining settlements, industrial cities and sky gardens, each with a visible working side.
- **Wireless power.** One great sky machine sends energy to **one regional tower at a time**. The receiving observatories are enormous towers that bring rain and perhaps other services. Transmission loses energy and creates no fuel, items or jurisdiction.
- **Ground access.** Players climb a public monument and take an energy-intensive beam for the final ascent — never a menu or teleport. Its approved exterior is the **Apogee Pyramid**: a 31×31×31 stepped masonry pyramid with copper spines on every face and an open 7×7 crown on diagonal supports (`tools/schematics/generate_apogee_pyramid.py`).

**Open**
- Sky power: range, switching, storage, whether charged towers keep working, ownership, payment and outages; other services such as fog clearing.
- The pyramid's interior ascent, construction, costs and access rights.
- Formal airship races.
