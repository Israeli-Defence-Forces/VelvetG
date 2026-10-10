# VelvetG + Violet r8 — Audit Fix Matrix

This file maps the r7 audit to the r8 cleanup work. “Fixed” means the issue is addressed in the r8 source/build package. “Mitigated” means the immediate failure mode is contained but the deeper architecture still needs a source-first migration. “Deferred” means it depends on larger inherited-base work or on real runtime/Windows integration testing. Gameplay correctness bugs are in scope; anti-cheat bypass, detection-evasion and concealment work are not.

## Fixed in r8

- **Hook crash containment:** Violet/assist tick and input entry points use fail-closed circuit breakers with subsystem logging instead of propagating an optional-module exception through the Minecraft tick.
- **Per-world transient state:** Violet and assist runtime state is grouped into session objects and reset on client/world/player identity changes instead of being spread across unrelated mutable statics.
- **Mixin robustness:** the three ordinal-dependent `ave.s()` injections are removed. r8 uses one optional `runTick` HEAD hook plus a KeyBinding state hook, with `required:false`, `defaultRequire:0` and `require=0`, so minor call-order changes no longer shift Violet ordinals or hard-crash startup.
- **Repeated reflection:** container text-entry detection is cached by screen class and fails closed for unknown layouts.
- **Per-tick settings signature allocation:** replaced the concatenated string signature with an integer runtime settings hash.
- **Repeated UI/module array allocation:** shared constant arrays are used for stable labels/icons.
- **HUD stale history:** history sampling is tick-driven instead of render-driven, so hiding/idling the HUD no longer freezes and later replays stale bars; graph geometry and divider/padding are normalized.
- **Violet UI system:** the custom screens share style tokens and a control renderer; pages are top-aligned, slider widths/margins are normalized, tiny/eyeballed typography/radii are reduced to a small shared set, disabled reasons are visible, and ambiguous state/action labels are clarified.
- **Appearance/Clicking organization:** Rate Profile is moved into Clicking instead of living in the Appearance/sidebar context; the giant preview area is reduced.
- **Assist UI:** header/content margins are aligned and concise descriptions/context hints are restored.
- **Reset wording:** the existing global-reset behavior is made explicit rather than presenting it as a page-local action.
- **Settings shortcut:** fresh installs default to Unbound instead of silently claiming `R`; existing saved binds are preserved.
- **Settings persistence:** discrete UI changes and slider releases save immediately, in addition to close-time persistence.
- **Profile persistence:** recovered `ProfileManager` replacement uses explicit UTF-8 and temp/replace writes with a recovery `.bak` copy where possible; its public r7 API was verified identical.
- **Translator robustness:** recovered `Translator` replacement uses explicit UTF-8, bounded connect/read timeouts and Gson request serialization instead of hand-built JSON; its public r7 API was verified identical.
- **Installer package verification:** installer checks the packaged r8 JAR against `SHA256SUMS.txt` before modifying `.minecraft`.
- **Installer encoding/atomicity:** launcher/version JSON handling is explicit UTF-8 and uses temp/replace writes.
- **Installer recovery:** affected client/profile files are backed up and core changes roll back on failure; newly created migrated config files are also tracked for rollback.
- **Installer hygiene:** install log is written to `%TEMP%`, recovery backups are pruned, a small install manifest is written, and an uninstall script is included.
- **Dead profile JSON:** shipped root `VelvetG.json` is removed; release/profile identity comes from `RELEASE.properties` plus installer generation.
- **Build download integrity:** the exact 1.8.9 metadata is pinned in the source package; Minecraft client/libraries are validated against its Mojang SHA-1 values, and the two extra build/runtime artifacts are pinned by SHA-256 before reuse.
- **Cross-platform source build path:** `BUILD.py` + `BUILD.sh` supplement the Windows PowerShell/BAT builder and preserve Mojang dependency paths rather than flattening colliding basenames.
- **No-op binary rewrite:** the PikaApiClient re-serialization that made no semantic edit is removed.
- **Stale release branding:** selected inherited VelvetG version/user-agent strings are patched to r8 and the old installer success text is removed.
- **Unprofessional inherited update error:** the targeted `something is broken lmao` string is replaced with `Update check failed`.
- **Documentation honesty:** the source package no longer calls the exact 2.0.9.6 base fully source-reproducible; build/runtime validation limits are explicit.

