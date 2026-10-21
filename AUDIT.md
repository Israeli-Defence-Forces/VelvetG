# RC1 audit

Continue from the hardened implementation. No production merge or original installation replacement.

Resolved defects: foreign first-caller ownership; missing exact Netty array callback signature detected by real Mixin transformation; release guards disabled by Python optimization; stale connection callbacks; duplicate packet observations; old installer reuse for a new JAR; partial writes presented as complete recordings; missing segment/tamper detection; startup/export state latches; non-atomic kinematic reflection initialization; dense analyzer scans; rendering lock/IO coupling.

Validation boundaries and per-system status are recorded in FINAL-REPORT.md. Runtime startup, full existing/new Mixin coexistence, native config serialization, GUI editor behavior, Windows filesystem behavior and real performance remain unexecuted. The requested historical CSV is absent. Authoritative server outcomes and calibrated timing are unavailable.

Existing `upgrade_v4.py`, historical manifests, source ZIP and old reports remain historical assets only. The RC1 builder always generates `*.install.py` from `install_release.py` pinned to the actual newly built JAR. Source original/v3/v4 profiles are cloned into a distinct RC1 profile after hash verification. Unknown targets, corrupt backups and outside edits are refused. Recovery uses a durable journal; first-install rollback removes the owned RC1 files and preserves the source profile.

New proprietary client archives must never be committed to this public repository. Release code and evidence can be reviewed through draft PR #3. Native Mixins and module are included in the binary but remain experimental until a real game launch.
