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

The implemented Oil Derrick demonstrates a minimal export path: [export_oil_derrick.py](export_oil_derrick.py) validates its [half-grid source](../../concept_art/extraction_sieges/oil_derrick_plan.py), derives saw-piece orientations and mixed-material occupancy masks, then writes [OilDerrickStructure.java](../../civilization-mod/src/main/java/dev/civilization/OilDerrickStructure.java). Re-run the exporter after editing that plan. Keep moving parts and fluids out of the required-block list; render them separately from machine state. A new multiblock can reuse the conversion logic without building another studio or editor.

For shaped or animated assets, use the [native modeling practice](MODELING.md): whole-assembly blockouts, measured landmarks, fixed offscreen cameras, explicit UV density and functional detail. `studio.py` supplies reusable native construction/capture helpers; asset-specific geometry stays with its source files.

This section covers native model export. The complete process for all asset types is below; do not begin implementation without its brief/mockup review.

1. Model a silhouette in the correct GeckoLib format with named groups/pivots. Preserve existing project tabs. Review front, side and isometric views before detailing.
2. Use deliberate per-face UVs and approximately 32 texels per block-sized surface; whole machines may have larger atlases. Preserve the [reference-driven material workflow](../../art/textures/README.md#block-families-required-workflow).
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

The [project skill](../../.agents/skills/civilization-art/SKILL.md) owns autonomous visual review behavior. `pipeline.py` supplies briefs, requests, immutable mockup iterations, evidence-linked review records and export validation/publishing. Image generation, visual judgment and gameplay implementation remain agent work using their real tools; this CLI does not call an image API or pretend to assess aesthetics. No routine user confirmation is required between stages.

Start from the [approved visual direction](../../design_direction.md#setting-and-material-style). New briefs name role, technological stage, material behavior, signature feature and neighboring assets. Declare subject/shape, material and style reference roles separately; acquire missing evidence where useful rather than making an unrelated style image define the substance.

Use one `art/assets/<name>/asset.json` per asset or coherent family. Existing texture/model manifests remain authoritative for their exports; do not migrate old art merely to fit this folder. The [Oil Engine brief](../../art/assets/oil_engine/asset.json) records the fresh design, generated iterations, critiques, native models and runtime outputs. The earlier A/B concepts are superseded.

### Routes

| Asset | Build route | Required review evidence |
| --- | --- | --- |
| Item sprite | High-resolution, native-size-designed master ([prompt rules](../../art/textures/sprite-design.md)) → alpha-safe exporter at the asset's declared size (existing 32×32 families remain until the 64×64 migration) → native generated-item JSON | Native-size and enlarged icon; background/edge clarity; inventory view |
| Shaped item or single block | Blockbench Java Block/Item export, referenced material family; use GeckoLib only if animation needs it | Model front/side/back, UVs, inventory/held views as relevant, in-world scale |
| Multiblock | One assembly brief plus named component sources/outputs; static pieces use Java models, animated pieces use GeckoLib | Full assembly and component map, build-guide match, ports/collision, consistent materials across parts |
| Animated machine | Static shell plus named moving groups, native GeckoLib geometry/animation/atlas | Pivots, mechanical axes and clearances over the whole cycle; stopped/startup/running states |
| UI assets | Existing cabinet/family and native Minecraft controls; reuse shared frame/slot parts | Actual supported GUI scales and interaction layout; use explicit output dimensions, not the world-texture resolution |

Java 1.21.1 export checks cover cuboid bounds, supported single-axis rotation angles and UV ranges. GeckoLib checks reuse `assets.py` for hierarchy, animation references and atlas geometry. PNG checks enforce declared dimensions and alpha requirements. The validators do not prove Minecraft resource inheritance, gameplay integration, collision or artistic quality; verify those during model/in-game review.

### Commands and records

Before detail work, put a few named visual landmarks/proportions in `brief.fidelity_targets` (for example barrel length/diameter, wheel/body ratio, support height and signature fittings). Compare the mockup against the **complete** native assembly at similar camera angles and object scale. Repeat after texturing; the model review's `matches_direction` finding should address those targets explicitly. Technical validity alone does not establish fidelity. Use actual existing block materials in assembly previews: a neutral placeholder can conceal an oversized bright copper part. If pack materials are unavailable, treat that appearance as unverified until Minecraft review.

`compare.py` creates a side-by-side evidence sheet with source hashes and explicit crop bounds, without modifying inputs. It does not infer cameras or score artwork:

```powershell
python tools/modeling/compare.py art/assets/example/comparison.png art/assets/example/mockup-01.png "art/assets/example/model-view.png|100,40,900,800" --labels Mockup Model
```

Translate detail in three passes: large silhouette/masses, functional secondary shapes (flanges, lids, bearings, feet), then surface marks. Match meaningful detail placement instead of increasing random rivets or cube counts. Use geometry where it changes silhouette or depth; UV-map panels/bolts deliberately and keep noise off broad castings. A low-resolution texture cannot recover missing geometry.

```powershell
python tools/modeling/pipeline.py init art/assets/example --kind block --description "Describe what this block should be"
# Fill purpose, silhouette, bounds, materials, interfaces, states, construction, invariants and existing visual references in asset.json.
python tools/modeling/pipeline.py request art/assets/example
# Inspect reference images, call built-in image_gen, save the exact prompt actually used.
python tools/modeling/pipeline.py record art/assets/example --image PATH_TO_GENERATED_PNG --prompt EXACT_PROMPT_FILE --references ACTUAL_REFERENCE_IMAGES
# View the resulting image. Record specific findings for every criterion, then pass or revise.
python tools/modeling/pipeline.py review art/assets/example --stage mockup --review FINDINGS_JSON --evidence mockup-01.png
# If revise: edit the current image with fixed invariants, record the new generation, inspect again.
python tools/modeling/export_project.py art/assets/example/model --name example
# Add editable/master sources and runtime output mappings to asset.json. Review actual exported model/textures.
python tools/modeling/pipeline.py review art/assets/example --stage model --review FINDINGS_JSON --evidence model-review.png
python tools/modeling/pipeline.py publish art/assets/example
# Implement/update registrations, models, ports, guides, renderer and recipes only where authorized; run the focused scene.
python tools/modeling/pipeline.py review art/assets/example --stage ingame --review FINDINGS_JSON --evidence ingame-review.png
python tools/modeling/pipeline.py check art/assets/example --complete
```

`export_project.py` supports Java Block/Item and GeckoLib projects through native codecs, preserves `.bbmodel`, exports textures, and refuses existing files unless `--replace` is explicit. Check native texture resource names before declaring outputs; it does not silently rewrite references or invent registrations. Use the existing item/family texture tools for downscaling instead of another reducer. Multi-face generation always references the inspected master and changes only needed surfaces. Reuse equivalent faces.

`asset.json` output entries contain `source` (relative local file), `target` (relative to `assets/civilization`), and `format`: `png`, `java_model`, `json`, `gecko_geometry`, or `gecko_animation`. PNG entries also declare `size: [width,height]` and optionally `alpha: opaque|transparent`. `sources` lists editable projects/masters; `gecko_manifests` lists existing-format manifests checked by `assets.py`. A multiblock lists all component outputs together so they are validated before publishing. Use `json` for blockstates/metadata, not a way to bypass model validation.

For simple items, use one `review.json` containing `mockup`, `model` and `ingame` entries, each in the following format; the CLI selects the entry matching `--stage`. Separate legacy review files remain supported. Keep provenance and hash checks, but scale written evidence to complexity. Compare the asset with its named neighbors in a contact sheet or in-game lineup and explicitly judge material identity, family fit, visual distinction, scale, contrast, saturation and detail density. Machines still require mechanical and assembly evidence.

Review JSON contains `decision: pass|revise`, `criteria` (specific findings keyed by the stage's names), and `remaining_issues` (blocking defects; must be empty to pass). Explain non-applicable checks instead of omitting them. Stage criteria are exposed in `pipeline.py`; generation requests return mockup criteria. Evidence paths are relative to the asset directory. Review records bind to the brief, references, source/export hashes and screenshots. Changing inputs invalidates affected reviews; a later revision invalidates a prior passing mockup. Publishing requires current mockup and model reviews; `check --complete` additionally requires in-game evidence. These are self-review records, not claims that code or a second reviewer independently judged the image.

Finish only when the asset is readable at its actual use scale, mechanically/buildably coherent, fits the Tesla-era family, and has no blocking visual defects. Iterate autonomously; a fixed number of generations is not a quality target. Preserve failed iterations and targeted findings without making another roadmap. Update only affected art/design/runtime documentation and follow the focused testing policy.

GeckoLib runtime is pinned to 4.9.3 in `civilization-mod/gradle.properties`. For offline development, the build accepts the exact-version JAR at `civilization-mod/.dev-libs/geckolib-neoforge-1.21.1-4.9.3.jar`; otherwise it resolves the normal Maven dependency. Deployment copies the resolved artifact and verifies its hash. This runtime update does not change the Blockbench editor plugin pin.
