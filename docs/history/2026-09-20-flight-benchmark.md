# Prepared superflat flight baseline - 2026-09-20

This is a bounded experiment, not a capacity guarantee. The reusable procedure is in the [benchmark protocol](../../civilization-mod/testing.md#controlled-fast-flight-benchmark). Each row is one 20-second measured run after 10 seconds stationary settling and 5 seconds moving warmup; no statistical repeat study was performed.

## Fixture and runtime

- Useful route: 262,144 blocks east, 2,048 blocks wide, with 1,024-block end margins; 2,113,536 fully generated/lit chunks in 2,064 regions (8,673,951,744 bytes). All region corner readbacks passed.
- DH cache: 721,002 validated uniform flat sections across detail levels 0-12, 4,096-block lateral/end LOD margins, 340,533,248 bytes. Each run gets fresh independent Minecraft and DH file copies. No new chunks were generated during any measured run below.
- Ryzen 7 5700X3D (16 logical processors), Radeon RX 7900 GRE. Hidden 1920x1080 rendering; project Java 21.0.12.1, 6 GiB maximum heap, G1 and JFR profile recording. This differs from the Prism launcher JVM and is held constant between these trials.
- Enabled main-client mods/configuration copied: Lithium 0.15.4, Sodium 0.8.13, Iris 1.8.14-beta.1, DH 3.3.1, C2ME 0.4.0-alpha.0.122, FerriteCore, Jade, JEI, JourneyMap, Lithostitched, Sable 2.0.5 and GeckoLib 4.9.3, plus Civilization 0.33.51-dev from the development source. Disabled Terrain Diffusion/Terralith remained excluded. Actual loaded-mod manifests were inspected.
- Photon 1.3b and its user shader options, Faithful resource pack, main-client graphics options including 120 FPS cap/VSync, 12-chunk render distance and 5-chunk simulation distance. DH background generator plan was DISABLED with eight threads; prepared cache avoids timing empty terrain.
- Source, mod and copied-configuration fingerprints match across the five trials. OS disk cache and global mod/shader caches were not reset.

## Measurements

CPU is the integrated game Java process averaged across all 16 logical processors, not host-wide CPU. Client coverage is the per-frame sampled mean of the 3x3 chunk cache around the camera. FPS is derived from frame intervals, with 1% low from the slowest 1% of intervals.

| Mode / order | Requested b/s | Actual b/s | TPS | Tick p95 ms | CPU % | FPS | 1% low FPS | Client coverage % |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| airship / directional | 200.00 | 161.57 | 20.00 | 3.56 | 24.29 | 117.43 | 52.80 | 100.00 |
| stream / directional | 200.00 | 200.00 | 20.00 | 2.10 | 23.79 | 118.02 | 62.49 | 100.00 |
| stream / directional | 1000.00 | 1000.06 | 20.00 | 4.07 | 37.64 | 112.47 | 36.67 | 100.00 |
| stream / directional | 5000.00 | 4992.86 | 19.97 | 40.95 | 55.81 | 114.48 | 44.57 | 11.52 |
| stream / spiral | 5000.00 | 4997.53 | 19.99 | 47.09 | 55.08 | 115.75 | 42.73 | 7.55 |

The real airship uses constant power estimated from the requested speed. It sustained approximately 162 blocks/s with zero measured holds or dismounts; the 200-block/s request is not a forced trajectory. Streaming mode moves a spectator independently of physics, so its requested speed is not proof an airship can sustain it.

At 5,000 blocks/s, prepared DH terrain remained visible while ordinary chunk loading fell behind: only 11.5% mean client near coverage with directional ordering (7.5% in the ordering-off check). The server predicted sweep was only about 1.3% loaded. Tick work p99 was 53.0 ms directional versus 79.8 ms spiral, with two versus nine tick intervals exceeding 100 ms. The off check recorded zero reordered C2ME iterators versus 356 enabled, proving the toggle works. One unpaired run of each is insufficient to claim a reliable scheduling improvement. Neither high-speed run qualifies as usable full-chunk streaming.

A separate diagnostic physical-airship run requested 5,000 blocks/s and stayed held for the entire measured phase: the required predicted sweep exceeds the current eight-chunk physics snapshot. That run preceded DH-cache preparation and is excluded from the rendering table. Improving that physical limit is separate work.

The initial fresh-DH and incomplete-mod smoke runs are excluded. The earlier client-readiness probe used Minecraft's always-true client hasChunk method; final rows instead query the actual chunk cache. Final airship and 5,000-block/s screenshots were inspected for visible terrain and active shaders. Logs retain Sable's missing optional Create-block tag messages; the airship startup also logged one early unknown-sublevel movement packet, with no measured pilot detachment.

## Evidence

Ignored local results and immutable input baseline are under `civilization-mod/runs/flight-benchmark/`. `lod-report.html` links all raw CSVs, JFR profiles, settings/manifests and screenshots; `lod-report.json` stores the numeric summary. The five runs are:

- `20260920-175512-lodcheck-r1-airship-200-bfe9f3`
- `20260920-175652-lodbaseline-r1-stream-200-cf5ea1`
- `20260920-175805-lodbaseline-r1-stream-1000-b31973`
- `20260920-175923-lodbaseline-r1-stream-5000-ee8836`
- `20260920-180102-lodordercheck-r1-stream-5000-34f3ba`
