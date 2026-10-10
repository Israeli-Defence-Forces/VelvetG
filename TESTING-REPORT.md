# Integration1 testing report

## Executed build

Working directory: `VelvetG-Violet-r8-Cleanup-JDK8-FIX6`.

```sh
PATH=/workspace/velvet-integration/tools/jdk/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH sh BUILD.sh
```

The existing Python/Java source-patch pipeline completed successfully on Linux with JDK 21 and `--release 8`. It rebuilt against the exact restored r7 baseline, checked pinned dependency downloads, remapped Minecraft compile-time symbols, compiled source replacements, stripped remapping prefixes, applied inherited-class hooks and produced the Integration1 JAR. Initial dependency access and subsequent compilation/test-fixture errors were resolved before delivery. No Gradle or incompatible loader dependency was added.

## Actual automated results

| Stage | Result | Scope |
|---|---|---|
| VioletVerify | PASS | 60 changed/new Java 8 classes JVM-verified; 1,966 inherited classes byte-identical; 2,026 classes structurally parsed; 852 Violet method references resolved; 10 font resources present |
| EngineChecks | 149 passed | Original scheduling, physical priority, delay/ramp/variation/lag, per-hand counting |
| ReliabilityChecks | 26 passed | Bounded CPS storage, five-second average, diagnostics limits, soup confirmation, block filters |
| AwarenessChecks | 26 passed | Scoreboard states/fallback, five projectile models, terrain/water, ray budgets, alert/sound cooldowns |
| VisualChecks | 23 passed | Hit correlation and timeout, observed health deltas, bounded bed/material/destruction records, cleanup |
| NativeChecks | 156 passed | Native registration and setting identities, real ProfileManager save/reload, old-profile defaults, input/menu routing, recorded controls/layout/HUD rendering |
| AssistChecks | 55 passed | Native tool/enchantment/durability selection and slot ownership, inventory-move focus guards, soup-use/refill acknowledgement, native projectile gating/own-shooter filtering and bed observation/reset |

Total: **435 behavioral assertions**, plus structural/JVM verification. Tests execute the produced/remapped Java classes and inherited APIs; Minecraft fixtures avoid game constructors and graphics/network side effects. Generated `validation-previews` are drawn-operation recordings, not screenshots of a running Minecraft client.

Baseline restoration independently matches `BASE-PARTS-MANIFEST.json`: SHA-256 `737b416bf9256e7f22d28768bb6f54e6819ad797301251065298e58827e5293e`, 36,799,945 bytes. RestoreBase also verifies 2,306 historical ZIP entries. The packaging check compares each entry of the freshly compiled runtime patch with its corresponding entry in the delivered JAR; final source/artifact hashes are included with delivery.

Additional focused command: `python3 source-patch/TEST-RELIABILITY.py` (with the same JDK on PATH) runs the original scheduler and new reliability checks without downloading Minecraft. Shell syntax and Python compilation checks cover the updated wrappers/builder.

## Not executed

No live game, multiplayer server, native OpenGL/NanoVG render context, Forge/OptiFine combination, actual Java 8 runtime, Windows PowerShell build, or installer mutation of a real Minecraft directory was tested. No FPS/memory result is asserted. The supplied baseline lacks a standalone Forge mod bootstrap; the artifact is the preserved LaunchWrapper client integration. See INTEGRATION-NOTES.md for remaining manual checks and implemented feature limits.
