# Physical materials and cargo

Shared material data and block geometry, the operator inspectors, the wheelbarrow and pallets, personal block carrying and shared crafting tables. Sable owns vessel motion, collision and inertia; physical mass here is separate from thermal capacity and machine work credit.

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

- Right-click the tray, front or sides to open cargo; right-click the rear handles to take them, whatever you are holding. You stand behind it in a pushing pose. **W/S** push forward and back, **A/D** turn, **Sneak** releases. Opening a screen, losing access, dying or entering water releases the handles, and missing input stops drive after half a second. Only one player handles a carrier at a time, and cargo is closed while it is handled.
- Forward pushing matches walking at 3.2 blocks/s; reversing is 1.6 blocks/s, turning 70°/s. Handling costs the [walking rate](survival.md#calories) for actual handle travel plus ascent, logged as `push_wheelbarrow`; load does not change speed or cost. It climbs successive one-block rises (full blocks, slabs, stairs) without jumping when the carrier and handler both have headroom; taller ledges block it.
- Players collide only with the tray, wheel and parking legs; **the handles have no collision**, though a wider interaction box makes them easy to grab. The tray is a solid platform at rim height that players can stand on. Collision uses a 1/32-block horizontal grid rebuilt only when heading changes. The carrier is about 1.04 blocks wide, 2.62 long and 0.97 high at the parked rear rim.
- **Sneak + empty-hand right-click** recovers a parked empty wheelbarrow; loaded wheelbarrows cannot be picked up, and attacks do not break it. Unclaimed carriers are shared; claim permissions govern taking and recovery and are rechecked while moving.

The server owns movement: clients send only bounded direction and release input. Heading is synchronized as a float, so travel, rendering and the camera stay aligned; steering keeps any deliberate look-around offset. First person shows both hands on the grips and hides held items. Wheel rotation follows actual displacement. Terrain and access checks stay within the small loaded footprint. Identity and wheel travel are saved; pending input is cleared on reload. It is an ordinary entity, not a Sable vessel.

## Pallets and cargo

Craft a **Pallet** from three planks across the top row with three sticks beneath them (`PPP / SSS`); any plank species works. Place it from either hand and right-click its timber base to open cargo.

Cargo is limited **by mass only**: a wheelbarrow holds **640,000 kg** and each Pallet **1,024,000 kg**, set by `wheelbarrowMassKg` and `stockpileMassKg` in `serverconfig/civilization-cargo.toml`. There is no stack-count, volume or material-type cap. Matching items merge into long-count entries shown 27 per page, with exact quantities in tooltips. Accepted goods are ordinary solid full building blocks, mapped native slabs and valid saw-cut pieces, using their material density and actual volume; tools, food, containers, block entities, TNT and unsupported shapes are rejected. A wheelbarrow fits, for example, 320 stone blocks, 640 slabs or 2,560 eighth cubes.

**Pallets touching horizontal sides at the same height share one inventory and capacity**, in any side-connected shape; diagonal-only contact and vertical stacking stay separate. Open any member to use the combined stock; the mass bar's tooltip shows the pallet count. Claim access must permit every connected member on each transaction.

- Click an entry to take a normal stack onto the cursor (right-click for half); click with matching cursor items to deposit what fits (right-click deposits one). Shift-click moves the offhand block stack into cargo or fills the free/matching offhand from one entry. Cursor and inventories always keep native stack limits. Mass-bound partial transfers leave the remainder in the source.
- Park a wheelbarrow within **three blocks** of a pallet group and open either: **Load nearby** pulls from the other store and **Unload nearby** pushes to it. The nearest accessible partner is chosen when the menu opens; both ends must still be loaded, parked, in range and accessible on each click. Goods never pass through pockets.

Each pallet shows its share of the stock with at most 18 representative blocks (12 on a wheelbarrow); cargo visuals have no collision. **Empty the whole shared inventory before breaking any of its pallets.** Pallets resist explosions. Administrator removal can erase a pallet's local share.

Each wheelbarrow saves one balance and each pallet only its local share, in the native cargo format; counts are saved separately from item identities and may exceed 32-bit integers. The shared inventory is a cached view rebuilt from loaded neighbors on placement, removal and load/unload, never loading chunks; unavailable pallets contribute neither stock nor capacity until they return. Splitting a group or lowering a mass limit never truncates stock: overfull loads can be withdrawn but not added to. Caches invalidate on cargo or material changes and visuals update at most once per tick.

## Personal block carrying

Survival and Adventure players carry **one stack of at most 32 placeable blocks or cut pieces, in the offhand**, whatever the material's mass (an item's own smaller maximum still applies). This covers full blocks (logs, planks, leaves, glass, wool and so on), slabs, stairs, walls, fences, doors, cut pieces, chests, beds, furnaces and placeable machines. **Seeds, crop foods, small plants and planting supplies, torches, lanterns, candles, rails, ladders, chains, small redstone parts, signs, banners, heads, carpets, flower pots, buds and cake use ordinary slots and hand controls**, as do **Pallets**. Named planting items and bush/growing-plant families are excluded directly; the [inventory_placeables item tag](src/main/resources/data/civilization/tags/item/inventory_placeables.json) lists the remaining small items and can be extended by datapacks. Unreviewed block items count as building cargo. Creative and Spectator are exempt.

- Mined and dropped block pickups go straight into a free offhand or merge with an identical stack up to the allowance, even when other slots are full; anything else stays on the ground and no shield or tool is displaced. Shift-click extraction from chests and machines uses the same route.
- Click, drag, number keys and F cannot move building blocks into ordinary slots. A cursor stack is allowed while a menu is open; closing returns what fits to the offhand and drops the rest.
- In Survival and Adventure a **Hand** well left of the hotbar in cargo, chest, crafting and machine menus shows the offhand slot for direct loading. It moves outside open recipe panels, is reserved from JEI, and its whole frame counts as inside the menu. It adds no capacity and does not change slot indices; Creative keeps its native layout.
- Recipe-book crafting can use offhand block ingredients and returns block results to the offhand allowance.

Filled shulker boxes, bundles and other component-backed containers cannot be carried as hidden loads. On login and between server transactions, misplaced block stacks and filled portable containers move into a private saved recovery balance after the hand stack is filled; nothing is deleted. Use **`/civilization recoverblocks`** with a free or matching offhand to withdraw the next permitted stack. Recovery is keyed by player UUID and survives reload and death; a saved filled container unpacks one layer per command, and non-block contents return to ordinary inventory.

The slot classification runs in both the client and the server; clients older than the classification keep their old slot rules ([server connections](README.md#server-connections)).

## Shared crafting tables

Placed vanilla crafting tables keep their nine input slots in the world. Closing the table, walking away or disconnecting leaves the ingredients there, and anyone with claim access who opens the same table sees and uses the same grid; open viewers receive input and result changes together. Each table's storage is saved with the dimension. Leave logs on a table, close it to free your hand and return with more. The player's 2×2 grid is unchanged.

Breaking or replacing the table drops its inputs once and invalidates open menus. Outputs are previews; crafting consumes the shared ingredients through the normal recipe path. There is no ticking block entity or hopper inventory. This is server-side and uses native menus, so protocol-compatible clients need no update.

## Save compatibility

- The Pallet keeps the block/item ID `civilization:material_stockpile`; older individual stockpiles join groups without conversion.
- Older saves' surplus block stacks, filled containers and misplaced small supplies are held for `/civilization recoverblocks`; no stock is deleted.
