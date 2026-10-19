# VelvetG Lean native overhaul — Overhaul2

[Download Overhaul2](downloads/VelvetG-Lean-Overhaul2-PATCH.zip) · [SHA-256](downloads/SHA256SUMS.txt)

A reversible native Java 8 patch for the exact collected `2.0.9.6-lean-fixed-20261016` client. Overhaul2 installs alongside the original and Overhaul1, with separate settings/receipts. The FIX1 Java-discovery repair is included.

- Independent waiting/game TAB/HUD profiles, drag/grid/scale, column reorder UI, typography/palettes, image geometry and bounded row exits.
- Shared profile-request coalescing, safe authentication/QUAD preservation, cached nametag/friend controls and visible match counters.
- Cape preview preload removal, disabled-context table-build suspension, bounded caches and Maximum FPS presentation controls.
- Named secret-free profiles, API status UI, source, tests, offline verification and rollback.

[Install and settings](overhaul/README.md) · [Feature comparison](overhaul/reports/CHUSTATSU-FEATURE-MATRIX.md) · [Changelog](overhaul/reports/CHANGELOG.md) · [Tests](overhaul/reports/TESTING.md) · [Performance evidence](overhaul/reports/PERFORMANCE.md) · [Remaining work](overhaul/reports/LIMITATIONS.md)

235 Java behavioral checks and 68 changed/new class verifications pass. Actual PowerShell 7/Linux installer transactions preserve original/Overhaul1 canaries. Three selections make 3/3/1 profile requests in the original/Overhaul1/Overhaul2 loopback workload. The new roster CPU microbenchmark was slower in the recorded comparison; no game FPS gain is claimed.

This remains staged implementation, not complete parity. Minecraft launch, live Pika authentication, native/GPU behavior, Windows cmd.exe/PowerShell 5.1 and game FPS remain untested here. The package contains no collected client/Minecraft JAR, original client assets, accounts, credentials or raw diagnostics. ChuStatsu ARR code/assets are not redistributed; Poppins has its included SIL OFL license.

[Original collector instructions](COLLECTOR-README.md)
