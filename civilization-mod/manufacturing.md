# Shared parts and assembly

The first manufacturing package separates **Foundry materials → Smithy parts → hand assembly → installed goods**. Existing finished items keep their IDs and behavior. Oil is unnecessary for establishing this chain. Foods, chemical processing, ordinary block placement and the existing saw-cut construction system keep their own routes.

## Assembly Bench

Craft one Assembly Bench with three planks across the top row and a stick under each end (`PPP / S S`). Any plank species works. It is a **2-wide × 1-deep × 1-high** timber workbench, placed as one item into two adjacent block spaces. Its continuous plank top has pegged joinery, a lower shelf, restrained wear/markings, a wooden mallet, chisel and marking square. These small tools and shelf offcuts are decorative, not extractable inventory. It needs no surrounding multiblock, metal, leather or fuel. Either half opens the same shared cabinet interface. It combines woodworking, assembly, upgrades and repairs. Placement reserves the adjacent space across the bench width; both spaces must be loaded, clear, inside the world border and accessible to the builder. Placement spends one bench item and canceled placement restores both cells. Both halves have the actual tabletop/leg/shelf collision rather than full invisible cubes. Dismantling either half returns one bench item and the single inventory once; Creative does not mint a bench item. Pistons cannot move either half.

Choose a recipe, insert its materials in any of four input slots and press **Work**. Automatic recognizes only a unique complete match; ambiguous ingredients need a selection. The recipe drawer and JEI show actual counts. Ghost requirements are guidance, never stock. The bench has one output and no coal slots or ignition controls. Work is one batch per press, not an unattended queue.

Remain alive, within the ordinary eight-block interaction distance, with this bench menu open and current claim access to both halves. Work advances ten ticks every half second, charging **2.5 kcal per work step before the existing comfort factor** (5 kcal/second at neutral comfort). A five-second job costs 25 kcal and a ten-second job costs 50 kcal before comfort; material-scaled equipment takes proportionally more labor. Creative pays no calories; Spectator cannot work. Depleted players stop. Closing the menu, leaving, losing access or unloading pauses work; shutdown/reload never supplies elapsed work. Only one worker can operate a bench at a time. Inputs/output/work save, while the worker session does not. Press Work to resume a saved job. Changing the recipe or any input resets its progress. Spent labor is not refunded.

Inputs are consumed only when the complete output fits. Missing/changed materials or blocked output cannot spend stock. Hoppers may supply the four inputs from above the root/first half and collect its output below, but cannot provide labor. Hand work creates no coal heat or production XP. Large engines/vessels still assemble at their actual sites using their existing guides; the bench makes their component items, not portable completed structures.

## Shared stock

| Product | Input | Output | Base productive time | Workplace |
| --- | --- | --- | --- | --- |
| Complete Tool Handle | 1 plank + 1 Leather | 4 handles | 5 seconds | Assembly Bench |
| Wooden Wheel | 1 plank + 2 Iron Fasteners | 1 wheel | 10 seconds | Assembly Bench |
| Metal Plate | 1 corresponding ingot | 1 plate | 5 seconds | Smithy |
| Metal Rod | 1 corresponding ingot | 2 rods | 5 seconds | Smithy |
| Iron Fasteners | 1 Iron Ingot | 8 fasteners | 5 seconds | Smithy |
| Machine Parts | 2 Steel Plates + 4 Copper Rods | 2 assemblies | 10 seconds | Assembly Bench |

Plate/Rod metals are iron, copper, steel and gold. Gold forming takes twice the base duration. Fasteners include ordinary nails, rivets, bolts, hinges and latches. Complete handles include the wood, leather grip and binding: final tools never require extra leather for that grip. A Wheel includes hub, spokes and rim; its axle is supplied separately. Machine Parts retain their existing ID and abstract internal bearings/gears/fittings. Do not make individual versions of those details.

The Smithy remains the existing coal-fired multiblock with its current ignition, weather, thermal efficiency, idle burn and four-input/single-output rules. It now makes parts rather than complete equipment. At neutral base efficiency one Coal supplies 400 productive ticks (20 seconds); a basic Plate/Rod/Fasteners batch uses 0.25 Coal before ignition/idle/thermal effects.

## Equipment and repairs

Smithy head/blade forms are pickaxe, axe, shovel, hoe, sword and saw. Each takes a base **25 seconds**, consuming respectively **3, 3, 1, 2, 2, 3** primary material. Iron and diamond cover all six forms; gold covers the first five because no gold saw exists. Gold takes 2× and diamond 4× duration/fuel. A coal mining pick reuses the iron pick head; its loading shovel reuses the iron shovel head.

Final tools/swords/saws take **one matching head/blade + one complete Tool Handle**, in a base **5 seconds** at the bench. Shears take **2 Iron Plates + 1 Fasteners**, also five seconds. Metal armor takes its familiar primary quantity (helmet/chest/legs/boots: **5/8/7/4**) as iron/gold Plates or Diamonds, plus **1 Leather + 2 Cloth**, in ten seconds. Chainmail uses Iron Plates. A Shield takes **6 planks + 1 Iron Plate + 1 Leather**, in ten seconds. Cloth remains wool-only from the Textile Workshop.

