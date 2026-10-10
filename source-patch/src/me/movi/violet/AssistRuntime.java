package me.movi.violet;

import java.util.concurrent.ConcurrentHashMap;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.mods.ModManager;
import me.movi.violet.mixin.AssistControllerAccessor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import vgstub.*;
import vgstub.auh.a;

/** Slot ownership is local and temporary; runtime failures disable the optional assist subsystem instead of crashing the client. */
public final class AssistRuntime {
    public static AssistModule tool, move, soup;
    public static VioletSettings.Flag restoreTool, containers, jump, sneak, creativeSoup, refillSoup;
    public static VioletSettings.Range health, cooldown;
    public static VioletSettings.Pick soupMode, toolFilterMode;
    public static VioletSettings.Text toolBlocks;
    private static final BlockFilter toolFilter = new BlockFilter();

    private static final ConcurrentHashMap<Class<?>, Boolean> TEXT_FIELD_CACHE = new ConcurrentHashMap<Class<?>, Boolean>();
    private static final class Session {
        ave client;
        Object world, player;
        int oldTool = -1, toolSlot = -1, oldSoup = -1, soupSlot = -1, soupAge, soupUse = Integer.MIN_VALUE;
        long nextSoup, toolYieldUntil, soupYieldUntil;
        zx soupStack;
        int refillSource = -1, refillTarget = -1;
        zx refillStack;
        long refillAt;
        final SoupConfirmation confirmation = new SoupConfirmation();
    }
    private static Session run = new Session();
    private static boolean failed, failureLogged;

    private AssistRuntime() {}

    public static void install(ModManager manager) {
        if (tool != null) return;
        tool = new AssistModule(0);
        move = new AssistModule(1);
        soup = new AssistModule(2);
        manager.getMods().add(tool);
        manager.getMods().add(move);
        manager.getMods().add(soup);
        restoreTool = new VioletSettings.Flag(tool, "assist.tool.restore", "Restore slot", true);
        toolFilterMode = new VioletSettings.Pick(tool, "assist.tool.filterMode", "Block filter", 0, new String[]{"All blocks", "Whitelist", "Blacklist"});
        toolBlocks = new VioletSettings.Text(tool, "assist.tool.blocks", "Block IDs", "minecraft:stone,minecraft:obsidian");
        containers = new VioletSettings.Flag(move, "assist.move.containers", "Other containers", false);
        jump = new VioletSettings.Flag(move, "assist.move.jump", "Allow jumping", true);
        sneak = new VioletSettings.Flag(move, "assist.move.sneak", "Allow sneaking", true);
        health = new VioletSettings.Range(soup, "assist.soup.health", "Health threshold (HP)", 12, 1, 20, true);
        cooldown = new VioletSettings.Range(soup, "assist.soup.cooldown", "Use cooldown (ms)", 350, 100, 2000, true);
        soupMode = new VioletSettings.Pick(soup, "assist.soup.mode", "Soup mode", 0, new String[]{"Instant soup server", "Normal eating"});
        creativeSoup = new VioletSettings.Flag(soup, "assist.soup.creative", "Allow in creative", false);
        refillSoup = new VioletSettings.Flag(soup, "assist.soup.refill", "Refill in inventory", false);
    }

    private static boolean ready(ave mc) { return mc != null && mc.f != null && mc.h != null && mc.h.bi != null && mc.t != null && mc.h.ai() && !mc.h.v() && !mc.V(); }
    private static boolean game(ave mc) { return ready(mc) && mc.w; }
    private static void sync(ave mc) { if (mc.c != null) ((AssistControllerAccessor) (Object) mc.c).violet$syncSlot(); }
    private static void select(ave mc, int slot) { if (slot >= 0 && slot < 9 && mc.h.bi.c != slot) { mc.h.bi.c = slot; sync(mc); } }

