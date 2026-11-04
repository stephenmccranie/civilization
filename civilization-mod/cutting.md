# Cutting and shaped construction

Every block space is a 2×2×2 grid of eight half-edge cubes. Each cut halves the next full-length axis; nothing thinner than half a block exists.

| Stage | Dimensions | Full-block volume | Orientations/positions |
|---|---|---|---|
| Slab | 1 × 1 × ½ | ½ | 6 faces |
| Quarter beam | 1 × ½ × ½ | ¼ | 12 edges |
| Eighth cube | ½ × ½ × ½ | ⅛ | 8 corners |

## Saw and recipes

Craft a **Stone Saw** from three cobblestone (`M`) and two sticks (`S`):

```text
MM.
.SM
S..
```

Iron and diamond saws are made from a Smithy-forged blade and a Tool Handle at the Assembly Bench ([manufacturing](manufacturing.md#equipment)).

Combine a saw with one full block anywhere in a crafting grid to make **two slabs**; saw one slab into **two quarter beams**, and one beam into **two eighth cubes**, the smallest size. The 59 ordinary vanilla slab recipes, 74 slab-producing stonecutter recipes and the Street Paver slab recipe are disabled, so the saw is how slabs are made; waxing an already-cut slab still works. Each cut uses one durability (base durability: stone 131, iron 250, diamond 1,561, scaled by [grade](manufacturing.md#grade)), and the final use produces its pieces and breaks the saw. Vanilla mining tiers apply: obsidian needs a diamond saw.

## Recombining

Put two pieces of the same material and size in any two crafting slots: two eighth cubes make a quarter beam, two beams a slab, and two slabs the original full block. This works in the 2×2 player grid and the crafting table with no saw or durability; split a stack across two slots and shift-click to recombine in bulk. Volume is always preserved. Vanilla's two-slab decorative recipes (chiseled blocks, bamboo mosaic, purpur pillar) take one full block instead, to avoid conflicts.

## Vanilla slabs

Where a material has a vanilla slab, cutting produces that item, and every vanilla slab maps back to its source material and can be cut into beams. Horizontal placement uses the vanilla slab block; vertical placement uses a cut block that picks up and drops as the same slab item. Placed vanilla slabs satisfy matching multiblock requirements. Materials without a slab use a generic material-carrying half item. Conventional modded slab names are mapped when a full source block can be resolved; other mods' slab recipes are not disabled.

Solid full building blocks can be cut. Containers and other block entities, fluids, unbreakable blocks, falling blocks, TNT and non-cube shapes cannot. Pieces keep the source material's textures, sounds and tool requirements; they are not smaller functional machines.

## Placement and joining

- Slabs attach to the center of the clicked face; aiming near a face edge selects a vertical/side slab, and crouching forces the clicked face.
- Beams attach to the clicked face along its nearest edge: horizontal on floors, upright when aiming near a wall's side edge.
- Eighth cubes attach to the aimed corner of the clicked face.
- A transparent textured preview shows the exact piece, sharing the [construction guide](machines.md#building-with-a-controller) opacity and outline settings. Red means an entity blocks placement.
- The eight cells of a block space fill independently in any non-overlapping arrangement — gaps, L-shapes and mixed materials included. Aim at the surface beside the cell to fill. Placement tries the aimed orientation first, then another axis that fits; crouching locks the aimed orientation.
- Matching material automatically becomes a native slab or full block when the filled cells form that shape. Breaking an assembly returns recombinable slabs, beams and cubes without loss; a mixed assembly needs a tool that can harvest all its materials.
- Pieces support waterlogging and stack to 32; drops and pick-block keep material and size.
- Placement costs calories in proportion to volume: 1 kcal for a slab, 0.5 for a beam and 0.25 for an eighth at the default 2 kcal full-block rate, before comfort. Filling an existing assembly costs only the added piece; rejected or canceled placements cost nothing.

Resource-pack textures, Faithful included, are cropped to keep their scale. Cut blocks store up to eight cells in an unticked block entity and render through chunk meshes; geometry, collision and previews share the cell grid. Client material data that arrives after a mesh was built refreshes the model and rebuilds that chunk mesh. Opaque cut faces block light (a solid half wall can enclose a lit room) while open and glass faces pass it; lighting uses fully covered outer faces, and collision and rendering keep the exact shape.

Block-structure machine shells use cut pieces; the workshop bills are in [manufacturing](manufacturing.md).

## Builder's Line

Assemble a **Builder's Line** at the Assembly Bench from **2 Iron Plates + 1 plank of any wood + 2 String**, ten seconds of hand work with no Coal; it is also in the Civilization creative tab or `/give @s civilization:builders_line`.

- To build, hold the reel in the **main hand** and full building blocks in the **offhand**. To mine, hold the reel in the **offhand** and a **pickaxe, axe or shovel** in the main hand.
- **Right-click** a face to mark the start: for building, the cell normal placement would fill; for mining, the block hit. The first click places, mines and opens nothing.
- Aim at the end to see translucent block ghosts (building) or amber target outlines (mining) and a thin cord; red outlines mark invalid or blocked cells. **Right-click again** to start immediately; releasing the button does not stop work, and holding it cannot repeat or double-select. Both endpoint cells count.
- Lines run along **X, Y or Z only**, up to **16 cells** (a single cell is valid); a diagonal or over-long second mark is rejected and the first point kept. Marks use ordinary reach and you may walk between them. Work needs a **16-block eye-to-cell-center range**, clear sight, loaded terrain, world bounds and claim access; it never works through walls or loads chunks.
- **Crouch-right-click cancels.** Changing the selected slot, the working item, reel or hand, or dimension, dying, disconnecting or leaving range also clears the line; completed cells remain.
- Building places **four blocks per second at 20 TPS** from the working-hand stack only, through normal placement events that reject occupied or fluid cells and entities and charge ordinary calories. It never refills from inventory or replaces blocks.
- Mining uses the held tool's **normal effective speed** (hardness, enchantments, grade, comfort, depletion) with a short post-break delay, and ordinary break events, durability, drops, XP and calories; drops stay in the world. An insufficient or broken tool or a changed target stops the line.
- Work stops at the first obstruction, denied action, unloaded cell or inaccessible target; earlier cells are kept and there is no rollback, skipping, saved job or reel wear.

Coverage is inert full-cube vanilla blocks and full Street Pavers, including glass and harvestable ores. Shaped pieces, stairs, slabs, leaves and plants, falling blocks, TNT, fluids, containers and block entities, machine parts and moving vessels are excluded. The client preview is advisory: the server raycasts both clicks and rechecks every target and stack, with one operation per player. The preview follows the [guide performance contract](machines.md#guide-performance-contract).

## Save compatibility

Legacy generic custom halves become their vanilla slab item in player inventories and are hidden from the creative tab. Legacy quarter-thickness plates load as quarter beams with the same volume and material. Older cut blocks update their lighting when their chunk loads.
