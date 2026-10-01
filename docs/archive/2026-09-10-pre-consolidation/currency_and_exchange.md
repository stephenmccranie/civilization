# Shared currency and exchange — decision draft

Status: **physical industrial fuels serve as tiered currencies: Mineral Coal for ordinary trade, oil and uranium for higher-level transactions because of their greater energy density.** Physical shops and funded purchase orders are accepted direction. Exact oil/uranium forms, denominations, packaging and settlement details remain open. Earlier abstract-money issuance and coal-only proposals below are historical context where they conflict with this decision. No economy code is changed.

## Current direction: energy is money

## Agreed shop and discovery model

Use one trade counter attached to physical storage, with sell and buy modes. A sell offer exchanges stocked goods for a specified fuel quantity. A buy offer pays reserved fuel for delivered goods. Each offer names one accepted fuel form and quantity; there is no automatic fuel conversion. Both modes operate while the owner is offline. Customers interact with the offer rather than gaining access to underlying owner inventories.

Trades complete only when the goods, payment and receiving space are available. Exact storage attachment, concurrent transaction behavior and permissions still need specification. These rules are agreed design, not implemented features.

**Shops and their offers appear on the map.** Discovery can happen remotely, while exchange and collection remain at the physical location. The map must not become remote cargo delivery or grant withdrawal access to the shop. No map mod, world-map implementation, complete terrain reveal or shop-visibility radius is selected by this decision.

Recommended initial map interaction: select a shop marker to see its offers, fuel denomination, quantities and current availability, then use its location to plan a trip. Labels, filtering, freshness, owner identity and navigation controls remain proposals. Displayed stock is informative, not a reservation; the transaction must recheck availability at completion. Remote ordering or reservations are separate undecided features.

The next planning task is to define that small map/discovery interface and the information visible about settlements, terrain and ownership. Do not automatically expose private inventories or undiscovered deposits just because shops are listed.

## Fuel currency details

**Agreed extension:** coal is not the sole required denomination for the entire economy. Oil and uranium also circulate as currencies for higher-level activity. Do not require players to convert a major transaction back into a huge coal payment. These are physical resources that can be traded and ultimately consumed through their respective industrial processes, not free account credits.

Agreed market interface: the seller chooses an accepted fuel form and quantity for each offer; a funded buy order reserves that same physical resource. A buyer supplies exactly the listed payment. Separate exchange offers let players trade fuels at negotiated rates. No automatic currency conversion, fixed energy-content exchange rate or simultaneous multi-fuel price basket is required initially. Detailed fulfillment behavior remains to be specified.

An advanced payment must identify an actual item or fluid and its quantity. Whether oil payment uses crude or refined fuel, and uranium payment uses ore, processed material or reactor fuel, is unresolved. Energy density does not make every intermediate directly usable: processing, location and packaging still affect value. Choose useful standard forms alongside their eventual material chains.

Reconcile denominations with freight deliberately: advanced fuel can carry more purchasing power in less space while bulk industrial deliveries remain impractical in player inventories. Do not disguise an industrial-scale oil shipment inside one unrestricted portable tank. Price, useful energy, stack limits and container payload are separate quantities to specify together. Fuel movement remains physical even when the payment fits in a pocket.

Use a form of energy as the exchanged value rather than introducing an unrelated currency with arbitrary starting grants. Recommend finite industrial energy, not renewable food calories, so agriculture does not become an unlimited industrial-money mint. The exact eligible form still needs a decision.

Two implementations remain to compare:

- **Physical energy carrier:** players exchange a standard fuel or stored-energy item that can also be consumed. Simple material accounting, but denominations, changing fuel tiers and payment transport need design.
- **Claims on stored energy:** balances transfer title to a defined quantity held at a named physical location. This can simplify payment without moving goods, but requires custody, redemption, reservations and reliable accounting. It is more infrastructure and is not automatically authorized by choosing energy currency.

Do not introduce an unbacked balance and call it energy. Extraction or conversion produces the spendable resource; consumption removes it. If a ledger represents stored energy, spending, withdrawal and industrial consumption cannot claim the same stock twice. Selling fuel for energy is an exchange of quantities/forms, not a minting event in addition to keeping the fuel.

