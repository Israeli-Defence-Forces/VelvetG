package me.eldodebug.soar.combatiq;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Passive, opt-in instrumentation for the verified historical client binary.
 * Does not delay/cancel/alter Minecraft input, packets, or gameplay state.
 * On purpose, packet payloads, player names, keys typed in chat, and IDs
 * are never logged. A network hook means an attempted send, not a TCP write.
 *
 * Start with JVM: -Dvelvet.combatiq.enabled=true
 * Default is off. CSV is saved under ~/VelvetG-CombatIQ/.
 */
public final class CombatIQNative {
    private CombatIQNative() {}
    private static final Object[] NO_ARGS = new Object[0];
    private static volatile boolean ENABLED = Boolean.getBoolean("velvet.combatiq.enabled");
    private static final int CAPACITY = 16384;
    private static final long[] TIME = new long[CAPACITY], SEQ = new long[CAPACITY],
        EPOCH = new long[CAPACITY], THREAD = new long[CAPACITY], TICK_ID = new long[CAPACITY];
    private static final int[] ENTITY = new int[CAPACITY], FLAGS = new int[CAPACITY];
    private static final double[] X = new double[CAPACITY], Y = new double[CAPACITY], Z = new double[CAPACITY];
    private static long sequence;
    private static volatile long epoch;
    private static volatile boolean active;
    // Owned exclusively by the thread established by the client tick bridge.
    // Packet producers only touch the bounded ring and volatile publications.
    private static volatile Thread clientThread;
    private static Object previousPlayer;
    private static int previousContext = -1;
    private static Field focus;
    private static Method sprint, sneak, blocking, entityId;
    private static Field grounded;
    private static boolean sprintKnown, lastSprint;
    private static volatile int contextFlags;
    private static volatile long stopAt;
    private static final long DURATION_NS = Math.max(1, Math.min(3600,
        Long.getLong("velvet.combatiq.durationSeconds", 300L))) * 1000000000L;
    private static final long FILE_BYTES = Math.max(65536, Math.min(268435456,
        Long.getLong("velvet.combatiq.maxBytes", 33554432L)));
    private static final int[] KIND = new int[CAPACITY], DATA = new int[CAPACITY];
    private static final float[] MOTION = new float[CAPACITY];
    private static final String[] CLASSNAME = new String[CAPACITY];
    private static final Object GATE = new Object();
    private static volatile long tail, head, dropped, ticks;
    private static volatile boolean running = true;
    private static volatile boolean recordable = true;
    private static volatile boolean capped;
    private static volatile long filtered;
    private static final long MAX_ROWS = Math.max(1, Math.min(150000L, Long.getLong("velvet.combatiq.maxRows", 150000L))); // bounded opt-in recording
    private static volatile String status = "CombatIQ: initializing";
    private static final AtomicBoolean starting = new AtomicBoolean();
    private static final AtomicBoolean shutdownRegistered = new AtomicBoolean();
    private static volatile Thread recorderThread;
    private static volatile boolean startRequested;
    private static long releaseAt;
    private static double lastReleaseMs = Double.NaN;
    private static volatile String lastAction = "No action observation";
    public static final class Snapshot {
        public final String status, sprint, lastRelease, action, health;
        public final long lost;
        Snapshot(String status, String sprint, String lastRelease, String action, long lost) {
            this.status=status; this.sprint=sprint; this.lastRelease=lastRelease;
            this.action=action; this.lost=lost; this.health="Lost: " + lost + " | filtered: " + filtered;
        }
    }
    private static volatile Snapshot snapshot = new Snapshot("CombatIQ off", "Sprint unknown", "Release unknown", "No action observation", 0);
    public static Snapshot snapshot() { return snapshot; }

    /** GUI/client control is asynchronous: never joins or writes files. */
    public static void setEnabled(boolean value) {
        synchronized (GATE) {
            if (value == ENABLED) return;
            ENABLED = value;
            if (value) startRequested = true;
            else {
                startRequested = false; recordable = false; running = false;
                status = "CombatIQ stopping";
                snapshot = new Snapshot("CombatIQ off", "Sprint unknown", "Release unknown", "No action observation", dropped);
            }
        }
    }

