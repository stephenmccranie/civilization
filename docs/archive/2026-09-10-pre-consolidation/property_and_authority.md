# Property and authority — first design draft

Status: **confirmed direction: market-based land allocation, with generous leeway, should favor those who can make the best (most profitable) use of land; no formal jurisdictions.** This is not merely a request for a trading interface. The concrete allocation, ownership and organization mechanics below remain proposals, not implemented features. Industry expansion is paused while we define the civilization's fundamental social systems.

## Clarification: land must remain contestable through the market

The user's intent is a mechanism governing who holds land, rather than only optional listings for goods and property. Holding a valuable site should face an economic test, with generous room for development and ordinary play. A first claim should not necessarily grant permanent control regardless of later demand or productive use.

The active mechanism is now the user's [powered land controller with monthly paid takeover](land_controller.md). It costs meaningful coal/materials to create, consumes power scaled with claimed land, has vertical limits and permits monthly takeover at a significant premium over controller-stored energy, paid to the owner with time to move; structures stay. Exact valuation, timing and failure rules remain open. Earlier lease/auction candidates are superseded.

Willingness to pay is a practical signal of expected value, not proof of actual productivity. Wealthy players may subsidize a prestige estate or exclude a rival; status consumption and concentration of power remain consistent with the design. Do not implement a server-calculated profit score, automatic confiscation for low activity, or immediate eviction on an unsolicited bid without a further decision.

The market offer types later in this document were an assistant proposal based on an overly broad reading. They are optional future exchange features, not the clarified requirement. Land allocation is the immediate planning priority.

Context: [design_direction.md](design_direction.md), [original design](status_power_civilization_game_design.md), [development_plan.md](development_plan.md).

## Purpose

Turn productive assets into meaningful decisions: who may use them, who receives their output, who can delegate authority, and how control changes hands. Hierarchy and exclusion are intended possibilities. Control should come from assets and relationships, not an assigned class or prestige score.

The proposed model is **holdings, organizations and a market for goods and rights**. Permissions enforce control; market transactions transfer goods, ownership or defined access. Power emerges from ownership, economic dependence and negotiated relationships. There is no territorial government layer over independently owned property.

## A town that exercises the model

Consider a river town with farmland upstream, a nearby mine, workshops, a warehouse and a freight landing. These are illustrative locations, not a committed map or new machine package.

| Participant | What they control | What they permit | What gives them bargaining power |
| --- | --- | --- | --- |
| Farmer | Fields, food stores and farmhouse | Harvesting for workers; deliveries into a designated store | Reliable food surplus and location |
| Mine company | Extraction site and stockyard | Operation for miners; collection from a loading store for a carrier | Resource access and sustained output |
| Workshop owner | Buildings, machines and inventories | Machine operation without unrestricted access to finished stock | Processing capacity and accumulated equipment |
| Port organization | Landing, warehouse and access facilities | Public trading area; restricted cargo handling | A convenient physical route and reliable service |
| Estate owner | Homes, gardens and prestigious buildings | Residence and building within particular plots | A desirable place, infrastructure and social proximity |
| Sky institution | An extraordinary destination and regional services | Access to chosen visitors, staff and customers | Capabilities and prestige that others value |

Nothing assigns these people professions. One organization may buy several holdings and integrate their supply. Another may specialize in transport without owning a mine.

If the farmer stops selling, customers use reserves, negotiate or seek another supplier. If a port withdraws access, carriers must negotiate or use a physically available alternative. The server does not invent replacement supplies or guarantee an equally good route. Whether these relationships arise depends on geography and demand, not permissions alone.

## 1. Holdings: own a place

Proposed: a named, bounded area with exactly one owner, either a player or an organization. It controls building, extraction and use of assets within its boundary. A farm, factory, estate and port use the same primitive.

Ownership persists while the owner is offline. Breaking a sign or controller must not erase title. A transfer is explicit and accepted by the recipient; placing a block on someone else's land does not create a competing ownership claim.

Recommended boundary: vertically bounded, grid-aligned parcels. Full-height claims would let a ground claim automatically own the sky above it, which conflicts with our vertical civilization. Exact grid and selection UX remain open. Start with simple non-overlapping parcels; allow only one level of delegated plots inside a holding if needed for housing and workshops. Avoid an arbitrary hierarchy of nested claims.

Machines inherit the containing holding's authority. A complete multiblock must fit within one permission scope for the first version. Use designated loading inventories for outsiders instead of giving them factory-wide withdrawal rights. Per-block ownership registries are not needed for every placed brick.

**Acquisition direction is agreed:** craft and place a land controller using meaningful coal and other materials, then supply power to retain the claim. Consumption increases with claimed land. Monthly premium-paid takeover provides contestability. Exact costs, geometry, energy valuation, wilderness rules and treatment of pre-existing builds remain open; see [land_controller.md](land_controller.md).

## 2. Organizations: own and act together

**Latest direction supersedes the detailed-role recommendations below for the initial system:** expect solo players and trusted groups of roughly 2–8 people. Members have full shared-asset access. Use [groups.md](groups.md) as the active baseline; custom ranks, per-member operator/carrier permissions and elaborate management are deferred. The owner-only administrative boundary remains to be confirmed separately. The permission categories below remain useful for outsiders and technical enforcement, not a requirement for member role configuration.

Use one persistent organization type. Players can call it a company, house, town, guild or ministry; these names do not require separate gameplay systems.

