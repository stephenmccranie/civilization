# Everyday play and the vanilla boundary

Status: first proposed rules sheet for Pass A of [systems_blueprint.md](systems_blueprint.md). This records existing behavior separately from recommendations. No gameplay changes or new approvals are implied. Implementation reference: [mod README](civilization-mod/README.md), version 0.9.2-dev.

**Confirmed design latitude:** vanilla systems can be disabled or substantially reworked. Simplicity and intuitive behavior take priority over vanilla preservation. The recommendations below are starting points to evaluate, not a commitment to retain familiar mechanics. Prefer one coherent replacement over many exceptions, and explain each change through its effect on play. This authorizes design exploration, not any particular removal or immediate coding.

**Confirmed carrying model:** use standard item inventories with item-specific stack sizes. Large or heavy items occupy more slots through low stack limits rather than a separate weight meter. Transporting large amounts of Tier 2 resources in a player's inventory should be impractical. The user cites Rust as the desired design reference; no exact Rust values are adopted. Exact stack limits remain to be specified.

## 1. The ordinary session

**Geographic constraint:** the user has established that farming works only within a certain distance of a river. Accessible subsistence therefore does not mean crops grow everywhere. Exact crop coverage, wild/animal food alternatives, river eligibility and distance remain open; see [regional_geography.md](regional_geography.md). Existing crop code does not enforce this rule yet.

Proposed experience: a player can explore, gather food, build a home, make tools and do useful work without joining an organization. Commercial activity becomes attractive when buying supplies or services saves time, grants access or enables a larger ambition. Independence remains possible; it need not be equally efficient at every location or scale.

Example: leave home with provisions, harvest a field, repair a building, take surplus to a nearby buyer and return with materials. Another player spends the same session exploring for a valuable site. A third supplies a major construction project. These are activities, not classes or required quests.

The session should involve decisions and visible progress. Avoid extra biological meters, repeated administrative chores and mandatory daily logins as substitutes for an economy.

## 2. Recommended simple baseline

| Area | Current or confirmed basis | Recommendation for the broader design |
| --- | --- | --- |
| Calories and recovery | One reserve, work costs, slower depleted mining, manual recovery; no idle/offline calorie drain | Retain the working model. Add missing action rules in a later audit, rather than another survival meter |
| Ordinary building | Manual construction and familiar blocks are the existing foundation | Keep manual building useful at every tier. Advanced construction expands scale; it does not make homes require industrial permits or tier unlocks |
| Tools and crafting | Existing vanilla systems plus custom recipes | Keep familiar manual tools and ordinary crafting initially; review recipes that bypass a specific energy or material rule |
| Food | Wheat/fertilizer, explicit food kcal and packed rations implemented | Keep accessible subsistence and more convenient provisions. Specify animal/crop yields before expanding the food catalog |
| Personal storage | Standard inventories with stack sizes as the agreed carrying constraint | Set low stack sizes for heavy/bulky goods, especially Tier 2 resources. Keep ordinary provisions and building supplies convenient enough for personal play; exact values remain open. No separate weight/encumbrance model is needed for this approach |
| Local logistics | Containers and hoppers are already used by machines | Retain understandable physical storage and transfer, subject to future permissions and active-world limits |
| Progression | Capability and infrastructure underpin hierarchy | Avoid character levels, profession locks and skill trees in the first design. Equipment, knowledge, location and organization provide progression |
| Guidance | Food tooltips, calorie HUD and contextual machine previews implemented | Explain actionable requirements at the point of use. Add a concise starting guide; avoid requiring a quest progression to use core systems |

These recommendations are not a blanket approval of every vanilla recipe, automation path or tool enchantment.

## 3. Travel and cargo: separate two questions

**Established:** goods move through physical routes; future airships are a major ambition. The current calorie system does not provide a complete transport-energy model.

**Recommendation:** begin with walking and sprinting, then choose a small coherent set of transport options. Boats, mounts and minecarts are candidates, not systems we must retain. Assess retained options for energy, capacity and permissions. Do not assume their default behavior satisfies the universal-energy rule.

