# Status, Power, and Civilization — Core Game Design

## I. Philosophy

### 1. Core Thesis

The game is built around a simple proposition:

> **Status is the social shadow cast by real power.**

Status should not originate from likes, XP, followers, achievements, or an arbitrary prestige score. Those are representations of status, not its foundation.

The foundation is **capability**: the ability to cause meaningful changes in the world and in the range of actions available to other players.

At the deepest level, civilization is constrained by:

- **Energy** — the capacity to perform work.
- **Matter** — the physical resources that can be transformed.
- **Information** — knowledge about how to transform and coordinate.
- **Organization** — the ability to coordinate many people and systems.

Everything else—wealth, armies, cities, corporations, political offices, monuments, luxury goods, titles, fame—is built on top of these primitives.

---

### 2. The Grounding Chain

The fundamental hierarchy is:

**Energy → Surplus → Capability → Dependency → Power → Status**

#### Energy
Life and production require continuous inputs. Food, fuel, labor, mechanical power, electricity, and other forms of usable energy are the ultimate physical constraints.

#### Surplus
A society becomes powerful when it can produce more than is required for immediate survival.

Surplus allows specialization, construction, warfare, science, administration, art, and luxury.

#### Capability
Stored resources are less important than the ability to produce future resources.

A warehouse full of grain is wealth.

A functioning agricultural system capable of producing grain indefinitely is power.

Power is therefore better understood as a **flow** than a stock.

#### Dependency
A player becomes structurally important when other players depend on systems they control.

Examples:

- food production
- water
- transportation
- energy
- tools
- manufacturing
- trade routes
- military protection
- capital
- information
- logistics
- institutions

#### Power
Power is:

> **Control over other people's feasible futures.**

If a player controls something necessary for another player's plans, that player can alter the other's available choices.

Power therefore emerges from control of **bottlenecks**.

#### Status
Status is society's recognition of underlying capability, scarcity, power, achievement, or association.

It should mostly be an emergent human judgment rather than a developer-assigned score.

---

### 3. Positive-Sum Civilization, Zero-Sum Rank

The game should distinguish **power creation** from **status competition**.

Civilization can be strongly positive-sum.

A new irrigation system can double food production. A railway can expand trade. A technological breakthrough can increase the total amount of useful work civilization can perform.

But relative status remains positional.

Only one person can be the most powerful.

Only 1% of players can occupy the top 1%.

The central tension of civilization is therefore:

> **People cooperate to enlarge the pie while competing over who controls the enlarged pie.**

This tension should drive the game.

---

### 4. Status Must Remain Grounded

The game should avoid directly converting social attention into power.

A player cannot become powerful merely because many people clicked a button.

Claims of importance must eventually survive contact with reality.

If someone is considered powerful:

- Can they feed people?
- Can they build?
- Can they defend territory?
- Can they mobilize labor?
- Can they move goods?
- Can they survive an embargo?
- Can they replace destroyed infrastructure?
- Can they coordinate people at scale?

The world should continually force social claims to prove themselves materially.

---

### 5. Costly Signals

Status displays should arise from real economic sacrifice.

A palace is impressive because players understand what was required to build it:

- quarrying
- transportation
- labor
- food
- engineering
- tools
- security
- time
- opportunity cost

A monument is therefore a **proof of surplus and organizational capacity**.

Luxury goods should often have little functional advantage. Their significance comes from scarcity, provenance, difficulty of production, and visible opportunity cost.

The developer should create the conditions for status symbols to emerge rather than declaring which objects are prestigious.

---

### 6. Entropy and the Reproduction of Power

Power must never become permanent for free.

All complex systems should require continuous maintenance.

Examples:

- humans consume food
- machines wear
- buildings decay
- roads deteriorate
- armies require supplies
- organizations require administration
- territory requires defense
- information becomes obsolete
- infrastructure requires replacement

The larger and more complex a system becomes, the more energy it must consume merely to continue existing.

This creates a fundamental rule:

> **Power must be continuously reproduced.**

A successful empire is not something a player simply owns. It is a structure that must successfully recreate itself every day.

---

### 7. Productive vs. Extractive Power

Power can be used to increase the productive base or merely extract from it.

A ruler may tax society and use the surplus to build:

- roads
- irrigation
- storage
- defense
- ports
- public infrastructure

This can make subjects more productive and increase the ruler's future power.

Another ruler may consume the same surplus on personal luxury and monuments.

The second ruler may initially appear more prestigious, but excessive extraction can destroy the productive system on which that prestige depends.

