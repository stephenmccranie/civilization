# Working on Civilization

The owner's latest decision wins over anything written here or in the docs.

## Where things live

Each fact has one home. Update it in the same change as the code or decision, replacing stale wording rather than appending a correction.

| Topic | Home |
| --- | --- |
| Agreed game rules, setting, and each system's open questions | `design_direction.md` |
| What is deployed now, known limits, ordered work queue | `development_plan.md` |
| Setup, `dev.ps1` commands, source map, mod doc index | `civilization-mod/README.md` |
| Shipped behaviour, recipes, controls, limits per system | `civilization-mod/<system>.md` |
| Which check to run for which change | `civilization-mod/testing.md` |
| Version | `civilization-mod/gradle.properties` |
| Player pack, client/server settings, releases | `modpack/README.md` |
| Live server services, scripts, settings and deployment | `server/README.md` |
| Art pipeline | `tools/modeling/README.md` and the `civilization-art` skill |

Docs describe the current state. History lives in git: no changelogs, diaries, status logs or per-build test counts in docs. Proposals stay in their design section's Open list until the owner decides; never promote one silently. Link only to tracked files, and run `python tools/check_docs.py` after doc or path changes.

## Implementation contracts

- **Conservation:** stock, fuel and reserved payments cannot be spent twice; transformations explicitly consume their inputs.
- **Authority:** the server resolves owner/whitelist access at action time, including automation and already-open menus.
- **Persistence:** claims, inventories, reservations and deadlines commit consistently and survive retries and restarts. Diagnostic logs are never the source of truth.
- **Bounded work:** inspection, map listings and claims never force-load the world; define active, unloaded and offline behaviour per consumer.
- **Migration:** live-world changes need an explicit migration. A fresh world is used only when the owner selects one, and the live server world is never reset without a new explicit request.
- **Feedback:** show the useful requirement, state and failure reason; keep implementation identifiers out of ordinary play.

A feature is ready to build when it has a concrete interaction, agreed rules, data ownership, lifecycle, bounded work and observable acceptance cases. A passing functional test proves neither an economy nor 200-player capacity.

## Verification

Pick the shortest check from `civilization-mod/testing.md`; be thorough but efficient. Documentation or configuration changes need no game launch. `dev.ps1 Deploy` already builds, and a passing check is not repeated unless relevant code changed.

Visual checks run hidden and silent at 1920x1080 with mouse capture disabled. Never launch a visible client. The owner's Prism instances are touched only by `.\dev.ps1 Deploy`, which installs into Civilization Dev. When appearance changes, look at the screenshots. Diagnose unexplained failures, including flaky fixtures; never retry until green or weaken an assertion.

Run directories under `civilization-mod/runs/` are rebuilt by each run, except `runs/server` (the owner's development world). `civilization-mod/run/` holds the client setup that visual checks copy. Scratch goes in `.tools/`, which is ignored and may be wiped at any time. Never delete user worlds, `art/`, `concept_art/` or local configuration, and preserve uncommitted work.

## Git and publishing

Start each task on a short-lived branch from current `origin/main` with a plain topic name, and continue that branch for follow-ups to the same task. Use a separate worktree when needed to protect concurrent or uncommitted work. Commit focused local checkpoints and stage explicit paths; never commit to `main`.

Civilization Dev is the owner's test instance and carries the newest build under test; their other Prism instance is the published GitHub pack. Once a change passes its checks, run `.\dev.ps1 Deploy` from its checkout and tell the owner what to try. A later deploy replaces the jar, so name the installed branch. The owner normally tests there before asking to publish.

Publish only when the owner asks or agrees to a milestone, grouping related work into one PR. Once publication is authorized, carry it through without asking again: push, open a PR to `main` describing behaviour, validation and limits, get required checks green, squash-merge, fast-forward local `main` and delete the merged branch. A modpack release happens only at an agreed distribution milestone. `main` is always the verified published version.

Never force-push, rewrite published history or use `git push --all`/`--mirror`. The local history branches `local/full-project-before-github` and `master` contain all early art and stay local. Commit as `stephenmccranie <179844616+stephenmccranie@users.noreply.github.com>`. `.gitattributes` keeps file bytes exact because asset receipts hash their sources.

**GitHub holds** code, runtime resources (including finished textures and models), docs, reusable tools and the approved style reference sheets in `style_references/`. **It never holds** the bulky working files in `art/` and `concept_art/` (masters, drafts, review history), worlds, local configuration, credentials, caches, downloaded third-party files, player data, home-network details or the server address.
