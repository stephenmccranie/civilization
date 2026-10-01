# Multiblock machines

Craft and place a controller, then build the machine around it. This page covers the Kiln and Fertilizer Furnace; [oil industry](industry.md) owns the newer pump/refinery/drill layouts, which all use contextual guides; the fully modeled Oil Derrick adds native sections from held materials. For the machines below, the controller's furnace-like front faces outwards at the bottom-center of the structure's front wall. The body extends two blocks behind it, one left and one right, and two blocks above it. The Fertilizer Furnace adds a fourth-height chimney.

## In-world building flow

1. Craft the controller using its existing recipe. Its tooltip lists the **additional structural blocks**, excluding the controller's own crafting ingredients.
2. Place it where the machine's bottom/front-center should be. The model's front faces you.
3. Transparent boxes show missing positions. If the preview is not selected, look directly at the controller. Only one nearby controller is selected at a time.
4. Ghost blocks use the actual baked block models and resource-pack textures at the configured guide opacity (32% default). Cobblestone, bricks and copper appear as themselves. Holding an accepted alternative (stone bricks, aged/waxed copper, or a downward hopper for the hatch) previews that variant. Holding the required block preserves the guide's orientation and connections; it never substitutes the item's default facing. A red outline means an existing block must be replaced or the chamber must be emptied. Incorrectly occupied positions show the real block with a correction outline, without a full-bright ghost layered over it; held-stack changes refresh the guide on the next client tick; other world changes appear at the next bounded guide scan. An air requirement has no textured block. The guide sets its own neutral shader color and restores the previous color afterward. Textures remain translucent to suggest the whole build, but hard outlines are shown only where they are not hidden by nearer guide surfaces or real blocks. Guides keep all on-screen ghost textures but draw at most 64 hard outlines, favoring nearby pieces and retaining the directly aimed visible piece within that limit. The nearest directly aimed guide gets a cyan outline and its material in the HUD; already-placed blocks stop target selection. Correctly filled requirements disappear. Declared shared-cell pieces (such as Smithy table feet and tops) may occupy the same block space: each missing piece keeps its own guide, while excess cells or wrong materials are marked for correction.
5. The HUD and guides appear while looking at the selected unfinished build area within eight blocks. The top-left hint lists remaining quantities by material and piece size, including pieces behind obstructions; air spaces count as obstructions but need no item. Visibility uses the rendered camera for both aiming and obstruction, with a 0.1-block retention margin and a 150 ms release delay to avoid flicker when walking along block edges. Looking away hides the guide after that brief delay; completing the build, opening a menu or hiding the HUD hides it immediately. The 3D guide and HUD share one visibility decision per frame. Walking more than 32 blocks away clears selection. No toggle key is needed. The guide is client-only and never loads distant terrain.
6. Right-click the controller with a matching stack to place its missing blueprint pieces, or place them yourself. One click uses as many of the held items as safely fit, with exact cut shapes and functional-block facing. Shared workshop cells accept their separate pieces without charging twice. Full blocks need full items; halves, quarter beams and eighth cubes need the corresponding saw-cut items or native slabs. **You must make the cuts yourself with a saw; the controller neither cuts full blocks nor recuts the wrong-sized pieces.** Already-correct pieces cost nothing. Existing blocks, liquids and inaccessible or unloaded spaces are never replaced; clear obstructions yourself. Empty-hand right-click opens the controller's inventory. The extra Build button and layer-plan screen remain unnecessary; the diagrams below are reference documentation only.

The retort is now named **Fertilizer Furnace**. Existing controller items/blocks retain their registry IDs (`civilization:brick_kiln` and `civilization:fertilizer_retort`) to preserve saves. No recrafting or item replacement is required. Existing single-block machines retain inventory but need their shells built before they can work.

## Kiln

Additional materials: **13 cobblestone/stone brick blocks, 4 slabs, 4 quarter beams, 4 eighth cubes**. Stone variants may be mixed. A downward input hopper may replace one full wall block.

From bottom to top: the full 3×3 plinth contains the controller at front-center; the next layer has four inset vertical quarter-beam corners, full side/hatch walls, and the empty center; the roof has four eighth-cube corners, four half-slab edge centers, and one full central crown. The vertical supports occupy the inward corner along both horizontal axes. Roof pieces attach to the bottom of their cells. The in-world guide shows exact shapes and orientations.

The shell uses no bricks, avoiding a progression deadlock. Its controller is crafted from cobblestone only. Clay blocks produce four bricks per 200 ticks, with two uninterrupted batches per Coal.

## Fertilizer Furnace

Additional materials: **13 brick blocks, 4 brick slabs, 4 brick beams, 1 copper slab, 4 copper eighth cubes**. A downward input hopper may replace one full brick wall block. Copper may be fresh, aged or waxed.

