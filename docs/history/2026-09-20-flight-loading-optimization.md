# Pregenerated flight loading optimization - 2026-09-20

Scope: already-generated chunk loading/traversal with real airship physics. Production candidate 0.33.53 retains the existing 1,024-chunk ship/4,096-chunk world ticket bounds, 0.65-second forward prediction and original readiness governor. The accepted change is primitive-long hash collections for corridor construction, ticket membership and immutable snapshots. Defensive snapshot ownership and collision checks are retained. No DH settings are changed in the main client.

## Bottlenecks established

The baseline high-power JFR's last approximately 20 seconds contained 735 server execution samples. Of those, 187 included our code and ImmutableCollections.SetN.probe. Packed chunk keys hash as x XOR z under Long.hashCode; the immutable set's linear probing clusters these corridor coordinates. Primitive-long collections mix the full key before table indexing. After replacement, the corresponding count was zero, snapshot-distance samples fell from 121 to three, and AirshipTerrain.update samples from 97 to 17. Samples are statistical observations, not exact elapsed-time percentages.

With the original buffer/governor unchanged, the first candidate trial reduced server tick-work p99 from 31.82 to 22.21 ms. Actual mean travel remained approximately 3,945 versus 3,955 blocks/s. FPS changed from 32.9 to 75.5, but the candidate still had a 318.6 ms frame and a 1,472-block/s speed trough; these single runs do not establish consistent client smoothness or eliminate readiness limiting.

Remaining work was dominated by DH render-data building/chunk updates and C2ME saved-chunk decoding. The candidate's filtered sample groups included 4,059 DH and 1,331 C2ME samples out of 6,840 total. Low overall CPU does not imply spare capacity in the critical loading stages. A subsequent larger-buffer/eight-worker trial hit a 562.5 ms stop-the-world GC pause and nearly filled its 6 GiB heap, establishing a distinct source of true freezes.

## Rejected tuning experiments

All completed trials below reported zero newly generated route chunks, no emergency physics rejection and no pilot detachments. Continuous readiness limiting remained active at high power.

| Experiment | Travel blocks/s | Body speed min-max | FPS | Outcome |
| --- | ---: | ---: | ---: | --- |
| Hash collections only, 6 GiB, DH 8, 20s warmup/20s measure | 3,955 | 1,472-4,525 | 75.5 | Retain the data-structure optimization; residual stalls remain |
| Larger forward buffer, original governor, DH 8, 10s/15s | 5,818 | 1,191-7,923 | 54.2 | Reject: higher bursts and deeper troughs |
| Larger buffer, more reserve/slower recovery, DH 8, 10s/15s | 4,497 | 2,209-4,878 | 30.6 | Insufficient improvement in worst dips |
| Same buffered governor, DH 2, 10s/15s | 4,769 | 4,051-4,873 | 83.9 | Reject: final screenshot has missing terrain |
| Same buffered governor, DH 8, 20s/20s | 4,289 | 1,600-4,709 | 56.3 | 562.5 ms GC pause; retain original geometry/governor |
| Same buffered governor, DH 2, 20s/20s | 4,740 | 4,495-4,861 | 75.6 | Reject: missing terrain despite better motion |

The two-worker intervention supports contention as part of the problem, but reduced rendering work/missing terrain makes its FPS unsuitable as a usable-gameplay gain. A further two-worker trial using the observed main-client 4 GiB heap completed server measurement, then failed with Java heap space exhaustion on the client. It is a failed run, not a successful benchmark. The stuck disposable client was explicitly identified by its benchmark argument file before termination. The user's client was not stopped or modified.

The buffer/governor trials were exploratory and have different warmup/measurement windows; they are not a repeated controlled estimate of a final release improvement. Increasing lead time alone can let the controller consume ready terrain in bursts. More work is needed on DH data/mesh update throughput and memory pressure while keeping terrain visible, alongside chunk decoding/promotion; simply weakening the brake or reducing worker count is not an acceptable solution.

## Evidence

Ignored runs under civilization-mod/runs/flight-benchmark/results:

