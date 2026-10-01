# Cutting and shaped construction — 0.11.3

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

Combine a saw with one full block anywhere in a crafting grid to make **two slabs**. Saw one slab into **two quarter beams**, then one beam into **two eighth cubes**. Eighths are the minimum size. Each cut uses one durability: stone 131, iron 250, diamond 1,561. The final use produces pieces and breaks the saw. Vanilla material mining-tier requirements apply (obsidian requires a diamond saw).

## Recombining in the crafting grid

Put two pieces of the same material and size in any two slots: two eighth cubes make one quarter beam, two beams make one slab, and two slabs restore one original full block. This works in the player's 2×2 grid and the crafting table, without a saw or durability cost. Split a stack across two slots; shift-click the output to recombine in bulk. Each step preserves material volume. Matching legacy custom halves and vanilla slabs can be mixed.

To avoid conflicting outputs, vanilla's two-slab decorative recipes (chiseled blocks, bamboo mosaic and purpur pillar) now take one corresponding full block instead.

## Vanilla slab integration

Where a material already has a vanilla slab, cutting produces that existing item. Existing slab items and recipes remain usable; there is no parallel custom half-item requirement. All vanilla slab types are mapped to their source material and can be cut into beams. Horizontal placement uses the vanilla slab block; vertical placement uses a cut block which picks up and drops as the same vanilla slab item. Existing placed vanilla slabs also satisfy matching multiblock requirements. Materials with no slab use a generic material-carrying half item. Conventional modded slab names are mapped when a full source block can be resolved.

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

Source resource-pack textures are cropped to preserve scale, including Faithful. Unticked material entities store up to eight cells and render through chunk meshes; there is no per-cell tick or entity. Geometry, collision and previews share the same cell grid. Horizontal vanilla slabs retain their normal block implementation.

## Machines and old saves

The kiln and Fertilizer Furnace use upright quarter-beam corner supports, half-slab roof edges and eighth-cube roof corners. Their plinths and central crowns remain full blocks. Controllers, inventories, recipes and hopper ports are unchanged. Old shells need reshaping using the guides, which check material, geometry and orientation. Nothing is automatically deleted.

| Machine | Full blocks | Slabs | Beams | Eighth cubes |
|---|---|---|---|---|
| Kiln | 13 stone | 4 stone | 4 stone | 4 stone |
| Fertilizer Furnace | 13 bricks | 4 brick + 1 copper | 4 brick | 4 copper |

Stone means cobblestone or stone bricks. Accepted copper oxidation/wax variants remain valid. A downward hopper may replace the full front hatch. The chamber must remain empty.

Existing custom halves remain usable and return vanilla slab items where available. Old quarter plates become quarter beams, preserving quarter-block volume and material. The new eighth stage is additional. No quarter-thickness plates remain.

## Verification

Server tests cover vanilla slab mappings and actual placement, cutting yields through the eighth stage, durability, persistence, joining consumption, exactly 6/12/8 unique shapes, half-grid dimensions, rotations, waterlogging, and multiblock geometry/material checks. Hidden 1920×1080 visual checks cover the slab, beam and cube previews and assembled machines with Photon and Faithful.

0.11.3 verification: 64 dedicated-server tests, including all 255 occupancy masks, non-rectangular placement order, four beams, mixed sizes/materials, native-slab additions, overlap/entity rejection, save/load and lossless drops, plus canceled placement rollback and event delivery for calorie accounting. Hidden 1920×1080 Photon/Faithful visual fixture: mixed-material and non-rectangular assemblies.

0.11.4 fixes stale cobblestone rendering when material packets arrive after the block mesh was built. Loading client material/cell data now refreshes model data and invalidates the chunk mesh. The material-sync visual regression deliberately separates block placement from material delivery; it checks the received material identities and captures the rendered result. Server material data and item quantities are unchanged.
