package me.eldodebug.soar.lean;

import java.util.*;
import java.util.concurrent.*;
import me.eldodebug.soar.management.nanovg.*;
import me.eldodebug.soar.management.nanovg.font.*;
import me.eldodebug.soar.scout.*;
import me.eldodebug.soar.scout.ui.ScoutNumberFormatter;

public final class TableSupport {
  private static boolean hud, preview;
  private static final float[] viewportWidth = new float[2], viewportHeight = new float[2];

  public static void preview(boolean value) {
    preview = value;
  }

  public static boolean preview() {
    return preview;
  }

  public static void viewport(float width, float height) {
    viewportWidth[hud ? 1 : 0] = width;
    viewportHeight[hud ? 1 : 0] = height;
  }

  public static float viewportWidth() {
    return viewportWidth[hud ? 1 : 0];
  }

  public static float viewportHeight() {
    return viewportHeight[hud ? 1 : 0];
  }

  private static final ScoutState.Phase[] phases = {ScoutState.Phase.LOBBY, ScoutState.Phase.LOBBY};
  private static final int[] TEAM_COLORS = {
    0xff5555, 0x5555ff, 0x55ff55, 0xffff55, 0x55ffff, 0xffffff, 0xff55ff, 0xaaaaaa
  };

  private static final String[] columnSource = new String[2];
  private static final List<String>[] columnCache = new List[2];

  public static String value(String suffix) {
    return LeanSettings.panel(hud, suffix, phases[hud ? 1 : 0].name());
  }

  public static int number(String suffix, int fallback, int low, int high) {
    String raw = value(suffix);
    if (!numeric(raw)) return fallback;
    try {
      return Math.max(low, Math.min(high, Integer.parseInt(raw)));
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static boolean numeric(String s) {
    if (s.isEmpty()) return false;
    char c = s.charAt(0);
    return (c >= '0' && c <= '9') || c == '-';
  }

  public static boolean flag(boolean original, String suffix) {
    String v = value(suffix);
    return v.equals("true") ? true : v.equals("false") ? false : original;
  }

  public static float scalar(float original, String suffix) {
    String raw = value(suffix);
    if (!numeric(raw)) return original;
    try {
      float n = Float.parseFloat(raw);
      if (Float.isNaN(n) || Float.isInfinite(n)) return original;
      if (suffix.equals("FontSize")) return Math.max(6, Math.min(20, n));
      if (suffix.equals("NameWidth")) return Math.max(40, Math.min(300, n));
      if (suffix.equals("CornerRadius")) return Math.max(0, Math.min(20, n));
      if (suffix.equals("Scale")) return Math.max(50, Math.min(150, n));
      return n;
    } catch (NumberFormatException e) {
      return original;
    }
  }

  public static int integer(int original, String suffix) {
    return number(suffix, original, 0, 5000);
  }

  public static boolean animationsEnabled() {
    return !TabPlacement.active()
        && !LeanSettings.on("maximumFPS")
        && !LeanSettings.animation().equals("NONE");
  }

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
    return original && animationsEnabled();
  }

  public static float spacing(float old) {
    int n = number("Spacing", 0, 0, 30);
    return n == 0 ? old : n;
  }

  public static List<ScoutRow> prepare(List<ScoutRow> rows, boolean h) {
    return prepare(rows, h, null, null);
  }

  public static List<ScoutRow> prepare(
      List<ScoutRow> rows, boolean h, ScoutState.Phase phase, ScoutConfig config) {
    context(h);
    phases[h ? 1 : 0] = phase == null ? ScoutState.Phase.LOBBY : phase;
    if (rows == null) return Collections.emptyList();
    Preparation p = preparations[h ? 1 : 0];
    long revision = LeanSettings.revision();
    ScoutState.Phase current = phases[h ? 1 : 0];
    if (p == null || p.revision != revision || p.phase != current || p.config != config)
      preparations[h ? 1 : 0] = p = new Preparation(revision, current, config);
    // Native table construction consumes but does not modify the input. Preserve default order
    // without allocating another roster. Explicit sorts always own their mutable copy.
    if (p.sort.equals("EXISTING"))
      return rows.size() <= p.limit ? rows : new ArrayList<>(rows.subList(0, p.limit));
    ArrayList<ScoutRow> copy = new ArrayList<>(rows);
    copy.sort(p.order);
    return copy.size() > p.limit ? new ArrayList<>(copy.subList(0, p.limit)) : copy;
  }

  private static final Preparation[] preparations = new Preparation[2];

  private static final class Preparation {
    final long revision;
    final ScoutState.Phase phase;
    final ScoutConfig config;
    final String sort;
    final int limit;
    final Comparator<ScoutRow> order;

    Preparation(long revision, ScoutState.Phase phase, ScoutConfig config) {
      this.revision = revision;
      this.phase = phase;
      this.config = config;
      sort = value("Sort");
      limit = number("Limit", 40, 1, 40);
      final boolean friends = LeanSettings.on("friendsAfterParty");
      final boolean watched = config != null && config.watchlistFirst.isToggled();
      final boolean party = config == null || config.partySortFirst.isToggled();
      order =
          (a, b) -> {
            if (a.spectator != b.spectator) return a.spectator ? 1 : -1;
            if (phase == ScoutState.Phase.ACTIVE) {
              int teamOrder = Integer.compare(team(a.color), team(b.color));
              if (teamOrder != 0) return teamOrder;
            }
            // Native toggles can change without a Lean revision, so read these two when sorting.
            boolean watchedFirst = config == null ? watched : config.watchlistFirst.isToggled();
            boolean partyFirst = config == null ? party : config.partySortFirst.isToggled();
            if (watchedFirst && a.watched != b.watched) return a.watched ? -1 : 1;
            if (partyFirst && a.party != b.party) return a.party ? -1 : 1;
            if (friends) {
              boolean af = ProfileExtras.friend(a.name), bf = ProfileExtras.friend(b.name);
              if (af != bf) return af ? -1 : 1;
            }
            Double av = a.stats == null ? null : a.stats.sortValue(sort);
            Double bv = b.stats == null ? null : b.stats.sortValue(sort);
            int c = av == null ? (bv == null ? 0 : 1) : (bv == null ? -1 : Double.compare(bv, av));
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
          };
    }
  }

  private static int team(int color) {
    for (int i = 0; i < TEAM_COLORS.length; i++) if (color == TEAM_COLORS[i]) return i;
    return 8;
  }

  public static List<String> columns(List<String> old, boolean h) {
    context(h);
    String value = value("Columns");
    if (value.equals("EXISTING")) return old;
    int slot = h ? 1 : 0;
    if (value.equals(columnSource[slot])) return columnCache[slot];
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String s : value.toUpperCase(Locale.ROOT).split(","))
      if (COLUMNS.contains(s.trim())) result.add(s.trim());
    if (result.isEmpty()) return old;
    columnSource[slot] = value;
    return columnCache[slot] = Collections.unmodifiableList(new ArrayList<>(result));
  }

