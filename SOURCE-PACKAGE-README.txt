VelvetG + Violet r8 — source/development package

This is NOT a claim that the exact VelvetG 2.0.9.6 base is fully source-reproducible.

What is editable here:
- Violet / HUD / assist source under source-patch/src/me/movi/violet/
- r8 replacement source for inherited ProfileManager and Translator
- bytecode integration/build tools and offline checks
- installer / uninstaller / release metadata

Why the r7 JAR remains bundled:
source-patch/BUILD.ps1 restores and verifies the exact historical binary baseline
before applying r8 source replacements. That preserves compatibility with the release
being cleaned up rather than silently substituting a later source snapshot.

A later user-owned VelvetG source archive was used only as a reference to recover the
ProfileManager and Translator source whose public APIs were checked against r7.

Run BUILD.bat from the package root on Windows with JDK 8 or newer and network access
to the artifact URLs pinned in `source-patch/minecraft-1.8.9.json`. On Linux/macOS use ./BUILD.sh. The built
release is emitted under source-patch\built\.


FIX3 BUILD NOTE: verifier now JVM-verifies only changed/replacement classes and proves all other inherited baseline classes byte-identical. Cosmetic bytecode branding rewrites were removed to avoid invalidating legacy StackMapTable frames.
