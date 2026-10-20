# CombatIQ Phase III validation ledger

## Previously verified
GitHub Actions run [39460312267](https://github.com/Israeli-Defence-Forces/VelvetG/actions/runs/39460312267) completed successfully on Java 8, 17 and 21 for the **earlier standalone kernel**.
The previous result does not automatically certify these Phase III commits.

## Phase III CI verification

GitHub Actions [run 39465947527](https://github.com/Israeli-Defence-Forces/VelvetG/actions/runs/39465947527) completed successfully for all three standalone jobs: Java 8, 17 and 21. Each completed the compile-and-test step. This verifies the Phase III kernel changes in CI, **not** in-game behavior.

## Changes verified in standalone CI
- Added dedicated `INBOUND` producer lane to prevent mixing receive and outbound-write callbacks.
- Expanded per-producer tracking to four lanes.
- Added strict event/stage checks for schema 2. Schema 1 replay remains available for historic files.
- Added baseline-aware forward-up classification; an unknown starting binding cannot be called a confirmed W-tap.
- Display watermark now uses max observed timestamp in-session rather than last drained producer timestamp.
- Expanded log loss footer with dedicated inbound-drop/misuse counters.
- Updated tests for input initialization, separate inbound lane, stage validation and cross-producer timing.

## Verified vs unverified boundaries
- **Code committed:** see Phase III branch history.
- **Java 8 build of this revision:** CI succeeded in the linked Phase III run.
- **Minecraft injection:** not done, requires hash-verified GlideClient binary.
- **Runtime HUD/Windows/Linux game launch:** not tested.
- **Pika server / real user combat advantage:** not tested.
- **Performance:** not benchmarked.

## Follow-up quality risks
1. Event snapshots currently provide execution counts and flags, not authoritative hit attribution.
2. Three lanes still have strict single-producer ownership; multi-channel adapters need separate queues.
3. `Analyzer.validEvent` tracks recognized combinations but must be reconciled with the exact injected hooks.
4. On reconnect, the native adapter must generate unique session epochs rather than merely reuse entity IDs.
5. The record worker makes no assertion that draining streams yields global chronological order.
6. Any snapshot probability requires calibrated private-server truth; no fabricated "perfect timing" percentage.
