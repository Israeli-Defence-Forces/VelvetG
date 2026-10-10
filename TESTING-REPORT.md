# Integration2 testing report

## Reproduced defect

The uploaded crash report contains a VerifyError in me.eldodebug.soar.scout.PikaApiClient.json at its exception-handler constructor call. Its merged catch frame was java/lang/Object, which is not assignable to java/lang/Throwable. The same error was reproduced locally with -Xverify:all on JDK 21 and Temurin Java 8. The failing Integration1 class is byte-identical to the exact restored r7 baseline; our earlier changed-class verification did not JVM-load this inherited class.

Integration2 recomputes only PikaApiClient's frames using the actual class hierarchy. ScoutFrameChecks proves its members/instructions/debug metadata are unchanged after removing verification metadata and max-stack counts, and checks the corrected RuntimeException handler frame. Scout is preserved.

## Executed commands

From the supplied source-patch development project:

```sh
PATH=/workspace/velvet-integration/tools/jdk/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH sh BUILD.sh
```

The existing Linux Python/Java builder compiles runtime code with --release 8, restores the exact baseline, verifies dependency hashes, performs remapping and applies five inherited-class patches (four integration hooks plus the Scout frame repair). No Gradle or incompatible loader dependency was introduced. Windows BUILD.ps1 has the same added regression stages but was not run on Windows.

An official Temurin 8u504 Linux JRE archive was downloaded from adoptium/temurin8-binaries and SHA-256 checked against its published release checksum. With that runtime, each of the following stages was also executed using `java -Xverify:all -cp <checks:artifact:Minecraft:dependencies> <stage> <arguments>`. Native checks use fixtures for graphics/game context and no live network.

| Stage | Actual result | Scope |
|---|---|---|
| VioletVerify | PASS | 61 changed/new Java 8 classes JVM-verified; 1,965 inherited classes byte-identical; 2,026 classes parsed; 852 Violet references resolved; 10 fonts present |
| StartupClassChecks | 280 class loads passed | Inherited Scout/module implementation classes, including the formerly failing PikaApiClient; reflection triggers verification without game initialization |
| ScoutFrameChecks | 3 passed | Repair actually applied, original code/members preserved, JSON catch frame has RuntimeException type |
| EngineChecks | 149 passed | Original scheduler, physical priority, CPS, delays/ramp/variation/lag |
| ReliabilityChecks | 26 passed | Bounded histories, averages, diagnostics, soup confirmation and block filters |
| AwarenessChecks | 26 passed | Scoreboard states/fallback, projectile models, terrain/water, ray budgets and alert/sound cooldowns |
| VisualChecks | 23 passed | Hit correlation, observed health deltas, bounded bed/material/destruction state and cleanup |
| NativeChecks | 156 passed | Native settings/registration, real profile round-trip and old defaults, input/menu routing, recorded UI/HUD operations |
| AssistChecks | 55 passed | Native enchanted/durability tool choice and slot ownership, inventory focus, soup/refill acknowledgement, projectile gating and bed observations |

Total: **438 behavioral/repair assertions**, plus **280 inherited startup class verifications** and the structural/changed-class checks. All nine stages pass on Java 8. The old artifact fails the startup check with the same reported VerifyError. These are actual JVM tests, not a live Minecraft startup.

The restored baseline still matches the supplied manifest: 36,799,945 bytes and SHA-256 737b416bf9256e7f22d28768bb6f54e6819ad797301251065298e58827e5293e. Its 2,306 historical ZIP entries remain verified. Final package validation compares compiled runtime entries with the delivered JAR, checks archive integrity and all source/install manifest entries, and confirms the batch wrapper resolves the configured Integration2 filename.

## Remaining checks

No live Minecraft/OptiFine/Forge session, native OpenGL/NanoVG context, multiplayer behavior, user’s exact Oracle 8u51 Windows runtime, Windows cmd/PowerShell build or real installer mutation was tested. No FPS/memory number is asserted. The supplied baseline lacks a standalone Forge bootstrap; this remains its original LaunchWrapper installation workflow. See INTEGRATION-NOTES.md for approximation limits and live validation steps.
