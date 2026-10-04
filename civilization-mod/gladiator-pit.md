# Gladiator Pit

A physical, controller-centered oval for two-player knockout fights and item wagers. Players build their own seating outside its required walls and prep rooms. The controller is a ground block, with its top flush with the fighting floor and its horizontal center exactly at the oval's center.

## Build and materials

Craft the **Gladiator Pit Controller** with four Stone Bricks at the corners, four Copper Ingots at the edge centers and one Iron Ingot in the center. It has no operating fuel. Place it in prepared level ground, replacing one floor block. The long axis runs left/right relative to its placement facing.

Additional structural materials:

| Material | Quantity |
| --- | --- |
| Stone Bricks, full blocks | 226 |
| Stone Brick half slabs | 160 |
| Oak Fence Gates | 6 |

Make the slab cuts with the existing saw. Native stone-brick slabs already present in a world also validate at the correct lower-half orientation. As with other guides, held-stack controller construction uses actual cut items rather than cutting blocks for you. Supply matching stacks by right-clicking the controller or place pieces yourself; obstruction, permissions, unloaded terrain and stock limits still apply. The contextual guide uses the [shared performance contract](multiblock-builds.md#guide-performance-contract).

The oval occupies **29×19 blocks**; the complete structure with prep rooms occupies **41×19**, centered on the controller. The wall is 2.5 blocks above the floor. Each opposed room has a **5×5 clear interior**, three-block headroom and a lower-slab roof. Each three-wide inward opening has three controller-operated oak fence gates. Rear two-wide openings admit players separately.

Prepare a solid full-block floor under the complete footprint; no liquids, holes, partial floor blocks or magma. Natural solid terrain is accepted; paving is optional and excluded from the bill. Keep the fighting area and room interiors clear for three blocks above the floor. The first version does not allow room furniture in required clear space. Seating, lighting and other building outside required cells are freeform. The pit is stationary land infrastructure; operation aboard vessels is rejected. [ArenaLayout.java](src/main/java/dev/civilization/ArenaLayout.java) is the authoritative runtime geometry.

## Controller and fight

Empty-hand right-click opens the shared cabinet menu. Public players may enroll, wager and collect through a controller on claimed land; this grants no construction or container access elsewhere on the claim. The placer retains host cancellation, and the placer must retain structural claim rights over the footprint. Claim owners/whitelists and operators still control construction through existing protection.

1. Choose **Join A** or **Join B**. A player can enroll in one arena at a time. A spectator with a wager in this match cannot become its fighter.
2. Optionally select a hotbar stack and press **Stake held**. This deposits the entire selected stack, up to its ordinary stack limit. **Refund stake** returns the old deposit to personal collection credit. Fighters may stake different items/quantities, or fight without stakes. Both escrow previews show the actual deposited stacks.
3. Both press **Accept terms** after reviewing participants and stakes. Changes to participants or fighter stakes clear both acceptances. Depositing items never authorizes a match by itself.
4. Both press **Ready / enter prep**. Ready requires Survival mode, full health and no active potion effects. Each fighter moves into their own prep room. A ready fighter cannot change stakes. The gates close, and when both are ready the controller locks betting, refunds unmatched offers and starts a **five-second countdown**.
5. The gates open together. Enter the fighting oval within **ten seconds**. Melee and ordinary player-owned projectiles, including the Paterson, can damage only the enrolled opponent inside the fighting area. Prep rooms remain safe; returning there after the entry allowance aborts the fight. Explosions, pets/mobs, environmental damage and attacks from outside participants do not damage protected fighters. A ready fighter cannot harm spectators or mobs through the arena damage path.
6. A would-be lethal opponent hit becomes a **knockout** before health loss; one winner receives both fighter stakes. There is no death inventory drop. Normal weapon/armor wear, consumables and ammunition use remain, including items breaking from ordinary wear. Both fighters return to their entry health, with fire, falling damage and harmful effects cleared. No inventory is copied or restored.

The enrollment/preparation window lasts at most **ten minutes** from the first join. A live fight lasts at most **five minutes**. The result has a five-second protection/reset interval. Fighters remain where they finished; the controller does not auto-eject players or construct spectator seating. The opponent consent applies only inside a live match, including when ordinary server PvP or team friendly-fire settings would otherwise disallow it. It does not require the planned wearable PvP flag and does not enable global PvP.

**Withdraw** or `/arena leave` cancels participation and refunds the unfinished match. The host's **X** button (tooltip: cancel and refund) and operators can abort a match. Gates reject manual interaction after a fighter readies; the controller reconciles their state during the match. An unready lobby join gives no world damage immunity; ready protection applies only within this arena's footprint.

## Spectator wagers and collection

Spectators select a held stack and choose **Back A: held** or **Back B: held**. The controller escrows that entire stack as an open offer. Browse offers with the arrows; **Match bet** backs the opposite fighter by depositing identical items, components and quantity from the player's main inventory. Named or otherwise distinct stacks do not match ordinary versions. For example, each backer deposits eight Coal; the winning backer receives sixteen Coal.

One wager per spectator per match, whether creating or matching an offer; at most **64 offers** in one match. Fighters cannot enter their own spectator ledger. There is no pooled odds, currency, house cut, remote betting or bookmaker role. **Cancel offer** refunds an unmatched offer belonging to that player. Matched wagers remain committed until the result or a match refund; betting closes with the countdown.

Winnings and refunds become saved personal collection credits. **Collect** at any Gladiator Pit or `/arena collect` moves complete owed stacks into available main-inventory space. Stacks that do not fit remain owed; collection does not drop items. Credits survive removal of the controller and may be collected even if the original arena is gone. The menu shows the first owed stack and total owed stack count.

## Interruptions and persistence

Leaving the allowed match/staging area, changing dimension or game mode, fighter disconnect, lost placer claim rights, required unloaded terrain, a damaged/obstructed structure, controller removal, preparation expiry, time-limit draw or server restart refunds each depositor's own unfinished stakes. There is no automatic disconnect or departure forfeit. Deliberate aborts can therefore avoid a loss; do not treat this first version as an adjudicated competitive betting service.

Results and refunds settle once on the server thread. Accepted terms, menu identity, range, current revision, actor, offer ownership and available items are checked at action time. Before awarding a knockout, the controller revalidates the structure. Stored escrow, credits and pending health recovery live in `civilization_arenas` SavedData independently of any block entity; unfinished matches never resume silently after reload/restart. Standard world/player saving is used; abrupt-crash atomicity across separate player and world files has not been established.

Static layout and guide geometry are cached. Active match validation reads only its bounded footprint once per second, with additional validation on player actions and before awards; it never force-loads chunks. Idle empty pits perform no footprint scans. No per-frame structural/collision union work is introduced.

## Verification

[Testing](testing.md#development-checks) owns commands; [status](../docs/status.md#validation-and-limits) owns current results. `gladiator` is the hidden Photon/Faithful host scene; `runArenaPeer` launches the independent guarded client against local test port 25570. `dev.ps1 Verify -Scope Visual -Scene gladiator` automatically starts and checks both guarded clients after the host publishes its disposable test world. The fixture coordinates via ignored marker files but uses actual menu payloads and melee input for enrollment, stakes, acceptance, readiness, knockout and collection. The peer uses `runs/arena-peer`; user worlds are not changed.

Server fixtures cover construction and geometry validity, outside seating, conservation/retry, exact components, competing wager acceptors, full-inventory recovery, restart refunds, controller removal, stale menus, damaged-result refund, actual server-player knockout and absence of lobby invulnerability. The two-client scene covers real fighter custody and damage; four simultaneous real fighter/spectator clients, abrupt process crashes and representative busy-arena load remain broader audits.
