# Calorie session review — September 10, 2026

Snapshot: player 19BitPrice, Civilization 0.5.0-dev, New World. Captured 04:23:20–04:31:34 MDT (10:23:20–10:31:34 UTC), 8 minutes 14 seconds of wall time. The game was still open; this is a bounded snapshot, not a completed-session report. Source: world/civilization-energy/energy-current.jsonl. Reproducible parsed snapshot: latest-session-snapshot.json.

## Accounting

574 records; no per-record balance errors and no balance discontinuities between successive player entries. No admin calorie changes, depletion transitions, healing, Hunger drain, or starvation events in the snapshot. Costs were fully paid; no food overflow occurred.

Starting reserve: 1,132.287 kcal. Food gained: 825 kcal. Calories spent: 848.028 kcal. Ending reserve: 1,109.259 kcal. Net change: -23.028 kcal. Lowest recorded reserve: 292.566 kcal, immediately before eating cooked porkchop.

| Activity | Quantity | kcal spent |
| --- | ---: | ---: |
| Breaking blocks | 119 | 476.00 |
| Placing blocks | 80 | 160.00 |
| Walking/other nonsprinting travel | 354.82 blocks | 35.48 |
| Sprinting | 295.15 blocks | 88.55 |
| Jumping | 41 | 82.00 |
| Successful melee attacks | 2 | 6.00 |
| Total | | 848.03 |

Breaking included 79 stone, 15 oak logs, 7 dirt, 3 cobblestone, 2 grass blocks, 2 leaves, 7 flowers, and 4 short grass. Placement included 58 cobblestone, 11 dirt, a crafting table, a furnace, and 9 torches (6 standing, 3 wall-mounted). Torches cost 18 kcal altogether.

Food: one cooked porkchop at 04:29:38 (+800 kcal) and one foraged morsel at 04:29:57 (+25 kcal). One forage began at 04:29:42.817 and completed at 04:29:52.799 (~9.98 seconds). Acquiring the morsel correctly added no body calories until it was eaten.

## Interpretation

- Work is the dominant expense: breaking and placement together cost 636 kcal, exactly 75% of total spending. Food demand came primarily from gathering and construction.
- A single substantial food item almost covered this productive burst. The morsel brought intake within 23 kcal of spending. This supports the user's impression of manageable food upkeep in this small test.
- Recorded travel matches configured distance rates: 0.10 kcal/block walking and 0.30 sprinting. Jumping is separately charged, so actual sprint-jumping costs more than flat sprinting.
- At the observed wall-time pace, consumption was about 103 kcal/minute. A full 2,400-kcal reserve would last about 23 minutes at that same pace with no food. This is a short-session extrapolation, not a general playtime estimate; walking around a home or idling will differ greatly from a construction burst.
- Emergency forage would need 34 morsels to replace this session's costs: 5 minutes 40 seconds of searching alone, plus eating and interruptions. It works but is substantially less convenient than one porkchop.
- Two tuning candidates surfaced: foliage/flowers/short grass cost the same 4 kcal per break as stone (52 kcal total across 13 such breaks); and cooked porkchop currently supplies 800 kcal via the fallback while explicitly configured cooked beef supplies 700. Neither is necessarily a bug, but both merit intentional balance decisions.

## Limits and recommendation

Keep the current broad rates for now. This session supports the ordinary gather/build/eat loop, but does not establish the economics of farming, depleted recovery, combat/healing, large-scale mining, or server-wide food trade. It included no natural healing or depletion. The audit records melee hits without target entity IDs, so it cannot independently verify which animal was killed. Block events do not describe a building's shape; the house identification comes from the user's account.

No balance changes were made. Next useful refinements would be a small intentional food-value table and lighter costs for incidental vegetation, rather than changing the overall calorie budget based on one session.
