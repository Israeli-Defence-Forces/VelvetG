# Checkpoint validation — 21 October 2026

Executed locally on Linux, OpenJDK 17.0.20, javac 17.0.20 targeting Java 8, Python 3.12. No Minecraft launch or Windows GUI/runtime test.

- Native source regression suite: **38 tests run, 37 passed, 1 explicitly skipped**. The skipped case requires an older historical private CSV. This count includes actual Java mock-hook compilation/execution, schema replay, sampling gaps, action distinction, same-thread ordering, context changes, synthetic installer upgrade/rollback, backup collision refusal, metadata tampering refusal and recovery from a failure between file replacements.
- Existing Phase III standalone kernel: **60 assertions passed**; three Python protocol self-tests passed.
- Owner JAR build: exact v3 SHA-256 and exact official Minecraft 1.8.9 SHA-256 checked. Original added Java source compiled for class major 52. Structural verifier checked 16 method targets, packet field types and PLAY packet registration. This does not apply or validate Mixin transformations.
- Packager: exactly four v3 entries changed (three recorder classes plus mixin config); six classes added; 2,309 v3 entries byte-identical. All compiler-generated telemetry siblings included.
- Installer transactions were exercised using synthetic file contents and patched test hashes. The original historical baseline was not available locally; its SHA guard remains mandatory on the user's installation. No actual Windows profile was modified here.

The separate manifest pins the actual delivered JAR. The proprietary JAR, official Minecraft JAR, user CSV and private derived event report are not committed to GitHub.

## Explicitly not verified
Actual Mixin application/LaunchWrapper startup, Coerce callback compatibility in the full client, Windows Java 8 runtime, snapshots/HUD/settings, true network writes, true connection IDs, raw mouse-press consumption, full blocking suppression attribution, complete target kinematics, FPS/GC overhead, server acceptance, optimal W/Zest/block-hit/hit-select timing, or any PvP advantage.

New CI repeats source/mocked tests on Java 8/17/21. Its status must be inspected separately; this document does not predeclare those runs successful.
