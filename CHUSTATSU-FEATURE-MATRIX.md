# ChuStatsu v1.2.0 integration audit — baseline collection pending

Reference inspected: tag v1.2.0, commit `26a3d83c78d14322dd211254bdb33e7dae984d5a`. The repository tag, build properties, code, configuration, rendering hooks, and the source of its tests were inspected. The reference's Gradle/JUnit suite was **not executed**; test source inspection is not a passing test claim.

The current VelvetG Lean baseline `2.0.9.6-lean-fixed-20261016` is unavailable in this Linux workspace. `D:\Roaming\.minecraft` is not mounted. Older Integration1/2 JARs exist, but are not substituted for the current baseline. Therefore none of the current client's features can honestly be classified as already correct, inferior or missing yet. **B1: exact installed JAR and sanitized launcher metadata required** applies to every pending comparison below. This is a reference audit and implementation queue, not a completed parity claim.

After collection, each B1 row will be assigned one of the requested five classifications: already correct, inferior/options missing, completely missing, not worth integrating, incompatible/requires redesign. Loader/UI adapters already require redesign; identity recovery is already excluded. Each row has a concrete next action and the shared technical blocker rather than a fabricated current-client classification.

## Compatibility and licences

- `fabric.mod.json` declares `All-Rights-Reserved`; no project code-copy licence was established. Do not copy ChuStatsu Java/assets into the client. Independently implement observable behavior and cite reference provenance. No reference source or JAR is included in this delivery.
- Gradle compiles with `release = 21`, mixins declare `JAVA_21`, while the mod descriptor says Java >=25 and README asks for Java 25. These are reference-side inconsistencies/requirements, not permission to change VelvetG's Java 8 target.
- Ornithe/Fabric Gen2 mappings, OSL, OneConfig HUD/config/event APIs, optional PolyPlus badges and Kotlin integration require native Glide equivalents. Installing the original mod or copying its mixins is not an integration strategy.
- The bundled Poppins fonts have their own `fonts/OFL.txt` (SIL OFL 1.1, Poppins Project Authors). If used later, preserve that licence and the font's terms independently of the ARR Java code. No fonts are redistributed by this collector.
- Existing Lean asset/dependency licences remain unverified until its exact JAR and library inventory arrive. Default distribution should be an exact-hash patch against the user's installed copy, rather than republishing unknown assets.

## Feature matrix

For rows marked pending, installed-client state is **B1 / not yet verified**. Source paths below are under `src/main/java/dev/chustatsu/` unless specified otherwise. Priorities follow the user's Stage B/C/D/E order; API reliability precedes cosmetics.

