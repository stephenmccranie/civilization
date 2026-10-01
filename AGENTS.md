# Working on Civilization

Start with [README.md](README.md), [current status](docs/status.md), and the active package in [development_plan.md](development_plan.md). Read the relevant implementation reference before changing behavior. The user's latest decisions take precedence over older documents.

## Multiblock guide performance

All current and future multiblock guides, including native model guides, must follow the [shared guide performance contract](civilization-mod/multiblock-builds.md#guide-performance-contract). Use `GuidePerformance` for refresh cadence and bounded visible outlines; cache layout and construction geometry outside the frame loop. Do not introduce separate polling/budget constants or per-frame collision unions.

## Keep documentation current in the same change

For asset creation/redesign, use the [Civilization art skill](.agents/skills/civilization-art/SKILL.md) and [shared asset workflow](tools/modeling/README.md#complete-asset-pipeline): Define → Build and refine → Verify and publish. Use compact new asset records, one authoritative source, reusable materials and actual visual proofs. Concepts are optional for established directions; routine approval gates are unnecessary. Preserve legacy assets and provenance.

For multiblocks built mostly from full and saw-cut blocks, use the small [isometric plan script](tools/modeling/README.md#isometric-block-plans). Define the block placements in one short Python layout file and review its two complete isometric views plus half-block layer diagrams. Blockbench remains for custom moving models; neither it nor Minecraft is needed for block-layout planning. The diagrams are engineering plans, not implemented machine art.

- Agreed rules: edit the relevant section of `design_direction.md`.
- Unresolved choices/proposals: edit `open_decisions.md`; resolve entries when their rules move to the design. Never promote a proposal silently.
- Shipped behavior, recipes, controls, limits or migration: update `civilization-mod/README.md` or its focused reference. Replace stale wording rather than appending a contradictory clarification.
- Work order and completion: update `development_plan.md`; link to implementation details rather than duplicating them.
- Build/version/validation/deployment: update `docs/status.md` when those facts change. `civilization-mod/gradle.properties` owns the actual version. Keep current test counts and build labels in status, not every guide.
- Art: update `art/textures/README.md` and the relevant manifests/receipts. Record prompts and original sources; concept art is not an implemented feature.

Update only affected documents. Do not create another roadmap, latest-decisions file, or per-turn diary. Link to the authoritative section instead of copying its rules. Historical evidence belongs in `docs/history`; frozen snapshots belong in `docs/archive` and should not be rewritten.

Before finishing, check for documentation conflicts and run `python tools/check_docs.py` after documentation/path changes. Report actual validation and limitations. Follow `civilization-mod/testing.md` for change-specific checks; documentation cleanup needs no Minecraft launch.

Keep runtime saves, shader installs, local configuration, caches and scratch output in their existing ignored locations. Do not move source/resource paths casually, delete user worlds, stage unrelated work, or invent a Git identity. Preserve uncommitted work during cleanup.

## GitHub workflow

The owner authorizes committing and pushing completed project work to the public `stephenmccranie/civilization` repository. After change-specific validation and documentation updates, commit the completed task and push its branch without asking for routine confirmation. Use `codex/` branches for larger or isolated changes and attach any created pull request to the task. Preserve unrelated uncommitted work; stage only the current task after the initial code/documentation upload checkpoint. Never force-push or rewrite shared history without explicit authorization. Publish code, runtime resources, documentation and tools only. Keep original art and visual review history (`art/`, `concept_art/`), worlds, local configuration, credentials, caches and downloaded dependencies out of GitHub. Preserve the local full-project history branch; never push it or use `git push --all`/`--mirror`. Preserve the byte-stable `.gitattributes` policy because asset receipts hash their original sources. The approved identity is `stephenmccranie <179844616+stephenmccranie@users.noreply.github.com>`; never invent another identity.
