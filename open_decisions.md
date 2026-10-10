# Open decisions and simplification review

This is the only active register of unresolved design choices. Agreed rules live in [design_direction.md](design_direction.md); implementation work lives in [development_plan.md](development_plan.md). Entries are gaps, not permission to reopen settled intent.

## First mob — Lacertan scout proposal

The owner requested the first custom mob concept using the [Lacerta Files](https://archive.org/details/lacerta-files/mode/2up) and a supplied green reptilian humanoid illustration. This is a concept study, not implemented behavior or approval of the proposed rules below. The Archive viewer could not be read directly; a [transcript mirror](https://www.auricmedia.net/wp-content/uploads/2013/03/Lacerta_files.pdf) supplied the lore. Treat its claims as worldbuilding inspiration. The illustration owns the anatomy direction; the transcript supplies underground habitation, warmth-seeking and intelligent observation. Its descriptions differ from the illustration, so this is an adaptation rather than a literal reconstruction.

**Working identity: Lacertan, “the Deep Listener.”** A scout from an older civilization beneath the surface, curious about human settlements but protective of its hidden passages. The proposed body is about 1.9 blocks tall: upright, lean and sinewy, broad shoulders, long forearms, plantigrade feet and no tail. Moss/olive skin, irregular brown markings and pale ochre throat/chest/belly plates follow the reference. A narrow blunt muzzle, heavy brow, slit pupils and a low row of thermal plates down the back distinguish the silhouette. A plain waist wrap and small belt pouch suggest a person with a purpose. Hairless head and conspicuous body scales follow the supplied picture rather than the transcript. No weapons or armor in the first card.

**Proposed encounter loop:** quiet patrol → observe → warn → defend or retreat, with basking as a peaceful idle. Initially neutral. Rare encounters near warm cave mouths make exploration memorable without turning every mine into a combat site. It pauses and turns its head toward nearby players; walking away ends the encounter. Approaching its guarded passage triggers a visible braced pose, raised hands and a low throat rattle before aggression. Attacking it provokes a short claw strike followed by repositioning. It stops pursuing once the player leaves its territory and retreats toward shelter when badly hurt. Actual warning distances, pursuit bounds, damage, health, spawn frequency and persistence remain unresolved.

**Warmth as readable behavior:** in safe daylight it turns its back toward the sun to bask; in cold conditions it favors shelter and moves more deliberately. Sunlight is attractive rather than damaging. First implementation should use a bounded warmth check through existing geography/heat queries, not a new body-temperature simulation. Exact warmth effects remain proposed and need ordinary-play review.

**First-pass scope recommendation:** one unarmed scout and a small state machine. No terrain destruction, inventory theft, player control, human disguise, telepathy, factions, underground city generation or trade system. Those exclusions keep the first encounter understandable; they do not settle future species lore. Do not assign farmable rare drops or new resource gates before choosing its role in progression.

The local `concept_art/lacertan/` study retains the source illustration, exact generation prompt, character card and review/provenance record. The card is a modeling target, not dimensioned geometry, a texture atlas or verified Minecraft appearance. Next decisions: confirm the identity/silhouette and neutral territorial role, then choose the smallest spawn/encounter specification before building the native model. The current manual-coal package remains active.

## Gladiator pit — follow-up review

The owner authorized choosing the remaining first-pass rules and building the pit. Those decisions now live in [design](design_direction.md#gladiator-pit) and the [implementation reference](civilization-mod/gladiator-pit.md); the original local engineering study is preserved as proposal provenance, with runtime geometry authoritative in ArenaLayout.

Follow-up reviews: ordinary Survival construction and wager UX; four simultaneous real fighter/spectator clients; abrupt-crash consistency across player/world saves; and crowded-arena work. The implemented abort/refund rule deliberately allows a fighter to avoid losing by leaving or disconnecting. Any later forfeit/reconnect adjudication, rankings/tournaments, configurable loadouts, furnishing exceptions or expanded betting must be chosen separately. None is required to use the initial two-player arena.

## Guns — first Paterson model

The user approved the playable Paterson prototype: left-click firing, tap R to cock each shot/empty reload and hold R for partial reload, slight recoil, projectiles and tunable damage. The user selected one complete .36 ammunition item per chamber, shown as a conical lead projectile with no cartridge case. These rules now live in [design](design_direction.md#setting-and-material-style); [firearms](civilization-mod/firearms.md) owns implementation and trial values. Acquisition/crafting progression, production combat balance, any later headshot/penetration rules and full historical loading fidelity remain unresolved. No crafting recipe is adopted.

## Simplification recommendations

These recommendations consolidate implementation choices; they do not silently add game rules.

| Reuse | Application | Avoid adding now |
| --- | --- | --- |
| Physical resource inventories | Fuel is usable energy and payment; shops reserve actual stock | Bank accounts, energy certificates, minting or fixed exchange-rate services |
| Player owner plus claim whitelist | Land and shared facilities; vehicle access remains later work | Formal groups, company/town/guild frameworks or per-member roles |
| One geographic query | Crop/forestry eligibility, habitat and deposit generation | Independent region systems or climate/soil/hydrology simulation |
| Explicit fuel consumers | Cooking, machines, claims and vehicles use a common resource vocabulary | A global power grid just to operate the first consumer |
| Atomic exchanges with reserved stock | Shop trades and paid land transfer share conservation/retry rules | A universal contracts engine, payroll, loans or courts |
| Map plus contextual inspection | Market discovery, property visibility, suitability and prospecting | Separate dashboards for each activity |

Reuse storage and accounting without forcing all consumers to behave identically. Current kiln heat burns while lit; a claim loses protection at zero; a boat stops; depletion leaves a player a recovery route. These differences are useful rules, not duplication to erase.

Keep short, direct controls: supply fuel, inspect requirements, load goods, set an offer. Persistent implementation work is justified where it enforces an agreed rule, especially claims and transactions. Do not build a general framework before concrete consumers need it.

## Builder's Line — follow-up choices

The right-click offhand tool, reel appearance, bounded full-block execution and modest Smithy recipe are implemented. [Design](design_direction.md#straight-line-hand-tool) owns the rules; [construction](civilization-mod/cutting.md#builders-line) owns controls, costs and coverage; [the package](development_plan.md#builders-line--implemented-package) owns follow-up work.

Remaining: evaluate the 16-cell/16-block working limits and placement pace during ordinary play; decide whether a future speed boost belongs to powered construction; extend shaped pieces through the existing half-grid placement path only after settling repeated orientation/anchors. Functional blocks and moving vessels need their own placement/access semantics before inclusion. The original crowded procedural reel sprite was rejected; its local history is preserved alongside the replacement generated master and exact prompt in the [asset](art/assets/builders_line/asset.json).

## Decisions that block coherent implementation

| ID | Decision | Why it matters / simplest candidate to evaluate |
| --- | --- | --- |
| D01 | Spawn, starting items and accessible subsistence | A fresh solo player must reach food, tools and claim inputs without existing buyers. No starter kit or no-kit policy is approved |
| D02 | Remaining vanilla boundary | Household cooking and the initial town-workshop catalog/gates are implemented; see [industry](civilization-mod/industry.md). Thermal/coal consolidation is implemented; the [remaining audit](#vanilla-replacement-audit) records follow-up boundaries. Special-equipment/finite-loot coverage (E08), renewable metals/fuels, enchanting/XP, mobs, redstone and automation still need review |
| D03 | Passenger travel and dimensions | Decide portals, personal boats/mounts, elytra and teleport commands before final distances. Keep-inventory is already superseded by local recovery |
| D04 | Region representation and generation | Shared biome-derived queries, crop coverage and tunable floodplain/forestry defaults are [implemented](civilization-mod/geography.md). Still open: navigable river width/depth/continuity, local river elevations, remaining plant/species rules and permanent-world generation policy. Terrain-aware coal/oil placement, prospecting and finite reserves are [implemented](civilization-mod/industry.md) and were exercised in a fresh Tectonic world; fresh development worlds are the target, with no migration requirement |
| D05 | Fuel forms, packages and conservation | Crude, refined fuel, lubricating oil, fixed tanks, basic pipes and 1,000 mB portable-liquid canisters establish initial oil handling. Internal heated-crude/vapor streams have no canisters. Select later oil freight quantities and oil/uranium payment denominations. Coal/oil remain bulk logistics; raw uranium's small hand-recovered finds are intentionally portable. The processed fuel package and energy yield remain open. Energy density, price and cargo volume are distinct; compressed coal/oil blocks/tanks must not bypass freight. Existing 16-stack items stay unchanged in 0.10.0; later per-item tuning remains open |
| D06 | Claim shape, power and denomination | Three generous tiers, chest-style Coal storage, offline upkeep and actual-reserve valuation are [implemented](civilization-mod/ownership-trade.md). Balance/capacity and later fuel denominations remain open |
| D07 | Takeover scheduling and failure | Initial rolling 30-day eligibility, full-payment reservation, top-ups and energy/resize locks are implemented. Agreed exhaustion behavior: cancel, lose protection immediately and refund the buyer. Revisit scheduling only if play evidence warrants it |
| D08 | Claim loss and movable asset rights | Initial counters retain owner-only management but can be broken on unclaimed land; left-behind stock drops on removal. Vehicle rights, remaining automation/protection audit and owner-only death recovery remain |
| D09 | Access administration | Formal groups are removed. Owner-managed per-claim username whitelists provide full local access; see ownership/trade reference. Public-server abuse and recovery policies remain |
| D10 | Recovery and vehicle lifecycle | Hazardous death placement, inaccessible claims, cleanup/repeated deaths, XP and respawn health; boat damage, disconnection, dismantling and empty-craft portability |
| D11 | Shop and map implementation | Local item-for-item counters, ghost selectors and shared physical chest storage are implemented; 2×2 Survey Table access, automatic shared local terrain and public claim/counter markers are implemented. External storage, item search, destination pins and expanded survey range remain. Real multi-client transaction verification is pending. No remote reservation or automatic route service is approved |
| D12 | World lifespan and operating budget | Choose reserve horizon and expansion/reset policy before a permanent world. Set hardware, active simulation and maintenance budgets before promising 200-player capacity |

Do not ask the user to resolve all twelve at once. Pick the decisions required by the next useful package; unrelated gaps can remain open.

## Tier 2 kitchen workshop

Cooking retains its ordinary-play follow-ups while the [manual huge-vein package](development_plan.md#manual-coal-mining--prototype-package) is in its first playable prototype. An entire kitchen with multiple multiblocks is the agreed direction; [food design](design_direction.md#3-people-food-and-recovery) owns sensory mastery, quality, calories and work benefits. The [room/station concepts](art/assets/t2_kitchen/asset.json) are planning art, not construction plans; their earlier oven mockup was rejected. The [oven](civilization-mod/cooking.md#baking-oven-physical-first-pass) now has an agreed 2x2 Creative physical preview with an opening door and continuous draft control; baking and Survival construction remain unresolved. The [current stove](civilization-mod/cooking.md) implements one skillet, continuous heat, resting/carryover and a generic work benefit; [the prototype plan](development_plan.md#tier-2-cooking--first-prototype-plan) owns the next work. Preparation is currently deferred.

Remaining choices: other station dimensions, Survival construction, supported recipes/substitutions, batch and container handling, specialties, packaging, full-reserve eating and interactions with comfort/tool effects. Existing trial values live in the cooking reference. A mining-speed specialty is a candidate, not implemented or agreed tuning. The manual huge-vein replacement is now agreed; current Coal Drill/refinery rates remain shipped values until that package is implemented and balanced.

### Focused cooking gameplay proposal

Later station roles remain proposals:

| Station | Useful responsibility / proposed interaction |
| --- | --- |
| Preparation bench | Select a recipe, supported substitutions and batch size; preview requirements, portions/calories/benefits and reserve one identifiable batch. No chopping score. Intermediates need an independent use |
| Cooking range | Pot dishes: hot browning followed by gentler simmering, with one meaningful heat transition communicated by food appearance/sound. No repeated stirring or timing prompts |
| Baking oven | Preheat, load rack-sized trays and remove in a generous browned window. Shared preheat and batch scheduling should matter; door opening/rotation needs a functional reason |
| Portioning bench | Convert a finished pot/tray into servings or useful travel packaging in one sequence. Preserve cooking quality; no bowl aiming or serving score. A multiportion packet needs capacity/container rules |

Use freely arranged stations and ordinary pantry storage as the starting candidate; room detection is not selected. Preparation/portioning need no independent minigame. Test whether portioning adds a useful handling/format role before making it mandatory. A pot meal and baked meal can later test the full process.

Recipe identity should determine specialties while one batch-quality result affects retained calories and bonus strength. Ingredient/cooked-food budgets cap edible energy; skill approaches the ceiling rather than creating calories through extra actions. Hold duration constant initially to avoid quality changing every parameter. Exact recipes, energy ceilings, portions, fuel and specialty stacking/refresh rules remain to settle.

### Spatial cooking mastery proposal

Extend the agreed continuous color/sound progression and forgiving plateau to later cookware. Candidate interactions:

- Snap prepared batches, pots and trays to broad supported positions; preserve contents/progress when moved. Avoid precision pouring, spills or handling scores.
- Learn when to change a pot from browning to simmering. Clear ingredient/broth/bubble cues should justify the transition; do not add a stirring timer.
- Coordinate oven preheat, tray capacity and removal alongside other batches. Extend paid carryover to pots/trays only after testing it.
- Let batch size, starting warmth and existing work vary through player choices. No randomized ideal timers, hidden quality rolls, layout score or adjacency bonus.
- Evolve surface/color, steam and crossfaded sounds across many states. Both color and sound should independently communicate readiness; plain descriptions support accessibility. No live perfect-state lamp or percentage chase, and no concealed frame-perfect advantage inside the plateau.

Playtest repeat recognition from color alone and sound alone, simultaneous station sound attribution, ordinary/Photon lighting, movement time between stations and carryover anticipation. A novice should make useful food slowly; mastery should support faster high-quality batches and eventual coordination of several dishes. The prototype proves mechanics, not enjoyment or player learning.

## Manual coal mining — proposed implementation plan

The [agreed direction](design_direction.md#4-regional-land-and-materials) preserves ordinary small coal deposits and replaces the Coal Drill with manual work on richer huge veins. Clicking starts a pick swing; held left-click repeats after full recovery, and camera movement shapes the stroke. The [first Creative/admin prototype](civilization-mod/coal-mining.md) now trials the 4×4×4 grid, click/camera behavior, saved loose stock, bounded shovel/cart loads and a hand-worked screen. The first two swing revisions were rejected in ordinary play, including a head-down windup and sideways contact spin. The supplied motion sequence now guides a close overhead windup and forward/downward strike in one plane, using weighted camera following with a close grip throughout the stroke; this remains an implemented trial for feel review, rather than an accepted final animation. Its values are implemented trials, not settled Survival rules. The remaining proposals below guide the next custody, progression and presentation stages. [The work package](development_plan.md#manual-coal-mining--prototype-package) owns delivery order; the drill remains shipped until replacement.

### Player loop and first playable slice

**Ordinary coal → special equipment and mine infrastructure → chip a huge vein → shovel loose raw coal → loaded minecart → surface preparation → usable Coal.** Keep one raw-coal material and the existing `civilization:mineral_coal` output; avoid introducing coke, multiple coal grades or a washing chemistry chain. Ordinary coal remains a direct fuel source and funds the upgrade. Proposed special tools come through the Smithy, with exact materials/tier still open. Prototype with Creative supplies first, then prove actual Survival acquisition before retiring the drill.

Start with a small placed vein face, a floor, one cart on a short accessible rail route and one preparation machine. Complete this entire loop before changing regional generation. Museum Wales documents [coal picks and shovels among its historical hand tools](https://museum.wales/curatorial/industry/coal-mining/) and a [reconstructed mid-nineteenth-century coal tram](https://amgueddfa.cymru/casgliadau/arlein/object/8f0a0c0e-00e7-352c-bdf6-528b18eb3c59/Tram/?field0=with_images&field1=string&index=0&value0=on). These support the material/transport reference, not exact game control or processing rules.

### Pick, fracture and feedback

- Proposed swing: wind-up → contact → recovery. A click commits one action; held left-click repeats after full recovery, and release stops the next action. Sample bounded relative camera yaw/pitch around the swing to shape the arc, impact orientation and follow-through; keep ordinary aim responsive. Still-camera clicks remain effective. Camera speed does not multiply resource yield, and spinning or high sensitivity must not produce extra hits. No compulsory camera shake or frame-perfect bonus.
- Resolve one local contact against the remaining exposed coal geometry. A miss completes visibly without removing material. The server checks held tool, reach, obstruction, swing cadence, calories, durability and current claim access at contact; the client predicts presentation only. Revalidate tool/target changes and reject duplicate or impossible strike requests. Handling of missed-swing labor/wear and canceled actions needs a chosen rule.
- Prefer fixed coarse subcells and small clustered removals over arbitrary mesh destruction. Trial a 4×4×4 logical grid, with a few irregular fracture silhouettes, and compare readability/cost before fixing resolution. Each accepted strike removes an aimed exposed portion, opens a real notch and reveals the next working face. Mining the final portion clears the block. Render, raycast and collision must agree; do not use a whole-block collision box around a hollowed face.
- Strong readable timing, a weighted held-tool arc, distinct coal impacts, falling clatter and restrained dust should carry the satisfaction. Review first/third person and Photon plus shaders-disabled views, and listen while working beside another miner. Make the playable tool and fracture readable before refining decorative detail.

### Loose coal, shovel and cart

- Proposal: a short-lived falling clump carries the exact removed quantity, then merges into persistent local piles. Aggregate visual chips; never create an inventory-bearing entity for every subcell. Settled piles stop ticking and render from cached quantity stages. Save falling quantities as well as settled stock. Bound neighbor/landing searches and pause at unloaded boundaries without forcing chunks.
- Failed landing or full pile capacity must retain real stock in a saved carrier/pending state. Piles on steps, in corners, under later excavation and with their support removed need deterministic relocation and conservation. Avoid a general rigid-body rubble solver. A local floor fixture is the first proof; vertical drops and crowded working faces follow before shipping.
- Proposed shovel operation: scoop from an aimed pile, visibly carry one bounded load, then deposit into a nearby cart. Store that load on the actual shovel, not in client animation or an unsaved player variable. Stop at cart capacity; leave overflow on the shovel/in the pile. Tool switching, dropping, breaking, death and relog must transfer or preserve the same quantity once. Exact buttons, scoop size and whether a return-to-ground action is needed remain open. Walking over a pile should not automatically grant finished Coal.
- Reuse ordinary rail movement for one dedicated raw-coal cart with a visible heap and one authoritative cargo amount. Choose a simple push/haul interaction for the first route; do not assume retired furnace carts or arbitrary powered rails satisfy the industrial rules. Flat drift access can prove the loop before choosing uphill haulage. Cart destruction, unload/reload, full targets and competing shovel users must conserve cargo. Cart access and cross-claim movement/unloading rules must be explicit before multiplayer shipping.

### Preparation and progression

Recommend one **Coal Preparation Screen**: a receiving hopper and visibly moving grate separate/size the mined material and output usable Coal. This is a proposed readable game abstraction, not a claim that real coal acquires fuel energy through screening. Decide power before its Survival implementation: coal-powered processing is reachable using ordinary ore; a hand-operated screen is another candidate. Oil power must not silently become mandatory for huge-vein mining. Choose throughput, input/output capacity, conversion yield and any waste together; avoid adding waste unless it has useful play.

Represent extracted quantity in integer raw units, with explicit conversion and saved fractional remainder where needed. The budget must account for **remaining vein + falling/piled raw coal + shovel loads + cart cargo + machine input/work/output + explicit processing losses**. Interruptions cannot destroy a paid batch or repeat its output. Whole processed yield per huge-vein block must exceed ordinary small-ore yield, without camera gestures, Fortune, Silk Touch, explosions, pistons, hoppers or Builder's Line bypassing the special extraction rules. Decide these interactions explicitly; ordinary small ore retains its normal rules. Existing finished Coal, claims, burners and Coal Bunkers remain compatible; raw cargo/storage adapters need their own eligibility decisions.

### Delivery choices and exclusions

Unresolved before their respective implementation stages: subcell/yield scale; tool recipes/grades/repair and work costs; shovel custody controls; cart capacity, pushing and uphill transport; preparation power/recipe/throughput; raw-material automation/access; seam access and geometry; retained drill contents and legacy IDs. New geology uses fresh development worlds under the existing policy. Retire drill acquisition/operation only after the Survival replacement works, preserving old registered IDs, inventories, liquids and user saves under an explicit recovery policy. Never regenerate mined coal from a site description. Keep the old editable assets/provenance local.

Do not bundle cave-ins, gas/explosions, ventilation, lung disease, mandatory supports, elevators, locomotives, conveyors, a new rail network or siege mechanics into this first loop. They are possible later topics, not agreed requirements. A bounded busy-face performance sample is required; it does not establish 200-player capacity.

## Coal and oil balance validation

The broad target of about two coal seams per oil field remains; [design](design_direction.md#4-regional-land-and-materials) owns the rule and [industry](civilization-mod/industry.md#find-a-site) owns current mechanics. Manual huge-vein mining supersedes the drill fuel budget of two coal fields plus a one-fifth vehicle reserve. Rebalance from actual mining/shoveling/hauling labor, preparation demand, ordinary-coal bootstrap and oil-powered transport rather than preserving that extraction ratio. Huge veins must yield more usable Coal per block than ordinary small deposits; choose the total yield and extraction time together. Measure realized stock, site counts, generation time and depletion in representative fresh Tectonic worlds before claiming the broad-world site ratio or labor economy is achieved. Geological suitability may make local ratios differ; current drill rates remain shipped reference values until retirement.

## Terrain Diffusion river evaluation — 2026-09-19

Candidate for D04, not an adopted world generator: [Terrain Diffusion](https://github.com/xandergos/terrain-diffusion-mc). The Python snippet in [issue #240](https://github.com/xandergos/terrain-diffusion-mc/issues/240) derives a binary river footprint from an elevation raster using Whitebox depression filling, D8 accumulation and width dilation. It does not implement Minecraft channels, water elevations or biome integration. The issue's comments also link [Alesrr's Terra Diffusion fork](https://github.com/Alesrr/terra-diffusion-compat), which advertises integrated rivers/lakes and publishes Minecraft 1.21.1 NeoForge builds. Evaluate that implementation before writing our own. Its additional terrain/cave/biome changes also need review; current upstream issue labels alone do not establish active progress.

A disposable run of the original snippet, changing only its working directory, passed output shape/value/nonempty checks on a synthetic 512×512 valley with 30 m cells. The mask stage including Whitebox initialization took 2.936 seconds and covered 17.742% of the raster; the lower channels were excessively broad for this fixture. This is neither a Terrain Diffusion sample nor an in-game compatibility result. Scratch source, environment, raster, result JSON and comparison image are in ignored `.tools/river-issue-240/`. The fork's `TD_v1.3.0` Windows NeoForge JAR was installed in Civilization Dev at the user's request; the installed SHA-256 matches the published asset digest. The first user launch exposed a missing-Terralith registry crash; Terralith and Lithostitched are now installed to address it. Post-fix in-game validation remains pending. Deployment and validation details live in [status](docs/status.md).

Remaining evaluation: isolated fresh-world startup, generated river continuity and navigable width/depth, generation cost, and Civilization farming/deposit compatibility. Our [geography provider](civilization-mod/geography.md) currently assumes a fixed river elevation and generator river-biome corridors; visible water alone will not make farmland eligible. Adoption and permanent-world policy remain open.

## Tectonic terrain trial — 2026-09-27

Tectonic 3.0.28 is installed in Civilization Dev as a candidate terrain generator, not yet the permanent-world choice. Its supplied Overworld settings keep the vanilla vertical range and sea level. A fresh dedicated-server trial with the same Tectonic/Lithostitched versions generated an elevated savanna-plateau coal clue and buried seam and a dry-desert oil seep and reservoir after terrain-aware placement changes; details and limits are in [status](docs/status.md). Deposit compatibility at those sites is verified. Still evaluate river width, depth, continuity and local water elevations against the 48-block/Y58–70 farming rule, representative boat routes, world-generation cost and broader deposit abundance before adopting a permanent-world preset.

## Vanilla replacement audit

Thermal/coal consolidation is approved and implemented, including removal of coal blocks. Agreed direction lives in [design](design_direction.md#unified-coal-and-thermal-production); exact coverage, controller recipes and legacy-stock handling live in [industry](civilization-mod/industry.md#thermal-processing-and-retired-vanilla-systems).

Remaining: renewable iron/gold and other resource farms, special equipment/finite loot, biological food shortcuts, and portable/remote storage or travel that bypass freight. Compare the stonecutter's full decorative output coverage before removing overlap with saws. These are progression boundaries, not automatically obsolete features. The clay/gravel fertilizer route is removed; sulfur + gravel now owns fertilizer production. Husbandry remains E05; passenger/dimension rules remain D03. Later oil freight limits and processed uranium fuel packaging still need tuning (D05); coal-block compression is removed rather than a pending balance option.

## Four interactions requiring explicit reconciliation

**Fuel currency versus personal freight limits.** Higher fuels should carry greater value per unit, yet a pocket should not supply a large industrial operation for an excessive period. Tune stack limits, fuel payloads and consumer rates together. This is not solved by declaring every high-tier item unstackable.

**Fixed-price takeover versus continuing upkeep — resolved for the initial implementation.** Reserve withdrawals and resizing lock during handover; both parties may add fuel without repricing. Upkeep continues. Exhaustion cancels the takeover and makes the full buyer payment collectible at the original controller location. Removing the block cannot erase the obligation.

**Powered claims versus separately protected assets.** Full protection for every unclaimed chest or machine could make land power unnecessary. Conversely, the agreed private death container must remain recoverable. Define a small, explicit protection boundary rather than an exception per block.

**Finite fuel versus permanent world.** Depletion, industrial consumption and fuel payments have different effects: payment transfers stock; use consumes it. Neither currency nor land competition replenishes reserves. The world-lifespan decision remains unavoidable.

## Tuning after the rules are chosen

Keep these in configuration when appropriate, rather than repeatedly reopening architecture:

- Crop/forestry rates, feed and cooking yields, calorie catalog and resource abundance.
- Stack limits for specific goods, fuel payloads, cargo slots and consumer rates.
- Claim construction cost, power scaling and takeover premium.
- Vehicle dimensions/speeds/costs and useful journey lengths.
- Instrument range/duration and supply budgets.

One week to move, 32 ordinary stacks and one log to one plank are already agreed baselines. The proposed seven-day takeover window every 30 days was not approved. Do not confuse it with the agreed move-out week.

## Immersion direction — proposals

The goal of immersion across every activity is agreed. The following is a cohesive treatment to evaluate, not an approved feature list or new subsystem.

**Make useful work perceptible.** Each activity should have a readable place, a satisfying action, feedback tied to real state and a visible consequence. A farmer should enjoy a well-run field as a place; a sky resident should experience speed, altitude and command of extraordinary equipment. Avoid using inconvenience or hidden numbers as a substitute for presence.

| Activity | Proposed experience | Use existing state where possible |
| --- | --- | --- |
| Farming | River ambience, clear planting/harvest sounds, visually readable crop maturity and distinct treated plots; rows visibly change as work is completed | Crop stage, fertilizer state and harvest events; no per-plant ecology simulation |
| Forestry | Dense woodland ambience, material-specific chopping and a clearing that becomes a recognizable work site | Local environment and real block changes; no falling-tree physics required |
| Mining | Surface clues lead to a visible seam; material-specific impacts and restrained dust make extraction tangible; lamps, galleries and loading places express development | Actual terrain, mined material and extraction state; huge-vein chipping is agreed, with presentation details in the [manual-mining proposal](#manual-coal-mining--proposed-implementation-plan) |
| Processing | Ignition, a working rhythm, glow/exhaust and a clear stop when fuel or inputs fail | Actual machine state transitions, with nearby client effects rather than persistent decorative entities |
| Freight and trading | Fueling, boarding, a changing engine note, departure and arrival at a physical counter/landing; landmarks make familiar routes recognizable | Fuel, engine mode, speed, inventories and existing map offers; do not require hand-carrying every item as a separate prop |
| Building and settlements | Distinct material sounds, warm occupied interiors, coherent signs and a skyline changed by supplied construction | Player-built architecture and small reusable assets; no simulated NPC crowd required |
| Fast airships and sky estates | Rapid landscape passage, powerful propulsion during acceleration, wind on exposed decks, clearer acoustics in enclosed spaces; calm gardens beside active service areas | Actual speed, engine operation, elevation and conservative enclosure cues; no mandatory camera shake or motion blur |

**One world, different rhythms.** River water, timber, stone, metal and powered machinery form a small shared sound/material vocabulary. Rural places feel open and seasonal in appearance without requiring a season simulation; industrial sites concentrate working sounds; sky spaces contrast exposed wind, powerful engines and quiet interiors. Mechanical activity should track actual operation, not an endless generic factory soundtrack. Regional weather should be perceptible from fields as well as from the instrument above.

**Keep information usable.** Prefer world cues first and exact information on inspection or in an ordinary menu. A changing engine sound cannot replace a readable fuel reserve. Never hide prices, land takeover deadlines or protection status for atmosphere. Reuse brief contextual interaction; no new immersive menu style for each machine.

**Investment changes the experience, not just a number.** Larger facilities, organized stores, loading areas and extraordinary craft visibly change a site. Advanced equipment expands reach and throughput while smaller work remains satisfying. No mandatory prestige stat, roleplay script or work minigame is implied.

**First treatment implemented in 0.10.0:** the stove, kiln and Fertilizer Furnace have state-driven heat/processing effects, quiet sounds and menu feedback. Crop-specific treatment and future boat effects remain proposals. Normal play and dense-site audiovisual tuning remain to be evaluated.

Acceptance: nearby players can distinguish working, stopped and complete states without opening every menu; exact state remains accessible; repeated actions stay responsive; sound does not become unbearable in a dense factory; visual effects are bounded and scalable, with independent sound/motion settings. Compare a short ordinary work session and a crowded site, not only an isolated showcase. Audio/effect assets, licensing and technical rendering/audio support need investigation when implementation begins.

## Other later design

### Sky power and regional observatories

Agreed: one great central sky machine directs energy to one regional tower at a time. Receiving observatories are monumental towers, not modest buildings. Proposed minimum: select one receiver and account for that transfer; avoid electromagnetic simulation or arbitrary relay routing. Range, obstruction, target switching, retarget delay and energy storage remain open. Whether other sky settlements can possess their own great machine is not specified; do not infer a server-wide singleton rule.

A local energy buffer remains only a proposal. If allowed, previously charged towers might continue services while another receives the beam. Decide that behavior explicitly: one active power target is agreed, but whether only one tower may produce an effect at a time is not. Interface and tests must enforce one active receiver per great machine with no duplicated transfer during switching or restart.

Suggested presentation: transmitter and receiver orient toward each other; charging changes light and sound; a restrained atmospheric beam/discharge makes an active link visible. Tower activation becomes a recognizable event across its valley. Exact visibility and sound range need tuning; no permanently glaring beam or mandatory camera effects are required.

Rain is the first service. Fog clearing around freight approaches and storm calming for local routes are additional candidates, not approved mechanics. Their usefulness depends on the chosen visibility/navigation rules; no full regional atmosphere simulation is implied. Do not expand this into resource creation, destructive weather, remote item transfer or unlimited fertility.

Decide receiver ownership and supply authorization, payment if needed, overlap, losses, outages, local reserves and offline/unloaded clocks. Supplying a tower does not create jurisdiction over nearby land. A supplier controls a useful service; an existing energy buffer must be consumed before an outage stops it. Agricultural effects should improve eligible fields rather than silently create river eligibility; final scope remains open.

The fictional chain can be ground-extracted fuel → supplied sky generation/transmission → regional receiver → useful effect. Explain why elevation improves the service instead of imposing a player-rank gate. Sky transmitter reactor/generator choice and transmission formulas remain unspecified. Airships now require nuclear power under the controller-built direction below.

Extremely fast airships are now an agreed sky status symbol and major outlet for energy. Specify how extraordinary speed consumes energy, what affects cargo capacity, and the server/client limits on safe travel. A sharply rising energy cost at extreme speeds is a candidate, not an approved formula. Do not assume the fastest prestige craft must also carry the maximum possible cargo; the broader airship mode remains the fastest, highest-capacity and most expensive transport tier.

### Ground access to the sky civilization

The sky civilization should require players to physically reach the summit of a supplied monumental installation, then use an energy-intensive controlled beam transfer for the final ascent. The access structure must read as a public great work rather than a menu, command or personal teleport item. The [Apogee Pyramid exterior](concept_art/schematics/apogee_pyramid_v4/README.md) is selected and recorded in the design direction; the earlier spire, terrace-pyramid and resonance-crown studies remain visual history in [concept art](concept_art/prompts.md#sky-access-monument-concepts).

Design the internal ascent with four meaningful stages while preserving the approved exterior shell and clear cardinal approaches to the summit. Resolve how the route begins, accessibility during construction, incomplete-structure behavior and whether all four cardinal approaches connect internally. Energy cost, access rights, transport capacity, activation time, interrupted-transfer behavior and whether this is also the regional weather receiver remain unresolved.

Further oil/uranium processing and tuning beyond the implemented three-product refinery; the proposed steel/component/alloy vocabulary; train/airship implementation and energy; supplied monumental construction; weather radius/overlap/shortage; sky architecture, lift and access; optional PvP stakes; world name and artistic vocabulary.

Natural-water-only fishing, empty boat portability, automatic route services and jurisdiction-like rental/toll systems are not approved mechanics. Ideas for research progression, provenance, detailed wear, NPC labor, banking and general contracts remain original-document ambitions or deferred candidates, not a launch checklist.

Work order belongs to the [development plan](development_plan.md). Resolve only the decisions required by the active package.


## Vehicle physics dependency research — 2026-09-10

Sable is selected and used by the production vehicle prototypes without Create gameplay. Exact pins, API examples, graphics checks and integration limits belong in [vehicles](civilization-mod/vehicle-physics.md); material/mass boundaries belong in [physics](civilization-mod/physics.md).

Remaining questions:

- Dynamic per-position mass for cut cells and cargo; vessel/world coordinate handling for claims, maps, guides, geography and labor accounting.
- Broader landing/shore behavior, peers, loaded terrain and active-vessel budgets. Existing local checks do not establish 200-player capacity.
- The user deferred clarification of Sable's [PolyForm Shield license](https://github.com/ryanhcode/sable/blob/main/LICENSE.md); no author contact or licensing conclusion is recorded.

Keep integration narrow against the pinned API. Production boats and flight proofs supersede the old proposal to begin with a powered barge.

## Modular vessel scope

The [implemented boat design](civilization-mod/vehicle-physics.md#freeform-motor-vessels) uses a freeform connected hull around one Helm. The previous 5×7 foundation and rectangular cabin band are superseded. A 4,096-block assembly cap prevents terrain capture but does not prescribe dimensions. Owners may add and remove connected supported blocks after launch; only the Helm is immutable. Controls and stabilization work by default, without linkages or manual center-of-mass tuning.

Every complete enabled Hot-Bulb Engine aboard contributes fixed thrust. Sable physical block mass reduces acceleration, terminal speed and steering response, so engines and vessel size form the scaling rule. Inventory and tank contents are not yet added to dynamic mass. Decide content mass, draft, dismantling, damage, detached-fragment recovery and crew custody separately.

Normal chests/barrels and the implemented Coal Bunker/Cargo Tank can be mounted aboard. Powered dock transfer remains open below; do not turn local loading into a universal pipe or shaft network.

## Controller-built airships — proposed implementation plan

[Agreed direction](design_direction.md#controller-built-antigravity-airships): a player-built vessel around a controller, nuclear power, antigravity, meaningful mass and aerodynamic/wind effects using Sable. The user accepted this starting direction and requested a working prototype with a typed numeric power level spanning the full finite range. The [first flight implementation](civilization-mod/vehicle-physics.md#airship-prototype) now provides that override without a configured speed cap; detailed survival tuning, accurate cargo mass and failure/recovery choices below remain unresolved. [Development plan](development_plan.md#airships--active-planning-package) owns work order.

### Construction and controls

The accepted starting package is **one controller containing a compact nuclear plant, antigravity drive and flight controls**. The current prototype substitutes a typed power supply for nuclear fuel processing. Place it, build a connected vessel, inspect flight readiness, assemble, load, fly, land and refuel. No required hull shape, balloon, wings or machinery linkage puzzle. Separate reactor/drive blocks can follow only if they add useful decisions.

Use bounded explicit assembly selection and a preview so touching terrain, docks or adjacent ships cannot become cargo accidentally. One active controller per vessel; additional controllers cannot multiply lift. Check custody, supported blocks/entities, dimensions and load before assembly. Keep fixed-world controllers excluded until moving-system adapters are proven. Initially recommend hull editing while landed, with cargo changes supported in flight.

Recommend automatic leveling, heading assistance and powered hover/position hold with finite force and torque authority. The controller makes normal construction comfortable to fly; overloaded or extreme asymmetric designs expose understandable limits. Controls request forces through Sable, rather than setting velocity or locking position. Braking also has finite authority. Exact pilot inputs, unattended flight and disconnect behavior remain open.

### Physical model

| Element | Proposed behavior |
| --- | --- |
| Antigravity | Distributed upward force with finite lift capacity cancels weight without removing inertia. Its mass-dependent upkeep is an explicit fictional rule |
| Mass | Count material volume, controller equipment, inventory contents and tank fluids. Update center of mass and inertia along with total mass; audit passenger coupling to avoid counting load twice |
| Acceleration | Net force divided by mass. Under equal force, twice the mass gives half the acceleration. Greater inertia also affects stopping and turning |
| Power | Reserve plant output for lift/stability first, then propulsion. Heavy loads consume more support power and can reduce cruising speed. Fuel quantity determines endurance; plant output determines available power |
| Propulsion | Both force and power ceilings, plus a positive low-speed/holding cost. Avoid singular power/velocity formulas at zero speed; stabilization is not free and braking does not regenerate fuel |
| Drag | Air-relative velocity equals vessel velocity minus wind. Approximate drag as `0.5 * air density * Cd * exposed area * airspeed²` |
| Shape | Cached directional exterior projections and coarse shape/coverage estimates, partial-block aware. Avoid bounding-box-only drag and counting internal faces; tune coefficients against slim, broad and open hulls |
| Wind | Smooth server-owned regional horizontal wind, with bounded gusts. Head/tailwind changes ground speed; crosswind causes drift and torque. Position hold spends power resisting wind |
| Speed | Cruise emerges from thrust balancing resistance. Mass alone does not impose an arbitrary speed multiplier; reduced propulsion headroom supplies the proposed load/speed link |

In still air with fixed shape and coefficients, doubling speed means four times the drag and approximately eight times the aerodynamic propulsion power (`P = drag * speed`). Total demand also includes field upkeep and losses. Physical basis: [NASA drag equation](https://www1.grc.nasa.gov/beginners-guide-to-aeronautics/drag-equation/) and [relative air velocity](https://www1.grc.nasa.gov/beginners-guide-to-aeronautics/factors-that-affect-drag/). Antigravity upkeep and drive efficiency are game rules, not real-world nuclear/flight claims.

Start with directional pressure drag, a conservative surface-drag term and bounded off-center pressure samples for torque. Full fluid simulation, terrain turbulence, wing lift and altitude-density variation are outside the initial proposal. Geometry updates on edits, not every substep. Show airspeed and ground speed separately when relevant.

Use the [shared material foundation](civilization-mod/physics.md), with explicit item/fluid profiles and fallbacks. Audit recipe mass transformations, nested storage and container tare; exclude zero-mass cargo loopholes and double counting. Current Sable integration does not implement dynamic inventory/cut-block mass. Prove supported mass/inertia invalidation against the pinned runtime before promising it; keep one authoritative body mass rather than a disconnected handling penalty.

### Nuclear supply and operating lifecycle

Uranium now appears as rare, small stone-hosted ore in defined rocky Overworld biomes, hand-mined with an iron-or-better pickaxe into portable Raw Uranium. A worn Geiger counter detects nearby loaded ore by sound. The realized distribution is a starting balance, not a proved frontier supply rate. How fresh frontier remains available, processing, fuel packaging, lifetime, cooling and spent-fuel handling remain undecided. Finite processed-uranium cartridges with returned spent cartridges are a proposal, not approved behavior. Begin the flight proof with a clearly labeled development energy source; add the survival chain after handling works. No universal grid, radiation simulation or reactor explosion is implied.

Display loaded mass, lift margin, available propulsion power, draw, endurance and useful stopping feedback. Recommend early shortage warnings and prioritizing support/control over speed. A finite emergency reserve for controlled landing is a candidate; exhaustion, controller destruction, overload, impacts and vessel splits need explicit recovery rules before survival release. Do not promise indefinite safe hover or inherit catastrophic failure accidentally.

Persist fuel/reserve, vessel identity, custody and operating state; charge energy once per authoritative interval, not once per physics substep. Proposed unloaded/offline behavior is no travel or fuel consumption, with controlled reactivation and cleared transient pilot input on load. Verify helm/build/cargo access using transformed world positions, plus restart and disconnect behavior.

Bound vessel dimensions, block count, active bodies and geometry rebuild work. Safe high-speed travel requires collision verification, stopping-distance-based loaded-terrain lookahead and a defined fallback when terrain cannot be supplied. Distant rendering does not provide simulated chunks. The user explicitly wants unlimited speed: do not impose a configured speed ceiling or a fixed thrust ceiling that silently substitutes for one. Power and drag govern speed. The prototype suspends motion when a proposed step would outrun its bounded loaded-terrain snapshot; high-speed streaming/collision work remains, and no 200-player claim follows from one working ship.

### Decisions to settle during the prototype

1. Integrated controller is the accepted prototype package, with unrestricted finite numeric test power. Survival ratings, fuel processing and any later scalable equipment remain open.
2. Pilot controls, hover assistance, live hull editing and unattended/disconnect policy.
3. Field upkeep/capacity, item/fluid/passenger mass and supported assembly contents.
4. Emergency reserve, depletion/destruction, collision damage and recovery.
5. Cruise envelope, hull dimensions and active-vessel budget, measured in Sable.

## Regional weather and farming package

Agreed weather/crop rules live in [design](design_direction.md#regional-weather-and-farming-package); shipped yields and exceptions live in [weather](civilization-mod/weather.md).

Remaining: non-farmland habitat/timing, berries and animal-food balance, any cold-region irrigation extension, tower energy/Survival access and offline production. The [food audit](docs/history/2026-09-28-new-surv-food-balance.md) informed current one-meat drops; supplied domestic yields remain unresolved and current drops do not distinguish wild from bred animals. Viewer-centered shader weather does not provide full regional fronts.

## Advanced animal husbandry and fertilizer

Advanced animal husbandry remains future work ([E05](development_plan.md#3-checklist)). The newer sulfur + gravel-only fertilizer decision supersedes the earlier commitment to animal fertilizer ingredients and immediate removal of the clay route is implemented. Before adding husbandry, decide its products and whether the current fertilizer-only rule should change. No additional animal needs or continuous simulation are approved.

## Town workshop implementation details

The [workshop direction](design_direction.md#town-workshops-and-the-ruralurban-production-loop) is agreed; [industry](civilization-mod/industry.md#town-workshops) owns shipped recipes, grades, repairs, visuals and acquisition gates.

Remaining:

- Play repair/replacement economics; extend catalog gates to ranged/special equipment, horse armor and utility tools. Audit renewable treasure and non-workshop metal sources. Finite exploration finds remain usable.
- Define feed-supported wool/hide production without unattended grass/shearing bypasses; no continuous animal-needs simulation is approved.
- Set the boundary between coal-fired smithing and oil-powered manufacturing while keeping refinery construction reachable with coal-era inputs.
- Validate batch/startup costs, storage and freight as reasons for shared commercial locations; no town-radius bonus is approved.
- Resolve maker accent choices, drawn-mark grid (6×6 is a candidate), supported surfaces, editing/application UI and retention through repairs/upgrades.
- Audit discrete mining capabilities, special equipment and non-numeric grade effects. If a nonpositive grade occurs, decide how engine-required positive durability/mining-speed values should behave. Tune the existing distribution only from play data.
- Review worn armor on player models and alternate resource-pack silhouettes; item/held wear overlays do not verify either.

## Shared interfaces — follow-up

The approved [shared interface direction](design_direction.md#shared-machine-interface-direction) is implemented across the current machine and civic controller screens. The [implementation contract](civilization-mod/industry.md#shared-controller-interfaces) records two coal slots, automatic versus selected recipes, native ghost guidance, ambiguous-match handling, inventory migration and retained burn policies.

The [coal](concept_art/11_ui_coal_machine.png) and [liquid-fuel](concept_art/12_ui_liquid_fuel_machine.png) concepts remain visual references. The hypothetical Machine Works in the liquid concept is not a new machine or recipe. [Concept provenance](concept_art/prompts.md) retains the prompts; runtime UI artwork uses its own original master.

Remaining follow-ups: decide whether heat policies should eventually converge (continuous furnace burning versus productive workshop heat and industrial batch credits), and playtest drawer discoverability at small GUI resolutions. No new heat policy, start button, offline production or generic replacement for physical extraction/repair logic is approved.

## Bulk freight implementation details

Requested scope: [freeform mass-scaled boats, player-designed wheelhouses, generic fuel/oil engines and reusable bulk storage](design_direction.md#bulk-freight-structures). The implementation sequence and acceptance checks live in the [freight package](development_plan.md#bulk-freight--planned-implementation-package). Vessel construction/propulsion and land-store rules are approved and implemented. Powered docking, cargo-content mass and lifecycle details below remain open.

### Engine identity and historical references

Selected and implemented first: **Hot-Bulb Engine** (formerly displayed as Oil Engine). Exact standalone behavior is in [industry](civilization-mod/industry.md#oil-engine). Base its appearance on early oil engines: heavy cast bed, large flywheel, single prominent cylinder, protected hot head, copper oil lines, modest exhaust and brass lubricator. It is an early internal-combustion engine, not a steam engine or a modern high-speed diesel. Real history already supplies the desired intermediate technology.

- The Science Museum's [Akroyd engine model](https://collection.sciencemuseumgroup.org.uk/objects/co62061/akroyd-crude-oil-engine-1890) represents the design patented in 1890. The [Anson Engine Museum](https://enginemuseum.org/about/history-emergence-of-the-oil-engine) describes production engines installed in 1892 and stationary/portable applications. These support the period choice, not our gameplay balance.
- [Craftsmanship Museum's engine demonstrations](https://craftsmanshipmuseum.com/artisan/find-hansen/) describe blowlamp starting for hot-bulb engines. A brief integrated startup burner is a proposed simplification; no separate blowtorch item or manual timing minigame.
- Oil-fired steam is a valid alternative: oil supplies boiler heat. [Royal Museums Greenwich](https://www.rmg.co.uk/stories/ocean/sailing-ships-steam-power-how-industrial-revolution-changed-life-sea-forever) describes maritime transition to oil bunkering. It would fit a bulkier boiler/piston aesthetic, but a modeled water/steam loop would expand this package.
- An alternate-history vapor engine could burn refined fuel to heat a sealed working-fluid loop. This is a fictional packaging/design choice, not a claim that oil vapor is free energy or a historical Tesla invention. It adds less to gameplay than the grounded hot-bulb option.

Proposed progression: coal-fired material processing → oil engines providing compact mobile/stationary mechanical work → later nuclear/sky machinery with a separate extraordinary visual and power scale. Nuclear generation remains fictionalized future design; do not claim wireless energy, uranium fuel or reactor systems are implemented. Requiring refined fuel despite some historical engines using rougher oils is an explicit economy simplification that preserves refining demand. Oil Engine is not permission to add diesel/kerosene/gasoline products.

### Proposed physical and economic defaults

| Part | Starting proposal | Why / remaining check |
| --- | --- | --- |
| Boat | **Resolved:** any face-connected supported hull around one Helm; 8-block minimum, four deep-water contacts and 4,096-block safety cap; no shape envelope | Physical mass provides the scale cost; representative river turning room and depth still need route testing |
| Engine | **Resolved for boats:** every complete enabled Hot-Bulb Engine aboard adds fixed thrust; Helm remains separate | Multiple engines are the intended answer to larger mass; land use still needs an explicit consumer connection |
| Coal bunker | Selected and implemented on land: [rules and capacity](civilization-mod/industry.md#bulk-freight-storage) | Vessel mounting and voyage balance remain |
| Cargo tank | Selected and implemented on land and allowed aboard: [rules and capacity](civilization-mod/industry.md#bulk-freight-storage) | Powered docking remains; existing small tanks retain local buffering role |
| Transfer | 128 coal/s or 1,000 mB/s, giving roughly a minute for a full proposed module | Server-side rate limit, loaded connected endpoints and supplied engine; exact work cost remains to tune |
| Engine operation | Existing work-based fuel/lubrication policy as baseline; about 5 seconds of automatic cold start | No idle drain at neutral for first implementation; dry oil gradually reduces efficiency, matching current boats. Cold-start fuel is charged once, cannot be rerolled for refunds; no continuous temperature simulation |

Keep engine service reservoirs separate from sale cargo. The Helm never burns cargo or hides its own tank; players fill each engine by canister or its explicit local supply connection. Never silently burn bulk cargo because it happens to contain refined fuel. Check service capacity and consumption against a representative loaded round trip and refinery throughput.

### Storage, transfers and player interaction

Use the same controller-backed storage state on land and on Sable vessels. One authoritative quantity and material/fluid identity; capacity belongs to the structure definition. Existing contextual guides and cabinet UI show build requirements, exact amount/capacity and manual insertion/extraction. Coal uses a small number of deterministic pile-mesh stages; no coal item entities or individual-piece physics. The tank sight strip uses the existing fluid colors and names. Render bounds, occlusion and models follow ship transforms.

Proposed dock interaction: a short physical loading connection between explicit ports, direction selected as Load/Unload, start/stop control and visible progress. The boat must be neutral and stationary to connect. Use a bounded temporary mooring state while connected; helm departure disconnects before any propulsion. No elastic cable physics or wide-area container search. The same direct connector works between nearby land stores. A generic Oil Engine can drive this loading unit as the second concrete consumer, demonstrating reuse without building a universal shaft network.

Ordinary hoppers remain slow local coal access and existing liquid pipes handle local tank plumbing. No new item-pipe network or conveyor factory in this package. Shore pipes must not pretend to become permanently connected across a moving Sable boundary. A dedicated dock adapter resolves transformed endpoints, then delegates to the same storage operations. Capacity-check source/destination and work fuel before committing any transfer; preserve exact item/fluid counts on partial moves, competing requests and disconnect. Stop with clear reasons for full, empty, wrong liquid, missing fuel or unavailable endpoint.

Both endpoints require authority: boat owner/operator and land claim access, including revocation while a screen/connection remains open. Do not implement crew permissions by accident; current boat ownership rules remain until a separate crew policy is chosen. No automatic commerce or payment is implied by loading someone else's stock.

### Persistence and failure policy proposals

Store content once in the controller block entity; the Sable vessel stores stable local references, not mirrored cargo balances. One engine simulation per authoritative tick, with one conserved work budget shared by its consumer; never debit independently on each physics substep. Bind one consumer at a time; visual flywheel speed is feedback, not a second power simulation. Cache structure validity and invalidate on relevant edits/chunk availability; no scan of every structure every tick.

Land cargo safety is approved: [incomplete shells retain stock, stocked controllers resist mining, forced destruction loses contents](design_direction.md#bulk-freight-structures). The same store controller remains authoritative aboard. Vessel dismantling and engine service-reservoir removal policy still need decisions; do not silently apply cargo policy to every machine. Owners may edit the moving hull freely except for the Helm. Do not quietly add sinking, content-mass instability, fires or catastrophic engine failures.

Graceful save/reload, partial transfers and ordinary interruption must conserve contents. Cross-chunk crash-atomic transactions are a separate durability risk: inspect existing save/transaction guarantees and choose a bounded persisted transfer record or a documented conservative recovery policy before claiming crash safety. No offline transfer/work and no forced chunk loading. Existing-world migration is explicitly out of scope for this development phase.

### Decisions to settle before gameplay implementation

Engine identity, steel construction, freeform mass-scaled boats and land/vessel store mounting are selected. Resolve powered transfer behavior, content mass, vessel dismantling/damage and crew custody. The generated [concepts](concept_art/prompts.md#freight-boat-and-bulk-storage-concepts) are style references only: the prefab wheelhouse and measurements are superseded by player construction.

## Pickpocketing and wearable PvP flag

Deferred task L04. The [approved direction](design_direction.md#pickpocketing-and-wearable-pvp-flag--planned) exchanges wearable PvP participation for immunity to pickpocketing; unflagged players can be pickpocketed from behind. Before implementation, choose the wearable item/slot, rear interaction range and duration, eligible items and theft amount, feedback/cooldown, flag switching during encounters, and how theft/PvP interact with claims and death recovery. These details are unresolved; no extra penalties or exceptions are approved.

## Extraction installation sieges

The settled direction is [resource installations as contested objectives](design_direction.md#4-regional-land-and-materials), with player-built defenses and a decisive encounter at the site control point. Reaching it should end the encounter; do not append a long capture phase after attackers breach the defenses. The oil machine remains the intended oil objective. Manual huge-vein mining replaces the Coal Drill, so the coal objective must be reconsidered; do not silently make the preparation machine, cart or every exposed coal face a siege controller. Earlier coal mockups are superseded proposals, and no siege rule is implemented. Before coding sieges, decide the smallest server-resolved condition for reaching the control point: who may initiate it, how defenders contest access, scheduling or notice, and what happens if the installation is incomplete or unpowered. Specify whether success transfers installation access, deposit access, stored inputs/outputs, or some subset; how this relates to powered claims and manual mining; and whether construction/demolition has special limits. Preserve physical finite deposits and avoid a general faction/jurisdiction system.

The [fully modeled Oil Derrick](civilization-mod/industry.md#build-and-supply-machines) uses controller-supplied construction materials; [runtime behavior](civilization-mod/industry.md#build-and-supply-machines) owns its current ports and processing. Its construction cost and performance at 200-player scale still need review. Site siege rules and the new coal control point remain open; coal sieges are outside the manual-mining package.

## Emergent architecture and heat

The [shared heat direction](design_direction.md#shared-heat-and-thermal-comfort) is approved and the [initial implementation](civilization-mod/heat.md) establishes the baseline. Do not treat these examples as approved feature requirements or alter existing farming/machine rules implicitly.

**Core direction:** enclosure and openings affect shelter/heat retention; materials affect insulation; working furnaces/engines/heaters supply heat. The thermal foundation now implements an initial approximation. Physical openings and equipment reach affecting loading/construction remain deferred. Avoid detecting a named “workshop room” as another structure recipe. Fixed mechanical cores can coexist with freely designed buildings.

**Possible outcomes:** a workshop warms a dwelling above it; a greenhouse uses industrial waste heat; compact insulated mountain homes; sheltered shared courtyards; open loading halls trade heat retention for access; engine rooms need ventilation; exposed sky gardens and generous glazing become energy-supported luxuries. Greenhouses, ventilation penalties and climate requirements are proposals, not shipped or approved rules. Any greenhouse interaction must explicitly reconcile river-only farming and current growth/water rules.

**Remaining heat architecture choices (initial defaults are recorded in the implementation):** the smallest useful player-facing payoff; ambient conditions (region, altitude, weather); representation of enclosed spaces and openings; heat sources, retention, loss and temperature feedback; whether/how heat moves between spaces; energy allocation and waste-heat recovery; effects on players, crops and machinery; unloaded/offline behavior and moving vessels. Keep existing internal machine heat credits distinct from environmental temperature until an explicit integration is chosen. Choose simple readable behavior before an implementation architecture; avoid forcing body-temperature micromanagement or unapproved idle fuel costs.

### Thermal-field redesign — proposal, 2026-09-27

The v2 field already implements heat above regional climate, partial-block airflow, buoyant transfer, finite casing/exhaust input, atmospheric loss, delayed felt temperature and diagnostic age. [Heat](civilization-mod/heat.md#initial-thermal-field) owns current coefficients, save migration and measured limits. The old emission-scatter replacement and initial v2 rollout are completed, not future work.

Remaining proposals:

- **Section-local storage/scheduling:** active loaded sections in a fair queue, cross-section transfers counted once, complete publication and edit-local material/face invalidation. Sleeping sections should require no tick work; sources must wake without a world scan. Pause unloaded sections without loading neighbors.
- **Overload/memory policy:** retain queued heat and expose age/backlog; set measured per-section memory/time budgets. If a hard ceiling requires retirement, account settled differences as atmospheric loss rather than evicting live sources/plumes by map order.
- **Background refinements:** decide slower unheated-ground response, weather/background rebasing of stored absolute temperature, and sustained negligible-heat retirement with explicit loss accounting. These are not current behavior.
- **Scale acceptance:** target an average below 2 ms per server tick and active sections no more than 3 seconds behind under representative 200-player load. These are unverified targets, not guarantees. Include helmet sampling/network cost.
- **Physical acceptance:** compare indoor/outdoor Foundries, open/blocked/extended cut-block flues, doors and insulation; sample actual torso air at the flue and decreasing distances. Verify paid heat equals storage plus atmospheric loss through shutdown, rain, edits, unloaded sections and reloads, including two distant installations and idle/productive jobs. Extend existing fixtures where behavior changes; do not repeat the whole rollout for each tuning edit.

Future storage/solver replacement needs equivalent conservation and lifecycle checks before switching player/helmet reads, a versioned save policy and a representative load trial. Exact ground lag and memory quota remain unsettled.

**Other deferred opportunities from the brainstorm:** movable homes/workshops/markets; canals, dredging, locks, tunnels and ship lifts that change useful routes; expedition frontiers that require infrastructure to inhabit; player-authored construction plans reproduced with real materials and energy; visible regional services and recognizable working skylines. These expand the same physical freight, construction and energy principles rather than introducing a separate town/status score. Each needs its own scope decision; flooding/destruction rights and construction permissions remain unresolved.

Desired emergence: discovery motivates a useful place; freight connects it; trade supports more capability; capability lets players reshape routes and build at larger scales. A port or town should arise from those relationships, not a prescribed settlement category.


## Unified physical foundation — proposed implementation plan

Requested planning scope: share material properties, mass, heat and energy rules across stationary systems and Sable vessels. The user approved building this architecture. Individual vehicle/recipe/machine balance consequences remain separate decisions. The first material/geometry/inspection stage and static Sable proof are now [implemented](civilization-mod/physics.md); dynamic mass and moving thermal state remain pending. Existing behavior remains authoritative in [heat](civilization-mod/heat.md) and [vehicles](civilization-mod/vehicle-physics.md).

### Boundary and ownership

Civilization owns materials, occupied volume, stored thermal state, fuel/work budgets and gameplay policies. Sable remains the rigid-body solver and owner of vessel pose, velocity, collision and inertia calculation. A narrow adapter supplies supported physical properties, reads transforms, and applies bounded forces/impulses. Do not create a competing motion simulation or take over Sable's native lifecycle.

There are three different quantities: physical mass; heat capacity (mass times specific heat); and stored chemical energy. Temperature is derived from thermal energy and capacity. They must not share an ambiguous `mass` or `energy` scalar. Existing ThermalField.mass is a game-tuned thermal capacity, not kilograms; existing machine heat credits are productive work counters, not temperatures.

### Five shared contracts

1. **Material profile.** Data-driven density, specific heat and thermal conductivity, with a small explicit fallback family set: air, water/liquids, timber, masonry, glass and metal. Tag defaults with explicit block/item/fluid overrides and deterministic precedence; caches invalidate on reload. Combustible resources have a separate fuel profile with energy per item or fluid quantity. Material realism is directional; effective gameplay coefficients and engine/unit conversion stay explicit. Do not assume a kilogram figure from the current capacity constants.
2. **Physical sample.** Resolve occupied volume, mass, heat capacity and six face contacts from a block/state plus block-entity contents. Full cubes, native slabs and the eight cut-block cells share this resolver. Sum component mass/capacity; derive face resistance from actual occupied paths rather than averaging conductivity. Keep the thermal storage lattice at one block initially, with face coverage/open paths informed by half-block geometry. Isolated eighth-pieces within one block therefore still share an approximate temperature; this limitation is explicit. Air does not become rigid collision mass, and block cargo, inventory items and tank fluid must not be counted twice. Block volume, buoyancy/displaced volume and contact coverage are distinct.
3. **Physical address.** Dimension + stationary-world or vessel UUID + local block position. Use local neighbors for a vessel's interiors and transform positions only when querying world climate, claims, ports or external contact. Ordinary world-grid code must never treat Sable plot coordinates as geographic locations. Assembly/disassembly/splits/reload must transfer thermal state exactly once; deletion/discard has an explicit energy policy. Vessel motion cannot leave its heat behind or duplicate it at the destination.
4. **Thermal store and exchange.** Keep temperature/reference and capacity semantics explicit, using one documented internal unit system (prefer kg, m, seconds, joules and kelvin; Fahrenheit only for display). Pair exchange follows Q = G × (Ta − Tb) × dt; G includes conductivity, thickness and contact area. Apply equal/opposite energy changes, stable substeps or bounded outgoing budgets, no overshoot/negative capacities. Mixing is an explicit effective air-exchange policy; outdoors is an external reservoir, not a room bonus. Separate actual mechanical geometry from this cheap thermal approximation. Track capacity changes and material replacement deliberately; prevent free heating/cooling via repeated cutting, combining, assembly or cargo changes.
5. **Fuel/work transaction.** One authoritative consumption result allocates chemical input between useful work, machine/environment heat and explicit exported losses. Existing work-credit counters stay separate during migration. Request work on the server tick; physics substeps consume a published power/force allowance without re-burning fuel. Conversions cannot create energy, lubrication is not automatically fuel, and thermal energy cannot convert straight into mechanical work without an explicit machine. The atmosphere, deleted material and future exhaust are named sinks/sources in diagnostics rather than unexplained energy changes.

These contracts should be small concrete types and operations, not a general graph framework or mandatory component system for every block. Item/fluid/block storage continues to own its real contents. Recompute cached physical totals on mutations instead of mirroring authoritative inventories.

### Sable verification gate

The pinned local release contains block-property definitions and mass/inertia trackers, and BoatSystem already uses its pose, mass and impulse APIs. Prove which public hooks support static material properties, per-position mixed cut-block mass, dynamic cargo changes, center of mass and mass-cache invalidation against this exact dependency before choosing an adapter. Do not promise dynamic cargo fidelity based only on static block-state configuration. Keep compile/API experiments in the existing laboratory. If a hook is missing, retain an explicit approximation or defer that feature rather than fork the engine silently.

Current boat propulsion applies fixed thrust per running engine, so Sable block mass reduces acceleration and speed. This does not authorize inventory/tank content mass, draft penalties, realistic capsizing or structural failure; those remain separate balance decisions.

### Work order and acceptance

1. **Extract common properties and geometry.** First deliverable is a material inspector showing material, occupied volume, physical mass, thermal capacity, conductivity and active coordinate space. Route heat property lookup through it; prove static Sable property integration in the lab. Preserve current thermal/fuel/boat balance using clearly labeled compatibility coefficients. Cover full block = two slabs = eight eighths, mixed-material sums, fallback materials, reload invalidation and no contents double-counting.
2. **Move thermal state safely.** Introduce domain-aware addresses and capacity/reference handling, then migrate saved thermal state without temperature jumps or invented energy. Exercise one heated moving vessel, boundary crossing, save/reload and supported assembly lifecycle. External ship/world thermal coupling starts bounded and conservative; do not simulate contact between every surface of every body. Do not enable unrestricted onboard machinery until its coordinates and lifecycle are audited. Block/cargo removal must define whether heat follows the removed matter or leaves through a recorded sink; item temperature metadata is not assumed.
3. **Unify energy accounting with two consumers.** Convert one Foundry and one Oil Engine using explicit legacy-credit conversion, preserving existing recipe throughput and fuel balance first. Show input energy = work + stored heat + released heat + explicit losses within tolerance. Handle partial work, interruption, full output, fractional fluid and multiple physics substeps without duplication. Only then adopt shared internal machine heat/cooldown and extend to other machines.
4. **Add physical consequences individually.** Decide cargo mass/handling, tank-fluid thermal inertia, heat exchangers, exhaust and other consumers one at a time. Existing engine, dock and cargo work should use this foundation. Electricity, radiation, pressure/steam, fire/melting, structural stress and nuclear systems remain later modules with their own gameplay approval.

### Runtime budget and test policy

No world-wide scan, per-block entity or forced chunk loading. Simulate only active thermal areas and explicitly active machines/bodies; sleeping regions and unloaded/offline behavior must be defined. Cache geometry/materials and invalidate on actual edits; content updates invalidate aggregate cargo totals. Account separately for loaded cells, interface faces, active vessels, pending work and clients inspecting heat. Stagger bounded work fairly rather than dropping heat silently when a global cap is reached. Profile representative workshops plus moving freight on the intended server; 200 connected players is not itself a physics benchmark.

Use a few meaningful invariant tests plus the normal gameplay suite when behavior changes, and the existing dedicated Sable save/reload harness for moving-state changes. UI/debug-only work uses focused checks. No new exhaustive framework is needed.

### Decisions before behavior changes

Recommend no new player meters or engineering chores: keep the current thermometer, simple stable vehicle controls and physical build guides. Select numerical material/energy scales and migration policy during phase 1; record the calibration separately from the architecture. Approve cargo penalties and internal machine-temperature rules before enabling them. Do not claim strict mass conservation across all vanilla recipes: first audit the recipe paths affected by physical inventory mass. Calories remain their existing player resource; universal energy accounting does not add metabolism or renewable biomass fuel.

## Directional chunk loading for warp travel

The user approved trying corridor-first request ordering. That prototype is now implemented; its exact scope and limits live in the [vehicle reference](civilization-mod/vehicle-physics.md#directional-chunk-request-order). It preserves the full requested footprint and changes only pending request order. Initial user feedback reports much smoother travel and approximately 50% CPU at 200 blocks/s versus approximately 100% before; see [status](docs/status.md) for evidence boundaries. Matched user-world performance measurements remain: useful-chunk readiness, terrain-hold time, p95/p99 server ticks, generated/requested chunks and queue size, for both saved terrain and new generation. Earlier JFRs were dominated by DH, so C2ME ordering may improve continuity without being the largest CPU reduction.

Still proposed: narrow warp-specific no-tick coverage and redirect a fixed chunk budget ahead. This requires matching retention/unload rules, preserved nearby physics coverage and generation dependencies, overlapping-player/ship coverage, and turn/stop recovery. Changing only traversal while retaining square `isInRange` would leave unwanted chunks pinned. Vanilla simulation/player tickets and other players still impose their own footprint. The airship now separately prepares a bounded padded corridor, publishes immutable readiness and brakes before its boundary; see the vehicle reference for implemented limits. A single chunk-wide line is insufficient for the hull. Narrowing or replacing C2ME/player coverage remains unapproved and unimplemented; the added readiness corridor does not do that.

Inspection of installed C2ME NeoForge `0.4.0-alpha.0.122` confirmed spiral source traversal, bounded no-tick load futures, round-robin sources, square retention and ticket-derived scheduling priorities. Local concurrency remains its documented default of 21 and global workers 7. Reference: [C2ME NeoForge](https://github.com/RelativityMC/C2ME-neoforge) and its 1.21.1 upstream [no-tick loader](https://github.com/RelativityMC/C2ME-fabric/blob/dc88fbf54af64c3f0a2226c8bfa1143209e3d9a0/c2me-notickvd/src/main/java/com/ishland/c2me/notickvd/common/PlayerNoTickLoader.java). Installed bytecode supplied the compatibility hook assessment.
