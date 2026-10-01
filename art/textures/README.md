# Original texture artwork

New standard block faces and item sprites target 64×64, beginning with Street Pavers. Previously shipped 32×32 families stay at their documented native sizes until each is reviewed and migrated; assembled model atlases and UI keep their declared dimensions. High-resolution original generated masters are reduced by the pipeline; Faithful assets are never copied into the mod. `pipeline.json`, `block_families.json`, and newer per-asset `asset.json` manifests own their respective source/output mappings. Runtime textures live in `civilization-mod/src/main/resources/assets/civilization/textures`.

The item set includes food/preparations, fertilizer ingredients, coal and saws. Machines use coherent generated families; the kiln layers custom ports over the active resource pack’s cobblestone. Cut pieces crop their source material textures. Earlier rejected masters remain provenance, not current directions.

The prototype Rain Caller references `minecraft:item/heart_of_the_sea`; its resolution/style come from the active resource pack. Wet soil reuses the pack’s native farmland textures. Neither is newly generated artwork, and no Faithful pixels are bundled.

The [crude-oil asset record](../assets/crude_oil/asset.json) preserves both generated masters, the user-supplied material reference, exact prompts and reproducible exporter for the 64×64 animated fluid atlas. The surface seep and finite underground reservoir share near-black petroleum with subtle umber depth and sparse broken cool reflections. The exporter closes the master’s imperfect tile edges and makes eight slowly moving frames; [Photon evidence](../assets/crude_oil/evidence/photon-surface.png) checks the actual surface appearance.

The [equipment-grade asset record](../assets/equipment_grade/asset.json) owns 16 composable 32×32 wear variants for each of 69 damageable item identities. Its deterministic exporter samples the installed Faithful 32× pack to place connected marks on lit parts inside each item's silhouette; no pack color pixels are shipped. A saved random seed chooses a different combination for each item, while grade determines how many layers appear. The item model retains the pack art underneath and adds no marks at 100% or higher. Worn armor on the player model is outside this item-art pass.

Town workshop construction continues to use active resource-pack brick, cobblestone, oak, copper and cut-piece textures. The redesigned [Tannery](../assets/tannery/asset.json) and [Textile Workshop](../assets/textile_workshop/asset.json) now add original 32× controller fronts: brick/iron/copper/leather for tanning and oak/ivory/indigo/copper for textiles. Their complete asset records retain exact prompts, generated assembly directions, high-resolution controller masters, deterministic exporters and Photon/Faithful evidence. Raw Hide and Cloth retain their original item sprites. The Smithy retains its reviewed partial-block assembly and pack-aware hearth, with an original anvil-marked gate from the thermal controller family below.

