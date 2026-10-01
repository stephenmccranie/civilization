# Forward terrain and collision validation - 2026-09-20

The [earlier reproduction](2026-09-20-airship-safety-hitches.md) established repeated snapshot-bound stops around 1,000 blocks/s. The replacement publishes immutable forward coverage, requests bounded FULL-status terrain ahead, and brakes against remaining ready distance. Implementation limits belong in the [vehicle reference](../../civilization-mod/vehicle-physics.md#airship-prototype).

## Physical flight

Each trial used the prepared flat corridor, real assembly/controls and the main client's enabled mods, Photon and resource-pack settings. No velocity injection or safety bypass was used. Numeric target is a power estimate, not a speed servo. All successful cruising trials below generated zero new chunks and recorded zero rejected physics steps, zero tick-end emergency-hold episodes and zero pilot detachments during 20 seconds of measurement (800 physics checks each).

| Trial | Measured travel blocks/s | Body speed p05-p95 | TPS | Tick work p99 ms | Process CPU | FPS / 1% low |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Candidate, nominal 1,000 | 1,787.6 | 1,784.05-1,784.40 | 19.988 | 26.56 | 50.9% | 51.7 / 13.8 |
| Candidate, nominal 5,000 | 4,012.2 | 3,762.80-4,236.69 | 20.003 | 35.45 | 56.8% | 28.7 / 11.0 |
| Final collision guard, nominal 700 | 1,048.6 | 1,046.55-1,046.85 | 19.992 | 11.10 | 39.7% | 31.6 / 7.0 |

The final trial's body speed range was 1,046.53-1,046.86 blocks/s; nearby client chunk coverage was 100%. Candidate warmup was 10 seconds; final warmup was 20 seconds, following 10 seconds settling. Earlier candidates lack the final continuous block-shape guard, so their source fingerprints differ. These are diagnostic single trials, not a repeated matched-speed A/B claim.

Physics continuity improved, but client smoothness is unresolved. The final run had 48 frame intervals above 50 ms and a 227.8 ms maximum despite no tick interval exceeding 100 ms. Candidate JFR execution samples show render-thread chunk lighting, Sodium rebuild scheduling/cache invalidation and chunk handling; DH chunk-update workers were also prominent. This identifies work to investigate, not a quantified exclusive cause or a proven GPU/GC attribution. Hiding ordinary terrain does not remove all client chunk processing. Sustained 5,000-block/s usable rendering is not established.

## Thin-wall regression

Inspection of the exact Sable 2.0.5 source found vertical-only terrain prediction. Although native bodies enable CCD, its custom terrain dispatcher returns Unsupported for both linear and nonlinear shape casts. The first full-height, one-block-thick bedrock wall trial tunneled through x=16,384, reaching controller x=16,403.90. Collider preparation alone was insufficient.

The final conservative swept-hull guard passed the same fixture: approach peak 3,989.79 blocks/s, furthest controller x=16,381.486, no crossing or pilot loss. The wall is 65 blocks wide and spans world height; it is built before timing. Continued thrust remained blocked. Its stationary measurement is not a cruising-performance sample. The guard bounds scans and fails closed if readiness/work limits fail. Existing contacts remain with Sable. Extreme rotation, moving-vessel collision, irregular hull false positives and dense modded terrain remain unverified.

Source inspected locally at Sable NeoForge 2.0.5 tag commit 6966d2928340de7631abcecf8549904b877df0a8: PhysicsChunkTicketManager.java, SubLevelPhysicsSystem.java, and sable_rapier/src/main/rust/rapier/src/dispatcher.rs. No Sable JAR or core code was changed.

## Fixture correction and receipts

The first candidate outran its target-derived copied corridor and generated 6,125 chunks. It is excluded from pregenerated performance comparisons. Airship runs now copy the complete baseline; spectator runs still copy the required prefix. The observer uses the exact immutable snapshot passed to production. Accepted collision-shortened motion is not an emergency rejection.

Ignored evidence under civilization-mod/runs/flight-benchmark/results:

- 20260920-183119-smooth-v1-r1-airship-1000-947fb5 (invalid pregenerated comparison)
- 20260920-183738-smooth-v2-r1-airship-1000-4af2a4
- 20260920-183914-smooth-v2-r1-airship-5000-b15c89
- 20260920-184303-smooth-wall-r1-airship-5000-4444d7 (expected regression failure discovered)
- 20260920-184902-smooth-wall-v2-r1-airship-5000-ea940e (COLLISION_PASS.txt)
- 20260920-185059-smooth-final-r1-airship-700-5f059d

Final cruising screenshot inspected: terrain, Photon and attached deck visible. Runtime differences and measurement definitions remain in the [benchmark protocol](../../civilization-mod/testing.md#controlled-fast-flight-benchmark). Build/deployment and ordinary regression results belong in [status](../status.md).
