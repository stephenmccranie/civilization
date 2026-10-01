# Designing item masters for 32×32

Generate high-resolution source art, but design its information for a **28-pixel silhouette inside a 32×32 canvas**. Resolution is an export constraint; visual complexity is a design constraint. Downscaling makes the output grid exact, not the source's invented geometry correct.

## Prompt rules

- Name one recognizable silhouette and two or three essential landmarks before generation. Give each landmark a share of the object's width. As starting estimates rather than fixed rules, a signature stripe might occupy 10–15%; important seams about 7% if they need two final pixels. Thin decorative lines may disappear.
- Request large connected color clusters, two or three main tones per material, roughly 12–16 deliberate colors, hard boundaries and a restrained upper-left light. Preserve smaller **material clusters** as well as broad shapes: a cloth surface needs woven marks, wood needs directional grain, metal can use restrained wear. Avoid smooth gradients, subpixel threads, tiny rivets, speckling and dithering; do not interpret this as a ban on surface texture.
- Keep generous transparent padding. Describe a **high-resolution master composed like a small inventory sprite**, rather than asking for a detailed illustration or a literally 32×32 generated file. A requested logical grid is not a verified grid.
- Reuse an inspected reference for palette and identity, but explicitly suppress its microdetail when it is too rich. For a referenced simplification, hold silhouette, camera, major folds and color accents fixed.

Example addition to a subject-specific prompt:

> High-resolution pixel-art master designed for a tiny final inventory icon. Use a clear silhouette, strong structural landmarks, then deliberate material clusters. Broad planes and thick shadow seams must remain legible. Include sparse, purposeful material marks on broad surfaces, sized to survive as roughly 1–3 final pixels; their contrast is weaker than structural edges but visible at native size. Specify the actual material pattern and direction. Two or three main tones per material plus restrained texture accents; start around 12–16 colors. No gradients, subpixel microtexture, dithering or random noise. Do not leave a textured material's broad face featureless. Single object with transparent padding; no drawn grid.

## Material detail across texture types

Use three scales: **silhouette → structure → material**. Name a characteristic surface treatment in every material brief instead of only saying “pixel art” or “detailed.” Define its direction, approximate final-pixel size, coverage and contrast. Preserve negative space between marks. For small cloth icons, short staggered 1–3px woven clusters over part of the face are a starting point; do not prescribe the same count or pattern for every asset.

For block faces, apply the same hierarchy at their declared texel density, preserve tiling where required, and continue material scale/direction across the family. Wood grain, masonry variation and cast-metal wear need different densities. UI panels may appropriately use smoother, quieter surfaces. This is material-specific guidance, not a global noise filter or a requirement to roughen everything.

At native-size review, check both extremes: **detail must not become noisy or muddy, and broad textured surfaces must not become blank plastic**. If marks vanish, strengthen or enlarge those marks in the referenced master, or retain a few more palette colors when a comparison proves that helps. Do not add procedural random noise after export. Record any deliberate authored pixel corrections.

## Export and review

Keep the existing direct alpha-premultiplied BOX reduction, silhouette normalization, binary alpha and no-dither palette reduction. Start at 12–16 colors for simple items; use more only when a native-size comparison shows a useful gain. Do not globally republish or reduce older assets' palettes.

Review the **native 32px image first**, on light and dark backgrounds, then a nearest-neighbor enlargement. Reject lost landmarks, featureless material faces, muddy folds, incidental speckling and fringes even if the master is attractive. If a landmark vanishes, enlarge/simplify it in a referenced generation; adding source resolution or export colors is not the default repair. Use a small deliberate final pixel-art pass where needed to clean edges or recover an accent; record corrections in the existing `pixels` spec and review the reproducible result. Never edit only the runtime PNG. Compare with the brief’s neighboring assets on a contact sheet or in-game lineup, judging material identity, family fit and distinction as well as technical cleanliness.

Use the comparison helper only when choosing between plausible masters/export settings:

```powershell
python art/textures/compare_sprites.py path/to/master-a.png path/to/master-b.png --output path/to/comparison
```

It exports native/dark/light and enlarged views for direct 12/16/24-color reduction plus a diagnostic 64→32 route. It records source hashes and output metrics and does not publish. The intermediate route includes an extra palette pass; this is not proof that every possible 64px workflow is inferior.

## Evidence and limits

The [cloth experiment](../assets/cloth/trials/results.md) compared three actual generated masters and twelve exports. Broad color clusters preserved its blue selvage and fold shadows best; subsequent user review identified an overly blank center, corrected with sparse woven clusters and a 16-color export; explicit coarse-grid prompting and an intermediate resize did not improve that example. This is a useful default for item sprites, not a universal conclusion about block faces, UI art, all materials, or an optimal image-generation resolution.
