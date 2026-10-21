# CombatIQ v4 — experimental sprint/packet checkpoint

Built from the exact working v3 supplied by the user. This is an **owner-only experimental build**, not a validated Minecraft release. Java 8 bytecode; tested with Java 17 mocks. **Minecraft launch/Mixin application, Windows installation, HUD and FPS are untested.**

## What changed
- Preserves v3's inherited movement-field search and one-time sampling diagnostics.
- Observes local sprint getter at real setter HEAD/RETURN and player-update points. A paired changed setter is separate from a change inferred between sparse observations.
- Adds allowlisted action decoding: START/STOP_SPRINTING, ATTACK vs INTERACT/INTERACT_AT, release-use, placement/use, velocity, status, relative movement, teleport and correction.
- Records focus/GUI/player-context epochs, capture-thread IDs and bounded-ring enqueue sequence. These are **not connection acknowledgements or server order**.
- Separates pre-send attempt, pre-filter receive, and client handler RETURN. Actual transport writes and server acceptance remain NOT_INSTRUMENTED.
- Caps recordings by duration, output size and row threshold; capped/lost/interrupted recordings are explicitly inconclusive.
- Adds retrospective W-release/sprint correlation and limited block-use, feedback-counterattack and attack-sequence diagnostics. Calibration abstains without controlled outcomes.

The original three v3-patched mixin classes stay byte-identical. Only the three native recorder classes and mixins.soar.json change; six new Java 8 classes are added. The original installation is not modified by building.

## Upgrade only an existing, verified v3 experimental profile
1. Close Minecraft and its launcher. Keep the working v3 package.
2. Extract the entire v4 package to a normal folder.
3. Run `python upgrade_v4.py --game-dir "D:\Roaming\.minecraft" --check`.
4. If it passes, run `python upgrade_v4.py --game-dir "D:\Roaming\.minecraft"` (or UPGRADE-v4.cmd).
5. Keep the existing experimental launcher profile and `-Dvelvet.combatiq.enabled=true` JVM argument.
6. Test in **singleplayer first**, exit normally, then analyze the new CSV.

Unknown baseline/installed/payload hashes and conflicting backups are refused. Existing v1/v2 backups remain untouched. Only v3 -> this exact v4 experimental JAR is accepted. The pinned original baseline must still be installed; it is not provided in this package.

Rollback with Minecraft closed:
`python upgrade_v4.py --game-dir "D:\Roaming\.minecraft" --rollback --check`
then the same command without `--check`. Rollback restores the verified v3 JAR and launcher JSON.

Recordings stay under `%USERPROFILE%\VelvetG-CombatIQ`. Optional JVM caps:
`-Dvelvet.combatiq.durationSeconds=300 -Dvelvet.combatiq.maxBytes=33554432`
Limits are clamped. Row threshold is 150000 with bounded queue drain. Hitting any limit invalidates conclusive interval analysis. There is no recording-toggle GUI yet.

## Analyze
`python combat_analyzer.py "C:\Users\User\VelvetG-CombatIQ\your-file.csv" --out report.json`

Native schema 1 (v3) and schema 2 (v4) are supported. `--out` refuses overwriting a file. Legacy `analyze_native.py --latest` remains for schema 1 only. Use the new analyzer for v4. Neither script gives server-success percentages.

## Rebuild and test
Use Python 3.9+ and a JDK 8+ providing java/javac. Do not substitute a JRE.
`python build_v4.py --v3 "path\to\working-v3.jar" --minecraft "path\to\1.8.9.jar" --out rebuilt.jar`

The builder refuses an unknown v3 or Minecraft binary, checks packet layouts/registration and target method descriptors, packages all named/anonymous telemetry siblings, verifies class-major 52, and refuses an existing output. It verifies structural targets, **not actual Mixin application**. The owner JAR and official Minecraft binary are not published to GitHub. The installer is pinned to the delivered build's hash; a rebuild with different compiler output is not automatically authorized for installation.

