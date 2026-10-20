# CombatIQ Phase III controlled test protocol

## Local execution reliability
Capture queued key edges, actual sampled binding, local sprint state, actual C0B/C02 write order, client ticks and wall-clock intervals.
Test W release durations 15, 30, 45, 60, 80, 100 and 140 ms at multiple input-to-update phases. These are **experimental settings**, not game timing recommendations.
Repeat with GUI focus loss, open chat, rebinding, synthetic SnapTap activity, unblocking and frame stalls at 100/250 ms.
Match every judged tap to a complete sequence. Missing samples or queue loss must suppress judgment.

## Controlled server
Use a private 1.8-compatible server instrumented at packet handler entry, attack evaluation, victim immunity decision and velocity generation.
Server timestamps cannot be subtracted directly from client `System.nanoTime` without synchronization. Record server tick and within-tick ordinal separately.
For damage/hitselect, compare attack intent, packet write, server acceptance, victim immunity, and motion outcomes; do not use hurt animation as sole proof.

## Network matrix
Sweep nominal RTT 40/120/180/240/300/400 ms, upload shares 20/50/80%, and stable/mild/bursty jitter. Validate actual realized direction delays and TCP stalls. Do not reorder application packets artificially.

## Evaluation
Compare no coach, optimized fixed timing, event-only coach, personalized SHIFT calibration and extended PULSE only where each is actually implemented.
Prespecify independent match/session units and main endpoint. Keep natural gameplay observational and use randomized permitted private practice for causal claims.
Report Brier score for calibrated predictions, attribution precision and unknown rate, frame time percentiles, drop count, and effects with confidence intervals.
Keep human reaction time separate from network delay; if feedback arrives too late for useful action, show retrospective training rather than a false instant cue.

## Gates
No public recommendation until its evidence is sufficient; no server timing accuracy claims without authoritative ground truth; no release without Windows Java 8 and native render/packet tests.