    /**
     * Only explicit hotbar-bind presses claim manual slot ownership. Ordinary movement and other
     * unrelated bindings no longer suppress assists. Slot mismatches remain the fallback signal.
     */
    public static void input(ave mc, int code, boolean down) {
        if (failed || !down || mc == null || mc.m != null || mc.t == null || mc.t.aw == null) return;
        // In the supported 1.8.9 base this is the nine-slot hotbar binding array. Fail narrow if a base changes it.
        if (mc.t.aw.length != 9) return;
        for (avb key : mc.t.aw) {
            if (key == null || key.i() != code) continue;
            long now = System.nanoTime();
            if (run.oldTool >= 0 && mc.h != null && mc.h.bi != null && mc.h.bi.c == run.toolSlot) {
                run.oldTool = run.toolSlot = -1;
                run.toolYieldUntil = now + 400000000L;
            }
            if (run.soupSlot >= 0) {
                endSoup(mc, false);
                run.soupYieldUntil = now + 400000000L;
            }
            return;
        }
    }

    public static boolean before(ave mc) {
        if (failed) return false;
        try { return before0(mc); }
        catch (Throwable t) { fail("tick", t); return false; }
    }

    private static boolean before0(ave mc) {
        if (tool == null) return false;
        run.client = mc;
        if (run.world != mc.f || run.player != mc.h) {
            if (run.soupUse != Integer.MIN_VALUE) VioletRuntime.setKeyStateSynthetic(run.soupUse, false);
            Session next = new Session();
            next.client = mc;
            next.world = mc.f;
            next.player = mc.h;
            run = next;
        }
        if (!game(mc) || mc.m != null) {
            endTool(mc, true);
            endSoup(mc, true);
            run.confirmation.clear();
            run.toolYieldUntil = 0;
            run.soupYieldUntil = 0;
            refill(mc, System.nanoTime());
            return false;
        }
        long now = System.nanoTime();
        boolean attack = VioletRuntime.physical(mc.t.ai.i());
        if (run.oldTool >= 0 && mc.h.bi.c != run.toolSlot) {
            // The player deliberately selected another slot. Release ownership briefly, not for the whole mouse hold.
            run.oldTool = run.toolSlot = -1;
            run.toolYieldUntil = now + 400000000L;
        }
        if (run.soupSlot >= 0) {
            if (mc.h.bi.c != run.soupSlot) {
                endSoup(mc, false);
                run.soupYieldUntil = now + 400000000L;
                return false;
            }
            if (!soup.isToggled() || mc.h.bi.a[run.soupSlot] != run.soupStack || !mc.h.bS() || ++run.soupAge > 45) endSoup(mc, true);
            else { hold(mc); return true; }
        }
        if (!soup.isToggled() || mc.c == null || now < run.soupYieldUntil || mc.h.bS() || mc.h.bA.d && !creativeSoup.isToggled() || mc.h.bn() > health.getValue() || now < run.nextSoup) {
            autoTool(mc, attack, now);
            return false;
        }
        int slot = findSoup(mc.h.bi.a);
        if (slot < 0) { autoTool(mc, attack, now); return false; }
        if (soupMode.index() != 0) run.confirmation.clear();
        else if (!run.confirmation.allows(slot, mc.h.bi.a[slot], mc.h.bi.a[slot].b, mc.h.bn())) { autoTool(mc, attack, now); return false; }
        endTool(mc, true);
        run.oldSoup = mc.h.bi.c;
        run.soupSlot = slot;
        run.soupStack = mc.h.bi.a[slot];
        run.soupAge = 0;
        run.nextSoup = now + (long) cooldown.getValue() * 1000000L;
        select(mc, slot);
        if (soupMode.index() == 0) run.confirmation.attempted(slot, run.soupStack, run.soupStack.b, mc.h.bn());
        mc.c.a(mc.h, mc.f, run.soupStack);
        if (soupMode.index() == 0) { endSoup(mc, true); return true; }
        if (!mc.h.bS()) { endSoup(mc, true); return true; }
        hold(mc);
        return true;
    }

