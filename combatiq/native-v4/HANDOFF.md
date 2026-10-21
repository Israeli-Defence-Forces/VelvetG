# Testing handoff

This is a source checkpoint. There is no newly generated installable Minecraft JAR.

Run source tests from the repository root with Python 3 and a JDK on PATH:

```sh
python3 combatiq/build.py
python3 -m unittest discover -s combatiq/native-v4 -p 'test*.py' -v
python3 combatiq/native-v4/combat_analyzer.py combatiq/native-v4/fixtures/synthetic-combat.csv
python3 combatiq/native-v4/benchmark.py --batches 1000 --repeats 2 --out benchmark.json
```

The fixture is synthetic. To analyze an actual v3/v4 recording, substitute its path. Keep the CSV private. `combat_analyzer.py` supports schemas 1/2; the historical `analyze_native.py` remains the schema-1 diagnostics tool. The optional schema-2 FILTERED footer is separate from loss; absence in old logs means unknown, not zero. A release/attack attempt is never server hit confirmation.

For native source validation against the exact baseline:

```sh
python3 combatiq/native-v4/validate_sources.py --client exact-v3.jar --minecraft minecraft-1.8.9.jar --out source-validation.json
```

`--abi-only` explicitly permits a different client API for compilation checks; it cannot certify an installable binary. The included source validation used this mode with an available r8 client.

Once the exact v3 baseline is available, build to a NEW output path:

```sh
python3 combatiq/native-v4/build_v4.py --v3 exact-v3.jar --minecraft minecraft-1.8.9.jar --native-module --out CombatIQ-hardening.jar
```

The build requires the pinned v3 SHA, official Minecraft SHA, Java 8 classes and all compiler-generated dependencies. It preserves unrelated original archive bytes and generates a manifest plus `CombatIQ-hardening.upgrade.py` pinned to the actual new artifact. That generated installer upgrades an exact installed v3 profile and supports `--game-dir`, `--check`, and `--rollback`; inspect its `--help` first and supply `--patched-jar CombatIQ-hardening.jar` explicitly. An existing historical v4 profile must first use its original tested rollback to restore v3. The unchanged `upgrade_v4.py` and old manifest are historical v3-to-v4 tools and will refuse a new source build. Do not change their constants by hand or run a historical installer on the new build.

Live validation, after a build exists: preserve the original profile; use the side-by-side experimental profile; verify --check and rollback before a test recording. Launch with recording disabled, inspect Mixin logs for all optional probe targets, enable CombatIQ/Recording in the native module, remap forward, repeat release/repress, open a GUI, lose focus, change world, test singleplayer and multiplayer, disable/re-enable, and inspect clean CSV endings and lost/filtered counts. Verify native position/scale/settings persistence, theme, preview, crosshair, Dynamic Widget, SnapTap and delay fixes. Collect real frame-time A/B runs separately; synthetic callback nanoseconds do not imply FPS.

## Controlled private-server calibration

Use only an authorized controlled server. Define the accepted outcome before collection (receipt alone is not damage acceptance). Randomize prespecified interval candidates within independent session units; keep context, target motion and network conditions controlled or explicitly stratified. Assign complete sessions to train or test before viewing results. Keep the held-out sessions untouched during candidate selection. Record measured RTT/jitter separately from any emulator setting; never infer one-way delay as RTT/2.

`calibrate.py` consumes JSONL with exactly these fields:

```
trial, session, split, context, candidate_ms, release_ms, tick_ms,
client_event, server_event, server_tick, server_sequence, accepted, authoritative
```

Identifiers must be anonymous nonempty strings. `split` is train/test, timings are positive finite numbers, ticks/sequences nonnegative integers, outcomes/source flags boolean. External instrumentation must establish event correspondence and authoritative provenance; a boolean in an imported file cannot authenticate it. No client/server clock offset or server tick phase is inferred. Run:

```sh
python3 combatiq/native-v4/calibrate.py trials.jsonl --baseline-ms 40 --candidates-ms 40 60
```

Candidate selection uses training session means. Separate held-out session bounds compare the selected candidate to the baseline; overlap or insufficient sessions yields no recommendation. Context summaries and variability remain observational. Multiple exploratory contexts require a separate multiplicity plan. This code never controls input or packet timing.