Energy content alone does not settle market price. Coal, oil and uranium have different useful forms, processing needs and access costs. Do not declare fixed exchange rates or universal convertibility among them. A shared price denomination need not erase their distinct industrial roles.

Physical location remains economically meaningful. A payment must not redeem fuel remotely from an unrelated store and thereby bypass freight. Stored-energy claims, if chosen, identify custody and collection location; no global redemption network is implied.

Land allocation remains connected: if payment consumes energy, name the real operation or explicitly chosen game rule that consumes it; if payment transfers energy to a seller or reserve, identify the recipient and subsequent custody. Do not invent a land generator, perpetual demand or nominal energy tax as an assumed solution.

**Next decision:** choose the energy carrier and whether it circulates directly or through stored claims. A physical carrier is the simpler baseline to evaluate first. Ordinary material trading can start before choosing an advanced electricity-storage network. No free starter-energy grants, default food conversion, battery item or mint machine is approved by this direction.

Context: [systems_blueprint.md](systems_blueprint.md), [property_and_authority.md](property_and_authority.md), [transport.md](transport.md). The original design treats money as a means of acquiring productive goods and services, not a prestige stat or substitute for material power.

## Mineral Coal as the first physical currency

Status: Mineral Coal is accepted as the initial physical currency; oil and uranium extend the same role at higher levels. Use existing Mineral Coal directly as payment, without requiring an account balance or new energy-storage technology. A coal-priced offer exchanges a quantity of coal for a quantity of goods. The coal received can be spent again or burned in a compatible machine. Detailed shop behavior below remains proposed.

| Interaction | Proposed behavior |
| --- | --- |
| Buy goods | Exchange carried coal for stocked goods at the shop |
| Fulfill a purchase order | Deliver the requested goods and receive coal reserved in the shop |
| Seller is offline | Payment remains in the shop's secured inventory for later collection or authorized use |
| Pay for work | Transfer coal directly; no automatic payroll or arbitrary work verification is implied |
| Exchange fuels | A seller may offer oil or uranium for coal or another chosen fuel; no fixed equivalence or automatic conversion is promised |
| Pay for land | A physical payment/escrow destination and recipient still need design; land charges do not automatically destroy fuel |

This is a single common payment commodity with a shop interface, rather than a distinct minted token. One whole coal is the initial proposed denomination. Batch prices can handle small purchases without introducing fractions: a listing can sell several items for one coal. Whether that granularity is convenient must be checked against food and fuel values; no actual prices are selected.

All payment fuel remains physical stock, subject to stack limits and custody. Shop/payment storage must not double as a free remote withdrawal network. Reserving coal for an order prevents it from also being burned or withdrawn. Transfers must commit both goods and payment together and preserve stock under restart and retries.

Main tradeoffs: paying consumes carrying space; large purchases may require delivering payment; scarcity and industrial demand change each fuel's purchasing power. Oil and uranium provide denser payment options for higher-level transactions. Stored claims are a possible later feature, not part of the agreed first model. No assurance of stable prices follows from using energy currencies.

Audit the distinction between Mineral Coal and ordinary vanilla coal before implementation. The existing one-way conversion must not create spendable duplicates or let renewable charcoal qualify as currency. No new recipe or migration is approved here.

Evaluate denominations with a food purchase, timber shipment, boat refueling and substantial land bid. Coal is the first available currency; higher-fuel forms can be specified with the corresponding industry. Land bids across different currencies require a comparison rule or a single accepted currency per auction; the land mechanism remains open. Do not silently impose fixed conversion rates to compare bids.

## Historical alternatives — superseded abstract-currency proposal

The numbered sections below preserve earlier analysis. Their account-balance and starter-issuance recommendations are not active recommendations after the user chose energy currency. Relevant transaction-conservation concerns still apply; the actual representation depends on the chosen energy carrier.

### 1. Keep player interactions simple

Agreed exchange direction: a seller stocks a physical shop and sets prices; buyers collect there; shops work while owners are offline. A funded purchase order gives producers/carriers a place to deliver goods and receive payment. Goods never teleport. Actual transaction rules, discovery UI and custody remain to be specified.