    static int findSoup(zx[] hotbar) { if (hotbar == null) return -1; for (int i = 0; i < Math.min(9, hotbar.length); i++) if (hotbar[i] != null && hotbar[i].b > 0 && hotbar[i].b() == zy.A) return i; return -1; }
    static int bestTool(zx[] items, int selected, afh block) {
        if (items == null || selected < 0 || selected >= Math.min(9, items.length) || block == null) return selected;
        float best = strength(items[selected], block); boolean harvest = harvests(items[selected], block); int slot = selected;
        for (int i = 0; i < Math.min(9, items.length); i++) {
            float score = strength(items[i], block); boolean can = harvests(items[i], block);
            boolean better = can && !harvest || can == harvest && score > best + .001f;
            // Keep an equally good manually selected tool; break ties only while selecting another tool.
            boolean tie = can == harvest && Math.abs(score - best) <= .001f && score > 1 && slot != selected
                && remaining(items[i]) > remaining(items[slot]);
            if (better || tie) { best = score; slot = i; harvest = can; }
        }
        return slot;
    }
    private static int remaining(zx stack) { return stack == null || stack.j() <= 0 ? 0 : Math.max(0, stack.j() - stack.i()); }
    private static boolean harvests(zx stack, afh block) { return block.t().l() || stack != null && stack.b > 0 && stack.b(block); }
    private static float strength(zx stack, afh block) { if (stack == null || stack.b <= 0) return 1; float score = stack.a(block); if (score > 1) { int efficiency = ack.a(32, stack); if (efficiency > 0) score += efficiency * efficiency + 1; } return score; }

    private static void autoTool(ave mc, boolean attack, long now) { if (!tool.isToggled() || now < run.toolYieldUntil || !attack || mc.h.bS() || mc.s == null || mc.s.a != a.b) { endTool(mc, true); return; } afh block = mc.f.p(mc.s.a()).c(); if (!toolFilter.allows(String.valueOf(afh.c.c(block)), toolFilterMode.index(), toolBlocks.getText())) { endTool(mc, true); return; } int best = bestTool(mc.h.bi.a, mc.h.bi.c, block); if (best != mc.h.bi.c) { if (run.oldTool < 0) run.oldTool = mc.h.bi.c; run.toolSlot = best; select(mc, best); } }
    private static void endTool(ave mc, boolean restore) { if (run.oldTool >= 0 && mc != null && mc.h != null && mc.h.bi != null && mc.h == run.player && mc.f == run.world && mc.h.bi.c == run.toolSlot && restore && restoreTool.isToggled()) select(mc, run.oldTool); run.oldTool = run.toolSlot = -1; }
    public static void hold(ave mc) { if (run.soupSlot >= 0 && soupMode.index() == 1 && game(mc)) { int code = mc.t.ag.i(); if (run.soupUse != Integer.MIN_VALUE && run.soupUse != code) VioletRuntime.setKeyStateSynthetic(run.soupUse, false); run.soupUse = code; VioletRuntime.setKeyStateSynthetic(code, true); } }
    private static void endSoup(ave mc, boolean restore) { if (run.soupSlot >= 0 && mc != null && mc.h != null && mc.h.bi != null && mc.h == run.player && mc.f == run.world) { if (mc.h.bS() && mc.c != null) mc.c.c(mc.h); if (restore && mc.h.bi.c == run.soupSlot) select(mc, run.oldSoup); } if (run.soupUse != Integer.MIN_VALUE) { VioletRuntime.setKeyStateSynthetic(run.soupUse, game(mc) && VioletRuntime.physical(run.soupUse)); run.soupUse = Integer.MIN_VALUE; } run.oldSoup = run.soupSlot = -1; run.soupStack = null; }
    public static void disabled(int kind) {
        try {
            if (kind == 0) {
                endTool(run.client, true);
                run.toolYieldUntil = 0;
            }
            if (kind == 2) {
                endSoup(run.client, true);
                run.soupYieldUntil = 0;
                run.nextSoup = 0;
                run.soupAge = 0;
                run.confirmation.clear();
                run.refillSource = run.refillTarget = -1; run.refillStack = null;
            }
        } catch (Throwable t) {
            // Cleanup must never recurse back through fail() via setToggled(false).
            if (!failureLogged) {
                failureLogged = true;
                Exception e = t instanceof Exception ? (Exception) t : new RuntimeException(t);
                GlideLogger.error("[VelvetG/Assists] Disable cleanup failed", e);
            }
        }
    }