    private static void beginRequested() {
        if (!startRequested || starting.get()) return;
        synchronized (GATE) {
            if (!startRequested || starting.get()) return;
            head=tail=sequence=dropped=ticks=stopAt=filtered=0;
            epoch++; previousContext=-1; previousPlayer=null; sprintKnown=false;
            releaseAt=0; lastReleaseMs=Double.NaN; lastAction="No action observation";
            diagnosticMask.set(0); sampleHookSeen.set(false); sampleHookFailure.set(false);
            previousForwardCode=Integer.MIN_VALUE;
            running=true; recordable=true; capped=false; startRequested=false;
        }
    }
    private static final int KEY_UP = 1, KEY_DOWN = 2, SAMPLE = 3, TICK = 4,
        SEND_ATTEMPT = 5, RECEIVE_BEFORE_FILTER = 6, SAMPLE_HOOK_SEEN = 7, SAMPLE_HOOK_FAILURE = 8;
    private static final AtomicInteger diagnosticMask = new AtomicInteger();
    private static final AtomicBoolean sampleHookSeen = new AtomicBoolean();
    private static final AtomicBoolean sampleHookFailure = new AtomicBoolean();
    private static volatile boolean reflectReady;
    private static Method minecraftGet, bindingDown, bindingCode;
    private static Field settings, forward, screen, player, forwardFloat;
    private static int previousForwardCode = Integer.MIN_VALUE;
    private static Method getEventKey, getEventKeyState;

