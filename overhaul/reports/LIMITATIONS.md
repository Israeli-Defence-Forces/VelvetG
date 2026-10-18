# Completed, partial and blocked work

This package is an incrementally tested native overhaul stage, not a claim that every requested ChuStatsu feature is finished.

## Implemented in code and covered by offline checks

Exact-hash reproducible patch workflow; source and Java 8 tools; baseline-entry preservation; constructor screenshot I/O/deletion removal; asynchronous gallery discovery and on-demand decode; bounded render-owner GPU cache and STB cleanup; existing JSON VerifyError repair preservation; Discord close repair; hard request/profile bounds; cancellation/epoch protections; safe production HTTP/authentication endpoint handling; protected key-file compatibility; opt-in bounded key capture and masked entry; cached guild/friends; shared-cache TAB/HUD extensions; column order/hiding; independent sorts/fonts/spacing/limits; licensed lazy Poppins/local fonts; cache-only nametags above/beside; optional show transitions and local/random/explicit Catbox backgrounds; reversible Maximum FPS presentation mode; text-measurement caches; debounced configuration mirrors; bounded instrumentation; source/tests/install/verify/rollback scripts.

Graphics threading/lifecycle and hooks are implemented, structurally checked and Java-verified, but not executed against a real OpenGL context here. Tests using mock graphics or IPC are labelled as such.

## Existing functionality retained

Native visibility scopes, HUD editor/grid snapping, appearance controls, heads, spectator/team/rank logic, seven-stat sorting, layouts/presets, sessions/deltas/history, notes/watchlists/threats, approximate party grouping, Dynamic Widget, Violet and input/module behavior. Original assets and all unmodified classes remain byte-identical. Only the two existing property-save implementations change how writes are scheduled, preserving their property data.

## Partial or not implemented

- True draggable TAB handle (click positioning is supplied); completely separate waiting/active appearance profiles; separate rank-combination, friend-highlight, skeleton and nametag phase options.
- Native entry/movement/resize motion is retained, but departed rows are dropped by the existing renderer. Full row exit/hide continuation requires a retained-row renderer and lifecycle tests; independent TAB/HUD show/hide/entry/exit/resize durations are not implemented.
- Image opacity/alignment/scaling controls are fixed to 18%/panel stretch. Explicit optional Catbox PNG retrieval is implemented, but browsing its remote gallery/manifest and complete cancellation parity are not.
- The original resource/SVG/game-skin wrapper caches are not globally bounded by this patch. In particular, safely evicting wrapped Minecraft texture handles requires confirming ownership flags against a live GPU; deleting those handles blindly can delete game-owned textures. Reported image counts/bytes describe the new file-image pool, not every OpenGL allocation.
- Shared text/layout caching is improved; complete hidden-component suspension and combined HUD/TAB NanoVG frame batching are not implemented. Fonts still use the native upload/initialization path; custom capes/resource packs need local timings before further changes.
- Exact ChuStatsu match-counter overview parser, fixed/dynamic name column alternatives and independent ping-unit controls remain missing. Existing native header/session summaries and latency behavior are preserved.
- Badge attestation is blocked by the absence of a trusted compatible remote-client provider. No fabricated badge is shown.
- A current winstreak source is not established: the confirmed leaderboard statistic is **highest** winstreak. It remains HWS/BEST WS; current streak is not fabricated.
- Shader glass/framebuffer port and true 3D head mesh require GPU/mixed LWJGL state validation. Existing native panels/skin hat layer are retained instead of introducing unverified native replacements.
- Profile hydration is bounded but does not yet have a separate per-profile pending-dedup map. Existing core-stat deduplication is preserved.
- API auto-capture expects the reference's English server-shaped header/key response within an explicit Pika request window. Actual server message shape, key issuance and live authenticated requests could not be tested without the game session/key, which was correctly excluded from collection. Automatic capture defaults off and manual entry remains available.
- Optional font registrations are bounded to two/run, at most 8 MiB each; restart to select further fonts. File previews are downsampled to 1024 pixels per dimension and cached GPU memory is bounded to 64 MiB/24 entries. Native GPU allocation/deallocation requires local validation; no claim of a measured GPU memory reduction is made.
- Secret-free exports include only additive Lean settings, not the full original client configuration. Original tools remain available for existing settings. Settings edits are saved on Apply; two original mirror writes are coalesced after 500 ms idle. Abrupt process termination can lose that short pending interval.

## Unverified platform/performance checks

No Minecraft window, native context startup, actual multiplayer server, live Pika authentication, resource-pack reload, hundreds-of-real-images gallery interaction, mouse/keybind gameplay regression, Windows cmd.exe execution, Windows PowerShell 5.1, launcher selection/download behavior, FPS, 1% lows or GPU timings were executed in this Linux cloud environment.

PowerShell 7/Linux runs the actual installer and Java 8 patcher against temporary launcher layouts with spaces/special characters, checks preservation, verifies metadata, detects tampering and exercises rollback. That is useful executable coverage, not a Windows/Minecraft launch claim. The batch wrappers are short and quoted, but Windows execution still needs validation. The original profile and library remain available if the new version fails.

Use the supplied local measurement instructions and export only the non-sensitive profiler report after the first local launch. No API key, raw launcher account file or screenshot upload is needed for that validation.

Automatic denicking/identity recovery is explicitly excluded and not implemented.