Legitimacy can therefore emerge naturally from material incentives rather than from a morality meter.

---

### 8. The Ultimate Measure of Power

The most meaningful theoretical measure of a player's importance is:

> **How much of civilization would have to reorganize if this player and the systems they control disappeared?**

A highly connected farmer, engineer, military commander, industrialist, financier, logistics operator, political leader, or infrastructure owner can all possess power in different forms.

The game should allow these forms of power to compete and interlock without reducing them to a single arbitrary stat.

---

## II. Technical Design

### 1. World Primitives

The simulation should be built from a small number of hard primitives.

#### Agents
Players and possibly simulated workers.

Agents require continuous resources to survive and act.

#### Matter
Physical resources with location, quantity, ownership, and transformation rules.

Examples:

- food
- water
- timber
- stone
- metals
- fuel
- manufactured goods

#### Energy
Every meaningful physical transformation requires energy.

Examples:

- biological labor
- animal power
- fire
- water power
- mechanical power
- fuel
- electricity

#### Space
Resources, transportation, terrain, distance, chokepoints, and geography must matter.

Resources should not teleport.

#### Time
Production, transportation, construction, maintenance, learning, and organization all require time.

---

### 2. Production Model

Every productive process should approximately follow:

**Inputs + Energy + Labor + Capital + Knowledge + Time → Outputs**

Example:

**Ore + Fuel + Furnace + Worker Time + Smelting Knowledge → Metal**

Complex production chains create specialization.

Specialization creates trade.

Trade creates infrastructure.

Infrastructure creates bottlenecks.

Bottlenecks create dependency.

Dependency creates power.

---

### 3. Stocks vs. Flows

The simulation should explicitly distinguish:

**Stock:** what a player currently possesses.

**Flow:** what a player can reliably produce, move, defend, or coordinate per unit of time.

Examples:

- 50,000 food stored = stock
- 5,000 food/day production = flow

- 1,000 soldiers nominally owned = stock-like capacity
- ability to continuously supply 600 soldiers in the field = actual military flow

Long-term power should depend more heavily on sustainable flows than accumulated inventories.

---

### 4. Needs and Survival

Agents consume resources continuously.

At minimum:

- food
- water
- shelter
- energy
- safety

More advanced populations may require increasingly sophisticated goods and infrastructure to maintain productivity.

Scarcity should reduce capability rather than merely subtracting abstract happiness points.

A starving workforce should physically produce less.

A fuel shortage should stop machines.

A broken transport network should prevent inputs from reaching factories.

---

### 5. Logistics

Logistics should be one of the primary generators of power.

Every resource has:

- origin
- destination
- mass or volume
- transport cost
- transport time
- storage requirements
- risk of loss

Transportation infrastructure reduces these costs.

This makes roads, ports, bridges, railways, canals, warehouses, and shipping networks naturally strategic.

A player controlling a bridge does not receive "+20 power."

They are powerful because traffic physically depends on the bridge.

---

### 6. Bottlenecks and Dependency

The game should track production graphs internally.

For any player's activity, the simulation can identify upstream dependencies.

Example:

`Farm → Grain → Mill → Flour → Bakery → City`

If one player controls the only mill, downstream production depends on that player.

A useful hidden analytical measure is **dependency centrality**:

- How much production passes through systems controlled by the player?
- How many agents depend on those systems?
- How difficult are those systems to replace?
- How long would substitution take?
- What fraction of civilization loses capability if the node disappears?

This metric can help simulation, AI, analytics, and balancing.

It should not necessarily be shown directly to players.

---

### 7. Substitutability

Monopoly power should depend on replacement difficulty.

A bottleneck is powerful when:

- alternatives are scarce
- alternatives are distant
- switching is expensive
- replacement takes time
- specialized knowledge is required
- infrastructure is unique

Power therefore becomes dynamic.

If competitors build another bridge, the first bridge owner's leverage falls automatically.

No artificial debuff is required.

---

### 8. Property and Control

The game must distinguish different forms of control.

Possible mechanisms include:

- direct ownership
- contractual rights
- leases
- debt claims
- political authority
- military occupation
- organizational control
- access permissions
- joint ownership

This allows power to become more abstract as civilization develops.

A player may control a factory without personally operating it.

A government may control a road without "owning" every shipment.

A creditor may influence productive assets through debt rather than physical possession.

---

### 9. Organizations

Players must be able to create persistent organizations.

Examples:

- firms
- guilds
- militaries
- towns
- states
- banks
- trade associations
- religious institutions