    /** One ordinary hotbar swap, only while the player's own inventory is open with an empty cursor. */
    private static void refill(ave mc, long now) {
        if (!soup.isToggled() || !refillSoup.isToggled() || !ready(mc) || !(mc.m instanceof azc)
                || mc.c == null || mc.h.bj == null || mc.h.bk != mc.h.bj || mc.h.bi.p() != null || mc.h.bS()) return;
        zx[] items = mc.h.bi.a;
        if (run.refillSource >= 0) {
            if (items[run.refillSource] == run.refillStack && (items[run.refillTarget] == null || items[run.refillTarget].b() != zy.A)) return;
            run.refillSource = run.refillTarget = -1; run.refillStack = null;
        }
        if (now < run.refillAt) return;
        int target = -1, source = -1;
        for (int i = 0; i < Math.min(9, items.length); i++) if (items[i] == null || items[i].b() == zy.z) { target = i; break; }
        if (target < 0) return;
        for (int i = 9; i < Math.min(36, items.length); i++) if (items[i] != null && items[i].b > 0 && items[i].b() == zy.A) { source = i; break; }
        if (source < 0) return;
        run.refillSource = source; run.refillTarget = target; run.refillStack = items[source]; run.refillAt = now + 350000000L;
        mc.c.a(mc.h.bj.d, source, target, 2, mc.h);
    }

    public static boolean allowsScreen(axu screen) {
        if (screen instanceof azc) return true;
        if (screen == null || !containers.isToggled() || !(screen instanceof ayl)) return false;
        return !hasTextField(screen.getClass());
    }

    private static boolean hasTextField(Class<?> type) {
        Boolean cached = TEXT_FIELD_CACHE.get(type);
        if (cached != null) return cached.booleanValue();
        boolean found = false;
        try {
            for (Class<?> c = type; c != null && !found; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (avw.class.isAssignableFrom(f.getType())) { found = true; break; }
                }
            }
        } catch (Throwable ignored) {
            found = true; // fail closed: do not borrow movement keys in an unknown text-entry container
        }
        TEXT_FIELD_CACHE.put(type, Boolean.valueOf(found));
        return found;
    }

    private static boolean down(avb key) { if (key == null) return false; int code = key.i(); if (code < 0) return code >= -100 && Mouse.isCreated() && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100); return code > 0 && code < 256 && Keyboard.isCreated() && Keyboard.isKeyDown(code); }

    public static void move(ave mc, beu input) {
        if (failed) return;
        try { move0(mc, input, Display.isActive()); }
        catch (Throwable t) { fail("movement", t); }
    }

    public static void move(ave mc, beu input, boolean windowActive) {
        if (failed) return;
        try { move0(mc, input, windowActive); }
        catch (Throwable t) { fail("movement", t); }
    }

    private static void move0(ave mc, beu input, boolean windowActive) {
        if (move == null || !move.isToggled() || mc == null || mc.m == null) return;
        input.a = input.b = 0;
        input.c = input.d = false;
        if (!ready(mc) || !windowActive || !allowsScreen(mc.m)) return;
        input.a = (down(mc.t.Z) ? 1 : 0) - (down(mc.t.ab) ? 1 : 0);
        input.b = (down(mc.t.Y) ? 1 : 0) - (down(mc.t.aa) ? 1 : 0);
        input.c = jump.isToggled() && down(mc.t.ac);
        input.d = sneak.isToggled() && down(mc.t.ad);
        if (input.d) { input.a *= .3f; input.b *= .3f; }
    }

    private static void fail(String stage, Throwable t) {
        failed = true;
        try { endTool(run.client, true); } catch (Throwable ignored) {}
        try { endSoup(run.client, true); } catch (Throwable ignored) {}
        try { if (tool != null && tool.isToggled()) tool.setToggled(false); } catch (Throwable ignored) {}
        try { if (move != null && move.isToggled()) move.setToggled(false); } catch (Throwable ignored) {}
        try { if (soup != null && soup.isToggled()) soup.setToggled(false); } catch (Throwable ignored) {}
        if (!failureLogged) {
            failureLogged = true;
            Exception e = t instanceof Exception ? (Exception) t : new RuntimeException(t);
            GlideLogger.error("[VelvetG/Assists] Disabled after " + stage + " failure", e);
        }
    }
}
