# Collector/restorer validation

Executed `python3 tests/test_collector.py` against the shipped PowerShell script with verified official PowerShell 7.4.13 (Linux). Package SHA-256: 59e5df675dacbfe45374c32c1bf2480168a33243423c9b19252d0476fd1b748c, checked against the release hashes from microsoft/PowerShell.

Actual result: **36 assertions passed**. Fixtures are synthetic, not copied player data. They exercise:

- A valid synthetic JAR over 24 MiB, paths containing spaces, matching/parent launcher JSON discovery, configured tweak class extraction.
- Byte-identical preservation of every original game/config/screenshot file.
- Two sub-32 MiB ZIP payload parts, exact part and whole-JAR hashes, shipped Python reconstruction, overwrite refusal, tampered-part rejection and removal of only the restorer's own incomplete output.
- Exclusion of synthetic key, username, UUID, IP and private server canaries from diagnostics; raw account/launcher profile files are excluded.
- Fixed QUAD enum preservation, sensitive configuration-section exclusion, safe crash runtime version and heap/GC flag extraction.
- Screenshot count-only handling for 250 corrupt images and an empty folder; no image decoding/copying.
- Missing installation, output-inside-game rejection, malformed configuration/parent version JSON, obvious bundled credential filename and a key-like value crossing a scan-buffer boundary.

The PowerShell script requires version 5.1 and avoids PowerShell 7-only language features; tests ran on 7.4.13 Linux, not Windows cmd.exe/PowerShell 5.1. Batch quoting and exit propagation were reviewed, not executed on Windows. Read permissions/NTFS locking may differ. No real installed Lean file, actual API secret, live server, native OpenGL startup, launcher modification or performance benchmark was exercised.

No executable third-party runtime, game file or ChuStatsu code/assets is shipped with the collector.
