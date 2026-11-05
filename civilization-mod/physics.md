# Physical materials and cargo

Shared material data and block geometry, the operator inspectors, the wheelbarrow and pallets, personal block carrying, the backpack and shared crafting tables. Sable owns vessel motion, collision and inertia; physical mass here is separate from thermal capacity and machine work credit.

## Materials

`PhysicalMaterials` loads [defaults.json](src/main/resources/data/civilization/physical_materials/defaults.json). A datapack can replace `data/civilization/physical_materials/defaults.json` and `/reload`; the whole file is validated and swapped atomically, and invalid data keeps the previous registry. Required families are air, water, wood, masonry, glass, metal, door and partial. Each defines density (kg/m³), specific heat (J/kg/K) and conductivity (W/m/K), plus labeled legacy heat capacity and conductance used by the [heat field](heat.md#thermal-field). Values must be finite, from 1e-9 to 1e9. They are effective game materials, not certified specifications.

An exact block ID overrides a tag; among matching tags the lexicographically first wins; otherwise state-aware defaults choose a family. Exact metal mappings cover iron and copper blocks and Industrial Casing; other blocks use the documented fallback. All fluids currently use a water-like fallback. There is no resolution cache, so reloads and tag changes take effect immediately.

## Geometry sample

`PhysicalSample.at` reads a block and its cut-block contents. Full cubes, native slabs (including doubles) and mixed eighth-cells give occupied volume, material mass, thermal capacity and six face descriptions; component mass and capacity sum by eighth-volume, vacant space adds air capacity but no mass. Each face reports solid and open area and a through-block conductive estimate from four quarter-area columns of two half-meter resistances in series. Other shapes are sampled at cell centers and flagged approximate (thin shapes may read as empty), fluids are provisionally full-volume, and inventories, tank contents and displacement are never included. Samples are recomputed on request, never stored.

## Inspector

Operators look at a block within eight blocks, or name one, and run:

```text
/civilization physics
/civilization physics <x> <y> <z>
```

It reports material family, occupied m³, estimated kg, J/K capacity, current Sable mass in engine units, legacy heat values, six face contact/conductance estimates and the coordinate domain, for loaded blocks only. Contents are excluded, so an empty shell's estimate is not a loaded vessel's mass. `SablePhysicalBridge` resolves world versus vessel UUID and plot-local position, plus the transformed world center, without assuming only Civilization vessels exist; unassigned plots fail explicitly. Civilization reads Sable's mass (`PhysicsBlockPropertyHelper.getMass`, which uses solid occupancy and a block-state property) and never overrides it.

### Cargo inspector

```text
/civilization cargo
/civilization cargo container <x> <y> <z>
```

The first scans the issuing player's inventory (hotbar, armor and offhand, not the cursor); the second scans one loaded block's direct `Container` inventory (double-chest halves separately), refusing ungenerated loot containers and never loading chunks. Stone and cobblestone blocks, their native slabs and saw-cut halves, quarters and eighths are recognized; `CargoVolume` counts exact integer eighth-m³ units, and mass uses the shared material density. Reports show supported material and storage m³, estimated kg and supported/unknown stack counts; totals are partial when unknown stacks exist. Bulk stores, tanks, capability-only and nested inventories are not read.

## Wheelbarrow

Assemble a Wheelbarrow at the [Assembly Bench](manufacturing.md#fixtures-and-carriers) (also in the Civilization creative tab or `/give @s civilization:wheelbarrow`). Place it on top of a solid surface with clear space for the whole carrier; placement checks loaded terrain, the world border, collision and claim access before consuming the item.

- Right-click the tray, front or sides to load the goods in your hand or open cargo, [as on a Pallet](#pallets-and-cargo); right-click the rear handles to take them, whatever you are holding. You take them facing along the carrier. **W/S** push forward and back and **A/D** steer, pivoting the carrier at 70°/s; your view turns with the handles. The mouse only looks around: your head turns at most 75° from the handles, and your body stays square to them. **Sneak** releases. Opening a screen, losing access, dying or entering water releases the handles, and missing input stops drive after half a second. Only one player handles a carrier at a time, and cargo is closed while it is handled.
- Forward pushing matches walking at 3.2 blocks/s, or sprinting at 4.6 while you hold the sprint key with enough calories to sprint; reversing is 1.6 blocks/s. You walk behind it on your own ground with a walking gait, and View Bobbing bobs the view as it does on foot. Handling costs the [walking or sprinting rate](survival.md#calories) for actual handle travel plus ascent, logged as `push_wheelbarrow`; load does not change speed or cost.
- The carrier steps at most one block up or down per move (full blocks, slabs, stairs), so it climbs successive one-block rises without jumping; its wheel stops at the edge of any deeper drop. You follow a step behind, stepping at most a block yourself; the carrier waits while you lack headroom or footing, and you let go if it ends up more than four blocks above or below you. The carriage pivots on the wheel axle between the wheel's ground and its rear support, the parked legs or your hands, so it tips with the slope; steps glide over about three ticks.
- The body is one solid outline from the ground to the tray rim, tight around the strapped tray, the frame and the tyre: straight sides that taper past the axle to a point at the wheel. **The handles have no collision**, though a wider interaction box makes them easy to grab. Walking into the body slides you smoothly along its faces at any heading, and the rim-height top is a platform you can stand on. The same turned outline meets terrain, other carriers and the mouse, and is what the debug hitbox (F3+B) draws, so blocks and clicks beside the body never touch it. The carrier is about 1.16 blocks wide, 2.7 long with its handles and 1.06 high at the parked rim.
- **Sneak + empty-hand right-click** recovers a parked empty wheelbarrow; loaded wheelbarrows cannot be picked up, and attacks do not break it. Unclaimed carriers are shared; claim permissions govern taking and recovery and are rechecked while moving.

The server owns movement: clients send only drive, steering and release input. Heading is synchronized as a float, so travel and rendering stay aligned. First person shows both hands on the grips at vanilla's first-person arm size and hides held items; the arms belong to your body, so looking around never swings them. Wheel rotation follows actual displacement, and rolling plays the ground's step sound. Terrain and access checks stay within the small loaded footprint. Identity and wheel travel are saved; pending input is cleared on reload. It is an ordinary entity, not a Sable vessel.

## Pallets and cargo

Craft a **Pallet** from three planks across the top row with three sticks beneath them (`PPP / SSS`); any plank species works. Place it from either hand.

Stored and carried building goods pack **64 to a block of space**, so each stored block is drawn as a quarter-size block and a store's capacity is its interior at that density. Wheelbarrows and Pallets take everything the [hand carries](#personal-block-carrying): native slabs and saw-cut pieces count by their volume, and every other block, such as stairs, doors, chests or a Cooking Stove, takes one block of space and is drawn as its block in one cell. Goods the hand does not carry, filled containers and items holding saved block data are refused, so nothing travels hidden inside a store. Matching items merge into long-count entries shown 27 per page, with exact quantities in tooltips.

- A **wheelbarrow** holds **64 blocks of space**, one block, whatever their mass, for example 64 stone or planks, 128 slabs or 512 eighth cubes: eight hands, half a Pallet. Its tray draws the load one for one: 16 quarter-size blocks to a layer in a 4×4 grid covering the tray floor, filled row by row from the wheel end in the order the goods arrived, with pieces filling eighth cells. The first two layers lie below the rim, the third rises past it and a full load heaps a fourth above it. The load has no collision.
- A **Pallet** holds **128 blocks of space**, in a stack up to two blocks high, whatever their mass; a slab counts as half a block and saw-cut pieces by their volume. The stack is drawn one for one: each stored block is a quarter-size block, 16 to a layer in a 4×4 grid and eight layers above the timber, filled row by row from the bottom in the order the goods arrived, with pieces filling eighth cells. Four layers fill the Pallet's own block; a full Pallet stands two blocks high, two pixels short of the second block's top. The stack is solid: you can stand on it and click any part of it, and its collision grows by layer.
- **The stack rises into the block above only while that block is empty**, with nobody standing in it, so a Pallet with anything above it holds 64. Once it rises, it holds that block: nothing can be placed into the stack, clicking its top acts on the Pallet, and the top refuses breaking while there are goods. It gives the block back when the stack is down to 64 again.

**Right-click a Pallet or a wheelbarrow's tray** to stack the goods in your hand onto it, main hand first, then offhand; the action bar shows what was stacked and the new total. When neither hand holds goods that fit, right-click opens the menu. Sneak to place blocks against a Pallet as usual.

**Pallets touching horizontal sides at the same height share one menu**, in any side-connected shape, summing each pallet's capacity (128, or 64 under a block); diagonal-only contact and vertical stacking stay separate. Each Pallet still holds its own stack. Goods go first onto Pallets already holding the same item, then onto empty Pallets, then on top of other goods, each filled to a full stack before the next, so a yard fills one pallet at a time and sorts itself by item. Taking goods empties the last Pallet holding them first. The capacity bar's tooltip shows the pallet count. Claim access must permit every connected member on each transaction.

- Click an entry to take a normal stack onto the cursor (right-click for half); click with matching cursor items to deposit what fits (right-click deposits one). Shift-click moves the offhand block stack into cargo or fills the free/matching offhand from one entry. Cursor and inventories always keep native stack limits. Capacity-bound partial transfers leave the remainder in the source.
- Park a wheelbarrow within **six blocks** of a pallet group and open either: **Load nearby** pulls from the other store and **Unload nearby** pushes to it. The nearest accessible partner is chosen when the menu opens; both ends must still be loaded, parked, in range and accessible on each click. Goods never pass through pockets.

**A Pallet with goods on it cannot be broken**; an empty one can, even in the middle of a group. Pallets resist explosions. Administrator removal can erase a pallet's stack.

Each wheelbarrow saves one balance and each pallet only its own stack, in the native cargo format; counts are saved separately from item identities and may exceed 32-bit integers. Clients receive each wheelbarrow's and pallet's stack to draw it, and pallets also take their shape from it. Each stored block is drawn as it would stand among the stack's other whole blocks in the world, so the resource pack's connected, repeating and random textures (Continuity with Patrix) tile across the stack as they do on placed blocks. A load's geometry is built once per change, leaving out faces hidden between whole cells of solid blocks: a pallet's stack is drawn with the terrain, and only goods drawn by their own block entity or as an item, such as chests and the Baking Oven, are drawn every frame. The shared menu is a cached view rebuilt from loaded neighbors on placement, removal and load/unload, never loading chunks; unavailable pallets contribute neither stock nor capacity until they return. A smaller limit never truncates stock: a store over its capacity, such as a wheelbarrow loaded under an older, larger limit, draws full and can be withdrawn from but not added to, and a pallet group takes goods from overloaded pallets first. Caches invalidate on cargo or material changes and visuals update at most once per tick.

## Personal block carrying

Survival and Adventure players carry **one stack of at most 8 placeable blocks or cut pieces, in the offhand**: an armful, an eighth of a block of space at cargo density, whatever the material's mass (an item's own smaller maximum still applies). This covers full blocks (logs, planks, leaves, glass, wool and so on), slabs, stairs, walls, fences, doors, cut pieces, chests, beds, furnaces and placeable machines. **Seeds, crop foods, small plants and planting supplies, torches, lanterns, candles, rails, ladders, chains, small redstone parts, signs, banners, heads, carpets, flower pots, buds, cake and the powder snow bucket use ordinary slots and hand controls**, as do **Pallets**. Named planting items and bush/growing-plant families are excluded directly; the [inventory_placeables item tag](src/main/resources/data/civilization/tags/item/inventory_placeables.json) lists the remaining small items and can be extended by datapacks. Unreviewed block items count as building cargo. Creative and Spectator are exempt.

- Mined and dropped block pickups go straight into a free offhand or merge with an identical stack up to the allowance, even when other slots are full; anything else stays on the ground and no shield or tool is displaced. Shift-click extraction from chests and machines uses the same route.
- **Drop** (Q) throws from the hand's blocks before the main hand: one block, or the whole stack with Ctrl. Once the hand holds no blocks, it drops the main hand's item as usual.
- A held right-click keeps to what the main hand started, such as eating: when the main hand stops, the hand's blocks wait for a fresh click instead of being placed.
- Click, drag, number keys and F cannot move building blocks into ordinary slots. A cursor stack is allowed while a menu is open; closing returns what fits to the offhand and drops the rest.
- In Survival and Adventure a **Hand** well left of the hotbar in cargo, chest, crafting and machine menus shows the offhand slot for direct loading. It moves outside open recipe panels, is reserved from JEI, and its whole frame counts as inside the menu; a worn [backpack](#backpack)'s Pack panel stands above it the same way. It adds no capacity and does not change slot indices; Creative keeps its native layout.
- Recipe-book crafting can use offhand block ingredients and returns block results to the offhand allowance.

Filled shulker boxes, bundles and other component-backed containers cannot be carried as hidden loads. On login and between server transactions, misplaced block stacks and filled portable containers move into a private saved recovery balance after the hand stack is filled; nothing is deleted. Use **`/civilization recoverblocks`** with a free or matching offhand to withdraw the next permitted stack. Recovery is keyed by player UUID and survives reload and death; a saved filled container unpacks one layer per command, and non-block contents return to ordinary inventory.

The slot classification runs in both the client and the server.

## Backpack

Assemble a **Backpack** at the [Assembly Bench](manufacturing.md#fixtures-and-carriers) and wear it in any of the three accessory slots beside the player portrait, one at a time. It rolls a [grade](manufacturing.md#grade) when made and has no durability; the grade sets its size: **9 slots and 24 building blocks at 100%**, each rounded (7 slots and 18 blocks at the 75% average, 11 and 30 at 125%). The slots hold ordinary goods at their native stack sizes and building blocks, which count against the block cap the way the hand counts them, each block or cut piece as one. A backpack holding anything cannot be taken off, and it never holds another backpack or a filled container.

- **Filling.** Picked-up and shift-click-extracted building blocks fill the hand to 8, then the pack; other goods fill the pockets, hotbar first, then the pack, and never the Hand.
- **Beside other menus.** Every chest, Pallet, wheelbarrow, crafting and machine menu shows the pack's slots in a **Pack** panel above the Hand, so goods move between that store and the pack by click, drag or shift-click.
- **Building.** After every placement, by hand, saw cut, controller feed or Builder's Line, the hand is topped back up to 8 with the block it held, from the bound store while it supplies and otherwise from the pack. The pack's own block stacks are then topped up from the store in slot order, up to the cap. The pack never takes on a block it does not already hold, so stock what you want to carry first (shift-click a store's entry fills the hand, then the pack).
- **Binding.** Open a wheelbarrow or Pallet while wearing a backpack and press **Bind backpack**: the pack binds to that store and its supply turns on; the same button reads **Unbind backpack** once bound there. A Pallet binding covers its connected pallets; binding elsewhere replaces it.
- **Supply** works while the player stands within **16 blocks** of the bound store in the same dimension, with the store loaded, a wheelbarrow parked rather than pushed, and claim access, checked at every pull. Out of reach, the hand refills from the pack alone. When the hand runs out, the action bar says why: supply off, out of reach, not nearby, being pushed, no access or nothing left of that block.
- **Controls.** **B** (Controls → Civilization) opens the pack: its slots, the binding and distance, the **Supply on/off** switch, **Unbind** and **Unload**, which puts the pack's building blocks onto the bound store. Shift-click moves pack blocks to the Hand and the Hand's blocks into the pack.
- **Badge.** Beside the Hand slot left of the hotbar, the pack's icon shows its blocks against its cap and, when bound, the store and its distance: green while building draws from it, gray otherwise. The badge is the client's own estimate; the server decides.

Other players see a worn backpack on the wearer's back; only the worn items are sent to them, never the pack's contents. On death the pack drops empty with its contents beside it, unless `keepInventory` is on.

The contents are the backpack item's own container contents, and its binding (a Pallet's position or a wheelbarrow's identity, with the dimension) and supply switch are saved on the item too, so all of it survives logout and restarts. A store that is gone leaves the binding in place until it is unbound or replaced. Supply checks run only after a placement and touch only the bound store: a Pallet by its position in a loaded chunk, a wheelbarrow by its identity among loaded entities; nothing is scanned or force-loaded.

## Shared crafting tables

Placed vanilla crafting tables keep their nine input slots in the world. Closing the table, walking away or disconnecting leaves the ingredients there, and anyone with claim access who opens the same table sees and uses the same grid; open viewers receive input and result changes together. Each table's storage is saved with the dimension. Leave logs on a table, close it to free your hand and return with more. The player's 2×2 grid is unchanged.

Breaking or replacing the table drops its inputs once and invalidates open menus. Outputs are previews; crafting consumes the shared ingredients through the normal recipe path. There is no ticking block entity or hopper inventory. This is server-side and uses native menus, so protocol-compatible clients need no update.

## Save compatibility

- The Pallet keeps the block/item ID `civilization:material_stockpile`; older individual stockpiles join groups without conversion.
- Pallets saved under the old mass limit keep every item. One holding more than its capacity draws as a full stack, two high where the block above is empty (which it then takes) and one high under a block, reads **Overloaded** in its menu and accepts nothing until it is back under. A group takes goods from its overloaded pallets first. Old `stockpileMassKg` settings are ignored.
- Existing pallets never displace what is above them: one with a block above keeps its 64-block stack, and one with free space above can now hold 128.
- Wheelbarrows loaded under the old 324-block tray or mass limit keep every item; they draw full, read **Overloaded** and accept nothing until back under 64.
- Older saves' surplus block stacks, filled containers and misplaced small supplies are held for `/civilization recoverblocks`; no stock is deleted. A hand of more than 8 from before the smaller allowance keeps 8, and the rest is held the same way.