- 20260920-190954-readiness-r1-airship-5000-faab56 (baseline)
- 20260920-192115-primitive-r1-airship-5000-f4b8c8 (accepted code change)
- 20260920-192424-lead-buffer-r1-airship-5000-3626e9
- 20260920-192748-steady-buffer-r1-airship-5000-9f281b
- 20260920-193143-dh-workers-2-r1-airship-5000-af60ec
- 20260920-193338-optimized-final-r1-airship-5000-76b676
- 20260920-193600-optimized-dh2-final-r1-airship-5000-deac0d
- 20260920-193826-optimized-main-heap-r1-airship-5000-5cafc7 (out of memory; no successful summary)

Filtered JFR observations: .tools/hotspots-before.txt and .tools/hotspots-after.txt. Full GC summary: .tools/optimized-final-jfr.txt. The runner now supports explicit disposable-client DH worker and heap overrides, recorded in its manifest; DH settings are copied from the client. Following these experiments, the runner default now reads the configured Prism heap; explicit heap overrides remain available. [Benchmark protocol](../../civilization-mod/testing.md#controlled-fast-flight-benchmark). Final validation and deployment are recorded in [status](../status.md).

## Final accepted code and heap check

Hash collections only, with original buffer/governor and eight DH workers:

- 20260920-194340-hash-only-main-r1-airship-5000-6c2093: 4 GiB, 3,560 blocks/s travel, 30.6 FPS, 2.33 FPS 1% low, tick-work p99 44.68 ms, 14 tick intervals over 100 ms. Full JFR GC pauses 7,152 ms total, maximum 311.7 ms.
- 20260920-194822-hash-only-8g-r1-airship-5000-6628c8: 8 GiB, 3,922 blocks/s travel, 68.5 FPS, 12.22 FPS 1% low, tick-work p99 22.56 ms, no tick interval over 100 ms. Full JFR GC pauses 2,061 ms total, maximum 51.8 ms. Screenshot inspected with terrain visible.

Both used 20 seconds moving warmup and 20 seconds measurement, generated zero chunks and recorded no emergency rejections or dismounts. The 8 GiB run still had readiness braking in all measured ticks, body speed 2,035-4,592 blocks/s, 95.2% sampled nearby client coverage and a 175.7 ms maximum frame. This is a useful optimization, not a completed smooth-flight solution. These are single trials and the development JDK still differs from Prism's Java installation.

Installed 0.33.53 after 54 unit tests, 193 server tests and the focused Photon flight passed. Main instance maximum heap set to 8192 MiB on a 64 GiB machine, initial heap 512 MiB, with settings backup .tools/prism-instance-before-8g-20260920-195311.cfg. Prism remained in the tray after graceful close; its verified idle process was stopped before editing so cached settings could not overwrite the change. No Minecraft client was running. Main DH configuration was preserved. The benchmark now reads configured Prism memory automatically; 8192 MiB resolution was verified without another game launch.


## Follow-up: DH cache-state blind spot

Inspection of the installed DH 3.3.1 bytecode found that `processQueuedChunkPreUpdate` creates height maps, compares the stored chunk hash to `getBlockBiomeHashCode`, and queues conversion on mismatch. The main client has `disableUnchangedChunkCheck=false`, so unchanged detection is already enabled. Disabling updates or trusting LOD presence alone would risk stale terrain.

Read-only SQLite inspection found 721,002 FullData rows and **zero ChunkHash rows** in the prepared baseline. The final 8 GiB trial (`20260920-194822-hash-only-8g-r1-airship-5000-6628c8`) ended with 721,007 FullData rows and 13,904 ChunkHash rows. The synthetic renderable LOD fixture therefore does not model an already-validated DH cache. These counts cover the entire run, not only the measurement window, and do not quantify how much time a warmed route would save.

The existing profile still establishes DH render building/conversion, C2ME saved-chunk decoding and server chunk serialization as remaining work, but does not establish that all DH conversion is redundant in a real world. The runner now records pre/post database row counts. Next useful experiment is first versus repeat traversal of the same unchanged route with actual DH-produced hashes, while retaining readiness-brake and visible-terrain checks. No additional runtime optimization, configuration change or deployment was made during this investigation; 0.33.53-dev remains installed. No competing Minecraft process was launched.


## First/repeat DH database comparison

The `--cache-pair` runner now performs a fresh trial followed by a fresh JVM/Minecraft baseline retaining only the completed first trial's DH database via SQLite backup. Both use the same eastbound start, numeric power, 10-second stationary settling, 10-second moving warmup and 15-second measurement. The 8 GiB heap, eight DH workers, all source/harness/mod/config/shader/resource fingerprints matched. No other Minecraft JVM was found before launch. OS caches and other host activity remain uncontrolled; this is one diagnostic pair, not a statistically established gain.

- First: `20260920-225459-dh-cache-pair-r1-airship-5000-607bd9`.
- Repeat: `20260920-225645-dh-cache-pair-repeat-r1-airship-5000-dd6433`.

| Measured result | First | Retained DH database |
| --- | ---: | ---: |
| Actual blocks/s | 3,952.5 | 3,996.0 |
| Body speed range, blocks/s | 3,014–4,594 | 3,395–4,431 |
| TPS | 20.01 | 19.99 |
| Tick-work p99, ms | 25.17 | 22.80 |
| Tick intervals above 100 ms | 1 | 0 |
| Mean FPS / 1% low | 88.7 / 18.6 | 89.1 / 17.9 |
| Maximum frame interval, ms | 59.5 | 70.9 |
| Process CPU / host CPU | 71.8% / 79.3% | 70.2% / 76.9% |
| Readiness-brake sample prevalence | 100% | 100% |
| New Minecraft chunks / holds / rejected physics checks / dismounts | 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 |

Both screenshots were inspected: flat terrain is visible and the ship is intact. Client nearby chunk coverage averaged 95.3% and 94.2%. Measured x ranges differ because the governor remains active: approximately 30,055–89,116 and 31,309–91,072. Comparisons therefore cover mostly overlapping routes, not exactly identical per-tick locations.

The first database finished with 8,693 chunk hashes; the repeat began with those and ended with 14,948. Of the original entries, 8,692 retained both their hash and last-modified timestamp. This is evidence that the cache survives the fresh JVM, but does not prove each retained entry was revisited. The extra 6,255 hashes also show that one fast pass did not fully validate the route; this is a partial-cache comparison, not a fully warmed bound. FullData row counts were 721,007 after both trials.

JFR execution samples from the last 15 seconds of each recording (approximately the measurement window, not exact phase-aligned CPU accounting) showed DH totals of 3,319 → 3,154. Inclusive render-build samples were 1,403 → 1,381, chunk-to-LOD conversion 892 → 794, chunk hash calculation 384 → 404, C2ME chunk deserialization 464 → 455, and chunk serialization 132 → 146. These stack counts overlap and are statistical observations, not summed timings. Conversion remains substantial and mesh building remains the largest listed DH path. A 1.1% speed difference with nearly unchanged FPS does not justify a runtime change.

Validation: both benchmark launches completed, 600 measured physics checks each passed, all comparison fingerprints matched, screenshots inspected, Python corridor checks and documentation checks passed. No production code/settings or installed JAR changed; 0.33.53-dev remains installed. Next investigate how much conversion is for first-seen versus changed chunks and how render-section replacement can reuse work without dropping updates or terrain. Do not disable hash checks, updates or readiness braking based on this pair.


## Bottleneck breakdown from the paired captures

Reanalyzed both existing JFRs, without launching another game. `.tools/FlightBottleneck.java` exports execution samples, GC pauses, recorded file I/O and thread CPU events; `.tools/bottleneck-first.csv` and `.tools/bottleneck-repeat.csv` retain the extracted observations. The window is seconds 20–35 relative to JFR's `recordingStart`, approximately matching the measurement phase. The benchmark sets its monotonic epoch just after `Recording.start()` returns, so sub-tick event alignment is not established and individual frame/GC overlap is not claimed.

Each execution sample is assigned exactly one category, with DH render-build stacks first, then DH conversion/hash/height-map/other, chunk deserialization/serialization, and remaining thread groups. Percentages below are shares of sampled Java execution, **not exact CPU-time shares**, total machine utilization or wall-clock delays. Native/GC/GPU time is not represented by this sample denominator. There were 5,339 first-pass and 5,275 repeat-pass samples in the selected windows.

| Exclusive execution category | First | Repeat |
| --- | ---: | ---: |
| DH render-source/mesh building | 26.50% | 26.01% |
| DH chunk-to-LOD conversion | 16.78% | 15.05% |
| DH change hashing | 7.27% | 7.62% |
| DH height maps | 0.58% | 0.51% |
| DH other (updates, lighting, scheduling, etc.) | 11.22% | 10.20% |
| Chunk deserialization | 8.50% | 8.83% |
| C2ME other | 8.04% | 8.70% |
| Chunk serialization | 2.49% | 2.73% |
| Minecraft lighting | 3.93% | 4.49% |
| Client render thread | 5.39% | 5.23% |
| Airship server work | 0.36% | 0.49% |
| Other server work | 4.27% | 4.36% |
| Remaining threads | 4.66% | 5.76% |

DH totals approximately 62.4% and 59.4%. In the repeat's mesh category, leading leaf methods include `ColumnBox.makeAdjVerticalQuad` (235 samples) and `ColumnBox.applyLightToRangeAndPopulateNewSgements` (139). Conversion spends 474 leaf samples in `PalettedContainer.get` and 152 in `ConcurrentHashMap.get`. These are concrete work targets; the results do not establish a safe shortcut or prove that DH alone causes the readiness limit.

Measured server chunk-load events averaged 2,159.2/s and 2,155.2/s. These are event counts, not unique forward-corridor chunk completions. Available forward distance averaged 1,658.8 and 1,675.2 blocks, with minima 905.8 and 1,169.0. Ready snapshot entries averaged 751.3 and 761.8, with minima 426 and 553, against the 1,024-entry maximum. Readiness braking remained active in every measured tick despite approximately 20 TPS and modest tick-work p99. This identifies ready-terrain supply as the immediate speed constraint; the whole chunk-loading pipeline competes with substantial DH work.

The first-pass 23-second bin dropped to roughly 3,670 blocks/s, 1,467 ready-forward blocks and 656 ready entries, while its maximum tick work was only 22.3 ms. At the 3,014-block/s trough near 24.05 seconds, tick work was 14.3 ms and the tick interval 47.9 ms. Speed loss therefore occurs without a server tick freeze; it is not explained by simulation tick time alone.

Each selected JFR window recorded 30 top-level GC pauses: 765.0 ms total / 47.1 ms maximum first, and 774.3 ms / 45.3 ms repeat. That is about 5.1% of the 15-second window paused and remains a separate frame-stutter contributor. First recorded zero slow file I/O events; repeat recorded two (38.7 and 33.2 ms). Thresholded JFR I/O events cannot rule out disk or native I/O limits, but these captures do not establish disk latency as the primary bottleneck.

Conclusion: prioritize reducing DH mesh/conversion and allocation work while preserving updates, and instrument request-to-FULL latency to separate chunk decoding, scheduling and promotion. Do not weaken the readiness brake: it is responding to incomplete forward coverage. No production change or deployment resulted from this measurement. Documentation checks passed.


## Request-to-ready tracing: lighting queue is the immediate brake bottleneck

Added bounded development-only observations at corridor request, updated NBT future completion, deserialization, lighting/spawn step futures, C2ME FULL conversion/main-thread publication, chunk load events and first physics snapshot inclusion. Missing cells just beyond the ready-distance boundary are classified at each benchmark tick. Production safety, scheduling, tickets, futures and installed 0.33.53-dev remain unchanged. The final refinement wraps lighting PRE_UPDATE tasks only to timestamp execution and records whole `runUpdate` batch durations. The analysis is reproducible with `tools/flight-benchmark/latency_report.py`.

Three short captures progressively narrowed the unknown interval, each using a fresh pregenerated world, the main mods/shaders, 8 GiB heap, eight DH workers, 10-second moving warmup and 15-second measurement:

1. `20260920-230628-chunk-stages-r1-airship-5000-8fa33f`: all 657 blocker observations were after deserialization. Decode-to-load median 91.4 ms / p95 318.7 ms, versus 1.68 ms median NBT completion and 0.183 ms deserialization. Actual flight 3,967 blocks/s, approximately 90 FPS.
2. `20260920-230918-chunk-promotion-r1-airship-5000-15b4c1`: of 601 blockers, 471 were awaiting LIGHT completion, 44 between lighting initialization and LIGHT, and 86 between FULL conversion and main-thread publication. Initialization and LIGHT future durations had medians 34.1 and 29.2 ms. Actual flight 3,989 blocks/s, approximately 89.7 FPS.
3. `20260920-231152-light-queue-r1-airship-5000-90fd52`: splits the lighting futures into queue wait, PRE_UPDATE callback and subsequent batch/completion wait; results below.

Final run's 606 blocker observations:

| Current state of missing forward chunk | Observations | Share |
| --- | ---: | ---: |
| LIGHT requested, PRE_UPDATE not yet executing | 431 | 71.1% |
| Initialization complete, LIGHT not yet started | 89 | 14.7% |
| FULL conversion begun, main-thread publication not yet begun | 73 | 12.0% |
| LIGHT PRE_UPDATE started, LIGHT future not yet complete | 13 | 2.1% |

These are repeated chunk-at-tick observations, not unique chunks or CPU percentages. All were after deserialization. The missing frontier, not global chunk work, supplies this denominator.

Final requested cohort: 30,579 coordinates first requested during measurement. First-observation stage timings on completed, nonnegative endpoint pairs:

| Stage | Median ms | p95 ms |
| --- | ---: | ---: |
| Updated NBT request to completion | 1.89 | 19.05 |
| NBT completion to decode start | 1.71 | 8.21 |
| Deserialization | 0.179 | 0.392 |
| Decode end to chunk-load event | 105.04 | 339.65 |
| Initialization start to PRE_UPDATE execution | 35.81 | 89.00 |
| Initialization PRE_UPDATE callback | 0.0219 | 0.0515 |
| Initialization callback end to future completion | 5.43 | 16.82 |
| LIGHT start to PRE_UPDATE execution | 33.20 | 72.13 |
| LIGHT PRE_UPDATE callback | 0.0001 | 0.0002 |
| LIGHT callback end to future completion | 4.18 | 11.48 |
| FULL conversion start to main-thread publication start | 0.38 | 19.05 |
| Load event to first observed ready snapshot | 30.86 | 50.47 |
| Corridor request to ready snapshot | 151.59 | 299.05 |

There were 11,598 lighting batches in the final 15-second window, totaling 5,429.5 ms elapsed in `runUpdate` on the lighting engine, median 0.0559 ms / p95 1.1732 ms / maximum 58.0 ms. Tiny per-chunk PRE_UPDATE callbacks do **not** mean all lighting work is free: shared propagation, other chunks, scheduler contention and GC remain inside or between batch work. The measurements establish queue latency, not which batch-size/priority change will safely reduce it. The vanilla implementation performs PRE_UPDATE tasks, shared `runLightUpdates`, then POST_UPDATE completion; valid saved light can avoid source propagation but still traverses these barriers.

Final run: 3,838 blocks/s actual, 20.00 TPS, 94.9 FPS / 19.5 1% low, tick-work p99 23.16 ms, no tick intervals over 100 ms, and readiness brake sampled active throughout. Zero newly generated Minecraft chunks, emergency holds, rejected physics steps or pilot losses. Nearby client-chunk coverage was only 75.6%, so the higher FPS is not a demonstrated rendering improvement. All three screenshots show visible flat terrain and intact ships. No timing caps were reached (final 66,641/131,072 traces and 975/20,000 blocker rows). Development compilation and all three runtime captures passed; Python corridor checks and documentation checks passed. These are instrumented diagnostic runs with changing instrumentation, not an optimization A/B test.

First timestamps are retained by coordinate, not reload lifecycle. Stage quantiles use different complete subsets and cannot be summed. Earlier C2ME/player loads can predate corridor requests. Incomplete or never-snapshot entries are censored; they may have left the corridor rather than failed to load. Future callbacks can race, and negative intervals are omitted by the report. Snapshot appearance is sampled at tick end, not the exact publication instant. Observation overhead is present and has not been separately subtracted.

This refines the previous CPU-based priority: DH remains a major CPU consumer, but the immediate readiness frontier is dominated by the **server lighting queue and its completion barriers**. Next optimization should test bounded, timely lighting service/prioritization for required forward chunks, retaining dependency ordering, shared light propagation, FULL completion and immutable physics readiness. Reducing deserializer cost or weakening the brake does not address the measured dominant wait. A concrete scheduler change and before/after latency test are still required before claiming an improvement.


## ScalableLux installation compatibility check

The user installed `ScalableLux-neoforge-0.3.0-alpha.0.8-all.jar` (declared version `0.3.0-alpha.0.8+1.21.1`). An isolated benchmark launch copied that mod with the rest of the main stack and failed before world startup: `20260920-231647-scalablelux-r1-airship-5000-c47bb4`. NeoForge reports that Sable is incompatible with ScalableLux. Inspection of the actual installed `sable-neoforge-1.21.1-2.0.5.jar` confirms `[[dependencies.sable]]`, `modId = "scalablelux"`, `type = "incompatible"`. This is an explicit loader restriction, not merely an untested combination. No performance measurement was produced. The launch also reported benchmark mixin configuration initialization failure after mod rejection; no successful runtime instrumentation or compatibility claim is made.

The earlier suggestion to test ScalableLux missed this Sable metadata restriction. Temporary ScalableLux-specific diagnostic accommodations were removed. No incompatibility override, Sable modification, world edit, deployment or main-client mod removal was performed. The user subsequently uninstalled ScalableLux; its absence from the main mods directory was verified before the following trials. Lighting-queue optimization within the existing supported stack remains the next candidate.


## Bounded lighting scheduling trials

Three development-only scheduler experiments used fresh isolated JVMs, pregenerated terrain, the main client mod/shader stack, 10-second settle plus 10-second warmup, and 15 seconds of physical flight at a requested 5,000 blocks/s. Original lighting dependencies and completion barriers remained intact.

| Trial | Actual blocks/s | Ready latency median / p95 ms | LIGHT queue median / p95 ms | Mean FPS |
| --- | ---: | ---: | ---: | ---: |
| Instrumented reference `231152` | 3,838 | 151.59 / 299.05 | 33.20 / 72.13 | 94.9 |
| Batch size 128 `232025` | 3,662 | 148.08 / 551.26 | 16.33 / 138.24 | 78.8 |
| Urgent corridor priority `232239` | 3,931 | 147.16 / 319.09 | 22.82 / 74.90 | 88.3 |
| Normal drain prompt, at most every 5 ms `232438` | 4,121 | 104.16 / 206.42 | 19.54 / 52.08 | 29.5 |

Full result directories under the ignored benchmark results root: `20260920-232025-light-batch128-r1-airship-5000-b7efdb`, `20260920-232239-light-priority-r1-airship-5000-ab9bb5`, and `20260920-232438-light-prompt-r1-airship-5000-29dba9`.

Smaller batches worsened tail latency and produced a 196 blocks/s minimum. Priority alone did not improve p95 readiness. Prompting the existing mailbox drain improved observed p95 readiness by 31% and actual speed by 7.4%, but FPS was much worse; nearby client coverage also rose from 75.6% to 96.0%. This single-run comparison does not establish the cause of the FPS change or isolate rendering cost from scheduling and external load. The prompt trial held approximately 20 TPS, p99 tick work 25.90 ms, zero intervals over 100 ms, no generated chunks, holds, physics rejections or dismounts. Its screenshot showed continuous visible terrain and an intact ship. The readiness brake remained active throughout.

None meets the combined smooth-flight and frame-rate acceptance criterion. All scheduler mutations and their diagnostic urgency helper were removed; observational telemetry remains. No production JAR was changed or deployed. A repeatable client frame-stall diagnosis with comparable terrain coverage is needed before adopting the promising drain prompt. These short trials are screening evidence, not statistically established effects.


## Turn-aware incremental corridor preparation

A new preparation strategy retains the original 1,024 direct tickets per ship / 4,096 per world. Its narrower straight corridor spends fewer cells on lateral padding, while retaining the 28-block readiness footprint. Steering reserves 640 cells along current momentum, then adds a wider curved forecast using hull facing, yaw response and lateral drive acceleration. Admission is capped at 128 new direct tickets per ship per tick; retained tickets do not consume that admission allowance. Lookahead reacts to observed readiness delay within fixed bounds. No changes to lighting scheduling, propulsion, collision protection or readiness acceptance.

Matched fresh-JVM, fresh-world 15-second trials at nominal power target 5,000 blocks/s:

| Metric | Previous corridor | New corridor | New corridor repeat |
| --- | ---: | ---: | ---: |
| Actual wall blocks/s | 3,617 | 5,741 | 5,438 |
| Body speed p05 | 1,775 | 4,652 | 3,291 |
| Body speed minimum | 392 | 2,475 | 1,311 |
| Average ready distance, blocks | 1,517 | 2,419 | 2,272 |
| Average checked cells | 1,024 | 1,024 | 1,024 |
| Average ready cells | 685 | 680 | 648 |
| Mean FPS / 1% low | 86.3 / 16.3 | 76.6 / 17.1 | 87.5 / 17.3 |
| Tick work p99, ms | 25.50 | 22.27 | 28.80 |
| Process CPU, percent | 70.7 | 71.6 | 71.9 |
| Nearby client coverage, percent | 94.0 | 69.6 | 59.4 |

Runs: `20260920-233259-corridor-before-r1-airship-5000-10a85e`, `20260920-233453-corridor-predicted-r1-airship-5000-ea3dc6`, `20260920-233643-corridor-repeat-r1-airship-5000-1c922d`. First before/after mod, shader, resource-pack, config and heap fingerprints matched. About 20 TPS in all three; zero new chunks, emergency holds, physics-step rejections and measured pilot detachments. The repeat had one 102 ms tick interval. The readiness brake remained active in every measured tick: this improves preparation reach and throughput, not completely smooth or brake-free flight. The numeric target controls test power; it is not a speed cap.

Screenshots inspected: both candidates show continuous terrain. The first candidate screenshot has no visible deck; the repeat shows the deck/controller normally. Server pilot attachment is not proof of uninterrupted client ship rendering. Reduced nearby client coverage, unequal distance traversed and host load prevent interpreting FPS as a controlled equal-rendering-cost comparison. Peak speed, dense terrain, sustained sharp turns and multiplayer need separate validation.

The first candidate's request trace marks planned cells before batch admission; do not interpret that run's request latency as admitted-ticket latency. The repeat fixes this development hook to observe owned tickets after update. Production code is identical in those two runs.


The steering trial `20260920-233821-corridor-turn-r1-airship-1000-678e67` applied two short opposing inputs in the measurement window. Actual mean speed 1,677 blocks/s, 20 TPS, 104.6 mean FPS / 29.3 1% low, z changed from 2.0 to 61.2 blocks. Zero generated chunks, emergency holds, physics-step rejections or dismounts; brake sampled active 16.1% of the time. The eastbound-only legacy snapshot-fit column was false, so it is not used as a curved-path safety result. Actual-velocity physics safety checks passed. Screenshot showed terrain, deck and controller. This exercises mild steering, not sustained sharp turns at 5,000 blocks/s.

Final build and all 57 unit tests / 194 server GameTests passed, including new diagonal/both-turn-direction coverage, prediction-versus-readiness separation, ticket batch admission, retention, reduced world budget and cancellation cleanup checks. Python corridor checks passed. Version 0.33.54-dev was deployed to Civilization Dev with matching JAR hash; no user worlds or settings changed. Restart the client to load it.
