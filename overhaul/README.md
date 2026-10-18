# VelvetG Lean Overhaul1 — reversible native patch

This patch targets your collected GlideClient **2.0.9.6-lean-fixed-20261016**, SHA-256 `3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813`. It implements a substantial first overhaul stage. It does **not** claim complete ChuStatsu parity or a tested Minecraft launch. Read `reports/CHUSTATSU-FEATURE-MATRIX.md` and `reports/LIMITATIONS.md` for remaining work.

No full client, Minecraft JAR, account files, collected logs, original assets or ChuStatsu code are redistributed. The installer transforms your exact installed copy using its existing shaded ASM, then validates every new/modified entry and proves other entries unchanged. No Gradle migration, Fabric, OneConfig, or Forge dependency is introduced.

## Install on Windows

1. Extract the entire ZIP into a folder. Close Minecraft and the launcher.
2. Run `INSTALL-WINDOWS.bat`. Default game directory: `D:\Roaming\.minecraft`. A Java 8 or newer runtime discoverable through `JAVA_HOME` or PATH is required; compilation and internet access are not required.
3. Run `VERIFY-INSTALLATION.bat`.
4. In the launcher create a **new installation** selecting version **VelvetG-Lean-Overhaul1**, using the same game directory and a maintained **Java 8** runtime. The collector reported old launches using Java 8u51 while 8u504 is installed; select the maintained runtime explicitly. Do not launch this client with Java 21 merely because the installer runs on it.

**Installer FIX1:** This package repairs the PowerShell Java-discovery error (“The expression after '&' ... was not valid”). Extract this entire corrected ZIP into a new folder and run its installer; mixing files from different packages fails checksum validation. The earlier invocation failure occurred before patch installation. If a receipt already exists, use Verify rather than reinstalling.

Your original library, launcher version, launcher profile and existing configuration are retained. The installer makes verified private backups and installs a separate library/version. It does not edit `launcher_profiles.json`. If the launcher hides custom versions, enable their display. The installer intentionally does not modify your existing launcher installation.

