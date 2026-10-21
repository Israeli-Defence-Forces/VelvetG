# Experimental source checkpoint — hardening continuation

Fixes shared-context mutation from foreign player callbacks, optional-injection hard failures, hardcoded W lookup, row-cap overshoot and unrelated/missing compiler-class packaging assumptions. Adds restartable recording, immutable HUD snapshots, separate filtered/lost accounting, UTF-8 size accounting, native HUD source integration, four evidence-aware analyzers, calibration and repeatable benchmarks.

Not a finished release: no new client binary; uploaded checkpoints were inaccessible. Native registration/config persistence and full diagnostics GUI remain unvalidated or incomplete. No full movement/velocity/target-motion capture, verified connection identity, transport writes or server outcome evidence exists. Java 8/17/21 mock CI does not demonstrate Minecraft compatibility. Optional probes may not apply; absence of observations remains unknown. Recorder errors/caps remain visible, and lossy or partial traces cannot produce valid technique conclusions.

## Compatibility

| Component | Status |
|---|---|
| Java 8 bytecode | All compiled native/core classes major 52 |
| Java 8/17/21 mock CI | Passed on published checkpoint; see CI report for exact SHA |
| Python CLI | Python 3; standard library only |
| Minecraft 1.8.9 mappings/protocol | Official SHA-pinned structural verification passed |
| Native r8 HUD API | Source compilation passed against explicitly different r8 binary |
| Exact v3 baseline integration | Blocked: missing verified v3 JAR |
| Windows 10 launcher / rollback | Synthetic transaction tests only |
| Minecraft / Mixin / OptiFine / widgets | Live validation pending |
| Server acceptance / real calibration | No authoritative experiments |

## Synthetic performance

See reports/benchmark-jdk21.json for two 1,000-batch runs per mode after 1,000 warmup batches. These are callback batches, not frames. Measured normal recording p95: 9.67 and 20.72 microseconds; p99: 53.07 and 65.37 microseconds. No normal recording/snapshot loss occurred in these runs. One intentionally unpaced packet-burst run dropped 9,619 events; the loss counter reports them and replay marks that trace invalid. This is not a loss-free guarantee or gameplay FPS result.

Provisional synthetic budgets: normal recording/snapshot p95 below 50 microseconds, p99 below 250 microseconds, zero loss at this normal workload. These particular runs meet those synthetic budgets. Gameplay budgets require real controlled A/B frame measurements, allocation measurements and repeated sessions. No gameplay performance acceptance is claimed.
