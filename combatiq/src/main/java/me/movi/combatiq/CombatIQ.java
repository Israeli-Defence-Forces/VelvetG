package me.movi.combatiq;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Passive, dependency-free telemetry kernel. No Minecraft/network/input mutation.
 * Timestamp stages describe observations, never physical switch or server time.
 */
public final class CombatIQ {
    private CombatIQ() {}
    public static final int MAIN = 0, NETWORK = 1, SERVER = 2;
    public static final int INPUT_QUEUE = 1, BINDING = 2, SAMPLE = 3,
            INTENT = 4, WRITE = 5, RECEIVE = 6, APPLY = 7, GROUND_TRUTH = 8;
    public static final int CONTEXT = 1, FORWARD_EDGE = 2, MOVEMENT_SAMPLE = 3,
            LOCAL_SPRINT = 4, SPRINT_START = 5, SPRINT_STOP = 6, ATTACK = 7,
            USE_START = 8, USE_END = 9, HURT = 10, VELOCITY = 11,
            POSITION = 12, FRAME = 13, BINDING_CHANGE = 14,
            SERVER_ACCEPT = 15, TICK = 16, CORRECTION = 17;
    public static final int DOWN = 1, FOCUSED = 2, IN_WORLD = 4, GAMEPLAY = 8,
            BINDING_DOWN = 16, SYNTHETIC = 32, MODIFIED_INPUT = 64;
    public static final long UNKNOWN_SOURCE_TIME = Long.MIN_VALUE;
    public static final String HEADER =
            "ns,source_ns,session,producer,sequence,correlation,type,stage,tick,entity,flags,x,y,z";

    /** Consumer-owned reusable carrier. Never retain it after a poll/accept callback. */
    public static final class Event {
        public long ns, sourceNs, session, sequence, correlation;
        public int producer, type, stage, tick, entity, flags;
        public double x, y, z;
    }

    private static final class Gate { volatile boolean accepting = true; }

    /** SPSC ring. One owning producer and one owning consumer; ownership is checked. */
    public static final class Ring {
        private final int mask, producer;
        private final long[] ns, source, session, seq, correlation;
        private final int[] type, stage, tick, entity, flags;
        private final double[] x, y, z;
        private volatile long head, tail;
        private long nextSequence;
        private volatile long dropped;
        private final AtomicLong producerOwner = new AtomicLong();
        private final AtomicLong consumerOwner = new AtomicLong();
        private final AtomicLong misuse = new AtomicLong();
        private final Gate gate;
        private volatile boolean inOffer;

        public Ring(int capacity, int producer) { this(capacity, producer, new Gate()); }
        private Ring(int capacity, int producer, Gate gate) {
            if (capacity < 2 || capacity > 65536 || (capacity & (capacity - 1)) != 0)
                throw new IllegalArgumentException("capacity must be a power of two in [2,65536]");
            if (producer < MAIN || producer > SERVER) throw new IllegalArgumentException("producer");
            this.producer = producer; this.gate = gate; mask = capacity - 1;
            ns = new long[capacity]; source = new long[capacity];
            session = new long[capacity]; seq = new long[capacity]; correlation = new long[capacity];
            type = new int[capacity]; stage = new int[capacity]; tick = new int[capacity];
            entity = new int[capacity]; flags = new int[capacity];
            x = new double[capacity]; y = new double[capacity]; z = new double[capacity];
        }

        private boolean owns(AtomicLong owner) {
            long id = Thread.currentThread().getId(), current = owner.get();
            return current == id || (current == 0 && owner.compareAndSet(0, id));
        }

