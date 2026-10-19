# ChuStatsu v1.2.0 → collected VelvetG Lean feature audit

Reference: v1.2.0, commit `26a3d83c78d14322dd211254bdb33e7dae984d5a`. Its actual source, configuration, rendering hooks, API implementation and test sources were inspected. Its own Gradle suite was not run. Current baseline was reconstructed and SHA-256 verified: `3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813` (36,725,098 bytes).

Classifications describe the **baseline**, followed by the implemented changes or remaining work. “Already implemented correctly” means the audited behavior exists in the binary; it does not certify live rendering. Java 8, logic, mock HTTP and installer checks are reported separately. Minecraft, GPU/native initialization, live Pika authentication, Windows cmd.exe/PowerShell 5.1 and FPS have not been tested here.

This is a substantial staged implementation, **not complete feature parity**. Partial features and missing options below are deliberately not presented as completed. Independent waiting/game context settings, bounded row exits and image geometry controls are now implemented. Full-table hide continuation, shader glass and true 3D heads still need work. Current winstreak and remote installed-client badges need an authoritative data source.

## Licensing and compatibility

ChuStatsu code is All Rights Reserved; no Java, mixins, render framework or private assets were copied. Its Java 21/25, Ornithe/Legacy Fabric, OSL and OneConfig runtime is incompatible with this Java 8 LaunchWrapper/Glide client. Features were independently implemented in native Java 8 helpers and narrow transforms. Bundled Poppins was obtained independently from Google Fonts under SIL OFL 1.1; the font license is included. No collected client, Minecraft, original assets, credentials, raw logs or private configurations are distributed.

## Feature matrix

