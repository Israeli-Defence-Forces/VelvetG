# RC1 verified artifacts

Code commit: `d75d79e5b1d8022f904d3175145d17557073f31d`. Draft PR: https://github.com/Israeli-Defence-Forces/VelvetG/pull/3 . No production merge.

The actual private release ZIP is `VelvetG-CombatIQ-RC1-Release.zip`, 36,121,236 bytes, SHA-256 `b9eb3d01e0b1d1d45f2176e549853b1575d2bc6502e172ba4223bfef7c8b1926`. It contains 26 payload artifacts plus CONTENTS.json, SHA256SUMS.txt and VERIFY.py. All 28 checksum-list entries passed independent verification; a second ZIP build was identical and tampering with the analyzer was refused. Final ZIP verification is recorded separately to avoid a circular self-checksum.

Client: `CombatIQ-RC1.jar`, SHA-256 `43f3a3f9b477986bf76b8ec8cc896df3ff1cc3e294d7526e16bc15fc128a3ac9`. Two builds in separate checkout paths produced identical JAR bytes. Native HUD module included; unrelated baseline entries preserved. Complete exact build details are in CombatIQ-RC1.manifest.json.

Pinned installer: `CombatIQ-RC1.install.py`, SHA-256 `39bc7a34cbc2943bc0e587b998cd01b8a2f7f96541d7955029b25fbbb7a403b8`. INSTALL/CHECK/ROLLBACK/UNINSTALL/RECOVER.cmd invoke this new installer. It was exercised with the real built JAR in a temporary Linux game directory, preserving original JAR/profile bytes. This is not Windows validation.

Standalone kernel: `combatiq-kernel.jar`, SHA-256 `0507abb1bf7d8f53b66b7ab94c01aa17114a0dafa9e6394898b49f1d688c26df`. Runnable analyzer/calibration zipapp: `CombatIQ-Analyzer.pyz`; its exact digest is in the private ZIP's CONTENTS.json/SHA256SUMS.txt.

101 native tests: 100 pass, 1 historical CSV test skipped, 0 failures; locally executed on Java 8 and 21. CI on the exact code commit succeeds on Java 8/17/21. Kernel: 60 assertions; protocol: 3 tests. Six new Mixins transform pinned target bytecode in a headless Java 8 harness. Original bootstrap warning retained in the log. The build manifest's Mixin flag stays false because the builder does not apply the full original/new client Mixin configuration; the separate scoped transformation check is recorded in mixin-transformation.json.

Six synthetic benchmark configurations, three repetitions each, zero event loss. No gameplay FPS, GPU, whole-client allocation or frame-time guarantee. See benchmark.json/performance-summary.txt.

Minecraft launch, full client Mixin coexistence, NanoVG/editor/config persistence, Windows OS behavior, historical CSV reproduction and authoritative private-server trials remain UNVERIFIED/UNEXECUTED. There are no failures in the executed checks; this does not certify untested behavior. Runtime acceptance procedure: ../../HANDOFF.md . Private JAR/ZIP bytes and download links are deliberately excluded from the public repository.