Decide passenger convenience separately from cargo movement. A travel shortcut can preserve regional trade only if its actual cargo behavior supports that choice. “Travel without cargo” needs a precise definition covering equipment, portable containers and secondary inventories; it is not a free implementation detail.

| Mechanic | Why it needs an explicit decision | Current recommendation or open question |
| --- | --- | --- |
| Home/spawn teleport commands | Can move labor and goods across economic distances | Do not add unrestricted item-carrying teleport commands as convenience defaults. Whether to offer constrained personal travel is open |
| Nether routes and portals | Change route geometry and may dominate freight | Decide dimension access and route costs before setting travel distances; no dimension removal is approved |
| Ender storage | Separates cargo access from its physical location | Confirmed: disable shared remote storage |
| Portable filled containers | Greatly change carrying capacity | Confirmed: empty containers before carrying them as inventory items; no filled-container nesting |
| Elytra and other flight | Can bypass ground routes and the intended sky progression | Specify availability, cargo and energy alongside airships; default availability is not approved |
| Beds, respawn and death | Relocate players and potentially their inventories | Resolve with death rules below; never review freight shortcuts only in isolation |

Next deliverable is a short allowed-transport table, not a new vehicle implementation. Geographic distances remain provisional until this is decided.

### Agreed storage and freight rules

- Players retain standard inventory slots, with ordinary stacks capped at 32 and lower limits for bulky industrial goods.
- Stationary storage supplies warehouse capacity through additional slots. Ordinary chests can serve the first warehouses; a custom storage machine is not required.
- Cargo vehicles move their dedicated inventories physically. Vehicle types, slot counts, energy and handling remain unspecified.
- Containers must be emptied before they can become portable inventory items. Filled crates, shulker boxes and equivalent nested storage cannot bypass personal capacity. This does not prohibit an assembled cargo vehicle from physically carrying its inventory.
- Shared remote ender storage is disabled in the planned rules.

These rules are accepted design, not implemented changes. Later implementation must preserve existing contents when changing portable storage or disabling remote storage; never delete stored goods as an incidental migration. Breaking a loaded container must not produce a portable item containing its cargo. Exact unloading/drop behavior and vehicle dismantling remain to be specified.

The cargo capacity foundation and ordinary death model below are settled. General passenger travel, portals and vehicle choices remain open. Recovery edge cases need specification before coding.

### Bulk-resource carrying budget

**Confirmed baseline:** halve the ordinary Minecraft stack limit from 64 to **32**. This applies to common building blocks as well as other items that normally stack to 64; large personal block inventories are part of the problem being addressed. Keep the standard inventory layout. Bulky industrial resources can have lower item-specific limits. This does not decide whether existing 16-stack items become 8; unstackable items remain unstackable. These are planned rules, not implemented changes.

Apply each item's stack limit consistently in player, stationary and vehicle inventories. Freight and warehouses gain capacity through more slots, not a different stack rule for the same item. Before implementation, specify how existing oversized stacks are split and any overflow preserved without loss or duplication, including full inventories and unloaded containers.

Design stack limits from useful loads, not item rarity alone. Proposed roles:

- Ordinary personal supplies: enough food, tools and common building material for useful independent sessions.
- Tier 2 bulk resources: hand carrying supports sampling, repairs and small startup deliveries; sustained industrial supply needs dedicated cargo capacity.
- Processed parts and personal equipment: assign limits by the item's role and bulk rather than automatically applying the same limit to everything in Tier 2.
- Tier 3 bulk feedstocks: apply the same reasoning when specifying their processing chain; no exact carrying limits are decided yet.

For an item, maximum carried quantity is usable cargo slots multiplied by its stack limit. Evaluate both a normal mixed inventory and the maximum load obtained by emptying every available slot. Even a stack size of one permits many individual units, so recipe quantities, fuel yields and resource packaging must agree with the intended delivery scale.

Compare that load to a normal facility's demand before setting stack values. A personal delivery should have a useful small role while industrial freight carries materially more per trip. Vehicle capacity, handling and route costs remain open; low player stack sizes alone do not establish profitable transport.

