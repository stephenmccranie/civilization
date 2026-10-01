# Development checks

Use the smallest check that covers the changed behavior. Do not run the whole battery after every edit, and do not repeat successful checks without a relevant code change or unresolved failure.

| Change | Check |
| --- | --- |
| Documentation or local configuration | Inspect the edit; no Minecraft launch |
| Routine code, text, or item registration | `./dev.ps1 Verify` (build + fast unit tests) |
| Recipes, inventory, placement, persistence, production, calories | `./dev.ps1 Verify -Scope Gameplay` |
| Rendering, shaders, HUD, textures | `./dev.ps1 Verify -Scope Visual -Scene <scene>` |
| Changes spanning client and server, or release checkpoint | `./dev.ps1 Verify -Scope Full -Scene <scene>` |

Verification runs its Gradle tasks in one invocation. Gradle reuses unchanged compilation and unit-test results. Deployment also uses this incremental build; there is no need to run Build separately immediately before Deploy.

The 64 server tests take about two seconds once Minecraft starts. Keep the full server suite for gameplay changes: filtering a few tests saves little because startup dominates. Recent warm runs were about 3 seconds for a build, 15–20 seconds for server checks, and 35–50 seconds for focused graphics checks. These are observed ranges, not guarantees; the script reports actual Gradle elapsed time.

## Focused visual checks

`./dev.ps1 Visual -Scene machines` is the short default: assembled machines in daylight, one screenshot, then exit. The previous tour also waited through night, outline mode, and several piece previews. Request that only when those modes are relevant:

```powershell
./dev.ps1 Visual -Scene machines -FullVisual
```

Other scenes:

- `modular`: mixed-material and irregular cut-block assemblies.
- `material-sync`: delayed material packets, checking received identities and before/after rendering.
- `guide`: textured multiblock guide.
- `textures`: item/controller display.

Keep the hidden 1920×1080 window, mouse-capture guard, and three-minute timeout. Review the relevant screenshot and logs. A successful process exit alone does not prove the image is correct. Usually one post-fix visual run is sufficient; reproduce a pre-fix image only when needed to establish the bug.

Use the longer visual tour independently of scope: `-Scope Full` selects server plus visual checks, while `-FullVisual` selects the extended scene sequence. Modular and material-sync scenes already have focused sequences and do not add an extended tour.
