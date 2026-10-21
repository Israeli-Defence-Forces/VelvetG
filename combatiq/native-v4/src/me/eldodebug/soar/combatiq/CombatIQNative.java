package me.eldodebug.soar.combatiq;

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
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Passive, opt-in instrumentation for the verified historical client binary.
 * Does not delay/cancel/alter Minecraft input, packets, or gameplay state.
 * Only allowlisted numeric packet fields and temporary entity IDs are logged.
 * Player names, chat text and credentials are never recorded. A network hook means an attempted send, not a TCP write.
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
    private static final LiveDiagnostics live=new LiveDiagnostics();
    private static volatile int analyzerMask=15, detailLevel=1;
    private static volatile boolean rotateFiles=Boolean.getBoolean("velvet.combatiq.rotate"),nextRotate=rotateFiles;
    public static void setRotation(boolean rotate){nextRotate=rotate;}
    private static volatile long nextDurationSeconds=Math.max(1,Math.min(3600,Long.getLong("velvet.combatiq.durationSeconds",300L))),
        nextMaxBytes=Math.max(65536,Math.min(268435456,Long.getLong("velvet.combatiq.maxBytes",33554432L)));
    private static long publishAt;
    private static int lastPlayerFlags=-1;
    private static volatile int targetId=-1;
    private static Object previousWorld;
    private static final class Connection {
        final Object manager; final long id;
        Connection(Object manager,long id){this.manager=manager;this.id=id;}
    }
    private static volatile Connection connection=new Connection(null,0);
    private static long connectionCounter, packetCounter;
    private static final long[] feedbackPackets=new long[512],feedbackEpochs=new long[512];
    private static final Object[] packetObjects=new Object[512];
    private static final long[] packetIds=new long[512];
    private static final long[] CONNECTION=new long[CAPACITY], PACKET_ID=new long[CAPACITY];
    private static final int[] PHASE=new int[CAPACITY];
    private static Method currentHandler, networkManager, worldEntity;
    private static Field world, strafeFloat;
    private static Field[] velocity, position;
    private static boolean kinematicsKnown;
    private static final double[] previousVelocity=new double[3], previousPosition=new double[3];
    private static final AtomicBoolean exporting=new AtomicBoolean();
    private static volatile Thread exportThread;
    private static volatile String lastExport="No report exported";
    private static volatile String exportDirectory=System.getProperty("velvet.combatiq.exportDir",
        Paths.get(System.getProperty("user.home"),"VelvetG-CombatIQ").toString());
    public static String exportDirectory(){return exportDirectory;}
    public static void setExportDirectory(String directory){
        if(directory!=null&&!directory.equals(exportDirectory)&&!directory.trim().isEmpty())exportDirectory=Paths.get(directory).toString();
    }
    private static String quoted(String text){
        StringBuilder result=new StringBuilder("\"");
        for(int i=0;i<text.length();i++){char c=text.charAt(i);
            if(c=='\\'||c=='\"')result.append('\\').append(c);
            else if(c<32)result.append(String.format("\\u%04x",(int)c));else result.append(c);}
        return result.append('\"').toString();
    }
    /** At most one bounded asynchronous export; never called from rendering. */
    public static boolean requestReportExport(){
        if(!exporting.compareAndSet(false,true))return false;
        final Snapshot report=snapshot;final String directory=exportDirectory;
        try { Thread worker=new Thread(new Runnable(){public void run(){
            try {
                Path folder=Paths.get(directory);Files.createDirectories(folder);
                Path output=folder.resolve("combatiq-report-"+UUID.randomUUID()+".json");
                StringBuilder json=new StringBuilder("{\"server_outcome\":\"UNKNOWN\",\"status\":").append(quoted(report.status))
                    .append(",\"lost_events\":").append(report.lost).append(",\"observations\":[");
                for(int i=0;i<report.diagnostics.size();i++){if(i>0)json.append(',');json.append(quoted(report.diagnostics.get(i)));}
                json.append("]}\n");
                Path partial=output.resolveSibling(output.getFileName()+".partial");
                Files.write(partial,json.toString().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
                Files.move(partial,output);
                lastExport="Exported "+output.getFileName();
            }catch(Throwable failure){lastExport="Export failed: "+failure.getClass().getSimpleName();}
            finally{exporting.set(false);}
        }},"VelvetG-CombatIQ-report-export");
        worker.setDaemon(true);worker.setPriority(Thread.MIN_PRIORITY);exportThread=worker;
            worker.start();return true;}catch(Throwable failure){exporting.set(false);lastExport="Export start failed";return false;}
    }

    /** Limits are applied at the next recording; diagnostic/analyzer choices are live. */
    public static void configure(long seconds,long maxBytes,int detail,int analyzers){
        nextDurationSeconds=Math.max(1,Math.min(3600,seconds));
        nextMaxBytes=Math.max(65536,Math.min(268435456,maxBytes));
        detailLevel=Math.max(0,Math.min(2,detail));analyzerMask=analyzers&15;
    }
    public static int detailLevel(){return detailLevel;}
    public static boolean relevantTarget(int entity){return entity==targetId;}
    public static String lastExport(){return lastExport;}

    // Owned exclusively by the thread established by the client tick bridge.
    // Packet producers only touch the bounded ring and volatile publications.
    private static volatile Thread clientThread;
    private static Object previousPlayer;
    private static int previousContext = -1;
    private static Field focus;
    private static Method sprint, sneak, blocking, entityId;
    private static Field grounded;
    private static boolean sprintKnown, lastSprint;
    private static volatile int localEntityId=-1;
    private static volatile int contextFlags;
    private static volatile long stopAt;
    private static volatile long DURATION_NS = Math.max(1, Math.min(3600,
        Long.getLong("velvet.combatiq.durationSeconds", 300L))) * 1000000000L;
    private static volatile long FILE_BYTES = Math.max(65536, Math.min(268435456,
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
        public final String status, sprint, lastRelease, action, health, transition, compactState;
        public final List<String> diagnostics;
        public final long lost;
        Snapshot(String status, String sprint, String lastRelease, String action, long lost) {
            this.status=status; this.sprint=sprint; this.lastRelease=lastRelease;
            this.action=action; this.lost=lost; this.health="Lost: " + lost + " | filtered: " + filtered;
            this.transition=live.transition();this.compactState=sprint+" | "+lastRelease;
            this.diagnostics=Collections.unmodifiableList(new ArrayList<String>(live.lines(analyzerMask)));
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
            previousForwardCode=Integer.MIN_VALUE;live.reset();publishAt=0;targetId=-1;kinematicsKnown=false;
            java.util.Arrays.fill(packetObjects,null);packetCounter=0;
            DURATION_NS=nextDurationSeconds*1000000000L;FILE_BYTES=nextMaxBytes;rotateFiles=nextRotate;
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
    private static Method getEventKey, getEventKeyState, mouseButton,mouseState;

    private static void startup() {
        if (!ENABLED || !starting.compareAndSet(false, true)) return;
        Thread t = new Thread(new Runnable() {
            @Override public void run() { record(); }
        }, "VelvetG-CombatIQ-native-recorder");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        recorderThread=t;
        if (shutdownRegistered.compareAndSet(false, true)) try { Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                synchronized (GATE) { recordable=false; running = false; }
                Thread writer=recorderThread;
                try { if (writer != null) writer.join(2000L); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                Thread report=exportThread;
                try {if(report!=null)report.join(1000L);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            }
        }, "VelvetG-CombatIQ-shutdown")); }
        catch (Throwable failure) { shutdownRegistered.set(false);starting.set(false);throw failure; }
        try { t.start(); }
        catch (Throwable failure) { recorderThread=null;starting.set(false);throw failure; }
    }

    private static void add(int kind, int data, float motion, String clazz) {
        emit(kind, data, motion, clazz, -1, contextFlags, Double.NaN, Double.NaN, Double.NaN);
    }

    // Shared bounded MPSC ring retained from v3. The sequence records enqueue
    // order under this lock; it is not a global causal/server order.
    static void emit(int kind, int data, float motion, String clazz,
                     int entity, int flags, double x, double y, double z) {
        emitRecord(kind,data,motion,clazz,entity,flags,x,y,z,null,Thread.currentThread()==clientThread?connection.id:0);
    }

    static void emitPacket(int kind,int data,String clazz,int entity,int flags,double x,double y,double z,Object packet,long owner) {
        emitRecord(kind,data,Float.NaN,clazz,entity,flags,x,y,z,packet,owner);
    }

    private static void emitRecord(int kind,int data,float motion,String clazz,int entity,int flags,
                                   double x,double y,double z,Object packet,long owner) {
        if (!ENABLED || !recordable) return;
        try { startup(); }
        catch (Throwable failure) {
            recordable = false; running = false;
            if(recorderThread==null||!recorderThread.isAlive())starting.set(false);
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
            CONNECTION[i]=owner;
            PHASE[i]=kind==10?1:kind==11?2:kind==3?4:kind==4?3:kind==22?data==4?5:6:0;
            if(packet!=null){int slot=System.identityHashCode(packet)&511;
                if(packetObjects[slot]!=packet){packetObjects[slot]=packet;packetIds[slot]=++packetCounter;}
                PACKET_ID[i]=packetIds[slot];
            }else PACKET_ID[i]=0;
            live.observe(kind,data,ns,entity,flags);
            if(kind==5&&data==13)targetId=entity;
            if((kind==6||kind==15)&&localEntityId>=0&&entity==localEntityId&&(data==18||data==19&&x==2)
                &&(PACKET_ID[i]==0||feedbackPackets[(int)PACKET_ID[i]&511]!=PACKET_ID[i]||feedbackEpochs[(int)PACKET_ID[i]&511]!=epoch)){
                live.localFeedback(ns);feedbackPackets[(int)PACKET_ID[i]&511]=PACKET_ID[i];feedbackEpochs[(int)PACKET_ID[i]&511]=epoch;
            }
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
            observeConnection(mc);
            Object currentWorld=world==null?null:world.get(mc);
            boolean changed = local != previousPlayer || currentWorld!=previousWorld;
            previousWorld=currentWorld;
            boolean boundary = changed || flags != previousContext;
            if (boundary) { epoch++; previousPlayer = local; sprintKnown = false; releaseAt=0;kinematicsKnown=false;targetId=-1;localEntityId=-1;lastPlayerFlags=-1; }
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

    private static void observeConnection(Object mc) {
        try {
            if(currentHandler==null){
                Method handler=mc.getClass().getDeclaredMethod("u");handler.setAccessible(true);
                Method manager=Class.forName("bcy").getDeclaredMethod("a");manager.setAccessible(true);
                Field f=mc.getClass().getDeclaredField("f");f.setAccessible(true);
                currentHandler=handler;networkManager=manager;world=f;
            }
            Object handler=currentHandler.invoke(mc,NO_ARGS);
            Object manager=handler==null?null:networkManager.invoke(handler,NO_ARGS);
            if(manager!=connection.manager){
                synchronized(GATE){connection=new Connection(manager,++connectionCounter);epoch++;sprintKnown=false;kinematicsKnown=false;targetId=-1;live.context();}
                add(25,manager==null?0:1,Float.NaN,null);
            }
        }catch(Throwable failure){diagnostic(10,failure);}
    }

    public static void onNetworkPacket(Object manager,Object packet,int stage){
        Connection current=connection;
        if(!ENABLED||!recordable||!active||manager==null||manager!=current.manager)return;
        try { PacketProbe.capture(packet,stage,current.id); }
        catch(Throwable failure){diagnostic(5,failure);}
    }

    public static void onMouseDispatch(){
        if(!gameplay())return;
        try {
            if(mouseButton==null){Class<?> mouse=Class.forName("org.lwjgl.input.Mouse");mouseButton=mouse.getMethod("getEventButton");mouseState=mouse.getMethod("getEventButtonState");}
            onMouseEvent(((Integer)mouseButton.invoke(null,NO_ARGS)).intValue(),((Boolean)mouseState.invoke(null,NO_ARGS)).booleanValue());
        }catch(Throwable failure){diagnostic(13,failure);}
    }

    public static void onMouseEvent(int button,boolean down){
        if(!gameplay()||button<0||button>1)return;
        add(19,button*2+(down?1:0),Float.NaN,null);
    }
    public static void onSwing(Object local){
        if(Thread.currentThread()!=clientThread||local!=previousPlayer||!gameplay())return;
        add(20,0,Float.NaN,null);
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
            localEntityId=((Integer)entityId.invoke(instance,NO_ARGS)).intValue();
            if (kind != 9 || !sprintKnown || now != lastSprint || (flags&63)!=lastPlayerFlags)
                emit(kind, now ? 1 : 0, Float.NaN, null,
                    ((Integer)entityId.invoke(instance, NO_ARGS)).intValue(), flags,
                    stage, Double.NaN, Double.NaN);
            lastSprint = now; sprintKnown = true;lastPlayerFlags=flags&63;
            if(stage==3||stage==4)emit(22,stage,Float.NaN,null,localEntityId,flags|(now?256:0),Double.NaN,Double.NaN,Double.NaN);
            if(stage==0)sampleKinematics(instance,flags|(now?256:0));
        } catch (Throwable failure) { diagnostic(4, failure); }
    }

    private static void sampleKinematics(Object local,int flags){
        try {
            if(velocity==null){Class<?> type=Class.forName("pk");
                Field[] v=new Field[]{inheritedField(type,"v"),inheritedField(type,"w"),inheritedField(type,"x")};
                Field[] p=new Field[]{inheritedField(type,"s"),inheritedField(type,"t"),inheritedField(type,"u")};
                position=p;velocity=v;
            }
            double vx=velocity[0].getDouble(local),vy=velocity[1].getDouble(local),vz=velocity[2].getDouble(local);
            double px=position[0].getDouble(local),py=position[1].getDouble(local),pz=position[2].getDouble(local);
            if(!kinematicsKnown||vx!=previousVelocity[0]||vy!=previousVelocity[1]||vz!=previousVelocity[2])
                emit(17,0,Float.NaN,null,localEntityId,flags,vx,vy,vz);
            if(!kinematicsKnown||px!=previousPosition[0]||py!=previousPosition[1]||pz!=previousPosition[2])
                emit(18,0,Float.NaN,null,localEntityId,flags,px,py,pz);
            previousVelocity[0]=vx;previousVelocity[1]=vy;previousVelocity[2]=vz;
            previousPosition[0]=px;previousPosition[1]=py;previousPosition[2]=pz;kinematicsKnown=true;
            int target=targetId;
            if(target>=0&&(analyzerMask&12)!=0&&world!=null){
                Object w=world.get(minecraftGet.invoke(null,NO_ARGS));
                if(w!=null){if(worldEntity==null){worldEntity=Class.forName("bdb").getDeclaredMethod("a",int.class);worldEntity.setAccessible(true);}
                    Object entity=worldEntity.invoke(w,target);
                    if(entity!=null)emit(21,0,Float.NaN,null,target,0,position[0].getDouble(entity),position[1].getDouble(entity),position[2].getDouble(entity));}
            }
        }catch(Throwable failure){diagnostic(9,failure);}
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
                epoch++; sprintKnown = false;releaseAt=0;
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
            float strafe=strafeFloat==null?Float.NaN:strafeFloat.getFloat(movementInput);
            emit(SAMPLE,down?1:0,sampled,null,-1,contextFlags,strafe,Double.NaN,Double.NaN);
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
                if(clientThread==null)try{
                    Class<?> type=Class.forName("ave");
                    Method get=type.getDeclaredMethod("A"),isClient=type.getDeclaredMethod("aJ");
                    get.setAccessible(true);isClient.setAccessible(true);
                    Object mc=get.invoke(null,NO_ARGS);
                    if(mc==null||!((Boolean)isClient.invoke(mc,NO_ARGS)).booleanValue())return;
                    clientThread=Thread.currentThread();
                }catch(Throwable failure){status="CombatIQ client-thread check unavailable";return;}
            }
        }
        if (Thread.currentThread() != clientThread || !ENABLED) return;
        beginRequested();
        onContextPoint();
        if (!active || !recordable) { publishSnapshot();return; }
        ++ticks;
        try { onPlayerPoint(player.get(minecraftGet.invoke(null, NO_ARGS)), 0); }
        catch (Throwable failure) { diagnostic(4, failure); }
        add(TICK, (int)(ticks & 0x7fffffff), Float.NaN, null);
        publishSnapshot();
    }

    private static void publishSnapshot(){
        long now=System.nanoTime();
        if(now-publishAt<100000000L)return;
        synchronized(GATE){snapshot=new Snapshot(status,sprintKnown?(lastSprint?"Sprint on":"Sprint off"):"Sprint unknown",
            Double.isNaN(lastReleaseMs)?"Release unknown":"Last release "+Math.round(lastReleaseMs)+" ms",lastAction,dropped);publishAt=now;}
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
        try {strafeFloat=inheritedField(input.getClass(),"a");}
        catch(NoSuchFieldException missing){diagnostic(11,missing);}
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
        RecordingFile output = null;
        try {
            Path folder = Paths.get(exportDirectory);
            Files.createDirectories(folder);
            String when = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
            output=new RecordingFile(folder,when+"-"+UUID.randomUUID().toString().substring(0,8),FILE_BYTES,rotateFiles);
            status="CombatIQ recording (local CSV)";
            long lastFlush = System.nanoTime();
            long rows = 0L;
            while (running || head != tail) {
                if (stopAt != 0 && System.nanoTime() - stopAt >= 0) {
                    capped=true; recordable = false; running = false;
                }
                long timestamp, order = 0, ep = 0, thread = 0, tick = 0, conn=0, packetId=0;int phase=0;
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
                        conn=CONNECTION[i];phase=PHASE[i];packetId=PACKET_ID[i];
                        tail++;
                    }
                }
                if (timestamp >= 0) {
                    String row = timestamp + "," + type + "," + value + "," + motion + ","
                        + (name == null ? "" : name) + "," + tick + "," + ep + "," + thread
                        + "," + order + "," + entity + "," + flags + "," + x + "," + y + "," + z + ","+conn+","+phase+","+packetId+"\n";
                    if (!output.append(row,dropped,filtered)) {
                        capped=true; recordable=false;running=false;
                        synchronized(GATE){dropped+=head-tail+1;tail=head;}
                        break;
                    }
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
                    output.flush(); lastFlush = now;
                    status = "CombatIQ ticks=" + ticks + " dropped=" + dropped;
                }
            }
            output.finish(dropped,filtered,capped);output=null;
            status = capped ? "CombatIQ capped; toggle recording for a new capture" : "CombatIQ stopped cleanly";
        } catch (Throwable ex) {
            status = "CombatIQ recorder stopped: " + ex.getClass().getSimpleName();
        } finally {
            running = false; recordable = false;
            if(output!=null)output.abort();
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

