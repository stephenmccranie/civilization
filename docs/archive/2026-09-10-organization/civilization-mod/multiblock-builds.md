# Multiblock machines — 0.11.1

Craft and place a controller, then build the machine around it. The controller's furnace-like front faces outwards at the bottom-center of the structure's front wall. The body extends two blocks behind it, one left and one right, and two blocks above it. The Fertilizer Furnace adds a fourth-height chimney.

## In-world building flow

1. Craft the controller using its existing recipe. Its tooltip lists the **additional structural blocks**, excluding the controller's own crafting ingredients.
2. Place it where the machine's bottom/front-center should be. The model's front faces you.
3. Transparent boxes show missing positions. If the preview is not selected, look directly at the controller. Only one nearby controller is selected at a time.
4. Ghost blocks use the actual baked block models and resource-pack textures at 30% opacity. Cobblestone, bricks and copper appear as themselves. Holding an accepted alternative (stone bricks, aged/waxed copper, or a downward hopper for the hatch) previews that variant. A red outline means an existing block must be replaced or the chamber must be emptied; an air requirement has no textured block. Aim at a guide to see the required material in the HUD. Correctly filled positions disappear.
5. The HUD and guides appear only while looking at the selected unfinished build area within eight blocks. Looking away or completing the structure hides both automatically. Walking more than 32 blocks away clears selection. No toggle key is needed. These guides are client-only and never place blocks or load distant terrain.
6. Right-click the controller to open its inventory. Missing-block feedback includes a coordinate and material; the status becomes complete when assembled. The extra Build button and layer-plan screen were removed; the diagrams below are reference documentation only.

The retort is now named **Fertilizer Furnace**. Existing controller items/blocks retain their registry IDs (`civilization:brick_kiln` and `civilization:fertilizer_retort`) to preserve saves. No recrafting or item replacement is required. Existing single-block machines retain inventory but need their shells built before they can work.

## Kiln

Additional materials: **13 cobblestone/stone brick blocks, 4 slabs, 4 quarter beams, 4 eighth cubes**. Stone variants may be mixed. A downward input hopper may replace one full wall block.

From bottom to top: the full 3×3 plinth contains the controller at front-center; the next layer has four inset vertical quarter-beam corners, full side/hatch walls, and the empty center; the roof has four eighth-cube corners, four half-slab edge centers, and one full central crown. The vertical supports occupy the inward corner along both horizontal axes. Roof pieces attach to the bottom of their cells. The in-world guide shows exact shapes and orientations.

The shell uses no bricks, avoiding a progression deadlock. Its controller crafting recipe remains a furnace surrounded by eight cobblestone. Clay blocks and Mineral Coal retain their 0.8 rates: four bricks per 200 ticks, eight uninterrupted batches per fuel.

## Fertilizer Furnace

Additional materials: **13 brick blocks, 4 brick slabs, 4 brick beams, 1 copper slab, 4 copper eighth cubes**. A downward input hopper may replace one full brick wall block. Copper may be fresh, aged or waxed.

The furnace follows the same plinth, inset supports and stepped roof as the kiln. Its roof corners are copper eighth cubes; a copper half slab forms the chimney cap above the full central crown. The hollow chamber and input hatch remain in their original positions. Vanilla slabs are accepted wherever their shape matches; all pieces must have the correct material and orientation.

The controller's recipe remains four brick blocks, four copper ingots and a furnace in the documented alternating pattern. These are in addition to the shell materials. Production remains one Raw Mineral Blend → four fertilizer per 400 ticks, four uninterrupted batches per Mineral Coal. The works cannot fire bricks; the kiln cannot make fertilizer.

## Hopper connections

The block directly above the controller can be replaced with a hopper pointing down into it. Supply that hopper from outside the front of the structure. It feeds the ingredient slot. A hopper outside the controller's front face feeds the fuel slot; a hopper underneath the controller extracts output (dig underneath or elevate the build to make room). Other decorative blocks are allowed outside required positions, but the central chamber must stay air.

No special casing inventories or hidden item transfers are introduced. Inventories remain in the controller. Breaking a shell block does not scatter stored goods. Breaking the controller drops its inventory and the controller itself; shell blocks remain normal independently placed blocks.

## Runtime behavior

Active machines validate 26 required positions for the kiln or 27 for the works on every processing tick. Idle empty controllers stagger checks once per second; opening a menu checks immediately. Validation only reads nearby loaded chunks, uses no global structure scan, and never force-loads chunks.

Removing/replacing a required block or obstructing the chamber stops processing before the next batch can complete. It extinguishes remaining fuel heat and resets unfinished work without consuming another stored fuel or deleting unprocessed materials. Repairing allows operation with new fuel. Breaking a running structure emits `kiln_structure_broken` or `retort_structure_broken` in the machine journal. The latter prefix remains stable for existing audit tools despite the display-name change.

If a required neighboring chunk is unavailable, the machine waits and preserves heat/work until all required chunks are loaded. Structure validity is recomputed after reload; a saved machine cannot bypass validation. Normal server restart and chunk-unload work/fuel behavior otherwise remains unchanged. Oxidation does not invalidate copper fittings.

## Verification

56 Minecraft GameTests and 19 unit tests cover existing production/calorie behavior plus standalone controller rejection, immediate interruption and repair at a batch boundary, exact missing-block feedback, aged/waxed copper, hopper orientation, air chamber, all horizontal facings, and structure revalidation after save/load. Earlier machine tests now construct full shells and run in larger test templates. Client startup checks registrations and assets; the new shapes and placement previews are visually checked with Photon and Faithful in the hidden 1920×1080 test client.
