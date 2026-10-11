# Native modeling practice

Read this for shaped blocks, machines, vehicles and creatures. The asset pipeline in [README](README.md#complete-asset-pipeline) still owns briefs, provenance and publication. This reference covers the gap between a good concept and a good native model.

## Make the model easy to judge

Use a small set of measured landmarks: overall envelope, primary mass ratios, shaft axes, contact heights and rotating clearances. Record them in the asset brief. A beautiful perspective image is not a dimensioned blueprint. Treat ambiguous hidden geometry as a design problem, not as something the generator has solved.

Block out the **whole assembly** before detailing one component. Use a single neutral material to expose poor proportions; check front, back, both sides and two three-quarter views at the same scale. Then test the actual material palette. Keep the known-good runtime asset intact until the replacement passes review.

`studio.capture` uses Blockbench's own offscreen Preview at a fixed resolution, target and orthographic span. It temporarily hides grid/outlines, restores them, and disposes the preview. It does not resize the user's editor. Each PNG has a `.view.json` with camera settings and its hash. Use the same settings to compare revisions. `studio.contact_sheet` lays out real captures; it does not generate model imagery.

For an existing native project, capture all six views without changing its source:

```powershell
python tools/modeling/review_model.py art/assets/oil_engine/studio/model/assembly.bbmodel --output .tools/modeling/engine-review --target 5 20 24 --span 75
```

Coordinates are model units; `span` is the square camera's visible world-unit width. Use the same target/span for before/after models. The model opens in a new tab.

Use close views to judge joints, and ordinary gameplay distance to judge readability. A high-resolution close-up alone can make an over-detailed model seem better than it plays.

## Build shape, then function, then surface

1. Primary shapes: masses, supports, negative space, unmistakable silhouette.
2. Functional secondary shapes: flanges, bearing collars, lids, openings, controls, feet. Each should explain assembly or operation.
3. Surface: material variation, seams and fasteners that do not need to change silhouette.

Fix a weak silhouette with geometry. Fix unreadable materials with their value/contrast and UVs. Adding more cubes or random bolts is not a substitute for either. Keep brass on fittings; use steel highlights to articulate dark castings without turning every edge gold.

Every moving assembly needs a named pivot in **assembly coordinates**, not guessed local coordinates. Derive component export offsets from that same source. Inspect the complete swept shape, not only the stopped pose. Distinguish deliberate shaft/bearing contact from unwanted wheel/body interference. An animation bone is a visual transform, not a Sable physics joint.

## Use the native format's constraints

Static Java 1.21.1 models allow cuboids and limited single-axis rotations. Do not design freeform meshes and assume a Java export will preserve them. GeckoLib can handle animated cuboids; it does not remove the need to keep static item/guide geometry compatible.

`studio.Model` supplies named boxes and X/Z octagonal casting approximations. These are conveniences, not a compulsory shape vocabulary. Castings use overlapping legal cuboids; cap planes are separated by tiny explicit offsets to avoid coplanar flicker. This is not a mesh boolean operation and does not remove hidden geometry. Inspect seams under animation and Minecraft lighting. Prefer a simpler shape if the approximation becomes expensive or visually poor.

## UVs are part of the design

New artwork uses **four texture pixels per model unit** (64px per standard block face). Retained 32px families use two pixels per unit until reviewed migration. Pass `density=4` to `studio.create` for new 64px assets; its default of two preserves existing build scripts. `studio.uv_faces` checks each face against its named material region. It rejects a face that would exceed the region instead of silently clamping and stretching it. Enlarge the atlas region or split a surface deliberately. A larger atlas is fine; it does not imply a higher texel density.

Explicit `fit` regions are for authored decals such as a gauge, not an escape hatch for all materials. A gauge decal must contain just the gauge. Mapping an entire instrument panel onto a small gauge gives duplicate controls and an unreadably tiny face. Declare per-face overrides so the decal does not wrap around every side of the housing.

Reuse inspected catalog materials where suitable; otherwise generate one coherent master and reduce it through the existing texture pipeline. Verify material boundaries and tiling in the model. Quiet surfaces still need purposeful variation; there is no global noise pass. Reserve brighter steel for joints/rims and inspect it against the body at gameplay scale.

The engine study uses material reuse for unimportant faces and a dedicated instrument decal. Unique painted islands can be added for a specific visual need; do not automatically create a full custom unwrap for every cube.

## Efficient native loop

Keep the asset-specific geometry recipe in its asset folder; share only operations that actually repeat in `studio.py`. A complete rebuild should take seconds through one MCP session, rather than hundreds of conversational cube calls. Keep the resulting `.bbmodel` editable. If a person edits the model manually, reconcile those edits into the source recipe before rebuilding; never silently overwrite them.

Use `create` to build the project, `save` for native exports, and `capture` for evidence. Scripts must explicitly choose what they overwrite. Discover plugin schemas before using new operations. Preserve unrelated project tabs and save authored work before closing it. A new empty project is needed before `risky_eval` when the editor has no Undo context.

The pinned MCP eval wraps calls in an Undo edit. When closing a generated tab through native APIs, select the retained project **after** closing and before the call returns; otherwise its `finishEdit` can fail because no project is selected. Close only known generated tab UUIDs after backing up their editable content. Do not leave dozens of disposable studies for the user to dismiss.

Run the focused studio regression checks when changing its UV/name/bounds rules:

```powershell
python tools/modeling/test_studio.py
```

Validate native Java/GeckoLib exports with the existing pipeline. For visual-only work, run the relevant hidden Minecraft scene after the editor model is convincing; do not launch Minecraft for every dimensional adjustment. Review real screenshots, model items, guides and working/stopped states. Record what remains unverified instead of filling a review with generic passing claims.

Support posts should stop at the underside of a cap, not share its exposed top plane. Check these joints from grazing angles in the shader scene: overlapping coplanar surfaces can flicker even when an editor still looks clean.

Do not distribute panel borders, corner plates or brass squares across structural feet by default. Detail must describe construction: access covers can have seams and fasteners; cast supports use quiet metal variation and actual mounting hardware.

## Surface accents

Custom modeled machines are not constrained to one-block material tiles. Reduce an aspect-matched crop from the original master to each measured face, preserving continuous grain without a periodic block grid. Recessed panels, raised rims and prominent fastener heads need geometry when their depth should be visible; reserve painted marks for secondary detail. Closed doors meet a modeled seat/rebate, and fixed firebox surrounds close unintended gaps while keeping deliberate vents open.

Prevent overlap in the source geometry: adjacent panels and posts meet at boundaries; raised fittings replace or sit clearly ahead of their backing surface. Remove duplicate buried faces where appropriate. Shared exposed planes cause z-fighting even if a still view looks acceptable. Check close and grazing views, including moving poses; brighter trim cannot repair an overlap. Keep authored UV islands disjoint with padding, and contain painted strokes within their island. Intentional reuse of a base material crop is different from overwriting an authored island. Use coherent trim tones rather than alternating bright edge pixels. Transparent windows must expose the chamber, and controls should visibly connect to their mechanism.

Shared material tiles alone do not place corners or seams on a part. For prominent covers and supports, allocate face-sized UV islands at the established density, then use nine-slice panel layouts to preserve corner pixels while fitting the middle. Keep secondary faces quieter; do not paste a complete framed panel onto every surface. Inspect actual model and shader views before increasing contrast. The engine `studio/surface_textures.py` demonstrates this reproducibly.

## Creature modeling

For custom mobs, establish the whole silhouette and a representative joint/material treatment before multiplying detail. Keep the approved concept's identity, expression and color distribution visible in the native model. The following route works within Minecraft's cuboid language and GeckoLib; it does not require a reconstruction service.

1. **Measure the character.** Record height, head/body ratio, shoulder-to-waist width, limb lengths, hand/foot size and distinctive features in the brief. Assign references separate roles for anatomy, expression, palette and surface detail. Infer hidden shapes deliberately; perspective art is not a blueprint.
2. **Build the whole body in clay.** Use connected primary masses, purposeful tapers and joint pivots. Inspect front, side, back and three-quarter views at matching scale. Fix gaps, bulky transitions and the resting expression here. Use rotations where they improve silhouette or movement; circular layers of small cubes rarely preserve the intended Minecraft shape.
3. **Finish one representative section.** For a humanoid, test the torso, shoulder and one complete arm with both neutral clay and fitted pixel art. Resolve the shoulder/elbow/wrist transitions and material treatment before extending them across the body. Choose a similarly revealing section for other anatomies. Preserve parts that already work rather than rebuilding the whole character each turn.
4. **Give geometry and paint distinct jobs.** Use cuboids for silhouette, joint structure, useful fingers/claws and prominent signature plates. Paint smaller scales, anatomical shading and cloth folds on measured face islands. Use deliberate pixel clusters, shaped highlights, contact shadows and quiet regions; avoid generic noise or repeated raised grids across every body part. Retain the concept's broad color regions while simplifying. A few connected clothing panels with painted folds can read better than many raised strips. Fit the treatment to anatomy rather than copying machine panel borders onto skin.
5. **Extend, then inspect the actual whole model.** Review the finished treatment from all sides in clay and color, including an ordinary gameplay-distance framing. Keep one matching-camera before/after comparison. Check close joint views and relevant animation poses; sample full cycles for clearance before publication. Correct specific defects such as accidental spikes, exposed gaps or stiff transitions before adding more detail. Cube count is a cost to assess, not a quality target or a universal cap.
6. **Save and verify the native source.** Keep one authoritative editable `.bbmodel`, with the recipe as reconciled provenance when the saved project owns edits. Use the existing `studio.capture`/`review_model.py` helpers for real views, padded authored UV islands at the declared 64px/block density, and the existing export validators. Verify saved/reopened geometry, animation and texture mapping. Native acceptance does not establish Minecraft lighting, motion or performance; those remain part of the shared publication stage.

The local [Lacertan study](../../art/assets/lacertan/head-study/README.md) is the accepted native example: connected torso/arm masses, fitted painted body scales and cloth, with the neutral head and exposed dorsal plates retained. Its representative clay/painted section, full views and comparison are reusable review examples. The original project and visual history stay local under the publication boundary; this procedure remains usable without those files.

## Sources and scope

- [Blockbench Minecraft style guide](https://blockbench.net/wiki/guides/minecraft-style-guide/): economical shapes and consistent pixel density. New assets use 64px per block; retained families use 32px rather than vanilla's 16px.
- [Blockbench Preview API](https://web.blockbench.net/docs/classes/generated_preview_preview.Preview.html): offscreen previews, projection, rendering and cleanup. The helper was checked against the installed pinned editor, not only current web documentation.

These practices improve iteration and catch specific failures; they are not an automatic aesthetic score or a promise to make every model right in one generation.
