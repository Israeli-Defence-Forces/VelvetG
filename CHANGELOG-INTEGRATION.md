# r8 Integration2 changes

## Integration2 startup crash repair

The user-provided crash report identifies a VerifyError in Scout's PikaApiClient.json: a multi-catch handler's StackMapTable incorrectly declares java/lang/Object where a Throwable-compatible type is required. The failing class was byte-identical to the restored historical r7 baseline. Integration2 recomputes that class's frames with the real dependency hierarchy; its methods, fields, instructions and behavior remain unchanged.

A regression check reproduces the original error on Java 8 and now JVM-verifies all 280 inherited Scout/module implementation classes without initializing the game. Three focused checks confirm the repaired JSON RuntimeException frame and preservation of the original code. All nine verification/test stages pass on Temurin Java 8 (1.8.0_504) as well as the standard JDK 21 build. There are now 438 behavioral/repair assertions; 1,965 other inherited classes remain byte-identical. This does not establish a successful live Windows/OptiFine launch.

The existing installer filename fix is retained. Integration2 has a distinct library/JAR version; the launcher profile name stays VelvetG. Re-run the new installer to update the launcher library reference. The earlier 5 KB installer fix does not repair this client crash.

## Integration1 module changes retained

This release extends the supplied r8 source patch against the exact r7 binary foundation. The original scheduler, launcher installer, native profiles, existing UI palette/fonts and Java 8 runtime target remain in place. No Fabric, Ornithe, OneConfig or new runtime library is included.

- Violet: fixed-size CPS histories, separate physical/generated counts in an additive Advanced HUD, five-second average, optional bounded in-memory diagnostics and explicit report button. Compact/Detailed layout indices and click scheduling remain unchanged.
- Auto Tool: retain native harvesting, strength and efficiency logic; add durability tie-breaking and cached All/Whitelist/Blacklist block filters. An equally fast selected tool stays selected; existing manual override and switch-back behavior remain.
- Auto Soup: confirm inventory/health changes before reusing an instant-use attempt; optional inventory-screen-only hotbar refill with a single ordinary slot-swap operation and acknowledgement guard. Existing instant/normal use, health thresholds and restoration remain.
- BedWars Awareness (new, off by default): five independently configurable projectile types; directional HUD indicators, distance, bounded cached terrain trajectories, approximate approach warnings; deduplicated fireball, pearl, obsidian and diamond-legging alerts; optional throttled sound; configurable scoreboard activation.
- World Awareness (new, off by default): directly observed bed positions, distance/material/destruction records and optional depth-tested bed boxes; visible player labels/health, team colours and boxes. Unknown bed teams are not inferred.
- Existing Combo Counter: correlated local-attack/hurt observations, configurable timeout, target-change and incoming-hurt resets, world/disable cleanup; original Icon setting and HUD identity retained.
- Existing Damage Particles: bounded observed health deltas, configurable lifetime/size/opacity/animation, tentative local critical feedback, depth-tested billboards and lifecycle cleanup.
- Existing Target Info: additive distance setting and disappearance/world cleanup; existing styles, name/head, armour/health presentation remain.
- Existing compact assist settings UI gains text fields and pagination for additional controls. Native profile keys stay stable; missing new-module keys in older profiles keep those modules disabled.

Implementation uses the inherited event bus, module manager, settings, NanoVG and render accessors. VioletPatch now also hooks Target Info, in addition to its three original inherited integration classes. Combo Counter and Damage Particles are source replacements of existing modules, rather than duplicates. Five existing Violet mixins are retained. Bounded caches are reset on world/disable transitions; observations run on the Minecraft thread.

Orven BW 0.7.3 was inspected as a conceptual reference. Its provided archive contains no project licence establishing permission to copy source, and targets an incompatible loader/Java runtime. New functionality was independently implemented. Original embedded licences/notices remain; downloaded dependency notice locations are inventoried separately.

## Installer correction

The Windows batch wrapper now reads the payload filename from RELEASE.properties instead of a hard-coded r8 name. A present prebuilt Integration1 JAR goes directly to the existing hash-verifying PowerShell installer. Source auto-build checks that BUILD.bat exists; missing payloads produce a clear error, and child-installer exit codes are propagated correctly. The compiled JAR is unchanged. See INSTALLER-FIX-NOTES.txt for the patch procedure and actual validation limits.