Organizations should solve coordination problems but introduce overhead.

As organizations grow, they incur:

- communication costs
- administrative labor
- corruption or leakage
- principal-agent problems
- internal politics
- slower decision-making

Scale should therefore produce both power and fragility.

---

### 10. Contracts and Markets

Players should be able to construct agreements rather than relying entirely on fixed developer systems.

Core primitives:

- exchange
- employment
- loans
- leases
- insurance
- supply agreements
- alliances
- taxation
- tribute
- access rights

Money may emerge naturally or be introduced as infrastructure for exchange.

The critical design principle is that money represents claims on real productive capacity rather than functioning as the fundamental source of power.

---

### 11. Violence

Violence is another mechanism for controlling feasible futures.

Military power should depend on logistics.

Armies require:

- people
- food
- weapons
- equipment
- transportation
- replacement parts
- information
- command structures

A wealthy army without supply lines should fail.

Territorial conquest should therefore create new logistical and administrative burdens rather than automatically increasing power.

---

### 12. Information and Knowledge

Knowledge increases the efficiency with which matter and energy can be transformed.

Information can include:

- recipes
- engineering knowledge
- maps
- resource locations
- production methods
- scientific discoveries
- market information
- military intelligence

Knowledge can create power when it is:

- scarce
- useful
- difficult to reproduce
- protected
- embodied in skilled people or institutions

Information should gradually become another major layer of civilization without losing its connection to physical production.

---

### 13. Maintenance and Decay

Every persistent asset should have a maintenance function.

Conceptually:

`Net Capability = Gross Productive Capacity - Maintenance Burden`

Possible maintenance costs:

- resource consumption
- replacement parts
- labor
- administrative effort
- defense
- capital renewal

Neglected systems should degrade gradually rather than disappearing instantly.

This creates realistic decline.

A civilization can become too large or complex to sustain.

---

### 14. Status Layer

The game should avoid a universal prestige currency.

Instead, status should be conveyed through observable facts:

- visible structures
- quality of possessions
- control of land
- military followers
- organizational position
- rare objects
- productive networks
- historical achievements
- endorsements
- provenance
- public ceremonies
- reputation

Different cultures or communities may value different signals.

The important requirement is that most durable status signals remain connected to expensive or difficult underlying achievements.

---

### 15. Provenance

Important objects and structures should preserve history.

Possible metadata:

- creator
- original owner
- previous owners
- battles involved
- age
- location history
- major events
- production origin

This allows culturally important artifacts to emerge without predefined prestige values.

An otherwise useless object can become extraordinarily valuable because of its history.

---

### 16. Anti-Instagram Design Principle

Attention alone must not generate material capability.

Social visibility may affect:

- recruitment
- trust
- coordination
- diplomacy
- trade
- cultural influence

But it cannot directly create food, energy, weapons, infrastructure, or labor.

Popularity can help a player mobilize real resources, but the resources must still exist and be physically organized.

---

### 17. Hidden System Metrics

The simulation can internally compute several measures without turning them into player-facing scores.

#### Productive Capacity
Maximum sustainable output per unit time.

#### Surplus
Production remaining after survival and maintenance costs.

#### Dependency Centrality
How much of other players' activity depends on systems controlled by an actor.

#### Replacement Cost
Resources and time required for society to replace a player's functions.

#### Network Reach
How many important economic or organizational systems a player influences.

#### Resilience
How much production remains after shocks, blockades, failures, or attacks.

#### Extraction Rate
How much value an actor removes from a system relative to how much they reinvest.

These are primarily simulation and balancing tools.

---

### 18. Emergent Endgame

The endgame should not simply be accumulating more resources.

As players gain power, the problem changes.

Early game:

**survive and produce**

Mid game:

**specialize and trade**

Later game:

**build infrastructure and organizations**

Advanced game:

**control bottlenecks and institutions**

Endgame:

**maintain a complex system of dependencies while rivals attempt to replace, capture, bypass, or destroy it**

At high levels, the primary challenge becomes maintaining legitimacy, resilience, organizational cohesion, and productive capacity.

The strongest player is not necessarily the richest.

It is the player whose position is hardest for civilization to route around.

---

## Core Design Principle

The game should never need to tell players:

**"This person is powerful."**

The simulation should make it materially obvious.

A player is powerful because farms depend on their fertilizer, factories depend on their electricity, armies depend on their logistics, cities depend on their roads, organizations depend on their decisions, and replacing them would require enormous effort.

**Status then emerges as the human interpretation of that underlying reality.**