| Feature / behavior | Actual reference source | Native integration decision / blocker | Stage |
|---|---|---|---|
| Independent movable/scalable TAB handle; centering migration | TableHud.java; StatsController.java:514 | B1 pending: Reimplement with existing Glide HUD/editor primitives; preserve manual position. | E |
| 1–40 TAB players; independent API-player cap | ChuStatsuConfig.java:229,380; StatsController.java:118 | B1 pending: Keep one shared service; test 40 displayed, 40 requested and lower limits separately. | D/E |
| Waiting/in-game/elsewhere visibility; header and name/rank composition | ChuStatsuConfig.java:220-258 | B1 pending: Preserve current scopes and native key behavior; independently configurable TAB controls. | E |
| Persistent TAB when key is released | StatsController.java:289-310 | B1 pending: User extension: reference still checks playerListKey; tabAlwaysShow alone does not implement persistence. | E |
| Seven statistic sort orders; social priority; active team colors | StatsController.java:118-135,341-346; TeamOrder.java | B1 pending: Stable cached row order; confirmed party, then friends, then statistic or team. | E |
| Player heads; optional enlarged hat layer | StatsController.java:858; ChuStatsuConfig.java:77 | B1 pending: Reuse existing skin textures; no per-frame or per-table head uploads. | E |
| Single-row loading/error states and skeletons | ColumnLayout.java; StatsController.java:707-835 | B1 pending: Retain stale successful data; distinguish missing key, rejected key, private/no data and cooldown. | D/E |
| Alternating rows, dividers, opacity, spacing, header | ChuStatsuConfig.java; ColumnLayout.java | B1 pending: Native settings; independent TAB/HUD values where requested. Reference spacing is shared, so independent spacing is an extension. | E |
| Independent HUD position/scale and 1–40 limit | TableHud.java; ChuStatsuConfig.java:112-217 | B1 pending: Share data/cache, not appearance or sorting state. | E |
| HUD waiting/game/elsewhere visibility and hide-on-TAB | StatsController.java:137-157 | B1 pending: Retain native dragging; independent visibility scopes. | E |
| HUD compact/detailed presets, grid snap and independent typeface | ChuStatsuConfig.java:103-108; TableHud.java | B1 pending: Requested extensions: reference uses one shared FontOptions selection; implement native presets without overwriting old profiles. | E |
| Head, dynamic/fixed name, level, FKDR, WLR, highest streak, final kills, wins, beds, guild, rank, ping, TAB HP | ColumnLayout.java; stats/StatsView.java | B1 pending: Reorder/hide columns via persisted IDs; label highest versus current streak accurately. Reference HUD has no HP option. | D/E |
| Unknown/zero ping, units and tier coloring | PingText.java | B1 pending: Use actual player-list latency; no fabricated ping or HUD-frame estimates. | E |
| Match overview from visible TAB header/footer | MatchOverview.java; StatsController.java | B1 pending: Only complete observed counters; preserve existing session/delta/history systems. | E |
| Installed-client badge | ClientBadge.java | B1 pending: Reference reflects optional PolyPlus methods. Native Glide requires a compatible trusted source; otherwise omit badge rather than infer installed clients. | E |
| Spectator styling, known rank formatting and preserved team colors | RankLabel.java; TeamOrder.java; StatsController.java:528-626 | B1 pending: Use native visible team/rank data; test unknown ranks and spectator transitions. | E |
| Nametag seven-stat selector and above/beside placement | mixin/PlayerRendererMixin.java; StatsController.java:907-932 | B1 pending: Reuse client nametag hooks; cache-only lookups, native visibility/sneaking/team rules. No renderer HTTP calls. | E |
| Automatic /apikey request, cooldown and bounded retries | ApiKeyCapture.java; StatsController.java:60-71 | B1 pending: Independent implementation gated to verified Pika connection, need/rejection and current request window; no unsolicited repeated commands. | D |
| Key capture, cancellation of sensitive message, manual key input | ApiKeyCapture.java; StatsController.java:939; ChuStatsuConfig.java:21-27 | B1 pending: Do not rely on a matching text prefix alone. Verify transport/message context and sanitize before chat logging; preserve existing key-file format. | D |
| Key rotation after 401/403; status without secret | stats/PikaStatsClient.java:71-90,294-295 | B1 pending: One auth state machine per connection, generation-aware retries, no cross-host redirect and no key in status/export. | D |
| ALL_MODES / SOLO / DOUBLES / QUAD and four periods | stats/PikaStatsClient.java:33-34 | B1 pending: Central normalization; preserve QUAD exactly; lifetime maps to API total. Real authenticated monthly/QUAD success is user-reported, not independently tested here. | D |
| Async shared service and published pending state | stats/PikaStatsClient.java:36-130 | B1 pending: Retain stronger current request service after inspection; atomic deduplication and hard bounds, not a wholesale replacement. | D |
| Pacing, global 429 cooldown and Retry-After | stats/PikaStatsClient.java:271-332 | B1 pending: Apply before every request; test delta/date formats, concurrent updates, cooldown resume and bounded jittered retries. | D |
| Separate profile cache, cached friends and core stats | stats/PikaStatsClient.java:152-189 | B1 pending: Reference fetches profile first; preserve VelvetG leaderboard-first plus independent profile enrichment. | D |
| Stale-result preservation and cache lifetime | stats/PikaStatsClient.java:78-118 | B1 pending: Keep successful stale core results through refresh failures, with explicit freshness state. Add hard LRU/TTL and bounded persistent cache where missing. | D |
| HTTP errors, disabled/private data, null/partial JSON, user-specific leaderboard entries | stats/PikaStatsClient.java:180-266; stats/StatsView.java | B1 pending: 400/404 alone must not be treated as proof of a hidden identity. Distinguish transport/schema/mode failures and unavailable stats. | D |
| Connect/read timeouts, size cap, disconnect and header safety | stats/PikaStatsClient.java:271-314 | B1 pending: Reference uses 7s/12s, 1 MiB and no redirects. Retain or improve exact limits after comparing current Lean. | D |
| Bounded priorities, departed-player cancellation and disk caching | stats/PikaStatsClient.java; StatsController.java | B1 pending: User requirements go beyond reference: fixed worker pool uses default unbounded queue; queued<40 check is not atomic admission; pruning does not guarantee a hard cache bound. Implement strict bounds/generations in existing service. | D |
| Confirmed party membership and cached-profile friends | SocialTracker.java; StatsController.java | B1 pending: Independent parser of verified server events; no sensitive social data in diagnostics or exports. | E |
| Party-first and friend-after-party ordering | ChuStatsuConfig.java:343-352; StatsController.java | B1 pending: Stable tie-breaker; ordinary stat refresh must not churn row order unnecessarily. | E |
| Experimental simultaneous-arrival groups and configurable window | OtherPartyTracker.java | B1 pending: Optional speculative hints visually distinct from confirmed party; clear on lobby exit; never identity inference. | E |
| Minecraft / Poppins / custom TTF and colored text | FontText.java; fonts/OFL.txt | B1 pending: Existing Glide fonts first; Poppins separately licensed under OFL 1.1. Bounded caches, background font preparation and render-thread uploads; independent TAB/HUD settings are an extension. | C/E |
| Glass panels, shadows, fallback if shaders unavailable | PanelStyle.java | B1 pending: Optional and independently disableable. Measure CPU/GPU/driver cost before choosing defaults; never transplant LWJGL contexts. | C/E |
| Local PNG selection, independent/random images, alignment/opacity/scale | BackgroundImages.java | B1 pending: Bound size/pixels/count; decode on worker, upload/dispose on render owner; no startup directory-wide decode. | C/E |
| Optional Catbox fetch/cache and offline fallback | CatboxGallery.java; BackgroundImages.java | B1 pending: Disabled by default; endpoint allowlist, strict limits/timeouts, credential-free connection, bounded caches and cancellation. | E |
| None/Slide/Zoom/Bounce/Pop show/hide and separate durations | TableMotion.java | B1 pending: Independent time-based animations, completely bypassed in Maximum FPS mode. | E |
| Arrival/departure movement and smooth resize | TableLayoutMotion.java | B1 pending: Native stable snapshots and cached dimensions; reference allocates collections during update, so do not copy render-loop design. | C/E |
| Low-performance mode | ChuStatsuConfig.java:94; PanelStyle.java; StatsController.java | B1 pending: User Maximum FPS mode includes more than reference glass/animation switches: cache text/layout, suspend hidden work and measure 40-player overhead. | C/E |
| Settings UI, toggle binding, HUD editor and saved-name-column migration | ChuStatsu.java; ChuStatsuConfig.java:29,401-420 | B1 pending: Use existing Glide settings/event bus; preserve profile keys and input ownership. OneConfig runtime/UI itself is incompatible and requires redesign. | E |
| /stats player and settings commands | StatsController.java:954-975 | B1 pending: Native command intercept with username validation, async lookup and rate limits; preserve /velvetgdebug. | D/E |
| Debug dump of raw TAB and experimental identity logs | ChuStatsuConfig.java:385; StatsController.java:1000-1133 | B1 pending: Do not reproduce raw personal logs. Add only bounded redacted diagnostics; denick-related dumps excluded. | C/E |
| Denick tracking and resolved-name presentation | DenickTracker.java; ChuStatsuConfig.java:340,361-365 | Excluded: Not worth integrating / excluded explicitly by user; no automatic nick revelation or identity recovery. | Excluded |
| Session deltas, encounter/player history, notes, threat/watchlists, Dynamic Widget, Violet, SnapTap | User requirements; no corresponding replacement system established in inspected reference | B1 pending: Preserve installed implementations and settings; reference parity does not authorize replacing these systems. | B–E |
| Lazy screenshot gallery with bounded GPU lifecycle | User requirements; not a ChuStatsu gallery feature | B1 pending: Inspect current ScreenshotManager and its callers before patching; no bulk startup decode, no arbitrary-worker GL, no screenshot deletion. | B |
| Frame/initialization/GC/cache/worker/request instrumentation and reproducible FPS benchmark | User requirements; no equivalent complete profiler established in reference | B1 pending: Integrate minimal disabled-path overhead only after locating installed frame/startup hooks; no measured performance claims yet. | B/C |

