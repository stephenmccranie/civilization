# Airship safety-hold reproduction - 2026-09-20

The user reported hitches and inconsistent speed around 1,000 blocks/s in superflat flight. The earlier 1,000-block/s benchmark moved a spectator and did not exercise production airship physics. Its 20 TPS result cannot establish smooth physical flight.

## Reproduction

A real 5x7 deck/controller used ordinary piloting and constant estimated power in the prepared corridor, with the main client's enabled mods and graphics settings. Settling lasted 10 seconds, moving warmup 10 seconds, and measurement 20 seconds. No velocity injection or safety bypass was used. The numeric benchmark speed is a power estimate, not a velocity constraint.

The nominal 1,000-block/s trial measured:

- Actual travel: **968.1 blocks/s average**; body velocity ranged from **0 to 1,601.3 blocks/s**, median 962.4. The HUD samples velocity only every ten server ticks, so it can show different parts of this cycle.
- **70 tick-end hold episodes** in 20 seconds, with the hold flag present in 17.5% of time-weighted tick-end samples. This percentage is not exact stopped duration.
- **74 rejected checks out of 800 physics checks**. The snapshot observed at return attributed **65 to snapshot bounds** and **9 to missing snapshot chunks**. These classifications observe state after the production decision; concurrent updates can affect them.
- **20.0 TPS**, 7.25 ms p95 tick work, maximum tick work 12.63 ms; no tick interval exceeded 100 ms (maximum 76.1 ms).
- **111.5 FPS**, 35.4 FPS 1% low; maximum frame interval 36.7 ms. Normalized Java-process CPU averaged 35.5%.
- Zero newly generated chunks, zero measured pilot detachments, 100% sampled nearby client coverage. That near-client metric is not the physics sweep or a guarantee of farther-ahead server coverage.

An initial higher-power estimate (nominal 1,250 blocks/s) reproduced an approximately 807 -> 1,344 -> 0 blocks/s cycle. A second run with the observer recorded 168 rejections: 164 observed snapshot-bound failures and four missing-snapshot-chunk failures. These runs are diagnostic, not a controlled before/after performance comparison.

## Cause established in this fixture

AirshipSystem snapshots loaded chunks around the controller at ServerTickEvent.Post. Each physics step checks the accelerated predicted velocity for at least 50 ms, with a 24-block margin, against that fixed snapshot. Movement between updates consumes some of the snapshot's forward reach. Its rejection path immediately cancels linear velocity; subsequent valid steps accelerate again with the still-active throttle. This can produce repeated stops well below a simplistic 128 blocks / tick estimate, independently of low average TPS or missing nearby client chunks.

The primary observed rejection here was the snapshot boundary, but missing snapshot chunks also occurred. Merely enlarging the boundary or removing checks is not demonstrated safe by this test. A structural correction needs forward coverage sized for the predicted path and snapshot age, while retaining actual unloaded-terrain/collision protection. No production physics behavior was changed by this investigation.

## Benchmark changes and evidence

The dev-only observer records safety.csv without modifying the result, motion or loading. Summaries/reports now expose body-speed percentiles, hold episodes, long frames, tick interval tails and observed rejection reasons. The ordinary target-speed coverage probe is retained but must not be confused with the actual accelerated physics sweep.

Ignored local evidence: civilization-mod/runs/flight-benchmark/hitch-report.html and hitch-report.json, with raw ticks, frames, safety checks, JFR and screenshots in these result directories:

- 20260920-181112-hitch-investigation-r1-airship-1250-ef1637 (before per-step observer)
- 20260920-181406-hitch-reasons-r1-airship-1250-8caeea
- 20260920-181615-hitch-1000-r1-airship-1000-117e91

The final screenshot was inspected: Photon terrain and the craft were visible. The main client was closed, so this is a controlled reproduction rather than a capture of the user's exact play session. Follow the [benchmark protocol](../../civilization-mod/testing.md#controlled-fast-flight-benchmark) for reruns.
