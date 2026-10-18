# VelvetG Lean — baseline collection and ChuStatsu audit

The installed `2.0.9.6-lean-fixed-20261016` client is not accessible from this cloud workspace. This branch contains a read-only collector and a source-based ChuStatsu v1.2.0 audit. It is **not an upgraded client or an installer**. Older Integration1/2 binaries are not used as substitutes for the current Lean baseline.

## Download and collect

[Download the small Windows collector ZIP](https://github.com/Israeli-Defence-Forces/VelvetG/raw/refs/heads/tools/lean-collection/downloads/VelvetG-Lean-COLLECTOR.zip)

1. Close Minecraft and its launcher. Extract the collector ZIP into a new folder outside `.minecraft`.
2. Run `COLLECT-WINDOWS.bat`. The default game directory is `D:\Roaming\.minecraft`; the script targets the exact installed `lean-fixed-20261016` JAR. It does not silently substitute an older JAR.
3. Upload `VelvetG-DIAGNOSTICS.zip` and **every** `VelvetG-*-PART-*.zip` from the generated `VelvetG-Collection-*` folder into the chat.

No admin access, Java installation, Python or network is required on the collecting Windows PC. Each part ZIP is below the 32 MiB upload limit. The Python restorer is for the analysis machine and is not required to collect on Windows.

The script reads the client and matching custom version JAR, exports sanitized launcher/dependency metadata, allowlisted settings and stack frames, and counts screenshots. It excludes raw account/profile files, API-key files, arbitrary strings/chat/log messages, screenshots and resource packs. It refuses obvious embedded key material rather than changing the JAR. It does not install, patch, reset or delete any Minecraft file. Read [collector instructions](collector/README-FIRST.txt) before running.

## Actual checks

[TESTING.md](TESTING.md): 36 assertions with actual PowerShell 7.4.13 on Linux, including path spaces, exact file preservation, 250 corrupt screenshot files, empty gallery, sanitized settings/logs/version data, split/restored hashes, malformed metadata, missing installation, embedded-key detection and tamper rejection. Windows PowerShell 5.1 execution remains untested. The script targets its syntax and .NET APIs.

These are collector tests. No current Lean client startup, native graphics, API authentication or FPS benchmark has run.

## ChuStatsu integration work

[Feature matrix](CHUSTATSU-FEATURE-MATRIX.md): actual v1.2.0 tag `26a3d83c78d14322dd211254bdb33e7dae984d5a`, 45 feature groups and 104 configuration controls. Current-client comparisons remain explicitly blocked until collection arrives. Java code is ARR; independently implement useful behavior rather than copying it. Poppins has a separate OFL font licence. No reference code/assets or client binary is redistributed by this branch.

The next stage audits the exact installed screenshot/API/classloader paths, creates reversible hash-targeted patches and preserves existing settings/features. Then performance instrumentation, bounded API/auth changes, native TAB/HUD/nametag integration and Windows verify/install/restore tools can be tested against the real baseline. No FPS improvement or completed feature-parity claim is made yet.