Audit alternate forms together: storage-block compression, packed crates, filled portable containers, offhand slots and eventual liquid containers must not multiply personal capacity around the intended limit. Crude oil storage and unit sizes are still unspecified. A container's payload must fit the carrying budget rather than hiding a factory-scale delivery inside one convenient item. Larger stationary and vehicle inventories should retain their cargo advantage when moved or dismantled.

Acceptance examples: a player can provision a normal outing; bring a small industrial sample or repair supply; cannot casually carry a normal bulk Tier 2 shipment by repacking it; and has a practical reason to use a cargo vehicle or carrier for sustained supply. Quantities remain tuning decisions pending facility demand and inventory capacity.

## 4. Death, danger and absence

**Implemented:** calories and depletion persist through death and logout. Starvation alone stops at three hearts, while other damage remains dangerous. Inventory recovery and general death penalties are not specified by the custom calorie rules.

**Recommended direction:** setbacks should create recovery work without turning a routine building accident into a request for administrator restoration. Preserve risks and the need for provisions. No automatic restoration of lost energy, duplication of cargo or free relocation of a shipment.

**Confirmed ordinary-death model, not yet implemented:** all carried items, including equipment, remain in an owner-only recovery container near the death location. The player respawns at their bed or spawn point with their existing calorie and depletion state. The container persists through logout and server restart. Recovering possessions requires returning to the location; cargo does not teleport to the respawn point. This replaces ordinary inventory drops for these deaths and does not retain equipment on the player.

Before implementation, specify safe placement in lava/void or other hazardous locations, recovery on protected property, repeated deaths, retention limits and abandoned-container cleanup. Also define container protection against destruction and automation, atomic inventory transfer on death/recovery, and how recovery containers avoid becoming free permanent warehouse storage. Do not silently choose an expiry timer or arbitrary relocation distance. XP handling and health on respawn remain separate open questions. Organized PvP stakes remain separate from this ordinary-death rule.

Hostile mobs and environmental hazards can supply everyday danger without making the world a horror-survival game. Spawn behavior, damaging effects in protected areas and difficulty remain open. Ordinary PvE death and optional organized PvP need not have identical stakes; define the latter separately.

Logging out does not consume body calories. Property costs, lease clocks, stored assets and machine operation use their own explicitly defined rules; do not infer that all economic time stops because a player is offline. Full-world sleep/time-skipping also needs a multiplayer policy before introducing scheduled processes.

## 5. Resource and energy boundary

**Confirmed: globally disable bone meal use.** Do not merely remove its effect on tree growth; it must not provide alternate growth, vegetation-generation or fertilization actions. Exact item/recipe cleanup remains to be specified. This does not remove bones or the custom Mineral Fertilizer system: the latter is an independently designed input. The change is planned, not implemented.

### Construction materials and physical volume

**Confirmed:** one log produces **one plank block**, replacing the four-plank output. Construction-material conversion should respect physical block volume more closely; processing a tree must not multiply its full-block building volume. This increases the timber needed for structures and strengthens the role of timber production and freight. It is planned, not implemented.

Apply this principle as a recipe audit, not a blanket one-item-in/one-item-out rule. A slab, stair, panel or functional object is not necessarily a full cubic block. Other conversion ratios remain proposals until reviewed. Avoid adding sawdust, moisture or a detailed material simulation merely to justify a simple construction rule.

Before implementation, review log/wood/stripped variants, plank recipes and alternate processing paths so they cannot restore the fourfold output. Decide treatment of bamboo and Nether wood families within the broader biological-resource rules. Review downstream tool, chest, crafting-station and housing costs: changing planks affects early necessities as well as large builds. Preserve the approved one-log/one-plank conversion while tuning those recipes if necessary. Existing placed blocks and stock need an explicit compatibility policy; do not silently delete them.

Construction demand now connects more directly to harvested timber. Evaluate both the cost of a small starter home and bulk commercial building before claiming that this ratio alone establishes a profitable timber industry.

**Confirmed:** renewable food supports people; renewable industrial generation and biomass fuel are excluded. Higher-tier resources are rare, large and infrastructure-gated. Custom coal machinery obeys a finite-fuel rule; the rest of vanilla is not yet comprehensively governed.

**Agreed resource classification, not fully implemented:**

