# Working on Civilization

Start with [README.md](README.md), [current status](docs/status.md), and the active package in [development_plan.md](development_plan.md). Read the relevant implementation reference before changing behavior. The user's latest decisions take precedence over older documents.

## Keep documentation current in the same change

For asset creation/redesign, use the project [Civilization art skill](.agents/skills/civilization-art/SKILL.md) and [shared asset pipeline](tools/modeling/README.md#complete-asset-pipeline): brief → one generated direction → autonomous visual review/iteration → implementation → actual model/in-game review. Do not default to competing concepts or routine user approval gates. Concept selection does not approve generated mechanical errors.

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

The owner authorizes committing and pushing completed project work to the public `stephenmccranie/civilization` repository. After change-specific validation and documentation updates, commit the completed task and push its branch without asking for routine confirmation. Use `codex/` branches for larger or isolated changes and attach any created pull request to the task. Preserve unrelated uncommitted work; stage only the current task after the initial whole-project upload checkpoint. Never force-push or rewrite shared history without explicit authorization. Keep worlds, local configuration, credentials, caches and downloaded dependencies out of Git. Preserve the byte-stable `.gitattributes` policy because asset receipts hash their original sources. Use the owner-approved Git identity; never invent one.
