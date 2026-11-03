# Ownership and trade

Land Controllers protect a bounded place with Coal; Trade Counters exchange any two physical item types; the Survey Table maps claims and offers. Each claim has one owner, who shares access through a per-claim username whitelist. All three controllers are Assembly Bench jobs (ten seconds of hand work) and face the player when placed.

## Land Controller

Assemble it from **4 stone bricks, 4 Coal and 1 iron block**; the Coal is consumed, so supply more to power the claim. It is a pale-stone survey cabinet with a green parcel emblem and a small coal slit.

Right-click it for a **single-chest inventory of 27 Coal slots** (27 stacks of 32) using ordinary inventory controls. Tier tabs I/II/III attach to the left edge; whitelist, purchase and collection tabs to the right, with labels and costs on hover. Anyone can inspect the controller; the server checks permission on every operation.

| Tier | Footprint, in blocks | Upkeep per real hour |
| --- | --- | --- |
| 1 | 64×64 | 1 Coal |
| 2 | 128×128 | 4 Coal |
| 3 | 256×256 | 16 Coal |

Every tier covers **64 blocks vertically**, from controller Y−32 through Y+31, and is centered horizontally with the same half-open convention (Tier 1 spans −32 through +31 on X and Z). A new controller starts at Tier 1 for free. Switching tier costs **24 hours of the destination tier's upkeep — 24, 96 or 384 Coal** — from the reserve, and starts a **24-real-hour cooldown**; some fuel must remain after paying. Same-tier clicks, insufficient fuel, overlap, missing access, an active cooldown and takeover locks cost nothing. The cooldown runs through logout, unloading and downtime, and the header shows what remains.

Upkeep uses real elapsed time, including logout, unloaded chunks and server downtime. The partly burned Coal cannot be withdrawn but counts toward remaining protection. **At zero reserve protection ends immediately**, with no grace period, and anyone can fuel a dormant, non-overlapping controller to claim the area.

Protected land lets its owner and whitelist build, break and use facilities; others can walk through and use public trade and controller menus. Unclaimed buildings stay usable and breakable. Level-2 operators bypass land protection for administration, but stock and controller management still enforce access.

Breaking a controller ends its claim and drops its whole remaining Coal; the partly burned piece is not refunded.

## Takeover

A claim becomes available for takeover **30 real days** after acquisition and stays available until someone funds one; each handover restarts the 30-day wait.

The price is **twice the remaining controller energy**, rounded up to whole Coal. The buyer brings physical Coal and clicks purchase twice — review, then confirm within ten seconds; a changed price or owner needs a new review. Payment is held at the controller, not given to the seller. If the price exceeds what the buyer carries, the delivery is stored and later trips complete it; partial deliveries reserve nothing, lock no price and can be collected back. Only full payment starts the takeover.

The seller then has exactly **one week** to move out, and may remove buildings, machines, goods and the controller itself. The land record and locked energy stay anchored at the original position: replace a controller there, or stand within six blocks and use `/civilization land`, to manage it. Sellers are notified when online, and login notices list pending handovers and collectible payments.

- Upkeep continues; the owner, whitelisted players and the funded buyer can add Coal without repricing. Withdrawals and tier changes are locked.
- At the deadline, ownership and the remaining reserve pass to the buyer, the whitelist is cleared, and the payment becomes collectible by the seller at the original location.
- If energy reaches zero before or at handover, protection ends, the takeover is canceled and the **full payment becomes collectible by the buyer**; the seller gets nothing.

Payments and refunds are reserved Coal held at that location, collected through the controller or a nearby `/civilization land`, in stack-sized portions into free inventory space. There are no remote balances. Removing the block erases nothing owed, and breaking a controller during a pending takeover drops only its shell. Deadlines settle in order even if the area stayed unloaded.

## Claim whitelist

There are no groups. The owner opens **Whitelist**, types a username and selects **Add** or **Remove**; listed names can be selected for removal. Players must have joined the server before. Access is stored by UUID, so name changes keep it. Each claim has its own list of up to 128 entries.

Whitelisted players fully use the claim: building, storage, machines, fuel deposits and withdrawals, tier changes and managing the owner's trade counters inside it, subject to takeover locks. Only the owner edits the whitelist or collects sale payments. Removal applies to already-open inventories. Counters outside a matching owner's active claim stay personally managed.

## Trade Counter

Assemble it from **6 planks, 2 Iron Plates and 1 chest**. It is an oak exchange cabinet with a balance-scale emblem and a counting tray. Each counter holds **one item-for-item offer** and **one shared 27-slot chest inventory** of any normal items within their stack limits.

