# Completed, partial and blocked work

Overhaul3 continues the native implementation. It is not a claim that every reference feature or platform test is finished.

## Implemented

All Overhaul1/FIX1 screenshot, API authentication, QUAD, bytecode repair, bounded image/text/cache, protected-key, configuration-write, instrumentation and installer work is retained. New code adds context-specific TAB/HUD settings; actual TAB drag/grid/viewport positioning; column toggle/reorder editing; independent sizes, palettes, spacing, image geometry and show/entry/resize/row-exit controls; nametag phase/spectator scopes; cached friend highlighting; explicit match-counter parsing; configurable experimental arrival window; grouped profile enrichment; named non-sensitive profiles and status UI; disabled-context table-build suspension; stronger Maximum FPS presentation bypass; removal of eager cape preview preload calls; and isolated Overhaul3 installation/settings migration.

Existing sessions/deltas/history/notes/watchlists/threat assessment, Dynamic Widget, Violet, SnapTap/input, resource packs and modules are retained. Original unmodified classes/assets remain byte-identical. No denicking or identity recovery is added. No Fabric/OneConfig/Java 21 runtime is introduced.

## Remaining partial features and concrete constraints

- Optional whole-table hide continuation now handles TAB key release and the native HUD hide-while-TAB condition. Other scope/module/world/GUI changes stop rendering immediately to avoid displaying stale/cross-world tables; the implementation does not animate through those safety gates. GPU composition/visual timing remains unverified. Row-exit expiry still releases on the next native rebuild (normally up to 250 ms later).
- Entry/movement and resize use the native animation engine, with independent toggles/durations. New row exits support fade/slide. Arbitrary per-row zoom/bounce/pop geometry is not implemented. Independent show/hide transitions support the requested styles (plus hide FADE), with tested pure reversal state; every visual style still needs native GPU observation.
- Independent fonts/colors/columns/geometry and waiting/game presentation controls are supplied. Some original global controls remain global: full theme preset selection, text-shadow styling and separator-shadow styling. Dedicated animated skeletons, full fixed-name-column alternatives and every rank-formatting variant are not reproduced. NameWidth caps text while preserving native minimum/rank/marker widths.
- A true 3D head mesh and shader/framebuffer glass implementation require mixed LWJGL/NanoVG state and GPU validation. The native flat head/hat pass and panels are retained; no unvalidated shader/native dependency replacement is shipped.
- Remote installed-client badges need a trusted compatible attestation provider. None was present in the installed client/reference integration environment; no guessed badge is shown.
- Current winstreak has no confirmed authoritative response source. Highest winstreak remains accurately labelled HWS/BEST WS. No fabricated current streak or hidden identity is used.
- Explicit optional Catbox PNG retrieval, bounded cache, stale-job invalidation, local/random images and geometry controls are implemented. Remote gallery/manifest browsing is absent. An in-progress Java URLConnection read can finish or hit its bounded timeout after cancellation; stale results are never selected.
- Original SVG/game-skin wrapper caches are not globally evicted: those handles can wrap Minecraft-owned textures. Blind deletion could remove game textures; the new file-image cache has separate ownership/bounds. Full HUD/TAB frame batching and suspension based only on an unheld TAB key remain unimplemented. Disabled contexts skip new table construction.
- Custom capes still use the original decode/registration path. The safe built-in preview/bind preloads are removed; changing selected/custom-cape registration and resource/font startup ordering needs local checkpoint/GPU evidence. This package does not rewrite chunk/entity/packet behavior or claim a whole-engine optimization.
- API-key capture uses the audited English server-shaped response inside an explicit Pika request window. Actual live message shape/key issuance/authentication needs the local session; credentials were correctly excluded from collection. Automatic fetching remains opt-in and masked manual entry remains available.
- Optional font registrations remain bounded to two per run (8 MiB each); more selections require restart because the native font lifetime API has no safe per-font deletion path established here. New file images use at most 24 entries/64 MiB GPU bytes and 1024-pixel previews. GPU lifecycle is implemented but unexecuted here.
- Native HUD maximum and shared API fetch maximum still also apply; set each to 40 to display/fetch 40 rows. Additive context caps do not silently rewrite native settings.

## Platform and performance tests still required

This Linux cloud has no Minecraft window/account/native rendering session. No actual Minecraft launch, Pika login/authenticated request, Windows cmd.exe/PowerShell 5.1 execution, resource-pack reload, gameplay input test, GPU frame/VRAM test, startup-time/FPS/1% lows benchmark or real 40-player render-overhead measurement was performed.

Real Java 8, mock/local HTTP, class verification, CPU geometry/state, disk migration/write and PowerShell 7/Linux installer tests are reported separately in TESTING.md. The local benchmark/profiler tools are included for equivalent-scene measurements. The original and Overhaul1/2 versions remain available if the new version fails.

## Launcher and recovery limits

Empty artifact/repository URLs implement the local-installed-library convention. Offline tests prove coordinates/path/hash/size/inheritance and absence of an invented download source. No official launcher repair/download execution was possible here; use the supplied local acceptance sequence. Launchers requiring a nonempty URL are not certified. The verifier refuses missing/changed artifacts and the installer refuses unknown baselines.

The collector supplied version/library summaries, not complete original JSON including all download fields. The installer reads the actual local source JSON during installation, preserves unrelated fields/inheritance and checks a fixture with legacy/current metadata in tests. Do not mistake the collected summaries for inspection of unavailable Windows files.

A crash before an install journal is written can leave a private staging directory under the candidate state directory; it is not auto-deleted as unknown contents. Backups are intentionally retained. Journaled final files recover by exact hash, while changed/read-only files require local investigation. No unknown output is forcibly discarded.

Startup exports are completed-startGame observations and may be stale after a crash before completion. Frame CSV contains the last 4096 exported loop intervals; it is not automatically a fresh equivalent-scene benchmark or a GPU trace. The smoke kit reports export ages and never includes raw log lines. Its opt-in online check is a standalone Java transport test; it does not certify native automatic chat capture or live statistics rendering.