## Complete declared-setting checklist

Every declared public configuration field is listed below to prevent an option being silently omitted from comparison. All non-excluded installed-client comparisons remain blocked by B1. Buttons and non-field behavior are covered in the feature matrix. A matching label alone will not establish correct behavior; persistence, bounds, threading and live UI still need verification.

| Reference control | Source line | Installed Lean state | Required disposition |
|---|---|---|---|
| `enabled` | ChuStatsuConfig.java:19 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `automaticApiKey` | ChuStatsuConfig.java:23 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `apiKey` | ChuStatsuConfig.java:27 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `toggleMod` | ChuStatsuConfig.java:30 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabEnabled` | ChuStatsuConfig.java:38 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudEnabled` | ChuStatsuConfig.java:41 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `nametagEnabled` | ChuStatsuConfig.java:44 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `nametagAlwaysShow` | ChuStatsuConfig.java:47 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `nametagShowWaiting` | ChuStatsuConfig.java:50 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `nametagShowInGame` | ChuStatsuConfig.java:53 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `nametagStat` | ChuStatsuConfig.java:56 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `nametagDisplayMode` | ChuStatsuConfig.java:60 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `onlyOnPika` | ChuStatsuConfig.java:63 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `apiOnlyInMatchOrWaiting` | ChuStatsuConfig.java:67 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hpOnlyInGame` | ChuStatsuConfig.java:70 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `textShadows` | ChuStatsuConfig.java:73 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `threeDimensionalHeads` | ChuStatsuConfig.java:77 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `separatorShadows` | ChuStatsuConfig.java:81 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `sortMatchTeams` | ChuStatsuConfig.java:86 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `spectatorTeamColor` | ChuStatsuConfig.java:91 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `lowPerformanceMode` | ChuStatsuConfig.java:94 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `columnSpacing` | ChuStatsuConfig.java:97 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `removePingMs` | ChuStatsuConfig.java:100 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `fontMode` | ChuStatsuConfig.java:105 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `customFont` | ChuStatsuConfig.java:108 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudAlwaysShow` | ChuStatsuConfig.java:112 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudShowWaiting` | ChuStatsuConfig.java:115 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudShowInGame` | ChuStatsuConfig.java:118 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hideHudWhileTab` | ChuStatsuConfig.java:121 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudMaxPlayers` | ChuStatsuConfig.java:124 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudShowHeader` | ChuStatsuConfig.java:127 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudCombineRankWithName` | ChuStatsuConfig.java:130 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudLoadingSkeleton` | ChuStatsuConfig.java:134 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudAlternatingRows` | ChuStatsuConfig.java:137 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudColumnDividers` | ChuStatsuConfig.java:140 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudGlass` | ChuStatsuConfig.java:143 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudBackgroundOpacity` | ChuStatsuConfig.java:147 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudShowAnimation` | ChuStatsuConfig.java:151 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudShowDuration` | ChuStatsuConfig.java:155 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudHideAnimation` | ChuStatsuConfig.java:159 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudHideDuration` | ChuStatsuConfig.java:163 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudResizeDuration` | ChuStatsuConfig.java:167 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudEntryAnimation` | ChuStatsuConfig.java:171 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudEntryDuration` | ChuStatsuConfig.java:175 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudExitAnimation` | ChuStatsuConfig.java:179 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudExitDuration` | ChuStatsuConfig.java:183 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudImageEnabled` | ChuStatsuConfig.java:186 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudImageFile` | ChuStatsuConfig.java:189 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudRandomBackground` | ChuStatsuConfig.java:192 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudCatboxImage` | ChuStatsuConfig.java:195 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudImageOpacity` | ChuStatsuConfig.java:202 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudImageSize` | ChuStatsuConfig.java:206 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudImagePosition` | ChuStatsuConfig.java:210 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `sortHud` | ChuStatsuConfig.java:213 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudSortStat` | ChuStatsuConfig.java:217 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabAlwaysShow` | ChuStatsuConfig.java:220 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabShowWaiting` | ChuStatsuConfig.java:223 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabShowInGame` | ChuStatsuConfig.java:226 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabMaxPlayers` | ChuStatsuConfig.java:229 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `sortTab` | ChuStatsuConfig.java:232 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabSortStat` | ChuStatsuConfig.java:236 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabShowHeader` | ChuStatsuConfig.java:239 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabCombineRankWithName` | ChuStatsuConfig.java:242 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabLoadingSkeleton` | ChuStatsuConfig.java:246 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabMatchOverview` | ChuStatsuConfig.java:249 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabClientIndicator` | ChuStatsuConfig.java:252 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabAlternatingRows` | ChuStatsuConfig.java:255 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabColumnDividers` | ChuStatsuConfig.java:258 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabGlass` | ChuStatsuConfig.java:261 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabBackgroundOpacity` | ChuStatsuConfig.java:265 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabShowAnimation` | ChuStatsuConfig.java:269 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabShowDuration` | ChuStatsuConfig.java:273 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabHideAnimation` | ChuStatsuConfig.java:277 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabHideDuration` | ChuStatsuConfig.java:281 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabResizeDuration` | ChuStatsuConfig.java:285 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabEntryAnimation` | ChuStatsuConfig.java:289 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabEntryDuration` | ChuStatsuConfig.java:293 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabExitAnimation` | ChuStatsuConfig.java:297 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabExitDuration` | ChuStatsuConfig.java:301 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabImageEnabled` | ChuStatsuConfig.java:304 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabImageFile` | ChuStatsuConfig.java:307 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabRandomBackground` | ChuStatsuConfig.java:310 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabCatboxImage` | ChuStatsuConfig.java:313 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabImageOpacity` | ChuStatsuConfig.java:320 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabImageSize` | ChuStatsuConfig.java:324 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabImagePosition` | ChuStatsuConfig.java:328 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `staticImageSize` | ChuStatsuConfig.java:331 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `showDenickedName` | ChuStatsuConfig.java:340 | Not worth integrating / explicitly excluded | Exclude per user: no denick/identity recovery. |
| `highlightParty` | ChuStatsuConfig.java:343 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `partyFirst` | ChuStatsuConfig.java:346 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `highlightFriends` | ChuStatsuConfig.java:349 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `friendsAfterParty` | ChuStatsuConfig.java:352 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `detectOtherParties` | ChuStatsuConfig.java:355 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `otherPartyWindowMs` | ChuStatsuConfig.java:358 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `denickEnabled` | ChuStatsuConfig.java:361 | Not worth integrating / explicitly excluded | Exclude per user: no denick/identity recovery. |
| `denickWaitingOnly` | ChuStatsuConfig.java:365 | Not worth integrating / explicitly excluded | Exclude per user: no denick/identity recovery. |
| `bedWarsOnly` | ChuStatsuConfig.java:368 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `mode` | ChuStatsuConfig.java:371 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `period` | ChuStatsuConfig.java:374 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `cacheSeconds` | ChuStatsuConfig.java:377 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `maxApiPlayers` | ChuStatsuConfig.java:380 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `debugMode` | ChuStatsuConfig.java:383 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `tabColumns` | ChuStatsuConfig.java:393 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |
| `hudColumns` | ChuStatsuConfig.java:398 | B1 / unknown | Compare exact binary/native settings, retain stronger implementation, fill only verified gaps. |

