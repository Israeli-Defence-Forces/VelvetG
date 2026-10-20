# CombatIQ Phase III native integration contract

Status: **kernel work in progress; no instrumented Minecraft client**.

## Exact target and non-goals
VelvetG Lean Overhaul3's documented source baseline is GlideClient `2.0.9.6-lean-fixed-20261016`, SHA-256 `3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813`.
The repository's installer does not contain that proprietary/local baseline. **Do not create an installable bundle without a hash-matched collected JAR and the patching workspace.**
CombatIQ's `combatiq-kernel.jar` is a standalone Java 8 telemetry library, not a Forge `@Mod` or drop-in GlideTweaker extension.

## Event hooks in source order
- `Minecraft.runTick`: actual keyboard/mouse queue-drain observation (not physical device action time), client tick and focus/GUI context.
- `MovementInputFromOptions.updatePlayerMoveState`: sampled W binding and effective forward movement (these are different).
- `EntityPlayerSP.onLivingUpdate`: observed local sprint transition and item-use scaling.
- `PlayerControllerMP.attackEntity`: attempted C02 attack (intent, not damage acceptance).
- Actual Netty outbound write path: C0B sprint, C02 attack and use/release-use packets, capture their connection-local order.
- Actual Netty inbound path **before** `PacketThreadUtil` main-thread handoff: entity velocity, entity status, movement, teleport and correction RECEIVE.
- Main thread **after** packet handoff: APPLY. Do not double-count RECEIVE on re-entrance.
- Rendering thread: read only immutable `Session.snapshot().hudText`; no synchronous disk writes or full inference.

## Queue ownership, lifecycle and missing proof
`Session.main`, `Session.network`, and `Session.inbound` are three independent SPSC rings. Their owning Java threads must be audited in the actual client. Outgoing Netty writes can occur on an event loop different from the main-thread intent source; incoming handler may run on another event loop or change on reconnect. **If more than one producer can call any ring, introduce per-channel producer rings or a proper MPSC ingress adapter.** Merely adding a third ring does not make arbitrarily many inbound event loops safe.

Establish unique session epochs for each connection, world change, and entity-id reset. Mark synthetic SnapTap/input patches explicitly, censor GUI/focus/rebind transitions, and cancel conclusions on any lost events.
Wire events using the exact hooks from bytecode signatures of the verified baseline; do not assume MCP deobfuscated names exist inside the collected binary.
The CSV's producer-order and timestamps are observations, not globally ordered server causality.

## Side-by-side release gates
1. Exact input JAR SHA and version gate; unknown binaries are refused.
2. Preserve original jar and launcher metadata. Stage new jar and a separate launcher profile/coordinates.
3. Verify modified bytecode, Java class major version 52, unchanged baseline entries and reversible backups.
4. Run Java 8 standalone tests and deterministic native hook tests.
5. Validate Windows Java 8 Minecraft 1.8.9 startup, packet semantics, W-tap sampling, SnapTap interaction and no altered packet contents.
6. Benchmark paired p95/p99/p99.9 frametimes (CombatIQ enabled and disabled) on user hardware.
7. Restrict public-server behavior to approved passive diagnostics; private controlled server only for automation/ground truth.

**Blocked:** the exact baseline JAR and a Windows Minecraft runtime are not in this workspace. No installer, native module, HUD patch or runtime performance claim has been produced.
