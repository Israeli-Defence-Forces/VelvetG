# CombatIQ audit — PR #2 d44f393 (2026-10-21)

Source retrieved through the GitHub connector at the pinned head. This is a text-source snapshot, not a clone with upstream history. PR #2 remains open/draft. Both supplied ZIP downloads were rejected by the transfer tool because they exceed 32 MiB. Neither checkpoint contents nor the historical CSV have been examined. Existing r8 workspace binaries are different artifacts and are not substitutes for the SHA-pinned v3 baseline.

## Execution inventory

| Stage | Actual implementation / ownership | Verification and defect |
|---|---|---|
| Keyboard | CombatIQNative.onKeyboardDispatch reads LWJGL event proxy | Mock-tested, W=17 hardcoded; repeats preserved for replay. Not physical latency. |
| Binding / movement | onMovementSample reads settings.Y.d(), inherited movement field b after external SnapTap callback | Mock-tested inherited-field regression. Original movement/keyboard/tick bridge and actual SnapTap ordinal live in the unavailable v3 baseline. |
| Context | onContextPoint reads local player, GUI, focus; publishes epoch/flags | Previously callable on any producer thread. Reject foreign callbacks before reading/writing context; bootstrap ownership only at client tick. |
| Sprint | PlayerMixin bew.d(Z)V HEAD/RETURN and bew.m()V RETURN | Mock setter pairs; method descriptor verifier exists. Previously onPlayerPoint mutated context before local identity check. Fixed check order. No actual Mixin application here. |
| Attack / use | MinecraftMixin ave.aw()/ax() HEAD | Intent only; not raw left/right press or confirmed attack. |
| Packets | PacketProbe ClassValue layouts for action enums, velocity/status/movement | Cache per runtime class; bounded allowlist. Outbound kind 5 means send attempt; inbound kind 6 precedes filtering. Baseline owns interception. No socket writes or connection identity. |
| Handler | HandlerMixin bcy.a(packet) RETURN, kind 15 | Method return on executing thread, not server outcome. Missing local entity may still return. |
| Serialization | CombatIQNative MPSC primitive ring (16,384 slots), GATE, one daemon writer | Enqueue order and producer ID retained; disk I/O outside GATE. Initialization and hook registration failures need hardening. File-size cap counts dropped queued rows; row cap can overshoot while draining. |
| Lifecycle | static enabled flag, one startup CAS, JVM hook, duration/size/row limits | Default off. No restartable native module lifecycle or connection reset. Disk errors leave partial CSV, recognized offline, but loss footer cannot be guaranteed on disk failure. |
| Mixins | Three additions to shared mixins.soar.json via build_v4.py | Hard require=1/5 can crash optional telemetry. Changed to require=0; absent callbacks remain unknown offline. Actual initialization logs unavailable. |
| Build / loading | compile all sources, package recursive classes, SHA-pinned v3 and official Minecraft descriptors | Includes anonymous/named siblings and Java 8 majors. Fixed changed-entry assertion assumes exactly two anonymous recorder siblings. Baseline absent; synthetic checks possible. |
| Install | upgrade_v4.py exact hashes, transactional replace, backups, --game-dir, rollback | Eight synthetic installer tests pass. Historical manifest applies only to historical binary; no new binary generated. |
| Analyzer | combat_analyzer.py schema 1/2 parser, sprint pairs, releases, sections | Existing 19 replay tests pass. Block-hit/combo are aggregate summaries, not full reconstructions. Cross-thread release candidates, entity mismatch in setter stacks, negative timestamps and empty-trace validity need regression coverage. |
| GUI | overhaul/lean screens/settings; actual native Mod architecture in external client | No CombatIQ native module/HUD in PR. Source adapter can be added only with verified native conventions; do not claim disconnected panels as integrated. |

## Baseline checks

JDK 21 found in prior workspace toolchain. Initial suite: 38 tests, 37 passed, historical CSV test skipped. Standalone kernel: 60 assertions; protocol: 3 tests passed. Native runtime tests compile Java 8 bytecode and execute a mock JVM, not Minecraft.

## Remediation order

1. Enforce context/client-state ownership and local identity before mutation; keep packet producers isolated. Regression: integrated-server callbacks during a GUI boundary must emit nothing and must not advance epoch.
2. Make optional added injections nonfatal, preserve unknown coverage, validate static descriptors where baseline permits.
3. Harden parser and deterministic reconstruction with independent evidence and separate analyzer modules; prevent cross-thread/cross-entity false matches.
4. Add offline calibration and benchmarks with honest uncertainty; harden caps, startup and packaging. Extend meaningful synthetic tests.
5. Native module, full binary verification, Minecraft/Mixin application, Windows/FPS/server trials require missing verified artifacts and runtime; retain exact blockers in PROGRESS.md.

Mutable ownership: client thread owns reflection handles, previousPlayer/context, sprint state, input/ticks. Packet ClassValue layouts are immutable; callback error latches are atomic. All ring slots, sequence, head/tail and drop accounting are guarded by GATE; volatile head/tail publish to writer. Writer exclusively owns CSV/path/row counters. Cross-thread timestamps establish observation proximity only. Epoch is context epoch, not verified connection identity.

## Implemented follow-through

The published continuation now includes the ownership/Mixin fixes, verified remapping, restartable recording and immutable snapshots, exact row/UTF-8 caps, explicit filtered/lost counts, sibling packaging checks, native HUD source adapter, four deterministic analyzer modules, dense-window bounds and abstention, calibration and benchmark tooling. Latest local validation: 72 tests (71 pass, one historical CSV skip), 60 kernel assertions and 3 protocol tests. An available official Minecraft JAR matches the pinned SHA; expanded structural checks pass. The optional native module API-compiles against a different local r8 client and is not live-validated. See PROGRESS.md for remaining features and exact artifact/runtime blockers; full Phases VI–IX acceptance is not claimed.