| Feature | ChuStatsu source | Baseline classification | Overhaul2 implementation / limitation |
|---|---|---|---|
| Independent movable/scalable TAB handle; centering migration | TableHud.java; StatsController.java:514 | Implemented but inferior or missing options | Native TAB drag handle with grid snapping, viewport bounds and atomic Apply/Cancel; additive scale override plus native auto-fit. HUD retains its native drag editor. |
| 1–40 TAB players; independent API-player cap | ChuStatsuConfig.java:229,380; StatsController.java:118 | Implemented but inferior or missing options | Native TAB allowed up to 200, API cap 40; added hard 1–40 TAB limit and additional independent HUD cap. Native HUD max still also applies. |
| Waiting/in-game/elsewhere visibility; header and name/rank composition | ChuStatsuConfig.java:220-258 | Implemented but inferior or missing options | Native visibility gates retained. Added separate Waiting/Game settings for each panel: columns, caps, sorting, typography, palette, opacity, geometry, images and motion. Some original global style switches remain shared. |
| Persistent TAB when key is released | StatsController.java:289-310 | Completely missing | Implemented `tabPersistent`, gated through native renderTab/context rules; default off. |
| Seven statistic sort orders; social priority; active team colors | StatsController.java:118-135,341-346; TeamOrder.java | Implemented but inferior or missing options | Native global seven-stat sort retained; added independent TAB/HUD sorts with party/watchlist controls, cached friends and active team grouping. Equal-stat tie-break is name. |
| Player heads; optional enlarged hat layer | StatsController.java:858; ChuStatsuConfig.java:77 | Implemented but inferior or missing options | Existing cached skin texture + hat pass retained; added hat-layer toggle. A true 3D mesh/enlarged-hat option remains absent. |
| Single-row loading/error states and skeletons | ColumnLayout.java; StatsController.java:707-835 | Implemented but inferior or missing options | Native placeholders retained; separate secret-free API status page added. Dedicated animated skeleton variants remain partial. |
| Alternating rows, dividers, opacity, spacing, header | ChuStatsuConfig.java; ColumnLayout.java | Implemented but inferior or missing options | Existing independent row backgrounds/dividers/opacity/header retained. Added independent cell padding; shadow options are not fully independent. |
| Independent HUD position/scale and 1–40 limit | TableHud.java; ChuStatsuConfig.java:112-217 | Already implemented correctly | Existing native draggable HUD, scale and 1–40 max retained, with additional Lean limit. |
| HUD waiting/game/elsewhere visibility and hide-on-TAB | StatsController.java:137-157 | Already implemented correctly | Existing HUD waiting/game visibility and hide-while-TAB preserved; same underlying API/cache. |
| HUD compact/detailed presets, grid snap and independent typeface | ChuStatsuConfig.java:103-108; TableHud.java | Implemented but inferior or missing options | Native compact/detailed/HUD editor/grid retained; independent fonts/font sizes, colors and named additive profiles plus Maximum FPS/Balanced/Visual presets added. |
| Head, dynamic/fixed name, level, FKDR, WLR, highest streak, final kills, wins, beds, guild, rank, ping, TAB HP | ColumnLayout.java; stats/StatsView.java | Implemented but inferior or missing options | Native columns plus GUILD and native toggle/reorder editor. Added NameWidth text cap, independent rank combination and fonts. Native minimum/rank/marker widths remain; a fully fixed name-column alternative is not added. HWS stays BEST WS. |
| Unknown/zero ping, units and tier coloring | PingText.java | Implemented but inferior or missing options | Uses actual cached latency. Independent PingUnits per panel/context hides ms only in the ping column. Native unknown/tier rules remain. |
| Match overview from visible TAB header/footer | MatchOverview.java; StatsController.java | Implemented but inferior or missing options | Added optional active-game summary only when visible server TAB text supplies all three explicit counters: beds destroyed, ordinary kills and finals. Native headers/session summaries remain. |
| Installed-client badge | ClientBadge.java | Incompatible and requiring redesign | Blocked: no trusted Glide equivalent of the reference optional PolyPlus client attestation API. No inferred or spoofable badge introduced. |
| Spectator styling, known rank formatting and preserved team colors | RankLabel.java; TeamOrder.java; StatsController.java:528-626 | Already implemented correctly | Existing spectator/rank/team rendering preserved. Explicit new stat sorts keep active team grouping; live visibility/regression checks still required. |
| Nametag seven-stat selector and above/beside placement | mixin/PlayerRendererMixin.java; StatsController.java:907-932 | Completely missing | Implemented cached seven-stat selection, above/beside placement and independent waiting/game/elsewhere/spectator scopes within vanilla visibility. No render requests; HWS is highest streak. |
| Automatic /apikey request, cooldown and bounded retries | ApiKeyCapture.java; StatsController.java:60-71 | Completely missing | Implemented opt-in, missing/rejected-key gating, Pika host check, join delay, 60 s cooldown, three attempts/connection and request window. |
| Key capture, cancellation of sensitive message, manual key input | ApiKeyCapture.java; StatsController.java:939; ChuStatsuConfig.java:21-27 | Implemented but inferior or missing options | Existing local key file/header retained. Added exact whole-server-shaped header/key capture in an opened window, early packet cancellation/redaction and masked manual input. Actual live message format must be verified. |
| Key rotation after 401/403; status without secret | stats/PikaStatsClient.java:71-90,294-295 | Implemented but inferior or missing options | Key rotation state is credential-aware; old failures cannot reject a replacement key. 401 or explicit auth-error 403 reauthenticates; generic private-profile 403 does not. Owner-only atomic storage. |
| ALL_MODES / SOLO / DOUBLES / QUAD and four periods | stats/PikaStatsClient.java:33-34 | Already implemented correctly | Confirmed original X-Api-Key behavior retained; centralized normalization and URL tests preserve ALL_MODES/SOLO/DOUBLES/QUAD and total/weekly/monthly/yearly. No QUAD→QUADS. |
| Async shared service and published pending state | stats/PikaStatsClient.java:36-130 | Already implemented correctly | Existing async priority service, pending futures and deduplication retained; no second TAB/HUD downloader. |
| Pacing, global 429 cooldown and Retry-After | stats/PikaStatsClient.java:271-332 | Already implemented correctly | Existing adaptive pacing, global 429 cooldown and delta/date Retry-After retained. Cooldown honors up to one day; do not copy reference’s shorter cap. |
| Separate profile cache, cached friends and core stats | stats/PikaStatsClient.java:152-189 | Implemented but inferior or missing options | Retained leaderboard-first/profile-later. Added per-player/session pending groups, exact selection callback deduplication, shared profile merging and 30-second failed-profile cooldown on the existing bounded executor. |
| Stale-result preservation and cache lifetime | stats/PikaStatsClient.java:78-118 | Already implemented correctly | Existing bounded 500-entry core cache, freshness/backoff, stale results and disk writer retained. Tests exercise expiry and refresh failure. |
| HTTP errors, disabled/private data, null/partial JSON, user-specific leaderboard entries | stats/PikaStatsClient.java:180-266; stats/StatsView.java | Implemented but inferior or missing options | Existing statuses/parser retained; generic 400/404 never used to reveal identities. Added auth status/redaction. Privacy vs auth error classification remains endpoint-dependent. |
| Connect/read timeouts, size cap, disconnect and header safety | stats/PikaStatsClient.java:271-314 | Already implemented correctly | Production allowlist, 4.5 s connect / 8 s read and body deadline, 1 MiB cap, no redirect and finally-disconnect verified through offline/mock tests. No main-thread HTTP. |
| Bounded priorities, departed-player cancellation and disk caching | stats/PikaStatsClient.java; StatsController.java | Implemented but inferior or missing options | Existing priority/cancellation/disk writer retained. Hard 160-job admission now also applies to manual commands; profile queue 64 and profile cache 500. Generation/epoch protections preserved. |
| Confirmed party membership and cached-profile friends | SocialTracker.java; StatsController.java | Implemented but inferior or missing options | Native confirmed-party parser retained. Added cached friend ordering/highlighting with independent panel/context colors/toggles, preserving active team, spectator and confirmed party colors. |
| Party-first and friend-after-party ordering | ChuStatsuConfig.java:343-352; StatsController.java | Implemented but inferior or missing options | Explicit independent sorts respect native party/watchlist priority and configurable friendsAfterParty. Default native ordering is unchanged. |
| Experimental simultaneous-arrival groups and configurable window | OtherPartyTracker.java | Implemented but inferior or missing options | Native approximate grouping retained; added bounded partyWindowMs. Approximate group markers remain visibly distinct from confirmed party membership. |
| Minecraft / Poppins / custom TTF and colored text | FontText.java; fonts/OFL.txt | Implemented but inferior or missing options | Independent panel/context font choices, font sizes and text/header/secondary palette overrides; lazy Poppins/local fonts retain two-registration/8 MiB bounds. |
| Glass panels, shadows, fallback if shaders unavailable | PanelStyle.java | Incompatible and requiring redesign | Existing native panels/shadows retained; Maximum FPS uses one fill. Shader framebuffer/glass port is blocked on mixed LWJGL/NanoVG GPU state validation; no untested native shader changes shipped. |
| Local PNG selection, independent/random images, alignment/opacity/scale | BackgroundImages.java | Completely missing | Independent local/random PNG images, opacity, contain/cover/stretch, edge/corner alignment and zoom, using bounded worker decode and render-owner upload. External retrieval remains off by default. |
| Optional Catbox fetch/cache and offline fallback | CatboxGallery.java; BackgroundImages.java | Completely missing | Explicit optional six-character Catbox PNG retrieval with bounded disk/GPU caches; obsolete jobs cancelled/ignored by generation. Blocking reads remain bounded by timeouts. Remote manifest/gallery browsing is not added. |
| None/Slide/Zoom/Bounce/Pop show/hide and separate durations | TableMotion.java | Implemented but inferior or missing options | Independent per-panel/context show styles and durations; independent native entry/resize toggles/durations; optional fade/slide row exits. Full-table hide continuation remains absent. |
| Arrival/departure movement and smooth resize | TableLayoutMotion.java | Implemented but inferior or missing options | Native entry/movement/resize retained with independent controls. Optional retained-row fade/slide exits hold cached row data only, clear on phase/generation/columns/settings changes, and enforce 40 total rows. GPU visuals remain unverified. |
| Low-performance mode | ChuStatsuConfig.java:94; PanelStyle.java; StatsController.java | Implemented but inferior or missing options | Maximum FPS additionally bypasses optional rows/dividers/header embellishments, motion and backgrounds. Context settings/columns are memoized, team color constants reused, and disabled-context table construction skipped. Combined NanoVG batching remains absent; no FPS gain is claimed. |
| Settings UI, toggle binding, HUD editor and saved-name-column migration | ChuStatsu.java; ChuStatsuConfig.java:29,401-420 | Implemented but inferior or missing options | Native controls/keybinds retained. Added searchable responsive Lean UI, native column editor, typed value hints, real TAB drag, API status page, named allowlisted profiles/presets, transactional imports, separate v2 settings migration and unknown-local-property preservation. |
| /stats player and settings commands | StatsController.java:954-975 | Already implemented correctly | Existing /stats, /velvetg and /velvetgdebug preserved; added /velvetlean. No API-key chat command is introduced. |
| Debug dump of raw TAB and experimental identity logs | ChuStatsuConfig.java:385; StatsController.java:1000-1133 | Not worth integrating / excluded | Raw personal TAB/identity dumps intentionally not ported. Added bounded, secret-free profiler/cache/queue/init reports instead. |
| Denick tracking and resolved-name presentation | DenickTracker.java; ChuStatsuConfig.java:340,361-365 | Not worth integrating / excluded | Explicitly excluded. No denick tracker, identity recovery, alias discovery or hidden-name revelation added. |
| Session deltas, encounter/player history, notes, threat/watchlists, Dynamic Widget, Violet, SnapTap | User requirements; no corresponding replacement system established in inspected reference | Already implemented correctly | Existing advanced modules/classes/assets/configuration retained. Unchanged entries proven byte-identical; profile-save implementations changed only to coalesce disk writes. No gameplay/input/module removal. |
| Lazy screenshot gallery with bounded GPU lifecycle | User requirements; not a ChuStatsu gallery feature | Implemented but inferior or missing options | All Overhaul1 bounded lazy gallery/resource ownership work retained. Also removed eager built-in cape preview/bind preloads; original custom-cape registration remains pending local profiling. |
| Frame/initialization/GC/cache/worker/request instrumentation and reproducible FPS benchmark | User requirements; no equivalent complete profiler established in reference | Completely missing | Implemented bounded interval/CPU/TAB/HUD percentiles, GC/cache/thread/request counters and startup checkpoints; local process sampler supplied. GPU timing, real FPS/1% lows and complete scene profiling require local Minecraft execution. |