## Findings that affect implementation

1. The API mode array is exactly ALL_MODES, SOLO, DOUBLES, QUAD. Never normalize to QUADS. Periods are total/weekly/monthly/yearly. Reference UI says Quads but sends QUAD.
2. Reference `fetch` obtains a profile before leaderboard statistics. This must not displace VelvetG's confirmed leaderboard-first design; profile data should enrich asynchronously.
3. Reference `StatsView.bestWinstreak` uses the API's highest streak and labels HWS. Do not claim this is a current streak without an actual supporting field.
4. Reference `get` publishes pending cache state before scheduling; that handles fast completion but does not by itself make concurrent admission atomic. Cache/profile bounds and request queue limits need explicit enforcement in the native implementation.
5. Reference key capture strips formatting and matches a full-line prefix and key, then cancels the captured key event. This is useful behavior, not proof against every spoofing or earlier logging path. Native capture must verify the active connection/origin and an expected capture window before persisting or hiding a key, and handle logging before display.
6. `tabAlwaysShow` controls context visibility; `shouldRenderTab` still requires TAB held. Persistent key-up display is an additional user requirement.
7. Font selection and column spacing are shared reference options. Independent TAB/HUD settings are additional native requirements.
8. Reference `FontText.width` allocates an image/Graphics2D measurement context, and table renderers build collections and widths during render. These are code observations, not measured FPS numbers. Prefer current Glide caches where stronger and instrument before changing the renderer.
9. Reference background decoding runs on an image worker; texture replacement happens during draw. Resource generation/cancellation, queue limits, disabled-feature cleanup and context destruction still need native lifecycle integration. Do not recreate a screenshot startup preload.
10. Reference badges rely on an optional PolyPlus interface that may not exist in Glide. No badge should imply another player's installed client without a reliable source.
11. Arrival grouping is speculative; server-visible party membership and cached friend records must remain distinguishable. No identity inference is included.
12. HTTP 400/404 mappings in a reference implementation are not sufficient evidence of a hidden identity. Distinguish bad mode/request/schema/unavailable responses; never automatically reveal identities.