Recommended first governance model: one controlling leader, with explicit delegated permissions. Members may belong to several organizations. Assets belong to the organization, so a worker leaving does not take a share of the factory automatically. Transfer leadership explicitly; ordinary asset managers cannot transfer leadership or dissolve the organization.

This makes a workable first hierarchy but does not model joint equity, elections or corporate takeovers. Those are possible later additions. A political bargain is not mechanically enforced unless represented by an actual supported transfer or permission.

Disbanding must first resolve all holdings and supported obligations. Logging out does not dissolve an organization. Succession on prolonged absence needs a published rule and preferably an owner-designated successor; exact timing and recovery policy remain open.

## 3. Grants: separate ownership from use

| Permission | Example |
| --- | --- |
| Build and break | A builder renovates a workshop |
| Operate | A worker starts a machine without redesigning the building |
| Deposit | A supplier delivers into an agreed loading store |
| Withdraw | A carrier collects from an agreed dispatch store |
| Manage grants | A steward assigns workers within a limited scope |
| Transfer title or leadership | The controlling owner sells a holding or hands over an organization |

Use a few understandable permission presets backed by these rights. A delegate cannot grant powers they do not possess. Ownership transfer and leadership transfer remain separate from routine management. Permission checks apply when an action occurs, including after revocation while a menu is open.

Basic access is revocable. A paid lease is a different promise: it specifies scope, expiry and termination terms. Do not present revocable permission as guaranteed tenancy. Recommend simple grants first; decide lease and payment rules before implementing rent.

For the first version, doors, loading points and protected construction provide practical access control. Protection does not automatically mean an invisible wall that ejects visitors. Absolute entry restrictions, toll collection and later airspace control require explicit design, especially once flight exists.

## 4. Authority has a physical scope

A town organization can own roads, rent buildings and control its port. It does not automatically gain authority over every independently owned farm nearby. Ownership of a warehouse does not confer ownership of cargo carried past it.

**Confirmed: no formal jurisdictions.** Organizations cannot impose laws, territorial taxes or annexation on independent holdings. Influence comes from control of assets, supply, prices and access. Rent and service charges are terms for a particular owned asset or service, not jurisdiction over surrounding territory. Names and titles do not confer extra powers.

### Proposed market scope

| Offer | What the server can enforce | What still needs design |
| --- | --- | --- |
| Goods for sale or purchase | Exchange backed by actual stock and payment | Currency or barter, listing visibility, pickup and delivery |
| Property sale | Transfer a defined holding against payment in one transaction | Included inventories, existing leases and initial land acquisition |
| Rental or access | Grant specific rights for an agreed period | Expiry, termination, tenant improvements and existing obligations when sold |
| Delivery order | Reserve payment and release it when specified cargo reaches a specified destination | Custody, deadlines, cancellation and partial fulfillment |

These are candidate applications of a common offer/agreement model, not four approved launch features. Start with conditions the server can verify. Open-ended employment and construction promises need separate treatment; arbitrary written contracts cannot be automatically judged.

Goods remain physical. Remote discovery of an offer need not move its stock: a buyer can learn about a sale remotely while collection or delivery still requires travel. The visibility of listings remains open. An accepted agreement reserves the promised stock, payment or rights; completion must survive simultaneous use and restart without duplication or partial ownership transfer.

A market does not by itself determine how previously unowned land first becomes property. Founding or initial sale rules, reserve policy and abandonment still need explicit decisions. No automatic auctions, currency or NPC land seller are implied by the market direction.

Optional organized PvP is established direction, but land seizure through it is not yet designed. Unrestricted raiding is not the default transfer mechanism. Sale, voluntary transfer, internal delegation and eventual published abandonment rules provide initial routes for control to change.

## 5. Failure cases to resolve before coding

- **Worker departs:** revoke grants; existing organization property remains. Already withdrawn stock is not magically recovered. Distinguish authorized misuse from a permission bypass.
- **Owner stops cooperating:** revocable access can end; enforceable leases must follow their stated terms. Do not ask the maintainer to judge unwritten promises.
- **Owner disappears:** stop granting new powers by inference. Define succession, inactivity notices, grace periods and final abandonment handling before automatic release. Avoid surprise deletion or loot release.
- **A tenant builds an expensive home:** specify who owns placed blocks, what a lease protects and what can be removed at expiry before offering rentals.
- **Two players claim or transfer simultaneously:** one authoritative transaction succeeds. Persist changes and audit actor, old owner and new owner.
- **Automation crosses a boundary:** hoppers, fluids, explosions, pistons and later moving contraptions must not bypass permissions. Define which environmental effects are protected; audit the actual supported paths.
- **A protected road traps someone:** define non-destructive exit and respawn behavior without enabling free freight teleportation. Exit mechanics remain open.

## Planning order

This is the local order within the property topic. The current whole-mod planning sequence in [systems_blueprint.md](systems_blueprint.md) takes precedence; land allocation is no longer the immediate task. Competitive leases and other mechanisms above remain candidates.

1. Decide initial land acquisition, boundary shape and wilderness rules. This determines what ownership means before any permissions UI exists.
2. Specify the market's first offer types, payment medium, listing visibility and physical fulfillment. No formal jurisdictions; that decision is settled.
3. Specify organization control, transfers, delegated plots and succession.
4. Define local exchange and the boundary between revocable grants and enforceable agreements. Currency remains open.
5. Walk through creation, trade, expulsion, departure, insolvency and abandonment in the example town.

Only then choose the first implementation package. Keep industry expansion deferred during this planning pass. The aim is a small set of rules capable of producing landlords, companies, estates and institutions through play.
