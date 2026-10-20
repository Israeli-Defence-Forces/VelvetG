# Lunar and comparable timing feedback — evidence boundary
Inspected 20 October 2026. This document separates public options, public scorer code, product descriptions and unverified behavior.

## Lunar: what is confirmed
The public [Apollo source at 63fda2a](https://github.com/LunarClient/Apollo/tree/63fda2ae735c6ca4c29e52cb5583ae9f89ac1cc3) includes [ModKnockbackTrainer.java](https://github.com/LunarClient/Apollo/blob/63fda2ae735c6ca4c29e52cb5583ae9f89ac1cc3/api/src/main/java/com/lunarclient/apollo/mods/impl/ModKnockbackTrainer.java). This is an option declaration, not the private scorer:
- enable default false;
- target-ticks default 0, range 0–6;
- window-ticks default 5, range 0–10;
- deprecated falling-hit-sound options are still declared in that source.

The [pinned documentation source](https://github.com/LunarClient/Apollo/blob/63fda2ae735c6ca4c29e52cb5583ae9f89ac1cc3/docs/developers/mods/knockbacktrainer.mdx) defines jump timing relative to the tick the player was hit, and documents the first three options. Live developer documentation/search snippets and pinned generated documentation differ in whether deprecated sound options appear; neither reveals the underlying detector.

The [official recap](https://www.lunarclient.com/news/lunar-client-monthly-recap-july-2026) describes early/late jump feedback, a ground-distance graph, configurable offset/window, and session statistics such as falling-hit percentage, streak and tick error. Its title says June while its URL/body refer to July; do not silently resolve that editorial inconsistency into a precise release date. Its description of landing hits while falling is not the same statement as the developer page's incoming-hit anchor. They might describe separate subfeatures; the internal anchor remains unresolved.

The official video listing [Knockback Trainer, Chat Emojis, and more! — Recap #28](https://www.youtube.com/watch?v=U5sQvRsGScc) was found. Video retrieval was unavailable/throttled; this investigation did not watch or frame-analyze it. Product images/descriptions and API settings were inspected as documentation; no installed Lunar build, live settings capture, authenticated measurement or private binary decompilation was performed.

### What these sources do not establish
| Question | Supported answer |
|---|---|
| Dedicated stock W-tap / hit-select trainer? | Not verified in official documentation, public Apollo tree or targeted searches. Absence from Apollo is not proof absence from every client build. |
| Fixed versus adaptive window? | Configurable tick target/window is confirmed. Automatic adaptation is not documented in the inspected sources. |
| Physical press, binding state, jump execution, or packet event? | Developer prose says jump press; exact hook unknown. Do not substitute the hook in another mod. |
| Hurt-time, damage status, velocity, outgoing attack or server-enriched event anchor? | Unknown. Official descriptions are not sufficiently consistent to decide. |
| Which client/server tick? | Unknown. Tick-valued UI does not establish synchronization with the authoritative server. |
| Latency compensation / one-way model? | No verified implementation evidence. Lack of documentation does not prove it is absent. |
| Successful hit confirmation? | Unknown attribution logic. Session “hits” in a UI should not be treated as instrumented damage acceptance. |
| Statistics? | Session summaries are documented. Statistical learning, confidence calibration, randomized outcome optimization and Bayesian state inference are unverified. |
| Why does it feel effective? | Hypothesis: immediate understandable error labels and repeatable practice anchors improve rhythm. No controlled efficacy result or attribution audit was found here. |

Apollo provides server/client option integration. Calling its public class “reverse-engineered Lunar combat timing code” would be false. Search results advertising W-tap or hit-select in a third-party tool that runs alongside Lunar also do not establish a Lunar feature.

## A public scorer actually inspected
[Jump Reset Indicator source at 5d535ff](https://github.com/sootysplash/jump-reset/tree/5d535ff2d1ca9d18dbc5dc3e01ae6b09bf17e7f0) is a Fabric project linked from its [Modrinth listing](https://modrinth.com/project/kJEc5zbS); it is not a Java 8/Forge 1.8.9 drop-in. Files inspected: JumpResetIndicator.java, mixin/LivingEntityMixin.java and ConfigJR.java.

Its mixin stores the local player's tickCount at jumpFromGround entry and at handleDamageEvent entry. The HUD calls the result perfect when jumpAge= hurtAge+1, with a configurable absolute-difference cutoff (default 10) and a 2.5-second time condition from the last jump. There is no network-delay estimator or learned window in these inspected files. This records jump execution and locally applied damage feedback, not physical key press or server ground truth.

Additional code observations/inferences:
- The late label uses jumpAge−hurtAge+1, while the perfect condition centers on hurtAge+1. Relative to that stated center, a two-tick positive error can be displayed for a one-tick late event. This is arithmetic inspection, not an in-game reproduction.
- Ages and lastModTime are static with no reset shown at connection boundaries in these files. Default zeros and later damage events can interact with the display before a genuine paired event; reproduce before calling it an observed bug.
- A later hurt event can change the score for an earlier jump while its display timeout remains active. There is no explicit per-attack identifier.
- These limitations do not establish the same implementation or bugs in Lunar.

No source is copied into CombatIQ. Apache-2.0 licensing on that project is distinct from the license and provenance of the user's client.

## Comparison
| System | What evidence establishes | Important limit |
|---|---|---|
| Lunar Knockback Trainer | Official tick target/window options; documented jump feedback, graph, session stats | Private scorer/anchor/latency model unknown |
| Lunar Combo / HitColor / Keystrokes / ReachDisplay option families | Public Apollo module-option names | Their existence is not a W-tap execution analyzer or an accuracy guarantee |
| [Badlion Combo Counter](https://www.badlion.net/wiki/combo-counter) | Official description: increments hit count, resets when hit; UI settings | Internal attribution and authoritative acceptance not exposed |
| [Badlion Keystrokes](https://www.badlion.net/wiki/keystrokes-mod?lang=en), [Hit Color](https://www.badlion.net/wiki/hit-color?lang=en) | Key display and damage-color customization | Key display alone does not show whether movement sampled a release |
| [BadlionClientModAPI](https://github.com/BadlionClient/BadlionClientModAPI) | Public mod-control integration API | Not source for private combat detectors |
| Jump Reset Indicator | Actual fixed tick relation between local execution and damage callback | Modern Fabric; no server synchronization or per-hit ground truth |
| Arbitrary W-tap listings / community “timing” systems | A listing or a player report may identify a lead | Unavailable source/build means hook precision and learning cannot be inferred |
| CombatIQ prototype | Executed tests for sampling classification, stage separation, losses, replay and thread ownership | No Minecraft adapters, live HUD rendering or comparative PvP result yet |

Useful community lead: [Hypixel W-tap timing discussion](https://hypixel.net/threads/w-tap-timing.4496539/) contains high-ping players using visible hit feedback. That is evidence of a practice habit and perceived usefulness, not a measurement of packet anchoring or damage acceptance. Exclude unsupported server-mechanics claims from the technical model.

## Black-box experiments that can resolve unknowns
Use an authorized private server, fixed client build/version hash and a screen recording synchronized to the client/server trace. Do not redistribute proprietary binaries. Save settings screenshots and record every changed option.

1. Sweep jump execution at each offset around server-confirmed incoming hit; vary only the download delay. Does the displayed score move with packet receipt/application or with server time?
2. Hold upload fixed and vary download; repeat with the opposite asymmetry. A change with one direction alone can distinguish classes of anchors, not prove exact internal code.
3. Stall main thread after packet ingress but before application. Compare HUD labels against receive and apply ticks.
4. Tap jump briefly without actual jumping (airborne, low ceiling, or prevented jump). Distinguish key/binding hook from executed jump.
5. Send two controlled damage events with one jump, then two jumps around one damage. Observe pairing, expiration and whether labels are rewritten.
6. Produce a stronger-damage accepted event without a fresh vanilla status, and an attack animation rejected by server immunity. Test false negatives/positives.
7. Vary target/window settings by one tick, hold the trace fixed, and check exact boundary inclusion. Repeat at 10 versus 20 TPS with true tick durations recorded.
8. Reconnect, respawn, change world/focus, and test startup defaults. Do old ages/streaks contaminate new sessions?
9. Record 0–300 ms one-way changes and compare stable versus jittered paths. Adaptive behavior requires a repeatable rule change beyond configured fixed offsets; subjective usefulness is insufficient.

Competing predictions must be registered before seeing the traces. A few screenshots cannot distinguish tick-based input, tick-based packet feedback and a shifted fixed window if their timestamps coincide in that trial. The goal is to force those anchors apart.

A demonstrably better system would need lower ground-truth attribution error or better randomized manual-combat outcomes at matched overhead and cue coverage. This prototype offers testable execution diagnostics that the inspected open-source scorer lacks; it does not claim to have beaten Lunar.
