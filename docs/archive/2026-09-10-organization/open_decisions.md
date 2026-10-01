# Open decisions and simplification review

This is the only active register of unresolved design choices. Agreed rules live in [design_direction.md](design_direction.md); implementation work lives in [development_plan.md](development_plan.md). Entries are gaps, not permission to reopen settled intent.

## Consolidation findings

The previous documents accumulated new decisions above old alternatives. They repeated the same rules, mixed implemented behavior with plans, and carried incompatible “next task” instructions. The original drafts are preserved in the [dated archive](docs/archive/2026-09-10-pre-consolidation/README.md).

Resolved using the conversation's latest decisions:
- Sellers can remove structures; controller energy stays locked.
- Relocation is one week; ordinary power failure has no grace.
- Physical coal/oil/uranium are currencies; abstract-money issuance is no longer the plan.
- Trusted members have full shared-asset access; custom member roles are unnecessary.
- River freight and its coal boat are selected; transport mode is not an open-ended choice.
- Current implementation baseline is 0.11.5-dev; implementation status is tracked in the backlog and mod README.

## Simplification recommendations

These recommendations consolidate implementation choices; they do not silently add game rules.

| Reuse | Application | Avoid adding now |
| --- | --- | --- |
| Physical resource inventories | Fuel is usable energy and payment; shops reserve actual stock | Bank accounts, energy certificates, minting or fixed exchange-rate services |
| One owner reference: player or group | Claims, shops, vehicles and shared machines | Separate company/town/guild frameworks or per-member roles |
| One geographic query | Crop/forestry eligibility, habitat and deposit generation | Independent region systems or climate/soil/hydrology simulation |
| Explicit fuel consumers | Cooking, machines, claims and vehicles use a common resource vocabulary | A global power grid just to operate the first consumer |
| Atomic exchanges with reserved stock | Shop trades and paid land transfer share conservation/retry rules | A universal contracts engine, payroll, loans or courts |
| Map plus contextual inspection | Market discovery, property visibility, suitability and prospecting | Separate dashboards for each activity |

Reuse storage and accounting without forcing all consumers to behave identically. Current kiln heat burns while lit; a claim loses protection at zero; a boat stops; depletion leaves a player a recovery route. These differences are useful rules, not duplication to erase.

Keep short, direct controls: supply fuel, inspect requirements, load goods, set an offer. Persistent implementation work is justified where it enforces an agreed rule, especially claims and transactions. Do not build a general framework before concrete consumers need it.

## Decisions that block coherent implementation

| ID | Decision | Why it matters / simplest candidate to evaluate |
| --- | --- | --- |
| D01 | Spawn, starting items and accessible subsistence | A fresh solo player must reach food, tools and claim inputs without existing buyers. No starter kit or no-kit policy is approved |
| D02 | Remaining vanilla boundary | Household cooking is implemented: food heat recipes use the stove; fire-cooked drops and villager baked/cooked supply are gated; cold assembly and finite loot remain. Renewable metals/fuels, other recipes, enchanting/repair, XP, mobs, redstone and automation still need review |
| D03 | Passenger travel and dimensions | Decide portals, personal boats/mounts, elytra and teleport commands before final distances. Keep-inventory is already superseded by local recovery |
| D04 | Region representation and generation | River width/depth/continuity, floodplain width/height, crop coverage, species conditions, stable forestry suitability, prospecting and migration. Candidate: a few overlapping geographic properties, not a biome per activity |
| D05 | Fuel forms, packages and conservation | Select oil/uranium payment forms and physical quantities. Energy density, price and cargo volume are distinct; compressed blocks/tanks must not bypass freight. Existing 16-stack items stay unchanged in 0.10.0; later per-item tuning remains open |
| D06 | Claim shape, power and denomination | Define billed area/volume, vertical reach, power clock across logout/unload/downtime, accepted fuel and takeover valuation. Candidate: one accepted denomination per controller avoids comparing market-priced mixed fuels; not approved |
| D07 | Takeover scheduling and failure | Define monthly eligibility, simultaneous buyers, top-ups, resizing, cancellation and reserve exhaustion during the locked one-week handover. No grace and no fixed multi-fuel exchange rate may be silently invented |
| D08 | Claim loss and movable asset rights | Who may use a shop/vehicle on unclaimed land, how reacquisition works, and treatment of left-behind goods. Prevent individually protected containers from making paid land protection irrelevant; preserve agreed owner-only death recovery |
| D09 | Group administration | Invitation/removal/leadership/disbanding and personal-to-group transfers. Candidate: owner-only administrative powers, explicit asset assignment and full member use; not yet approved |
| D10 | Recovery and vehicle lifecycle | Hazardous death placement, inaccessible claims, cleanup/repeated deaths, XP and respawn health; boat damage, disconnection, dismantling and empty-craft portability |
| D11 | Shop and map implementation | Storage attachment, physical fuel/payment custody, stale listings, unloaded shops, publication/sharing, concurrent transactions and retries. No remote reservation or automatic route service is approved |
| D12 | World lifespan and operating budget | Choose reserve horizon and expansion/reset policy before a permanent world. Set hardware, active simulation and maintenance budgets before promising 200-player capacity |

Do not ask the user to resolve all twelve at once. Pick the decisions required by the next useful package; unrelated gaps can remain open.

## Four interactions requiring explicit reconciliation

**Fuel currency versus personal freight limits.** Higher fuels should carry greater value per unit, yet a pocket should not supply a large industrial operation for an excessive period. Tune stack limits, fuel payloads and consumer rates together. This is not solved by declaring every high-tier item unstackable.

**Fixed-price takeover versus continuing upkeep.** The buyer pays against the deposit-time reserve but receives what remains a week later. Clarify allowed burn/top-ups and exhaustion before coding. Controller movement, claim resizing or an attached consumer cannot provide an alternate withdrawal path.

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
| Mining | Surface clues lead to a visible seam; material-specific impacts and restrained dust make extraction tangible; lamps, galleries and loading places express development | Actual terrain, mined material and extraction state; new vein presentation remains to be designed |
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

The fictional chain can be ground-extracted fuel → supplied sky generation/transmission → regional receiver → useful effect. Explain why elevation improves the service instead of imposing a player-rank gate. Reactor/generator choice and transmission formulas remain unspecified; cargo airships need not be nuclear-powered.

Extremely fast airships are now an agreed sky status symbol and major outlet for energy. Specify how extraordinary speed consumes energy, what affects cargo capacity, and the server/client limits on safe travel. A sharply rising energy cost at extreme speeds is a candidate, not an approved formula. Do not assume the fastest prestige craft must also carry the maximum possible cargo; the broader airship mode remains the fastest, highest-capacity and most expensive transport tier.

Oil/uranium extraction and processing recipes; the proposed steel/component/alloy vocabulary; train/airship implementation and energy; supplied monumental construction; weather radius/overlap/shortage; sky architecture, lift and access; optional PvP stakes; world name and artistic vocabulary.

Natural-water-only fishing, stable forestry after clearing, empty boat portability, automatic route services and jurisdiction-like rental/toll systems are not approved mechanics. Ideas for research progression, provenance, detailed wear, NPC labor, banking and general contracts remain original-document ambitions or deferred candidates, not a launch checklist.

## Recommended next move after this review

Everyday material rules and household cooking are now implemented. Next is the regional foundation: settle the minimum natural river/floodplain representation and forestry suitability, then implement consistent geographic queries and useful player feedback. Protection/custody decisions can wait until groups, land, shops and recovery become the active package. Siege brainstorming is deferred and has not become a mechanic specification.
