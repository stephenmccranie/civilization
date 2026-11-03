# Gladiator Pit

A physical oval for consenting two-player knockout fights with escrowed item wagers. Players build their own seating outside its walls and prep rooms. The controller is a ground block whose top is flush with the fighting floor and whose center is exactly the oval's center.

## Build and materials

Assemble the **Gladiator Pit Controller** at the Assembly Bench from **4 Stone Bricks, 4 Copper Plates and 1 Iron Plate** (ten seconds). It needs no fuel. Place it in prepared level ground, replacing one floor block; the long axis runs left–right relative to its facing.

| Additional material | Quantity |
| --- | --- |
| Stone Bricks, full blocks | 232 |
| Stone Brick half slabs | 160 |
| Oak Fence Gates | 6 |

Cut the slabs with a saw; native stone-brick slabs in the correct lower-half orientation also count. Build with the [construction guide](machines.md#building-with-a-controller) by right-clicking the controller with matching stacks or placing pieces yourself.

The oval is **29×19 blocks**; with its two opposed prep rooms the structure is **41×19**, centered on the controller. The wall stands 2.5 blocks above the floor. Each room has a **5×5 interior**, three blocks of headroom, a lower-slab roof, a centered one-block rear opening for players and a three-wide inward opening closed by three controller-operated oak fence gates.

Only the controller, oval walls, room shells and roofs and gates are validated. Ground, paving and interior air are not: players may place any blocks, furniture, obstacles or liquids inside and raise or dig the terrain. Build the shell on flat ground. The pit is stationary land infrastructure and refuses to operate aboard vessels. [ArenaLayout.java](src/main/java/dev/civilization/ArenaLayout.java) is the authoritative geometry.

## Fights

Empty-hand right-click opens the controller. On claimed land the public may enroll, wager and collect without gaining any other access to the claim. The placer can cancel as host and must keep structural claim rights over the footprint.

1. Choose **Join A** or **Join B**. A player can enroll in one arena at a time, and a spectator with a wager in this match cannot fight in it.
2. Optionally select a hotbar stack and press **Stake held** to deposit the whole stack; **Refund stake** returns it to your collection credit. Fighters may stake different items or nothing. Both escrow previews show the actual stacks.
3. Both press **Accept terms**. Any change to participants or fighter stakes clears both acceptances; depositing never authorizes a match.
4. Both press **Ready / enter prep**, which requires Survival, full health and no active effects. Each fighter moves to a dry, clear standing spot in their prep room at the nearest usable height below its roof (if none exists, Ready fails with a message). Ready fighters cannot change stakes. The gates close; when both are ready, betting locks, unmatched offers are refunded and a **five-second countdown** starts.
5. The gates open together. Enter the oval within **ten seconds**. Melee and ordinary player projectiles, including the Paterson, can damage only the enrolled opponent inside the fighting area, at any terrain height. Prep rooms are safe, and returning there after the entry allowance aborts the fight. Explosions, pets, mobs, the environment and outsiders cannot damage protected fighters, and fighters cannot harm spectators or mobs through the arena.
6. A would-be lethal hit becomes a **knockout** before health is lost, and the winner receives both fighter stakes. Nothing drops on knockout. Weapon and armor wear, consumables and ammunition are spent normally. Both fighters return to their entry health with fire, fall damage and harmful effects cleared; inventories are never copied or restored.

Enrollment and preparation last at most **ten minutes** from the first join; a live fight at most **five minutes**; the result has a five-second protection interval. Fighters stay where they finished. Consent applies only inside a live match, even where server PvP or team friendly fire is off, and enables no global PvP.

**Withdraw** or `/arena leave` cancels participation and refunds the unfinished match. The host's **X** button and operators can abort a match. Gates reject manual use once a fighter is ready. Joining a lobby gives no damage immunity; ready protection applies only inside this arena.

## Spectator wagers

Spectators select a held stack and choose **Back A: held** or **Back B: held**, escrowing the whole stack as an open offer. Browse offers with the arrows; **Match bet** backs the other fighter by depositing identical items, components and quantity from the main inventory (named or otherwise distinct stacks do not match). If each backer stakes eight Coal, the winning backer receives sixteen.

Each spectator places one wager per match, creating or matching; a match holds at most **64 offers**. Fighters cannot bet on their own match. There are no odds, currency, house cut or remote betting. **Cancel offer** refunds your unmatched offer; matched wagers stay committed until the result or a refund. Betting closes with the countdown.

Winnings and refunds become saved collection credits. **Collect** at any Gladiator Pit or `/arena collect` moves complete owed stacks into free main-inventory space; anything that does not fit stays owed and nothing drops. Credits survive the controller's removal. The menu shows the first owed stack and the total owed.

## Interruptions and persistence

Leaving the match or staging area, changing dimension or game mode, a fighter disconnecting, the placer losing claim rights, required terrain unloading, damage to required blocks, controller removal, preparation expiry, a time-limit draw or a server restart refunds each depositor's own unfinished stakes. There is no forfeit for leaving, so a deliberate abort avoids a loss.

Results and refunds settle once on the server thread, checking terms, menu identity, range, revision, actor, offer ownership and items at action time; the structure is revalidated before a knockout is awarded. Escrow, credits and pending health recovery live in `civilization_arenas` saved data independent of any block entity, and unfinished matches never resume after a restart. A crash between Minecraft's separate world and player saves is not transactional.

Layout and guide geometry are cached. An active match validates its footprint once per second and on player actions and awards, never loading chunks; idle pits do no scans.