- **Renewable:** food, timber and other biological materials. Their yields and automation still need review. Renewable calories and biomass must not become industrial power.
- **Finite:** metals, industrial fuels and strategic mineral deposits. Remove renewable metal/fuel outputs from mob farms, trading and other alternate sources. Audit acquisition routes for every material before claiming its scarcity is enforced. Finite does not mean every deposit is rare or infrastructure-gated: ordinary Tier 1 metals remain distinct from higher-tier deposits.
- **Common building materials:** plentiful through extraction so ordinary construction remains accessible. Exact abundance, extraction mechanics and the treatment of generated stone and other nonmetal material loops remain open; no blanket ban on all renewable building blocks is implied.
- **Automation:** may gather, move or process resources, but cannot generate supposedly finite resources indefinitely. Define which productive work requires industrial energy. Trading existing finite stock is legitimate; replenishing it from an unlimited NPC source is not.

Conservation must include alternate item forms, salvage and recycling. Recovering metal already present in an item is different from creating a net renewable supply. Exact recovery recipes remain open. Deposit size, world expansion/reset policy and long-term supply are still unresolved; this classification does not solve finite-world exhaustion.

Audit recipe families and loot/trading rules for charcoal/wood/lava fuel, furnaces and campfires, villagers, mob drops, generated materials, crop/animal automation, enchanting/repair and renewable XP. This is a review list, not a decision to delete these systems. Current brick gating is an example of a specific replacement; do not extend it silently to unrelated materials.

### Agreed household cooking direction

Food roles are now agreed: river-grown staples, livestock production consuming farm feed, and fishing/wild gathering for subsistence and variety. Livestock feed is charged at breeding/production interactions, not through a continuous hunger simulation. Yields and full interaction coverage remain open; see [regional_geography.md](regional_geography.md).

Use one simple cooking station burning small amounts of coal. Basic farming and uncooked food remain sufficient for subsistence; cooking improves food value and preparation options. This is agreed design, not an implemented station. Reuse the existing Mineral Coal fuel rather than introduce a separate household fuel family.

Raw and cooked foods both feed the same calorie reserve. Exact yields, recipes, coal consumption, timing, fuel remainder handling and station construction are still open. Cooked food values are deliberate game values, not a claim that heat creates nutritional energy. Packaging alone still conserves food calories. Food and cooking outputs cannot be converted into industrial fuel.

Proposed role boundary: hand preparation handles simple uncooked provisions; the household station prepares food with finite fuel; industrial processors transform materials or provide later bulk capability. The cooking station should be constructible with Tier 1 materials without requiring its own output. Its final physical shape is unspecified; no new multiblock is implied.

Before implementation, classify existing furnace, smoker and campfire food recipes, crafting-table bread, fire-based cooked drops and automatic cooking paths as retained, replaced or disabled. There must be no unreviewed free-cooking bypass. Existing cooking methods are not disabled by this document. Validate that accessible uncooked foods sustain the full cultivation/replanting cycle without coal and without relying on emergency morsels as normal nutrition.

The next low-dependency planning topic is the renewable-versus-finite resource boundary. This can proceed while exact cooking recipes and station details remain open.

## 6. Next decisions and acceptance examples

Work in this order, keeping difficult decisions open rather than blocking the rest:

1. Review the simple baseline in section 2.
2. Choose everyday death/recovery and the passenger/cargo travel boundary together.
3. Specify household cooking and classify ordinary versus strategic resource loops.
4. Finish a keep/change/defer table for relevant vanilla mechanics, with dependencies and implementation tasks.
5. Carry these rules into the illustrative region and its travel/demand model (Pass B).

Before coding, the completed rules should explain these cases:

- A new player feeds themselves and builds shelter without purchasing access to a profession.
- A depleted player recovers without a death-based calorie refill.
- A builder dies carrying construction materials; recovery is understandable and does not deliver the materials to a distant home.
- A player logs out for a week; body energy, property and stored goods each follow stated rules.
- A carrier cannot bypass the intended route by combining storage, respawn and travel features.
- A renewable vanilla process cannot mint a supposedly finite industrial fuel or material.

The first draft is complete; the decisions and the full vanilla audit are not. No code is changed by this document.
