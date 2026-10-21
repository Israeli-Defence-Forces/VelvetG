# CombatIQ RC1 handoff

This package contains an actual experimental native client built from the SHA-pinned Sampling Fix v3 JAR. It is not runtime-certified. Java 8 bytecode; Python 3.9+ is required for installer and analyzer. Use the source/v3 client installation that already launches successfully. Its version JSON must directly declare one `me.eldodebug:GlideClient` library with an explicit artifact path and a recognized original/v3/v4 JAR hash. Inherited-only GlideClient declarations are refused.

Extract the entire RC1 ZIP into one directory. In Command Prompt there, run:

```bat
py -3 VERIFY.py
INSTALL.cmd --game-dir "%APPDATA%\.minecraft" --source-version "VelvetG-CombatIQ-Experimental" --check
INSTALL.cmd --game-dir "%APPDATA%\.minecraft" --source-version "VelvetG-CombatIQ-Experimental"
CHECK.cmd --game-dir "%APPDATA%\.minecraft"
```

Replace the game directory and source-version ID with the existing working installation if different. The installer creates `VelvetG-CombatIQ-RC1`, preserving original files. Select that profile in the launcher; the installer does not edit launcher accounts or select a profile. Start using the known working client's Java/runtime configuration.

Enable CombatIQ in the native module list; Recording defaults off. Configure the export folder, seconds, MiB, rotation and diagnostics before starting a recording. Limits apply next recording; analyzer choices apply live. Use Export local report for a bounded asynchronous snapshot report. Analyze a finalized CSV with:

```bat
py -3 CombatIQ-Analyzer.pyz "C:\recordings\combatiq-native-session-0000.csv" --format text
py -3 CombatIQ-Analyzer.pyz "C:\recordings\combatiq-native-session-0000.csv" --out report.json
```

For rotated sessions, provide every CSV segment as a separate quoted argument; order is reconstructed. `.partial`, missing segments, event loss and bad checksums invalidate evidence. Legacy schema 1/2 recordings remain supported. `py -3 CombatIQ-Analyzer.pyz calibrate --help` exposes the held-out private-server trial tool; it supplies no universal optimum.

Recovery/rollback/uninstall commands (close Minecraft first):

```bat
RECOVER.cmd --game-dir "%APPDATA%\.minecraft"
ROLLBACK.cmd --game-dir "%APPDATA%\.minecraft" --check
ROLLBACK.cmd --game-dir "%APPDATA%\.minecraft"
UNINSTALL.cmd --game-dir "%APPDATA%\.minecraft"
```

Recovery rolls back an interrupted transaction and refuses outside edits. Rollback restores the previous owned RC1 state; first-install rollback removes RC1 and leaves the source profile available. Uninstall removes only receipt-owned RC1 files. Unknown/tampered files are refused.

Indispensable runtime acceptance (NOT EXECUTED here): verify actual launch and all original/new Mixins together; original profile still launches; native module appears once; idle/recording off produces no recording; input focus, remapped forward and GUI transitions remain correct; sprint/raw input/packet stage evidence appears on a private test server; stop/restart/reconnect/rotation produce valid sessions; drag/scale/font/theme/editor/config restart persist correctly; export and storage failure remain responsive; shutdown drains or clearly leaves partial evidence; Windows install/check/recovery/rollback/uninstall preserve originals; matched real gameplay A/B measures CPU, allocations, frame-time percentiles and loss. Combat acceptance claims require authoritative private-server labels.

See FINAL-REPORT.md and the package's validation folder for scope, raw logs and exact counts. Historical v4 upgrade files are not the RC1 installer.