1. Pick up an item on the cursor and click the left **Receive** selector to set a ghost copy; the carried item is not consumed.
2. Set the right **Pay** selector the same way (default Coal; any item works).
3. Type the **Per trade** quantities (1–9999, possibly several stacks; both sides must fit their inventories) and press Enter, click away or close to save.
4. Put payment stock in the chest; incoming goods arrive there too.
5. Customers see **You give** and **You get**; each **Trade** click exchanges exactly one complete batch.

Clicking a selector with an empty cursor clears it. Ghosts cannot be extracted, cloned or spent, and changing an offer leaves stored items alone. Item components must match exactly, durability included. To sell bread for Coal, set Receive to Coal and Pay to bread.

Only the owner and authorized whitelist can edit offers or move stock; customers can see stock but not take it, and access is rechecked on every operation. Offers run while the owner is offline. Both inventories must hold the items and room; outgoing items can free space for incoming ones, nothing commits unless both sides validate, incoming goods cannot fund the same trade, and competing customers cannot spend the same last payment. A stale offer revision refreshes without trading. Chests and hoppers cannot attach.

Breaking the counter drops its goods and Coal. Its owner gets no land claim from it: outside protected land anyone can break it, and a land buyer can remove a left-behind counter and recover its contents.

## Survey Table

A **2×2, one-high** table: assemble **Survey Table Sections** from 1 paper, 1 oak slab and 1 Copper Plate, then craft one section with a compass into the **Survey Table Controller**. Place the controller and three more sections following its construction guide; each piece is placed and recovered independently. Removing any section disables the table.

The oak-and-copper tabletop (using the resource pack's wood and copper) shows one continuous north-up map of **256×256 world blocks** centered on the controller. Right-click any section for a closer view; leaving eight blocks or losing a section closes it, and claim access is rechecked throughout. There is no handheld map, hotkey, panning or minimap.

- **Gold outlines:** powered claims; select one for owner, size, controller coordinates and inclusive vertical bounds. Exhausted claims disappear and pending handovers are labeled.
- **Blue markers:** placed Trade Counters, configured or not; select one for You give / You get quantities and stocked payments (information, not a reservation).
- **White marker:** the table center, or your position in the closer view. No other players, cargo or private deposits are shown.
- Toggle claim and counter layers in the closer view; click repeatedly to cycle overlapping markers.

Terrain is **surveyed automatically** and shared by everyone at the table, saved in its block entity: 4×4-block samples, at most 64 per tick while a player is within 24 blocks (a full pass takes about 3.2 seconds), from already-loaded surface chunks only. Unavailable samples keep their previous color or parchment. Ceiling dimensions have no survey. A new table surveys afresh. The tabletop syncs terrain and markers every five seconds while attended and the closer view every two seconds; views are dimension-local and capped at 256 markers, and the client caches at most 32 tables.

## Protection scope

Protection covers ordinary block breaking and placement, block interactions and tool use, buckets, crop trampling, entity interaction and player-attributed damage, explosions, fire spread and destruction, common mob griefing, pistons across ownership boundaries, and vanilla hopper, dispenser, dropper and fluid movement across boundaries. Automation inside one claim is allowed; crossing a boundary needs matching owners at both ends or both ends unclaimed. Chest, hopper and machine menus recheck access on every click, including after whitelist removal or handover. These are targeted integrations, not a guarantee against arbitrary mods or administrative world edits.

`civilization_civic` saved data in the Overworld owns identities, whitelists, fuel, stock, held payments and proceeds for all dimensions, so removing a controller cannot remove a pending transaction. Claim lookups use a chunk index; upkeep settles once per second for active claims and at action time, without loading chunks. Mutations run on the server thread. Normal saves are safe; a crash between Minecraft's separate world and player saves is not transactional.

`serverconfig/civilization-civic.toml` (stop the world before editing): `coalMinutes` (60, upkeep duration of one Coal at Tier 1), `takeoverDays` (30) and `takeoverPriceMultiplier` (2). Controllers keep their recorded coal-energy unit and existing eligibility dates; the move-out week is fixed.

## Save compatibility

- The Survey Table Controller keeps the item ID `civilization:survey_map`; old survey map items are controllers. Old per-player survey files are ignored, not deleted.
- Claims from before tiers keep their footprint, height and upkeep until an authorized player selects a tier.
- Legacy group ownership loads as personal ownership by the former leader, with former members copied to each claim's whitelist.
- Legacy buy/sell counter offers load as the matching Receive/Pay pair with goods moved into the chest; Coal that does not fit stays collectible by the owner through `/civilization land` within six blocks.
