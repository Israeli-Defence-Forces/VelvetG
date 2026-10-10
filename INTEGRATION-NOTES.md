# Build, installation and remaining verification

## Rebuild

Extract the delivered SOURCE ZIP, then run the original restoration script with the two uploaded foundation archives:

```sh
python3 RESTORE-BASE.py /path/VelvetG-r8-BASE-PART-1.zip /path/VelvetG-r8-BASE-PART-2.zip
sh BUILD.sh
```

Windows: use `python RESTORE-BASE.py ...` and `BUILD.bat`. The existing PowerShell builder has the same additional test stages; it was not executed on Windows here. Use a JDK, not a runtime-only installation. The Python build was exercised with JDK 21 using `--release 8`; original JDK 8 handling remains. Minecraft/runtime target remains Java 8. Builds download hash-checked, pinned Mojang 1.8.9 dependencies. No Gradle replacement was introduced.

Restored baseline: 36,799,945 bytes; SHA-256 `737b416bf9256e7f22d28768bb6f54e6819ad797301251065298e58827e5293e`. The part manifest and original restoration script are included. The SOURCE ZIP deliberately excludes the binary foundation, downloads and generated work directory; reuse your original two BASE-PART ZIPs.

## Installation

Extract the INSTALL ZIP into a new directory. Close Minecraft and the launcher. Read `README-FIRST.txt`, then run `Install-VelvetG.bat` (or the included PowerShell installer). The existing installer verifies the release JAR hash, backs up launcher/version state and selects the VelvetG profile. It retains the existing profile name and uses the new Integration2 library version. It has not been executed against a real Windows Minecraft installation here.

For an isolated manual validation installation:

```powershell
.\Install-VelvetG.ps1 -MinecraftDir 'C:\Minecraft-Test' -WithoutOptifine -DoNotSelectProfile
```

First prepare Minecraft 1.8.9 in that directory. After checking the isolated install, repeat validation with your intended OptiFine configuration. Uninstall/rollback helpers remain provided. Do not put the produced client JAR into `mods/`: this supplied baseline is installed through LaunchWrapper/GlideTweaker.

## Compatibility boundary

The supplied source-of-truth baseline has no Forge `@Mod` entry point or standalone Forge mod metadata. Its installer inherits vanilla 1.8.9 and uses LaunchWrapper, with optional OptiFineForgeTweaker. Therefore the delivered artifact is a compiled client integration for that existing installer, **not a verified standalone Forge mod**. No claim of Forge coexistence is made. Satisfying the requested standalone Forge output needs a deliberate loader/bootstrap port and a live Forge compatibility test; replacing this historical build system without that work would be misleading.

## Implemented limits and unfinished work

- Projectile arrows are directional indicators inside the movable compact HUD, rather than edge-anchored overlays. Paths approximate up to 16 future ticks, with at most eight projectiles and 128 block rays per observation pass. They model drag/gravity/water and fireball acceleration, not entity collision, moving-player prediction, server corrections or perfect impact timing. Approach warnings are proximity estimates.
- Match detection uses the visible sidebar, a configurable BedWars marker and conservative state tokens. Unrecognised formats remain UNKNOWN and gated features do not activate. No universal server adapter or complete map database is supplied.
- Bed records begin with crosshair observations, are capped at 32, and track directly observed nearby materials. Bed halves can appear as separate records. No lobby-template inference, reliable bed-team attribution or complete defence reconstruction is supplied. Destruction checks require a loaded, unobstructed cached position.
- Player information requires visibility checks, ignores invisible entities and is capped at eight cached players. Team colours come from native scoreboard teams. Skeleton overlays and outline shaders were not added.
- Combo events indicate recent local-attack/hurt correlation, not server-confirmed damage attribution. Damage numbers indicate observed health changes, including healing; delayed updates can combine hits. `crit?` explicitly marks a tentative local critical attempt. Optional combo fade was not added.
- Auto Soup refill works only with the player's own inventory screen open, an empty cursor and no item use. It replaces an empty/bowl hotbar slot; it does not silently discard bowls or rearrange a closed inventory.
- Diagnostics are disabled by default, hold up to 128 events in memory and write only an explicit throttled report through the existing logger. No automatic log export/parser is supplied.
- AutoBlock was evaluated separately; the reference includes packet queuing/delay behavior, which was excluded. No AutoBlock module was added.
- Native OpenGL/NanoVG rendering, live client startup, Forge/OptiFine compatibility, multiplayer behavior, the user’s exact Oracle 8u51 Windows runtime, FPS/memory benchmarking and Windows install/reinstall/rollback/uninstall remain untested. Recorded rendering checks do not establish hardware rendering correctness. Existing third-party dependency licence notice locations were inventoried; full legal review of the historical binary is not claimed.

## Live validation checklist

1. Launch an isolated 1.8.9 install with Java 8, first without OptiFine, then with the intended configuration; inspect startup/mixin logs.
2. Load old and new profiles; inspect all four Violet pages, advanced/legacy HUDs and paginated awareness controls at multiple GUI scales. Change bindings, save, restart, and check persistence.
3. Exercise menu opening, disconnect/reconnect, death/respawn, dimension/world changes, module disable and manual hotbar changes. Confirm no stale clicks, soup attempts, targets or observations.
4. On a permitted test server, test each projectile type, walls/water/range, sound/alert cooldowns, known/unknown scoreboards and own-projectile exclusion.
5. Observe beds/materials/destruction and visible/invisible/occluded players; confirm depth testing and GL state with other render modules.
6. Test actual combat updates, crit uncertainty, combo resets, soup inventory acknowledgement and Auto Tool enchantment/durability/filter behavior.
7. Measure frame/tick timings and allocations on target hardware. No FPS improvement figure is asserted.

## Integration2 crash repair and update

The inherited Scout JSON exception-handler frame was invalid. Integration2 repairs its verification metadata and preserves its instructions/API. The original error is reproduced on Java 8; the repaired JAR passes all nine test stages on Temurin 8u504 for Linux, including 280 inherited startup classes. A live Windows/OptiFine launch remains untested. Close the game and launcher, extract the new Integration2 installer into a separate directory, run Install-VelvetG.bat, then reopen the VelvetG profile. The installer updates the library version, retains its backup/rollback behavior and leaves saved module configuration in place. The previous tiny batch-file fix does not replace the crashing JAR.