        /**
         * No allocations or clock reads here. Pass ns from the actual observation hook.
         * sourceNs is an optional timestamp in a separately documented clock domain.
         * Full buffers drop the new record; sequence gaps and a loss footer expose it.
         */
        public boolean offer(long atNs, long sourceNs, long sessionId, long token,
                             int eventType, int eventStage, int clientTick, int entityId,
                             int eventFlags, double a, double b, double c) {
            if (!gate.accepting) return false;
            if (!owns(producerOwner)) { misuse.incrementAndGet(); return false; }
            inOffer = true;
            try {
                if (!gate.accepting) return false;
                long sequence = ++nextSequence, h = head;
                if (h - tail >= ns.length) { dropped++; return false; }
                int i = (int) h & mask;
                ns[i] = atNs; source[i] = sourceNs; session[i] = sessionId;
                seq[i] = sequence; correlation[i] = token;
                type[i] = eventType; stage[i] = eventStage; tick[i] = clientTick;
                entity[i] = entityId; flags[i] = eventFlags;
                x[i] = a; y[i] = b; z[i] = c;
                head = h + 1; // volatile release publishes the complete record
                return true;
            } finally { inOffer = false; }
        }

        public boolean poll(Event out) {
            if (!owns(consumerOwner)) { misuse.incrementAndGet(); return false; }
            long t = tail;
            if (t == head) return false; // volatile acquire before payload reads
            int i = (int) t & mask;
            out.ns = ns[i]; out.sourceNs = source[i]; out.session = session[i];
            out.sequence = seq[i]; out.correlation = correlation[i]; out.producer = producer;
            out.type = type[i]; out.stage = stage[i]; out.tick = tick[i];
            out.entity = entity[i]; out.flags = flags[i];
            out.x = x[i]; out.y = y[i]; out.z = z[i];
            tail = t + 1;
            return true;
        }
        public long dropped() { return dropped; }
        public long misuse() { return misuse.get(); }
        public boolean isEmpty() { return head == tail; }
        private boolean quiescent() { return !inOffer; }
    }

    /** Descriptive local analyzer. It intentionally makes no public-server hit claims. */
    public static final class Analyzer {
        private final boolean trustServerTruth;
        private final long[] previousSequence = new long[3];
        private final long[] previousNs = new long[3];
        private final boolean[] hasPrevious = new boolean[3];
        private boolean context, releaseOpen, releaseSampled, rawKnown, rawDown;
        private boolean localSprintKnown, localSprint, writtenSprintKnown, writtenSprint;
        private boolean complete = true;
        private long session = Long.MIN_VALUE, releaseAt;
        private long unsampled, sampled, censored, modifiedSamples, attackIntents, attackWrites;
        private long hurtReceives, hurtApplies, velocityReceives, useWrites, confirmedHits, gaps;
        private long lastFrameNs, lastFrameInterval;
        private boolean hasFrame;
        private long latestObservationNs;
        public Analyzer() { this(false); }
        public Analyzer(boolean trustServerTruth) { this.trustServerTruth = trustServerTruth; }

        private void censor() {
            if (releaseOpen) censored++;
            releaseOpen = false; rawKnown = false;
        }

        /** Close a recording boundary without inventing a matching press. Idempotent. */
        public void finish() { censor(); }

        public void invalidate() {
            complete = false; censor(); writtenSprintKnown = false;
        }

