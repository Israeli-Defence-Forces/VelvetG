# CombatIQ continuation — experimental source checkpoint

Branch: `combatiq/phase4-hardening`, based on PR #2 at `d44f393899c4ea476ff8b0d25f64d5393a0c8a3a`.
Draft PR: https://github.com/Israeli-Defence-Forces/VelvetG/pull/3

Completed: client-thread/local-entity guards before context mutation; nonfatal optional added injection requirements; verified active forward binding; bounded recorder caps, stop/restart, separate filtered/lost counters and immutable HUD snapshots; four separate retrospective analyzer modules; dense-window bounds; session-held-out calibration; synthetic benchmarks; generated installer pinning; Java 8 source/bytecode checks and regressions.

The native HUD adapter extends the actual HUDMod API and registers through an optional ModManager Mixin. It exposes native recording, visibility and compact/expanded settings. Compilation against a different, explicitly identified local r8 API passed; it has not been applied to the exact v3 baseline. Existing native config owns position/scale/settings, but persistence, preview, dragging, scaling and theme behavior have not been tested live. Analyzer selection, full timeline/distributions interface, recording-limit GUI controls and export controls remain incomplete. This is not Phase VI acceptance or a production experimental release.

Local validation: 72 tests, 71 passed, historical CSV skipped; 60 kernel assertions; 3 protocol tests. All native source including optional module compiles to class major 52 with JDK 21. Official Minecraft SHA-pinned method/field/protocol checks pass. Hosted CI on an earlier continuation code checkpoint passed Java 8, 17 and 21; final CI results are recorded separately.

## Exact blockers

- Both uploaded ZIP transfers were rejected by the tool's 32 MiB limit. Their contents remain unexamined. Supply an extracted source-only checkpoint, and split baseline binary parts each below 32 MiB, or place them directly in the attached workspace.
- Required v3 input SHA-256: `7949d73d9be9eb74b662b2844faee2939c0463222c01b4a89378f19ddaa8bbc7`. Available r8 client SHA-256 is `5bab852a43ea6f15d0525d246864c519437267d3c54f502f35119c88ee151b82`; it was used only for API compilation, never as the patch baseline.
- The v3 baseline's keyboard/movement/tick/network bridge and SnapTap application order cannot be audited from the supplied source alone. No new client JAR has been generated.
- The historical CSV is unavailable, including the requested 85,504-row recording. Its reported measurements have not been reproduced. The existing skipped historical test targets a different earlier recording; it is not validation of the requested session.
- No Minecraft/LaunchWrapper/Mixin launch, Windows installation, integrated server, multiplayer, OptiFine or real frame pacing run has occurred.
- Connection identity, stale callbacks from old connections, actual transport handoff/socket write, raw press overlap, swing, target motion and server outcomes are not fully instrumented. Context epoch must not be called a verified connection epoch.
- No private server or authoritative labeled observations are attached. Calibration tests use synthetic labels; no real interval recommendation exists.

## Recovery and publication

Incremental commits are published in the continuation branch, preserving PR #2 as parent. The local repository is a text-source import with separate local hashes because terminal Git transport could not reach its proxy. GitHub API commits preserve the real upstream history. No production merge was performed. Both an initial upload rejection and its later successful retry were resolved by verifying the connected account owned the named repository and checking the user's explicit branch/PR instructions.
