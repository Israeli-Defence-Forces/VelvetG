package me.movi.violet;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModManager;
import me.movi.violet.mixin.VioletKeyBindingAccessor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import vgstub.*;
import vgstub.auh.a;

/** Runtime bridge for Violet. Optional hook failures disable Violet rather than taking Minecraft with them. */
public final class VioletRuntime {
    public static VioletModule module;
    public static VioletHud hud;
    public static VioletSettings settings;
    public static final VioletEngine engine = new VioletEngine(() -> ThreadLocalRandom.current().nextDouble());

    private static final class Session {
        final boolean[] owned = new boolean[2];
        final byte[] observed = new byte[356];
        ave client;
        Object world, player;
        int attack = Integer.MIN_VALUE, use = Integer.MIN_VALUE;
        int settingsHash = Integer.MIN_VALUE;
        boolean latch, previousEnabled;
    }

    private static Session session = new Session();
    private static volatile boolean failed;
    private static boolean failureLogged;
    private static int syntheticDepth;
    public static String state = "Disabled";

    private VioletRuntime() {}

    public static void install(ModManager manager) {
        if (settings != null) return;
        module = new VioletModule();
        hud = new VioletHud();
        manager.getMods().add(module);
        manager.getMods().add(hud);
        settings = new VioletSettings(module, hud);
        hud.setToggled(true);
        AssistRuntime.install(manager);
        BedWarsAwareness.instance = new BedWarsAwareness();
        manager.getMods().add(BedWarsAwareness.instance);
        WorldAwareness.instance = new WorldAwareness();
        manager.getMods().add(WorldAwareness.instance);
        TargetDetails.install(manager);
    }

    /** Single runtime hook used by r8. Kept deliberately independent of ordinals inside Minecraft.runTick. */
    public static void tick(ave mc) {
        if (failed) return;
        long started = System.nanoTime();
        if (settings != null) VioletDiagnostics.enabled(settings.flag("diagnostics"));
        try {
            head0(mc);
            if (AssistRuntime.before(mc)) {
                pause();
                AssistRuntime.hold(mc);
                state = "Using soup";
            } else {
                before0(mc);
            }
            if (hud != null) hud.tick(System.nanoTime());
            if (BedWarsAwareness.instance != null) BedWarsAwareness.instance.tick(mc);
            if (WorldAwareness.instance != null) WorldAwareness.instance.tick(mc);
        } catch (Throwable t) {
            fail("tick", t);
        } finally {
            VioletDiagnostics.state(state, System.nanoTime());
            VioletDiagnostics.tick(System.nanoTime() - started);
        }
    }

    /** Compatibility/testing entry point. */
    public static void head(ave mc) {
        if (failed) return;
        try { head0(mc); }
        catch (Throwable t) { fail("tick context", t); }
    }

    private static void head0(ave mc) {
        if (settings == null || mc == null || mc.t == null) return;
        session.client = mc;
        int attack = mc.t.ai.i(), use = mc.t.ag.i();
        boolean changed = session.world != mc.f || session.player != mc.h || session.attack != attack || session.use != use;
        if (changed) {
            reset();
            clearInput();
            engine.clear();
            VioletDiagnostics.clear();
            if (hud != null) hud.clearHistory();
            Session next = new Session();
            next.client = mc;
            next.world = mc.f;
            next.player = mc.h;
            next.attack = attack;
            next.use = use;
            session = next;
        }
        if (!context(mc)) {
            reset();
            clearInput();
        }
        int current = settings.runtimeHash();
        if (current != session.settingsHash || module.isToggled() != session.previousEnabled) {
            reset();
            session.settingsHash = current;
            session.previousEnabled = module.isToggled();
        }
    }