`python -m unittest discover -p "test*.py" -v`

The native harness compiles only original added telemetry code plus synthetic public test stubs. It does not launch Minecraft. The installer tests use synthetic files/hashes and exercise validation, transaction recovery and rollback. A historical v3 CSV test explicitly skips if its old fixture is absent.

## Exact hook semantics
- Existing onKey: AFTER Minecraft.dispatchKeypresses inside runTick, after binding updates. This is a W-event observation proxy, **not switch time or earliest queue drain**.
- Existing movement callback: RETURN of SnapTap's TAIL callback on bev.a()V, after its input transformation. Item-use scaling in the later player update is a separate stage.
- Existing onTick: runTick TAIL. The `tick` column is the count of observed completed gameplay ticks, not a newly added tick-HEAD clock; same-iteration earlier callbacks can carry the previous count. Use event order and stage, not equality of this counter, for ordering claims.
- New bew.d(Z)V HEAD/RETURN: actual local sprint getter observations around the setter; bew.m()V RETURN adds a post-living-update observation.
- New ave.aw()/ax() HEAD: clickMouse/rightClickMouse function-entry intent, not every raw mouse button press and not proof an attack packet exists.
- Existing network hooks: send attempt before later cancellation; inbound before filtering. No traffic is delayed, cancelled or rewritten by added code.
- New bcy handler RETURN: a handler returned after its main-thread handoff check; entity absence/early return can still mean no motion application. It is not an unconditional APPLY or hit confirmation.
- `epoch` increments on observed player, GUI or focus changes. It is a context identity, **not a true NetworkManager/connection ID**. Changes entirely between hooks may remain unobserved.
- The retained v3 synchronized bounded MPSC queue records enqueue order under its lock. It is not globally causal order across producer threads; it is not lock-free. Reflection caches remain; some reflective calls/boxing are still present.

## Schema
`ns,kind,data,moveForward,packetClass,tick,epoch,thread,sequence,entity,flags,x,y,z`

Kinds 1 W-up, 2 W-down, 3 post-SnapTap sample, 4 tick TAIL, 5 packet send attempt, 6 inbound before filtering, 7 sampling hook reached, 8 sampling error, 9 sparse local sprint state, 10 setter HEAD, 11 setter RETURN, 12 context, 13 attack-function entry, 14 use-function entry, 15 handler RETURN, 16 other diagnostic.

Kinds 5/6/15 data codes: 11 sprint start; 12 sprint stop; 13 ATTACK; 14 INTERACT; 15 INTERACT_AT; 16 release use; 17 use/placement; 18 velocity; 19 entity status; 20 relative entity update; 21 entity teleport; 22 player correction; 23 other entity action; 24 other digging. A C08 use/placement does not establish sword blocking.

Context/state flags: 1 player present; 2 GUI absent; 4 focused; 8 on ground; 16 sneaking; 32 blocking; 64 previous sprint known; 128 previous sprint on. Packet rows currently carry flags=0. Packet x/y/z are raw encoded components (S12 units 1/8000; S14/S18 coordinate encoding must be interpreted with packet type); player corrections retain relative-flag ambiguity because correction flags are not captured. Do not treat them as a complete opponent physics model.

## Gates still open
Phase IV: native experimental implementation and synthetic reconstruction available; actual transport write/connection identity, raw input consumption, complete transformation attribution and real-client validation are missing.
Phase V: four descriptive sections run on fixtures; block-hit suppression, target movement and successful combo/hit-select attribution are not established.
Phase VI: no native CombatIQ module/settings/HUD delivered.
Phase VII: caps and calibration abstention added; no learned recommendation policy, comparative outcome experiment or Minecraft performance benchmark.

Do not merge this into production or claim PvP improvement. Public functionality is observational, subject to the server's rules. Any intervention/ground-truth experiment belongs on an authorized private server.
