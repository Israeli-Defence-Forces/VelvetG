# VelvetG r8 Integration2

Integration2 fixes the inherited Scout PikaApiClient.json stack-frame VerifyError reported during game initialization. Only verification metadata is repaired; original Scout methods/instructions remain unchanged. The installer filename fix and all Integration1 Violet/BedWars/combat/world improvements are retained.

All nine verification/test stages pass, including 438 behavioral/repair assertions and 280 inherited Scout/module startup class loads, on the normal JDK 21 build and an actual Temurin Java 8 runtime. Live Windows/OptiFine/Forge testing remains outstanding.

Extract the new INSTALL ZIP into a new folder, close Minecraft and the launcher, run Install-VelvetG.bat, then reopen the VelvetG profile. The older installer-only fix ZIP cannot repair the crashing client JAR.

See CHANGELOG-INTEGRATION.md, TESTING-REPORT.md and INTEGRATION-NOTES.md for actual evidence, installation and remaining limits. This remains the supplied LaunchWrapper client integration, not a standalone Forge mod.
