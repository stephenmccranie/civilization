# Ownership and trade

Land Controllers protect a bounded place using Coal. Trade Counters exchange any two physical item types; Coal is the default payment. Each has a player owner; claim access is shared through a local username whitelist. These systems work without a prepared demonstration area.

## Land Controller

The controller is a pale-stone survey cabinet with an ivory-bordered green parcel emblem, house and red flag on its front and top, iron corner fittings and a small lower coal-access slit. Its original textures are native 32×32. New placements face the player.

Craft with four stone bricks in the corners, four Coal on the edges, and an iron block in the center. Construction coal is consumed; supply additional fuel to activate protection.

Place the controller and right-click it. It opens a **single-chest inventory with 27 real coal slots**. Drag, right-click, number-key swap and shift-click work through ordinary inventory controls. Compact I/II/III tier tabs attach to the left edge; whitelist, purchase and collection icon tabs attach to the right. Hover a tab for its label and details. Both civic menus use shared beveled panels and inset controls attached to the inventory frame. Anyone can inspect the controller; the server checks permissions for every operation.

Starting defaults:

| Tier | Footprint, in blocks | Upkeep per real hour |
| --- | --- | --- |
| 1 | 64×64 | 1 Coal |
| 2 | 128×128 | 4 Coal |
| 3 | 256×256 | 16 Coal |

All three cover **64 blocks vertically**, from controller Y−32 through Y+31. Horizontal bounds are centered on the controller with the same half-open convention: Tier 1 spans −32 through +31 on X/Z. Switching consumes **24 hours of the destination tier’s base upkeep**: **24, 96 or 384 Coal**, taken from the controller reserve. Upgrades and downgrades both cost coal and start a **24-real-hour cooldown**. Some fuel must remain after paying. Same-tier clicks, insufficient fuel, overlap, lack of access, active cooldown and takeover locks do not charge a switching fee or reset its deadline. Normal upkeep continues. The cooldown persists through logout, unloading, server downtime and handover. Tabs show costs on hover; the header shows remaining cooldown. The initial placement starts at Tier 1 without a switching fee.

The inventory holds 27 stacks of 32 Coal, plus the fractional energy of the coal currently burning. That partly consumed coal cannot be withdrawn; the remaining protection time includes it. No other items are accepted. Upkeep uses real elapsed time, including logout, unloaded chunks and server downtime. At zero reserve protection ends immediately; anyone can supply a dormant, non-overlapping controller to claim the area. There is no grace period. These are initial tuning defaults, not a final economy balance.

Protected land permits its owner and whitelist to build, break and use facilities. Other players can walk through and use public trade/controller menus. Ordinary unclaimed buildings remain usable and breakable. Level-2 operators bypass land protection for administration; stock and controller management still enforce access rules.

## Takeover

A claim becomes available for takeover 30 real days after acquisition. After that it remains available until someone funds a takeover; successful handover starts a new 30-day wait. This implements a rolling minimum interval, not a calendar-month auction or seven-day bidding window.

The price starts at **twice actual remaining controller energy**, rounded up to whole Coal. Empty storage capacity adds no value. The buyer brings physical payment and clicks the purchase button twice: first to review, then to confirm within ten seconds. A changed price or owner requires another review. Payment goes into held stock, not the seller's inventory yet. If the price exceeds the coal carried, confirmation stores that delivery locally; further trips can finish the purchase. Partial deliveries do not reserve the land or lock its price and can be collected back. Only full payment starts the takeover.

The seller has exactly one week to move. They may remove buildings, machines, goods and the physical controller. The land record and locked energy stay anchored at the original position even if the block is removed. Replace a controller there to reconnect, or stand within six blocks and use `/civilization land` to manage it. The seller is notified when online; login notices show pending handovers and collectible payments.

During the week:

- Normal upkeep continues; the owner, whitelisted players and funded buyer can add coal without repricing.
- Reserve withdrawals and tier changes are locked.
- At the deadline, ownership and remaining reserve transfer to the buyer, and the former whitelist is cleared; payment becomes collectible by the seller at the original controller location.
- If energy reaches zero before or exactly at handover, protection ends, the takeover is cancelled, and the **full payment becomes collectible by the buyer**. The seller receives nothing. There is no grace period.