  public static Font font(Font original) {
    String f = value("Font");
    if (f.equals("MINECRAFT")) return Fonts.MOJANGLES;
    if (f.equals("REGULAR")) return Fonts.REGULAR;
    if (f.equals("MEDIUM")) return Fonts.MEDIUM;
    if (f.equals("POPPINS") || f.endsWith(".ttf") || f.endsWith(".otf"))
      return LocalFonts.get(f, original);
    return original;
  }

  public static me.eldodebug.soar.scout.ui.ScoutTheme theme(ScoutConfig config) {
    return LeanSettings.on("maximumFPS")
        ? me.eldodebug.soar.scout.ui.ScoutTheme.MINIMAL
        : config.theme();
  }

  public static String sort(ScoutConfig config) {
    String s = value("Sort");
    return s.equals("EXISTING") ? config.sort() : s;
  }

  public static List<String> sortChain(ScoutConfig config) {
    String s = value("Sort");
    return s.equals("EXISTING") ? config.sortChain() : Collections.singletonList(s);
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

  public static java.awt.Color color(java.awt.Color original, String column, ScoutRow row) {
    if ("NAME".equals(column)
        && flag(LeanSettings.on("friendHighlight"), "FriendHighlight")
        && phases[hud ? 1 : 0] != ScoutState.Phase.ACTIVE
        && !row.party
        && !row.spectator
        && ProfileExtras.friend(row.name)) {
      try {
        return new java.awt.Color(
            (Integer.parseInt(
                        value("FriendColor").equals("INHERIT")
                            ? LeanSettings.get("friendColor")
                            : value("FriendColor"),
                        16)
                    & 0xffffff)
                | (original.getAlpha() << 24),
            true);
      } catch (NumberFormatException ignored) {
      }
    }
    return original;
  }

  public static String cell(String text, String column) {
    return "PING".equals(column) && !flag(true, "PingUnits") && text.endsWith("ms")
        ? text.substring(0, text.length() - 2)
        : text;
  }

  public static String header(String header, String footer, boolean h, ScoutState.Phase phase) {
    if (h || phase != ScoutState.Phase.ACTIVE || !LeanSettings.on("tabMatchOverview"))
      return header;
    String summary =
        MatchSummary.parse((header == null ? "" : header) + "\n" + (footer == null ? "" : footer));
    return summary == null ? header : summary + "\n" + (header == null ? "" : header);
  }

  public static me.eldodebug.soar.scout.ui.ScoutPalette palette(
      ScoutConfig config, boolean h, int opacity) {
    context(h);
    me.eldodebug.soar.scout.ui.ScoutPalette p =
        me.eldodebug.soar.scout.ui.ScoutPalette.resolve(config, h, opacity);
    int alpha =
        value("Opacity").equals("EXISTING")
            ? p.background.getAlpha()
            : number("Opacity", p.background.getAlpha() * 100 / 255, 0, 100) * 255 / 100;
    boolean changed = !value("Opacity").equals("EXISTING");
    for (String suffix : PALETTE_KEYS) if (!value(suffix).equals("EXISTING")) changed = true;
    if (!changed) return p;
    java.awt.Color background = colorOverride(p.background, "PanelColor");
    return new me.eldodebug.soar.scout.ui.ScoutPalette(
        new java.awt.Color(background.getRed(), background.getGreen(), background.getBlue(), alpha),
        colorOverride(p.surface, "RowColor"),
        p.accent,
        colorOverride(p.primary, "TextColor"),
        colorOverride(p.secondary, "SecondaryColor"),
        colorOverride(p.header, "HeaderColor"),
        colorOverride(p.separator, "SeparatorColor"),
        p.status,
        p.party,
        p.self,
        p.shadow);
  }

  private static final String[] PALETTE_KEYS = {
    "TextColor", "SecondaryColor", "HeaderColor", "PanelColor", "RowColor", "SeparatorColor"
  };

  private static java.awt.Color colorOverride(java.awt.Color original, String suffix) {
    try {
      String v = value(suffix);
      return v.equals("EXISTING")
          ? original
          : new java.awt.Color(
              (Integer.parseInt(v, 16) & 0xffffff) | (original.getAlpha() << 24), true);
    } catch (NumberFormatException e) {
      return original;
    }
  }

  public static float fontSize(float old) {
    return scalar(old, "FontSize");
  }

  public static float rowHeight(float old) {
    return Math.max(old, scalar(old - 5, "FontSize") + 5);
  }

  public static float nameWidth(float old) {
    return scalar(old, "NameWidth");
  }

  public static float fittedScale(float old) {
    String v = value("Scale");
    return "EXISTING".equals(v) ? old : scalar(old * 100, "Scale") / 100f;
  }

  public static boolean entryEnabled(boolean old) {
    return animations(old) && !value("EntryAnimation").equals("NONE");
  }

  public static boolean resizeEnabled(boolean old) {
    return animations(old) && !value("ResizeAnimation").equals("NONE");
  }

  public static int entryMs(int old) {
    return number("EntryMs", old, 50, 5000);
  }

  public static int resizeMs(int old) {
    return number("ResizeMs", old, 50, 5000);
  }

  public static long partyWindow() {
    return LeanSettings.number("partyWindowMs", 50, 5000);
  }

  public static boolean needsTable(
      ScoutTable tab, boolean tabVisible, ScoutTable hudTable, boolean hudVisible) {
    return (tabVisible && tab == null) || (hudVisible && hudTable == null);
  }

  public static ScoutState.Phase phase() {
    return phases[hud ? 1 : 0];
  }

  public static ScoutTable build(
      List<ScoutRow> rows,
      ScoutConfig config,
      boolean h,
      String header,
      String footer,
      boolean pika,
      ScoutState.Phase phase) {
    if (!config.visible(h, phase, pika, phase != ScoutState.Phase.DISCONNECTED)
        && !NativeHooks.editing()) return null;
    return new ScoutTable(rows, config, h, header, footer, pika, phase);
  }

  public static float tabX(float centered) {
    if (TabPlacement.active()) return TabPlacement.x();
    int x = LeanSettings.number("tabX", -1, 10000);
    return x < 0 ? centered : x;
  }

  public static float tabY(float old) {
    return TabPlacement.active() ? TabPlacement.y() : LeanSettings.number("tabY", 0, 10000);
  }
}
