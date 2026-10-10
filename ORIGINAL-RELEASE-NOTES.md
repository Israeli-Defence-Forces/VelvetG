## r8 build compatibility hotfix

- JDK 8 `javac -version` is now queried through `ProcessStartInfo`, avoiding PowerShell `NativeCommandError` when javac writes its version to stderr.
- JDK 8 continues to compile with `-source 1.8 -target 1.8`; newer JDKs use `--release 8`.

# VelvetG + Violet r8 — Cleanup / Foundation
- Build scripts now detect JDK 8 and use `-source 1.8 -target 1.8`; JDK 9+ uses `--release 8`. JDK 8 is fully supported for the build.

r8 is a corrective release based on the r7 audit. It focuses on crash containment,
UI consistency, persistence safety, installer safety and build provenance.

## Fixed

### Runtime stability
- Added guarded Violet and assist hook entry points. Unexpected hook failures now
  disable/log the affected optional subsystem instead of propagating directly into
  the Minecraft tick/input path.
- Removed the three ordinal-dependent `runTick` injections. Violet now uses one optional
  tick hook and one KeyBinding state hook, with `require=0`, so minor call-order changes
  no longer move an ordinal target or hard-fail client startup.
- Cached the inventory text-field screen reflection check.
- Grouped transient Violet/assist state into per-world session objects to reduce stale
  cross-world state and lifecycle drift.
- Made assist cleanup non-recursive if cleanup itself throws.

### UI / HUD
- Introduced shared type, radius, row-height and control-height tokens.
- Added a shared Violet/assist control renderer instead of maintaining duplicate
  button hitbox/render logic in separate screens.
- Top-aligned Violet pages so content no longer jumps vertically between tabs.
- Moved Rate Profile from the Appearance/sidebar context to Clicking.
- Standardized slider widths and screen spacing.
- Added visible explanations for disabled controls.
- Clarified stateful controls and global reset wording.
- Fixed assist-window header/content alignment and restored concise descriptions.
- Moved HUD history sampling onto the tick path instead of the render path.
- Improved Detailed HUD divider/graph geometry and removed the cryptic count split.
- Fresh installs no longer bind the settings screen to R by default.

### Assist correctness
- Manual hotbar ownership is handled narrowly instead of treating unrelated keys as a global cancel.
- Manual slot overrides use a short grace window instead of suppressing an assist for an entire held click.
- Synthetic Violet key-state changes are isolated from physical/manual input accounting.

### Persistence / network utility
- Discrete Violet/assist UI changes and slider releases persist immediately instead
  of relying only on screen-close persistence.
- ProfileManager now reads/writes UTF-8 explicitly.
- Profile saves write to a temporary file and replace the destination, with a
  recovery `.bak` copy when possible.
- Translator now has connect/read timeouts and uses Gson for request JSON escaping.

### Installer
- Release JAR SHA-256 is checked against SHA256SUMS before installation.
- launcher/version JSON is written as explicit UTF-8 through temporary replacement.
- Existing launcher/client files are backed up before changes.
- Core install failure triggers rollback.
- Installer logs use `%TEMP%` instead of requiring the extracted directory to be writable.
- Added an uninstall script and install manifest.
- Backup history is pruned to the newest five recovery points.
- Removed stale `PerfPatch + Violet BW` success branding.
- Removed the dead shipped `VelvetG.json`; the installer now has one profile source.

### Build / branding
- Added `RELEASE.properties` as the release/build/installer identity source.
- Pinned the exact Minecraft 1.8.9 metadata locally; Mojang client/library downloads are SHA-1 checked, including cached files.
- Explicitly pins LaunchWrapper 1.12 and ASM-all 5.0.3 with SHA-256, fixing the old impossible `lwjgl-2.9.3` dependency assumption.
- Added a cross-platform Python builder (`BUILD.py` / `BUILD.sh`) alongside the
  Windows PowerShell/BAT path.
- Removed the no-op PikaApiClient re-serialization.
- Updated several inherited stale VelvetG user-agent/version strings during patching.
- Replaced the rude inherited update failure string with `Update check failed`.

## Gameplay scope

r8 fixes correctness bugs in assist ownership/cancellation, but does not add anti-cheat
bypasses, detection-evasion logic or packet-pattern concealment.

## Remaining architectural debt

- r8 remains a patch-over-baseline build for 2.0.9.6. A later VelvetG source archive
  exists, but it is not an exact historical r7 base and should not be silently treated
  as one.
- ModuleCategory integration still uses a build-time bytecode heuristic. It is checked
  during the build and fails loudly if the expected structure changes, but a future
  source-based integration should remove it.
- Inherited legacy names/config generations still exist in the base client. r8 cleans
  user-facing release identity where safely patchable without attempting a risky mass
  binary rewrite.
- Full localization of Violet's custom strings needs a dedicated Violet resource layer;
  the inherited TranslateText enum cannot accept arbitrary runtime keys without a base
  localization refactor.

## Validation caveat

This source package was statically/compatibility checked, but the preparation environment
could not download Mojang's Minecraft 1.8.9 dependencies, so the full BUILD.bat pipeline
and a live client launch were not run here. See VALIDATION.txt.


FIX3 BUILD NOTE: verifier now JVM-verifies only changed/replacement classes and proves all other inherited baseline classes byte-identical. Cosmetic bytecode branding rewrites were removed to avoid invalidating legacy StackMapTable frames.

### FIX4 - input transition arbitration
- Fixed a NativeChecks regression where a real KeyBinding press/release transition could be overridden immediately by contradictory LWJGL polling.
- Real observed transitions are now authoritative for the current session; raw polling is a fallback only until a transition is observed.
- Context changes still clear observed input state, and Violet-generated synthetic key writes remain excluded.

### FIX5 build-harness correction
- NativeChecks now returns explicitly to the Clicking page before opening the preset menu.
- This fixes the stale `Build preset` assertion introduced when r8 moved the preset control to the Clicking-page header.
- No runtime clicking behavior was changed by FIX5; this is a validation-harness correction.

### FIX6 - assist lifecycle ownership
- Disabling Auto Tool now clears any pending manual-override grace timer after restoring owned slot state.
- Disabling Auto Soup now clears its manual-override grace, cooldown timestamp and transient age after cleanup.
- This fixes stale assist-session state surviving a disable/re-enable cycle.
- AssistChecks now verifies that a re-enabled Auto Tool can immediately reacquire its slot before testing an explicit same-slot manual choice.
