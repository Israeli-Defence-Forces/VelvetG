# Performance evidence and local comparison protocol

No Minecraft launch, FPS increase, 1% low, GPU timing, real startup reduction or real overlay rendering-overhead result was measured in this cloud environment. There is no basis to promise 120 FPS or a percentage gain. The Linux Java 8 tests and synthetic microbenchmarks are separate from gameplay measurements.

## Evidence collected here

- Exact original and patched Java 8 classes verify, including inherited startup/module classes. The historical json method's behavior is preserved.
- The patched screenshot constructor does no file enumeration, deletion, image decode, network I/O or GPU work. Actual constructor invocation passes without game/graphics initialization. A temporary directory containing 501 corrupt/placeholder PNGs is discoverable without decoding them or deleting files.
- The text-cache test uses a **mock metric provider**: 1,001 identical width requests invoke the provider once; size/font/context changes invalidate it, and unique entries remain bounded. This proves cache behavior, not NanoVG render-time savings.
- A burst of 1,000 property changes produces one atomic final write, preserving an unknown Unicode property. This is measured filesystem behavior under the isolated test, not an FPS result.
- Existing scheduler tests use a local HTTP server and real client scheduler/JSON parser, including deduplication, stale retention, cooldown and bounded queues. Mock network timings are not real Pika latency measurements.
- `offline-benchmark.txt` contains the actual current Java 8 microbenchmark output: disabled-probe entry cost and synthetic 40-row preparation. These loops exclude GPU calls, font rasterization, API work, Minecraft and normal scene rendering. JIT, host scheduling and synthetic inputs materially limit their meaning. They cannot be used to estimate game FPS.

## Final Overhaul3 CPU/allocation comparison

The earlier 466/659 ns numbers came from a short synthetic harness with a volatile sink on each call. That result did not establish a game/frame-time regression or isolate layout, fonts, sorting, GL state or allocations. Code inspection did confirm an avoidable default roster copy and exceptions when parsing inherited numeric settings in rendering controls.

`tests/benchmark_preparation.py` now compares the exact published Overhaul2 JAR with the final candidate, alternates run order across five independent JVM forks per variant/workload, warms each fork for 1.5 seconds, and retains seven 250 ms samples per fork. All JVMs use actual Java 8, identical dependencies and a fixed 128 MiB test heap (not a recommended Minecraft setting). `ThreadMXBean` allocation counters report bytes per operation. There were no concurrently running project test processes during this final run; host scheduling/load is uncontrolled.

| Synthetic operation | Overhaul2 median of fork medians | Overhaul3 median of fork medians | Median allocated bytes, 2 → 3 |
|---|---:|---:|---:|
| Default preparation of an existing 40-row list, no explicit sort | 134.124 ns | 3.897 ns | 200 → 0 |
| One inherited font-size plus one inherited entry-duration lookup | 4457.114 ns | 39.369 ns | 1264 → 0 |

The first result measures the new no-copy fast path. It does not measure constructing/rendering a 40-player table. The second isolates sentinel parsing (EXISTING), which previously allocated NumberFormatExceptions. Explicit sorting, native font measurement/drawing, GPU work, networking, scene rendering and Minecraft initialization are excluded. Do not turn these ratios into an FPS or startup claim.

All samples, hashes, fork medians, min/max and standard deviations are retained in `preparation-comparison.json`. Variance remains substantial: roster sample ranges were 71.327–270.844 ns (2) and 2.657–50.367 ns (3); settings ranges were 2466.244–22414.639 ns (2) and 31.963–578.247 ns (3). Every paired fork median favored the new paths, and the zero-allocation result is consistent with the removed copies/exception branches. There is no discarded outlier or cherry-picked best run.

The local HTTP comparison also executes against final binaries: original/Overhaul2/Overhaul3 issue **3/1/1 profile requests** for one player across three selections. This preserves the earlier coalescing fix; see `profile-comparison-overhaul3.json`. It is not a live Pika latency/authentication measurement.

Startup startGame/splash/Glide/module timelines are persisted after completed startup, and the profiler provides resettable local start/stop/export commands plus the last 4096 raw loop intervals. WINDOWS-SMOKE-TEST.md explains equivalent-scene Overhaul2/3 runs and fresh-export checks. The native head query/temporary arrays and hidden-TAB model guard are verified structurally/behaviorally, with no native GPU timing result claimed.