## All 104 declared reference controls

The mapping below covers every public reference field. “Retained” is code-audit evidence, not a live UI certificate. The matrix above gives shared subsystem limitations. New settings are additive; old config keys and schema are not reset.

| Reference control | Native disposition |
|---|---|
| `enabled` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `automaticApiKey` | New `autoKey`; opt-in/cooldown state machine. |
| `apiKey` | Masked native entry + existing protected key file. |
| `toggleMod` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `tabEnabled` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudEnabled` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `nametagEnabled` | New `nametag` toggle. |
| `nametagAlwaysShow` | New nametagElsewhere allows other Pika phases, still inside normal visibility; no forced invisible-player labels. |
| `nametagShowWaiting` | New nametagWaiting. |
| `nametagShowInGame` | New nametagGame. |
| `nametagStat` | New seven-stat selector; HWS is highest. |
| `nametagDisplayMode` | New ABOVE/BESIDE. |
| `onlyOnPika` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `apiOnlyInMatchOrWaiting` | Native visibility/request scopes retained. |
| `hpOnlyInGame` | Native active-game HP scope retained. |
| `textShadows` | Partial: existing text renderer preserved; no separate new global toggle. |
| `threeDimensionalHeads` | Partial: retained flat skin head + optional hat pass, no new 3D mesh. |
| `separatorShadows` | Partial: native panel/divider appearance retained. |
| `sortMatchTeams` | Native team order retained; new explicit stat sorts group teams in active games. |
| `spectatorTeamColor` | Native spectator/team styling retained. |
| `lowPerformanceMode` | New reversible `maximumFPS`. |
| `columnSpacing` | Independent tabSpacing/hudSpacing (0 keeps native density). |
| `removePingMs` | Independent panel/context PingUnits. |
| `fontMode` | Independent `tabFont`/`hudFont`. |
| `customFont` | Lazy local fonts; two optional registrations/run, 8 MiB each. |
| `hudAlwaysShow` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudShowWaiting` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudShowInGame` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hideHudWhileTab` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudMaxPlayers` | Native HUD cap plus Lean hudLimit, each 1–40. |
| `hudShowHeader` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudCombineRankWithName` | hudRankWithName and Waiting/Game overrides. |
| `hudLoadingSkeleton` | Partial: native loading/status placeholders retained; no full skeleton port. |
| `hudAlternatingRows` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudColumnDividers` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `hudGlass` | Not implemented: GPU/native state compatibility validation required. |
| `hudBackgroundOpacity` | Native independent panel opacity retained. |
| `hudShowAnimation` | Independent hudShowAnimation and Waiting/Game overrides; five show styles. |
| `hudShowDuration` | Independent hudShowMs, 50–5000 ms, with context overrides. |
| `hudHideAnimation` | Still absent: native visibility stops drawing; full-table hide continuation not implemented. |
| `hudHideDuration` | Still absent with full-table hide continuation. |
| `hudResizeDuration` | Independent hudResizeMs and ResizeAnimation (native/none), including context overrides. |
| `hudEntryAnimation` | Native arrival/movement retained; independent hudEntryAnimation EXISTING/NONE. |
| `hudEntryDuration` | Independent hudEntryMs and context overrides. |
| `hudExitAnimation` | Optional hudExitAnimation NONE/FADE/SLIDE; retained-row lifecycle bounded to 40. |
| `hudExitDuration` | Independent hudExitMs, 50–5000 ms, with context overrides. |
| `hudImageEnabled` | New independent hudBackground local/random/explicit Catbox setting; remote browsing absent. |
| `hudImageFile` | New independent hudBackground local/random/explicit Catbox setting; remote browsing absent. |
| `hudRandomBackground` | New independent hudBackground local/random/explicit Catbox setting; remote browsing absent. |
| `hudCatboxImage` | New independent hudBackground local/random/explicit Catbox setting; remote browsing absent. |
| `hudImageOpacity` | Independent hudImageOpacity 0–100 and context overrides. |
| `hudImageSize` | Independent hudImageScale STRETCH/COVER/CONTAIN and ImageZoom 25–400; context overrides. |
| `hudImagePosition` | Independent hudImageAlign CENTER/edges/corners; context overrides. |
| `sortHud` | New independent hudSort (EXISTING preserves native). |
| `hudSortStat` | New hudSort seven-stat selector. |
| `tabAlwaysShow` | Native visibility scope retained; new tabPersistent implements key-release persistence. |
| `tabShowWaiting` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `tabShowInGame` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `tabMaxPlayers` | New tabLimit 1–40. |
| `sortTab` | Native toggle retained; explicit tabSort is an override. |
| `tabSortStat` | New independent tabSort seven-stat selector. |
| `tabShowHeader` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `tabCombineRankWithName` | tabRankWithName and Waiting/Game overrides. |
| `tabLoadingSkeleton` | Partial: native loading/status placeholders retained; no full skeleton port. |
| `tabMatchOverview` | New optional tabMatchOverview; only explicit visible server counters, all three required. |
| `tabClientIndicator` | Blocked: no reliable native remote-client attestation source. |
| `tabAlternatingRows` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `tabColumnDividers` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `tabGlass` | Not implemented: GPU/native state compatibility validation required. |
| `tabBackgroundOpacity` | Native independent panel opacity retained. |
| `tabShowAnimation` | Independent tabShowAnimation and Waiting/Game overrides; five show styles. |
| `tabShowDuration` | Independent tabShowMs, 50–5000 ms, with context overrides. |
| `tabHideAnimation` | Still absent: native visibility stops drawing; full-table hide continuation not implemented. |
| `tabHideDuration` | Still absent with full-table hide continuation. |
| `tabResizeDuration` | Independent tabResizeMs and ResizeAnimation (native/none), including context overrides. |
| `tabEntryAnimation` | Native arrival/movement retained; independent tabEntryAnimation EXISTING/NONE. |
| `tabEntryDuration` | Independent tabEntryMs and context overrides. |
| `tabExitAnimation` | Optional tabExitAnimation NONE/FADE/SLIDE; retained-row lifecycle bounded to 40. |
| `tabExitDuration` | Independent tabExitMs, 50–5000 ms, with context overrides. |
| `tabImageEnabled` | New independent tabBackground local/random/explicit Catbox setting; remote browsing absent. |
| `tabImageFile` | New independent tabBackground local/random/explicit Catbox setting; remote browsing absent. |
| `tabRandomBackground` | New independent tabBackground local/random/explicit Catbox setting; remote browsing absent. |
| `tabCatboxImage` | New independent tabBackground local/random/explicit Catbox setting; remote browsing absent. |
| `tabImageOpacity` | Independent tabImageOpacity 0–100 and context overrides. |
| `tabImageSize` | Independent tabImageScale STRETCH/COVER/CONTAIN and ImageZoom 25–400; context overrides. |
| `tabImagePosition` | Independent tabImageAlign CENTER/edges/corners; context overrides. |
| `staticImageSize` | Independent contain/cover/stretch and zoom; no arbitrary pixel-size mode. |
| `showDenickedName` | Excluded. |
| `highlightParty` | Retained native ScoutConfig/module control; code-audited, live UI verification pending. |
| `partyFirst` | Native toggle respected by new explicit sorts. |
| `highlightFriends` | Global friendHighlight/friendColor plus panel/context FriendHighlight/FriendColor. |
| `friendsAfterParty` | Configurable friendsAfterParty for explicit Lean sorts. |
| `detectOtherParties` | Native approximate party inference retained. |
| `otherPartyWindowMs` | New partyWindowMs (50–5000, default 1600); approximate inference retained. |
| `denickEnabled` | Excluded. |
| `denickWaitingOnly` | Excluded. |
| `bedWarsOnly` | Native PikaContext/phase gates retained. |
| `mode` | Native modes retained; QUAD regression-tested. |
| `period` | Native periods retained and URL-tested. |
| `cacheSeconds` | Native TTL retained and tested. |
| `maxApiPlayers` | Native 1–40 shared fetch cap retained. |
| `debugMode` | Existing /velvetgdebug retained; new optional bounded profiler. |
| `tabColumns` | Native controls plus interactive toggle/reorder editor and independent Waiting/Game Columns. |
| `hudColumns` | Native controls plus interactive toggle/reorder editor and independent Waiting/Game Columns. |
