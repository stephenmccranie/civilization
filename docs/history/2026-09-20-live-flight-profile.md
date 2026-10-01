# Live flight profile - 2026-09-20

Read-only JFR capture of the user's running Civilization Dev session, PID 23220, 18:59:39-18:59:54 America/Denver (15 seconds). No settings or gameplay code changed. Installed build 0.33.52-dev.

Java process CPU averaged 91.0% of machine capacity across JFR CPU samples, peaking at 100%. Of 4,006 Java execution samples, 1,660 were C2ME threads (41.4%), 1,019 DH threads (25.4%), 418 server thread (10.4%), 208 lighting thread (5.2%), and 191 render thread (4.8%). These are sampled execution proportions, not exact CPU allocation or elapsed-time shares.

Full-stack consumer analysis found 641 C2ME samples inside ChunkSerializer.read and 268 inside FlatLevelSource.fillFromNoise: both saved-chunk decoding and actual flat terrain generation were running. DH samples included 449 in LOD render-data building and 317 in chunk pre-update processing. Main-thread distance-manager/ticket processing was prominent. AirshipTerrain.update appeared in 104 server samples; immutable-set probing and Long equality were visible leaves, making our readiness bookkeeping a further optimization target. Inclusive stack counts overlap and must not be added.

There were 25 stop-the-world GC phase pauses, totaling 512.4 ms (3.4% of the capture), maximum 35.5 ms. GC can contribute visible stutter but does not alone explain the sustained CPU saturation. One recorded server monitor wait of 25.0 ms was in the chunk-holder lookup path used by getChunkNow. No exact tick-duration or frame-interval stream was enabled in this initial live capture, so it cannot supply accurate TPS/FPS or count readiness-brake activations.

A later 15-second ship-state sample began at approximately 19:00:21. It found the ship unpiloted, forward input zero, coasting from 1.85 to 0.13 blocks/s, with no emergency hold. This misses the active flight and does not establish whether anticipatory braking occurred earlier. The temporary probe's new readiness columns were not inserted correctly in its row expression; its CSV header was corrected to match the fields actually captured. Probe source is corrected for a future capture; no fabricated brake/readiness data is inferred.

Conclusion: terrain-production/loading pressure supports the user's readiness-braking hypothesis, but direct correlation was not captured. Next optimization targets are bounded corridor/ticket bookkeeping, competing DH update/build work and chunk decoding/generation throughput. Keep the safety guard; do not treat missing terrain as ready.

Ignored raw evidence: .tools/airship-diagnostics/live-flight-current.jfr, live-initial-summary.txt, live-flight-current.json, live-readiness-15.jfr and live-readiness-15.csv. The JFR command initially parsed the space-containing destination incorrectly; its complete 15-second recording was recovered unchanged into the intended diagnostics directory. The subsequent recording used a quoted destination.

## Subsequent benchmark comparison

Comparison against final test 20260920-185059-smooth-final-r1-airship-700-5f059d found no changed enabled dependency-mod hashes. DH, C2ME and Sodium configurations match byte-for-byte; Iris differs only in its saved timestamp. Render distance 12, simulation distance 5, VSync and 120 FPS cap match. These file comparisons describe settings on disk, not proof that every runtime option was unchanged during flight.

The benchmark has all route chunks FULL and a prebuilt DH database; its measured new-chunk count is zero. Live flat-terrain generation is present in JFR, so it exercises additional work the successful pregenerated trial excluded. The benchmark also follows a fixed straight eastbound route after 20 seconds of moving warmup. Live heading, actual speed and instantaneous braking were not captured, so equivalent demand/route changes cannot be assumed. The later live controller sample showed 1e10 W; the final benchmark screenshot showed approximately 1.67e9 W, but this does not prove the power throughout the earlier live capture.

The live JFR records Java 21.0.7 and a 4 GiB maximum heap. The development run used Java 21.0.12.1 and 6 GiB. These are further mismatches, not a demonstrated explanation by themselves. The benchmark did show client slowdowns: 31.6 FPS and 7.0 FPS 1% low; only body velocity and server tick continuity were steady.

A telemetry blind spot was confirmed in code: held sampled only Drive.hold (emergency stops), while the gradual governor sets Drive.streamingBrake. Therefore zero emergency holds does not establish zero readiness braking. The benchmark now records that flag separately, forward ready distance, checked/ready chunk counts, and brake sample prevalence/episodes; legacy samples report unknown. Java fixture compilation and synthetic summary checks passed. This is development-only instrumentation, not a deployed gameplay change. New runtime telemetry has not yet been exercised in a full client trial.