        public void accept(Event e) {
            if (e.producer < 0 || e.producer > 2) { invalidate(); return; }
            int p = e.producer;
            if (hasPrevious[p] && (e.sequence != previousSequence[p] + 1
                    || e.ns - previousNs[p] < 0)) {
                gaps++; invalidate();
            } else if (!hasPrevious[p] && e.sequence != 1) {
                gaps++; invalidate();
            }
            previousSequence[p] = e.sequence; previousNs[p] = e.ns; hasPrevious[p] = true;
            latestObservationNs = e.ns;
            if (p == MAIN && e.type == CONTEXT) {
                boolean next = (e.flags & (FOCUSED | IN_WORLD | GAMEPLAY))
                        == (FOCUSED | IN_WORLD | GAMEPLAY);
                if (e.session != session || next != context) {
                    censor(); localSprintKnown = false; writtenSprintKnown = false;
                    hasFrame = false;
                }
                session = e.session; context = next;
                return;
            }
            if (e.session != session) return; // retain in log; don't join across connections
            if (p == MAIN) {
                if (e.type == FORWARD_EDGE && e.stage == INPUT_QUEUE && context && complete) {
                    if ((e.flags & SYNTHETIC) != 0) { censor(); return; }
                    boolean down = (e.flags & DOWN) != 0;
                    if (rawKnown && rawDown == down) return; // repeat, not a fresh edge
                    rawKnown = true; rawDown = down;
                    if (!down) {
                        releaseOpen = true; releaseSampled = false; releaseAt = e.ns;
                    } else if (releaseOpen) {
                        if (e.ns - releaseAt < 0) { invalidate(); return; }
                        if (releaseSampled) sampled++; else unsampled++;
                        releaseOpen = false;
                    }
                } else if (e.type == MOVEMENT_SAMPLE && e.stage == SAMPLE) {
                    if ((e.flags & MODIFIED_INPUT) != 0) modifiedSamples++;
                    if (releaseOpen && context && complete && (e.flags & BINDING_DOWN) == 0)
                        releaseSampled = true;
                    // moveForward == 0 is NOT evidence that W was released.
                } else if (e.type == BINDING_CHANGE && (e.flags & SYNTHETIC) != 0) {
                    censor();
                } else if (e.type == LOCAL_SPRINT && e.stage == SAMPLE) {
                    localSprintKnown = true; localSprint = (e.flags & DOWN) != 0;
                } else if (e.type == ATTACK && e.stage == INTENT) attackIntents++;
                else if (e.type == FRAME) {
                    if (hasFrame) lastFrameInterval = e.ns - lastFrameNs;
                    lastFrameNs = e.ns; hasFrame = true;
                } else if (e.type == CORRECTION) censor();
            }
            if (p == NETWORK && e.stage == WRITE) {
                if (e.type == SPRINT_START || e.type == SPRINT_STOP) {
                    writtenSprintKnown = true; writtenSprint = e.type == SPRINT_START;
                } else if (e.type == ATTACK) attackWrites++;
                else if (e.type == USE_START || e.type == USE_END) useWrites++;
            }
            if (e.type == HURT && e.stage == RECEIVE) hurtReceives++;
            if (e.type == HURT && e.stage == APPLY) hurtApplies++;
            if (e.type == VELOCITY && e.stage == RECEIVE) velocityReceives++;
            if (trustServerTruth && p == SERVER && e.type == SERVER_ACCEPT
                    && e.stage == GROUND_TRUTH) confirmedHits++;
        }

        public Snapshot snapshot() {
            return new Snapshot(complete, unsampled, sampled, censored, modifiedSamples,
                    attackIntents, attackWrites, hurtReceives, hurtApplies, velocityReceives,
                    useWrites, confirmedHits, gaps, localSprintKnown, localSprint,
                    writtenSprintKnown, writtenSprint, lastFrameInterval, latestObservationNs);
        }
    }

    /** Immutable, worker-published HUD state; construction/formatting stays off capture threads. */
    public static final class Snapshot {
        public final boolean complete, localSprintKnown, localSprint,
                writtenSprintKnown, writtenSprint;
        public final long unsampledReleases, sampledReleases, censoredReleases,
                modifiedSamples, attackIntents, attackWrites, hurtReceives, hurtApplies,
                velocityReceives, useWrites, confirmedHits, gaps, frameIntervalNs, observationNs;
        public final String hudText;
        private Snapshot(boolean complete, long unsampled, long sampled, long censored,
                         long modified, long intents, long writes, long hurtRx, long hurtApply,
                         long velocity, long uses, long confirmed, long gaps,
                         boolean localKnown, boolean local, boolean writtenKnown, boolean written,
                         long frame, long at) {
            this.complete = complete; unsampledReleases = unsampled; sampledReleases = sampled;
            censoredReleases = censored; modifiedSamples = modified;
            attackIntents = intents; attackWrites = writes;
            hurtReceives = hurtRx; hurtApplies = hurtApply; velocityReceives = velocity;
            useWrites = uses; confirmedHits = confirmed; this.gaps = gaps;
            localSprintKnown = localKnown; localSprint = local;
            writtenSprintKnown = writtenKnown; writtenSprint = written;
            frameIntervalNs = frame; observationNs = at;
            hudText = complete
                    ? "CombatIQ | unsampled " + unsampled + " | sampled " + sampled
                      + " | attack writes " + writes + " | server acceptance unknown"
                    : "CombatIQ | incomplete telemetry; timing verdicts suspended";
        }
    }

