# Land controller and paid takeover

Status: the core model below is the user's confirmed direction. Numerical values, energy representation, takeover timing details and failure handling remain open. No claim or controller implementation exists yet. This replaces the earlier renewable-lease/auction candidates as the active land-allocation design.

## Agreed rules

- Before claiming, ordinary gathering, building and facility use are allowed on unclaimed land without claim protection. The first controller must be obtainable through this unclaimed-land gameplay; claiming does not unlock basic actions.

- Players craft and place a land-management controller. Crafting requires a meaningful minimum amount of coal and other resources.
- The controller manages a bounded land claim, limited vertically both above and below it. Exact horizontal shape, vertical reach and parcel units remain unspecified.
- Maintaining the claim consumes power. The cost increases with the amount of land claimed. The scaling formula and definition of billable area/volume remain open.
- There is **no grace period** when controller energy reaches zero. An ordinary claim immediately becomes unclaimed and its claim-based protection ends. This replaces the proposed seven-day power-failure grace period. Physical buildings and goods are not automatically deleted. Independent inventory protections, reactivation/reclaim behavior and the pending-takeover exception still require specification.
- Once a month, another player can take over by paying a significant premium over the actual energy stored in the controller. Empty storage capacity adds no value to the takeover calculation.
- The outgoing owner receives the takeover payment.
- A funded takeover gives the outgoing owner **one week (7 days)** to move before handover. This replaces the proposed 14-day relocation interval. Exact clock handling during server outages remains to be specified.
- During the move-out week, the outgoing owner may remove goods, vehicles, machines and building blocks. The takeover primarily purchases land; the earlier requirement to retain structures is superseded. The buyer is not guaranteed the site's buildings or equipment.
- The controller's stored energy is the exception to removal rights: withdrawals remain locked during relocation, normal claim upkeep continues, and the remaining reserve transfers to the buyer. The outgoing owner receives the takeover payment but cannot also withdraw this reserve.

This combines an entry investment, ongoing energy demand and a market route for replacing the owner. It does not assign territorial jurisdictions or a server profit score. A wealthy owner may maintain an estate without maximizing industrial output.

## Controller interface — proposed minimum

Show the claim boundary, owner, power consumption, stored energy, estimated powered time remaining, next takeover opportunity and any pending move-out deadline. Let an authorized owner adjust the claim, add energy and manage permissions. Exact controls and which values are public remain open.

Claim boundaries must persist independently of the block's visual representation. Breaking, moving or replacing the controller must not erase claims, steal title, reset a takeover clock or duplicate stored energy. Relocation and voluntary closure need explicit rules rather than implicit block-break behavior.

## Energy and valuation: resolve before selecting numbers

**Valuation basis is confirmed: actual stored energy, not maximum capacity.** The owner must deposit real energy to increase the takeover price. When a valid buyer deposits the full required payment, snapshot the stored energy and fix the price. Reserve withdrawals are locked during relocation; normal claim upkeep continues. Simultaneous changes must be resolved in one authoritative transaction.

Also define whether claim power is drawn from physical fuel, a stored derived-energy buffer or a later power network. Existing coal/oil/uranium currencies do not establish automatic conversion rates or universal fuel interchangeability. The controller needs an explicit valuation and payment denomination if different fuel forms can be deposited.

Actual stored energy and the payment-time snapshot are agreed. A single payment denomination per claim remains a recommendation. The relationship between physical fuel and stored energy still needs specification; no separate valuation account or universal conversion is implied.

If the formula is payment = reference value × premium multiplier, the reference is the actual stored energy at accepted full payment. Do not assign a percentage yet. The remaining reserve transfers with the controller to the buyer after normal claim upkeep during relocation; it is not withdrawn by the outgoing owner. The price stays fixed. The buyer receives both property and remaining energy, so gross payment is not the same as the net acquisition cost.

## Takeover lifecycle — agreed sequence

1. During an eligible takeover opportunity, the buyer deposits the full required payment.
2. Fix the price using actual stored energy at that moment; reserve the payment pending handover.
3. Begin the relocation period and lock reserve withdrawals. Normal claim upkeep continues without repricing the accepted takeover.
4. At the deadline, transfer ownership and release payment to the outgoing owner together. The remaining controller reserve transfers to the buyer. The seller may have removed buildings, equipment and goods during relocation; the purchase does not guarantee those assets.

The numbered sequence and one-week relocation interval are agreed. Calendar-month windows versus claim anniversaries, multiple challengers, owner counteroffers, cancellation and subsequent takeover eligibility remain open. Notifications and access during relocation need specification. The user has not required an auction or granted the incumbent a matching right.

Unaccepted scheduling proposal retained for discussion: a seven-day takeover window every 30 days, with the next window beginning no sooner than 30 days after handover. These are separate from the agreed seven-day move-out period; the user's relocation correction does not approve the other scheduling details.

## Buildings, possessions and failure cases

- **Removal:** the seller may remove building blocks, installed machinery, goods and vehicles during the one-week notice period. Do not promise these assets to the buyer or impose a structure-preservation restriction. Specify treatment of anything left behind separately.
- **Controller and energy:** the reserve remains locked and transfers to the buyer after normal upkeep. Removing the management block must not release the locked energy, cancel the funded takeover, duplicate payment or erase the pending land transfer. Define the physical block's recovery independently of persistent claim state.
- **Power exhaustion:** no grace period; ordinary claim protection ends immediately at zero. Remaining-time displays and warnings are proposed feedback, not an extension of protection. Define whether refueling reactivates a still-available claim or requires claiming again; refueling cannot silently override a new owner. No automatic demolition or deletion is implied.
- **Offline operation:** ongoing claim costs need an explicit clock, including logout, unloaded chunks and server downtime. No forced chunk loading should be required to account for claims.
- **Pending takeover:** normal claim upkeep continues and the price is fixed. Explicitly resolve exhaustion during the funded relocation interval against the no-grace rule and promised handover. Do not silently release paid-for land to a third party or invent free claim power. Define top-ups and owner disappearance. Prevent resizing, increased draw or connected machinery from draining a locked reserve beyond agreed claim upkeep.
- **Splits and transfers:** resizing, subdividing, merging and transferring to another account must not silently reset monthly eligibility or evade an accepted takeover.
- **Neighbors:** prevent overlapping volumes and expansion into another owner's land. Vertical bounds must support distinct ground and sky holdings without accidental control of both.
- **Recovery:** persist payments, ownership, deadlines and inventories coherently so restart/retry cannot pay twice or transfer land without payment. Ordinary diagnostic logs are not the transaction database.

## Next decisions

Stored-energy valuation, payment-time price fixing, withdrawal locking, continued normal upkeep, one-week relocation and simultaneous ownership/payment handover are settled. Next choose the monthly schedule and the handover boundary between retained structures and removable possessions. Consumption scaling, power exhaustion, construction costs and premium remain open.

Related: [property_and_authority.md](property_and_authority.md), [currency_and_exchange.md](currency_and_exchange.md), [map_and_discovery.md](map_and_discovery.md).
