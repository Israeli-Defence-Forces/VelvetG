VelvetG + Violet r8 — Cleanup / Foundation

WHAT THIS RELEASE IS
r8 is a stability, UI, installer, persistence and maintainability pass over Violet r7.
It deliberately avoids adding another pile of features before the existing foundation is sane.

INSTALL
1. Fully close Minecraft and the Minecraft Launcher.
2. Extract the entire built release ZIP.
3. Run Install-VelvetG.bat.
4. Reopen the launcher and select VelvetG.

The r8 installer verifies the packaged client JAR against SHA256SUMS.txt before
changing .minecraft, writes JSON as strict UTF-8, creates recovery backups,
uses temporary/replace writes for launcher JSON, rolls core files back on
failure, and writes velvetg-install.json for a clean uninstall.
Uninstall-VelvetG.ps1 removes launcher/client files while deliberately keeping
user settings and caches.

VIOLET UI
The Violet settings UI now follows one compact layout system instead of a pile
of page-specific measurements. Pages are top-aligned, sliders use one width,
header/content margins are consistent, state buttons say what state they are
in, disabled controls show a visible reason, Reset is explicitly global, and
the profile selector lives with Clicking rather than in the sidebar.

Typography and corner radii have been reduced to a small set of shared tokens, and
Violet/assist screens share a low-level control renderer rather than duplicating
button geometry in separate screens.
The Detailed HUD has a full-height divider, safer graph padding/minimum bars and
no cryptic physical/generated breakdown. HUD history is sampled independently
of whether the HUD is currently visible, so hiding it no longer leaves a stale graph.

RUNTIME / RELIABILITY
Violet and the optional assist layer now guard their injected tick/input hooks.
An unexpected exception disables the affected optional layer and logs a useful
[VelvetG/...] diagnostic instead of deliberately turning one module problem into
a whole-client crash. Transient runtime fields are grouped into per-world session
objects so world/player changes reset one coherent state bundle instead of a scatter
of unrelated statics.

Violet mixins are optional (`required:false`, defaultRequire 0). r8 also removes the three ordinal-dependent `runTick` injections: runtime work comes from one HEAD tick hook and key transitions from KeyBinding itself. This is still not a promise that every arbitrary modified 1.8.9 build will work; missing targets degrade the feature instead of forcing a startup failure.

Fresh installs leave the Violet settings shortcut UNBOUND instead of claiming R. Existing saved keybinds are preserved. Manual hotbar overrides are handled narrowly and receive a short ownership grace period instead of unrelated movement keys suppressing the assist layer.

PERSISTENCE / TRANSLATOR
r8 replaces two inherited base classes from the later VelvetG source tree with
API-compatible implementations:
- ProfileManager uses explicit UTF-8 and temp/atomic-replace profile writes, with
  a .bak recovery copy when an old profile exists.
- Translator uses bounded connection/read timeouts, UTF-8 and proper JSON
  serialization instead of hand-built request JSON.

BUILD / RELEASE
RELEASE.properties is the release source of truth for the r8 build and installer.
The builder uses the bundled pinned Minecraft 1.8.9 metadata, validates Minecraft/library downloads against Mojang-provided SHA-1 hashes, pins its extra LaunchWrapper/ASM artifacts by SHA-256, and rejects corrupt cached downloads. The old no-op PikaApiClient
re-serialization is removed. Targeted stale VelvetG version/user-agent strings
are updated during packaging.

The build still uses the exact r7 binary baseline plus source replacements and
Violet source. A later VelvetG archive contains a much larger source tree, but
r8 intentionally does not pretend that later source is an exact 2.0.9.6 baseline.
A true source-only/reproducible migration remains a separate architectural job.

SOURCE BUILD
For the SOURCE package, run BUILD.bat from the ZIP root on Windows with JDK 8 or newer
and internet access. On Linux/macOS, run ./BUILD.sh (or
`python3 source-patch/BUILD.py`) with a compatible JDK. The output is
source-patch\built\.

The builder ships the exact Minecraft 1.8.9 metadata and only needs its pinned client/library artifact URLs to be reachable. It then:
- restores/verifies the exact inherited baseline,
- compiles Java 8 output,
- applies Violet/inherited replacements,
- runs bytecode verification and the included scheduler/UI/assist checks,
- emits SHA256SUMS.txt.

PREVIEWS
The old r7 synthetic previews are kept only under reference-r7/. They are reference
material, not screenshots of r8. A successful full build/test run regenerates current
synthetic previews; real in-game NanoVG output still needs runtime inspection.

VALIDATION STATUS OF THIS PACKAGE
In the packaging environment used to prepare r8:
- VioletPatch/build-tool Java compiles successfully.
- ProfileManager and Translator replacements compile against r7 public APIs
  (Gson was stubbed only for this local compatibility compile).
- replacement public method signatures match the r7 classes.
- the full Violet source parses without Java syntax errors; unresolved errors are
  expected because the Minecraft/LWJGL compile dependencies are not locally present.
- VioletEngine + EngineChecks passes 149/149 scheduler/unit checks after the r8 refactor.
- BUILD.py passes Python bytecode compilation and BUILD.sh passes shell syntax checking.

A full r8 build and real Minecraft launch could NOT be executed in that environment
because it had no outbound access to fetch the Mojang 1.8.9 build dependencies.
Do not treat this source/dev package as a live-tested binary release until BUILD.bat
passes on a normal Windows machine and the result is launched in Minecraft.

SECURITY / SERVER RULES
No anti-cheat bypass or detection-evasion work is part of this cleanup. The build
does not claim that automated gameplay modules are permitted by any server.
Check the rules of whichever server you use.


FIX3 BUILD NOTE: verifier now JVM-verifies only changed/replacement classes and proves all other inherited baseline classes byte-identical. Cosmetic bytecode branding rewrites were removed to avoid invalidating legacy StackMapTable frames.

FIX4 INPUT NOTE: KeyBinding press/release transitions now take precedence over contradictory raw LWJGL polling for the active session. This fixes the FIX3 NativeChecks failure "Mouse event starts hold with polling released" while preserving raw polling as the fallback before any transition has been observed.
