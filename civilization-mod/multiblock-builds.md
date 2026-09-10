# Multiblock machines — 0.9.2

Craft and place a controller, then build the machine around it. The controller's furnace-like front faces outwards at the bottom-center of the structure's front wall. The body extends two blocks behind it, one left and one right, and two blocks above it. The Fertilizer Works adds a fourth-height chimney.

## In-world building flow

1. Craft the controller using its existing recipe. Its tooltip lists the **additional structural blocks**, excluding the controller's own crafting ingredients.
2. Place it where the machine's bottom/front-center should be. The model's front faces you.
3. Transparent boxes show missing positions. If the preview is not selected, look directly at the controller. Only one nearby controller is selected at a time.
4. Ghost blocks use the actual baked block models and resource-pack textures at 30% opacity. Cobblestone, bricks and copper appear as themselves. Holding an accepted alternative (stone bricks, aged/waxed copper, or a downward hopper for the hatch) previews that variant. A red outline means an existing block must be replaced or the chamber must be emptied; an air requirement has no textured block. Aim at a guide to see the required material in the HUD. Correctly filled positions disappear.
5. The HUD and guides appear only while looking at the selected unfinished build area within eight blocks. Looking away or completing the structure hides both automatically. Walking more than 32 blocks away clears selection. No toggle key is needed. These guides are client-only and never place blocks or load distant terrain.
6. Right-click the controller to open its inventory. Missing-block feedback includes a coordinate and material; the status becomes complete when assembled. The extra Build button and layer-plan screen were removed; the diagrams below are reference documentation only.

The retort is now named **Fertilizer Works**. Existing controller items/blocks retain their registry IDs (`civilization:brick_kiln` and `civilization:fertilizer_retort`) to preserve saves. No recrafting or item replacement is required. Existing single-block machines retain inventory but need their shells built before they can work.

## Kiln

Additional materials: **25 cobblestone and/or stone bricks**. They may be mixed. One wall position may instead be a downward-facing hopper, reducing the stone requirement to 24 plus that hopper.

Top-down layers, bottom to top; the front is the **bottom row** of every plan:

```text
Layer 1       Layer 2       Layer 3
S S S         S S S         S S S
S S S         S . S         S S S
S C S         S H S         S S S
```

S = cobblestone or stone bricks. C = controller. `.` = empty chamber. H = same stone wall, or a downward-facing hopper pointing into the controller.

The shell uses no bricks, avoiding a progression deadlock. Its controller crafting recipe remains a furnace surrounded by eight cobblestone. Clay blocks and Mineral Coal retain their 0.8 rates: four bricks per 200 ticks, eight uninterrupted batches per fuel.

## Fertilizer Works

Additional materials: **21 brick blocks + 5 full copper blocks**. One brick wall may instead be the input hopper, reducing bricks to 20. Copper blocks may be fresh, exposed, weathered, oxidized, or waxed; cut copper, slabs and stairs are not full copper blocks for this pattern.

```text
Layer 1       Layer 2       Layer 3       Layer 4
B B B         B B B         P B P
B B B         B . B         B B B           P
B C B         B H B         P B P
```

B = brick block. P = full copper block. C = controller. `.` = air. H = brick wall or downward input hopper. Blank spaces in layer 4 are unrestricted; only the central chimney block is required.

The controller's recipe remains four brick blocks, four copper ingots and a furnace in the documented alternating pattern. These are in addition to the shell materials. Production remains one Raw Mineral Blend → four fertilizer per 400 ticks, four uninterrupted batches per Mineral Coal. The works cannot fire bricks; the kiln cannot make fertilizer.

## Hopper connections

The block directly above the controller can be replaced with a hopper pointing down into it. Supply that hopper from outside the front of the structure. It feeds the ingredient slot. A hopper outside the controller's front face feeds the fuel slot; a hopper underneath the controller extracts output (dig underneath or elevate the build to make room). Other decorative blocks are allowed outside required positions, but the central chamber must stay air.

No special casing inventories or hidden item transfers are introduced. Inventories remain in the controller. Breaking a shell block does not scatter stored goods. Breaking the controller drops its inventory and the controller itself; shell blocks remain normal independently placed blocks.

## Runtime behavior

Active machines validate 26 required positions for the kiln or 27 for the works on every processing tick. Idle empty controllers stagger checks once per second; opening a menu checks immediately. Validation only reads nearby loaded chunks, uses no global structure scan, and never force-loads chunks.

Removing/replacing a required block or obstructing the chamber stops processing before the next batch can complete. It extinguishes remaining fuel heat and resets unfinished work without consuming another stored fuel or deleting unprocessed materials. Repairing allows operation with new fuel. Breaking a running structure emits `kiln_structure_broken` or `retort_structure_broken` in the machine journal. The latter prefix remains stable for existing audit tools despite the display-name change.

If a required neighboring chunk is unavailable, the machine waits and preserves heat/work until all required chunks are loaded. Structure validity is recomputed after reload; a saved machine cannot bypass validation. Normal server restart and chunk-unload work/fuel behavior otherwise remains unchanged. Oxidation does not invalidate copper fittings.

## Verification

36 Minecraft GameTests and 19 unit tests cover existing production/calorie behavior plus standalone controller rejection, immediate interruption and repair at a batch boundary, exact missing-block feedback, aged/waxed copper, hopper orientation, air chamber, all horizontal facings, and structure revalidation after save/load. Earlier machine tests now construct full shells and run in larger test templates. Client startup checks registrations and assets; in-world preview appearance has not yet been visually verified on this host.