Hand equipment duration scales iron/chainmail 1×, gold 2×, diamond 4×, netherite 8×. New finished equipment rolls grade once on completion; raw parts do not roll grade. Netherite upgrades are bench jobs using the existing diamond base, ingot, template and existing fitting/Cloth requirement; they retain grade, wear seed, name, enchantments and proportional damage. There is no disassembly route or free grade reset.

Choose **Repair** and supply one damaged catalog item. The quote charges `ceil(primary fresh amount × 0.5 × damage/maxDamage)`, minimum one, plus **one complete Tool Handle** for headed tools, **one Cloth** for armor, or **one Leather** for other covered gear. Iron/gold primary stock is Plates; other materials retain their existing primary input. The base hand duration is ten seconds, with the same material multiplier. Repairs preserve identity/enchantments, remove prior-work penalty, restore damage to zero, and lower grade using the existing exponential restored-fraction rule. A repair whose resulting grade is zero or below is unavailable. No Coal or XP is charged. The actual damaged stack is consumed even when another pristine copy appears earlier in the inputs.

## Fixtures and carriers

| Product | Assembly Bench bill | Output / base time |
| --- | --- | --- |
| Wooden doors | 6 matching species planks + 3 Fasteners | 3 / 5 seconds |
| Wooden trapdoors | 6 matching species planks + 2 Fasteners | 2 / 5 seconds |
| Wooden fence gate | 2 matching species planks + 4 Sticks + 2 Fasteners | 1 / 5 seconds |
| Chest / Barrel | 8 / 7 mixed planks respectively | 1 / 5 seconds |
| Wheelbarrow | 3 mixed planks + 1 Wooden Wheel + 1 Iron Rod + 4 Fasteners | 1 / 10 seconds |
| Ordinary Minecart | 4 Iron Plates + 2 Iron Rods + 1 Machine Parts | 1 / 10 seconds |
| Coal Minecart | 4 mixed planks + 2 Iron Rods + 1 Machine Parts + 4 Fasteners | 1 / 10 seconds |
| Coal Preparation Screen | 4 mixed planks + 2 Iron Plates + 2 Sticks | 1 / 10 seconds |
| Iron Bars | 12 Iron Rods | 16 / 5 seconds |
| Iron Door | 6 Iron Plates + 3 Fasteners | 3 / 5 seconds |
| Iron Trapdoor | 4 Iron Plates + 2 Fasteners | 1 / 5 seconds |
| Rail | 12 Iron Rods + 1 Stick | 16 / 5 seconds |
| Powered Rail | 12 Gold Rods + 1 Stick + 1 Redstone | 6 / 5 seconds |

Wood variants cover oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson and warped. The cheap existing crafting-table Chest/Barrel routes remain for early storage. Their bench recipes are additional routes. Primitive tools, the initial Kiln/Foundry/Tannery/Textile/Smithy controllers and anvil crafting remain available so construction never requires a workshop's own unavailable output. A stone saw still supplies initial structural cuts.

## Industrial and civic assembly

All jobs in this paragraph take **ten seconds by hand**. Existing industrial controller/part bills replace Iron/Copper/Steel Ingots with corresponding Plates; their structural shells and existing saved construction payments are unchanged. The [industry recipe table](industry.md#recipes-and-controls) lists the refinery family. Engine/bulk controller bills use Plates in place of their former ingots. Engine Cylinder uses **6 Iron Plates + 2 Machine Parts**; Engine Flywheel uses **8 Steel Rods + 1 Machine Parts**. Small Boat Helm uses **4 Steel Plates + 3 Machine Parts + 1 plank**. Builder's Line uses **2 Iron Plates + 1 plank + 2 String**. Prospecting Rod uses **1 Iron Rod + 2 Sticks**.

The bench also assembles the Geiger Counter, Land Controller, Trade Counter, Survey Table section and Gladiator Pit controller. Their former ingot inputs become Plates; other actual ingredients stay. Cooling Grilles and Fluid Pipes use Fasteners in place of their former iron nuggets. Industrial Guardrails use **6 Copper Rods + 10 Iron Rods → 16 rails**. Ordinary structural blocks/cuts remain actual building stock; decorative block shaping and non-manufactured recipes are not globally rewritten. The complete authoritative catalog is `ManufacturingJobs`/`AssemblyRecipes`, exposed in the bench and JEI.

## Existing saves and limits

No world regeneration, inventory sweep, item-ID replacement or existing machine-body conversion occurs. Existing tools, controllers, installed structures, output stacks, fuel and stored materials remain. Old Smithy recipe selection/work resets because it no longer produces complete goods; actual input/output stacks remain available to collect and reroute. Existing narrow one-block benches retain their old visual, inventory and operation; they do not overwrite neighboring terrain to expand. Pick up and replace a narrow bench to adopt the two-block version. New wide benches store one inventory in the root/first half; the extension has no second item bank. Current catalog selections save by output ID. Tannery/Textile production remains unchanged. No crafting/uncrafting conversion can turn parts back into a larger raw stock balance.

This package supplies wheelbarrow acquisition, not cargo capacity or a new vehicle controller. Component physical mass/volume remains explicitly unknown in the current stone-only inspector, consistent with other unaudited item families; no new capacity exemption is implemented. Later powered machine works, separate part durability and an economy/ordinary multiplayer playtest remain follow-up work. [Validation and deployment](../docs/status.md) records the actual local checkpoint.
