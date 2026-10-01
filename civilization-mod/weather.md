# Regional weather and farming

Weather is local to fixed **512×512-block Overworld districts**. It never changes river eligibility or claim ownership. Use a Rain Caller from the Civilization creative tab to start 30 minutes of rain in your district. It has no survival recipe or consumption cost; it is a prototype for later tower control. Calling during rain does nothing and does not extend it.

Crouch-use a hoe on farmland to see **River-fed: 50% growth** or **Rain-soaked: 100% growth**, stored-water minutes and fertilizer status. The Survey Table inspection shows district grid lines and the viewer's current district/weather. There is no handheld map or permanent weather HUD.

## Rain and soil

- Natural storms last 25–35 minutes. Each following clear interval is independently sampled from an exponential distribution with a 12-hour mean. There is no dry cooldown, maximum drought or increasing guarantee of rain. Roughly 13.5% of intervals exceed 24 hours and 1.8% exceed 48 hours; these are probabilities, not a schedule.
- Eligible river farmland stays farmland without water trenches. It provides 50% growth even when dry. Placed water supplies no additional growth bonus.
- Exposed rain grows crops at 100% and adds four seconds of stored water per second, capped at 120 minutes. An empty field fills in 30 minutes. Afterwards it spends one second per second, including when empty or mature. Roofs block recharge; harvesting and replanting preserve the reserve. Breaking the soil discards it.
- Snow does not recharge soil. Cold or rainless eligible farmland retains the river baseline unless it already has stored water. The first release does not add melting, irrigation pipes or temperature simulation.
- Wet soil uses the active resource pack's dark farmland texture. Fertilizer changes yield, never growth speed or moisture.

Weather time advances at 20 ticks per second while the server runs, independently of the 60-minute day. Sleeping does not clear storms. Shutdown pauses it; severe server lag slows these nominal real-time durations. Unvisited districts begin their saved schedule when first queried; existing schedules catch up lazily without loading terrain.

**Soil and crops advance only in actively simulated chunks.** Unloaded soil freezes, retains its reserve and misses rain. There is no offline catch-up or forced loading. `randomTickSpeed=0` pauses this farm simulation; positive values do not multiply its rate. Vanilla light, plant support and headroom requirements still apply. Keep crops lit for uninterrupted night growth.

## Covered crops and harvests

Each cycle averages **two hours at full water or four hours at the river baseline**, with ±10% variation. Row layouts and vanilla random-tick growth multipliers no longer change the covered crops' rates. Melon and pumpkin stems need a full interval for each subsequent fruit too; they pause while their existing fruit remains attached or no placement space is available.

| Mature harvest | Ordinary | Fertilized |
| --- | --- | --- |
| Wheat | 1 wheat + 2 seeds | 3 wheat + 2 seeds |
| Carrots | 4 carrots | 10 carrots |
| Potatoes | 4 potatoes | 10 potatoes |
| Beetroot | 1 beetroot + 2 seeds | 3 beetroot + 2 seeds |
| Torchflower | 1 flower | 3 flowers |
| Pitcher crop | 1 plant | 3 plants |
| Melon fruit | 3 slices | 9 slices |
| Pumpkin fruit | 1 pumpkin | 3 pumpkins |

Carrots and potatoes reserve one planting item before tripling the remaining produce. One fertilizer dose applies to an immature crop or a detached fruit stem; it cannot stack. Fruit production consumes the stem's dose and records the fruit's yield until harvest. Replanting loses the dose, but retains water. Either half of a mature pitcher crop must produce one harvest. Fortune and Silk Touch do not multiply or replace these governed mature yields. Immature drops retain their normal planting rules. Torchflower/pitcher seed acquisition remains vanilla.

### Growable audit: explicit exceptions

| Source | Current treatment |
| --- | --- |
| Eight farmland crop types above | Shared soil, timing and fertilizer rules |
| Sugar cane, cocoa, sweet berries | Existing river/height eligibility plus native surface requirements, timing and yields; no farmland reserve or fertilizer |
| Saplings | Existing woodland eligibility/speed treatment; no farmland reserve |
| Bamboo, cactus, kelp, mushrooms, vines and Nether wart | Native growth pending habitat/resource policy |
| Grass, flowers, moss and other decorative spread | Native behavior except globally disabled bone-meal growth |
| Animal food, fishing and emergency foraging | Existing separate systems; not changed by this package |

The broader food/resource economy is **not fully normalized**. In particular berries and animal food remain balance work before the two-hour farming economy can be considered complete. See [open decisions](../open_decisions.md#regional-weather-and-farming-package).

## Commands and configuration

- `/civilization weather`: inspect your district; no operator permission required.
- `/civilization weather rain` or `clear`: operator level 2, target the command's position.
- Overworld `/weather rain` and `/weather clear` use the same regional control. Rain is always 30 minutes; an explicit vanilla duration argument is reported as ignored. `/weather thunder` is rejected here. Other dimensions retain vanilla commands.
- `doWeatherCycle=false` freezes the regional schedule in its current state. Existing rain still wets active fields. Re-enable to resume the remaining event time.
- World `serverconfig/civilization-weather.toml`: `meanClearHours=12`, `cropSeconds=7200`. Stop the world before editing. Changing crop duration applies to newly initialized cycles. Soil's water cap remains 120 minutes.

## Implementation and limits

`RegionalWeather` owns saved district schedules and the clock. Natural rain and the Rain Caller share `call`; later towers can use the same boundary. `SoilChunk` holds sparse per-soil state in chunk attachments. `SoilSystem` advances registered fields once per active second, without a block entity per plot. Plant/soil changes maintain the records; old fields are discovered by ordinary ticks. Existing partially grown crops retain their visible age while the new timer catches up. Mature crops are not destroyed.

The server checks local precipitation for exposed wetness, fire and precipitation ticks, including native snow/cauldron handling. Natural destructive thunder is disabled. Other mods that directly read the global weather flag are not automatically regionalized.

Clients receive the nine nearby districts once per second. Rain/snow particles sample their positions; sky strength fades over two seconds and blends near the viewer's district boundary. **Photon is the primary shader target.** Clouds, fog and shader wetness use viewer-centered shader uniforms; this is not an independently simulated cloud bank or physically moving storm front. Other shader packs remain unverified for this system.

No 200-player capacity claim: field work is proportional to registered active farmland, while district evaluation requires no terrain loading. Growable mods, exceptional harvesting tools and adversarial automation need specific integration rather than a universal compatibility promise. Validation and the installed build live in [status](../docs/status.md).
