# Original texture artwork

Native target: 32×32. Generated using the built-in image generation tool. Sources are original generated artwork, not edits of Faithful assets.

The original 0.10.3 exports were inadequate: their files were 32×32, but arbitrary point sampling retained hundreds of colors and partially transparent pixels. They did not establish a consistent pixel-art grid or silhouette scale. The replacement pipeline explicitly constructs and validates the final pixels.

Sources are kept here; runtime textures live in `civilization-mod/src/main/resources/assets/civilization/textures`. These first two items establish a candidate style; remaining icons and machine faces still use vanilla references.

Current sources are `sources/mineral_coal-v3.png` and `sources/fertilizer-v3.png`, freshly generated detailed 1254×1254 masters. Their [prompts](sources/v3-prompts.md) contain no target-size or tiny-grid instruction. The pipeline downsamples them to 32×32, normalizes silhouette extent, limits visible colors to 16 and produces binary alpha. Published in 0.10.5-dev. Earlier coarse-grid attempts are retained as references, not the current source. Review the final result in [the comparison sheet](candidates/review.png); fine master-image detail naturally disappears at inventory scale.

0.10.6 adds Raw Mineral Blend, Bread Dough, Cookie Dough, Cake Batter and Unbaked Pumpkin Pie from fresh high-resolution masters; [their exact prompts](sources/food-and-blend-prompts.md) follow the same approved approach. All seven icons pass export validation and appear in the development client's hotbar with Photon, both without and with the user's Faithful 32x pack enabled. The build and 19 unit tests pass. Food bowls/tins are illustrative parts of the icons and do not add container mechanics or recipe requirements.

## Generation contract

Use the built-in image generation tool for artwork, one asset per call. It exposes prompts and image references, not guaranteed dimensions, palette limits or a strict pixel grid. Asking for those constraints guides composition but does not enforce them. Do not switch to API/CLI generation merely to obtain size controls.

Generate **detailed high-resolution pixel art**: one isolated subject, crisp stepped edges, coherent pixel clusters, rich material detail, consistent top-left light, true transparent background and generous padding. Do not mention 32×32, a tiny logical grid, a tiny palette, or enlarged low-resolution pixels in generation prompts. The user explicitly chose to reserve target resolution and palette reduction for the pipeline. Measure the returned file instead of assuming compliance; area filtering reconstructs the final grid from the detailed master artwork.

For block faces request square orthographic face artwork, fully opaque, edge-to-edge, without perspective or lighting effects. Treat seamless edges and matching active/inactive variants as separate visual acceptance requirements. The pipeline supports opaque block export but does not magically make textures tile or align a machine's faces.

## Block families: required workflow

Do not generate a block's faces independently from text prompts. The built-in image tool exposes explicit reference images/edit targets, not a persistent named generation thread. Each block instead has a family in `block_families.json`, rooted in one master face. The kiln is the first working example. The independently generated `kiln_masonry-v1.png` is rejected and is not a runtime asset.

1. Generate and inspect one high-resolution master face. This defines material, scale, lighting, edge treatment and detail density.
2. Run `python art/textures/family_pipeline.py request kiln --face side`. Inspect the returned reference image with `view_image`. Pass the returned `prompt` and `referenced_image_paths` to the built-in image generation tool. The hash is bookkeeping, not an image-tool argument. Describe changes to this exact block, not a new standalone image. Always include the master; do not rely on earlier conversation images.
3. Record the returned local PNG with `python art/textures/family_pipeline.py record kiln --face side --output PATH`. If the actual prompt differs from the template, save it and supply `--prompt TEXT_FILE`. Receipts retain the prompt, reference path and reference/output hashes. These are local provenance records, not independent proof of a remote tool call.
4. Repeat for `front_on`, editing the cold front. Reuse an existing face for top/back/side when appropriate instead of generating unnecessary variants. The kiln top reuses the side verbatim.
5. Run `python art/textures/family_pipeline.py prepare kiln`. The family shares a 24-color palette derived from the master; a hot face may add eight fixed heat colors. All sources are downscaled through the existing alpha-safe pipeline. Only the configured native-pixel rectangles are accepted from edits. All other pixels are copied from the reference, guaranteeing that hot-state casing and existing masonry do not drift. Rectangles are inspected per block, not guessed globally.
6. Inspect `families/kiln/review.png`: faces together, adjacent wrapping faces and native size. Check course alignment, material scale and contrast; palette sharing alone does not fix geometry. Test the assembled block in game with Photon and Faithful. Extend the family definition if a block needs different directional faces or edge rules; do not assume arbitrary textures are seamless.
7. Run `python art/textures/family_pipeline.py check kiln`, then `publish kiln` to copy checked textures to runtime. Model references are updated separately. Changes to a reference or recorded output invalidate the family until the edit is explicitly regenerated/recorded; stale prepared pixels are rejected.

