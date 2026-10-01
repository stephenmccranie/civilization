# Development verification

Follow [testing.md](testing.md) for the shortest appropriate check. Routine edits use the incremental build and unit tests. Gameplay changes use server GameTests; rendering-only changes use the relevant focused visual scene without an unrelated server test run. Changes spanning both use both. Documentation/configuration-only edits do not need a game launch.

Do not repeat successful checks unless relevant code changed, a check failed, or a specific concern remains unresolved. Do not run Build immediately before Deploy: Deploy already builds incrementally. The long visual tour is opt-in (`-FullVisual`), not routine.

Preserve hidden 1920×1080 visual testing and mouse-capture guards. Never launch a visible test client or interfere with the user's Prism game. Read visual output and explicit GameTest pass results when those checks are used.

Documentation ownership and same-change updates are defined in the [root instructions](../AGENTS.md). Keep current behavior here and current validation/version facts in `../docs/status.md`.
