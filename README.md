# VelvetG Lean native overhaul

[Download the patch ZIP](downloads/VelvetG-Lean-Overhaul1-PATCH-FIX1.zip) · [SHA-256](downloads/SHA256SUMS.txt)

**Installer FIX1** repairs Java executable discovery in PowerShell; extract the entire new ZIP into a fresh folder. The client patch itself is unchanged.

A reversible Java 8 patch for the exact collected `2.0.9.6-lean-fixed-20261016` installation.

- [Installation and settings](overhaul/README.md)
- [ChuStatsu v1.2.0 feature comparison](overhaul/reports/CHUSTATSU-FEATURE-MATRIX.md)
- [Executed tests](overhaul/reports/TESTING.md)
- [Performance evidence](overhaul/reports/PERFORMANCE.md)
- [Completed, partial and blocked work](overhaul/reports/LIMITATIONS.md)
- [Changelog](overhaul/reports/CHANGELOG.md)
- [Original collector instructions](COLLECTOR-README.md)

The download installs a separate launcher version/library and preserves the original installation. It includes source, deterministic patch tools, offline verification and rollback. It does not contain the collected client, Minecraft, original client assets, account files or raw diagnostics.

This is a substantial staged implementation, not complete ChuStatsu parity. Java 8 and offline/mock/installer checks are executed; Minecraft launch, live Pika authentication and FPS/GPU measurements are not claimed.