## Mitigated, not architecturally eliminated

- **Obfuscated mixin names:** ordinal dependence is eliminated, but r8 still targets historical 1.8.9 obfuscated method names because the exact base is binary-first. A named/refmap migration belongs in the source-first base migration.
- **Build-time ModuleCategory heuristic:** still scans historical bytecode to integrate the category UI. It fails loudly during build rather than silently producing a malformed client, but should disappear once the inherited UI is built from source.
- **Launcher JSON round-trip:** PowerShell still parses/serializes launcher JSON, but r8 makes the operation UTF-8, backed up and atomically replaced. Eliminating round-trip formatting changes requires a narrower JSON-edit implementation.
- **Legacy internal names/config generations:** user-facing release identity is cleaned where safely patchable, but mass-renaming inherited binary internals would be a high-regression rewrite.
- **Exact-base source reproducibility:** a later, much larger VelvetG source archive exists and was useful as a reference for two API-compatible inherited classes, but it is not silently substituted for the exact r7/2.0.9.6 baseline.

## Deferred larger work

- Move the exact inherited client to a source-controlled Gradle build and verify reproduced output/behavior before dropping the binary-baseline patcher.
- Collapse inherited Scout/Velvet/VelvetG persistence generations into one versioned `ConfigStore` with migrations.
- Replace remaining legacy commands/namespaces and dead inherited flows only after the full source baseline is under test.
- Add a dedicated localization resource layer for Violet custom strings rather than abusing a fixed inherited enum.
- Add authenticated/signed remote metadata or remove remote behavior controls where feasible.
- Replace the discontinued third-party OptiFine/library dependency path with a verified, maintainable distribution strategy consistent with upstream licensing.
- Run Windows PowerShell 5.1/7 installer tests, actual Minecraft + OptiFine launches, multiple GUI scales/resolutions, network-failure tests and live resource-lifecycle tests.

## Gameplay-scope boundary

- r8 fixes incorrect ownership/cancellation behavior such as unrelated keys suppressing assist state and manual slot overrides persisting for an entire hold.
- It does **not** add anti-cheat bypasses, detection-evasion logic, packet-pattern disguising, or concealment features.

## Validation completed while packaging

- `VioletPatch` and the bytecode build utilities compile against the r7 client JAR.
- Recovered `ProfileManager` and `Translator` replacements compile against the r7 client API when supplied Gson, and retain the original externally visible API.
- `VioletEngine` + `EngineChecks` pass **149/149** scheduler/unit checks after the refactor.
- `BUILD.py` passes Python bytecode compilation and `BUILD.sh` passes shell syntax checking.
- All r8 Java sources were passed through `javac`; unresolved compile errors are missing Minecraft/LWJGL/`vgstub` dependencies, with **zero syntax-like Java errors** detected.
- A full fresh r8 build, Windows installer run and live Minecraft launch were not possible in the isolated packaging environment because the Mojang binary dependencies could not be fetched there. The builder itself now uses the pinned local 1.8.9 metadata rather than live manifest discovery.


FIX3 BUILD NOTE: verifier now JVM-verifies only changed/replacement classes and proves all other inherited baseline classes byte-identical. Cosmetic bytecode branding rewrites were removed to avoid invalidating legacy StackMapTable frames.

## FIX5 validation harness
NativeChecks no longer relies on page state leaked from the preceding Timing test when exercising the preset menu. It explicitly navigates to Clicking before selecting Build.

## FIX6 assist lifecycle regression
The FIX5 Windows/JDK 8 run reached AssistChecks after the bytecode verifier, all 149 EngineChecks and all 142 NativeChecks had passed. The remaining assertion exposed a real lifecycle edge case: the short manual-override grace timer survived disabling Auto Tool, so an immediate re-enable inherited stale session state. FIX6 clears assist-owned grace/cooldown state on module disable and keeps the regression assertion so the runtime fix, rather than a test-only bypass, is required.