The furnace follows the same plinth, inset supports and stepped roof as the kiln. Its roof corners are copper eighth cubes; a copper half slab forms the chimney cap above the full central crown. The hollow chamber and input hatch remain in their original positions. Vanilla slabs are accepted wherever their shape matches; all pieces must have the correct material and orientation.

The controller's recipe remains four brick blocks, four copper ingots and a furnace in the documented alternating pattern. These are in addition to the shell materials. Production remains one Raw Mineral Blend → four fertilizer per 400 ticks, four uninterrupted batches per Coal. The works cannot fire bricks; the kiln cannot make fertilizer.

## Survey Table

A separate **2×2, one-block-high** assembly: one Survey Table Controller and three Survey Table Sections. Its contextual guide uses the same transparent models and visible-surface outlines as the machines. The controller sits at a corner; follow its guide for the other three positions. Completing the structure enables a continuous tabletop map and right-click inspection. Removing any section disables both. See [Survey Table recipes and behavior](ownership-trade.md#survey-table).

## Town workshops

The Tannery, Textile Workshop and Smithy use the same partial-block guide system. The Tannery is a low brick vat with an open center and narrow oak rack; the Textile Workshop is a thin oak bench on shared-cell feet with an open loom frame and central copper roller; the Smithy combines a broad hearth, chimney, bench and real anvil. Exact material counts, recipes and migration behavior live in [Town workshops](industry.md#town-workshops).

## Hopper connections

The block directly above the controller can be replaced with a hopper pointing down into it. Supply that hopper from outside the front of the structure. It feeds the ingredient slot. A hopper outside the controller's front face feeds the fuel slot; a hopper underneath the controller extracts output (dig underneath or elevate the build to make room). Other decorative blocks are allowed outside required positions, but the central chamber must stay air.

No special casing inventories or hidden item transfers are introduced. Inventories remain in the controller. Breaking a shell block does not scatter stored goods. Breaking the controller drops its inventory and the controller itself; shell blocks remain normal independently placed blocks.

## Runtime behavior

Active machines validate 26 required positions for the kiln or 27 for the Fertilizer Furnace on every processing tick. Idle empty controllers stagger checks once per second; opening a menu checks immediately. Validation only reads nearby loaded chunks, uses no global structure scan, and never force-loads chunks.

Removing/replacing a required block or obstructing the chamber stops processing before the next batch can complete. It extinguishes remaining fuel heat and resets unfinished work without consuming another stored fuel or deleting unprocessed materials. Repairing allows operation with new fuel. Breaking a running structure emits `kiln_structure_broken` or `retort_structure_broken` in the machine journal. The latter prefix remains stable for existing audit tools despite the display-name change.

If a required neighboring chunk is unavailable, the machine waits and preserves heat/work until all required chunks are loaded. Structure validity is recomputed after reload; a saved machine cannot bypass validation. Normal server restart and chunk-unload work/fuel behavior otherwise remains unchanged. Oxidation does not invalidate copper fittings.

## Guide performance contract

This contract applies to every current and future multiblock guide. Ordinary block assemblies use `MachinePreview`; native Oil Derrick sections use `DerrickPreview`. Both use the shared `GuidePerformance` policy. New assemblies should reuse these renderers; a custom renderer must use the same policy.

- Cache static positions and bounds by world, controller and orientation/layout. Cache native missing-section geometry by construction state. Clear snapshots when selection or world changes.
- Prepare missing parts, obstruction states and material counts outside rendering. Refresh immediately on a changed selection/construction key or relevant held-stack change. Poll external block changes through `GuidePerformance.Refresh`, once every ten client ticks. Read only loaded terrain; never load chunks for a guide.
- Render cached geometry. Never rebuild collision unions or repeatedly query structural world state in the frame loop. Camera aiming and visibility still update each frame.
- Cull off-screen parts before outline selection. `GuidePerformance.outlines` caps outlines at 64, including the aimed visible part. Use `outlineSteps` for adaptive outline sampling. Keep the complete visible translucent build preview; the outline budget does not remove construction sections.
- Verify shared cadence, culling and budget behavior with `GuidePerformanceTest`, then run the relevant [focused visual check](testing.md#focused-visual-checks). For costly geometry changes, compare bounds and preparation timings; preparation timings are not a total-FPS benchmark.

## Verification

The server and unit suites cover existing production/calorie behavior plus standalone controller rejection, immediate interruption and repair at a batch boundary, exact missing-block feedback, aged/waxed copper, hopper orientation, air chamber, all horizontal facings, and structure revalidation after save/load. Earlier machine tests now construct full shells and run in larger test templates. Client startup checks registrations and assets; the new shapes and placement previews are visually checked with Photon and Faithful in the hidden 1920×1080 test client.