    private static void startup() {
        if (!ENABLED || !starting.compareAndSet(false, true)) return;
        Thread t = new Thread(new Runnable() {
            @Override public void run() { record(); }
        }, "VelvetG-CombatIQ-native-recorder");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        recorderThread=t;
        if (shutdownRegistered.compareAndSet(false, true)) Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                synchronized (GATE) { recordable=false; running = false; }
                Thread writer=recorderThread;
                try { if (writer != null) writer.join(2000L); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }, "VelvetG-CombatIQ-shutdown"));
        t.start();
    }

    private static void add(int kind, int data, float motion, String clazz) {
        emit(kind, data, motion, clazz, -1, contextFlags, Double.NaN, Double.NaN, Double.NaN);
    }

    // Shared bounded MPSC ring retained from v3. The sequence records enqueue
    // order under this lock; it is not a global causal/server order.
    static void emit(int kind, int data, float motion, String clazz,
                     int entity, int flags, double x, double y, double z) {
        if (!ENABLED || !recordable) return;
        try { startup(); }
        catch (Throwable failure) {
            recordable = false; running = false;
            status = "CombatIQ startup failed: " + failure.getClass().getSimpleName();
            return;
        }
        synchronized (GATE) {
            if (!recordable) return;
            long ns = System.nanoTime();
            if (stopAt == 0) stopAt = ns + DURATION_NS;
            if (ns - stopAt >= 0) { capped=true; recordable = false; running = false; return; }
            long ordinal = ++sequence;
            if (head - tail >= CAPACITY) { dropped++; return; }
            int i = (int)head & (CAPACITY - 1);
            TIME[i] = ns; KIND[i] = kind; DATA[i] = data;
            MOTION[i] = motion; CLASSNAME[i] = clazz;
            SEQ[i] = ordinal; EPOCH[i] = epoch; THREAD[i] = Thread.currentThread().getId();
            TICK_ID[i] = ticks; ENTITY[i] = entity; FLAGS[i] = flags;
            X[i] = x; Y[i] = y; Z[i] = z;
            head++;
            if (kind==5 && (data==11 || data==12 || data==13))
                lastAction = data==11 ? "START sprint attempt" : data==12 ? "STOP sprint attempt" : "ATTACK attempt";
        }
    }

    public static void onContextPoint() {
        if (!ENABLED || Thread.currentThread() != clientThread) return;
        try {
            initializeMinecraft();
            Object mc = minecraftGet.invoke(null, NO_ARGS);
            Object local = player.get(mc);
            int flags = (local != null ? 1 : 0) | (screen.get(mc) == null ? 2 : 0)
                | (focus.getBoolean(mc) ? 4 : 0);
            boolean changed = local != previousPlayer;
            boolean boundary = changed || flags != previousContext;
            if (boundary) { epoch++; previousPlayer = local; sprintKnown = false; releaseAt=0; }
            active = flags == 7;
            contextFlags = flags;
            if (boundary) {
                previousContext = flags;
                if (active || starting.get()) add(12, active ? 1 : 0, Float.NaN, null);
            }
        } catch (Throwable failure) { active = false; diagnostic(3, failure); }
    }

    private static void initializeMinecraft() throws Exception {
        if (minecraftGet != null) return;
        Class<?> cl = Class.forName("ave");
        Method method = cl.getDeclaredMethod("A"); method.setAccessible(true);
        Field gui = cl.getDeclaredField("m"); gui.setAccessible(true);
        Field local = cl.getDeclaredField("h"); local.setAccessible(true);
        Field focused = cl.getDeclaredField("w"); focused.setAccessible(true);
        screen = gui; player = local; focus = focused; minecraftGet = method;
    }

    private static void diagnostic(int code, Throwable failure) {
        if (firstDiagnostic(code))
            add(16, code, Float.NaN, failure == null ? null : failure.getClass().getSimpleName());
    }

    public static void onPlayerPoint(Object instance, int stage) {
        if (!ENABLED || !recordable) return;
        // Reject foreign threads/entities BEFORE any mutable context observation.
        if (Thread.currentThread() != clientThread || instance == null) return;
        try {
            initializeMinecraft();
            Object mc = minecraftGet.invoke(null, NO_ARGS);
            if (instance != player.get(mc)) return;
            onContextPoint();
            if (!active) return;
            if (sprint == null) {
                Class<?> owner = Class.forName("pk");
                sprint = owner.getDeclaredMethod("aw");
                sneak = owner.getDeclaredMethod("av");
                entityId = owner.getDeclaredMethod("F");
                grounded = owner.getDeclaredField("C");
                blocking = Class.forName("wn").getDeclaredMethod("bW");
            }
            boolean now = ((Boolean)sprint.invoke(instance, NO_ARGS)).booleanValue();
            int flags = contextFlags | (grounded.getBoolean(instance) ? 8 : 0)
                | (((Boolean)sneak.invoke(instance, NO_ARGS)).booleanValue() ? 16 : 0)
                | (((Boolean)blocking.invoke(instance, NO_ARGS)).booleanValue() ? 32 : 0)
                | (sprintKnown ? 64 : 0) | (lastSprint ? 128 : 0);
            // kind 9 records changes at observation points; 10/11 are actual
            // setSprinting HEAD/RETURN observations, including no-op setters.
            int kind = stage == 1 ? 10 : stage == 2 ? 11 : 9;
            if (kind != 9 || !sprintKnown || now != lastSprint)
                emit(kind, now ? 1 : 0, Float.NaN, null,
                    ((Integer)entityId.invoke(instance, NO_ARGS)).intValue(), flags,
                    stage, Double.NaN, Double.NaN);
            lastSprint = now; sprintKnown = true;
        } catch (Throwable failure) { diagnostic(4, failure); }
    }

    public static void onAttackIntent() {
        if (!ENABLED) return;
        if (Thread.currentThread() != clientThread) return;
        onContextPoint();
        if (active) add(13, 0, Float.NaN, null);
    }
    public static void onUseIntent() {
        if (!ENABLED) return;
        if (Thread.currentThread() != clientThread) return;
        onContextPoint();
        if (active) add(14, 0, Float.NaN, null);
    }
    public static void onHandlerReturn(Object packet) {
        if (ENABLED && recordable && active) safeCapture(packet, 15);
    }

    /** Keyboard event proxy for the current forward binding; never record chat. */
    public static void onKeyboardDispatch() {
        if (!ENABLED || !gameplay()) return;
        try {
            if (getEventKey == null) {
                Class<?> keyboard = Class.forName("org.lwjgl.input.Keyboard");
                getEventKey = keyboard.getMethod("getEventKey");
                getEventKeyState = keyboard.getMethod("getEventKeyState");
            }
            int key = ((Integer)getEventKey.invoke(null, NO_ARGS)).intValue();
            initializeBinding();
            Object binding = forward.get(settings.get(minecraftGet.invoke(null, NO_ARGS)));
            int code = ((Integer)bindingCode.invoke(binding, NO_ARGS)).intValue();
            if (previousForwardCode != Integer.MIN_VALUE && code != previousForwardCode) {
                epoch++; sprintKnown = false;
                add(12, active ? 1 : 0, Float.NaN, null);
            }
            previousForwardCode = code;
            // Negative codes are mouse bindings, not keyboard events. Abstain.
            if (code <= 0 || key != code) return;
            boolean down=((Boolean)getEventKeyState.invoke(null, NO_ARGS)).booleanValue();
            if (!down && releaseAt==0) releaseAt=System.nanoTime();
            else if (down && releaseAt!=0) {
                lastReleaseMs=(System.nanoTime()-releaseAt)/1000000.0; releaseAt=0;
            }
            add(down ? KEY_DOWN : KEY_UP, key, Float.NaN, null);
        } catch (Throwable failure) { diagnostic(7, failure); }
    }

    /** Called after the original SnapTap movement callback. Receives the target's this.
     *  This class inherits moveForward ("b") from its MovementInput superclass.
     *  The v2 code used getDeclaredField on the subclass, which silently failed.
     */
    public static void onMovementSample(Object movementInput) {
        if (!ENABLED || !gameplay()) return;
        if (sampleHookSeen.compareAndSet(false, true))
            add(SAMPLE_HOOK_SEEN, 1, Float.NaN, null);
        try {
            initReflection(movementInput);
            float sampled = forwardFloat.getFloat(movementInput);
            Object mc = minecraftGet.invoke(null, NO_ARGS);
            Object options = settings.get(mc);
            Object key = forward.get(options);
            boolean down = ((Boolean)bindingDown.invoke(key, NO_ARGS)).booleanValue();
            add(SAMPLE, down ? 1 : 0, sampled, null);
        } catch (Throwable failure) {
            // Emit one minimal diagnostic per session rather than silently hiding a hook bug.
            if (sampleHookFailure.compareAndSet(false, true))
                add(SAMPLE_HOOK_FAILURE, reflectReady ? 2 : 1, Float.NaN,
                    failure.getClass().getSimpleName());
        }
    }

    public static void onClientTick() {
        // This bootstrap must only be called by the Minecraft client-tick bridge.
        if (clientThread == null) {
            synchronized (GATE) {
                if (clientThread == null) clientThread = Thread.currentThread();
            }
        }
        if (Thread.currentThread() != clientThread || !ENABLED) return;
        beginRequested();
        onContextPoint();
        snapshot = new Snapshot(status, sprintKnown ? (lastSprint ? "Sprint on" : "Sprint off") : "Sprint unknown",
            Double.isNaN(lastReleaseMs) ? "Release unknown" : "Last release " + Math.round(lastReleaseMs) + " ms",
            lastAction, dropped);
        if (!active || !recordable) return;
        ++ticks;
        try { onPlayerPoint(player.get(minecraftGet.invoke(null, NO_ARGS)), 0); }
        catch (Throwable failure) { diagnostic(4, failure); }
        add(TICK, (int)(ticks & 0x7fffffff), Float.NaN, null);
    }

    /** Pre-send callback can be cancelled later; explicitly NOT a confirmed socket write. */
    public static void onSendAttempt(Object packet) {
        if (!ENABLED || !recordable || packet == null || !active) return;
        safeCapture(packet, SEND_ATTEMPT);
    }
    /** Pre-filter receive; packet may be cancelled before it reaches the main thread. */
    public static void onInboundBeforeFilter(Object packet) {
        if (!ENABLED || !recordable || packet == null || !active) return;
        safeCapture(packet, RECEIVE_BEFORE_FILTER);
    }

    private static void safeCapture(Object packet, int stage) {
        try { PacketProbe.capture(packet, stage); }
        catch (Throwable failure) { diagnostic(5, failure); }
    }

    private static boolean gameplay() {
        if (!ENABLED || !recordable || Thread.currentThread() != clientThread) return false;
        onContextPoint();
        return active;
    }
    private static void initializeBinding() throws Exception {
        if (bindingCode != null) return;
        Object mc = minecraftGet.invoke(null, NO_ARGS);
        Field options = mc.getClass().getDeclaredField("t"); options.setAccessible(true);
        Object opts = options.get(mc);
        Field key = opts.getClass().getDeclaredField("Y"); key.setAccessible(true);
        Object kb = key.get(opts);
        Method down = kb.getClass().getDeclaredMethod("d"); down.setAccessible(true);
        Method code = kb.getClass().getDeclaredMethod("i"); code.setAccessible(true);
        if (down.getReturnType() != boolean.class || code.getReturnType() != int.class)
            throw new NoSuchMethodException("binding signatures");
        settings = options; forward = key; bindingDown = down; bindingCode = code;
    }

    private static void initReflection(Object input) throws Exception {
        if (reflectReady) return;
        initializeBinding();
        Field mf = inheritedField(input.getClass(), "b");
        if (mf.getType() != float.class) throw new NoSuchFieldException("float movement b");
        forwardFloat = mf;
        reflectReady = true;
    }

    private static Field inheritedField(Class<?> owner, String name) throws NoSuchFieldException {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException notHere) { /* try parent */ }
        }
        throw new NoSuchFieldException(name);
    }

    private static void record() {
        BufferedWriter out = null;
        try {
            Path folder = Paths.get(System.getProperty("velvet.combatiq.exportDir",
                Paths.get(System.getProperty("user.home"), "VelvetG-CombatIQ").toString()));
            Files.createDirectories(folder);
            String when = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
            Path csv = folder.resolve("combatiq-native-" + when + ".csv");
            out = Files.newBufferedWriter(csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            out.write("# CombatIQ native experimental schema 2; packet hooks are NOT server acceptance or actual socket writes\n");
            out.write("ns,kind,data,moveForward,packetClass,tick,epoch,thread,sequence,entity,flags,x,y,z\n");
            status = "CombatIQ recording " + csv.getFileName();
            long lastFlush = System.nanoTime();
            long rows = 0L, bytes = 0L;
            while (running || head != tail) {
                if (stopAt != 0 && System.nanoTime() - stopAt >= 0) {
                    capped=true; recordable = false; running = false;
                }
                long timestamp, order = 0, ep = 0, thread = 0, tick = 0;
                int type, value, entity = -1, flags = 0; float motion; String name;
                double x = Double.NaN, y = Double.NaN, z = Double.NaN;
                synchronized (GATE) {
                    if (tail == head) {
                        timestamp = -1; type = value = 0; motion = Float.NaN; name = null;
                    } else {
                        int i = (int)tail & (CAPACITY - 1);
                        timestamp = TIME[i]; type = KIND[i]; value = DATA[i];
                        motion = MOTION[i]; name = CLASSNAME[i]; CLASSNAME[i] = null;
                        order = SEQ[i]; ep = EPOCH[i]; thread = THREAD[i]; tick = TICK_ID[i];
                        entity = ENTITY[i]; flags = FLAGS[i]; x = X[i]; y = Y[i]; z = Z[i];
                        tail++;
                    }
                }
                if (timestamp >= 0) {
                    String row = timestamp + "," + type + "," + value + "," + motion + ","
                        + (name == null ? "" : name) + "," + tick + "," + ep + "," + thread
                        + "," + order + "," + entity + "," + flags + "," + x + "," + y + "," + z + "\n";
                    int rowBytes=row.getBytes(StandardCharsets.UTF_8).length;
                    if (bytes + rowBytes > FILE_BYTES - 512) {
                        capped=true; recordable = false; running = false;
                        synchronized (GATE) { dropped += head - tail + 1; tail = head; }
                        break;
                    }
                    out.write(row); bytes += rowBytes;
                    rows++;
                    if (rows >= MAX_ROWS) {
                        capped=true; recordable = false;
                        running = false;
                        status = "CombatIQ recording capped; restart for a new capture";
                        synchronized (GATE) { dropped += head - tail; tail = head; }
                        break;
                    }
                } else {
                    try { Thread.sleep(12); } catch (InterruptedException ignored) { }
                }
                long now = System.nanoTime();
                if (now - lastFlush > 1000000000L) {
                    out.flush(); lastFlush = now;
                    status = "CombatIQ ticks=" + ticks + " dropped=" + dropped;
                }
            }
            out.write("# LOSS," + dropped + "\n");
            long filteredCount;
            synchronized (GATE) { filteredCount=filtered; }
            out.write("# FILTERED," + filteredCount + "\n");
            if (capped) out.write("# CAPPED recorder row/duration/size limit reached\n");
            out.write("# END\n");
            out.flush();
            status = capped ? "CombatIQ capped; toggle recording for a new capture" : "CombatIQ stopped cleanly";
        } catch (Throwable ex) {
            status = "CombatIQ recorder stopped: " + ex.getClass().getSimpleName();
        } finally {
            running = false; recordable = false;
            if (out != null) try { out.close(); } catch (IOException ignored) {}
            starting.set(false);
        }
    }

    public static void recordFilteredPacket() {
        synchronized (GATE) { if (ENABLED && recordable) filtered++; }
    }

    private static boolean firstDiagnostic(int code) {
        int bit=1 << Math.max(0, Math.min(30, code));
        int old;
        do {
            old=diagnosticMask.get();
            if ((old & bit) != 0) return false;
        } while (!diagnosticMask.compareAndSet(old,old | bit));
        return true;
    }

    public static void reportPacketFailure(int code) { diagnostic(code, null); }

    public static String status() { return status; }
    public static boolean enabled() { return ENABLED; }
}