## Overhaul2 comparison executed here

`build-comparison.json` preserves the historical Overhaul2 run with its exact hashes and raw results. It is not regenerated from Overhaul3. The current comparison script labels the rebuilt candidate Overhaul3; use the release source/JAR matching a historical hash when reproducing that older experiment.

| Workload | Original | Overhaul1 | Overhaul2 |
|---|---:|---:|---:|
| Profile HTTP requests for the same player across three stat selections, local HTTP server | 3 | 3 | 1 |
| Median synthetic 40-row preparation, 3 JVM runs, ns/call | Not applicable: original has no TableSupport helper | 465.775206 | 658.568584 |

The CPU microbenchmark did **not** improve in this run. Individual samples vary substantially; all are recorded, without discarding slower results. The additional context-aware preparation must not be described as a measured speedup. This tiny isolated workload does not measure table construction, font/layout rendering, frame times, startup, native driver work or game FPS.

The HTTP comparison is an executed request-count result against loopback fixtures. It demonstrates coalescing, not production Pika latency or authenticated availability. Eager cape-preview preload elimination is proven by bytecode guards; no startup-seconds saving is measured.

## Expected mechanisms, still requiring runtime measurement

Background image preparation, bounded GPU uploads, text-measurement reuse and coalesced writes should reduce particular render-thread and disk workloads. Original lazy screenshot startup had already removed bulk decoding, so the old reported 3–7 minute startup delay cannot honestly be attributed entirely to this new patch. Startup checkpoint measurements are needed to locate remaining font, NanoVG/context, custom-cape, resource-pack, module and driver delays.

Maximum FPS changes optional table presentation only. It does not alter gameplay packets, movement, render visibility, entity logic, SnapTap or the FPS counter. No assumption that every alpha blend or animation is expensive is used as a measured result. Explicit modes allow the user to compare equivalent scenes with effects on/off.

## Local protocol

1. Preserve the original version. Choose the same maintained Java 8 executable for both versions. Keep heap/GC, video options, resolution (e.g. 1280×720), resource packs, server/world, route, view direction and FPS cap identical. Record GPU selection and driver once.
2. Perform at least three cold-start runs and three warmed scene runs per build. Do not call a menu-only interval a world-startup measurement. Avoid changing several options between runs.
3. Collect process CPU/memory/thread samples with RECORD-BENCHMARK.ps1 for 60 seconds after a consistent warm-up. CPU is process CPU expressed relative to one logical core, so values can exceed 100%.
4. In the patched build, enable the profiler and export after the same scene. Frame interval average/P95/P99 and current-thread CPU are distinct. TAB/HUD samples are CPU-side sections around their native draw methods; they exclude a proper GPU timestamp query. GC counts/time are lifetime JVM totals; compare start/end deltas for matching runs.
5. Repeat with Maximum FPS off/on, TAB held/not held, HUD hidden/visible, 1/20/40 players, optional backgrounds/fonts/transitions enabled/disabled. The native fetch cap and HUD cap both need appropriate values for the 40-player case. Check input responsiveness and layout while async results arrive.
6. Test empty and hundreds-image galleries, rapid navigation, corrupt/unreadable files, disconnect/reconnect, missing key, manual entry and optional /apikey capture. Preserve image files and verify texture counts settle after 120 seconds idle. No authentication key should appear in chat, logs or exported diagnostics.
7. Measure baseline frame times with a trusted existing frame recorder or an equivalently instrumented local build before asserting a change in 1% lows. An average FPS counter alone cannot supply P95/P99. The supplied process sampler measures CPU/memory, not frames.

No automatic JVM-argument change is shipped. The collection contains both -Xmx2G and -Xmx3G from historical runs plus tuned G1 flags; that is not evidence those settings are optimal. Keep one heap limit and a consistent GC configuration while collecting the first comparison, and use the installed maintained Java 8 (8u504 was reported) instead of the old 8u51 from historical crash reports. Further heap/GC recommendations require actual memory/GC samples.
