# Cloth reduction experiment

Same subject, reference, view and color identity. Masters generated with the built-in image tool; prompts retained beside the sources. Inspect [comparison](comparison/comparison.png) at native scale; [report](comparison/comparison.json) records hashes and measured exports.

| Source | Native result | Decision |
| --- | --- | --- |
| Detailed weave, widened blue edge (`../mockup-02.png`) | Weave averages into mottled beige; accent becomes grey and fold boundaries soften | Reject for final cloth |
| Large connected clusters (`b-macro.png`) | Stronger separate fold seams, readable blue band, broad ivory top | Select direct 12-color export |
| Requested coarse 64-cell sprite (`c-coarse64.png`) | Cleaner than detailed master, but band reads less distinctly than B; exact requested grid was not enforced | Retain as comparison |

Each master was reduced directly to 32px at 12, 16 and 24 colors and through an intermediate 64px/32-color image to 32px/16 colors. Twelve outputs, identical 28px extent, alpha threshold and BOX filtering, no authored pixel edits. Extra colors did not recover lost structure; the intermediate pass gave no meaningful visual advantage here. B at 12 colors keeps essential contrast with fewer incidental tones. These are visual judgments, not automatic aesthetic scores or a statistically broad benchmark.

Original B: `C:/Users/admin/.codex/generated_images/01a08a58-4280-7292-9a4b-a14f940d3cfc/exec-8c151dcc-d13e-4e71-b72a-e74317aa965c.png`.
Original C: `C:/Users/admin/.codex/generated_images/01a08a58-4280-7292-9a4b-a14f940d3cfc/exec-6aac83f7-88e3-4e6a-98a9-519f0e664b6a.png`.
Both were referenced edits of `../mockup-02.png`; exact prompts are `b-prompt.txt` and `c-prompt.txt`. Originals remain unchanged; local copies are in this directory.

## Surface variation refinement

User review found the selected macro master’s center too blank. A referenced edit (`../mockup-04.png`, exact prompt `../weave-prompt.txt`) adds sparse oatmeal/cream woven clusters while retaining silhouette, seams and blue band. [Old/new export comparison](weave-comparison/comparison.png) checks both at 12/16/24 colors and via64. Direct16 selected: center variation survives without fine-thread noise. The general rule is now silhouette, structure **and material clusters**; simplification must not erase the material surface.