Run `python -m unittest discover -s art/textures -p "test_*.py"` for both item and family checks. The current 12 tests include real kiln casing/masonry preservation, reuse equality and reference-change invalidation. Eight lower-level tests cover the item/block export operations. Kiln, Fertilizer Works and stove now all use reference-driven families; independently generated face sets remain rejected.

Version 0.10.8-dev adds grey cobblestone kiln masonry, the copper/iron Fertilizer Works family and dark-iron Cooking Stove family. All start from detailed high-resolution masters; the pipeline alone handles native 32×32 output. Active states accept changes only in fire-chamber regions, preserving casing and grate pixels. The kiln top and Works top reuse their respective sides; the stove has an edited burner top.

Use `civilization-mod/dev.ps1 Visual` for the reusable in-game check with Photon and Faithful. Its isolated hidden client prevents mouse capture/recentering and checks a native 1920×1080 framebuffer. It captures faces, hot states, night lighting and assembled shells under `civilization-mod/runs/visual/screenshots`. Hot models are held on for art inspection; this is not a processing behavior test. Current comparisons are saved in `families/in-game.png` and `families/assembled.png`. The older red-brick kiln source and comparison are retained as history.

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

## Mineral Coal v1 prompt

Use case: stylized-concept. Asset type: Minecraft inventory item sprite, original artwork for Civilization mod, designed for a native 32 by 32 pixel texture alongside Faithful 32x. Create ONE mineral coal item icon, not a sheet or mockup. A single chunky irregular lump of black anthracite with angular broken planes, charcoal-black shadow, restrained slate grey highlights, a tiny warm grey mineral inclusion. Recognizable dark coal silhouette at tiny inventory size. Classic Minecraft pixel art, slightly richer detail than vanilla but not a smooth illustration, no outlining in solid black that hides the silhouette. Top-left lighting. Object occupies 26 by 25 logical pixels centered in a 32 by 32 logical pixel canvas. Strict square pixel grid; every logical pixel one flat color, small palette about 12 colors, no gradients, no antialiasing, no glow, no cast shadow, no text, no border, no scenery, no ground. Genuinely transparent background. Return an actual 32x32 PNG if supported; otherwise render an exact nearest-neighbor enlargement of that 32x32 sprite, with uniformly sized square pixels and no added detail. Production sprite, not a concept presentation.

## Fertilizer v1 prompt

Use case: stylized-concept. Asset type: original Minecraft inventory item sprite for Civilization, native 32x texture direction alongside Faithful 32x. ONE fertilizer icon, not a sprite sheet or presentation. A squat open coarse tan paper sack containing a visible small mound of chalky pale mineral granules; sack body warm muted ochre, folded rim, one simple dark olive horizontal band, no text or logo. Grounded early industrial agricultural supply, no magic or glow. Clear compact silhouette distinguishable from grey raw mineral blend and black coal at inventory scale. Strict 32 by 32 logical pixel art, object occupies about 24x27 logical pixels centered with transparent margin. 12–16 flat colors, deliberate square pixel clusters, top-left highlights, no gradients, no antialiasing, no smooth curves, no cast shadow. Genuinely transparent background, not a checkerboard painted into the image. Return actual 32x32 PNG if supported; otherwise exact nearest-neighbor enlargement of the 32x32 sprite with evenly sized square pixels and no extra detail. No labels, no border, no environment.

Version 0.10.9 uses a deterministic family-wide tone curve (`tone` in `block_families.json`) after quantization to lift dark metal and chamber detail. It preserves the color budget, alpha and protected-pixel equality. Generated high-resolution sources remain unchanged. `python art/textures/kiln_model.py` builds the layered kiln models: the full cube uses `minecraft:block/cobblestone`, and only the two iron-panel rectangles use custom front textures. No Faithful pixels are copied or bundled; resource-pack changes automatically apply. The generated kiln side/top remain artwork history and are no longer referenced by runtime models. Fertilizer Works is now displayed as Fertilizer Furnace.

Saw artwork uses one high-resolution iron-saw master, recorded in `saw-prompt.txt`. `saw_variants.py` derives stone and diamond blade colors while preserving warm handle/brass pixels, then `pipeline.py` exports all three at 32×32. Cut building pieces need no generated artwork: their baked models use and crop the source material's active textures.