    public static void writeEvent(Writer out, Event e) throws IOException {
        out.write(e.ns + "," + e.sourceNs + "," + e.session + "," + e.producer + ","
                + e.sequence + "," + e.correlation + "," + e.type + "," + e.stage + ","
                + e.tick + "," + e.entity + "," + e.flags + ","
                + e.x + "," + e.y + "," + e.z + "\n");
    }

    /** Strict schema/version, explicit loss trailer, and conservative truncated-log handling. */
    public static Snapshot replay(Reader input, boolean trustServerTruth) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        if (!"# CombatIQ schema=1".equals(reader.readLine())
                || !HEADER.equals(reader.readLine())) throw new IOException("Unsupported telemetry schema");
        Analyzer a = new Analyzer(trustServerTruth);
        Event e = new Event();
        String line; boolean ended = false, lossSeen = false;
        while ((line = reader.readLine()) != null) {
            if (line.length() > 2048) throw new IOException("Oversized telemetry row");
            if (line.startsWith("# LOSS,")) {
                if (lossSeen) throw new IOException("Repeated loss trailer");
                lossSeen = true;
                String[] f = line.split(",", -1);
                if (f.length != 5) throw new IOException("Malformed loss trailer");
                try {
                    for (int i = 1; i < 5; i++) {
                        long n = Long.parseLong(f[i]);
                        if (n < 0) throw new NumberFormatException("negative loss");
                        if (n != 0) a.invalidate();
                    }
                } catch (NumberFormatException ex) { throw new IOException("Malformed loss count", ex); }
                continue;
            }
            if ("# END".equals(line)) { ended = true; break; }
            if (lossSeen) throw new IOException("Event after loss trailer");
            if (line.startsWith("#")) throw new IOException("Unknown telemetry control row");
            String[] f = line.split(",", -1);
            if (f.length != 14) throw new IOException("Malformed telemetry row");
            try {
                e.ns = Long.parseLong(f[0]); e.sourceNs = Long.parseLong(f[1]);
                e.session = Long.parseLong(f[2]); e.producer = Integer.parseInt(f[3]);
                e.sequence = Long.parseLong(f[4]); e.correlation = Long.parseLong(f[5]);
                e.type = Integer.parseInt(f[6]); e.stage = Integer.parseInt(f[7]);
                e.tick = Integer.parseInt(f[8]); e.entity = Integer.parseInt(f[9]);
                e.flags = Integer.parseInt(f[10]); e.x = Double.parseDouble(f[11]);
                e.y = Double.parseDouble(f[12]); e.z = Double.parseDouble(f[13]);
                if (!Double.isFinite(e.x) || !Double.isFinite(e.y) || !Double.isFinite(e.z))
                    throw new NumberFormatException("non-finite payload");
                if (e.producer < MAIN || e.producer > SERVER || e.sequence <= 0
                        || e.type < CONTEXT || e.type > CORRECTION
                        || e.stage < INPUT_QUEUE || e.stage > GROUND_TRUTH
                        || e.flags < 0 || (e.flags & ~127) != 0)
                    throw new NumberFormatException("unknown event identifier");
            } catch (NumberFormatException ex) { throw new IOException("Invalid telemetry value", ex); }
            a.accept(e);
        }
        if (!ended || !lossSeen) a.invalidate();
        else if (reader.readLine() != null) throw new IOException("Data after END");
        a.finish();
        return a.snapshot();
    }

    /**
     * Optional async CSV recording. Hooks offer only primitives. One writer owns both
     * consumers and the Analyzer. Drain order is recorded, not asserted global causality.
     * Call stop() on the game thread; awaitStopped() only from a non-game shutdown/tool thread.
     */
    public static final class Session implements AutoCloseable {
        private final Gate gate = new Gate();
        public final Ring main, network;
        private final Thread worker;
        private volatile Snapshot snapshot = new Analyzer().snapshot();
        private volatile IOException failure;
        private volatile boolean stopped;
        public Session(final Path output, int capacity) {
            main = new Ring(capacity, MAIN, gate); network = new Ring(capacity, NETWORK, gate);
            worker = new Thread(new Runnable() {
                public void run() { record(output); }
            }, "VelvetG-CombatIQ-recorder");
            worker.setDaemon(true); worker.start();
        }
        public boolean isAccepting() { return gate.accepting; }
        public Snapshot snapshot() { return snapshot; }
        public IOException failure() { return failure; }
        public boolean isStopped() { return stopped; }
        public void stop() { gate.accepting = false; LockSupport.unpark(worker); }
        private void record(Path path) {
            Analyzer analyzer = new Analyzer(); Event event = new Event();
            long lastPublish = System.nanoTime();
            try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                out.write("# CombatIQ schema=1\n"); out.write(HEADER); out.write('\n');
                while (gate.accepting || !main.quiescent() || !network.quiescent()
                        || !main.isEmpty() || !network.isEmpty()) {
                    int count = 0;
                    for (int i = 0; i < 128 && main.poll(event); i++) {
                        analyzer.accept(event); writeEvent(out, event); count++;
                    }
                    for (int i = 0; i < 128 && network.poll(event); i++) {
                        analyzer.accept(event); writeEvent(out, event); count++;
                    }
                    if (main.dropped() + network.dropped() + main.misuse() + network.misuse() > 0)
                        analyzer.invalidate();
                    long now = System.nanoTime();
                    if (now - lastPublish >= 100000000L) {
                        snapshot = analyzer.snapshot(); lastPublish = now;
                    }
                    if (count == 0) LockSupport.parkNanos(1000000L);
                }
                if (main.dropped() + network.dropped() + main.misuse() + network.misuse() > 0)
                    analyzer.invalidate();
                analyzer.finish();
                out.write("# LOSS," + main.dropped() + "," + network.dropped() + ","
                        + main.misuse() + "," + network.misuse() + "\n# END\n");
                snapshot = analyzer.snapshot();
            } catch (IOException ex) { failure = ex; gate.accepting = false; }
            catch (RuntimeException ex) {
                failure = new IOException("Recorder failed", ex); gate.accepting = false;
            } finally { stopped = true; }
        }
        public void awaitStopped(long timeoutMs) throws IOException, InterruptedException {
            if (timeoutMs <= 0) throw new IllegalArgumentException("positive timeout required");
            worker.join(timeoutMs);
            if (worker.isAlive()) throw new IOException("Recorder stop timed out");
            if (failure != null) throw failure;
        }
        public void close() { stop(); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2
                || (args.length == 2 && !"--trusted-server-trace".equals(args[1])))
            throw new IllegalArgumentException("Usage: CombatIQ recording.csv [--trusted-server-trace]");
        try (Reader in = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            Snapshot s = replay(in, args.length == 2);
            System.out.println(s.hudText);
            System.out.println("complete=" + s.complete + " censored=" + s.censoredReleases
                    + " hurtReceives=" + s.hurtReceives + " confirmedHits=" + s.confirmedHits);
        }
    }
}
