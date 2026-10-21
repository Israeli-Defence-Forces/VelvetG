# VelvetG Knockback Trainer RC1

A native, passive Minecraft 1.8.9 / Java 8 trainer built on the exact working CombatIQ RC1. Enable **Knockback Trainer** in the native module list and fight normally. CombatIQ recording can remain off. There is no CSV dependency and no input, knockback or packet modification.

## Install and recover

Extract the complete private release ZIP. Requires Python 3.9+ and an existing working VelvetG/CombatIQ installation. In Command Prompt in the extracted folder:

```bat
py -3 VERIFY.py
INSTALL.cmd --game-dir "%APPDATA%\.minecraft" --source-version "VelvetG-CombatIQ-RC1" --check
INSTALL.cmd --game-dir "%APPDATA%\.minecraft" --source-version "VelvetG-CombatIQ-RC1"
CHECK.cmd --game-dir "%APPDATA%\.minecraft"
```

Use your actual game directory/source profile ID if different. Select **VelvetG-Knockback-Trainer-RC1** in the Minecraft launcher. The new profile clones the working source metadata and uses a new, exactly pinned GlideClient library; original RC1/client files are preserved. Source profiles must directly declare their GlideClient library/artifact path. Close Minecraft before installation or rollback.

```bat
ROLLBACK.cmd --game-dir "%APPDATA%\.minecraft"
UNINSTALL.cmd --game-dir "%APPDATA%\.minecraft"
RECOVER.cmd --game-dir "%APPDATA%\.minecraft"
```

First-install rollback removes the owned trainer profile and leaves RC1 available; upgrades support one-step rollback. Recovery handles interrupted transactions and refuses outside edits or corrupt backups.

## Training and observable definitions

- A qualifying local impact proxy pairs a local entity hurt-status observation with nontrivial local velocity feedback within two client ticks. It does not identify an attacker or prove a server damage tick. Unpaired hurt, airborne/unknown ground, recent significant falls, water/lava/fire/riding, interrupted contexts and stalled processing abstain as UNKNOWN. A valid pre-impact grounded jump can still be matched after takeoff.
- Signed timing error = manual jump edge time minus locally observed impact time minus configured target offset. Negative means early; positive means late. Target offset is 0–6 ticks; the configurable matching window is 0–10 ticks. PERFECT means error within half a nominal client tick. Other matched presses are EARLY/LATE; no eligible press by the end of the matching window is MISSED. Settings changes clear pending correlations. Milliseconds are nominal tick conversions, not server latency measurements.
- UNKNOWN is counted separately, excluded from failed-attempt counts and mean error, and preserves the streak. Mean timing error is signed and includes matched PERFECT/EARLY/LATE attempts only. Session Reset clears accumulated statistics/history.
- **Falling attempts %** = outgoing ATTACK attempts observed with known ground/movement context, airborne, descending and above a collision surface, divided by all attack attempts with known qualifying context. Rising/grounded attempts remain in the denominator; unavailable or environmental contexts are unscored. This is not confirmed falling-hit percentage. Accepted falling-hit percentage is explicitly UNKNOWN.
- The graph samples collision distance beneath the actual player footprint (up to 16 blocks) using block collision boxes, including slabs/stairs. Ascending/descending paths have separate colours. Dots mark outgoing attack attempts (falling attempts use the ascending/positive colour); short baseline ticks mark takeoff/landing and vertical ticks mark local impact proxies. Missing surfaces create gaps. World/player/connection transitions clear the graph and pending correlations.
- Sound is local feedback, rate-limited to one playback per five client ticks. Falling sounds are for observed falling **attempts**, not accepted hits. Built-in sound names such as `note.pling` or `random.orb` are configurable; volume 0 disables playback. Timing sound is optional.

## Native controls

22 stable-key native settings cover timing/graph/statistics, falling/timing sounds, popup/debug, compact/expanded style, target/window, volume, display duration, 2–30 second history, 0.5–16 block graph range, background opacity, HUD scale, four colours, sound name and session reset. Position/scale/theme/font/editor drawing use the existing HUDMod system. Native ProfileManager's existing typed settings and widget persistence are reused; the trainer does not write config or recordings on render.

## Build and tests

From repository root, with a JDK:

```text
python -m unittest discover -s knockback-trainer -p "test*.py" -v
python knockback-trainer/build.py --base CombatIQ-RC1.jar --minecraft minecraft.jar --out VelvetG-Knockback-Trainer-RC1.jar
```

Base SHA-256: `43f3a3f9b477986bf76b8ec8cc896df3ff1cc3e294d7526e16bc15fc128a3ac9`.
Minecraft SHA-256: `14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298`.

The build enforces Java 8 bytecode, dependency closure, exact original-entry preservation, unique Mixin registration and a new-artifact installer pin. See reports for actual checks and limitations. The source included in the package requires the existing verified RC1 baseline; proprietary baseline bytes are not included separately.

## Validation boundaries and final acceptance

Executed: deterministic core/bridge/native draw/settings contracts; original RC1 regressions; actual headless transformation of the four new trainer Mixins together with RC1's six additive Mixins; binary preservation and real-artifact installer/rollback checks; synthetic timings/allocations.

NOT executed here: a real Minecraft launch, all original client Mixins together, real NanoVG/editor interaction, actual profile save/restart and audio hardware behavior, Windows OS installation, real-world terrain/chunk boundaries, multiplayer latency or gameplay FPS/frame times. Native persistence code paths were inspected; the executed persistence test is a contract round-trip, not an actual restart.

On your Windows client: launch original RC1 and the new profile; enable the trainer with recording off; verify a real fight produces local timing feedback, graph and statistics; test remapped and mouse-bound jump, held-key repeats, focus/GUI changes, water/fall uncertainty, slab/stair landings, reconnect; drag/scale/change theme/font and restart to check persistence; test sounds/volume/reset; perform rollback/uninstall; compare matched gameplay frame-time/FPS captures with trainer off/on. Treat this as an experimental build until those checks pass.

## Public references

Original implementation informed by [Lunar Apollo's documented option ranges](https://lunarclient.dev/apollo/developers/mods/knockbacktrainer) and [Lunar's public feature description](https://www.lunarclient.com/news/lunar-client-monthly-recap-july-2026). No Lunar implementation or assets were copied. The half-tick perfect threshold and evidence rules above are this implementation's explicit definitions.
