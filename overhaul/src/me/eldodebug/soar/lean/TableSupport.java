package me.eldodebug.soar.lean;

import java.util.*;
import java.util.concurrent.*;
import me.eldodebug.soar.management.nanovg.*;
import me.eldodebug.soar.management.nanovg.font.*;
import me.eldodebug.soar.scout.*;
import me.eldodebug.soar.scout.ui.ScoutNumberFormatter;

public final class TableSupport {
  private static boolean hud;
  private static long started;
  private static final Set<String> COLUMNS =
      new HashSet<>(
          Arrays.asList(
              "NAME", "LV", "RANK", "FKDR", "WLR", "HWS", "FK", "WINS", "BEDS", "GUILD", "PING",
              "HEALTH", "INSIGHT"));

  public static void context(boolean h) {
    hud = h;
  }

  public static boolean isHud() {
    return hud;
  }

  public static long begin(boolean h) {
    hud = h;
    return FrameProbe.start();
  }

  public static boolean cosmetic(boolean original) {
    return original && !LeanSettings.on("maximumFPS");
  }

  public static boolean animations(boolean original) {
    return cosmetic(original) && !LeanSettings.get("animation").equals("NONE");
  }

  public static float spacing(float old) {
    int n = LeanSettings.number(hud ? "hudSpacing" : "tabSpacing", 0, 30);
    return n == 0 ? old : n;
  }

  public static List<ScoutRow> prepare(List<ScoutRow> rows, boolean h) {
    return prepare(rows, h, null, null);
  }

  public static List<ScoutRow> prepare(
      List<ScoutRow> rows, boolean h, ScoutState.Phase phase, ScoutConfig config) {
    context(h);
    if (rows == null) return Collections.emptyList();
    ArrayList<ScoutRow> copy = new ArrayList<>(rows);
    String sort = LeanSettings.get(h ? "hudSort" : "tabSort");
    if (!sort.equals("EXISTING")) {
      copy.sort(
          (a, b) -> {
            if (a.spectator != b.spectator) return a.spectator ? 1 : -1;
            if (phase == ScoutState.Phase.ACTIVE) {
              int order = Integer.compare(team(a.color), team(b.color));
              if (order != 0) return order;
            }
            if (config != null && config.watchlistFirst.isToggled() && a.watched != b.watched)
              return a.watched ? -1 : 1;
            if ((config == null || config.partySortFirst.isToggled()) && a.party != b.party)
              return a.party ? -1 : 1;
            if (ProfileExtras.friend(a.name) != ProfileExtras.friend(b.name))
              return ProfileExtras.friend(a.name) ? -1 : 1;
            Double av = a.stats == null ? null : a.stats.sortValue(sort),
                bv = b.stats == null ? null : b.stats.sortValue(sort);
            int c = av == null ? (bv == null ? 0 : 1) : (bv == null ? -1 : Double.compare(bv, av));
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
          });
    }
    int limit = LeanSettings.number(h ? "hudLimit" : "tabLimit", 1, 40);
    return copy.size() > limit ? new ArrayList<>(copy.subList(0, limit)) : copy;
  }

  private static int team(int color) {
    int[] colors = {0xff5555, 0x5555ff, 0x55ff55, 0xffff55, 0x55ffff, 0xffffff, 0xff55ff, 0xaaaaaa};
    for (int i = 0; i < colors.length; i++) if (color == colors[i]) return i;
    return 8;
  }

  public static List<String> columns(List<String> old, boolean h) {
    String value = LeanSettings.get(h ? "hudColumns" : "tabColumns");
    if (value.equals("EXISTING")) return old;
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String s : value.toUpperCase(Locale.ROOT).split(","))
      if (COLUMNS.contains(s.trim())) result.add(s.trim());
    return result.isEmpty() ? old : new ArrayList<>(result);
  }

  public static Font font(Font original) {
    String f = LeanSettings.get(hud ? "hudFont" : "tabFont");
    if (f.equals("MINECRAFT")) return Fonts.MOJANGLES;
    if (f.equals("REGULAR")) return Fonts.REGULAR;
    if (f.equals("MEDIUM")) return Fonts.MEDIUM;
    if (f.equals("POPPINS") || f.endsWith(".ttf") || f.endsWith(".otf"))
      return LocalFonts.get(f, original);
    return original;
  }

  public static String stat(String column, ScoutStats stats, boolean compact) {
    return column.equals("GUILD")
        ? (stats == null ? "—" : ProfileExtras.guild(stats.username))
        : ScoutNumberFormatter.stat(column, stats, compact);
  }

  public static ExecutorService profileWorkers(java.util.concurrent.ThreadFactory factory) {
    return new ThreadPoolExecutor(
        2,
        2,
        0,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(64),
        factory,
        new ThreadPoolExecutor.AbortPolicy());
  }

  public static Object boundedPut(ConcurrentHashMap<Object, Object> map, Object key, Object value) {
    Object old = map.put(key, value);
    while (map.size() > 500) {
      Iterator<Object> it = map.keySet().iterator();
      if (!it.hasNext()) break;
      map.remove(it.next());
    }
    return old;
  }

  public static float tabX(float centered) {
    int x = LeanSettings.number("tabX", -1, 10000);
    return x < 0 ? centered : x;
  }

  public static float tabY(float old) {
    return LeanSettings.number("tabY", 0, 10000);
  }
}
