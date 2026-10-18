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
  private static volatile Map<String, String> nametags = Collections.emptyMap();
  private static long snapshot;
  private static String lastHost = "";
  private static volatile boolean onPika;

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
      if (mod != null && mod.isToggled()) {
        Map<String, String> m = new HashMap<>();
        String stat = LeanSettings.get("nametagStat");
        if (Arrays.asList("FKDR", "WLR", "HWS", "LV", "FK", "WINS", "BEDS").contains(stat))
          for (ScoutRow r : mod.currentRows())
            if (r.stats != null && r.stats.status == ScoutStats.Status.OK)
              m.put(
                  r.name,
                  "§7" + (stat.equals("HWS") ? "BEST WS" : stat) + " " + r.stats.text(stat));
        nametags = Collections.unmodifiableMap(m);
      }
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

  public static String tag(Object entity) {
    if (!LeanSettings.on("nametag") || !onPika || !(entity instanceof wn)) return null;
    return nametags.get(((wn) entity).e_());
  }

  public static String above(Object entity) {
    return LeanSettings.get("nametagPlacement").equals("ABOVE") ? tag(entity) : null;
  }

  public static String nametag(String original, Object entity) {
    if (LeanSettings.get("nametagPlacement").equals("ABOVE")) return original;
    String tag = tag(entity);
    return tag == null ? original : original + " §7[" + tag + "]";
  }

  public static boolean chat(EventSendChat event) {
    String msg = event.getMessage().trim();
    if (msg.equalsIgnoreCase("/apikey")) KeyVault.manual(host, System.currentTimeMillis());
    if (!msg.equalsIgnoreCase("/velvetlean")) return false;
    event.setCancelled(true);
    ave.A().a(new LeanScreen());
    return true;
  }

  public static void frame() {
    FrameProbe.frame();
    AsyncImages.frame();
    if (LeanSettings.on("tabPersistent")) {
      ave mc = ave.A();
      ScoutMod m = ScoutMod.getInstance();
      if (m != null && mc.h != null && !mc.t.al.d()) m.renderTab();
    }
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
