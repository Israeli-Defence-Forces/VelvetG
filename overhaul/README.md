# VelvetG Lean Overhaul3 release candidate — reversible native patch

This patch targets your collected GlideClient **2.0.9.6-lean-fixed-20261016**, SHA-256 `3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813`. It continues the staged native overhaul with the additions listed below. It does **not** claim complete ChuStatsu parity or a tested Minecraft launch. Read `reports/CHUSTATSU-FEATURE-MATRIX.md` and `reports/LIMITATIONS.md` for remaining work.

No full client, Minecraft JAR, account files, collected logs, original assets or ChuStatsu code are redistributed. The installer transforms your exact installed copy using its existing shaded ASM, then validates every new/modified entry and proves other entries unchanged. No Gradle migration, Fabric, OneConfig, or Forge dependency is introduced.

## Install on Windows

1. Extract the entire ZIP into a folder. Close Minecraft and the launcher.
2. Run `INSTALL-WINDOWS.bat`. Default game directory: `D:\Roaming\.minecraft`. A Java 8 or newer runtime discoverable through `JAVA_HOME` or PATH is required; compilation and internet access are not required.
3. Run `VERIFY-INSTALLATION.bat`.
4. In the launcher create a **new installation** selecting version **VelvetG-Lean-Overhaul3**, using the same game directory and a maintained **Java 8** runtime. The collector reported old launches using Java 8u51 while 8u504 is installed; select the maintained runtime explicitly. Do not launch this client with Java 21 merely because the installer runs on it.

**Upgrading from Overhaul1/2:** Install this package into the same Minecraft directory. Overhaul3 has a separate version, library, backup/receipt directory and additive settings file. It reads v2 settings first, then legacy Lean settings on first use and saves edits to `config/velvetg-lean-v3.properties`; the old file is retained. Overhaul1 and Overhaul2 can remain installed. Choose **VelvetG-Lean-Overhaul3** in a new launcher installation. Its rollback removes only Overhaul3. The FIX1 Java executable-discovery repair is included.

Your original library, launcher version, launcher profile and existing configuration are retained. The installer makes verified private backups and installs a separate library/version. It does not edit `launcher_profiles.json`. If the launcher hides custom versions, enable their display. The installer intentionally does not modify your existing launcher installation.

