# Transport hierarchy

Status: the modal hierarchy and first coal-powered cargo-boat direction are agreed design, not implemented transport behavior. Recipes, energy budgets, vehicle dimensions and technical foundations remain unspecified. See [systems_blueprint.md](systems_blueprint.md) and [regional_geography.md](regional_geography.md).

## Agreed progression

| Mode | Role | Relative position |
| --- | --- | --- |
| River transport | Early freight backbone connecting productive river valleys and settlements | Earlier and cheaper than rail |
| Trains | Later ground freight | Always more expensive than river transport |
| Airships | Advanced freight, including sky access | Most expensive, greatest bulk capacity and fastest |

The user chose trains for ground freight; do not introduce an intermediate cart/truck progression as an assumed requirement. Personal walking and small deliveries remain distinct from bulk freight. Whether any additional local transport is useful remains a separate question.

Preserve the ordered cost hierarchy when assigning numbers. What constitutes comparative cost still needs definition: construction, operating energy, maintenance and cost per useful delivered load. Airships carrying more cargo must not accidentally make them the cheapest freight option at full utilization. No exact unit-cost multiplier, speed, capacity or fuel assignment is approved.

## Proposed reasons to choose each mode

- Rivers offer inexpensive delivery along naturally available routes. River geography determines access and makes landings useful.
- Rail requires investment but connects inland sites and creates routes that rivers do not provide. This geographic reach, rather than lower cost than boats, gives trains their role. Rail speed and capacity relative to river craft remain open.
- Airships offer maximum speed and bulk, access over difficult terrain and service to sky destinations. Their premium cost makes the benefit a consequential choice rather than replacing all cheaper transport.

These tradeoffs are design proposals supporting the agreed hierarchy. Exact route restrictions, handling and infrastructure remain open.

## Shared rules and dependencies

Cargo stays in physical vehicle inventories. Players use standard slots, ordinary stacks of 32 and lower limits for bulky industrial resources. Vehicles gain capacity through additional slots; filled portable container items cannot bypass carrying limits. Disabled remote ender storage and local death recovery preserve the need to transport goods.

Vehicle energy must follow the no-renewable-industrial-power rule. Early river propulsion needs an explicit design; do not assume vanilla boat behavior already meets our energy rules. Manual personal movement and industrial freight power require a clear boundary. Coal, oil and uranium are established industrial stages, but no one-to-one assignment of boats/trains/airships to those fuels is approved. Reactors do not automatically imply nuclear airships.

World layout must support navigable rivers before we rely on them as the early freight backbone. Determine river continuity, width, depth, obstructions and useful landing sites in a sample region. River-dependent farming and river navigation share geographic data but have different requirements.

Ownership and access apply to terminals, loading inventories and vehicles. Define cargo custody, loading, unloading, vehicle loss, disconnection and restart before implementation. Greater capacity must not mean unbounded active entities or forced chunk loads. Physics/airship integration remains deferred and unvalidated.

## Next specification

### First river-freight design — agreed core, open details

The agreed core is a simple coal-powered cargo boat built from early materials, with a fuel slot and a large cargo inventory. Players load, pilot and unload it directly at an ordinary bank or simple landing; no port machinery is required. Fuel exhaustion stops propulsion while cargo remains aboard. A compact steam launch/workboat is the accepted visual direction, without detailed boiler-pressure or water management. Exact capacity, fuel rates, dimensions and implementation remain open. Lifecycle and energy-accounting details below are still recommendations unless separately marked agreed.

**One small coal-powered cargo boat.** Use a fixed craft with steering, a fuel slot and one cargo inventory. Build it from Tier 1 materials; do not require oil or an advanced engine to establish early trade. A fixed craft instead of a physical-ship construction framework is the proposed implementation scope. Exact recipe, model and implementation foundation remain open. This design does not select or integrate a vehicle mod.

Player loop: bring the craft alongside a bank or simple landing, load actual goods, add Mineral Coal, pilot it to another landing and unload. Reuse ordinary inventory interactions. Shore-side chests can serve as the first warehouse. Do not require a port controller, crane or terminal multiblock merely to make the first delivery. Loading permission still follows the relevant owner and inventory rules.

Give it substantially more usable cargo slots than a player, with the same item stack limits. Exact slots and operating rates remain open. Evaluate a representative timber load, provisions and a Tier 2 bulk-resource shipment before assigning capacity. Keep an empty return trip in the cost calculation.

Recommended power abstraction: coal purchases a stored propulsion-energy budget, consumed while the engine operates, with no fuel use when shut down. No boiler pressure, water supply or per-part engine simulation in the first design. Starting and stopping must preserve fractional remaining fuel energy; toggling the engine must not create energy. Fuel rate and its relationship to speed remain tunable.

On fuel exhaustion, propulsion stops and the cargo remains aboard. Proposed stationary failure behavior avoids a current simulation. The crew can refuel using carried reserves or an ordinary supply trip. No automatic towing, teleport-home button or calorie-powered freight fallback is implied. Ordinary personal boats remain a separate open design question.

Keep the craft persistent when a player dismounts or disconnects. No unattended autonomous route service is required initially. Unloaded vehicles pause; ownership and inventory survive save/restart. Specify loaded-but-unpiloted behavior, loss/damage, land recovery and permission coverage before implementation. A loaded vessel cannot become an inventory item carrying its contents. Empty-craft portability also needs a decision because it could bypass geographic barriers.

### Navigable-world requirements

Agreed direction: connected rivers must be wide and deep enough for useful freight routes, with places to build landings. Not every tributary must be navigable. Numerical geometry and generation method remain open.

The sample world must demonstrate an actual usable water route between agricultural production and an exchange/processing settlement. Connected main channels need sufficient width, depth, turning room and overhead clearance for the selected craft. Set numerical dimensions after defining its size; not every tributary must be navigable.

Test both directions, bends, landing approaches, terrain boundaries and bridges. Provide natural places where players can build simple landings without mandatory large excavation. Floodplain farming bounds and boat navigation share the same landscape but remain distinct checks. Route continuity cannot be inferred from a biome label alone.

For the first sample, prefer naturally connected navigable reaches rather than requiring locks or simulated river currents. Canals, dredging and river engineering may become later infrastructure choices; their costs and permission rules remain open. Artificial navigability must not create agricultural eligibility outside the natural floodplain.

### Readiness checks for later implementation

- A player loads and delivers real cargo between two shore inventories with no additional machine chain.
- Fuel consumption is explicit, preserves partial energy and stops when the engine is off.
- Exhaustion and disconnection preserve cargo without delivering it for free.
- Restart and concurrent inventory actions do not duplicate the craft or cargo.
- Breaking, carrying or nesting containers cannot bypass the intended freight journey.
- The sample route is navigable in both directions, and measured delivery cost remains below the eventual rail/airship targets.

These checks describe the intended package, not completed tests. Cost, timing, cargo capacity and river geometry must be evaluated together. The core boat direction is accepted; detailed numerical specifications can wait while whole-mod planning continues.

Define the first river-freight interaction: craft and propulsion, practical cargo space, loading/unloading, and where players can land. Start with the player experience and energy source; tune cost and distance together later. Track train and airship targets as constraints without implementing all three at once.

Broader passenger travel, dimensions/portals, elytra and teleport restrictions remain unresolved. Check these against the hierarchy before selecting final speeds and route lengths.
