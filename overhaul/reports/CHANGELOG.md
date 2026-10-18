# Overhaul1 changelog

Target: exact collected 2.0.9.6-lean-fixed-20261016; Java 8 runtime.

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
