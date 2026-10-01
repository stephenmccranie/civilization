# Pregenerated flight readiness-brake experiment - 2026-09-20

The user clarified that optimization should target already-generated chunk loading and traversal. Two real-airship trials used the complete prepared corridor and DH cache, main-client enabled mods/settings, 10 seconds settling, 20 seconds moving warmup and 20 seconds measurement. The main client was closed. Production remained 0.33.52; only development telemetry changed. Each trial measured 400 server ticks and 800 physics clearance checks. Both generated zero chunks, recorded zero emergency rejections/holds and zero pilot detachments.

| Measurement | Lower-power trial | Higher-power trial |
| --- | ---: | ---: |
| Nominal power-estimate target (not forced speed), blocks/s | 700 | 5,000 |
| Actual travel, blocks/s | 1,049.1 | 3,944.5 |
| Mean body velocity, blocks/s | 1,046.7 | 3,928.1 |
| Body speed minimum-maximum | 1,046.53-1,046.86 | 3,127.07-4,347.98 |
| Gradual readiness-brake tick samples | 0/400 | 400/400 |
| Forward ready distance minimum/median/maximum, blocks | 723.75 / 783 / 791.5 | 1,068 / 1,627.5 / 2,019 |
| Checked corridor chunks | 424-431 | 1,024 |
| Ready corridor chunks minimum/median/maximum | 375 / 403 / 410 | 493 / 741 / 922 |
| TPS | 20.000 | 19.999 |
| Tick work p99 / maximum, ms | 12.55 / 31.81 | 31.82 / 77.61 |
| Tick intervals above 100 ms | 0 | 2 |
| Java CPU | 40.3% | 60.5% |
| FPS / 1% low | 40.5 / 8.0 | 32.9 / 10.2 |
| Frame intervals above 50 ms | 43 | 91 |
| Maximum frame interval, ms | 212.1 | 136.0 |
| Sampled nearby client chunk coverage | 100% | 94.8% |

Readiness-brake samples mean the governor reduced proposed velocity, not that actual speed decreased in every sample. The higher-power run had 211 successive sample pairs with body speed dropping by more than 1 block/s. At lower power, even the moving warmup recorded no readiness brake or emergency hold.

## Worst speed trough

In the higher-power run, the forward ready distance fell from 1,514 to 1,068 blocks. Body velocity then fell from 3,744 to 3,127 blocks/s in about 0.22 seconds. It recovered as the ready distance rose back toward 1,689 blocks. Tick work during these samples was approximately 16-22 ms, so this trough was not a long server freeze. The readiness flag remained true throughout.

At the current 25 ms physics substep, the governor's distance-derived target is ready distance / 0.4 seconds. Across adjacent measured samples, the difference between that target and current speed correlated 0.9996 with the following body-speed change. This is evidence for the implemented governor driving these fluctuations in this fixture; it does not isolate the cause of every frame stall. The post-tick distance uses a newly published snapshot, while the flag describes the most recent physics decision, so it is not a same-instruction trace.

## Findings and next work

The earlier zero-emergency-hold metric missed continuous readiness limiting. The lower-power steady-speed result is confirmed, but the high-power test must not be described as unconstrained or consistently smooth. Pregenerated chunks still require decoding, promotion and availability before physics may enter them. Not all 1,024 tracked chunks were ready, and readiness changed fast enough to drive speed oscillation. The bounded corridor also reached its capacity; this run does not independently quantify loading throughput versus corridor-budget constraints.

Next work should improve the stability and throughput of forward ready coverage, reduce ticket/snapshot bookkeeping overhead, and evaluate governor sensitivity against this exact fixture while retaining collision protection. Fresh generation is outside this optimization target. Client frame stalls remain independently visible even when the governor is inactive. These are single diagnostic trials, not a repeated performance comparison or proof of behavior for every route, heading or hull.

The new telemetry compiled and ran successfully end to end. The final high-speed screenshot was inspected: terrain and deck are visible, with the readiness-braking HUD active. Ignored evidence under civilization-mod/runs/flight-benchmark/results:

- 20260920-190815-readiness-r1-airship-700-fa4128
- 20260920-190954-readiness-r1-airship-5000-faab56

Combined report: civilization-mod/runs/flight-benchmark/readiness-report.html and readiness-report.json. Each result retains ticks, frames, per-step emergency checks, JFR, manifests and screenshot. See the [protocol](../../civilization-mod/testing.md#controlled-fast-flight-benchmark) for timing and runtime differences. No production change or deployment was made in this experiment.