For a different directory or Java executable, run PowerShell directly (paths with spaces are supported):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\Manage-VelvetG.ps1" -Action Install -GameDir "D:\Roaming\.minecraft" -JavaPath "C:\Path To Java 8\bin\java.exe"
```

If several launcher versions reference the same original JAR, append `-SourceVersion "the original version folder name"`. A changed/unknown baseline is refused. No emergency patch is applied to an unrecognized build.

To roll back, close Minecraft and run `RESTORE-BACKUP.bat` (or the PowerShell command with `-Action Restore`). It verifies the originals/backups, then removes only the exact new library/version files. Choose your original launcher version. Backups and new settings remain. If installed files were edited after installation, rollback refuses to discard them automatically.


## Overhaul3 changes and local acceptance

The custom library now uses **local-only empty artifact/repository URLs**, with actual local Maven path, SHA-1 and size. It has no invented Mojang/custom download endpoint. This follows the local-installed-library convention; actual official-launcher validation/repair behavior remains a required local smoke check. A missing/corrupt artifact must be repaired using this exact patch package, never a guessed network URL. Inherited dependencies and their real metadata are retained; the exact verified vanilla game JAR is selected.

The installer uses a persistent exact-hash transaction journal and an exclusive reusable lock. Interrupted moves/removals can recover on the next invocation; changed/read-only outputs are retained and refused. Original and backup/source hashes are checked before commit. Existing Overhaul1/2 versions/settings are retained. Do not modify/remove backup or receipt files manually.

Default roster preparation no longer copies the input list, inherited numeric render settings no longer create parse exceptions, font-name validation no longer repeats for registered fonts, cached nametag joins invalidate on stat/team-format changes, hidden tables no longer force model reconstruction every tick, and an unused per-head native image-size query/two discarded arrays are removed. No game FPS gain is claimed; read the reproducible CPU/allocation results in reports/PERFORMANCE.md.

Presets now select `presentationPreset=CUSTOM/MAXIMUM_FPS/BALANCED/VISUAL`, without overwriting custom appearance values. CUSTOM restores them; explicit profile loads retain the chosen preset. Maximum FPS suppresses optional head hat layers, table embellishments, images and motion while retaining heads/core data. Its scope is the integrated presentation subsystem, not global Minecraft world rendering.

After installation run **TEST-OVERHAUL3-WINDOWS.bat**. It is offline by default. Optional authentication: `TEST-OVERHAUL3-WINDOWS.bat -Online -Player YourName`; the selected Java runtime reads the existing local key privately and makes one HTTPS MONTHLY/QUAD request. It exports only status, never a response body/key. Native automatic chat capture remains a separate in-game smoke check.

`WINDOWS-SMOKE-TEST.md` at the package root (source: `installer/WINDOWS-SMOKE-TEST.md`) covers launcher checks, game smoke tests and matched Overhaul2/3 benchmark runs. The kit collects allowlisted startup/frame CSV data and exception/warning counts, excluding raw logs, usernames, addresses, command lines and credentials. No game is launched automatically. The client records startGame/splash/Glide/module intervals asynchronously into config/velvetg-startup.properties and exports the last 4096 frame intervals through /velvetlean Export. Inspect export ages and collect freshly for each run.

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
| `tabX`, `tabY` | GUI pixels. `tabX=-1` centers; default Y is 8. Use Move TAB in the screen; select existing HUD editor for HUD dragging/grid snap. Existing native scale/auto-fit remains. |
| `tabPersistent` | `true` also displays TAB after the key is released, subject to existing context/visibility controls. Default `false`. |
| `nametag` | Default `false`; enables cached statistics within the original nametag visibility checks. |
| `nametagStat` | `FKDR`, `WLR`, `HWS`, `LV`, `FK`, `WINS`, `BEDS`. `HWS` is **highest**, not current, winstreak. |
| `nametagPlacement` | `BESIDE` or `ABOVE`. No API calls or ratio formatting inside the renderer. |
| `animation` | `EXISTING`, `NONE`, `SLIDE`, `ZOOM`, `BOUNCE`, `POP`. These are global show defaults; independent show/entry/resize/exit controls are described below. Optional independent hide controls are described below. |
| `animationMs` | `50`–`1500`, shared show duration; defaults 180. |
| `tabBackground`, `hudBackground` | `NONE` (default), a local PNG filename under `config/velvetg-backgrounds`, `RANDOM`, or `CATBOX-abcdef.png` for an explicit six-character Catbox ID. Credential-free downloads occur only after opt-in; cache is bounded. Image opacity/alignment/scale are configurable below. |
| `headLayers` | Default `true` preserves the existing skin hat pass. `false` skips it. This is a hat layer, not a 3D head mesh. |

## Independent presentation controls

Each panel supports base settings such as `tabFont`, plus independent `tabWaitingFont` / `tabGameFont` overrides. `INHERIT` uses the panel's base setting; `EXISTING` preserves the native control. The same names work with `hud`. Search for `Waiting`, `Game`, or a suffix in `/velvetlean`. Changes are additive; existing presets/settings remain available.

| Suffix | Values / behavior |
|---|---|
| `Limit`, `Sort`, `Columns`, `Font`, `Spacing`, `Background` | Existing Lean controls, now independently overridable for waiting and active games. Click a Columns setting to open toggle/Up/Down editing. |
| `Header`, `Heads`, `RankWithName`, `AlternatingRows`, `Dividers`, `Shadow` | `EXISTING`, `true`, `false`. Per-context overrides use `INHERIT` by default. |
| `Scale` | `EXISTING` or `50`–`150` percent, before native fit/scale multiplier. HUD position still uses its existing native editor. |
| `FontSize`, `NameWidth`, `CornerRadius`, `Opacity` | `EXISTING` or 6–20 font pixels, 40–300 name text-width cap, 0–20 radius, 0–100 panel opacity. Name width is a text cap; native minimum/rank/marker widths still apply. |
| `TextColor`, `SecondaryColor`, `HeaderColor`, `PanelColor`, `RowColor`, `SeparatorColor` | `EXISTING` or six RGB hex digits. Row/separator colors retain native alpha. Team/rank/stat colors keep their normal rules. |
| `ImageOpacity`, `ImageScale`, `ImageAlign`, `ImageZoom` | 0–100%; `STRETCH`/`COVER`/`CONTAIN`; `CENTER`, edges/corners such as `TOP_LEFT`; 25–400%. Decoding stays asynchronous and uploads stay on the render thread. |
| `HideAnimation`, `HideMs` | `NONE` (default), `FADE`, `SLIDE`, `ZOOM`, `BOUNCE`, `POP`; 50–5000 ms. TAB release and HUD hide-while-TAB retain only valid native tables. Module/world/GUI/context invalidation stops drawing immediately. |
| `ShowAnimation`, `ShowMs` | `INHERIT` uses `animation` / `animationMs`; otherwise the existing None/Slide/Zoom/Bounce/Pop choices and 50–5000 ms. Hide styles/durations are independent, below. |
| `EntryAnimation`, `ResizeAnimation`, `EntryMs`, `ResizeMs` | `EXISTING` / `NONE` independently enable native arrival/movement and resize motion; durations are `EXISTING` or 50–5000 ms. Global `animation=NONE` and Maximum FPS disable motion. |
| `ExitAnimation`, `ExitMs` | `NONE` (default), `FADE`, `SLIDE`; 50–5000 ms. Retains only already-renderable cached rows, at most 40 total, and clears them on phase/generation/column/settings changes. |
| `PingUnits` | `true` retains ms; `false` hides the unit for the ping column only. Valid usernames ending in ms remain intact. |
| `FriendHighlight`, `FriendColor` | `INHERIT` uses global `friendHighlight` / `friendColor`; explicit boolean / six RGB digits override per panel/context. Active team colors, spectators and confirmed party colors are preserved. |

**Move TAB** opens a draggable panel with grid snapping (`tabSnapGrid`, 1–32 pixels). Drag its interior; Enter applies both coordinates atomically, Esc cancels. When there is no live table, a bounded placeholder shows its last/default dimensions. Position changes do not write repeatedly while dragging. Stored positions are fitted back into the current viewport.

`tabMatchOverview=true` adds a compact summary only when beds destroyed, ordinary kills and final kills are all explicitly present in the visible server TAB header/footer. No counters are guessed. `partyWindowMs` (50–5000, default 1600) adjusts the native experimental arrival window; approximate group markers remain visibly approximate.

`nametagWaiting`, `nametagGame` default true; `nametagElsewhere`, `nametagSpectators` default false. They preserve the existing cached-data, Pika-host and vanilla-visibility rules. `friendsAfterParty` controls friend priority in explicit Lean sorts.

**API status** opens a secret-free page that refreshes once per second. **Preset** cycles Maximum FPS, Balanced and Visual. **Save/Load** store named, allowlisted profiles under `config/velvetg-lean-profiles`; edit `profileName` first. Export/import remains available. Unknown local properties are retained, but unknown fields and credentials are excluded from all exported/named profiles. Invalid batch imports leave the current settings intact.

The shared service coalesces background profile work across different mode/period selections. It keeps the existing 2-worker/64-queue executor, merges one cached profile into each selection, limits each pending group and backs off failed profile fetches for 30 seconds. Core leaderboard requests, production pacing, authentication and QUAD normalization remain shared and unchanged in purpose.

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

For the local HTTP comparison, optionally set `PREVIOUS_PATCH_JAR` to your installed Overhaul2 library, then run `python tests/compare_builds.py --profiles-only`. It records original/previous/current profile request counts in `reports/profile-comparison-overhaul3.json`; it never launches Minecraft or claims FPS results. The older `build-comparison.json` is preserved historical Overhaul2 evidence.

For the warmed Overhaul2/3 CPU/allocation experiment, set PREVIOUS_PATCH_JAR to your installed Overhaul2 JAR and run `python tests/benchmark_preparation.py`. It alternates five JVM forks per variant/workload, with 1.5 s warmup and seven 250 ms samples each. No game or GPU is initialized.

For a clean measurement window without opening a GUI, use `/velvetlean profile start`, follow the scene/route, then `/velvetlean profile stop` to freeze and export. `/velvetlean profile export` exports without stopping. These commands are handled locally; they do not change the saved profiler preference.
