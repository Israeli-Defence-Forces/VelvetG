# VelvetG / Violet r8 Integration2

This delivery contains the updated editable source patch, a compiled client integration and the original launcher installation workflow. It builds against the exact historical r7 binary foundation. Integration2 repairs the inherited Scout JSON stack-frame VerifyError reproduced from the supplied crash report. All nine verification/test stages also pass on Temurin Java 8 (1.8.0_504). No Gradle replacement, Fabric, Ornithe, OneConfig or new runtime dependency was introduced.

## Downloads

- [Installer ZIP, including the compiled JAR](https://github.com/Israeli-Defence-Forces/VelvetG/raw/refs/heads/delivery/r8-integration1/downloads/VelvetG-r8-Integration2-INSTALL.zip)
- [Source ZIP](https://github.com/Israeli-Defence-Forces/VelvetG/raw/refs/heads/delivery/r8-integration1/downloads/VelvetG-r8-Integration2-SOURCE.zip)
- [SHA-256 checksums](downloads/SHA256SUMS.txt)

Extract the installer ZIP into a new folder and read `INTEGRATION-NOTES.md` before running `Install-VelvetG.bat` on Windows. The JAR is inside that archive. The installer and live game behavior have not been tested on Windows here.

## What changed

Existing Violet input/CPS, Auto Tool, Auto Soup and diagnostics are enhanced. BedWars Awareness and World Awareness are added, default off. Existing Combo Counter, Damage Particles and Target Info are enhanced in place. Native profiles, existing HUD layout indices, the compact Violet design and Java 8 bytecode target are retained.

- [Changelog](CHANGELOG-INTEGRATION.md)
- [Actual testing report](TESTING-REPORT.md)
- [Build results](BUILD-RESULTS.txt)
- [Architecture audit](AUDIT-INTEGRATION.md)
- [Installation, limits and remaining checks](INTEGRATION-NOTES.md)
- [Reviewable integration patch](INTEGRATION.patch)

The full Linux source-patch build passed **438 behavioral/repair checks**, plus verification of **280 inherited startup classes**, plus JVM/structural verification. 1,965 inherited classes remain byte-identical, and the compiled patch entries match the delivery JAR.

**Compatibility boundary:** the supplied baseline is a LaunchWrapper/GlideTweaker client, not a standalone Forge `@Mod` artifact. This delivery preserves that workflow. Standalone Forge porting, live Minecraft/OptiFine/Forge compatibility, hardware graphics and Windows installer verification remain unfinished. See the detailed notes for approximation limits and optional omissions.

## Build from this repository

Keep the original two uploaded BASE-PART archives. They are required to reconstruct the baseline; they are deliberately not duplicated in this Git repository.

```sh
python3 RESTORE-BASE.py /path/VelvetG-r8-BASE-PART-1.zip /path/VelvetG-r8-BASE-PART-2.zip
sh BUILD.sh
```

On Windows use `python RESTORE-BASE.py ...` and `BUILD.bat`. Use a JDK; builds download pinned, hash-checked Minecraft 1.8.9 dependencies. The output target remains Java 8. This is an editable source-patch development package, not full historical GlideClient source. Embedded baseline notices are retained in the compiled JAR; Orven concepts were independently implemented.

## Updating from Integration1

Close Minecraft and the launcher. Extract the Integration2 INSTALL ZIP into a new folder, then run `Install-VelvetG.bat`. Reopen the launcher and select VelvetG. This updates the version/library reference to the repaired JAR. The earlier installer-only fix ZIP cannot repair the Scout crash. Old Integration1 download files are retained for history; use Integration2 for installation.
