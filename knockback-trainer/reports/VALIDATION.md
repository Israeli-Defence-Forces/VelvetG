# Trainer RC1 validation scope

The release is a real patched native Java 8 client, built on exact CombatIQ RC1. Training is independent of CSV/recording. The GUI uses HUDMod, native settings and real NanoVG paths; it is not a screenshot or pre-rendered demo.

Local trainer suite: five Python test cases, with 92 Java assertions across timing/ground/statistics (38), native bridge/sound/input (16), and real adapter/control/drawing/persistence contract (38). Executed with Java 8 and Java 21. RC1 regressions: 101 tests, 100 pass and one historical CSV skipped. Four new Mixins plus RC1's six additive Mixins transform pinned target bytecode together. Original headless bootstrap warning is retained in the evidence. This is not a full-client launch or all-original-Mixin validation.

Contract persistence tests exercise stable native keys and typed value round-trips. Inspection of actual RC1 ProfileManager confirms native save/load of getNameKey, Boolean/Number/Color/Text settings, HUD X/Y and Scale. A real Minecraft save/restart is not executed and must not be inferred from the contract test.

The builder verifies every pre-existing RC1 JAR entry remains byte-identical except the additive mixins.soar.json. This preserves existing SnapTap, Dynamic Widget, crosshair/delay fixes and CombatIQ bytecode/resources; runtime coexistence still requires a game launch.

Synthetic benchmark includes disabled callbacks, the live bridge with a fake collision world/default history, and core snapshots at maximum 600 samples. Three repetitions/configuration, 1000 warmup + 5000 timed batches. It measures callback time and producer allocation, not real terrain/GPU/audio or game FPS/frame-time impact. Raw results are included.

Real Minecraft startup, NanoVG/game editor interaction, actual profile restart, Windows installer/filesystem behavior, real terrain/chunk behavior, and multiplayer timing remain unverified. Local observation timing cannot establish a server damage tick, combat source, accepted hits, or successfully negated knockback. UNKNOWN is explicit and excluded from failures.
