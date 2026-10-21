# CombatIQ engineering checkpoint

Implemented and published seven continuation checkpoints on `combatiq/phase4-hardening`, starting from PR #2's verified head `d44f393899c4ea476ff8b0d25f64d5393a0c8a3a`. [Draft PR #3](https://github.com/Israeli-Defence-Forces/VelvetG/pull/3) remains experimental and unmerged. Code validation covers commit `3cf47c3eb6c0388390d91166b9c2f1c77a1e5eea`; the later delivery commit adds reports and the source archive.

Local results: **72 tests run, 71 passed, one historical recording test skipped**. Also passed 60 standalone kernel assertions and 3 protocol tests. [Hosted CI](https://github.com/Israeli-Defence-Forces/VelvetG/actions/runs/39559227348) passed Java **8, 17 and 21** on that code commit. All 15 native source/tool/module classes compiled to Java 8 class major 52. Official Minecraft 1.8.9 SHA-pinned descriptors and packet layouts pass structural verification. Actual Minecraft and Mixin application have not been tested.

| Subsystem | Implemented? | Compiles? | Tests passed? | In Minecraft? | Ready for user testing? | Remaining blockers |
|---|---|---|---|---|---|---|
| Client ownership / sprint probes | Yes, guarded local observations | Java 8 bytecode | Mock concurrency/sprint regressions | No | Source tests | Exact v3 bridge and live injection |
| Input / packet telemetry | Partial; remapping and allowlisted actions | Java 8 bytecode | Remapping, action layouts, thread guards | No | Source tests | Raw mouse/press overlap, connection identity, actual transport |
| Recorder | Bounded ring, restart, caps, filtered/lost counts, snapshots | Java 8 bytecode | Overflow, multi-producer accounting, caps, disk errors, restart | No | Mock tests | Gameplay overhead, real storage failures, live lifecycle |
| W-tap analyzer | Deterministic event evidence / candidates | Python | Multi-event fixtures | N/A | Offline yes | Requested real CSV; live sprint coverage |
| Block-hit analyzer | Attack/use order and sparse block changes | Python | Multi-event order/abstention tests | N/A | Offline yes | Raw overlap, swing, slowdown and immunity evidence |
| Hit-select analyzer | Local feedback and ambiguous counterattempt matching | Python | Local/nonlocal and thread ambiguity tests | N/A | Offline yes | Attacker identity, local velocity response, authoritative outcomes |
| Combo analyzer | Configurable target-specific attempt chains | Python | Target/gap/correction tests | N/A | Offline yes | Accepted hits and target separation |
| Native module / HUD | Partial native HUDMod + registration/settings source | Alternate r8 API, Java 8 | Core snapshot/lifecycle tests; no native GUI test | No | Not yet | Exact v3 registration/config, preview/drag/scale/theme, full diagnostics view |
| Calibration / experiments | Offline server-evidence model and held-out engine | Python | Synthetic labeled sessions / leakage checks | No server run | Offline fixtures | Private server instrumentation and authoritative labels |
| Performance | Repeatable callback and replay benchmarks | Java 8 / Python | Actual synthetic runs recorded | No | Synthetic only | Real FPS, allocations, CPU/GPU/frame pacing A/B |
| Build / installer | Pinned patch build, sibling checks, generated installer hash | Native source compiles; full JAR not built | Archive preservation and transaction mocks | No | Source tests | Exact v3 JAR and actual Windows installation |
| Experimental release | Source package and handoff delivered | Source validated | CI passes | No | Offline/source only | No new installable client JAR; full VI–IX acceptance incomplete |

The highest-risk confirmed defect was context mutation before local-player/client-thread validation. The fix rejects foreign callbacks before observation. Diagnostic injections now tolerate missing targets, active keyboard bindings replace hardcoded W, and resource caps no longer silently overshoot their row budget. Each analyzer preserves unavailable evidence rather than reporting accepted-hit percentages.

Normal synthetic recording/snapshot runs had zero loss; one deliberately unpaced packet-burst run dropped **9,619 events**, explicitly counted and invalidated by replay. Normal recording p95 callback batch times were 9.67 and 20.72 microseconds, p99 53.07 and 65.37 microseconds. These are mock callback batches, not Minecraft frames or FPS. No gameplay performance or combat improvement is claimed.

## Actual downloads and evidence

- [Source-only ZIP](combatiq/native-v4/reports/VelvetG-CombatIQ-Hardening-SOURCE.zip): 153,621 bytes, 64 source/tooling files, deterministic archive generation verified. This contains source at code commit `3cf47c3`; no baseline JAR or private CSV is included.
- [ZIP checksum](combatiq/native-v4/reports/SHA256SUMS.txt): SHA-256 `205b0630e5ac918d3e97ba2692d1f2099591fd30a930e22f1c1778fe678ceff0`.
- [Exact test handoff](combatiq/native-v4/HANDOFF.md), [release notes / compatibility](combatiq/native-v4/RELEASE-NOTES.md), [audit](AUDIT.md), [recoverable progress](PROGRESS.md).
- [Native test log](combatiq/native-v4/reports/test-results.txt), [kernel/protocol log](combatiq/native-v4/reports/kernel-tests.txt), [CI result metadata](combatiq/native-v4/reports/ci-results.json).
- [Actual benchmark data](combatiq/native-v4/reports/benchmark-jdk21.json), [native API/descriptor validation](combatiq/native-v4/reports/native-source-validation.json), [synthetic fixture](combatiq/native-v4/fixtures/synthetic-combat.csv).

The attached ZIP transfers both exceed the tool's 32 MiB limit. Neither checkpoint could be inspected, and the exact v3 baseline and requested historical CSV are unavailable. To unblock binary generation and real baseline reproduction, provide the extracted source-only checkpoint, baseline split into parts below 32 MiB (or placed directly in the workspace), and the actual recording. Required v3 SHA-256 is `7949d73d9be9eb74b662b2844faee2939c0463222c01b4a89378f19ddaa8bbc7`.

The available r8 client was used only for explicitly labeled API compilation; it was never substituted as the patch baseline. Historical `upgrade_v4.py` remains pinned to the historical binary. A future successful exact-baseline build generates a separate installer pinned to the new output; no such new binary or installer is being presented as generated today.
