# First playable economy: farm, mine, and sky estate

Status: historical numerical design experiment, not approved balance or implemented behavior. All numbers are invented, tunable game values. Its next-prototype sequence is superseded by [development_plan.md](development_plan.md); do not use its generator, ration or sky-core numbers as current recipes. See `design_direction.md` for confirmed intent.

## Question this experiment answers

Can ordinary players feed themselves while commercial farms profitably supply heavy labor, industrial owners control meaningful flows, and a sky estate demonstrates an extraordinary ability to mobilize resources?

Build this small chain before a full modpack. Do not interpret a working spreadsheet-style calculation as proof that players will enjoy the tasks or choose to trade.

## Common units and assumptions

- One ration (R) is an accounting amount of edible energy, not one item or eating animation. Food items can contain different amounts.
- One fuel unit (F) is an extracted, finite resource package. It is not necessarily one Minecraft coal item.
- One industrial energy unit (E) is a shared machine accounting unit. Calories and E need not be numerically convertible because food-to-industrial-power conversion is prohibited.
- One hour is 60 real minutes. Production rates below refer to active worker-hours over repeated harvest cycles, assuming sufficient established land and crop availability. They do not grant output on a timer or promise that plants mature in an hour.
- Capital construction, exploration, transport, trading time, taxes, and profit margins are excluded from the first arithmetic. Measure and add them before balancing a real economy.

## First parameter table

| Parameter | Trial value | Purpose |
| --- | ---: | --- |
| Light activity | 1 R/hour | Accessible ordinary play |
| Sustained mining, farming, or comparable heavy work | 3 R/hour | Work creates food demand |
| Manual farm gross harvest | 12 R/worker-hour | Positive subsistence surplus |
| Fertilized farm gross harvest | 36 R/worker-hour | Intensive land/labor productivity |
| Intensive farm fertilizer use | 24 doses/worker-hour at this output | Bounded bonus per treated harvest |
| Fertilizer energy cost | 5 E/dose | Industry supplies agriculture |
| Fuel extraction at a working deposit | 8 F/worker-hour | Finite fuel requires labor |
| Fuel energy content | 100 E/F | Chemical energy bookkeeping |
| Generator efficiency | 40% | 40 usable E/F; remainder lost |

Heavy-work rates are totals, not extra charges on top of the light-activity rate. Actual implementation should charge categories of valid work and movement; a stationary player holding a pickaxe does not count as continuous mining.

Fertilizer's trial recipe is a simplified powered process with implicit ambient inputs; no finite mineral ingredient is specified yet. The farm needs 120 E, or 3 F, per intensive worker-hour. This is fertilizer production energy only. The first farm is manually worked; any later machinery must add its own fuel and maintenance costs.

One treated-harvest allowance yields 1.5 R versus 0.5 R untreated in this toy model, consuming exactly one fertilizer dose. The worker processes 24 such allowances per hour. These are output-equivalent batches, not a required crop count. This defines the arithmetic without assuming a particular crop mod.

Food energy comes from renewable growth. Fertilizer improves yield; it is not a claim that 5 E turns directly into a given number of calories. There is no return conversion into industrial energy.

## Subsistence check

A person spending one hour on a mix of farming and light activity can sustain that hour with about 6 minutes of farming at the trial rate:

`12t = 3t + 1(1 - t)` gives `t = 0.1 hours`.

An otherwise continuously heavy-working player needs about 15 minutes farming per hour:

`12t = 3`, giving `t = 0.25 hours`.

These are steady-state food budgets with established crops, no travel, and no stockpile buildup. Initial crop growth and seed acquisition need separate onboarding tests. This makes buying food a potential way to recover productive time, while basic independence remains possible.

Prototype food policy: allow at least one directly edible staple. Better meals can improve food carried per inventory slot. Do not require fuel merely to avoid starvation. All powered cooking or processing uses finite fuel; no wood/charcoal fuel exception is assumed.

## Six miners supplied for an hour

Six miners consume 18 R and extract 48 F during one hour of actual mining. Farming is additional worker time; this comparison does not assume the same six players are simultaneously farming.

| Supply method | Farm labor | Gross food | Farmer food during that labor | Food delivered before transport | Fuel consumed making fertilizer |
| --- | ---: | ---: | ---: | ---: | ---: |
| Manual | 2 hours | 24 R | 6 R | 18 R | 0 F |
| Intensive | 0.545 hours | 19.636 R | 1.636 R | 18 R | 1.636 F |

