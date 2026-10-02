# Cutting and shaped construction

The geometry follows a 2×2×2 grid of eight half-edge-length cubes. Every cut bisects the next full-length axis; it never produces a thinner plate.

| Stage | Dimensions | Full-block volume | Orientations/positions |
|---|---|---|---|
| Slab | 1 × 1 × ½ | ½ | 6 faces |
| Quarter beam | 1 × ½ × ½ | ¼ | 12 edges |
| Eighth cube | ½ × ½ × ½ | ⅛ | 8 corners |

## Saw and recipes

Craft Stone, Iron or Diamond Saws from three cobblestone, iron ingots or diamonds and two sticks:

```text
MM.
.SM
S..
```

Combine a saw with one full block anywhere in a crafting grid to make **two slabs**. The 59 ordinary vanilla slab crafting recipes, 74 slab-producing stonecutter recipes and Street Pavers' own three-block slab recipe are disabled; a saw is the production route. Waxing a slab that was already cut remains possible. Saw one slab into **two quarter beams**, then one beam into **two eighth cubes**. Eighths are the minimum size. Each cut uses one durability: stone 131, iron 250, diamond 1,561. The final use produces pieces and breaks the saw. Vanilla material mining-tier requirements apply (obsidian requires a diamond saw).

## Recombining in the crafting grid

Put two pieces of the same material and size in any two slots: two eighth cubes make one quarter beam, two beams make one slab, and two slabs restore one original full block. This works in the player's 2×2 grid and the crafting table, without a saw or durability cost. Split a stack across two slots; shift-click the output to recombine in bulk. Each step preserves material volume. Matching legacy custom halves and vanilla slabs can be mixed.

To avoid conflicting outputs, vanilla's two-slab decorative recipes (chiseled blocks, bamboo mosaic and purpur pillar) now take one corresponding full block instead.

## Vanilla slab integration

Where a material already has a vanilla slab, cutting produces that existing item. Existing slab items remain usable, including slabs from old stock or loot; there is no parallel custom half-item requirement. All vanilla slab types are mapped to their source material and can be cut into beams. Horizontal placement uses the vanilla slab block; vertical placement uses a cut block which picks up and drops as the same vanilla slab item. Existing placed vanilla slabs also satisfy matching multiblock requirements. Materials with no slab use a generic material-carrying half item. Conventional modded slab names are mapped when a full source block can be resolved. Other mods' recipes that produce slabs are not automatically disabled.

The system cuts solid full building blocks. Containers/block entities, fluids, unbreakable blocks, falling blocks, TNT and other non-cube shapes remain excluded. Pieces preserve the source material's default textures, sound and tool requirements; they are not smaller functional machines.

## Placement and joining

- Slabs attach to the center of the clicked face. Aiming near a face edge selects vertical/side attachment; crouch to force the clicked face.
- Beams attach to the clicked face and its nearest edge. Floor placement gives horizontal beams; aim near a wall's side edge for an upright beam.
- Eighth cubes attach to the clicked face's aimed-at corner.
- A transparent textured preview shows the exact geometry and position. It shares multiblock opacity and outline settings. Red means an entity blocks placement.
- Every block space has eight independent cells. Slabs, beams and cubes can share it in any non-overlapping arrangement, including gaps, L-shapes and mixed materials. Aim at the surface beside the cell you want to fill. Placement tries the aimed orientation first, then another axis that fits at that cell; crouching locks the aimed orientation.
- Matching material automatically becomes a native slab or full block when the filled cells form that shape. Breaking an assembly returns its materials as recombinable slabs, beams and cubes, with no loss of volume. A mixed assembly requires a tool capable of harvesting all its materials.
- Existing generic custom halves automatically become their canonical vanilla slab item in player inventories. The generic backing item is no longer exposed separately in the creative tab.
- Slabs, beams and cubes support waterlogging. Pieces stack to 32; drops and pick-block preserve their material and size.
- Successful placement spends calories in proportion to the added piece's volume: 1 kcal for a half slab, 0.5 for a quarter beam and 0.25 for an eighth cube at the default 2 kcal full-block rate, before comfort discounts. Native slab items use the same cancellable placement/accounting path as custom pieces. Filling an existing cut assembly costs only the piece added; rejected or cancelled placements cost nothing.

Source resource-pack textures are cropped to preserve scale, including Faithful. Unticked material entities store up to eight cells and render through chunk meshes; there is no per-cell tick or entity. Geometry, collision and previews share the same cell grid. Horizontal vanilla slabs retain their normal block implementation.

Opaque cut faces now block Minecraft light, so a solid half wall can enclose a lit room. Open and glass faces still pass light. When pieces join or their material changes, the block updates its lighting state; older placed pieces update when their chunk loads. Lighting uses fully covered outer faces, while collision and rendering retain the exact eight-cell shape.

## Machines and old saves

