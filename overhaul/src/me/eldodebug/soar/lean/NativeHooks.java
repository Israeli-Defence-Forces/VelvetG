package me.eldodebug.soar.lean;

import java.util.*;
import java.util.concurrent.*;
import me.eldodebug.soar.management.event.impl.EventSendChat;
import me.eldodebug.soar.management.mods.impl.ScoutMod;
import me.eldodebug.soar.scout.*;
import vgstub.*;

public final class NativeHooks {
  private static final ExecutorService KEYS =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(4),
          r -> {
            Thread t = new Thread(r, "VelvetG-key-store");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private static volatile String host = "";
  private static Object connection;
  private static volatile Map<String, NameLabel> nametags = Collections.emptyMap();
  private static long snapshot;
  private static String lastHost = "";
  private static volatile boolean onPika;

  public static boolean editing() {
    ScoutMod m = ScoutMod.getInstance();
    return m != null && m.isEditing();
  }

  public static boolean previewTab() {
    ScoutMod m = ScoutMod.getInstance();
    return m != null && m.renderTab();
  }

  public static boolean tagPhase(ScoutState.Phase phase) {
    return LeanSettings.on(
        phase == ScoutState.Phase.ACTIVE
            ? "nametagGame"
            : phase == ScoutState.Phase.WAITING ? "nametagWaiting" : "nametagElsewhere");
  }

  public static void tick() {
    ave mc = ave.A();
    if (mc == null) return;
    Object c = mc.u();
    String h = mc.D() == null ? "" : mc.D().b;
    host = h;
    if (connection != c || !h.equals(lastHost)) {
      connection = c;
      lastHost = h;
      onPika = KeyVault.pika(h);
      ProfileExtras.clear();
      RowLifecycle.clear();
      TableEffects.clear();
      nametags = Collections.emptyMap();
      if (c != null) {
        try {
          KEYS.execute(() -> KeyVault.get());
        } catch (RejectedExecutionException ignored) {
        }
      }
    }
    if (mc.h == null || mc.f == null || mc.F()) {
      KeyVault.request(null, "", System.currentTimeMillis(), false);
      return;
    }
    ScoutMod activeModule = ScoutMod.getInstance();
    boolean needed = activeModule != null && activeModule.isToggled();
    if (!needed) nametags = Collections.emptyMap();
    ProfileExtras.local(mc.h.e_());
    if (KeyVault.request(c, h, System.currentTimeMillis(), needed && LeanSettings.on("autoKey")))
      mc.h.e("/apikey");
    if (LeanSettings.on("nametag") && System.nanoTime() - snapshot > 250000000L) {
      snapshot = System.nanoTime();
      ScoutMod mod = ScoutMod.getInstance();
      if (mod != null && mod.isToggled() && tagPhase(ScoutState.currentPhase())) {
        Map<String, NameLabel> m = new HashMap<>();
        String stat = LeanSettings.get("nametagStat");
        if (Arrays.asList("FKDR", "WLR", "HWS", "LV", "FK", "WINS", "BEDS").contains(stat))
          for (ScoutRow r : mod.currentRows())
            if (r.stats != null
                && r.stats.status == ScoutStats.Status.OK
                && (!r.spectator || LeanSettings.on("nametagSpectators"))) {
              String value =
                  "§7" + (stat.equals("HWS") ? "BEST WS" : stat) + " " + r.stats.text(stat);
              NameLabel old = nametags.get(r.name);
              m.put(
                  r.name, old != null && old.statistic.equals(value) ? old : new NameLabel(value));
            }
        nametags = Collections.unmodifiableMap(m);
      } else nametags = Collections.emptyMap();
    }
  }

  /** Called before the packet event bus or vanilla chat logging receives a key. */
  public static boolean saveKey(String value, java.util.function.Consumer<Boolean> completion) {
    try {
      KEYS.execute(
          () -> {
            boolean saved = KeyVault.save(value);
            if (completion != null) completion.accept(saved);
          });
      return true;
    } catch (RejectedExecutionException e) {
      return false;
    }
  }

  public static boolean packet(Object packet) {
    if (!(packet instanceof fy)) return false;
    fy p = (fy) packet;
    String text = p.a() == null ? "" : p.a().c();
    String k = KeyVault.capture(text, host, p.c(), System.currentTimeMillis());
    String clean = KeyVault.redact(text);
    if (clean.equals(text)) return false;
    if (k != null) saveKey(k, null);
    return true;
  }

  private static NameLabel label(Object entity) {
    if (!LeanSettings.on("nametag")
        || !onPika
        || !tagPhase(ScoutState.currentPhase())
        || !(entity instanceof wn)) return null;
    return nametags.get(((wn) entity).e_());
  }

  public static String tag(Object entity) {
    NameLabel l = label(entity);
    return l == null ? null : l.statistic;
  }

  public static String above(Object entity) {
    return LeanSettings.get("nametagPlacement").equals("ABOVE") ? tag(entity) : null;
  }

  public static String nametag(String original, Object entity) {
    if (LeanSettings.get("nametagPlacement").equals("ABOVE")) return original;
    NameLabel label = label(entity);
    return label == null ? original : label.beside(original);
  }

  public static boolean chat(EventSendChat event) {
    String msg = event.getMessage().trim();
    if (msg.equalsIgnoreCase("/apikey")) KeyVault.manual(host, System.currentTimeMillis());
    if (msg.toLowerCase(Locale.ROOT).startsWith("/velvetlean profile ")) {
      event.setCancelled(true);
      String action =
          msg.substring("/velvetlean profile ".length()).trim().toLowerCase(Locale.ROOT);
      String notice;
      if (action.equals("start")) {
        FrameProbe.reset();
        FrameProbe.enabled = true;
        notice = "Profiler window started; stop with /velvetlean profile stop";
      } else if (action.equals("stop")) {
        FrameProbe.enabled = false;
        FrameProbe.export();
        notice = "Profiler stopped and exported locally";
      } else if (action.equals("export")) {
        FrameProbe.export();
        notice = "Profiler exported locally";
      } else notice = "Use /velvetlean profile start, stop, or export";
      if (ave.A().h != null) ave.A().h.a(new fa("§bVelvetG §7" + notice));
      return true;
    }
    if (!msg.equalsIgnoreCase("/velvetlean")) return false;
    event.setCancelled(true);
    ave.A().a(new LeanScreen());
    return true;
  }

  public static void frame() {
    FrameProbe.frame();
    TableEffects.nextFrame();
    AsyncImages.frame();
    ave mc = ave.A();
    ScoutMod m = ScoutMod.getInstance();
    if (mc == null || mc.h == null || mc.f == null || m == null || !m.isToggled() || mc.m != null) {
      TableEffects.clear();
      return;
    }
    boolean held = mc.t.al.d();
    if (!held && (LeanSettings.on("tabPersistent") || TableEffects.tabTail(false))) m.renderTab();
  }

  public static boolean tabWanted() {
    ave mc = ave.A();
    return TabPlacement.active() || LeanSettings.on("tabPersistent") || (mc != null && mc.t.al.d());
  }

  public static String apiSummary() {
    ScoutMod m = ScoutMod.getInstance();
    return m == null ? "API unavailable" : m.apiHealthLabel() + "; " + m.cacheSummary();
  }

  public static void shutdown() {
    FrameProbe.export();
    ConfigWriter.shutdown();
    AsyncImages.shutdown();
    GalleryIndex.shutdown();
    KEYS.shutdownNow();
    LocalFonts.shutdown();
    Backgrounds.shutdown();
  }
}
