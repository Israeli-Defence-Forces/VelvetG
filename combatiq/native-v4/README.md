# CombatIQ native RC1

Use HANDOFF.md for the generated binary package and Windows acceptance procedure; FINAL-REPORT.md at the repository root is the current validation status. Existing v4 artifacts and older reports are historical.

Build: `python build_v4.py --v3 EXACT-V3.jar --minecraft minecraft.jar --native-module --out CombatIQ-RC1.jar`. Requires a JDK, pinned Minecraft 1.8.9 and its official Netty library (`--netty`, or auto-discovered under `deps/` beside Minecraft). The builder refuses unknown input hashes. It preserves unrelated baseline entries, verifies all added Java 8 classes/dependencies/Mixin config, and emits an exact-hash installer and manifest.

Run tests: `python -m unittest discover -s combatiq/native-v4 -p 'test*.py' -v` from repository root. Run the kernel/protocol checks with `python combatiq/build.py`. Headless transformation: `python verify_mixins.py --help`; requires Java 8 and exact local dependencies. This does not launch Minecraft.

Package: `python release_bundle.py --jar CombatIQ-RC1.jar --kernel ../build/combatiq-kernel.jar --evidence evidence --readme HANDOFF.md --out CombatIQ-RC1-Release.zip`. Includes the real client, pinned installer, Windows wrappers, analyzer/calibration zipapp, kernel and supplied evidence; performs ZIP and payload checks. Keep the resulting private client package out of this public repository.
