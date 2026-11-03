# Model authoring toolset

Civilization models are made in Blockbench, driven through Jason Gardner's MCP plugin, with the GeckoLib editor plugin for animated assets; GeckoLib 4 is the runtime library (version in `civilization-mod/gradle.properties`). [pins.json](pins.json) pins the authoring downloads, commits and checksums. The visual rules live in [design](../../design_direction.md#setting-and-material-style); the workflow and quality bar live in the [civilization-art skill](../../.claude/skills/civilization-art/SKILL.md). This file covers setup, commands and modelling craft.

## Setup

From the repository root:

```powershell
python tools/modeling/setup.py
.\tools\modeling\start.ps1
```

The portable editor, plugins, a separate profile and the pinned [Codex CLI](https://github.com/openai/codex) live in ignored `.tools/modeling`. Codex draws concept images on the owner's ChatGPT plan and shares the Codex app's sign-in; without the app, run `.tools\modeling\codex\codex-x86_64-pc-windows-msvc.exe login` once. In Blockbench use **File → Plugins → Load Plugin from File** for `.tools/modeling/mcp.js` and `.tools/modeling/geckolib.js`, permit MCP network access and leave the editor open. The launcher reuses a ready editor, waits up to 45 seconds for MCP to initialise and never opens a duplicate of this profile; new launches log to `.tools/modeling/startup.log`. It disables Chromium's GPU sandbox, whose GPU process cannot start when the editor is launched from the Claude desktop app. Save projects before restarting an unresponsive editor. Rerun setup to verify the pinned downloads.

Setup patches the pinned MCP plugin to bind `127.0.0.1` instead of all interfaces and records the original and patched hashes. It remains [Jason Gardner's plugin](https://github.com/jasonjgardner/blockbench-mcp-plugin/tree/b187b4b056f0efafcc573335400ecbb21ad26ecc) under its upstream license; authoring tools never go into the Minecraft JAR.

The repository's `.mcp.json` registers the server (`http://127.0.0.1:3000/bb-mcp`) for Claude Code; a session started before the editor was running may need a restart to see the tools. The direct client works regardless:

```powershell
python tools/modeling/mcp.py list
python tools/modeling/mcp.py call get_project_info
python tools/modeling/mcp.py call capture_screenshot --out .tools/modeling/view
python tools/modeling/mcp.py call place_cube .tools/modeling/cube-arguments.json
```

Read returned tool schemas before using a new operation. `--out` saves responses and extracted images; console output omits image bytes. The client handles initialisation, sessions, JSON/SSE and tool errors.

## Asset records and commands

Each asset has one compact schema-2 `asset.json`, managed by `art.py`, in its own folder under the local `art/assets/`. Its brief has three short fields: `purpose`, `target` (recognisable features and material treatment) and `constraints` (bounds, interfaces, motion, construction and gameplay invariants). Name inspected references by role — shape, material or project style. Choose one authoritative `source`: a hand-edited Blockbench `project` or a Python `recipe`. A recipe owns its generated models, a project owns its derived exports, and a recipe never silently overwrites manual edits. Declare extra source dependencies, native `models`, output mappings and GeckoLib manifests. Use `--kind entity` for character models.

```powershell
python tools/modeling/art.py init art/assets/example --kind block
# Fill asset.json; author the source and declare output paths.
python tools/modeling/art.py materials
python tools/modeling/art.py prepare art/assets/example
python tools/modeling/art.py views art/assets/example --model model.bbmodel --target 8 8 8 --span 24
# Inspect the images; review.json holds decision, criteria (the four questions), defects and optional limits.
python tools/modeling/art.py review art/assets/example --stage build --review art/assets/example/review.json --evidence review/views.png
python tools/modeling/art.py preview art/assets/example
# Run the relevant civilization-mod/dev.ps1 visual scene and inspect the real screenshots.
python tools/modeling/art.py review art/assets/example --stage verify --review art/assets/example/review.json --evidence review/ingame.png
python tools/modeling/art.py publish art/assets/example
python tools/modeling/art.py check art/assets/example --complete
```

- `prepare` runs the recipe or exports the saved project (never an arbitrary open tab) to `export/`, then checks the whole bundle and UV density. Large native exports compile once in Blockbench and transfer in bounded text chunks; incomplete transfers are rejected.
- `views` captures native Blockbench views; `compare --images` maintains the single reference-versus-actual comparison sheet; `record --prompt prompt.txt` generates a concept with Codex's image tool, passing any `--references` images to it, or records a supplied concept given `--image`; either way the asset keeps the concept with its exact prompt and references.
- `preview` needs a current build review and copies validated candidates into runtime resources for the focused scene, keeping replaced files in local content-addressed history (`preview-backup.json`). `publish` needs a current verify review; `check --complete` confirms fresh review hashes, exports and the runtime receipt. Source, export and evidence hashes are recorded automatically and history is immutable.
- A model-only deliverable without runtime targets validates its paired manifest with `assets.validate` and `art.definition`/`art.uv_density` from its own verification script; it is not "published" until it passes a Minecraft scene.
- Older schema-1 records keep `pipeline.py`; `assets.py` and the item/family reducers remain export backends. Preserve their history rather than migrating them.

Runtime GeckoLib files follow `geo/<id>.geo.json`, `animations/<id>.animation.json` and `textures/entity/<id>.png`; [MachineGeoModel](../../civilization-mod/src/main/java/dev/civilization/client/MachineGeoModel.java) resolves those paths and leaves machine-specific state explicit.

Native export details: `export_model` with codec `project` saves the source and `bedrock` invokes the GeckoLib geometry hooks; discover codecs rather than guessing. `Animator.buildFile(null, Animation.all.map(a => a.name))` exports animations where no codec is exposed.

Fast checks need no Minecraft launch: `python -m unittest discover -s tools/modeling -p "test_*.py"`.

### Isometric block plans

For multiblocks made mostly of ordinary and saw-cut blocks, [isometric_plan.py](isometric_plan.py) draws plans offline. A layout file defines `PLAN = Plan("name")` and `PLAN.box(material, x, y, z, w=1, h=1, d=1)` calls on the half-block grid (full blocks, slabs, quarter beams and eighth cubes); off-grid, boundary-crossing and overlapping pieces are rejected.

```powershell
python tools/modeling/isometric_plan.py tools/modeling/examples/isometric_example.py .tools/isometric-example
```

It writes a front/rear isometric sheet, individual views and a labelled half-block layer plan; colours identify materials, not final textures. Keep a machine's layout source with its asset record and review the PNGs before adopting dimensions or material counts. [export_oil_derrick.py](export_oil_derrick.py) shows converting a half-grid plan into saw-piece orientations and mixed-cell masks; [export_derrick_sections.py](export_derrick_sections.py) splits a native model into repairable sections with matching collision.

## Modelling craft

### Make the model easy to judge

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
- Thin placeable models need authored item display transforms: review an oblique centred GUI icon and both hand views before changing world proportions or making a new sprite.
- `studio.Model` offers named boxes and X/Z octagonal casting approximations built from overlapping legal cuboids, with cap planes offset slightly to avoid flicker. It is not a mesh boolean; inspect seams under animation and Minecraft lighting, and simplify if the approximation gets expensive.

### UVs and density

- The design's 64px-per-block rule means **four texture pixels per model unit**; retained 32px families use two until migrated. Pass `density=4` to `studio.create` for new assets. `art.py` checks real per-face UV spans with one-pixel rounding; `decals` names fit exceptions as `model.bbmodel:element_name/face`, and legacy or UI exceptions need a `density_reason`. A larger atlas is fine but does not by itself raise effective detail.
- `studio.uv_faces` rejects a face that would overflow its material region instead of stretching it; enlarge the region or split the surface.
- Explicit `fit` regions are for authored decals such as a gauge, holding only that decal, with per-face overrides so it does not wrap around a housing.
- Small static Java block models may be declared in `models`. `material_textures` maps each referenced resource ID to locally kept source PNGs listed in `sources`; the checker resolves aliases and real resolutions. Referencing a pack material never authorises shipping its pixels.
- Start from the small original [material catalog](materials.json) before making new materials, and include chosen sources in the asset's references so edits invalidate reviews. Copy crops through the existing reducers with provenance; never copy Faithful pixels. Keep object-specific seams, fittings and wear separate from base material. Small raster sprites follow the sprite guidance in the local `art/textures/`.

### Surfaces for machines

- Build prominent cast frames, door surrounds, hinges, latches and supports with real depth; recessed panels, raised rims and prominent fastener heads are geometry when their depth should show, and painted marks are secondary.
- Give exposed panels face-sized UV islands at the declared density, using nine-slice layouts to keep corners while fitting the middle, with fitted seams, fastener seats, cast-edge highlights and localised handling or heat wear. Reused material tiles are the base layer; one tile mapped over a whole machine is unfinished. Paint a broad metal face from one continuous master crop at its measured size rather than repeating a block-sized tile. Keep quiet areas between details and secondary faces calmer.
- Prevent overlap in the source: adjacent panels and posts meet at boundaries, raised fittings sit clearly ahead of their backing, buried duplicate faces go, and support posts stop under a cap rather than sharing its top plane. Brighter trim cannot hide z-fighting. Keep authored islands disjoint and padded with paint contained inside. Thin frames get one coherent treatment, not alternating bright pixels.
- Closed doors meet a modelled seat or rebate; fixed firebox surrounds close incidental gaps while deliberate vents stay open; windows show the chamber; controls visibly connect to their mechanism. `studio/surface_textures.py` in the oil engine's asset folder demonstrates the approach.
- Compare the result with the target and ask what became flatter, smoother or disappeared; restore useful depth unless simplification was asked for. Broad exposed faces that read as plain material fill mean the build is unfinished, whatever the technical checks say.

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
