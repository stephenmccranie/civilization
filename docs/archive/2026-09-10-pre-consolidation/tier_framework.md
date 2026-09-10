# Three-tier civilization framework

Recorded September 10, 2026.

**Confirmed direction:** use tiers as an organizing principle for energy, food, materials and productive capability, with calories supporting human labor throughout. The agreed industrial progression is **Tier 1 coal → Tier 2 oil → Tier 3 uranium**. Higher-tier materials and energy sources occur in large, rarer deposits and require real infrastructure to extract.

Industrial fuel identities are agreed; other assignments and detailed mechanics remain proposals, not finalized recipes or implemented features. This framework organizes the regional economy; it does not assign social ranks to players.

The expanded [tier_matrix.md](tier_matrix.md) now works backward from the already documented Tier 3 capabilities into material families, extraction facilities, energy inputs and bootstrap dependencies. It is a draft specification; it does not reopen the broad purpose of Tier 3. Fuel identities are agreed; material choices and process details remain proposals.

## Working tier matrix

| | Tier 1 — Coal | Tier 2 — Oil | Tier 3 — Uranium |
| --- | --- | --- | --- |
| Industrial energy | Hand-accessible coal for heat and first machinery | Crude oil refined into fuel for engines, transport and larger industry | Processed uranium fuel used in reactors supplying major installations |
| Labor energy | Calories | Calories | Calories |
| Resources | Common materials, base metals and hand-mineable coal | Large, rare oil reservoirs and proposed bulk material deposits | Rare large uranium deposits and proposed advanced-material deposits |
| Production | Hand tools, farming, kilns, fertilizer and first machinery | Drilling, refining, bulk processing, mechanized extraction and freight | Advanced materials, enormous throughput, infrastructure with regional reach |
| Food | Basic ingredients and simple meals | Prepared provisions suited to sustained work and transport | Concentrated expedition provisions and costly luxury foods |
| Typical scale | Person, household, small workshop | Farm estate, mine, factory, town | Industrial network, major institution, sky civilization |

Players and organizations can operate across tiers. Higher-tier infrastructure should increase the scale of action, not simply multiply tool speed. The sky civilization is a proposed concentration of Tier 3 capability, not the only place high industry may exist.

## Design rules

1. **Access comes through infrastructure.** Discovering a higher-tier deposit reveals a valuable site; it does not make its resources extractable through enough ordinary pickaxe swings. A Tier 2 operation might require powered drilling and material handling. Tier 3 requires more substantial supporting facilities. Exact extraction mechanisms remain open.
2. **Large reserves justify investment.** Higher-tier deposits should support mines, settlements, warehouses and supply routes long enough to make their construction worthwhile. Determine quantities from consumption and the chosen world-lifespan policy; do not choose arbitrary sizes now.
3. **Each tier can begin the next.** Tier 1 materials and capability must enable the first Tier 2 extraction facility; Tier 2 must enable the first Tier 3 facility. Audit the bootstrap path to prevent recipes requiring resources available only from the machine being constructed.
4. **Higher tiers continue needing lower tiers.** Advanced facilities consume bulk materials, ordinary industrial inputs and provisioned labor. Progression expands the supporting economy rather than making its farms and mines obsolete.
5. **Food retains one calorie scale.** Food tiers describe preparation, carrying density, convenience, expedition suitability and luxury. They do not introduce separate biological energy currencies or make basic food unusable. Packing must conserve food energy unless additional ingredients supply it. The field ration already demonstrates calorie-neutral concentration per inventory slot.
6. **Industrial energy conversions have losses and explicit inputs.** Calories and food cannot power generators. Advanced fuel requires appropriate feedstock and facilities, not unlimited compression of ordinary coal. Define allowed conversions and usable output; the universal-energy principle does not permit converting every form into every other form.
7. **Three tiers organize capability, not item quotas.** Do not manufacture three versions of every metal, meal or machine merely to fill the table. A new material or machine needs a distinct economic function.

## Relationship to hierarchy and geography

Rare deposits make particular places worth discovering, developing and controlling. Extraction infrastructure turns a find into productive capacity. Fuel, materials, labor and transport connect that capacity to other players' plans. Ownership and organization determine who benefits from those flows.

The underlying chain remains **energy → surplus → capability → dependency → power → status**. Tiers should make that chain legible without replacing it with a prestige score, an assigned player class or a mandatory social ladder.

## Proposed placement of the existing build

| Existing feature | Working placement | Qualification |
| --- | --- | --- |
| Calorie reserve, hand labor, ordinary farming | Foundation across all tiers | Renewable subsistence and the base labor economy |
| Basic food | Tier 1 | Remains useful at every stage |
| Field rations | Tier 2 food preparation | Improved carrying density, not a new energy type |
| Mineral Coal, kiln, Fertilizer Works | Tier 1 coal industry | Hand-mined fuel supports the first industrial stage |
| Fertilizer-assisted agriculture | Available in the coal stage | Depends on finite-fuel manufacture while ordinary farming remains available |
| Uranium fuel processing, reactors and regional instruments | Tier 3 | Not implemented or fully specified |

**Existing coal now has a clear place:** hand-mined Mineral Coal is Tier 1 fuel. No separate starter-fuel item or inventory migration is needed to fit this design. Coal-stage materials and power must establish the first oil extraction/refining facilities; oil-stage industry must establish initial uranium extraction, fuel processing and reactors. Large infrastructure-gated oil and uranium deposits remain unimplemented. Earlier fuels remain useful, especially oil for engines and freight after reactors. Food preparation tiers need not match industrial fuel tiers one-to-one.

The clay/gravel fertilizer blend remains a placeholder. Its eventual feedstock should be chosen within this tier framework rather than independently inventing a new ore chain.

## Next specification checklist

- [x] Establish coal → oil → uranium as the industrial progression, with calories supporting every stage.
- [ ] Define the practical capability boundary between stages.
- [ ] Assign energy sources and physical carriers, including which sources are extracted and which are processed products.
- [ ] Define each strategic deposit's location pattern, rarity, size and extraction infrastructure.
- [ ] Define permitted energy conversions, losses, operating costs and exhaustion behavior.
- [ ] Trace the complete Tier 1 → Tier 2 → Tier 3 bootstrap path.
- [ ] List material outputs and continuing dependencies on lower-tier production.
- [ ] Specify food progression without duplicating calorie currencies or invalidating basic food.
- [ ] Specify inputs, operating requirements and buyers for the already documented Tier 3 capabilities, then trace their Tier 2 and Tier 1 dependencies. The broad endgame purpose is established; implementation order and concrete recipes remain open.
- [x] Place current Mineral Coal and its machines in Tier 1.
- [ ] Resolve the fertilizer-feedstock placeholder.
- [ ] Connect reserve quantities to the still-open world-lifespan policy.

**Deliverable:** one concrete tier matrix and its input/output dependency table. Use them to choose the next coherent development package. Do not jump directly from this framework to implementing every candidate machine.

Current work schedule: [development_plan.md](development_plan.md). Confirmed design context: [design_direction.md](design_direction.md).