Intensive farming saves approximately 1.455 farm-worker-hours but consumes fuel worth 0.205 extraction-worker-hours at the trial extraction rate. That leaves **1.25 worker-hours of potential labor savings**, before delivery, fertilizer handling, equipment costs, and other overhead. It is an opportunity for trade, not a guaranteed profit or administered price.

The additional fuel extraction also consumes food. Closing that small recursive input at the same intensive production rate gives approximately 0.565 farm-worker-hours and 1.694 F to supply the six miners plus the extra fuel miner's food. This still omits transport and other operators; those must enter the ledger when specified.

Players negotiate the distribution of this surplus. A farm can be independent, mine-owned, rented, or controlled by a larger organization. The design does not require trade between independent firms or prohibit vertical integration.

## Provisional sky asset: one estate service core

The setting still needs creative development. Use a placeholder core to test the economic scale, not to settle sky-world fiction.

- Construction consumes a batch of manufactured components plus player-built architecture. Exact capital cost is deferred until component production is measured.
- Trial operation consumes **1,600 E per active service-hour**, equivalent to **40 F** through the trial generator. This represents five fuel-extraction worker-hours per service-hour, before agriculture and transport.
- Power enables a prototype private access terminal and an estate lighting/display effect. These are disposable proof-of-concept effects, not the final reward for elite status.
- The owner sets terminal access permissions. No public access is assumed.
- The first test charges while the service is enabled and simulated. Continuous offline billing is not implemented or assumed; the long-term operating clock is a later design decision.
- On shortage, the terminal and powered display stop. The structure remains. A basic exit remains available to occupants. No falling-city simulation or confiscation is proposed.
- Energy state and permissions must survive restart. Failure behavior must be explainable without moderator intervention.

This cost is intentionally conspicuous but entirely provisional. A single six-person mining crew can almost entirely occupy itself supporting one operating estate. Test whether that is compelling concentration of surplus or simply excessive repetitive labor.

## What the numbers reveal

1. Intensive farming can save enough labor to support a real commercial relationship without making subsistence impossible.
2. Food demand is bounded by active players and their work. At 200 active players averaging 2 R/hour, consumption is about 400 R/hour. At the trial intensive net output of 33 R/worker-hour, roughly 12 farmer-equivalent hours per wall-clock hour could cover that demand before logistics and upstream adjustments. This is rough market sizing, not an exact population equilibrium.
3. Cheap automation could let one farm saturate that entire market. Evaluate actual output and operating cost before adding automated harvesting.
4. Fuel ownership matters only if there are desirable consumers. Fertilizer alone consumes little of the example mine's output; advanced manufacturing and elite infrastructure must provide additional demand.
5. Stockpiles legitimately buffer suppliers' leverage. Measure burn rates and storage rather than introducing spoilage automatically.
6. The amount of interesting work per player matters as much as resource scarcity. If mining is merely a tax paid to reach the enjoyable content, change the activity or costs.

## Next prototype, in order

1. Map ration costs to representative mining, construction, travel, and farming actions. Avoid charging unsuccessful clicks and prevent free calorie refills through death/respawn.
2. Test one edible crop, one fertilizer treatment, one finite fuel deposit, one generator, and one useful powered recipe in a tiny test world. Establish actual harvest and extraction rates before expanding content.
3. Test the six-miner provisioning scenario with human players or timed role sessions. Record labor, food, fuel, trip times, and whether buying food is preferable to self-supply. A small test provides no evidence of 200-player capacity.
4. Add the placeholder estate consumer to test whether operating surplus feels significant. Separately develop the actual sky experience so the reward is more than an expensive switch.
5. Check every available alternative food/fuel path, crop multiplier, generator, and portable storage route. Then profile the chosen implementation at growing load.

For the first closed test, use a fixed finite fuel reserve and reset the disposable test world as necessary. This does not select the live world's lifespan policy. Persistent frontier expansion versus campaign resets remains open.

## Next decision after this model

Define what owning a sky estate actually allows its owner to do, experience, and display. That determines whether players want the surplus these systems make possible. Then compare compatible mod/loader candidates against this small specification.