    /** Called from KeyBinding.setKeyBindState. Synthetic state changes made by Violet itself are ignored. */
    public static void keyState(int code, boolean down) {
        if (failed || syntheticDepth != 0 || settings == null || code == 0 || code < -100 || code >= 256) return;
        try {
            int index = code + 100;
            byte previous = session.observed[index];
            session.observed[index] = (byte) (down ? 1 : 2);
            if (!down || previous == 1) return; // edge-trigger shortcuts and physical-click accounting

            ave mc = session.client != null ? session.client : ave.A();
            if (mc == null || mc.m != null || !mc.w) return;
            AssistRuntime.input(mc, code, true);
            if (code == settings.key("settings")) {
                open();
                return;
            }
            if (code == settings.key("toggle")) {
                module.toggle();
                save();
                return;
            }
            if (!context(mc)) return;
            long now = System.nanoTime();
            if (code == session.attack) { engine.physical(0, now); VioletDiagnostics.physical(now); }
            if (code == session.use) { engine.physical(1, now); VioletDiagnostics.physical(now); }
            if (module.isToggled() && settings.choice("mode") == 2 && code == settings.key("activation")) session.latch = !session.latch;
        } catch (Throwable t) {
            fail("key state", t);
        }
    }

    /** Compatibility/testing entry point for the old r7 event bridge. */
    public static void input(ave mc, int code, boolean down, boolean repeat) {
        if (repeat || failed) return;
        session.client = mc;
        keyState(code, down);
    }

    /** Compatibility/testing entry point. */
    public static void before(ave mc) {
        if (failed) return;
        try {
            if (AssistRuntime.before(mc)) {
                pause();
                AssistRuntime.hold(mc);
                state = "Using soup";
                return;
            }
            before0(mc);
        } catch (Throwable t) {
            fail("click processing", t);
        }
    }

    private static void before0(ave mc) {
        if (settings == null || mc == null) return;
        session.client = mc;
        long now = System.nanoTime();
        if (!context(mc)) {
            reset();
            clearInput();
            state = "Paused";
            return;
        }
        if (!module.isToggled()) {
            reset();
            state = "Disabled";
            return;
        }
        int mode = settings.choice("mode");
        boolean active = false;
        String blocked = "Ready";
        for (int hand = 0; hand < 2; hand++) {
            int code = hand == 0 ? session.attack : session.use;
            boolean trigger = mode == 0 ? physical(code) : mode == 1 ? physical(settings.key("activation")) : session.latch;
            boolean wanted = settings.flag(hand == 0 ? "left" : "right") && trigger && code != 0;
            String reason = wanted ? filter(mc, hand) : "Ready";
            boolean allowed = wanted && reason == null;
            if (allowed && hand == 1) {
                setKeyStateSynthetic(code, false);
                session.owned[hand] = true;
            } else {
                release(hand);
            }
            double min = settings.number(hand == 0 ? "leftMin" : "rightMin");
            double max = Math.max(min, settings.number(hand == 0 ? "leftMax" : "rightMax"));
            avb binding = hand == 0 ? mc.t.ai : mc.t.ag;
            boolean pending = ((VioletKeyBindingAccessor) (Object) binding).violet$getPending() > 0;
            int count = engine.poll(hand, now, allowed, pending, min, max,
                    (int) settings.number("delay"), (int) settings.number("ramp"),
                    settings.flag("jitter") ? settings.number("variation") : 0);
            for (int click = 0; click < count; click++) pressSynthetic(code);
            if (count > 0) { engine.generated(hand, now, count); VioletDiagnostics.generated(count, now); }
            active |= allowed;
            if (wanted && reason != null) blocked = reason;
        }
        state = active ? "Clicking" : blocked;
    }

    static String filter(ave mc, int hand) {
        if (settings.flag("creative") && mc.h.bA.d) return "Creative";
        if (settings.flag("using") && mc.h.bS()) return "Using item";
        zx stack = mc.h.bZ();
        zw item = stack == null ? null : stack.b();
        if (hand == 0) {
            if (settings.flag("mining") && mc.s != null && mc.s.a == a.b) return "Mining";
            if (settings.flag("weapon") && !(item instanceof aay || item instanceof yl || item != null && item == zy.y)) return "Need weapon";
            if (settings.flag("target") && (mc.s == null || !(mc.s.d instanceof wn) || !mc.s.d.ai())) return "Need player";
        } else if (settings.flag("blocks") && !(item instanceof yo)) return "Need blocks";
        return null;
    }

