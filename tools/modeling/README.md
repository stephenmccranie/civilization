# Model authoring toolset

Civilization models are made in Blockbench, driven through Jason Gardner's MCP plugin, with the GeckoLib editor plugin for animated assets; GeckoLib 4 is the runtime library (version in `civilization-mod/gradle.properties`). [pins.json](pins.json) pins the authoring downloads, commits and checksums. The visual rules live in [design](../../design_direction.md#setting-and-material-style); the workflow and quality bar live in the [civilization-art skill](../../.claude/skills/civilization-art/SKILL.md). This file covers setup, commands and modelling craft.

## Setup

From the repository root, with [Pillow](https://pypi.org/project/pillow/) installed (`python -m pip install --user pillow`; setup checks it):

```powershell
python tools/modeling/setup.py
.\tools\modeling\start.ps1
```

The portable editor, plugins, a separate profile and the pinned [Codex CLI](https://github.com/openai/codex) live in ignored `.tools/modeling`. Codex draws concept images on the owner's ChatGPT plan and shares the Codex app's sign-in; without the app, run `.tools\modeling\codex\codex-x86_64-pc-windows-msvc.exe login` once. In Blockbench use **File → Plugins → Load Plugin from File** for `.tools/modeling/mcp.js` and `.tools/modeling/geckolib.js`, permit MCP network access and leave the editor open. The launcher reuses a ready editor, waits up to 45 seconds for MCP to initialise and never opens a duplicate of this profile; new launches log to `.tools/modeling/startup.log`. It disables Chromium's GPU sandbox, whose GPU process cannot start when the editor is launched from the Claude desktop app. Save projects before restarting an unresponsive editor. Rerun setup to verify the pinned downloads.

Setup patches the pinned MCP plugin to bind `127.0.0.1` instead of all interfaces and records the original and patched hashes. It remains [Jason Gardner's plugin](https://github.com/jasonjgardner/blockbench-mcp-plugin/tree/b187b4b056f0efafcc573335400ecbb21ad26ecc) under its upstream license; authoring tools never go into the Minecraft JAR.

The ignored `art/` and `.tools/` folders live only in the main checkout, and its git worktrees share them: `setup.py`, the launcher and `art.py` find the main checkout with `git rev-parse --git-common-dir`, `art.py` reads command-line paths relative to it, and `preview` and `publish` write the runtime resources of the checkout that runs them. Keep asset folders in the main checkout's `art/assets/`; an `art/` folder inside a worktree is deleted with the worktree.

The repository's `.mcp.json` registers the server (`http://127.0.0.1:3000/bb-mcp`) for Claude Code; a session started before the editor was running may need a restart to see the tools. The direct client works regardless:

```powershell
python tools/modeling/mcp.py list
python tools/modeling/mcp.py call get_project_info
python tools/modeling/mcp.py call capture_screenshot --out .tools/modeling/view
python tools/modeling/mcp.py call place_cube .tools/modeling/cube-arguments.json
```

Read returned tool schemas before using a new operation. `--out` saves responses and extracted images; console output omits image bytes. The client handles initialisation, sessions, JSON/SSE and tool errors.

## Asset records and commands

Each asset has one compact schema-2 `asset.json`, managed by `art.py`, in its own folder under the local `art/assets/`. Its brief has three short fields: `purpose`, `target` (recognisable features and material treatment) and `constraints` (bounds, interfaces, motion, construction and gameplay invariants). Name inspected references by role — shape, material or project style — at most five, the image tool's limit; every generation attaches them in order and states each role ahead of the prompt. List `neighbours`, workspace images of the assets it must sit beside, to put them in its review lineup. Choose one authoritative `source`: a hand-edited Blockbench `project` or a Python `recipe`. A recipe owns its generated models, a project owns its derived exports, and a recipe never silently overwrites manual edits. Declare extra source dependencies, native `models`, output mappings, GeckoLib manifests and, for a model painted from a generated master, its `texture` layout. Use `--kind entity` for character models.

```powershell
python tools/modeling/art.py init art/assets/example --kind item
# Fill asset.json; author the source and declare output paths.
python tools/modeling/art.py materials
python tools/modeling/art.py record art/assets/example --prompt art/assets/example/concept-prompt.txt
# Declare the texture layout, then draw and reduce the texture master.
python tools/modeling/art.py texture art/assets/example --prompt art/assets/example/texture-prompt.txt
python tools/modeling/art.py prepare art/assets/example
python tools/modeling/art.py views art/assets/example --model model.bbmodel --target 8 8 8 --span 24
# Inspect the images; review.json holds decision, criteria (the four questions), defects and optional limits.
python tools/modeling/art.py review art/assets/example --stage build --review art/assets/example/review.json --evidence art/assets/example/review/views.png
python tools/modeling/art.py preview art/assets/example
# Run the relevant civilization-mod/dev.ps1 visual scene and inspect the real screenshots.
python tools/modeling/art.py review art/assets/example --stage verify --review art/assets/example/review.json --evidence civilization-mod/runs/visual/screenshots/example-frames.png
python tools/modeling/art.py publish art/assets/example
python tools/modeling/art.py check art/assets/example --complete
```

- `record --prompt` draws a concept with Codex's image tool, passing the declared references to it, or records a supplied concept given `--image`; `texture --prompt` does the same for a texture master. Either way the asset keeps the image as a numbered generation with its exact prompt and references; the references' bytes are kept by content in the local `art/references/`, so redrawing a referenced asset later leaves the record valid. Each generation spends the owner's ChatGPT plan usage.
- `prepare` builds the texture atlas, then runs the recipe or exports the saved project (never an arbitrary open tab) to `export/`, and checks the whole bundle and UV density. Recipes run in their asset folder with the running checkout's `tools/modeling` on `PYTHONPATH`, so they import `studio`, `mcp` and `proof` directly. Large native exports compile once in Blockbench and transfer in bounded text chunks; incomplete transfers are rejected.
- `views` captures native Blockbench views; `compare --images` maintains the single reference-versus-actual comparison sheet.
- `preview` needs a current build review and copies validated candidates into runtime resources for the focused scene, keeping replaced files in local content-addressed history (`preview-backup.json`). `publish` needs a current verify review; `check --complete` confirms fresh review hashes, exports and the runtime receipt. Evidence is any workspace image, such as a scene screenshot, and the record keeps its own copy; source, export and evidence hashes are recorded automatically and history is immutable.
- A model-only deliverable without runtime targets validates its paired manifest with `assets.validate` and `art.definition`/`art.uv_density` from its own verification script; it is not "published" until it passes a Minecraft scene.
- `init` on an existing asset restarts it: the previous record goes to `history/` and its files stay in the folder as provenance. A restarted schema-1 asset becomes schema 2.
- Older schema-1 records keep `pipeline.py` until restarted; `assets.py` and the item/family reducers remain their export backends.

Runtime GeckoLib files follow `geo/<id>.geo.json`, `animations/<id>.animation.json` and `textures/entity/<id>.png`; [MachineGeoModel](../../civilization-mod/src/main/java/dev/civilization/client/MachineGeoModel.java) resolves those paths and leaves machine-specific state explicit.

Native export details: `export_model` with codec `project` saves the source and `bedrock` invokes the GeckoLib geometry hooks; discover codecs rather than guessing. `Animator.buildFile(null, Animation.all.map(a => a.name))` exports animations where no codec is exposed.

The tools write UTF-8 text with LF line endings; recipes write theirs with `newline='\n'`, so an export has the same bytes on every platform.

Fast checks need no Minecraft launch: `python -m unittest discover -s tools/modeling -p "test_*.py"`.

### Textured models

Image generation paints the materials; the native geometry and its UVs stay in the recipe. Items and machines follow one route: **concept → texture master → atlas → model**.

1. **Concept.** `record` draws the visual target from the brief and style references. It settles character, proportions and materials, not dimensions.
2. **Layout.** Plan the faces at four texels per model unit, then declare the layout:

   ```json
   "texture": {
     "grid": [3, 2], "ground": "#808080", "atlas": "model/backpack.png", "size": [128, 128], "colors": 32,
     "regions": {
       "front": {"cell": [0, 0], "rect": [0, 0, 32, 40]},
       "plain": {"cell": [0, 1], "rect": [0, 40, 64, 64]},
       "strap": {"cell": [1, 1], "rect": [92, 0, 6, 38]}
     }
   }
   ```

   Each region is an atlas rectangle `[x, y, width, height]` in pixels and the master `cell` `[column, row]` reduced into it. A region marked `"sprite": true` takes its cell's whole object instead of a panel: its silhouette keeps its proportions, centred in the rectangle, and the atlas stays transparent around it. A sprite item is a one-cell layout whose atlas is the item texture, for example `"grid": [1, 1], "ground": "#ff00ff", "atlas": "item.png", "size": [64, 64], "colors": 16` with one sprite region `[4, 4, 56, 56]`, and its hand-written item model as the `project` source. Sprites in one layout share one scale, so a family drawn together on one master, such as four metal plates, keeps its common shape and size. Regions may share a cell but never overlap. An inset has no rectangle: it names an earlier panel of its image (`"in": "door"`), that panel is drawn with one opening of plain ground, and the inset's cell is reduced into the opening wherever the drawing put it. A panel enclosing ground with no inset to fill it is rejected; ground reaching in round its edge, as at rounded or stepped corners, is outside it. An object panel (`"on": "mortar"`) is one whole object painted with its own light and shade, such as a single stone: the sheet asks for it in the proportions of its pixel size, the atlas keeps its outline, reducing it premultiplied like a sprite but filling its rectangle, and sets it on the named panel's material wherever the ground showed, so each rounded stone fills its rectangle on mortar. A layout may fill several images: declare `"images": {"front.png": [64, 64], "front_on.png": [64, 64]}` in place of `atlas` and `size` and give each region its `image`. A block's faces are separate images drawn together on one master and sharing one palette; a cold and a lit face take the same door panel and differ only by the windows filling its opening. Give a material one region large enough for every face that takes it, so faces take parts of one reduction rather than each squashing the whole panel. `colors` sizes the shared palette. The `ground` must contrast with every panel: mid gray suits leather and timber, but gray steel or cast iron would merge into it, so give those a saturated key such as `#ff00ff`.
3. **Master.** `texture --prompt texture-prompt.txt` attaches the declared references with their roles and sends the layout's sheet contract — the grid and ground, one flat unlit panel (or lit object panel) per used cell inside a plain margin, and the pixel sizes each cell is reduced to — followed by your prompt, which describes each panel by row and column: material, seams, stitches, fasteners and wear, drawn bold enough to survive those sizes. The image is recorded as `master-NN.png` and becomes `texture.master`; `--image` records a supplied master with the prompt that made it. To go back to an earlier master, set `texture.master` to its file.
4. **Atlas.** The same command, and every `prepare`, reduces the master. It finds the subjects by their edge against the ground, as connected regions given to the cell holding each region's centre, so a panel drawn a little past its cell stays whole; it fails when a used cell has none, when a subject is cut by the master's edge or fills under half its cell. A panel is cropped inside its antialiased rim and area-averaged into each of its regions; a sprite drops the rim blended with the ground and reduces premultiplied to binary alpha. One palette taken from the visible result applies without dithering, and unused space in an image of panels takes the mean colour. Inspect `review/texture.png`: the subjects (red) and openings (cyan) found on the master, the labelled images, and a lineup of the images beside the declared neighbours at native and triple size on dark and light grounds. Judge family fit in that lineup, and measure the greys: one whose blue sits above its green reads lavender and turns pink under Photon's warm daylight. After a layout change, `texture` without `--prompt` rebuilds the atlas. Every `prepare` rebuilds it, so fix a weak panel with a new master or layout, not by painting the atlas.
5. **Model.** The recipe reads `texture.regions` from `asset.json`, maps each face to a part of its region at four texels per unit and embeds the atlas in the native project; `studio.uv_faces` and `studio.create` take the same region shape. Declare the atlas as a PNG output: the prepare receipt and the reviews then cover master, layout and atlas, so the asset needs no texture receipt of its own. A hand-edited project instead loads the atlas as its texture, reloads it after a new master and declares it a source.

The local Backpack asset (`art/assets/backpack`) shows an item recipe's geometry, face rectangles inside shared regions, and display transforms; its recipe predates the shared reduction and still reduces its master itself.

### Isometric block plans

For multiblocks made mostly of ordinary and saw-cut blocks, [isometric_plan.py](isometric_plan.py) draws plans offline. A layout file defines `PLAN = Plan("name")` and `PLAN.box(material, x, y, z, w=1, h=1, d=1)` calls on the half-block grid (full blocks, slabs, quarter beams and eighth cubes); off-grid, boundary-crossing and overlapping pieces are rejected.

```powershell
python tools/modeling/isometric_plan.py tools/modeling/examples/isometric_example.py .tools/isometric-example
```

It writes a front/rear isometric sheet, individual views and a labelled half-block layer plan; colours identify materials, not final textures. Keep a machine's layout source with its asset record and review the PNGs before adopting dimensions or material counts. [export_oil_derrick.py](export_oil_derrick.py) shows converting a half-grid plan into saw-piece orientations and mixed-cell masks; [export_derrick_sections.py](export_derrick_sections.py) splits the derrick into repairable sections with matching collision.

## Modelling craft

### Make the model easy to judge

- Measure the concept's silhouette before shaping: its width at landmark heights as a fraction of its base. Build the main body from one profile fitted to those ratios and lay everything on it; a body assembled from separate primitives, such as a cylinder under a dome, reads wrong however good its details.
- Generated or supplied multi-view concepts can disagree with themselves. Compare the same landmarks across views and resolve differences in one measured native assembly. A perspective image is not a blueprint; treat ambiguous hidden geometry as a design problem.
- Record a few measured landmarks in the brief: overall envelope, primary mass ratios, shaft axes, contact heights and rotating clearances. For carriers, measure usable interior separately from the outer envelope and check wheel contact, resting supports and grip reach before proposing capacity.
- Block out the **whole assembly** in one neutral material before detailing anything, and check front, back, both sides and two three-quarter views at one scale. Keep the known-good runtime asset until its replacement passes review.
- Judge small equipment beside a complete standing player in a real Minecraft view; close-ups and editor dummies hide poor scale. When resizing, update geometry, pivots, UV spans, wheel radius and handler/collision offsets together, and test diagonal headings as well as cardinal ones.
- Inspect load-bearing supports from inside an open carrier as well as underneath, and include whole orthographic views alongside close-ups.
- Use close views for joints and normal gameplay distance for readability; a high-resolution close-up flatters an over-detailed model.

`studio.capture` renders with Blockbench's own offscreen preview at a fixed resolution, target and orthographic span, without touching the user's editor; each PNG gets a `.view.json` with its camera and hash. `studio.contact_sheet` lays out real captures. For an existing project, capture all six views without changing it (`span` is the visible width in model units; reuse target and span for before/after):

```powershell
python tools/modeling/review_model.py art/assets/oil_engine/studio/model/assembly.bbmodel --output .tools/modeling/engine-review --target 5 20 24 --span 75
```

### Shape, then function, then surface

1. Primary shapes: masses, supports, negative space, an unmistakable silhouette.
2. Functional secondary shapes: flanges, bearing collars, lids, openings, controls, feet — each explaining assembly or operation.
3. Surface: material variation, seams and fasteners that do not change the silhouette.

Fix a weak silhouette with geometry and unreadable materials with value, contrast and UVs; more cubes or random bolts substitute for neither. Keep brass on fittings and use steel highlights to articulate dark castings. Detail must describe construction: access covers get seams and fasteners, cast supports get quiet variation and real mounting hardware, and feet do not get panel borders by default.

### Motion

- Every moving part has a named pivot in **assembly coordinates**, with export offsets derived from the same source. Inspect the full swept shape, distinguish intended shaft contact from interference, and remember that an animation bone is not a Sable physics joint.
- Check exported-clip playback, not just static poses. The pinned MCP animation tool adds an `animation.` prefix itself. GeckoLib export converts rotation conventions, so verify the visible direction of asymmetric motion. Sample a spoked wheel between its symmetry angles. Exclude temporary player or scale geometry from the saved source.
- A non-living GeckoLib entity gets zero body yaw from the default renderer: apply interpolated yaw and test against travel heading. Apply rider steering after the player tick to avoid a stepped camera, and check first and both third-person views. Non-solid handles stay out of the physical shape and bounding box; extend only the mouse-ray target, and check grabbing and walking across them at several angles.

### Native format constraints

- Static Java 1.21.1 models allow cuboids with limited single-axis rotation; never design freeform meshes and expect them to survive export. GeckoLib animates cuboids but static item and guide geometry must stay compatible.
- `studio.Model` offers named boxes and X/Z octagonal casting approximations built from overlapping legal cuboids, with cap planes offset slightly to avoid flicker. It is not a mesh boolean; inspect seams under animation and Minecraft lighting, and simplify if the approximation gets expensive.
- GeckoLib cubes may turn about all three axes: give `box` an angle triple, applied about x, then y, then z round the pivot, to tilt a part that also turns, such as a dome's stones following its curve.
- A machine model names its build sections in its groups (`section_<n>_…`), and `machine_cells` derives each section's collision cells from its parts, each rising through its cell from the section's lowest foot to its highest top there, so a leaning body steps back only at whole cells and players climb it by jumping, never by walking up its courses. Give rough surfaces a hidden solid core and mark the visible detail on it `collide=False`, so collision follows the mass rather than every stone; give a face `None` where it lies against the core, and it is left out of the model and never drawn. `claim` gives the controller the solid core inside its own cell, which the placed controller draws before any section exists. The Kiln recipe (`art/assets/kiln`) shows a body laid in courses up one profile: mortar beds, painted stones and cores. The game draws a machine's still bones with the terrain, each cube from the nearest cell its section fills, so its texture is listed in the block atlas (`assets/minecraft/atlases/blocks.json`); only bones its animation moves are drawn every frame.

### Items

- A 3D item model authors its `display` transforms in the recipe for `gui`, `ground`, `fixed`, both `thirdperson_*` and both `firstperson_*` hands, plus `head` when worn there; write the left-hand slots explicitly. Thin placeable models get them too: review an oblique centred GUI icon and both hand views before changing world proportions or making a new sprite.
- The third-person hand layer turns an item to point forward from the hand. An object carried by a handle, such as a pack, lantern or bucket, needs roughly `[90, 180, 0]` to stand upright and hang below the grip, front out; the first-person view shows the front, small enough to keep the crosshair clear. The Backpack's values (third person: rotation `[90, 180, 0]`, translation `[0, 1, -3]`, scale 0.5; first person: rotation `[0, -45, 0]`, translation `[1, 3, 0]`, scale 0.35) are a starting point.
- Set the transforms in Blockbench's display mode with the player reference (MCP `enter_display_mode` with `"reference": "player"`, then `capture_screenshot`), then confirm them in the Minecraft scene, which is the acceptance.
- A worn item's renderer fixes which face touches the body; the Backpack's north face sits on the wearer's back. Keep that face the contact side in the geometry, and check the worn views from behind, three-quarter and side, over a chestplate, and as another player sees them.
- The item's visual scene shows the inventory and hotbar icon, both first-person hands, third-person front and back, the dropped and framed item and, for a worn item, the worn views. A new item usually needs those views added to its scene ([testing](../../civilization-mod/testing.md#visual-scenes)). Pass the screenshots straight to `review --evidence`.

### UVs and density

- The design's 64px-per-block rule means **four texture pixels per model unit**; retained 32px families use two until migrated. Pass `density=4` to `studio.create` for new assets. `art.py` checks real per-face UV spans with one-pixel rounding; `decals` names fit exceptions as `model.bbmodel:element_name/face`, and legacy or UI exceptions need a `density_reason`. A larger atlas is fine but does not by itself raise effective detail.
- `studio.uv_faces` rejects a face that would overflow its material region instead of stretching it; enlarge the region or split the surface. A face takes its region's corner by default, `(material, x, y)` an offset island so neighbouring pieces show different parts of one panel, and `(material, x, y, 90)` the island turned a quarter so grain painted along the panel follows an upright piece.
- Explicit `fit` regions are for authored decals such as a gauge, holding only that decal, with per-face overrides so it does not wrap around a housing.
- Small static Java block models may be declared in `models`. The checker resolves aliases and measures every face on a texture the asset publishes; a model without elements is a parent template such as `cube` or `orientable`, whose textures must be whole 64px faces. Pack and other assets' materials are theirs to measure; `material_textures` may map one to a locally kept copy listed in `sources` to measure it too. Referencing a pack material never authorises shipping its pixels.
- Start from the small original [material catalog](materials.json) before making new materials, and include chosen sources in the asset's references so edits invalidate reviews. Copy crops through the existing reducers with provenance; never copy Faithful pixels. Keep object-specific seams, fittings and wear separate from base material. Small raster sprites follow the sprite guidance in the local `art/textures/`.

### Surfaces for machines

- Build prominent cast frames, door surrounds, hinges, latches and supports with real depth; recessed panels, raised rims and prominent fastener heads are geometry when their depth should show, and painted marks are secondary.
- Give exposed panels face-sized UV islands at the declared density, using nine-slice layouts to keep corners while fitting the middle, with fitted seams, fastener seats, cast-edge highlights and localised handling or heat wear. Reused material tiles are the base layer; one tile mapped over a whole machine is unfinished. Paint a broad metal face from one continuous master crop at its measured size rather than repeating a block-sized tile. Keep quiet areas between details and secondary faces calmer.
- Prevent overlap in the source: adjacent panels and posts meet at boundaries, raised fittings sit clearly ahead of their backing, buried duplicate faces go, and support posts stop under a cap rather than sharing its top plane. Brighter trim cannot hide z-fighting. Keep authored islands disjoint and padded with paint contained inside. Thin frames get one coherent treatment, not alternating bright pixels.
- Closed doors meet a modelled seat or rebate; fixed firebox surrounds close incidental gaps while deliberate vents stay open; windows show the chamber; controls visibly connect to their mechanism. `studio/surface_textures.py` in the oil engine's asset folder demonstrates the approach.
- Compare the result with the target and ask what became flatter, smoother or disappeared; restore useful depth unless simplification was asked for. Broad exposed faces that read as plain material fill mean the build is unfinished, whatever the technical checks say.
- Keep materials honest to the recipe: a dark band standing in for a concept's iron hoops reads as metal on an all-stone machine, so build it from the machine's own material instead.

### Assembled surfaces

Walls, roofs and floors made of discrete pieces — stones, bricks, planks, shingles — are where a crop of a tiling texture fails: it cannot show where one piece ends, so the surface reads as a patchwork of rectangles however the geometry is rounded.

- Paint the pieces whole: a catalogue of object panels, one per piece, each sized at the declared density to the face it will cover and drawn as the concept draws its pieces, with its own rim light, shade and outline. A few sizes in each colour cover a whole machine.
- Lay the model from the catalogue: pick sizes that fit each course, share the leftover length out as joints, and never set a piece beside one of its own colour. Give each course a bed that fills it edge to edge, such as mortar, and set the pieces on it, so the surface is completely solid; a joint that shows a dark core reads as a hole. Where the surface leans in, lay the bed in strips each as long as the course at its own foot so no wedge opens.
- Match the concept's piece size to the whole object by counting pieces across a landmark; small pieces in even courses read as brickwork. On a curve a flat piece of width w stands off at its ends by about w²/8r, so keep w under √(6r) for no more than three quarters of a unit.
- Build narrow places beside openings from squared quoins rather than squeezed rubble.
- Recipes lay brick and fieldstone through [masonry.py](masonry.py): mortar masses carrying the collision, faced in running bond from a catalogue named `<kind>_<tone>`, cut to fit round openings and arches, capped with bricks laid flat, and stained by place (soot, sulfur) where the catalogue paints a stained piece.

### Creatures

Work within Minecraft's cuboid language and GeckoLib; no reconstruction service is needed.

1. **Measure the character**: height, head/body ratio, shoulder-to-waist width, limb lengths, hands, feet and signature features, recorded in the brief. Give references separate roles for anatomy, expression, palette and surface detail.
2. **Build the whole body in clay** with connected primary masses, purposeful tapers and joint pivots; check front, side, back and three-quarter views at one scale and fix gaps, bulky transitions and the resting expression here. Circular layers of small cubes rarely read well.
3. **Finish one representative section** — for a humanoid, the torso, shoulder and one complete arm — in clay and painted pixel art, resolving the joint transitions before extending. Keep parts that already work.
4. **Split the jobs**: cuboids carry the silhouette, joint structure, useful fingers or claws and prominent signature plates; paint carries small scales, anatomical shading and cloth folds in deliberate clusters with quiet regions. Keep the concept's broad colour regions. A few connected cloth panels with painted folds read better than many raised strips; machine panel borders do not belong on skin.
5. **Extend, then inspect the whole model** from all sides in clay and colour and at gameplay distance, with one matching-camera before/after comparison, close joint views and sampled animation cycles. Fix specific defects before adding detail. Cube count is a cost, not a target.
6. **Save and verify** one authoritative `.bbmodel` (with the recipe kept as reconciled provenance), padded islands at the declared density and validated exports, reopened and compared. Native acceptance is not in-game acceptance.

### Efficient native loop

Keep each asset's geometry recipe in its own folder and share only genuinely repeated operations in `studio.py`; a full rebuild should take seconds in one MCP session, not hundreds of conversational cube calls. Reconcile manual edits into the recipe before rebuilding. Use `create` to build, `save` for native exports and `capture` for evidence; scripts choose explicitly what they overwrite. Create a project before `risky_eval` when the editor has no undo context, since the pinned MCP eval wraps calls in an undo edit; when closing a generated tab, reselect the retained project before the call returns. The pinned eval rejects comment markers even inside embedded strings. Close only known generated tabs after saving their content, and never leave disposable studies for the user to dismiss.

For visual-only work, run the relevant hidden Minecraft scene once the editor model is convincing, not for every dimensional tweak. Review real screenshots, items, guides and working/stopped states, and record what remains unverified instead of generic passing claims.

## Review

A review answers four questions — **character, materials, readability, function** — with specific observed findings, blocking `defects` and optional `limits`. A pass has no blocking defects. The build review covers native evidence; the verify review covers the relevant Minecraft views and checks, scaled to the asset: inventory and held views for items, motion, clearances, collision, guides and sampled performance for machines, Sable placement only for assets claimed to work aboard vessels. Technical validation never certifies visual quality.

## Sources

- [Blockbench Minecraft style guide](https://blockbench.net/wiki/guides/minecraft-style-guide/): economical shapes and consistent pixel density.
- [Blockbench Preview API](https://web.blockbench.net/docs/classes/generated_preview_preview.Preview.html): offscreen previews and cleanup; the capture helper was checked against the pinned editor.