All new asset work starts with the [complete asset pipeline](../../tools/modeling/README.md#complete-asset-pipeline) and project art skill: brief, one mockup direction, autonomous inspection/iteration, then building and actual model/in-game review. The texture workflows below remain the export implementations, not an alternative entry point.

The [Tier 2 kitchen concept family](../assets/t2_kitchen/asset.json) contains a [whole-room mockup](../assets/t2_kitchen/mockup-04.png) and [matching four-station sheet](../assets/t2_kitchen/mockup-03.png), generated with the built-in image tool and reviewed after correcting the range emblem and copper glow. Exact prompts, original source paths/hashes and visual findings are preserved together. These remain planning concepts for the full four-station kitchen. A separate [stove prototype](../../civilization-mod/cooking.md) now implements the first cooking position; full multiblock layouts remain unimplemented.

Machine Parts now use an original steel bearing-and-shaft sprite with a functional copper collar. The [asset record](../assets/machine_parts/asset.json) preserves the master, 32×32 export and contextual Photon review.

The [frontier uranium family](../assets/frontier_uranium/asset.json) adds a cast-steel/brass Geiger counter, subdued stone-hosted Uranium Ore and a raw mineral fragment. Exact prompts, original generated sheets, deterministic 32× exports, native-size review and a Faithful inventory screenshot are kept together. The Photon visual run stalled in Iris initialization; no Photon appearance claim is made for this family.

The shared uranium radiation effect has a [motion brief](../../concept_art/uranium_radiation/brief.md), [animated preview](../assets/uranium_tracks/animation-preview.gif) and reproducible [frame generator](../assets/uranium_tracks/export.py). The game draws the approved edge-originating streaks procedurally in [UraniumRadiation.java](../../civilization-mod/src/main/java/dev/civilization/client/UraniumRadiation.java); the study frames are not runtime textures.

The [prototype stove](../assets/prototype_stove/asset.json) uses quiet original cast-iron, clean seasoned-iron, steel and brass masters at 64px per block, with a declared 512×256 atlas and editable native Java Blockbench shell. Measured face islands place fitted housing seams, steel fasteners, a vent surround, control ticks and skillet-lip wear; supporting legs use longitudinal wear and attachment marks. Generic gritty fills from the previous pass were rejected after user review. A shallow burner head on an open steel stand replaces the former oven-like cabinet and door; four narrow ember vents identify the coal fire. Earlier rejected coarse/gritty masters and the current quiet material source are preserved with exact prompts, the shared palette exporter and explicit native accent recipes in the asset record. Its exposed food and rotary pointer render continuously in the game; reproducible original positional sound loops are generated by the asset build script. The [vegetable skillet portion](../assets/vegetable_skillet/asset.json) has an original transparent generated master, shared alpha-safe exporter and reviewed 64×64 inventory sprite. Exact built-in prompts, original sources and review evidence are preserved in those records.

## Frontier supply art pass

The [bulk-storage asset record](../assets/bulk_storage/asset.json) owns the Coal Bunker and Cargo Tank controllers, generated direction, editable Blockbench assemblies and review evidence. Shells reuse the original Industrial Casing and its cut forms; the shared atlas derives its quiet steel and functional fittings from that material. Fill height is rendered from actual stored quantities.

[Raw Hide](../assets/raw_hide/asset.json), [Mineral Blend](../assets/mineral_blend/asset.json), [Enriched Mineral Blend](../assets/enriched_mineral_blend/asset.json) and [Mineral Fertilizer](../assets/fertilizer/asset.json) now use original 32×32 sprites. The enriched blend is a referenced edit of the ordinary blend with visible sulfur inclusions. Fertilizer uses an open sack and green sprout stencil; hide has irregular lobes, mottling and a pale folded underside. Packaging is illustrative, with no new recipe ingredients. Exact prompts, sources, exports and reviews live beside each manifest.

These per-asset manifests own publication; fertilizer and mineral blend were removed from the legacy `pipeline.json` to prevent an old batch from overwriting them. Earlier source images remain provenance. [Family comparison](../assets/raw_hide/evidence/supplies-family.png) checks scale, material identity and density beside Cloth, sulfur and Machine Parts.

The initial icon audit identified food/preparation icons as more finely illustrated than the new family; the completed kitchen pass below replaces those sources. Coal remains readable and lower priority. Block/machine redesign requires separate assembly review, not simply applying the item palette everywhere. Work order lives in the development plan.

## Kitchen food family

Six original 32×32/16-color sprites now share the frontier material language: [bread dough](../assets/bread_dough/asset.json), [cookie dough](../assets/cookie_dough/asset.json), [cake batter](../assets/cake_batter/asset.json), [unbaked pie](../assets/unbaked_pie/asset.json), [field ration](../assets/field_ration/asset.json) and [foraged morsel](../assets/foraged_morsel/asset.json). Pale raw dough contrasts golden cooked bread; batter uses plain terracotta, pie a shallow tin, and the ration a simple cloth wrap. The morsel retains its woodland mushroom identity. Container/wrapping imagery adds no ingredients or returned items.

Each folder preserves the exact built-in image-generation prompt, master, exporter and review. Per-asset manifests now own these outputs; their legacy `pipeline.json` entries were retired to avoid overwrites, with earlier masters retained. [Family comparison](../assets/bread_dough/evidence/foods-family.png) includes native dark/light views beside Cloth and fertilizer. The `foods` Photon scene reviews all six together in inventory and frames.

## Manufactured items

[Steel Ingot](../assets/steel_ingot/asset.json) now has a substantial cast-bar sprite with cool blue-gray planes, bright edges and restrained scuffs. [Prospecting Rod](../assets/prospecting_rod/asset.json) replaces the borrowed brush with a wooden T-grip, iron socket and continuous pointed shaft. Both use original generated masters and 32×32 native sprites; exact prompts and reviews are beside the manifests. Recipes and tool behavior are unchanged.

The `manufactured` scene compares them with Machine Parts and vanilla iron/copper under Photon. The metalworking generator no longer writes retired steel/Machine Parts placeholder models. Industrial casing and cooling textures use the shared industrial steel asset described below.

## Generation contract

Use the built-in image generation tool for artwork, one asset per call. It exposes prompts and image references, not guaranteed dimensions, palette limits or a strict pixel grid. Asking for those constraints guides composition but does not enforce them. Do not switch to API/CLI generation merely to obtain size controls.

Generate high-resolution masters designed for the final icon’s information budget: large connected color clusters, a few broad light/shadow tones and landmarks wide enough to survive 32×32 reduction. Include readable material-specific clusters between the large shapes: sparse weave, directional grain or restrained metal wear as appropriate. Avoid subpixel weave, random speckling, disappearing accents and smooth illustration shading; do not erase all surface variation. The generator does not enforce an exact pixel grid; the reducer does. Follow [sprite design and measured comparisons](sprite-design.md), including native-size review before approving a master.

For block faces request square orthographic face artwork, fully opaque, edge-to-edge, without perspective or lighting effects. Treat seamless edges and matching active/inactive variants as separate visual acceptance requirements. The pipeline supports opaque block export but does not magically make textures tile or align a machine's faces.

## Legacy block-family workflow

The following kiln examples document the original workflow. Current kiln, stove and fertilizer exports are owned by `art/assets/thermal_controllers`; legacy publication is disabled. Use the asset publisher for these machines.

Do not generate a block's faces independently from text prompts. The built-in image tool exposes explicit reference images/edit targets, not a persistent named generation thread. Each block instead has a family in `block_families.json`, rooted in one master face. The kiln is the first working example. The independently generated `kiln_masonry-v1.png` is rejected and is not a runtime asset.

1. Generate and inspect one high-resolution master face. This defines material, scale, lighting, edge treatment and detail density.
2. Run `python art/textures/family_pipeline.py request kiln --face side`. Inspect the returned reference image with `view_image`. Pass the returned `prompt` and `referenced_image_paths` to the built-in image generation tool. The hash is bookkeeping, not an image-tool argument. Describe changes to this exact block, not a new standalone image. Always include the master; do not rely on earlier conversation images.
3. Record the returned local PNG with `python art/textures/family_pipeline.py record kiln --face side --output PATH`. If the actual prompt differs from the template, save it and supply `--prompt TEXT_FILE`. Receipts retain the prompt, reference path and reference/output hashes. These are local provenance records, not independent proof of a remote tool call.
4. Repeat for `front_on`, editing the cold front. Reuse an existing face for top/back/side when appropriate instead of generating unnecessary variants. The kiln top reuses the side verbatim.
5. Run `python art/textures/family_pipeline.py prepare kiln`. The family shares a 24-color palette derived from the master; a hot face may add eight fixed heat colors. All sources are downscaled through the existing alpha-safe pipeline. Only the configured native-pixel rectangles are accepted from edits. All other pixels are copied from the reference, guaranteeing that hot-state casing and existing masonry do not drift. Rectangles are inspected per block, not guessed globally.
6. Inspect `families/kiln/review.png`: faces together, adjacent wrapping faces and native size. Check course alignment, material scale and contrast; palette sharing alone does not fix geometry. Test the assembled block in game with Photon and Faithful. Extend the family definition if a block needs different directional faces or edge rules; do not assume arbitrary textures are seamless.
7. Run `python art/textures/family_pipeline.py check kiln`, then `publish kiln` to copy checked textures to runtime. Model references are updated separately. Changes to a reference or recorded output invalidate the family until the edit is explicitly regenerated/recorded; stale prepared pixels are rejected.

Run `python -m unittest discover -s art/textures -p "test_*.py"` for both item and family checks. The tests include real kiln casing/masonry preservation, reuse equality and reference-change invalidation. Eight lower-level tests cover the item/block export operations. Kiln, Fertilizer Works and stove now all use reference-driven families; independently generated face sets remain rejected.

Block families include the Fertilizer Furnace and Cooking Stove. The generated kiln masonry is retained as source history; its runtime casing uses resource-pack cobblestone. Active variants preserve casing/grate pixels and change only the specified fire-chamber region.

Use `civilization-mod/dev.ps1 Visual` for the reusable in-game check with Photon and Faithful. Its isolated hidden client prevents mouse capture/recentering and checks a native 1920×1080 framebuffer. The default captures a daylight machine display; `-FullVisual` adds faces, hot states, night lighting and assembled shells under `civilization-mod/runs/visual/screenshots`. Hot models are held on for art inspection; this is not a processing behavior test. Current comparisons are saved in `families/in-game.png` and `families/assembled.png`. The older red-brick kiln source and comparison are retained as history.

## Reproducible pipeline

Install the pinned Pillow version in `requirements.txt`. Run from the project root:

```powershell
python art/textures/pipeline.py prepare
python -m unittest discover -s art/textures -p test_pipeline.py
python art/textures/pipeline.py check
# After inspecting candidates/review.png:
python art/textures/pipeline.py publish
```

`pipeline.json` owns source paths, asset kinds, palette limits and icon extent. Preparation:

1. Reject empty, clipped or opaque-background item sources rather than guessing a background removal.
2. Crop to the alpha silhouette and fit its longest dimension to 28 pixels, retaining aspect ratio and at least two pixels of padding. This standardizes the bounding box, not perceptual mass; unusually thin icons may need an explicit art decision.
3. Downscale with alpha-premultiplied area filtering so invisible RGB cannot create dark/color fringes.
4. Threshold coverage to binary alpha; reduce visible colors to 16 per current item without dithering. Block faces stay opaque and fill the whole canvas.
5. Apply any explicit authored pixel corrections in the manifest (`pixels`: `[x, y, [r,g,b,a]]` entries), then validate again.
6. Save native PNGs, a source/grid/native-size review sheet, and source/output hashes plus Pillow version in `candidates/report.json`.

`check` reconstructs expected pixels and rejects stale candidates or unrecorded changes. `publish` validates the whole batch before copying to runtime texture paths; it does not build or deploy the mod. Re-run preparation after any source or manifest change. Human review must check silhouette readability, material identity, consistent apparent detail and native-size appearance; numeric checks cannot certify those qualities. For new block sets also inspect tiled faces and matching edges in game with Photon and Faithful.

The current source pair demonstrates normalization, not automatic recovery of perfect hand-authored pixel art. If the downscaled result needs different shapes or clusters, use the manifest's pixel corrections or regenerate the source; do not keep changing image size and call it fixed.

## Current runtime material treatment

Street Pavers use [one generated whole-road painting and a reproducible exporter](../assets/street_pavers/asset.json). The exporter reduces the painting to a 512×512 top surface spanning 8×8 one-meter blocks at 64 pixels per block, and takes one matching 64×64 square for exposed sides and items. Full blocks, slabs, stairs and saw-cut pieces select a world-aligned square of the top during chunk baking. The larger-than-life but plausible paver scale favors readability in Minecraft, with individual fired color, chips and wear instead of repeated brick-face stamps. The generated source is not mathematically seamless; a repeat can become apparent on very long uninterrupted paving. This is the first family at the new 64×64 per-block target; the existing 32×32 families have not yet been upscaled or regenerated.

The original family reducer supports deterministic tone curves and protected hot-state pixels. Current kiln models are published by `art/assets/thermal_controllers`, with one broad iron door overlay on a resource-pack cobblestone cube. No Faithful pixels are bundled. The historical `kiln_model.py` command refuses to overwrite these models. Fertilizer Works is displayed as Fertilizer Furnace.

Saw artwork uses one high-resolution iron-saw master, recorded in `saw-prompt.txt`. `saw_variants.py` derives stone and diamond blade colors while preserving warm handle/brass pixels, then `pipeline.py` exports all three at 32×32. Cut building pieces need no generated artwork: their baked models use and crop the source material's active textures.

## Civic block families

The [civic cabinet family](../assets/civic_cabinets/asset.json) replaces the earlier generic dial and drawer designs with function-readable symbols. Land Controller uses a pale limestone cabinet, green parcel boundary, house/red flag emblem and small coal-access slit. Trade Counter uses oak, an ivory balance-scale plaque, a lower transaction slot and a real shallow two-bay tray within the existing one-block bounds. Emblems are static, not live maps or weighing mechanics.

One revised generated concept owns both designs; each block has one referenced 2×2 orthographic atlas master. The shared reducer exports three native 32×32 faces per block with a 24-color palette and protected perimeter/corner pixels. Back/bottom reuse the side. Palette reserves retain neutral iron and the land flag's red/green. Exact built-in prompts, original tool paths, source hashes, deterministic exporter and review evidence are in the asset folder. The earlier concept was rejected because its dial implied pressure and drawers implied storage; it remains recorded provenance.

Publication now belongs to `tools/modeling/pipeline.py publish art/assets/civic_cabinets`; legacy families reject publication to prevent overwrites. The `civic` scene reviews actual models, icons and existing menus under Photon/Faithful. Gameplay and crafting are unchanged.

## Historical prompts — superseded

These early tiny-grid prompts are preserved as failed experiments. Use the high-resolution generation contract above for new work.

### Mineral Coal v1 prompt

Use case: stylized-concept. Asset type: Minecraft inventory item sprite, original artwork for Civilization mod, designed for a native 32 by 32 pixel texture alongside Faithful 32x. Create ONE mineral coal item icon, not a sheet or mockup. A single chunky irregular lump of black anthracite with angular broken planes, charcoal-black shadow, restrained slate grey highlights, a tiny warm grey mineral inclusion. Recognizable dark coal silhouette at tiny inventory size. Classic Minecraft pixel art, slightly richer detail than vanilla but not a smooth illustration, no outlining in solid black that hides the silhouette. Top-left lighting. Object occupies 26 by 25 logical pixels centered in a 32 by 32 logical pixel canvas. Strict square pixel grid; every logical pixel one flat color, small palette about 12 colors, no gradients, no antialiasing, no glow, no cast shadow, no text, no border, no scenery, no ground. Genuinely transparent background. Return an actual 32x32 PNG if supported; otherwise render an exact nearest-neighbor enlargement of that 32x32 sprite, with uniformly sized square pixels and no added detail. Production sprite, not a concept presentation.

### Fertilizer v1 prompt

Use case: stylized-concept. Asset type: original Minecraft inventory item sprite for Civilization, native 32x texture direction alongside Faithful 32x. ONE fertilizer icon, not a sprite sheet or presentation. A squat open coarse tan paper sack containing a visible small mound of chalky pale mineral granules; sack body warm muted ochre, folded rim, one simple dark olive horizontal band, no text or logo. Grounded early industrial agricultural supply, no magic or glow. Clear compact silhouette distinguishable from grey raw mineral blend and black coal at inventory scale. Strict 32 by 32 logical pixel art, object occupies about 24x27 logical pixels centered with transparent margin. 12–16 flat colors, deliberate square pixel clusters, top-left highlights, no gradients, no antialiasing, no smooth curves, no cast shadow. Genuinely transparent background, not a checkerboard painted into the image. Return actual 32x32 PNG if supported; otherwise exact nearest-neighbor enlargement of the 32x32 sprite with evenly sized square pixels and no extra detail. No labels, no border, no environment.

## Survey Table materials

The Survey Table uses code-authored block geometry and references the active pack's oak planks, copper block and birch planks. No new generated raster faces are introduced. The continuous map surface is a runtime texture built from real terrain samples, claim outlines and counter markers; it is not an image-generation asset. The four sections share the same model material references so their style remains consistent with Faithful.

## Refinery family and geometry

The [industrial steel asset](../assets/industrial_steel/asset.json) owns ten 32×32 faces and seven controller/state models. One generated steel master, its pump edit and two referenced fitting atlases establish broad cool-gray plates, iron fasteners and restrained wear. All six controllers have original faces: pump dial/service cover, heater octagonal firebox, column pressure/sight-glass pair, condenser louvers/temperature dial, tank level window and drill load gauge/lever. The Cooling Grille also has newly generated louvers. Controller faces, Industrial Casing, flues, ports and slim pipes share the body material. Protected borders, one bounded palette and a Photon-adjusted tone curve keep the family consistent; heater hot/cold faces differ only in the inspection window. Shapes, recipes and behavior are unchanged. Painted gauges are decorative rather than live telemetry. Bricks remain pack-native; the engine and canisters retain their dedicated artwork.

The earlier family remains as historical provenance: [master prompt](refinery-master-prompt.txt), [master receipt](families/refinery/master-receipt.json), [edit receipts](families/refinery/receipts.json) and [native review](families/refinery/review.png). Current exact prompts, high-resolution masters, atlas crops and protected-edit rules live beside the industrial steel asset manifest. No industrial controller uses its old face. The legacy `refinery_front` sheet is preserved solely because existing boat helm and guardrail models sample its brass texel; the column now uses `column_front`.

This family reserves eight of its 24 shared palette entries for specified brass, ivory and steel accents; the remaining 16 come from the master. Hot faces may add eight heat colors. Families without `accents` retain the original 24-master-color behavior. Protected rectangles lock the outer casing between faces; the cold heater changes only its inspection slit. The family review supports multiple rows so larger families never overlap the comparison strip. Automated checks cover accent budgets and existing casing protection; visual review is still required.

Current reproduction: `python art/assets/industrial_steel/export.py`, followed by review and publication through `tools/modeling/pipeline.py`. The older `refinery` family and its receipts remain historical; its check/publish commands stop with directions to the new owner to prevent rollback. The [refinery model generator](../../tools/generate_refinery_models.py) owns the other geometry/blockstates and consumes published textures; it skips all asset-owned controller models. The hidden `industry` scene checks connected production, complete refinery context, pump front/rear, contextual guides and all six controllers in world/inventory. Hot/cold casing remains normally shaded rather than emissive metal.

The Industrial Guardrail uses modeled steel posts and horizontal rails, with the handrail sampling an existing brass texel from the refinery front. Its multipart connections and inventory model are generated by `tools/generate_refinery_models.py`; no separate raster family is needed.

The [glass pipe asset](../assets/glass_pipes/asset.json) owns world/item multipart models and the eight-pixel-wide octagonal tube. It reuses a frozen shared steel sample; sparse code-native glass glints leave actual transparent holes. Solid steel bevels and broad shoulders surround four narrow inspection slits (about 19% of the tube perimeter); flush rings preserve the sturdy octagonal profile. The renderer draws colored flow inside the tube only while confirmed transfer telemetry is active. Idle glass is static chunk geometry, with no opaque backing or translucent sorting dependence. `build.py` and the retained Blockbench source reproduce the geometry; the older refinery generator no longer overwrites it. The tower requires no rails, ladders or deck; condenser saddles meet the rounded vessel.

The Foundry uses active-pack bricks with an inset vanilla furnace face and cut-brick shoulders/chimney. Steel and Machine Parts use code-native item geometry with existing refinery steel and pack copper surfaces. `tools/generate_metalworking.py` owns these models and progression recipes, and runs automatically at the end of the refinery generator so recipe gates remain intact. No new generated raster sources are involved.

The Small Boat Helm uses a two-wide code-native console: upright octagonal wooden wheel, eight brass spokes, instrument housing and a four-position modeled throttle lever. Both halves share active-pack spruce, refinery steel and the existing refinery brass texel. This follows the [Tesla-era setting](../../design_direction.md#setting-and-material-style). `tools/generate_boat_models.py` owns its paired models, gear/facing variants, recipe, loot and language entries. Hull/cabin art comes from the blocks used to construct the vessel; no new raster master is generated.

## Shared controller UI

The [Smithy redesign concept](../assets/smithy/mockup-03.png) explores a supported brick hearth/chimney and open timber anvil bench using slabs, quarter beams and eighth-block corbels. [Brief](../assets/smithy/brief.md) and [prompt/review receipt](../assets/smithy/concept-receipt.json) separate structural cut pieces from small modeled fittings. Implemented with a 2×2 hearth, recessed slab rear facade, low 2×1.5 bench on eighth-cube feet, quarter-beam piers/hood/bench legs, eighth-cube hood shoulders and a real ground-level anvil and existing resource-pack materials. The tabletop anvil and vise from the concept are intentionally replaced/omitted per the approved refinement. [Comparison](../assets/smithy/comparison.png) and [implementation evidence](../assets/smithy/implementation-receipt.json) record the result; recipes remain unchanged.

All machine/civic panels, standard chest inventories, crafting tables and the normal player inventory/recipe-book backdrop use the original [cabinet master and prompt](ui/prompt.md). [UI export](ui/export.py) preserves the high-resolution source at 512×512 RGB without palette reduction. Forty-eight-source-pixel corners render at eighteen GUI pixels; a three-pixel outward expansion keeps the larger rivets and thicker metal borders clear of native slots. Only panel centers and edge lengths stretch. Raised beveled brass title plates have corner fasteners; category tabs use dark inset metal with brass selection, and recipe search has a magnifying-glass icon and muted placeholder. This UI-specific export is separate from the 32×32 world-block pipeline. `ui/receipt.json` records output settings and hashes; the [resolution review](ui/review-receipt.json) records earlier inventory evidence and the [frame review](ui/frame-review-receipt.json) records the current chest/multiblock refinement. Slots, labels, nameplates, buttons, progress and sight glasses are shared code components; the fire window samples the resource pack’s animated Minecraft fire. No baked screenshot or copied Faithful artwork is shipped.

The [recipe browser mockup](../assets/recipe_browser/mockup-02.png) proposes category tabs, contextual material filters and compact icon-and-name rows in the shared cabinet drawer. Implemented with shared native recipe rows/tabs and existing item sprites, preserving the cabinet texture. The [comparison sheet](../assets/recipe_browser/comparison.png) and [implementation receipt](../assets/recipe_browser/implementation-receipt.json) record the actual Photon review. [Brief](../assets/recipe_browser/brief.md), [review](../assets/recipe_browser/review.md) and [generation receipt](../assets/recipe_browser/mockup-02-receipt.json) record the direction and implementation constraints.

## Bulk freight concept references

[Boat, coal bunker and liquid cargo tank concepts](../../concept_art/prompts.md#freight-boat-and-bulk-storage-concepts) establish visual candidates only. Prompts, original sources and image hashes live in the linked receipt. No runtime textures have been exported from these perspective images; selected structures will use the reference-driven family pipeline when implemented.

## Oil Engine material and animation

The [active engine asset record](../assets/oil_engine/asset.json) now uses the native modeling study in `art/assets/oil_engine/studio/`. The engine fills a complete 3×3 bed with a 24-unit barrel, forward-set crankcase/wheel, and a continuous stepped cast-iron bed joining the controller to the body. It has a thinner steel-faced wheel, narrower flanges, explicit covers/fasteners, recessed stack opening, a compact burner tucked underneath the end cap (no projecting black lip), and a raised isolated gauge with a physical lever. Earlier concepts, sources and `pre-studio-asset.json` remain provenance.

`python art/assets/oil_engine/studio/build.py --clay` builds a neutral whole-assembly study; omit `--clay` for textured native assembly/components. It uses the [generated material master](../assets/oil_engine/studio/material-master.png), shared sprite reducer, 32px-per-block material density and a 512×512 atlas with dedicated face islands. [Material provenance](../assets/oil_engine/studio/material-receipt.json) includes the exact prompt. The [surface master](../assets/oil_engine/studio/surface-master.png) supplies access-cover corners, brass trim and barrel enamel; the continuous bed and integrated control face reuse quiet cast iron instead of decorative panels. `surface_textures.py` preserves corner pixels with nine-slice layouts sized to each face; [surface provenance](../assets/oil_engine/studio/surface-receipt.json) records reduction and opacity handling. Current build scripts are asset-specific; [studio helpers and modeling practice](../../tools/modeling/MODELING.md) provide reusable geometry, checked UVs, native export and fixed-camera reviews. Old `prepare_textures.py` / `build_model.py` produce historical sources only and are not the active publication route.

The [same-camera before/after comparison](../assets/oil_engine/studio/before-after.png), six-view sheets, motion poses and native geometry check retain review evidence. The material pass corrected an actual duplicated-control decal and weak steel/iron separation; native save/reopen testing caught and fixed bevel-coordinate rounding drift. In-game evidence under `studio/ingame/` checks the actual Photon/Faithful assembly, wheel and guides. No gameplay or construction change accompanies this visual revision.

Static controller/cylinder/shaft, loose wheel, item transforms and blockstates are declared together with the native GeckoLib wheel geometry/clip. The whole rim and spokes rotate; the crankcase encloses the linkage. `python tools/modeling/pipeline.py check art/assets/oil_engine --complete` checks reviewed source/publication consistency. The previous `art/textures/engine/export.py` is retired from publishing so it cannot overwrite this family; its `--check` still inspects the historical iron texture.

The [diagnostic model source](../models/engine_proof/engine_proof.bbmodel), [manifest](../models/engine_proof/manifest.json) and [receipt](../models/engine_proof/export-receipt.json) demonstrate the model pipeline. The atlas is an original procedural checker created by `tools/modeling/engine_proof.js` through Blockbench Canvas APIs, with 32 texels per block; no generated concept image or third-party artwork is used. Editor front/side/isometric views and Minecraft running/motion evidence live beside the source. This is test art, not the Oil Engine redesign.

## Canister item family

The [canister asset record](../assets/canisters/asset.json) owns four native Java item models sharing an original iron/steel/brass atlas and generated enamel labels. One [mockup](../assets/canisters/mockup-01.png) establishes the open bridge handle, brass filler, sloped shoulders and folded rims; the [comparison](../assets/canisters/fidelity-comparison.png) checks those forms against the model. No GeckoLib or custom item renderer is used.

`python art/assets/canisters/prepare_textures.py` reuses a preserved copy of the engine material master and reduces the referenced four-label master through the existing 32-pixel pipeline. [Texture provenance](../assets/canisters/texture-receipt.json) records the exact label prompt, source/reference hashes, crops and output. `build_model.py --textured` exports editable Blockbench projects and native models from one geometry definition; the asset pipeline owns publishing. `generate_refinery_models.py` no longer writes canister models.

Gray ring = empty; charcoal drop = crude; amber flame = fuel; green oil-can = lubricant. Labels use both shape and color. First-person display carries the tin low at the side, mostly forward with a small inward tilt exposing the badge at an oblique angle. Verify inventory, frames and held appearance with `./dev.ps1 Verify -Scope Visual -Scene canisters`.

## Comfort HUD

A [reviewed mockup](../assets/comfort_hud/mockup-04.png) explores a tiny unified thermometer centered between health and calories, with comfort conveyed by color/proposed breathing highlights and an integrated road accent. [Brief](../assets/comfort_hud/brief.md) and [generation receipt](../assets/comfort_hud/mockup-04-receipt.json) record scope, exact prompt, reference and visual review. Implemented as shared native whole-pixel GUI drawing, with a small scaled temperature label; no raster atlas is needed. The [implementation receipt](../assets/comfort_hud/implementation-receipt.json) records source and in-game evidence.

Shared coal ignition button: [brief](../assets/coal_ignition/brief.md), original two-frame master, reproducible exporter and [implementation receipt](../assets/coal_ignition/implementation-receipt.json). Brass cabinet frame with resting and contact/sparks poses.

Sulfur now has an original brimstone-yellow mineral sprite with broad facets and sparse fracture marks, generated using the material-cluster guidance. [Brief, source, exports and reviews](../assets/sulfur/asset.json).

## T1 thermal controller family

The [thermal controller asset](../assets/thermal_controllers/asset.json) owns original Kiln, Foundry, Cooking Stove, Fertilizer Furnace and Smithy fronts. Broad brick, pouring-crucible, loaf, sprout and anvil marks identify their jobs. The stove uses a single rectangular ridged cookplate; paired circular burners were rejected for their face-like appearance. Pack cobblestone/brick remains on masonry bodies.

Referenced generated masters and edits reduce to thirteen 32×32 textures and nine native models with shared material palettes, protected edges and local hot-window changes. Exact prompts, sources and reviews live in the asset folder. Reproduce with `python art/assets/thermal_controllers/export.py`, review, then publish with `python tools/modeling/pipeline.py publish art/assets/thermal_controllers`. The hidden `thermal-art` scene checks cold/lit fronts, assembled context, backs and inventory under Photon/Faithful. Structures and recipes are unchanged.