The kiln and Fertilizer Furnace use upright quarter-beam corner supports, half-slab roof edges and eighth-cube roof corners. Their plinths and central crowns remain full blocks. Controllers, inventories, recipes and hopper ports are unchanged. Old shells need reshaping using the guides, which check material, geometry and orientation. Nothing is automatically deleted.

| Machine | Full blocks | Slabs | Beams | Eighth cubes |
|---|---|---|---|---|
| Kiln | 13 stone | 4 stone | 4 stone | 4 stone |
| Fertilizer Furnace | 13 bricks | 4 brick + 1 copper | 4 brick | 4 copper |

Stone means cobblestone or stone bricks. Accepted copper oxidation/wax variants remain valid. A downward hopper may replace the full front hatch. The chamber must remain empty.

Existing custom halves remain usable and return vanilla slab items where available. Old quarter plates become quarter beams, preserving quarter-block volume and material. The new eighth stage is additional. No quarter-thickness plates remain.

## Builder's Line

Craft **Builder's Line** at the Smithy from **2 iron ingots + 1 plank of any wood + 2 string**: one reel in **20 seconds** of productive work. Base productive coal cost is one Coal; temperature, ignition and idle burn follow ordinary Smithy rules. It is also in the Civilization Creative tab or `/give @s civilization:builders_line`.

- Put the reel in the **offhand**. Hold ordinary full building blocks or a **pickaxe, axe or shovel** in the main hand.
- **Right-click** a face to mark the start. For building, the mark is the cell normal placement would fill; for mining, it is the block hit. The first click performs no placement, mining or container action.
- Aim at the end to inspect translucent block ghosts (building), amber target outlines (mining) and a thin cord between endpoints. Red outlines mark invalid/blocked cells. **Right-click again** to immediately start the operation; releasing the button does not interrupt work. Both endpoint cells count. Holding the button cannot double-select or repeat an operation.
- Lines run along **X, Y or Z only**, with **16 cells maximum**, including a valid single-cell line. A diagonal/over-limit second mark is rejected while keeping the first point selected. Marks use ordinary interaction reach; you may walk between them. Active work checks a **16-block eye-to-cell-center range**, clear sight, loaded terrain, world bounds and current claim access. It cannot operate through walls or force-load chunks.
- **Crouch-right-click cancels**. Switching the selected slot, working item/components, offhand reel or dimension, dying, disconnecting or leaving reach clears the operation. Completed cells remain. A new line requires two new clicks.
- Building places **four blocks per second at 20 TPS**, consuming only the current main-hand stack. Normal placement events reject occupied/fluid cells and entity collisions and pay ordinary calories once. No automatic inventory refill or replacement of existing blocks.
- Mining uses the held tool's **normal effective mining speed**, including hardness, enchantments, equipment grade, bonuses and depletion, with a short ordinary post-break delay. Each harvest uses normal cancellable player break events, durability, enchantment drops, experience and calories. Drops remain in the world. Insufficient harvest tools, a broken tool or a changed target stop the line before another cell is affected.
- Work stops at the first obstruction, denied/canceled action, unloaded cell or inaccessible target; only successful preceding cells are paid. No whole-line rollback, automatic skipping or saved/offline jobs. There is no separate reel fuel or wear.

Initial coverage is inert full-cube vanilla blocks and full Street Pavers. Glass and ordinary ores are included when harvestable; shaped pieces, stairs, slabs, leaves/plants, falling blocks, TNT, fluids, containers/block entities, custom machine parts and moving vessels are excluded. Shaped-piece repetition remains a follow-up through the existing half-grid system. Unsupported held items retain their ordinary interactions.

The client preview is advisory; the server raycasts endpoint clicks and rechecks every target and actual stack. One bounded operation exists per player. Preview layout is cached outside rendering, with shared `GuidePerformance` cadence and visibility/outline bounds under the [guide performance contract](multiblock-builds.md#guide-performance-contract).

## Verification

Server tests cover vanilla slab mappings and actual placement, cutting yields through the eighth stage, durability, persistence, joining consumption, exactly 6/12/8 unique shapes, half-grid dimensions, rotations, waterlogging, and multiblock geometry/material checks. Hidden 1920×1080 visual checks cover the slab, beam and cube previews and assembled machines with Photon and Faithful.

Server coverage includes all 255 occupancy masks, non-rectangular placement order, four beams, mixed sizes/materials, native-slab additions, overlap/entity rejection, save/load and lossless drops, plus canceled placement rollback and event delivery for calorie accounting. Lighting tests cover opaque and glass cut faces and actual relighting of a sealed room when a glass half wall becomes brick. Hidden 1920×1080 Photon/Faithful visual fixture: mixed-material and non-rectangular assemblies.

Material packets may arrive after the block mesh was built. Loading client material/cell data now refreshes model data and invalidates the chunk mesh. The material-sync visual regression deliberately separates block placement from material delivery; it checks the received material identities and captures the rendered result. Server material data and item quantities are unchanged.