Payments/refunds are physical reserved coal held at that location, accessible through the controller or nearby `/civilization land`. No remote account balance or item delivery is created. Inventory space is required; collection can be repeated in stack-sized portions. Removing the block does not erase owed stock. The engine settles deadlines in chronological order even if the area was unloaded throughout the week.

Breaking an ordinary controller ends its claim and drops whole remaining fuel locally. Fractional spent coal is not refunded. Breaking a controller during a pending takeover drops only its shell and preserves the obligation.

## Claim whitelist

There is **no formal group system or group command**. The claim owner opens **Whitelist**, types a Minecraft username and selects **Add** or **Remove**. Existing names are listed and can be selected for removal. Players must have joined this server before they can be added. Access is stored by UUID, so a name change does not revoke it. Each claim has its own list; new lists allow up to 128 entries.

Whitelisted players have full use of the claimed place: building, storage, machines, fuel deposits/withdrawals, tier changes, and management of the owner's trade counters inside that claim. Takeover locks still apply. Only the owner edits the whitelist or collects their sale payment. Removal takes effect on already-open inventories. Counters outside a matching owner's active claim remain personally managed. Vehicle sharing remains future work.

### Existing worlds

Old group-owned assets and payment rights become personal to the former leader. Former members are copied into each corresponding claim's whitelist. No formal group records remain in new saves. Personal assets remain personal.

Existing claims retain their old footprint, height and upkeep until an authorized player selects one of the new tiers. This avoids involuntary overlap or changing the terms of a paid takeover; pending takeovers still lock tier selection. Existing energy becomes real coal stacks without duplicating the reserve. New controllers start at Tier 1.

## Trade Counter

The counter is an oak exchange cabinet with a large balance-scale emblem, a lower transaction slot and a shallow two-bay counting tray on top. Its original textures are native 32×32. New placements face the player.

Craft six planks across the top/bottom rows, a chest in the center, and an iron ingot on each side. Place and right-click it.

The counter supports **one item-for-item offer** and **one shared 27-slot chest inventory**. It accepts any normal item, respecting that item's stack limit. There is no separate goods capacity, fuel purse or buy/sell mode.

1. Pick up an item onto your cursor and click the left **Receive** selector. This creates a one-item ghost copy without consuming the carried item.
2. Set the right **Pay** selector the same way. It defaults to Coal, but any item can be payment.
3. Type the exact quantities in the **Per trade** fields. Press Enter, click away, or close the screen to save valid edits. Quantities range from 1–9999 and may span multiple stacks; both sides must fit their actual inventories. Invalid or empty values are not saved.
4. Put payment stock in the central chest using ordinary inventory controls. Incoming goods go into this same chest.
5. Customers see **You give** and **You get**. Each **Trade** click exchanges exactly one complete batch at those quantities; no partial batches are exchanged.

Clicking a selector with an empty cursor clears it. Ghosts cannot be extracted, shift-clicked into storage, cloned or spent. Changing an offer leaves all real stored items untouched. To sell bread for coal, set Receive to coal and Pay to bread; to buy wheat, set Receive to wheat and Pay to the chosen payment. Item components must match exactly, including durability and other stored data.

Only the owner and authorized claim whitelist can edit offers or move storage items. Customers can see storage but cannot take it directly. Access is rechecked on every operation, including already-open menus. Storage accepts unrelated items too; those occupy real capacity.

Offers operate while their owner is offline. Both inventories must have the required items and room for the result. Outgoing items can free space for incoming items in the same exchange. No side commits unless both validate. The counter must already hold its outgoing payment; incoming goods cannot fund the same transaction. Competing customers cannot spend the same last payment. A request carrying an outdated offer revision refreshes without trading. The funded-trade count reports stocked payments; incoming space and customer inventory are checked when trading.

External chest attachment and hopper access remain unimplemented.

### Existing counters

Old buy/sell offers migrate to the corresponding Receive/Pay pair. Real goods and coal move into the shared chest. If an old counter's separate coal reserve exceeds the remaining chest space, that coal stays collectible by its owner at this location using `/civilization land` within six blocks. Saving and reopening does not repeat the conversion.

Breaking the counter drops its real goods and coal locally. Its management owner does not create a free land claim: outside protected land anyone can break it. A land buyer can remove a left-behind counter and recover its contents; land transfer does not silently transfer the counter's separate management identity. Public map markers expose the offer, while purchases remain local.

## Survey Table

