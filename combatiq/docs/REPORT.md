# VelvetG CombatIQ — Phase II evidence audit and algorithm specification
Research cut-off: 20 October 2026. Status: source inspection plus tested standalone telemetry software; no Minecraft launch or comparative PvP experiment.

## Reading and evidence scope (Deliverable A)
Both supplied reports were read completely: **R1**, “Pasted text (2)(1).txt” (1,091 lines, ordered-event reconstruction); **R2**, “Pasted text (4).txt” (962 lines, PULSE/SHIFT/SPAR/TICK-LENS). Their conclusions are not experimental results. Neither supplied an instrumented gameplay dataset demonstrating superiority.

Labels used here:
- **Code fact:** follows from the particular inspected implementation, subject to forks/modifications.
- **Documented:** a project describes its product this way; this does not reveal private implementation.
- **Inference:** a consequence of stated assumptions, requiring trace verification.
- **Hypothesis:** an untested improvement or causal explanation.
- **Tested:** only an experiment actually executed and linked in VALIDATION.md.

The baseline source is the publicly accessible MCP 1.8.9 mirror at [commit 6bc307e](https://github.com/Marcelektro/MavenMCP-1.8.9/tree/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft). This is a community decompilation, not an official Mojang source release, and is not proof of the binaries on PikaNetwork, MMC, or VelvetG. No Minecraft source/classes are redistributed here. Source locations below are method names; the pinned links provide exact inspected files.

The supplied VelvetG repository is a different constraint from a normal Forge source checkout. [lean/overhaul3 at aedd37f](https://github.com/Israeli-Defence-Forces/VelvetG/tree/aedd37f4e979c4c6233aa02d0c560f668ffcdafb) uses an exact-baseline binary patcher, plus public Java/ASM tools. The original client JAR and game/dependency JARs are absent. Therefore this change supplies a separately executable Java 8 kernel and a concrete integration plan, without claiming a rebuilt or instrumented client.

### Audit of earlier claims
| Earlier claim / proposal | Audit | Consequence |
|---|---|---|
| R1: local attack effects can disagree with server acceptance | Correct; PlayerControllerMP.attackEntity sends C02 before local attack; EntityOtherPlayerMP.attackEntityFrom returns true. Retained, not claimed as new. | Separate intent, write, damage feedback, private-server acceptance. |
| R1/R2: immunity has a stronger-damage exception | Correct but incomplete for feedback attribution. The stronger branch skips fresh hurt status/base knockback while the enclosing attack can still succeed. | Missing red flash is not evidence of rejection; see finding 9. |
| R2 §3: “raw input timestamp” directly measurable at a hook | Overspecified. A Java hook measures queue drain, binding mutation, or sampling; it does not measure USB switch closure. An OS/LWJGL timestamp is another clock domain. | Rename measurements by stage; do not publish physical input latency from nanoTime alone. |
| R2 §7: nominal 20 Hz means no reason for inference at 120 Hz | Too broad. Event capture must follow actual input/Netty callbacks, including between ticks. Periodic expensive model refresh may remain slow. | Event-driven capture; worker snapshots at 10 Hz; rendering does not run inference. |
| R2 PULSE hypotheses include uncertain sprint/attack ordering | Where a single connection's outbound order is observed, alternative permutations contradicting that order should receive zero weight under vanilla serial handlers. Acceptance and state changes between packets remain uncertain. | Condition on observed order before adding probability. |
| R2 PULSE posterior equations constitute a usable model | They are a framework, not a fully specified filter: transition, emission, acceptance likelihoods, priors, and calibration are missing. | Do not implement the illustrative percentages as probabilities. |
| R1/R2: incoming packet rhythms may estimate server phase | Plausible only with a specified emission model. Received phase includes server flush, network delay and client scheduling; these are not identifiable separately from ordinary traffic. | TICK-LENS is a private-server falsification experiment, not a production clock. |
| R2 SPAR: regress knockback against subsequent positions | Missing the player velocity-send/restore path and victim-controlled movement reports. | Separate server motion, S12 payload, client application and next movement report. |
| R1 ordered reconstruction is the strongest starting point | Supported as an engineering priority, not as a proven combat advantage. | Implement deterministic observable diagnostics before a probabilistic planner. |
| Both: Lunar has useful timing feedback | Supported, but configuration/API and marketing are not scorer source. | Detailed evidence boundary in LUNAR.md; no invented stock W-tap module. |
| Both: fixed timing is an inadequate ultimate objective | Reasonable research motivation, not a result. A tuned fixed window may beat an overfit adaptive one. | Compare against separately optimized fixed and simple event-based baselines. |

## Ranked findings and falsifiable interactions (Deliverables B/C)
Rank reflects likely diagnostic value and testability, not measured PvP effect. “New” means absent or inadequately developed in the supplied reports, not worldwide novelty.

Source keys:
- [MC: Minecraft.runTick/runGameLoop/setIngameNotInFocus](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/client/Minecraft.java)
- [SP: EntityPlayerSP.onLivingUpdate/onUpdateWalkingPlayer](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/client/entity/EntityPlayerSP.java)
- [EP: EntityPlayer.attackTargetEntityWithCurrentItem/damageEntity](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/entity/player/EntityPlayer.java)
- [LB: EntityLivingBase.attackEntityFrom/knockBack/moveEntityWithHeading/jump/handleStatusUpdate](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/entity/EntityLivingBase.java)
- [PT: PacketThreadUtil.checkThreadAndEnqueue](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/network/PacketThreadUtil.java)
- [NS: NetHandlerPlayServer.processUseEntity/processEntityAction](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/network/NetHandlerPlayServer.java)
- [MI: MovementInputFromOptions.updatePlayerMoveState](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/util/MovementInputFromOptions.java)
- [KB: KeyBinding.setKeyBindState/onTick/isPressed/unPressAllKeys](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/client/settings/KeyBinding.java)

| Rank | Finding and evidence | Implication / disproof experiment |
|---|---|---|
| 1 | **Attack processing precedes this tick's player movement consumption in this vanilla loop** (MC runTick: attack loops, then world.updateEntities; SP onUpdate → super update → onUpdateWalkingPlayer). New code-level interaction. | A W-up binding change in the same tick need not update sprint before that tick's attack. Log actual attack entry, consumption and C0B/C02 order. Falsified for VelvetG if its hooks reorder these stages. |
| 2 | **Unblocking can discard queued attacks for that branch**: MC tests isUsingItem, may stop using, then drains attack press counts without calling clickMouse; the else is not re-entered after stopping. New code fact. | Some “late block-hit” failures can be no C02 generated, independent of ping. Controlled release-use plus attack in one tick should reproduce; a client fix may remove it. |
| 3 | **Separate device queues and action batching lose physical alternation**: MC drains Mouse.next before Keyboard.next, then later consumes attack presses before use-item presses. New. This does NOT mean the mouse attack callback necessarily runs before keyboard drain. | L/R/L host chronology may become several attacks followed by use, depending on current-use branch. Replay actual hook order; compare idle and stalled frames. |
| 4 | **Several simulation ticks may occur almost together after a stall**: MC runGameLoop loops Timer.elapsedTicks; [Timer.updateTimer](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/util/Timer.java) caps catch-up at 10. New. | Tick gap × 50 ms is not wall elapsed time, and samples are not regularly spaced after stalls. Inject 100/250 ms stalls privately and measure both domains. |
| 5 | **A brief up/down interval may leave no consumed up state** (MC queue drains, KB final state, MI sampling). Earlier “input timing” descriptions did not specify this test. | Diagnose unsampled release, not “bad network W-tap.” Sweep release width and phase; disprove if every observed interval contains a real binding-up sample under the target client. |
| 6 | **W binding, effective forward movement, and sprint are three different states**: MI combines forward/back and sneak; SP applies item-use scaling and a sprint threshold. New aliasing analysis. | forward=0 can mean W+S; use/sneak can reduce movement without W-up. Record binding separately from consumed floats and synthetic module changes. |
| 7 | **Focus/GUI transitions are censoring boundaries**: MC focus loss invokes KB.unPressAllKeys, resetting state without a physical key-up. New. | Never score this as a manual tap. Tests alt-tab, chat, GUI and rebind with an open interval; any “miss” produced falsifies the classifier. |
| 8 | **Ray selection has its own age**: MC runTick refreshes getMouseOver(1.0F) before device drains; [EntityRenderer.getMouseOver](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/client/renderer/EntityRenderer.java) also handles render-time selection. New ordering detail. | It is wrong to assume only last rendered frame's ray, or to assume attack-time geometry equals later movement. Log ray build, chosen entity and attack generation separately; test multiple queued clicks. |
| 9 | **Accepted stronger damage may lack fresh hurt status/base KB** (LB attackEntityFrom). Strengthened earlier immunity finding, not newly discovered immunity. | Bayesian hit attribution needs a “successful without fresh status” likelihood. Private unequal-damage tests measure false negatives of red-flash-only trainers. |
| 10 | **Player knockback is not simply persistent server motion**: EP captures old victim motion, can send S12 for a player, clears velocityChanged and restores old motion. New. | Fit packet impulse → victim application → reported movement; do not fit one immediate server vector as the full physical trajectory. Disable victim movement or use a nonplayer control to expose the difference. |
| 11 | **An APPLY hook before thread handoff can double count** (PT re-enters the handler on main thread). New instrumentation consequence, although R1 mentioned thread scheduling. | RECEIVE belongs at channel ingress; APPLY belongs after the thread check. Verify one receive/one application correlation, including main-thread stalls. |
| 12 | **No extra server tick is required between C0B and C02** (NS serial state setters and attack handler). New precise ordering test. | Same-tick start→attack and attack→start differ; nominal tick equality is insufficient. Record within-tick ordinal, sprint at damage call and post-attack state. |
| 13 | **Local fake acceptance can consume sprint during a batch of clicks** ([PlayerControllerMP.attackEntity](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/client/multiplayer/PlayerControllerMP.java), EP; [EntityOtherPlayerMP.attackEntityFrom](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/client/entity/EntityOtherPlayerMP.java)). New interaction of previously known facts. | First and later C02 in a client tick can have different local state while server immunity rejects some. Test 1 versus several queued clicks; avoid teaching every animation as a fresh reset. |
| 14 | **Player immunity timer has a separate decrement path**: [EntityPlayerMP.onUpdate](https://github.com/Marcelektro/MavenMCP-1.8.9/blob/6bc307e9f937728886d3859987c0d3b441d14557/src/main/java/net/minecraft/entity/player/EntityPlayerMP.java); LB excludes MP from its ordinary decrement. New simulator hazard. | A simplistic test simulator can decrement twice and “discover” nonexistent hit windows. Assert one decrement per authoritative update. |
| 15 | **Nearby packet delay covariance can dominate RTT jitter** (mathematical inference, not game-code finding). Var(D_i−D_j)=Var(D_i)+Var(D_j)−2Cov(D_i,D_j). | Shared queue delay cancels from gaps; independent half-ping sampling exaggerates uncertainty and may invent reordering. Compare equal-RTT independent/bursty traces with matched marginal variance. |
| 16 | **Block mitigation and immunity compare different damage quantities**: EP damageEntity applies (1+d)/2 for eligible blocking after LB's incoming-damage gate. New interaction. | Blocking is not exactly 50% health reduction and does not simply shorten immunity. Log incoming amount, lastDamage, damage-call argument and final health/absorption separately. |
| 17 | **Missing telemetry is not negative evidence** (engineering hypothesis implemented conservatively). Sequence gaps, tail overflow and unclosed files can erase the very sample that distinguishes a successful release. | Loss must suspend verdicts, including losses after the final surviving event. Force overflow/truncation and require explicit incompleteness. |

These findings are mechanisms and predicted trace signatures, not demonstrated improvements. Findings 1–8 and 10–17 supply the requested additional investigations beyond the reports. Findings 9 and 13 deliberately acknowledge their overlap.

## Mechanistic model and limits
This section supplements rather than repeats the reports' basic combat descriptions.

### Sprint, attacks and blocking
Let S_j be server sprint immediately before ordered packet/event j, I_j the predicate that the target's attackEntityFrom returns true, and K the held weapon's knockback enchantment contribution. Vanilla's extra impulse multiplier is i=K+1[S_j]. On an accepted attack with i>0, extra target velocity is 0.5i(-sin yaw, cos yaw), target vertical addition is 0.1, attacker horizontal motion is multiplied by 0.6, and attacker sprint is cleared. With i=0 that particular slowdown/reset branch is not entered. Server rejection does not execute it. Local prediction can disagree.

C0B start sets S=1, stop sets S=0; accepted attacks can clear S independently of a stop packet. The client's field named serverSprintState remembers what it last tried to announce; it is not an acknowledgement. Therefore a missing explicit STOP does not prove a failed reset, nor does a START prove the server was sprinting at a later attack. Server inference must include accepted attacks and other server changes.

“Zest tapping” is a community technique label, not a distinct vanilla mechanic or packet. Its operational definition must be the actual forward release/repress, sprint and attack sequence shown by the player. Test short versus longer release and spacing patterns under that definition; do not invent a special Zest reset threshold.

Block use affects at least four mechanisms: effective forward input, sprint eligibility, attack generation while using, and damage mitigation. Record C08 use intent/writes and C07 RELEASE_USE_ITEM separately from animation and actual item-use state. A sword-block animation pack cannot establish authoritative blocking. The server can accept or reject interactions differently.

Vanilla 1.8.9 has no 1.9 attack-strength recharge. That does not make every click damaging. Input suppression, client selection, server range validation, target immunity, game mode, plugins and cancellations all intervene. NS uses a broad squared entity-distance guard of 36 when visible and 9 otherwise; this is not a “6-block legitimate reach” recommendation, nor the client's ray criterion, nor an anti-cheat rule.

### Damage, trades and knockback
For an ordinary living target, let h be hurtResistantTime, H its maximum, L lastDamage, and a incoming damage before the later mitigation stages. Subject to early rejection checks:
- h>H/2 and a<=L: reject.
- h>H/2 and a>L: pass a−L into damageEntity, replace L, skip the fresh status/base-KB branch.
- h<=H/2: set h=H, pass a, set hurt animation, run fresh-hit path.

This is a stateful gate, not a universally fixed 500 ms click cooldown. H and tick duration can change; stronger hits, server plugins, zero final health loss and mitigation complicate labels. Each victim has its own gate, so reciprocal trades are not forbidden by a shared combat cooldown. Hit-selecting can change relative geometry, momentum and who enters the other's gate; taking a hit does not create a special offensive permission to hit through the opponent's immunity.

For base knockback (ordinary nonzero separation, resistance check passed), horizontal victim motion becomes
v_h^+=0.5 v_h^- +0.4 n_away;
v_y^+=min(0.5 v_y^-+0.4,0.4).
Add the extra attack impulse above when its branch executes. Direction from relative position and extra direction from attacker's yaw need not agree. A sprint jump adds about 0.2 along facing while replacing vertical jump velocity; its relation to an applied velocity packet is ordered, not commutative. A packet arriving before versus after the jump can produce different outcomes. This is a testable interaction, not proof that any “perfect” display minimizes KB.

Between impulses, a limited free-motion approximation is
v_{h,k+1}=f_k(v_{h,k}+a_k), p_{k+1}=p_k+v_k,
v_{y,k+1}=0.98(v_{y,k}−0.08),
with collision resolution, acceleration, jump and packet application in their actual code order. Vanilla's ordinary horizontal drag factor uses approximately 0.91 airborne and 0.91×ground slipperiness on ground; acceleration also depends on ground/slipperiness and input. These equations are not valid for water, ladders, collisions, flying, nonvanilla velocity handling, or as a substitute for the exact update order.

PikaNetwork and MMC backend KB constants, acceptance rules, proxy scheduling and server TPS are unverified. [PandaSpigot's configurable-knockback patch](https://github.com/hpfxd/PandaSpigot/blob/master/patches/server/0014-Configurable-knockback.patch), inspected here, demonstrates modifiable friction/impulse behavior in a real compatible fork; it does not identify either public server's implementation.

### What can be observed
| Quantity | Public-client status |
|---|---|
| Hook entry nanoTime, client tick index, binding and movement values at that hook | Measured at that stage, with hook/scheduling error |
| LWJGL event timestamp | Source timestamp with separately established clock origin/resolution; not a physical switch timestamp |
| C02/C0B logical send call | Measured intent; may be queued/cancelled/replaced |
| Netty outbound handler order | Measured traversal at that chosen pipeline point; not NIC transmission/remote acceptance |
| S12/status arrival and main-thread application | Separately measured; status does not identify attacker |
| Own local sprint versus last observed outbound sprint transition | Separate observations; neither acknowledges server state |
| Remote encoded coordinates, interpolation target and rendered pose | Three distinct client quantities; future authoritative position hidden |
| RTT from ordinary server/tab mechanisms | Sampled and possibly smoothed/application-delayed; not exact current path delay |
| One-way delays, precise server phase, opponent input/RTT, private KB/anti-cheat rules | Unidentified without additional assumptions/instrumentation |
| USB switch time, monitor photon time | Require external instrumentation; a Java timestamp cannot recover them |

Let c be client clock, s=c+theta the server clock, D_up one-way upload delay, and E scheduled server emission time. A received timestamp has form r=E−theta+D_down+client_queue. Replacing theta by theta+epsilon and D_down by D_down+epsilon preserves r. Ordinary arrivals alone cannot separate these values. Similarly an RTT observation constrains D_up+D_down+processing, not each term; dividing RTT by two adds an assumption. See [RFC 7679 §3.7](https://www.rfc-editor.org/rfc/rfc7679.html) on one-way timing/synchronization uncertainty.

An attack's processing time in client-clock coordinates is
T_i=c_i^write+D_i^up+Q_i^server.
A “round to next tick” map is only one queue model. Real handler tasks can run within the same server tick in order, and a modded task drain can differ. Tick identity and within-tick ordinal must both be logged. TCP preserves byte order on one connection; it does not preserve user action order across clients, avoid head-of-line stalls, or guarantee particular flush timing.

With symmetric paths as an explicitly hypothetical example, a display at 300 ms RTT may reflect approximately 150 ms of download age before local queue/render delay, while your next action still faces upload and processing. These are not measurements of your actual one-way paths. Opponent packet age adds further uncertainty. No confidence interval narrower than its unmodeled delay error becomes justified because the estimator uses nanosecond arithmetic.

Human feedback has another delay L_human+L_display+L_capture. If the remaining target interval ends sooner than that combined distribution, an immediate “tap now” cue cannot reliably affect the event it describes. Prefer post-exchange feedback and advance rhythm practice. Measure each player's cue-response distribution privately rather than assigning a universal 200 ms reaction constant.

## Algorithm comparison (Deliverable D)
Ranks are engineering priorities conditional on missing real data, not an empirical leaderboard. “New” below describes combinations/applications; state machines, Bayesian inference, system identification and abstention are established methods.

| Rank | Candidate | Cost / state | At 180–300 ms and failure condition | Falsification |
|---|---|---|---|---|
| 1 | **Execution-aware ordered analyzer**, then small SHIFT calibration | O(1)/event; bounded rings; later small context table | Detects observable client execution failures independently of RTT. Cannot decide optimal server timing. Loses confidence on gaps, focus or synthetic inputs. | Ground-truth binding samples disagree with classifier, or diagnostic coaching fails held-out randomized practice. |
| 2 | **Execution-conditioned SHIFT**: learn a broad release/counterattack window only among clean executions | O(B) per worker update, small fixed B; no deep model | Can personalize frame/input effects. Needs randomized labels and enough comparable exchanges; opponent drift and selection bias can erase benefits. | Tuned fixed or event-only trainer matches/beats it on held-out conditions or harms spacing. |
| 3 | **Relative-gap robust feasibility**: optimize a cue across a set of plausible delays/positions rather than guess phase | O(BM) small grid outside hot path | Shared delay can cancel for closely spaced own packets; remote attack/position uncertainty may leave no safe interval. Abstain. | At equal cue coverage, interval violations or outcomes fail to improve over simple intervals. |
| 4 | **SPAR with innovation gating**: identify impulse response only on isolated, attributable trials | O(d²), low-dimensional regression; offline first | Potentially detects server-profile changes. Victim input, packet restore path and walls confound passive public observations. | Cannot distinguish hidden known private KB profiles on held-out moving trials; stop fitting. |
| 5 | **PULSE hybrid Bayesian filter**, discrete acceptance/sprint modes plus bounded movement state | O(M) or more per observation; model-dependent | Can combine ambiguous evidence, but posterior confidence is meaningless with incorrect likelihoods. At 300 ms hidden opponent actions multiply. | Poor reliability/Brier score or no incremental outcome benefit after the simpler analyzer+SHIFT. |
| 6 | **TICK-LENS** joint period/phase/delay reconstruction | Window scan/particles; worker only | Identifiability fails with unknown emission/queue delay. A precise estimate may only track packet flush rhythm. | Held-out server phase error no better than uniform/no-phase baseline or predicted intervals under-cover. |
| 7 | HMM/sequence model and opponent-specific policy | Extra parameters, training/memory/identity lifecycle | Could learn recurring attack rhythms; sparse encounters, player adaptation and latency changes overfit. | Chronological leave-opponent-out evaluation beats neither fixed nor basic velocity/event model. Defer. |

A Kalman filter is a possible smoothing tool, not authority recovery: teleports, collisions, quantization and velocity packets create discontinuities. Reset/update modes on these events; compare an alpha-beta filter and constant-velocity baseline before more complex models. “Low residual error” on interpolated positions is not future server hitbox accuracy.

Novel hypotheses worth testing are (i) conditioning learning on execution completeness to avoid learning stall artifacts as network strategy; (ii) choosing by relative-delay robustness instead of exact tick phase; and (iii) censoring unattributed impulse transitions before SPAR. This search did not establish worldwide novelty or patent originality.

## Strongest candidate: observable execution first, selective personalized windows second (Deliverable E)
### Stage 0, implemented exact diagnostic
Event e=(t, sourceTime, session, producer, sequence, correlation, type, stage, tick, entity, flags, x,y,z). All t values supplied to the kernel must use one process's nanoTime domain; sourceTime is opaque separate metadata. Never subtract client/server clocks without a fit.

For an eligible queue-observed forward-up U_j and subsequent forward-down D_j in the same context, define
M_j={movement-consumption samples m with U_j <= m < D_j and forward_binding(m)=UP}.
Use actual MAIN producer sequence for interval membership; equal t values remain ordered. Define:
- sampled release iff M_j is nonempty;
- unsampled release iff M_j empty AND the interval and capture are complete;
- censored otherwise.

“Sampled” means a binding-up state was present at the movement-consumption observation, not that a sprint reset or advantageous hit occurred. MODIFIED_INPUT remains visible; a zero effective forward float alone is not evidence of W release. SYNTHETIC mutation/focus/connection/correction boundaries censor intervals. Incomplete captures suspend subsequent verdicts for the recording; this conservative implementation requires a new recording to recover.

State: rawKnown/rawDown; releaseOpen/releaseSampled/releaseAt; localSprintKnown/localSprint; writtenSprintKnown/writtenSprint; per-producer previousSequence/previousNs; session/context; counters. Ground-truth acceptance is separate and disabled by default. On end-of-recording, an unclosed release is censored once.

Pseudocode:
    on gap, backward producer time or known loss: invalidate; censor pending interval
    on context/session change: censor; unknown local/written sprint
    on eligible nonsynthetic queue up: open interval
    on movement SAMPLE with forward binding up: mark interval sampled
    on eligible queue down: close as sampled/unsampled
    on local sprint SAMPLE: update local observation
    on Netty sprint WRITE: update last written observation
    on ATTACK intent/write or HURT receive/apply: count distinct stages
    on ordinary feedback: never increment confirmed-own-hit
    on trusted private SERVER_ACCEPT: count explicit ground truth

No public probability of hit success is emitted. The current confidence interface is complete/incomplete plus known/unknown states, not an uncalibrated percentage. A release seen in the host queue but missed by movement is a deterministic local result under complete hooks. It remains conditional on hook coverage.

### Stage 1, fully specified research policy, not yet implemented or validated
Use a deliberately small contextual table rather than full PULSE:
- contexts: grounded/airborne × item-use/no-use × stable/variable local frame pacing; split server profile and session. Add movement direction or network categories only after an ablation supports them.
- candidate release durations: an experimental grid such as {15,30,45,60,80,100,140} ms. These are assigned practice conditions, not recommended public-server delays.
- hit-select candidates: offsets relative to a named observable incoming feedback/apply event, using a separately prespecified grid. Do not mix offsets from different anchors in one table.
- record assigned cue a, actual execution, quality/censoring, and server-labeled endpoint. For causal estimates keep intention-to-treat assigned arms; never remove difficult executions from one arm after randomization. The “clean execution” model is a separate descriptive diagnostic, not a substituted causal comparison.

For each independent randomized practice unit g, compute z_g,a = fraction of eligible attacks accepted with the server's sprint flag on at attack, with explicit eligibility fixed before treatment. Preserve damage/spacing safety endpoints separately. Units can be short sessions/blocks; attacks within a unit are correlated. Fit mean outcome mu_a(c), uncertainty by resampling paired units. For a simple isolated-trial Bernoulli sandbox only, Beta(1+s,1+n−s) is an optional smoothed descriptive estimate; it is not a valid independent-attack posterior for clustered human combat.

Choose the set A_safe(c) whose lower confidence bound for improvement over the tuned fixed baseline exceeds a prespecified practical margin eta, whose upper bound on spacing/damage harm is below its margin, and whose independent-unit count meets a preregistered minimum. Merge adjacent duration bins only when all included bins satisfy the rule. If empty, recommend baseline or abstain. Choose the shortest safe interval only if the lower-bound advantage is maintained; otherwise report the broad set without an optimum point.

Formally, for uncertain latent state/delay parameters u in an empirically calibrated set U_c and candidate cue a:
V(a,c)=inf_{u in U_c} E[Y(a)|c,u] − lambda C_spacing(a,c,u).
a*=argmax_{a in A_safe} V(a,c).
Y and lambda must be fixed before evaluation; for the initial trial use the single sprint-at-accepted-attack endpoint and spacing as a noninferiority guard, avoiding arbitrary composite-score weights. U must include correlated delay differences and frame/sampling uncertainty; it must not be fitted as a zero-width phase from RTT/2. If intervals widen until rankings overlap, abstain. If there is no validated latent model, use the randomized table directly and do not compute V.

Public observational play can update descriptive counts and flag distribution shift, not establish causal treatment benefit. A valid private policy is frozen before held-out testing; any online exploration/automated interventions remain in permitted private environments. Version every anchor, server profile, policy and feature schema.

### Technique-specific state
| Technique | Anchor and controllable variable | Required outcome distinction |
|---|---|---|
| W/Zest manual tapping | Observed attack generation plus forward release/repress; actual consumed samples | Unsampled input vs sampled state change vs C0B order vs server sprint at attack |
| Block-hit | Actual use begin/end and attack-generation branches | No attack generated vs generated/rejected; defensive mitigation vs sprint/spacing cost |
| Hit-select/midtrade | Named incoming receive/apply event and counterattack generation | Delayed cue vs actual server attack order; both victims' immunity; range and relative motion |
| Combo maintenance | Sequence of accepted exchanges and relative position/velocity | Excess KB/separation can reduce next-hit probability; maximizing KB is not equivalent to winning |

For a richer geometric model use relative state r=p_opponent−p_self, w=v_opponent−v_self. At uncertain horizon tau, r(t+tau)≈r+w*tau+0.5*a_rel*tau². Propagate a set or covariance only within one motion mode; collision/teleport/unknown input broadens it. Attack acceptance is an event through server code, not merely norm(r)<3.0. A favorable window is the set of cue times meeting both registration and spacing criteria with calibrated lower probability bounds. An empty window is a meaningful answer.

Failure conditions: incomplete hooks, multiple producers using one ring, unknown synthetic edits, reconnect/entity reuse, untrusted labels, changed server code, phase/delay nonidentifiability, too few randomized units, policy distribution shift, or overhead above the frame-time budget. Stop cueing rather than invent a narrower interval.

## Recommendation (Deliverables I/J)
Integrate the passive execution analyzer first. Its most useful initial output is “your forward-up was never consumed” or “an attack was not generated while unblocking,” with an inspectable stage trace. Then test broad personalized windows against a genuinely tuned fixed strategy. At 180–300 ms, execution reliability, packet order and spacing are supportable training targets; exact hidden-server-tick cues are not yet supportable.

VelvetG now has tested standalone machinery for censored input-sampling diagnostics, separate local/written sprint observations, stage-aware packet records, loss-aware replay and explicit unknown hit acceptance. Lunar's public configuration does not demonstrate these diagnostics, and the inspected open-source indicator does not implement them. That is a source-supported feature distinction, not proof Lunar lacks undisclosed internals or that VelvetG improves PvP. No system in this investigation has demonstrated a superior combat outcome in a controlled comparison yet.

See [LUNAR.md](LUNAR.md) for product evidence, [INTEGRATION.md](INTEGRATION.md) for exact hook contracts, [EXPERIMENTS.md](EXPERIMENTS.md) for reproducible tests, and [VALIDATION.md](VALIDATION.md) for executed checks and release gates.
