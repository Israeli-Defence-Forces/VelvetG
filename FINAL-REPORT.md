# CombatIQ RC1 engineering validation

The native client builds from the exact Sampling Fix v3 baseline, including the native HUD module. This is an experimental release candidate. Minecraft startup, full baseline Mixin coexistence, actual NanoVG/config persistence, Windows execution and gameplay performance remain UNVERIFIED. No production merge is authorized.

| System | Implemented | Tests | Real Minecraft validation | Release status |
|---|---|---|---|---|
| Recorder | Bounded MPSC, restart, duration/row/size limits, segmented CSV/checksums, partial-file failure detection | Concurrency, overflow, disk quota, abrupt kill, reassembly, tamper, lifecycle | Unverified | RC1 binary |
| Input sampling | Active binding, gameplay context, mouse proxy, forward/strafe | Remap, focus, local/client-thread and foreign first-caller harnesses | Unverified | RC1 binary |
| Sprint reconstruction | Actual setter pairs, sparse state, sprint action attempts | Transitions, repeats and abstention | Unverified | RC1 binary + analyzer |
| Packet correlation | Temporary object tokens, observed network-manager identity, API/dispatch/channel-invocation stages | Stale manager rejection, normalization, six real headless Mixin transformations | Unverified | Socket writes/server receipt unavailable |
| W-tap | Raw/normalized releases, sampling and retrospective candidates | Positive, contradictory, missing/unknown-owner cases | Unverified | No optimal-delay claim |
| Block-hit | Mouse overlap, swing, block observations, use/attack sequence, movement context | Ordering, missing packet and abstention cases | Unverified | Acceptance/immunity unknown |
| Hit-select | Local feedback, velocity/ground/observed target context, ambiguous attribution | Packet deduplication, dense-window abstention and identity boundaries | Unverified | Attacker/server outcome unknown |
| Combo | Target-specific attempt chains, gaps/corrections/releases | Target, gap, correction and boundary cases | Unverified | Accepted-hit chain unknown |
| Native GUI/HUD | Native HUDMod registration, compact/expanded snapshots, cached measurements, theme/font/position/scale inheritance | Actual module compiled against exact v3 API; contract harness verifies settings, draw, fault isolation and no render lock | Unverified | NanoVG and editor behavior pending |
| Settings | 13 native settings; recording defaults off; export action, path, limits, rotation and analyzer controls | ABI and contract behavior | Unverified | Native config serialization pending |
| Performance | Six fixed-workload JVM configurations; producer allocation, CPU, percentiles, GC, loss and offline replay | 18 measured synthetic runs; raw evidence included | Unverified | No FPS/frame-time claim |
| Calibration | Session-held-out authoritative-trial framework and rejection of leakage/invalid labels | Synthetic labeled-session tests and protocol self-tests | Unverified | No real recommendation/clock alignment |
| Installer/rollback | Exact new-JAR pin, separate profile, original preserved, durable journal/recovery, rollback/uninstall | 11 transaction tests plus real built-JAR install/rollback/tamper smoke | Unverified | Python 3.9+; Windows OS pending |
| Final JAR | Exact v3 patch, native module included, Java 8 bytecode, dependency closure and unrelated-entry preservation | Structural checks, ZIP integrity, repeated build digest and artifact verification | Unverified | Experimental RC1; runtime acceptance pending |

Current source regression suite: 101 discovered, 100 passed, 1 skipped (historical CSV unavailable). Standalone kernel: 60 assertions. Protocol: three tests. The final package carries actual logs and benchmark JSON; earlier reports in `reports/` are historical checkpoint evidence, not current certification. Current CI status must be read from the final branch run rather than inferred from previous passing CI.

Exact required inputs:
- v3 SHA-256 `7949d73d9be9eb74b662b2844faee2939c0463222c01b4a89378f19ddaa8bbc7` reconstructed from the supplied two parts and verified.
- Minecraft SHA-256 `14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298`.
- Official Minecraft Netty SHA-256 `50510e9c9874b539b98ef931fe7eeeb002f6b5b1976ea37669b9493426294561`.

The final client manifest records its code commit, Java-source digest, compiler, exact input/output digests and compiled entry digests. The consolidated ZIP contains the actual client, freshly pinned installer, five Windows command wrappers, runnable analyzer/calibration zipapp, standalone kernel, README and evidence. `CONTENTS.json`, `SHA256SUMS.txt`, `VERIFY.py` and the outer ZIP checksum support verification. Private client binaries and input JARs must stay out of the public repository.

Known practical limits: file checksum proves integrity, not authoritative outcomes; writer failure retains `.partial`; segment rotation is bounded by the recording row/duration caps. Packet tokens use bounded identity retention and can lose correspondence on eviction; they cannot prove socket writes or delivery. Dense evidence queries truncate and abstain; movement context reports truncation. Reflection, storage and renderer failures are isolated. The headless Mixin harness retains its bootstrap warning and logs; it transforms six new Mixins but does not initialize the whole original client.

Windows acceptance procedure is in `combatiq/native-v4/HANDOFF.md`. Do not call the package release-validated until that procedure and actual Minecraft acceptance have been executed.
