# Executed checks

Compiler: OpenJDK 21 with `--release 8`. Runtime: actual Temurin Java 8u504-b01, `-Xverify:all`. Tests use the authentic signed Minecraft 1.8.9 JAR and cached official launcher dependencies. Test entry classes are in their own package so they do not conflict with Minecraft's signed default package. No Minecraft JAR signing or native loading rule is changed for installation.

Latest detailed Java output: `test-results.txt`. Current build:

- 52 modified/new Java 8 classes pass ASM method analysis and JVM declaration/verification checks.
- 1,956 inherited classes remain byte-identical; 2,008 classes parse; no original ZIP entries are removed.
- Every injected reference to a new helper resolves to its actual descriptor. The original Pika `json` instructions are unchanged; its verification repair remains valid.
- 285 baseline and 315 patched Scout/module/screenshot/new-helper classes pass inherited startup declaration checks without game initialization.
- 162 behavioral assertions pass: 85 kernel/security/HTTP/image/bounds, 48 existing-scheduler mock integration, 18 cache/gallery/profile/cooldown, 3 configuration-write and 5 mocked Discord lifecycle and 3 real-profile-worker generation cancellation checks.
- The local HTTP tests cover 200/204/400/401/403/404/429/500/302, no redirect, authentication headers, safe key rejection/rotation state, supported modes/periods, timeout, malformed/partial/empty data, invalid/null names, deduplication, fresh/stale cache behavior, bounded retry/admission, disconnect cancellation and global cooldown.
- Gallery tests cover no-I/O construction, empty/501-file discovery, corrupt/non-directory handling, on-demand image downsampling and preservation. They do not execute native texture uploads.
- Cache tests use mocked width measurement and prove reuse, font/size/context invalidation and bounds. Discord pipe tests use a fake IPC transport; no Discord instance is contacted.
- Actual PowerShell 7.4.13/Linux and Java 8 run the Windows installer logic in a temporary launcher layout with spaces, `!` and `&`. They exercise install, entry verification, original/config/screenshot/profile preservation, metadata SHA-1/size, refusal to overwrite, tamper detection, guarded rollback, reinstall after rollback and wrong-target refusal. See installer-test-results.txt.
- The process sampler executes against an owned dummy process for 10 seconds and produces CPU/memory/thread CSV rows without command-line fields. This is not a Minecraft benchmark.
- Python scripts compile; batch wrappers use quoted script paths, disabled delayed expansion and explicit exit-code propagation. cmd.exe / Windows PowerShell 5.1 execution has not been performed.

Offline microbenchmarks are recorded separately and are not FPS/render/launch measurements. Live authentication, Minecraft launch, native/GPU context/state/lifecycle, actual launcher selection/download behavior and gameplay performance remain unverified. Tests of source or bytecode alone are never counted as a successful game launch.
