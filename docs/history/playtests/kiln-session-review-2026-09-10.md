# Kiln session review — 0.7

Source: Civilization Dev / New World / civilization-energy / energy-current.jsonl.
Server run: `ef51dc68-b74c-4f16-a64b-9c23590e6c2b`.
Session UTC: 2026-09-10 11:08:57.988 to 11:15:48.500, approximately 6 minutes 51 seconds wall time.

There are 481 records, with sequence numbers 1 through 481. Five are machine events; 476 are player/body or crop-plant events.

## Production

The kiln at overworld (-101, 65, -108) consumed **one Mineral Coal** and completed **four batches: four clay blocks → sixteen brick items**. Fuel ignition was tick 6081. Completions were ticks 6280, 6496, 6696 and 6896. The later two intervals are exactly 200 ticks; the first interval between completed batches is 216 ticks. This is consistent with ten-second batches and a short interruption, but the cause is not recorded.

The recorded output is half the maximum 32 bricks per fuel under continuous supply. Do not interpret this alone as a fuel-accounting fault: the journal logs ignition and completed batches, not remaining heat, idle time, input transfers or output withdrawals. It does not prove the player collected all output or establish full utilization.

## Body calorie ledger

Start: **888.81 kcal**. End: **1,537.97 kcal**. Minimum recorded: **565.62 kcal**.
Food restored **1,400 kcal** in two eating events. Activity spent **750.84 kcal**, for a net **+649.16 kcal**.

| Activity | Quantity | kcal spent |
| --- | ---: | ---: |
| Block breaking | 76 | 304.00 |
| Placement, including six crop plants | 20 | 34.00 |
| Jumps | 61 | 122.00 |
| Walking | 293.06 blocks | 29.31 |
| Sprinting | 338.45 blocks | 101.54 |
| Natural healing | 4 health points | 160.00 |

Machine outputs do not alter player calories. No depletion occurred in the recorded balance range. No rate change is justified by this session alone; keep the current calorie and kiln rates.

The user's requested next progression is implemented in 0.8: gate renewable brick manufacture behind the kiln and add a separate brick-built Fertilizer Retort. Bigger coherent development steps are preferred over requiring another manual playtest for each addition.