The map is a **2×2 multiblock table**, one block high. Craft a **Survey Table Section** from one paper, one oak slab and one copper ingot (shapeless). Combine one section with a compass to make the **Survey Table Controller**. A complete table therefore uses four sections in total: convert one to the controller and place the other three alongside it, following the contextual textured construction guide. The controller tooltip lists those three extra sections. Each piece is independently placed and recovered when broken.

The assembled oak-and-copper table displays one continuous north-up map across its surface. It borrows the active resource pack's wood/copper textures, including Faithful. Right-click any section for a closer view and offer/claim inspection. The view is fixed at **256×256 world blocks**, centered on the controller. No handheld map, map hotkey, panning or minimap. A missing section or moving more than eight blocks from the controller closes inspection; claim access is rechecked throughout. The physical tabletop is visible to anyone who can see it.

- **Gold outlines:** powered claim footprints. Select one for owner, size, controller coordinates and inclusive vertical bounds. Exhausted claims disappear; pending handovers are labeled.
- **Blue markers:** placed Trade Counters, including unconfigured/empty ones. Select one for exact You give / You get quantities and stocked payments. Stock is informational, not a reservation or a guarantee of incoming storage space.
- **White marker:** the table center on the tabletop, or your position in the closer view. No other players, cargo or private deposits are transmitted.
- Toggle claim/counter layers in the closer view; click repeatedly at overlapping markers to cycle them.

Terrain is **automatically surveyed**, shared by everyone at that table and persisted in its block entity. It does not depend on who has explored. Each terrain sample covers 4×4 blocks. With a player within 24 blocks, the table samples at most 64 points per tick, completing a pass in about 3.2 seconds. It reads only already-loaded surface chunks; it never adds chunk tickets or generates terrain. Unavailable samples retain their previous colors, or parchment if never sampled. Ceiling dimensions have no surface survey. Breaking the controller discards its saved survey; a newly placed table surveys its own surroundings.

The tabletop synchronizes terrain and public markers every five seconds while attended; the closer view refreshes every two seconds. Both views are dimension-local and cap public markers at 256. A bounded client texture cache holds at most 32 tables and releases resources on world exit. Production load profiling remains open. No remote trade, teleportation, automatic routing or map copying. Item search, destination pins and expanded survey range remain future work.

**Migration:** the former `civilization:survey_map` item ID is retained, but now places the controller. Existing items become controllers automatically. Old per-player survey files are no longer read or updated; no user save files are deleted.

## Protection and technical scope

Protection covers ordinary player block breaking/placement, block interactions/tool changes, buckets through native interaction permission checks, crop trampling, entity interactions/direct and player-attributed damage, explosions, fire destruction/spread, common mob griefing, piston movement across ownership boundaries, and vanilla hopper/dispenser/dropper/fluid movement across boundaries. Internal automation remains allowed; boundary transfer requires matching owners at both ends or both ends unclaimed.

Vanilla chest/double-chest, hopper and custom machine menus recheck access on inventory clicks, including after whitelist removal or handover. Public civic menus separately check every operation. These are targeted integrations, not a guarantee against arbitrary third-party mods or administrative world-edit commands. Broader automation, vehicle/entity movement, redstone side effects and the remaining vanilla grief/duplication audit need multiplayer hardening before a public server. Private death-recovery containers are a separate unimplemented system.

`civilization_civic` SavedData in the Overworld owns player identities, claim whitelists, fuel slots, stock, held payments and collectible proceeds across dimensions. Controller removal cannot remove a pending transaction. Claim lookups use a chunk index; upkeep is settled once per second for active claims and at action time, without loading their chunks. Stock/payment mutations are serialized on the server thread. Normal saves/restarts preserve records; unexpected process or disk failure across Minecraft's separate world/player saves is not a transactional database guarantee. Backups and crash recovery remain production work.

World tuning: `serverconfig/civilization-civic.toml`. Stop the world before editing. Upkeep duration, takeover interval and price multiplier are configurable. Existing controllers retain their recorded coal-energy unit; new controllers use a changed upkeep setting. Existing eligibility dates remain fixed; future acquisition/handover uses the configured interval. The move-out week is fixed.

Current tests and deployment are recorded in [project status](../docs/status.md). The focused `civic` visual scene checks the actual synchronized menus in the hidden client. This is development functionality, not verified 200-player or public-server readiness.
