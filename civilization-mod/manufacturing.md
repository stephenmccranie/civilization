# Manufacturing

Town manufacturing runs **Foundry metals + hides and wool → Tannery, Textile Workshop and Smithy parts → Assembly Bench goods and repairs**. It needs Coal and labor, not oil. Forming metal is coal-fired; final assembly, upgrades and repairs are player labor at the Assembly Bench. Guides, the cabinet interface, coal fires and XP are shared [machine rules](machines.md).

## Foundry

Craft the **Foundry** controller from **8 brick blocks around cobblestone**. Placed, it is the japanned furnace door of a brick furnace three blocks wide and two deep behind the controller: a low barrel-vaulted body with piers at its front corners, a chimney rising at the back to about two and a half blocks, and a sand casting trough with two ingot moulds along its right side. It is built in five sections paid with **3 stone and 20 brick blocks**: footing (3 stone or cobblestone), walls and piers (8 brick blocks), vault and arches (6), chimney (4) and casting trough (2). Neither the Kiln nor the Foundry needs metal to build. The fire warms the whole masonry body, and its exhaust leaves through the chimney. It has two ingredient slots, two Coal slots and two outputs, and runs one recipe at a time.

| Input | Output | Time |
| --- | --- | --- |
| Raw iron, copper or gold, or their ores | 1 ingot | 10 seconds |
| 1 iron ingot | 1 Steel Ingot | 40 seconds |
| Silk-touched coal, diamond, emerald, lapis, redstone and quartz ores | Their usual drop | 10 seconds |
| Ancient debris | Netherite scrap | 10 seconds |
| Iron, copper, gold or steel ingot, or a diamond | The same items, hot for [hammering](#grade), up to 8 at once | 20 seconds |

Ore smelting is 1:1, without multiplication. Coal is both heat and steel's carbon source; there is no coke. On Automatic, iron ingots make steel; select **Heat Iron Ingot** to heat them. The other ingots and diamonds have no other Foundry job, so Automatic heats them. Heating awards no XP. Vanilla metal smelting and equipment-to-nugget recycling are disabled.

## Town workshops

| Controller | Crafting pattern (top / middle / bottom) |
| --- | --- |
| Tannery | plank–brick block–plank / plank–barrel–plank / three cobblestone |
| Textile Workshop | three brick blocks / plank–copper ingot–plank / three iron ingots |
| Smithy | three brick blocks / plank–cobblestone–plank / three iron ingots |

Controller crafting needs no leather, cloth, shears, anvil or oil; the assembled Smithy includes an anvil. Shells use vanilla or resource-pack materials and saw-cut pieces:

- **Tannery** (3×3, four high): **2 brick blocks, 6 cobblestone or stone-brick blocks, 8 plank slabs, 5 plank quarter beams, 2 plank eighth cubes**. A stone plinth with two front brick accents carries a half-height oak rim around an open vat; the eighth cubes share the rear rim cells and join the rim to two narrow posts, and a three-piece crossbar completes the drying rack. No water or loose hide is placed.
- **Textile Workshop** (3×3, four high): **5 plank slabs, 9 plank quarter beams, 4 plank eighth cubes, 1 copper quarter beam**. A two-deep upper-slab bench stands on four shared-cell feet; two rear posts and a crossbar frame an open center, with the copper roller in the supported central cell.
- **Smithy** (6×3, 4.5 high): **9 brick blocks, 14 brick slabs, 8 brick quarter beams, 4 brick eighth cubes, 2 plank slabs, 2 plank quarter beams, 4 plank eighth cubes, 1 cobblestone or stone-brick footing and 1 anvil**. The controller sits at the front center of the forge base; a 2-wide, 1.5-deep, one-high workbench on four eighth-cube feet and a floor-level anvil (its footing one block below ground) stand to its local right. Any anvil damage state and orientation work. The open hearth is two blocks wide and deep; a recessed slab back wall, quarter-beam piers, eighth-cube shoulders and paired chimney slabs narrow the silhouette toward the top. The hearth's coal bed, fire and chimney smoke are visual only — no inventory, free fuel or real fire.

| Workshop | Job | Productive time |
| --- | --- | --- |
| Tannery | 1 Raw Hide → 1 Leather | 10 seconds at half heat rate (0.25 Coal) |
| Textile Workshop | 1 wool of any color → 2 Cloth | 10 seconds at half heat rate (0.25 Coal) |
| Smithy | Ingots → Plates, Rods, Fasteners, heads and blades | [Shared stock](#shared-stock), [equipment](#equipment) |

Choose a job in the recipe drawer or let Automatic start a unique match; ambiguous materials wait for a selection while a lit fire keeps the Smithy hot. A Smithy with an occupied output pauses until the item is collected or removed by a hopper. Missing inputs or a changed selection reset unfinished work; blocked output or a broken structure pauses it. Inputs are consumed only when the output fits. Selections are saved by output ID. Hoppers and menus check range and claims.

Ordinary leather-producing animal drops and piglin leather barter yield **Raw Hide**; four rabbit hides craft one Raw Hide. Existing Leather and treasure leather remain usable. Cloth comes only from wool at the Textile Workshop.

## Assembly Bench

Craft one with three planks across the top row and a stick under each end (`PPP / S S`); any plank species works. It is a **2-wide × 1-deep × 1-high** timber workbench, placed as one item into two adjacent spaces that must be loaded, clear, inside the world border and accessible; canceled placement restores both cells. Holding it previews both halves where a right-click would put them, in red when the second space is blocked. Both halves have real tabletop, leg and shelf collision, open the same screen and inventory, and resist pistons. Dismantling either half returns one bench and the inventory once; Creative mints no bench item. It needs no multiblock, metal, leather or fuel.

Choose a recipe, insert its materials in any of the four input slots and press **Work**. Automatic recognizes only a unique complete match. The drawer and JEI show actual counts; ghosts are guidance, never stock. Each press runs one batch; there is no unattended queue.

Stay alive, within eight blocks, with the bench menu open and claim access to both halves. Work advances ten ticks every half second and charges **2.5 kcal per step before the comfort factor** (5 kcal/second at neutral comfort): a five-second job costs 25 kcal and a ten-second job 50 kcal. Creative pays nothing; Spectators cannot work; depleted players stop. Closing the menu, leaving, losing access or unloading pauses work, and shutdown never supplies elapsed work. One worker at a time. Inputs, output and progress save, but the worker session does not: press Work again to resume. Changing the recipe or any input resets progress, and spent labor is not refunded.

Inputs are consumed only when the whole output fits. Hoppers may supply inputs from above the first half and collect output below, but cannot provide labor. The bench makes no heat or XP. Large engines and vessels are built at their sites with guides; the bench makes their component items.

## Shared stock

| Product | Input | Output | Base time | Workplace |
| --- | --- | --- | --- | --- |
| Tool Handle | 1 plank + 1 Leather | 4 | 5 seconds | Assembly Bench |
| Wooden Wheel | 1 plank + 2 Iron Fasteners | 1 | 10 seconds | Assembly Bench |
| Metal Plate | 1 matching ingot | 1 | 5 seconds | Smithy |
| Metal Rod | 1 matching ingot | 2 | 5 seconds | Smithy |
| Iron Fasteners | 1 iron ingot | 8 | 5 seconds | Smithy |
| Machine Parts | 2 Steel Plates + 4 Copper Rods | 2 | 10 seconds | Assembly Bench |

Plates and Rods come in iron, copper, steel and gold; gold takes twice as long. Fasteners stand for nails, rivets, bolts, hinges and latches. A Tool Handle includes the wood, leather grip and binding, so tools never need extra leather. A Wheel includes hub, spokes and rim. Machine Parts abstract bearings, gears and fittings. A Plate, Rod or Fasteners batch draws 0.25 Coal from a hot Smithy; warming up and heat loss come on top.

## Equipment

The Smithy forges heads and blades for the pickaxe, axe, shovel, hoe, sword and saw, each in a base **25 seconds**, consuming **3, 3, 1, 2, 2 and 3** primary material respectively. Iron and diamond cover all six; gold covers the first five (there is no gold saw). Gold takes 2× and diamond 4× the time and coal.

At the Assembly Bench:

- **Tools, swords and saws:** one matching head or blade + one Tool Handle, 5 seconds.
- **Shears:** 2 Iron Plates + 1 Iron Fasteners, 5 seconds.
- **Metal armor:** helmet/chestplate/leggings/boots take **5/8/7/4** Iron or Gold Plates or Diamonds, plus 1 Leather + 2 Cloth, 10 seconds. Chainmail uses Iron Plates.
- **Shield:** 6 planks + 1 Iron Plate + 1 Leather, 10 seconds.
- **Netherite upgrades:** the diamond item + 1 netherite ingot + the upgrade template + 1 Leather (tools) or 2 Cloth (armor). Upgrades keep grade, wear seed, name, enchantments and proportional damage.

Hand durations scale iron/chainmail 1×, gold 2×, diamond 4×, netherite 8×. The Coal Mining Pick and Loading Shovel reuse the iron pickaxe and shovel heads ([coal mining](coal-mining.md)).

The catalog covers iron, gold and diamond pickaxes, axes, shovels, hoes, swords and armor; iron and diamond saws; shears; shields; chainmail armor; and all nine netherite upgrades. Gold tools have 600 base durability, mining speed 7, iron-level harvesting and melee damage halfway between iron and diamond; gold armor has 264/384/360/312 base durability (helmet/chestplate/leggings/boots), 17.5 protection for a full set and 1 toughness per piece, with vanilla enchantability. Wooden and stone equipment stays craftable. Bows, crossbows, flint and steel, horse armor, tridents, maces and other special equipment are outside the catalog.

### Grade

Grade belongs to each unit of metal. Every ingot the Foundry smelts from ore or raw metal, and every diamond from silk-touched ore, rolls a persistent grade, kept to a tenth of a percent (normal distribution, mean 75%, standard deviation 20 points, no floor or ceiling). Steel keeps the grade of the iron ingot it was made from. **Refining** is heat and hammer:

- **Heat.** A heat at the Foundry takes a whole load of up to eight units from its first input slot, as many as the output has room for, and draws one Coal in 20 seconds whatever the load, so full loads are cheapest. The load leaves hot: it glows in its slot, its tooltip counts down the seconds left, and it cools a minute later wherever it is. Hot metal stays apart from cold metal, and hoppers cannot take it from the Foundry.
- **Hammer.** Hold use on any anvil with hot metal in your main hand to strike a heavy blow once a second; the metal's cooldown sweep shows the swing recovering. Each blow works the whole stack, and the fifth, about four seconds of hammering, refines every unit and leaves the stack cold. Hammering costs `hammeringKcal` (40 kcal at neutral comfort) for every unit, paid a fifth with each blow. Paid blows count on across pauses and toward whatever hot metal you strike next; logging out resets them. Metal that cools unhammered goes back into the fire, losing only the heat.

Every hammering raises each unit by at least a tenth of a point, by a random amount that shrinks steeply as the unit nears 150%: exponentially distributed, with a mean of 4.5 points at 75% that falls with the fourth power of the distance left to 150%, and never past it. From 75% one hammering in five gains a point or less and one in nine gains 10 or more. The mean gain is 1.8 points at 90%, 0.9 at 100%, 0.4 at 110% and 0.1 at 125%, where nearly every hammering adds a tenth. Reaching 100% takes about 12 heats and hammerings from 75% (7 to 17 in most cases) and 14 from 50%; 100% to 125% takes about 100 more, and 125% to 150% about 250. With full loads, an ingot reaches 100% from 75% for about 1.5 Coal and 490 kcal and 120% for about 9 Coal and 2,900 kcal, on top of [warming up](machines.md#coal-fires) the Foundry. Metal and diamonds from anywhere else, such as mined diamonds, loot or nuggets, are standard 75%.

Smithy parts (Plates, Rods, Fasteners, heads and blades) take the mean grade of the units they consume, rounded, and every part of a batch shares it. At the Assembly Bench, tools, swords and saws take their head's grade; armor, shears and shields take the mean of their Plates or diamonds. A [Backpack](physics.md#backpack) rolls its grade when made, as an ingot does. Other equipment made at a crafting table or without graded material, such as leather armor and wooden or stone tools, and found equipment such as loot are standard 75%. Netherite upgrades keep the diamond item's grade. Other products, such as Machine Parts, rails and doors, carry none.

**Stacks.** Units of one material stack whatever their grades. A stack keeps every unit's grade, best first, and its tooltip shows the average and range, such as "Grade: 83.4% average, 52.1–118.6%". Units leave from the top: taking half, placing one, dragging across slots, dropping and machine work hand over or use the best units first, so spreading a stack out gives each unit back its own grade. Clicking, shift-clicking, double-click collecting, pickups, machine outputs and hoppers filling chests, barrels, shulker boxes, hoppers and machines join differing grades onto one stack. Paths that cannot say which units they move, such as droppers, the recipe book's auto-fill and other mods' inventories, keep a stack's worst units, so they can lower grade but never raise it. Trade counters match goods by their exact grades, and ingots crafted into nuggets or storage blocks and back come out standard.

At 100% an item has its base stats; a Backpack's grade scales its slots and block cap instead, and it has no durability. The grade multiplies maximum durability, mining speed, armor, toughness and knockback resistance (including vanilla armor's default attributes), melee damage and attack speed, and the pre-mitigation damage of projectiles from graded bows, crossbows and thrown tridents. An iron pickaxe's mining speed 6 becomes 3 at 50% and 7.5 at 125%. Enchantments are separate. Minecraft requires durability of at least one and positive mining speed, so nonpositive grades keep their true value in data while those two engine values use the smallest legal amount. Special behaviors and discrete harvest levels are not scaled.

The tooltip prints the grade. The 13-pixel durability bar shows remaining durability in green, durability spent since repair in black and capacity unavailable at the item's grade in gray. Above 100%, one or two cyan pixels at the right end show bonus capacity (about the first 10% of exceptional grade); it drains first. Each item has a saved wear seed choosing up to eight of 16 generated scuff and corrosion layers for its type as grade falls, so equal items can wear differently; repairs and upgrades keep the pattern. Layers follow the Faithful 32× item silhouette and leave the base art visible; at 100% or above the icon is pristine, and worn armor on the player model is unchanged. At 100% or above, trimmable armor (leather included) gets one seed-chosen vanilla trim pattern and material; below 100% it has none, so a repair that drops grade below 100% removes the trim.

### Repairs

Choose **Repair** at the Assembly Bench and supply one damaged catalog item. The quote charges `ceil(0.5 × fresh primary amount × damage / current maximum)` primary material, minimum one (Plates for iron and gold, otherwise the original material, netherite ingots for netherite), plus one Tool Handle for headed tools, one Cloth for armor or one Leather for other gear, in a base 10 seconds with the material multiplier. The item returns fully repaired with its identity and enchantments, prior-work cost cleared, and grade reduced by `max(1, ceil(2 × damage / current maximum × exp((100 − grade) / 25)))` points; weak equipment loses grade faster. A repair that would leave grade at zero or below is unavailable. No Coal or XP is charged. The actual damaged stack is consumed even if a pristine copy sits earlier in the inputs.

Example: a 100% iron pickaxe with 250 maximum and 220 damage costs 2 Iron Plates + 1 Tool Handle and returns at **98% grade, 245 maximum**. The one-unit minimum means some cheap repairs cost as much primary material as a new tool.

### Acquisition limits

Vanilla crafting for catalog items, netherite upgrade recipes and all 18 Smithing Table trim recipes are disabled. Anvils cannot material-repair or combine catalog equipment; crafting-grid combining and two-item grindstone repairs are disabled for it. Naming, enchanted-book application and single-item disenchanting remain, with ordinary XP rules, and restore no durability. Mending has no effect and no supported items; existing copies are labeled disabled. Villager offers for damageable equipment or Mending books are filtered, including saved ones. Naturally spawned catalog gear does not drop from mobs; gear players supplied or mobs picked up remains recoverable. Exploration loot is kept.

## Fixtures and carriers

| Product | Assembly Bench bill | Output / base time |
| --- | --- | --- |
| Wooden doors | 6 matching species planks + 3 Fasteners | 3 / 5 seconds |
| Wooden trapdoors | 6 matching species planks + 2 Fasteners | 2 / 5 seconds |
| Wooden fence gate | 2 matching species planks + 4 Sticks + 2 Fasteners | 1 / 5 seconds |
| Chest / Barrel | 8 / 7 mixed planks | 1 / 5 seconds |
| Wheelbarrow | 3 mixed planks + 1 Wooden Wheel + 1 Iron Rod + 4 Fasteners | 1 / 10 seconds |
| [Backpack](physics.md#backpack) | 8 Leather + 4 String + 4 Sticks | 1 / 10 seconds |
| Minecart | 4 Iron Plates + 2 Iron Rods + 1 Machine Parts | 1 / 10 seconds |
| Coal Minecart | 4 mixed planks + 2 Iron Rods + 1 Machine Parts + 4 Fasteners | 1 / 10 seconds |
| Coal Preparation Screen | 4 mixed planks + 2 Iron Plates + 2 Sticks | 1 / 10 seconds |
| Iron Bars | 12 Iron Rods | 16 / 5 seconds |
| Iron Door | 6 Iron Plates + 3 Fasteners | 3 / 5 seconds |
| Iron Trapdoor | 4 Iron Plates + 2 Fasteners | 1 / 5 seconds |
| Rail | 12 Iron Rods + 1 Stick | 16 / 5 seconds |
| Powered Rail | 12 Gold Rods + 1 Stick + 1 Redstone | 6 / 5 seconds |

Wood species are oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson and warped. Crafting-table chests and barrels stay available for early storage. Primitive tools, the Kiln, Foundry, Tannery, Textile Workshop and Smithy controllers and the anvil stay craftable, so no workshop needs its own output to be built.

## Other bench jobs

Every other Assembly Bench job takes **ten seconds** and is listed with its feature: the refinery family, Hot-Bulb Engine parts and bulk stores in [industry](industry.md#recipes), the Small Boat Helm in [vehicles](vehicle-physics.md#boats), the Builder's Line in [cutting](cutting.md#builders-line), the Geiger Counter in [geography](geography.md#frontier-uranium), the Prospecting Rod in [industry](industry.md#prospecting), civic controllers in [ownership and trade](ownership-trade.md), and the Gladiator Pit controller in its [reference](gladiator-pit.md#build-and-materials). `ManufacturingJobs` and `AssemblyRecipes` are the authoritative catalog.

## Save compatibility

Narrow one-block Assembly Benches from older saves keep their shape, inventory and operation and never expand into neighboring terrain; pick one up and place it again to get the two-block bench.
