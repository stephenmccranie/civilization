# Model authoring toolset

Selected tools: Blockbench, Jason Gardner's MCP plugin and the GeckoLib editor plugin. GeckoLib 4 is the approved runtime library. [Pins](pins.json) own authoring downloads, commits and checksums; Gradle owns the runtime version. [Status](../../docs/status.md) distinguishes completed validation from remaining vessel checks.

## Setup and connection

From the repository root:

```powershell
python tools/modeling/setup.py
.\tools\modeling\start.ps1
```

The portable editor, plugins and separate profile live in ignored `.tools/modeling`. In Blockbench use File → Plugins → Load Plugin from File for `.tools/modeling/mcp.js` and `.tools/modeling/geckolib.js`. Permit MCP network access. Leave the editor open during work. No debugging port is needed.

Setup applies one explicit patch to the pinned MCP artifact: bind to `127.0.0.1`, because upstream binds all interfaces. Its local receipt records original/patched hashes. This remains [Jason Gardner's plugin](https://github.com/jasonjgardner/blockbench-mcp-plugin/tree/b187b4b056f0efafcc573335400ecbb21ad26ecc), not our own modeling server. Retain its upstream license/provenance and do not put authoring tools in the Minecraft JAR.

The project `.codex/config.toml` declares `http://127.0.0.1:3000/bb-mcp` for future tasks. A running task may need to restart to discover native MCP tools. The direct HTTP client works without rediscovery:

```powershell
python tools/modeling/mcp.py list
python tools/modeling/mcp.py call get_project_info
python tools/modeling/mcp.py call capture_screenshot --out .tools/modeling/view
python tools/modeling/mcp.py call place_cube .tools/modeling/cube-arguments.json
```

Read returned tool schemas before use. `--out` saves responses and extracted images; console output omits image bytes. The client handles initialization, session IDs, JSON/SSE and tool errors.

## Reusable workflow

### Isometric block plans

Use [isometric_plan.py](isometric_plan.py) for multiblocks made mostly of ordinary and saw-cut blocks. It is a small offline drawing script, not a 3D editor. Write one short Python layout file containing `PLAN = Plan("name")` and `PLAN.box(material, x, y, z, w=1, h=1, d=1)` calls; loops are fine. Coordinates and dimensions use Minecraft blocks on the existing half-block grid. Supported pieces are full blocks, slabs, quarter beams and eighth cubes. The script rejects off-grid, boundary-crossing and overlapping pieces.

Run from the repository root:

```powershell
python tools/modeling/isometric_plan.py tools/modeling/examples/isometric_example.py .tools/isometric-example
```

The output is a large front/rear isometric sheet, individual view PNGs and a labeled lower/upper half-block layer plan. Isometric views draw every piece as a complete closed cuboid; layer diagrams are explicitly schematic slices for exact placement. Colors identify materials rather than pretending to be final textures; glass draws translucent and crude can illustrate visual-only operating states in a separate plan. [The example](examples/isometric_example.py) is a geometry proof, not an accepted Oil Pump design. Keep each proposed machine's source layout with its concept/asset record and review the actual PNGs before adopting its dimensions or material counts. Blockbench remains appropriate for custom shaped or animated components after the block-built frame is planned. Neither Blockbench nor Minecraft is needed to make these block plans.

The retired real-block Oil Derrick provides a small conversion example: [export_oil_derrick.py](export_oil_derrick.py) validates its [historical half-grid plan](../../concept_art/extraction_sieges/oil_derrick_plan.py), derives saw-piece orientations and mixed-cell masks, and writes an ignored local study rather than runtime code. The current [Oil Derrick asset](../../art/assets/oil_derrick/asset.json) is a complete native Blockbench tower; its authoring recipe exports geometry, animation and physical collision from the same beam endpoints. [export_derrick_sections.py](export_derrick_sections.py) partitions the approved model into repairable visible sections and matching collision without changing its silhouette.

For shaped or animated assets, use the [native modeling practice](MODELING.md): whole-assembly blockouts, measured landmarks, fixed offscreen cameras, explicit UV density and functional detail. `studio.py` supplies reusable native construction/capture helpers; asset-specific geometry stays with its source files.

This section covers native model export. Use the shared workflow below and establish the visual target before authoring.

1. Model a silhouette in the correct GeckoLib format with named groups/pivots. Preserve existing project tabs. Review front, side and isometric views before detailing.
2. Use deliberate per-face UVs and 64 texels per block-sized surface for new assets (declared 32 for retained legacy art); whole machines may have larger atlases. Preserve the [reference-driven material workflow](../../art/textures/README.md#block-families-required-workflow).
3. Author animation clips and explicit operating states. Bones are visual transforms, not physical constraints. Server code still owns production/fuel/collision.
4. Save editable `.bbmodel`, native geometry/animation and PNG together in `art/models/<asset>/`. `export_model` codec `project` saves source; codec `bedrock` invokes the GeckoLib geometry hooks. Discover codecs rather than guessing. `Animator.buildFile(null, Animation.all.map(a => a.name))` supplies native animation export where no animation codec is exposed. Return content over MCP and save with local file tools if editor filesystem permission is unavailable.
5. Add a manifest with `id`, `project`, `geometry`, `animation`, `texture`, `required_animations`; source paths are relative and cannot escape the asset directory. Run `python tools/modeling/assets.py art/models/<asset>/manifest.json`; use `--check` to detect stale runtime files/receipts without writing. The exporter validates native files and copies them without interpreting `.bbmodel`.
6. Inspect the assembled model in Minecraft. Editor lighting cannot establish Photon appearance or Sable compatibility. Keep static shells as ordinary blocks where appropriate.

Runtime convention: `geo/<id>.geo.json`, `animations/<id>.animation.json`, `textures/entity/<id>.png`. [MachineGeoModel](../../civilization-mod/src/main/java/dev/civilization/client/MachineGeoModel.java) shares those paths, leaving machine-specific state/controllers explicit.

## First integration proof

```powershell
python tools/modeling/proof.py
python tools/modeling/assets.py art/models/engine_proof/manifest.json
python tools/modeling/assets.py art/models/engine_proof/manifest.json --check
cd civilization-mod
.\dev.ps1 Verify -Scope Visual -Scene models
```

The proof creates a diagnostic wheel/piston assembly through native editor APIs, a checker atlas, a cycle, exports and editor views. It refuses to overwrite an existing source. `engine_proof.js` is a fixture, not final engine art. Live creation/export/reopen and the Photon model scene are verified. Use `python tools/modeling/proof.py --verify` to reopen the existing source, compare geometry and animation exports, and refresh views without replacing the source. The pinned MCP eval rejects comment markers even inside embedded PNG strings; the fixture removes whole-line comments and JSON-escapes slashes for source reopening. Create a project before eval because the plugin initializes its undo context first.

[ModelProofRenderer](../../civilization-mod/src/gameTest/java/dev/civilization/ModelProofRenderer.java) demonstrates `GeoBlockRenderer`, an instance cache and state-driven speed. Only the `models` scene enables that diagnostic adapter. The production engine now has its own native wheel renderer; use `engine` for its actual art review.

Fast checks: `python tools/modeling/test_assets.py` covers missing textures, bad UVs, hierarchy/animation references, nonfinite geometry and path escapes. Rerun setup to verify pinned downloads. Ordinary texture edits do not require the full gameplay suite.

## Complete asset pipeline

The [project art skill](../../.agents/skills/civilization-art/SKILL.md) follows **Define → Build and refine → Verify and publish**. The [design direction](../../design_direction.md#setting-and-material-style) owns visual style; this section owns the workflow. New assets use `art.py` and one compact schema-2 `asset.json`. Existing schema-1 records and `pipeline.py` remain supported unchanged; preserve their sources and history rather than migrating them routinely.

### Define

Fill three short brief fields: `purpose`, `target` (recognizable features and intended material treatment), and `constraints` (bounds, interfaces, motion/construction and gameplay invariants). Name inspected references by role: shape, material or project style. Choose one authoritative `source`: a hand-edited `project` or a Python `recipe`. Declare extra source dependencies, native `models`, output mappings and any GeckoLib manifests. A recipe owns generated models; a project owns its derived exports. Never silently overwrite manual edits with a recipe.

Generate one coherent concept when the shape or direction needs exploration, using actual inspected image references and preserving the exact prompt. Routine repairs and established designs can use existing references directly. Competing concepts remain opt-in. Concepts are visual targets, not dimensioned engineering or finished textures.

### Build and refine

Block out the whole silhouette first. Finish a representative section containing the main material, a joint and a signature feature; inspect it on the actual model before extending it. For sprites, make the native-size export the proof. Use the small original [material catalog](materials.json) before generating new materials. It points to local reviewed source crops and evidence; include chosen source files in asset references so edits invalidate reviews. Copy crops into asset-owned textures through the existing reducers, retaining provenance. Keep object-specific seams, fittings and selective wear separate from base material. Extend the catalog only for a demonstrated need; do not copy Faithful pixels.

For machines, build prominent cast frames, door surrounds, hinges, latches and supports with actual depth. Author face-sized islands for exposed panels: fitted seams, fastener seats, cast-edge highlights and localized handling/heat wear. Reused material tiles supply the base, not the complete surface. Keep quiet areas between details; small hardware belongs in textures when it does not change silhouette. Prove this treatment on the representative section before applying it across the assembly. Do not substitute a global noise pass or a larger atlas for authored detail.

New world art uses 64 texels per block. `art.py` checks actual native per-face UV spans, allowing one-pixel rounding; `decals` explicitly names fit exceptions as `model.bbmodel:element_name/face`. Legacy density and UI exceptions need `density_reason`. A larger image alone does not establish effective detail. Mechanical axes, clearances, joints and native export constraints live in [modeling practice](MODELING.md); small sprites use [native-size guidance](../../art/textures/sprite-design.md).

Keep one current comparison sheet: reference, actual asset and the useful close-up or gameplay view. Use matching angles/scales via `compare.py`. Correct the visible cause: shape, missing functional detail, UV placement or material treatment. Inspect native pixels, actual model and gameplay distance. Preserve history automatically instead of maintaining multiple parallel review files.

Compare visible construction detail with the target: what became flatter, smoother or disappeared? Restore useful depth and accents unless the user requested simplification. Broad exposed machine faces that read as undifferentiated material fill are an unfinished build, even when geometry, UV density and motion checks pass.

### Verify and publish

A short review answers four questions: **character**, **materials**, **readability**, **function**. Record specific observed findings, blocking `defects` and optional `limits`. A pass requires no blocking defects. Build review covers the native proof; verify review covers the relevant real Minecraft views and checks. Review complexity follows the asset: sprites need inventory/held views as relevant; machines need motion, clearances, collision/guides and sampled performance where costly. Technical validation and image-generation success do not certify visual quality. Iterate autonomously, with no routine approval gates.

`prepare` runs a declared Python recipe or exports the saved Blockbench project to `export/`, then checks the entire output bundle and UV density. `views` reuses native Blockbench capture; `compare --images` maintains the current reference/actual comparison sheet; `record` preserves optional generation prompts/references. `preview` requires a current build review and copies validated candidates to runtime for the focused test scene, preserving replaced files in local content-addressed history. `publish` requires a current verify review; `check --complete` checks fresh review hashes, exports and runtime receipt. Original source/export/evidence hashes are automatic. History is local and immutable; preview backups are recorded in `preview-backup.json`. Only publish completed code/runtime resources under the project Git workflow.

```powershell
python tools/modeling/art.py init art/assets/example --kind block
# Fill asset.json; author the source and declare output paths.
python tools/modeling/art.py materials
python tools/modeling/art.py prepare art/assets/example
python tools/modeling/art.py views art/assets/example --model model.bbmodel --target 8 8 8 --span 24
# Inspect images; review.json has decision, criteria (four questions), defects and optional limits.
python tools/modeling/art.py review art/assets/example --stage build --review art/assets/example/review.json --evidence review/views.png
python tools/modeling/art.py preview art/assets/example
# Run the relevant civilization-mod/dev.ps1 Visual scene; inspect real screenshots.
python tools/modeling/art.py review art/assets/example --stage verify --review art/assets/example/review.json --evidence review/ingame.png
python tools/modeling/art.py publish art/assets/example
python tools/modeling/art.py check art/assets/example --complete
```

Output entries retain the existing `source`, `target`, `format`, PNG `size`/`alpha` and GeckoLib manifest validation. Existing item/family reducers and `assets.py` remain export backends. `prepare` for a saved project preserves its source and exports the explicitly selected project, never an arbitrary current tab. Native `views` opens review tabs; close only known generated tabs after saving authored work.

Native model creation and saved-project export transfer JSON in bounded string chunks before invoking the native project codec. This avoids sending detailed models as one giant JavaScript object literal through MCP; the Paterson model passed reopening and native geometry/animation comparison using this path.

Fast tool checks: `python -m unittest discover -s tools/modeling -p "test_art.py"` and the existing `test_pipeline.py`, `test_assets.py`, `test_studio.py`. No Minecraft launch is required for workflow/documentation changes. The baking-oven pilot reached native modeling; user review exposed insufficient surface detail despite the first build review passing. The authoring guidance above addresses that failure. The oven now has a [physical runtime preview](../../civilization-mod/cooking.md#baking-oven-physical-first-pass), checked in the dedicated hidden `oven` scene; baking remains deferred. Judge the workflow by actual art and defects the user still has to identify.

GeckoLib runtime is pinned to 4.9.3 in `civilization-mod/gradle.properties`. For offline development, the build accepts the exact-version JAR at `civilization-mod/.dev-libs/geckolib-neoforge-1.21.1-4.9.3.jar`; otherwise it resolves the normal Maven dependency. Deployment copies the resolved artifact and verifies its hash. This runtime update does not change the Blockbench editor plugin pin.
