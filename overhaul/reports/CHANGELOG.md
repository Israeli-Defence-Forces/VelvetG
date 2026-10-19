# Overhaul2 changelog

Continues Overhaul1/FIX1 against the same exact collected baseline. Includes every preceding repair.

- Removed eager built-in cape preview uploads and startup cape binding. Existing on-demand preview drawing and game texture binding remain. Custom-cape decode/registration is retained.
- Grouped pending profile enrichment by player/session across stat selections. Duplicate cache-key callbacks are coalesced; successful profiles merge into every waiting mode/period cache. The existing bounded executor is reused; failed profile fetches have a 30-second admission cooldown. Added a post-response generation guard before parsing social data.
- Added independent waiting/active TAB and HUD profiles for columns, limits, sort, font/size, spacing, heads, header, rank placement, rows, dividers, shadow, scale, radius, opacity, palette, backgrounds and motion controls.
- Added native column toggle/reorder UI, value hints, responsive settings pagination, a once-per-second API status page, named secret-free profiles and Maximum FPS/Balanced/Visual presets.
- Added actual TAB dragging, grid snapping, viewport bounds and atomic two-coordinate Apply. Preserved the native HUD editor.
- Added independently timed native entry/resize motion, optional bounded cached row exits, independent panel show motion and image opacity/fit/alignment/zoom. Obsolete image jobs are cancelled/ignored using per-slot generations.
- Added explicit nametag phase/spectator controls, configurable cached friend highlighting/order, experimental party arrival window, optional match counters from visible TAB text, and independent ping units.
- Memoized resolved context settings and column lists; moved team color constants out of sort comparisons; skipped table construction in disabled contexts. Maximum FPS bypasses optional row/divider/shadow/motion/background work without rewriting normal settings.
- Added isolated Overhaul2 version/library/receipt, first-use read migration into a separate v2 settings file, preservation of unknown local properties, transactional validation and secret-free named-profile import/export.
- Build now clears generated tool/helper class directories so removed classes cannot leak into subsequent packages.
- Added Java 8 structural, profile-coalescing, retained-row, geometry, settings, migration and installer preservation regression coverage. No Minecraft/FPS or Windows PowerShell 5.1 result is claimed.

# Overhaul1 changelog

Target: exact collected 2.0.9.6-lean-fixed-20261016; Java 8 runtime.

Installer FIX1:

- Fixed the default installer failing with “The expression after & ... was not valid”: a helper named Java was shadowing discovery of the Java executable.
- Renamed the helper; restricted PATH discovery to executable applications; validated explicit paths; retained JAVA_HOME priority with fallback for stale values and quoted paths.
- Added eight executable-discovery regression scenarios and installation/verification/rollback coverage without an explicit Java path. Runtime client changes are identical to the initial Overhaul1 package.

Client changes:

- Removed screenshot startup discovery/deletion; preserved all physical screenshot files.
- Moved gallery metadata scanning and image decode away from the render thread; added bounded previews, one-upload/frame admission, LRU/idle eviction, file-change checks and native image cleanup.
- Preserved existing LWJGL compatibility and JSON verification repair; fixed evidenced Discord null-close and connected/repeated-close handling.
- Preserved leaderboard-first asynchronous API architecture, X-Api-Key and QUAD; bounded manual and automatic request admission plus profile queue/cache; rejected stale epoch work before profile I/O.
- Added opt-in bounded API-key acquisition, whole server-shaped response/window checks, pre-chat key suppression, credential-aware rejection and suppression of further rejected-key HTTP, protected atomic storage, manual masked entry, exact production host guard and secret redaction.
- Added cached profile guild/friends; kept profile enrichment separate from core statistics.
- Added independent table row caps, sort overrides, explicit column order/hiding including guild, fonts and cell spacing; added persistent TAB and position controls while retaining native HUD settings/editor.
- Added optional cached nametag statistics above/beside normal names within native visibility checks; highest streak correctly labelled BEST WS.
- Added lazy licensed Poppins and local fonts, optional local/random/explicit Catbox PNG backgrounds and show transitions. External images default off; disk/GPU/worker bounds apply.
- Added reversible Maximum FPS presentation mode and bounded glyph-width/vertical metrics caches with allocation-free warm lookup keys.
- Coalesced the two existing property mirrors into atomic writes after 500 ms idle, retaining unknown keys/Unicode.
- Added disabled-by-default frame/CPU/render-section/GC/cache/thread/API/startup instrumentation and offline/local measurement tools.
- Added source-only patch distribution, exact target/entry verification, private backups, separate launcher version/library, SHA-1/size metadata, refusal to overwrite, tamper checks and rollback.

See the feature matrix and limitations for incomplete parity and unverified live behavior. No source, assets or JAR from the ARR ChuStatsu mod are installed.