    static boolean context(ave mc) {
        return mc != null && mc.f != null && mc.h != null && mc.m == null && mc.w && !mc.V() && mc.h.ai() && !mc.h.v();
    }

    public static boolean physical(int code) {
        if (code == 0 || code < -100 || code >= 256) return false;

        // KeyBinding.setKeyBindState is the canonical transition stream. Once Violet has
        // observed a real press/release for this key, preserve that state until the next
        // transition. This matters because Violet may temporarily suppress the vanilla
        // KeyBinding state it owns, while LWJGL polling can lag or disagree for a frame.
        // Device polling is only a fallback for keys for which no transition has been
        // observed in the current world/session. Context changes clear observed[].
        byte event = session.observed[code + 100];
        if (event == 1) return true;
        if (event == 2) return false;

        try {
            if (code < 0 && Mouse.isCreated() && code + 100 < Mouse.getButtonCount()) return Mouse.isButtonDown(code + 100);
            if (code > 0 && Keyboard.isCreated()) return Keyboard.isKeyDown(code);
        } catch (Throwable ignored) {}
        return false;
    }

    private static void clearInput() { Arrays.fill(session.observed, (byte) 0); }

    static void setKeyStateSynthetic(int code, boolean down) {
        syntheticDepth++;
        try { avb.a(code, down); }
        finally { syntheticDepth--; }
    }

    static void pressSynthetic(int code) {
        syntheticDepth++;
        try { avb.a(code); }
        finally { syntheticDepth--; }
    }

    private static void release(int hand) {
        if (session.owned[hand]) {
            int code = hand == 0 ? session.attack : session.use;
            setKeyStateSynthetic(code, context(session.client) && physical(code));
            session.owned[hand] = false;
        }
    }

    private static void pause() {
        engine.cancel();
        release(0);
        release(1);
    }

    public static void reset() {
        pause();
        session.latch = false;
    }

    private static void fail(String stage, Throwable t) {
        failed = true;
        state = "Error";
        try { engine.clear(); } catch (Throwable ignored) {}
        try { release(0); release(1); } catch (Throwable ignored) {}
        try { if (module != null && module.isToggled()) module.setToggled(false); } catch (Throwable ignored) {}
        if (!failureLogged) {
            failureLogged = true;
            Exception e = t instanceof Exception ? (Exception) t : new RuntimeException(t);
            GlideLogger.error("[VelvetG/Violet] Disabled after " + stage + " hook failure", e);
        }
    }

    public static void open() { reset(); ave.A().a(new VioletScreen()); }

    public static boolean openFor(Mod mod) {
        if (mod instanceof WorldAwareness) { reset(); ave.A().a(new AssistScreen(mod, ((WorldAwareness)mod).controls())); return true; }
        if (mod instanceof BedWarsAwareness) {
            reset(); ave.A().a(new AssistScreen(mod, ((BedWarsAwareness)mod).controls())); return true;
        }
        if (mod instanceof AssistModule) {
            reset();
            ave.A().a(new AssistScreen((AssistModule) mod));
            return true;
        }
        if (mod instanceof VioletModule || mod instanceof VioletHud) {
            reset();
            ave.A().a(new VioletScreen(mod instanceof VioletHud ? 3 : 0));
            return true;
        }
        return false;
    }

    public static String keyName(int key) {
        if (key == 0) return "Unbound";
        if (key < 0) return "Mouse " + (key + 101);
        String n = Keyboard.getKeyName(key);
        return n == null ? "Key " + key : n;
    }

    public static void save() {
        if (Glide.getInstance().getProfileManager() != null) Glide.getInstance().getProfileManager().save();
    }
}