## Reference test coverage inspected

ApiKeyCaptureTest; PikaStatsClientTest (local HTTP, key/rotation/redirect, pacing, stale success, profile sharing, Retry-After/cooldown); ColumnLayoutTest; TabPositionTest; TeamOrderTest; SocialTrackerTest; OtherPartyTrackerTest; MatchOverviewTest; PikaContextTest; RankLabelTest; PingTextTest; TableMotionTest; TableLayoutMotionTest; CatboxGalleryTest; StatsViewColorTest. Denick tests are excluded from native integration. These tests were read, not run.

Native API tests must additionally cover every requested HTTP status and failure path, hard queue bounds, simultaneous callers, cancellation/generation resets, secret-free logs, partial/oversized responses, credential changes and endpoint restrictions. Reference tests do not validate the installed client.

## Current completion state

- Completed: filesystem discovery, exact reference tag and source audit, setting inventory, collector/restorer implementation and fixture tests.
- Pending essential input: exact Lean client parts and diagnostic ZIP. No installed binary, native settings, current key-file format, screenshot patch or launcher metadata has been inspected yet.
- Blocked until that input: native patches/integrations, classification against current Lean, client regression tests, API integration against existing service, Windows install/rollback and startup/render profiling.
- Not measured: startup time, FPS, frame percentiles, GC, render overhead or memory gains. No live game or authenticated Pika request was performed.

Once collected, audit ScreenshotManager and API/classloader/bootstrap patches first, retain existing configuration/module identities, create exact-hash reversible transforms, then work through B/C/D/E with Java 8 verification and local HTTP tests before an installer is delivered. Stage F includes installation verification, rollback and local benchmark tooling. The current deliverable is only a collector, not a client patch.