Recommended currency representation: one server-maintained balance per player, with organization balances once organization permissions are defined. Prices use an integer smallest unit. No physical coin cargo, minting machine, interest, loans, exchange rates or multiple currency types are required initially. These are scope recommendations, not confirmed exclusions forever.

A payment can be remote without moving goods remotely. Physical pickup and delivery still determine freight demand. Whether users can browse all offers remotely is a separate open question.

## 2. The central question: how does money first enter circulation?

| Candidate | Attraction | Main tradeoff |
| --- | --- | --- |
| Modest starting balance for admitted players | Easy to explain; buyers exist at launch; later earnings come from other players | Creates money per admission; alternate accounts and free grants need controls; inactive accounts can reduce circulation |
| Limited recurring issuance to eligible participants | Can continue circulating money as population changes | Eligibility can reward idling or account farming; creates an income stream unrelated to demand |
| Resource exchanged for newly minted money | Tangible acquisition route | Selects a privileged resource and effectively gives it a server conversion price; mining money may displace useful production |
| Bounded treasury purchases of real deliveries | Injects money through productive work | Requires persistent demand, budgets and destinations; arbitrary purchase prices can become NPC price support |

Do not silently combine every candidate. Choose one initial issuance rule and explicitly define any exceptional grants. A fixed initial supply, per-newcomer issuance and recurring issuance are different policies.

Preliminary recommendation: investigate a modest initial balance for admitted players as the simplest prototype, with subsequent ordinary income earned through other players. This is not a final monetary model: it requires an account-admission policy and a compatible treatment of land payments. A blanket automated account grant on an open server is not resistant to alternate-account farming.

## 3. Where payments go

Distinguish transfers from removal or creation of money:

- Player purchase, wages or rent to another owner: money transfers; total supply is unchanged.
- Funded order: money is reserved, then paid or returned; reservation does not destroy it.
- First land purchase or recurring land charge paid to the server: choose explicitly between removal and a treasury balance. A treasury needs an explicit spending policy; merely retaining funds does not return them to circulation.
- New grants or resource minting: money is created under a stated rule.

Land is the main dependency. A recurring competitive lease model, if chosen later, could continually withdraw purchasing power. Combining that with a fixed or newcomer-only money supply without a return mechanism may make circulation increasingly constrained. Equally, unlimited payouts are not made sustainable merely by adding a transaction fee. Choose issuance and land-payment destinations together, without presuming that a formal government or player jurisdiction exists.

Do not automatically impose wealth taxes, balance decay or confiscation of inactive balances. Stockpiling and concentrated wealth are legitimate outcomes. Their effects on circulation must still be considered when selecting the policy.

## 4. Prices and demand

Recommend player-set prices with no infinite NPC buyer/seller for ordinary goods. Creating a currency does not create a reason to buy food, transport or materials; those reasons still come from calorie expenditure, production, construction, distance and access.

No automatic commodity peg, cost-of-living target or guaranteed resale value is selected. Resource depletion can change relative prices; do not label every price increase a money-supply problem. Test a small goods basket, trade volume, active/idle balances and land-payment flows before adjusting the policy.

Use illustrative numbers only after the mechanisms are chosen. The currency's name and denomination can wait.

## 5. Transaction correctness for later implementation

Treat the balance ledger as persistent authoritative state, separate from the existing best-effort calorie/machine logs. Purchases need an atomic outcome linking payment to actual stock. Orders reserve funds; cancellation releases only the unspent amount. Partial fulfillment, simultaneous customers, disconnects, restart and retry must not duplicate or lose money or goods.

No overdrafts in the first proposed model. Organization spending uses scoped authority. Every mint/burn/admin adjustment needs an explicit reason and audit record. Test accounting conservation across available and reserved balances; define backup consistency with inventories and property state.

These are requirements to specify and verify before coding, not evidence of an existing ledger.

## Earlier discussion — superseded by energy-currency direction

First choose whether currency is an abstract account balance or a physical resource-backed object. Recommend the account balance to keep freight about goods and keep payment reliable. Then compare starter issuance with alternatives in the context of land acquisition/holding charges. Keep uncertainty visible; do not fill it with a standing administrator obligation to tune prices or purchase unwanted stock.