For a different directory or Java executable, run PowerShell directly (paths with spaces are supported):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\Manage-VelvetG.ps1" -Action Install -GameDir "D:\Roaming\.minecraft" -JavaPath "C:\Path To Java 8\bin\java.exe"
```

If several launcher versions reference the same original JAR, append `-SourceVersion "the original version folder name"`. A changed/unknown baseline is refused. No emergency patch is applied to an unrecognized build.

To roll back, close Minecraft and run `RESTORE-BACKUP.bat` (or the PowerShell command with `-Action Restore`). It verifies the originals/backups, then removes only the exact new library/version files. Choose your original launcher version. Backups and new settings remain. If installed files were edited after installation, rollback refuses to discard them automatically.

## Use the additions

With VelvetG enabled, enter **`/velvetlean`**. This opens a native Minecraft settings screen with search, editable values, masked API-key entry, status, safe import/export, and TAB positioning. Enter a setting's value and click **Apply**. Existing `/velvetg`, `/stats <player>` and `/velvetgdebug` controls remain.

| Setting | Accepted values / behavior |
|---|---|
| `maximumFPS` | `true` / `false`; disables table motion, optional backgrounds and panel/shadow embellishments. Saved original settings are not reset. Not a promise of higher FPS. |
| `autoKey` | Default `false`. Opt in to bounded `/apikey` acquisition on Pika when missing/rejected. Three attempts/connection, 60-second cooldown, 3-second join delay, 10-second response window. |
| `profiler` | `true` enables interval, main-thread CPU and TAB/HUD CPU sampling; `false` bypasses sampling. Click Export to write `config/velvetg-performance.txt`. |
| `tabLimit`, `hudLimit` | `1`–`40`. The HUD limit additionally respects the existing native HUD maximum; raise that setting too to display 40 HUD rows. Existing shared API fetch limit still applies. |
| `tabSort`, `hudSort` | `EXISTING`, `FKDR`, `WLR`, `HWS`, `LV`, `FK`, `WINS`, `BEDS`. Explicit sorts retain spectator handling, team grouping in active matches, party priority and cached friends. Default preserves original ordering/watchlist behavior. |
| `tabColumns`, `hudColumns` | `EXISTING` or ordered comma-separated IDs: `NAME,LV,RANK,FKDR,WLR,HWS,FK,WINS,BEDS,GUILD,PING,HEALTH,INSIGHT`. Unknown/duplicate IDs are ignored. Heads have existing separate toggles. |
| `tabFont`, `hudFont` | `EXISTING`, `MINECRAFT`, `REGULAR`, `MEDIUM`, `POPPINS`, or a local `.ttf`/`.otf` filename under `config/velvetg-fonts`. Poppins is bundled under SIL OFL 1.1. Two optional fonts/run, at most 8 MiB each; restart when selecting more. |
| `tabSpacing`, `hudSpacing` | `0` preserves native density spacing; `1`–`30` sets cell padding independently. Native density/scale/color controls remain. |
| `tabX`, `tabY` | GUI pixels. `tabX=-1` centers; default Y is 8. Use Place TAB in the screen; select existing HUD editor for HUD dragging/grid snap. Existing native scale/auto-fit remains. |
| `tabPersistent` | `true` also displays TAB after the key is released, subject to existing context/visibility controls. Default `false`. |
| `nametag` | Default `false`; enables cached statistics within the original nametag visibility checks. |
| `nametagStat` | `FKDR`, `WLR`, `HWS`, `LV`, `FK`, `WINS`, `BEDS`. `HWS` is **highest**, not current, winstreak. |
| `nametagPlacement` | `BESIDE` or `ABOVE`. No API calls or ratio formatting inside the renderer. |
| `animation` | `EXISTING`, `NONE`, `SLIDE`, `ZOOM`, `BOUNCE`, `POP`. These add show transitions; native row-arrival/resize motion is retained. Full exit-animation parity is pending. |
| `animationMs` | `50`–`1500`, shared show duration; defaults 180. |
| `tabBackground`, `hudBackground` | `NONE` (default), a local PNG filename under `config/velvetg-backgrounds`, `RANDOM`, or `CATBOX-abcdef.png` for an explicit six-character Catbox ID. Credential-free downloads occur only after opt-in; cache is bounded. Current opacity is 18%, stretched to the panel; advanced alignment/opacity editing is pending. |
| `headLayers` | Default `true` preserves the existing skin hat pass. `false` skips it. This is a hat layer, not a 3D head mesh. |

Manual API entry is local and masked; Ctrl+V works. An explicit `/apikey` command on Pika also opens a bounded capture window even when automatic fetching is disabled. Existing `glide/pika-api-key.txt` remains supported. A successful key save uses owner-only permissions and atomic replacement. Keys are excluded from exports and diagnostic reports. Only HTTPS `stats.pika-network.net` receives the authentication header, with redirects disabled. A generic private-profile HTTP 403 does not automatically rotate a key; a 401 or explicit authentication-error 403 does.

The gallery never deletes screenshot files on startup. Its old deletion records are no longer executed. Gallery trash hides a screenshot during the current session; the physical file remains. Discovery runs in a background worker, at most every 15 seconds while used. Visible files decode on demand; previews are capped at 1024 pixels per dimension, GPU cache at 64 MiB/24 entries, and uploads at one per frame. Full-resolution files remain available through the original Open action.

## Diagnostics and repeatable local measurements

`VERIFY-INSTALLATION.bat` is fully offline and checks original/backup hashes, every patch entry, unchanged baseline contents, class versions, and launcher SHA-1/size metadata. It does not initialize Minecraft or native libraries.

For before/after runs use the same Java 8, resolution, world/route, resource packs, video settings, FPS cap, warm-up, and sample duration. Record at least three runs each. Do not compare a crowded online lobby against an empty world.

```powershell
.\RECORD-BENCHMARK.ps1 -ProcessId 1234 -Label baseline -DurationSeconds 60
.\RECORD-BENCHMARK.ps1 -ProcessId 5678 -Label patched -DurationSeconds 60
```

This collects only process CPU/memory/thread counts, without command lines, account data or raw logs. For the patched client enable `profiler`, play the same scene, then Export in `/velvetlean`. Frame intervals are CPU-observed presentation-loop intervals; they are not GPU timings. Render-section samples cover TAB/HUD CPU work. Export also includes GC totals, bounded cache counts, API queue counts, and startup checkpoints. A full baseline frame-time/FPS comparison still needs local game execution.

## Rebuild / test source

Development requires JDK 17/21, Python 3.9+, your exact original client and Minecraft 1.8.9 JAR, and the installed launcher dependencies. Runtime additions and patch tools compile with `--release 8`. Set `JAVA_HOME`, `JAVA8` (a Java 8 executable), `BASELINE_JAR`, `MINECRAFT_JAR`, and `DEPENDENCIES_DIR`, then run `python build.py` and `python run-tests.py`. PowerShell installer tests additionally use `POWERSHELL_EXECUTABLE`; no binary baseline is supplied publicly. Run `python tests/test_java_discovery.py` and `python tests/test_installer.py` against the assembled distribution to check executable discovery and the installer transaction. See the reports for the actual executed environment and results.
