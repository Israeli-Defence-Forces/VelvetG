package me.eldodebug.soar.lean;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Additive settings. Existing Glide and VelvetG settings are never rewritten. */
public final class LeanSettings {
  private static final Properties P = new Properties();
  private static final File FILE = new File("config/velvetg-lean-v3.properties");
  private static final String[] BASE_KEYS = {
    "maximumFPS",
    "autoKey",
    "profiler",
    "nametag",
    "nametagStat",
    "tabLimit",
    "hudLimit",
    "tabSort",
    "hudSort",
    "tabFont",
    "hudFont",
    "tabColumns",
    "hudColumns",
    "tabX",
    "tabY",
    "animation",
    "animationMs",
    "tabBackground",
    "hudBackground",
    "headLayers",
    "tabPersistent",
    "nametagPlacement",
    "tabSpacing",
    "hudSpacing"
  };
  private static final String[] BASE_DEFAULTS = {
    "false",
    "false",
    "false",
    "false",
    "FKDR",
    "40",
    "40",
    "EXISTING",
    "EXISTING",
    "EXISTING",
    "EXISTING",
    "EXISTING",
    "EXISTING",
    "-1",
    "8",
    "EXISTING",
    "180",
    "NONE",
    "NONE",
    "true",
    "false",
    "BESIDE",
    "0",
    "0"
  };

  public static final String[] KEYS, DEFAULTS;
  private static final Map<String, String> DEFAULT_LOOKUP = new LinkedHashMap<>();
  private static volatile long revision;

  private static void add(String key, String value) {
    DEFAULT_LOOKUP.put(key, value);
  }

  static {
    for (int i = 0; i < BASE_KEYS.length; i++) add(BASE_KEYS[i], BASE_DEFAULTS[i]);
    add("presentationPreset", "CUSTOM");
    add("nametagWaiting", "true");
    add("nametagGame", "true");
    add("nametagElsewhere", "false");
    add("nametagSpectators", "false");
    add("friendsAfterParty", "true");
    add("friendHighlight", "false");
    add("friendColor", "55FFFF");
    add("partyWindowMs", "1600");
    add("profileName", "Custom");
    add("tabMatchOverview", "false");
    add("tabSnapGrid", "4");
    for (String panel : new String[] {"tab", "hud"}) {
      for (String suffix :
          new String[] {
            "Header",
            "Heads",
            "RankWithName",
            "AlternatingRows",
            "Dividers",
            "Shadow",
            "Opacity",
            "CornerRadius",
            "Scale",
            "NameWidth",
            "FontSize"
          }) add(panel + suffix, "EXISTING");
      for (String suffix :
          new String[] {
            "TextColor", "SecondaryColor", "HeaderColor", "PanelColor", "RowColor", "SeparatorColor"
          }) add(panel + suffix, "EXISTING");
      add(panel + "FriendHighlight", "INHERIT");
      add(panel + "FriendColor", "INHERIT");
      add(panel + "ImageOpacity", "18");
      add(panel + "ImageScale", "STRETCH");
      add(panel + "ImageAlign", "CENTER");
      add(panel + "ImageZoom", "100");
      add(panel + "ShowAnimation", "INHERIT");
      add(panel + "ShowMs", "INHERIT");
      add(panel + "HideAnimation", "NONE");
      add(panel + "HideMs", "180");
      add(panel + "EntryAnimation", "EXISTING");
      add(panel + "ResizeAnimation", "EXISTING");
      add(panel + "EntryMs", "EXISTING");
      add(panel + "ResizeMs", "EXISTING");
      add(panel + "ExitAnimation", "NONE");
      add(panel + "ExitMs", "180");
      add(panel + "PingUnits", "true");
      for (String phase : new String[] {"Waiting", "Game"})
        for (String suffix :
            new String[] {
              "Limit",
              "Sort",
              "Font",
              "FontSize",
              "Columns",
              "Spacing",
              "Header",
              "Heads",
              "RankWithName",
              "AlternatingRows",
              "Dividers",
              "Shadow",
              "Opacity",
              "CornerRadius",
              "Scale",
              "NameWidth",
              "Background",
              "ImageOpacity",
              "ImageScale",
              "ImageAlign",
              "ImageZoom",
              "ShowAnimation",
              "ShowMs",
              "HideAnimation",
              "HideMs",
              "EntryAnimation",
              "ResizeAnimation",
              "EntryMs",
              "ResizeMs",
              "ExitAnimation",
              "ExitMs",
              "PingUnits",
              "TextColor",
              "SecondaryColor",
              "HeaderColor",
              "PanelColor",
              "RowColor",
              "SeparatorColor",
              "FriendHighlight",
              "FriendColor"
            }) add(panel + phase + suffix, "INHERIT");
    }
    KEYS = DEFAULT_LOOKUP.keySet().toArray(new String[0]);
    DEFAULTS = DEFAULT_LOOKUP.values().toArray(new String[0]);
    try {
      File previous = new File("config/velvetg-lean-v2.properties");
      File source =
          FILE.isFile()
              ? FILE
              : previous.isFile() ? previous : new File("config/velvetg-lean.properties");
      if (source.isFile() && source.length() <= 65536)
        try (InputStream in = new FileInputStream(source)) {
          P.load(in);
        }
    } catch (IOException | IllegalArgumentException ignored) {
    }
  }

  private LeanSettings() {}

  public static synchronized String get(String k) {
    String fallback = DEFAULT_LOOKUP.get(k);
    return fallback == null ? "" : P.getProperty(k, fallback);
  }

  public static boolean on(String k) {
    if (k.equals("headLayers") && on("maximumFPS")) return false;
    if (k.equals("maximumFPS")) {
      String preset = get("presentationPreset");
      if (!preset.equals("CUSTOM")) return preset.equals("MAXIMUM_FPS");
    }
    String v = get(k);
    return "true".equalsIgnoreCase(v)
        || (!"false".equalsIgnoreCase(v) && "true".equals(DEFAULT_LOOKUP.get(k)));
  }

  public static String animation() {
    String preset = get("presentationPreset");
    return preset.equals("BALANCED") || preset.equals("MAXIMUM_FPS")
        ? "NONE"
        : preset.equals("VISUAL") ? "SLIDE" : get("animation");
  }

  public static int number(String k, int low, int high) {
    try {
      return Math.max(low, Math.min(high, Integer.parseInt(get(k))));
    } catch (NumberFormatException e) {
      return low;
    }
  }

  public static long revision() {
    return revision;
  }

  private static final class PanelSnapshot {
    final long revision;
    final Map<String, String>[] values;

    PanelSnapshot(long revision, Map<String, String>[] values) {
      this.revision = revision;
      this.values = values;
    }
  }

  private static volatile PanelSnapshot panels;

  public static String panel(boolean hud, String suffix, String phase) {
    int slot = (hud ? 3 : 0) + ("ACTIVE".equals(phase) ? 2 : "WAITING".equals(phase) ? 1 : 0);
    PanelSnapshot snapshot = panels;
    if (snapshot == null || snapshot.revision != revision) snapshot = refreshPanels();
    String value = snapshot.values[slot].get(suffix);
    return value == null ? "" : value;
  }

  @SuppressWarnings("unchecked")
  private static synchronized PanelSnapshot refreshPanels() {
    if (panels != null && panels.revision == revision) return panels;
    Map<String, String>[] resolved = new Map[6];
    for (int slot = 0; slot < 6; slot++) {
      Map<String, String> values = new HashMap<>();
      String prefix = slot >= 3 ? "hud" : "tab";
      String scene = slot % 3 == 1 ? "Waiting" : slot % 3 == 2 ? "Game" : "";
      for (String key : KEYS)
        if (key.startsWith(prefix) && !key.contains("Waiting") && !key.contains("Game")) {
          String suffix = key.substring(prefix.length());
          String inherited = prefix + scene + suffix;
          String value =
              !scene.isEmpty() && DEFAULT_LOOKUP.containsKey(inherited)
                  ? get(inherited)
                  : "INHERIT";
          values.put(suffix, value.equals("INHERIT") ? get(key) : value);
        }
      resolved[slot] = Collections.unmodifiableMap(values);
    }
    return panels = new PanelSnapshot(revision, resolved);
  }

  public static synchronized void set(String k, String v) throws IOException {
    Map<String, String> change = new LinkedHashMap<>();
    change.put(k, v);
    setAll(change);
  }

  public static synchronized void setAll(Map<String, String> changes) throws IOException {
    for (Map.Entry<String, String> e : changes.entrySet()) validate(e.getKey(), e.getValue());
    Properties before = new Properties();
    before.putAll(P);
    for (Map.Entry<String, String> e : changes.entrySet()) P.setProperty(e.getKey(), e.getValue());
    try {
      save(FILE);
      revision++;
    } catch (IOException e) {
      P.clear();
      P.putAll(before);
      throw e;
    }
  }

  private static void validate(String k, String v) throws IOException {
    if (!DEFAULT_LOOKUP.containsKey(k)
        || v == null
        || v.length() > 512
        || !v.matches("[A-Za-z0-9_,. /-]*")) throw new IOException("Unknown or invalid setting");
    if (v.equals("INHERIT")
        && ("INHERIT".equals(DEFAULT_LOOKUP.get(k))
            || k.contains("Waiting")
            || k.contains("Game")
            || k.endsWith("ShowAnimation")
            || k.endsWith("ShowMs"))) return;
    String base = k.replace("Waiting", "").replace("Game", "");
    if (base.equals("presentationPreset")) {
      if (!v.matches("CUSTOM|MAXIMUM_FPS|BALANCED|VISUAL")) throw new IOException("Invalid preset");
      return;
    }
    if (v.equals("EXISTING") && ("EXISTING".equals(DEFAULT_LOOKUP.get(base)))) return;
    if (base.endsWith("Columns")) {
      if (!v.equals("EXISTING") && !v.toUpperCase(Locale.ROOT).matches("[A-Z_]+(?:,[A-Z_]+)*"))
        throw new IOException("Invalid columns");
      return;
    }
    if (base.endsWith("Font")) {
      if (!v.matches(
              "EXISTING|MINECRAFT|REGULAR|MEDIUM|POPPINS|[A-Za-z0-9 _.-]{1,80}\\.(?:ttf|otf)")
          || v.contains("..")) throw new IOException("Invalid font");
      return;
    }
    if (base.endsWith("Background")) {
      if (!v.matches("NONE|RANDOM|CATBOX-[a-z0-9]{6}\\.png|[A-Za-z0-9 _.-]{1,80}\\.png")
          || v.contains("..")) throw new IOException("Invalid image");
      return;
    }
    if (base.endsWith("Sort") || base.equals("nametagStat")) {
      if (!v.matches("EXISTING|FKDR|WLR|HWS|LV|FK|WINS|BEDS"))
        throw new IOException("Invalid statistic");
      return;
    }
    if (base.endsWith("EntryAnimation") || base.endsWith("ResizeAnimation")) {
      if (!v.matches("EXISTING|NONE")) throw new IOException("Invalid motion setting");
      return;
    }
    if (base.endsWith("ExitAnimation")) {
      if (!v.matches("NONE|FADE|SLIDE")) throw new IOException("Invalid exit motion");
      return;
    }
    if (base.endsWith("HideAnimation")) {
      if (!v.matches("NONE|FADE|SLIDE|ZOOM|BOUNCE|POP"))
        throw new IOException("Invalid hide animation");
      return;
    }
    if (base.equals("animation") || base.endsWith("ShowAnimation")) {
      if (!v.matches("EXISTING|NONE|SLIDE|ZOOM|BOUNCE|POP"))
        throw new IOException("Invalid animation");
      return;
    }
    if (base.endsWith("ImageScale")) {
      if (!v.matches("STRETCH|COVER|CONTAIN")) throw new IOException("Invalid image scale");
      return;
    }
    if (base.endsWith("ImageAlign")) {
      if (!v.matches("CENTER|TOP|BOTTOM|LEFT|RIGHT|TOP_LEFT|TOP_RIGHT|BOTTOM_LEFT|BOTTOM_RIGHT"))
        throw new IOException("Invalid alignment");
      return;
    }
    if (base.equals("nametagPlacement")) {
      if (!v.matches("ABOVE|BESIDE")) throw new IOException("Invalid placement");
      return;
    }
    if (base.equals("profileName")) {
      if (!v.matches("[A-Za-z0-9_-]{1,40}")) throw new IOException("Invalid profile name");
      return;
    }
    if (base.endsWith("FriendHighlight")) {
      if (!v.matches("true|false")) throw new IOException("Expected true or false");
      return;
    }
    if (base.endsWith("Color")) {
      if (!v.matches("[A-Fa-f0-9]{6}")) throw new IOException("Invalid color");
      return;
    }
    if (base.equals("friendColor")) {
      if (!v.matches("[A-Fa-f0-9]{6}")) throw new IOException("Invalid color");
      return;
    }
    String d = DEFAULT_LOOKUP.get(base);
    if ("true".equals(d)
        || "false".equals(d)
        || base.matches(
            "(?:tab|hud)(?:Header|Heads|RankWithName|AlternatingRows|Dividers|Shadow)")) {
      if (!v.matches("true|false")) throw new IOException("Expected true or false");
      return;
    }
    int lo = 0, hi = 10000;
    if (base.endsWith("Limit")) {
      lo = 1;
      hi = 40;
    } else if (base.endsWith("Spacing")) {
      hi = 30;
    } else if (base.endsWith("Opacity")) {
      hi = 100;
    } else if (base.endsWith("Scale")) {
      lo = 50;
      hi = 150;
    } else if (base.endsWith("ImageZoom")) {
      lo = 25;
      hi = 400;
    } else if (base.endsWith("CornerRadius")) {
      hi = 20;
    } else if (base.endsWith("NameWidth")) {
      lo = 40;
      hi = 300;
    } else if (base.endsWith("FontSize")) {
      lo = 6;
      hi = 20;
    } else if (base.endsWith("Ms")) {
      lo = 50;
      hi = 5000;
    } else if (base.equals("tabSnapGrid")) {
      lo = 1;
      hi = 32;
    } else if (base.equals("tabX")) {
      lo = -1;
    }
    try {
      int n = Integer.parseInt(v);
      if (n < lo || n > hi) throw new NumberFormatException();
    } catch (NumberFormatException e) {
      throw new IOException("Number outside allowed range");
    }
  }

  public static void preset(String name) throws IOException {
    // A reversible presentation layer; no custom appearance values are overwritten.
    set("presentationPreset", name);
  }

  private static void save(File f) throws IOException {
    Path p = f.toPath();
    Files.createDirectories(p.toAbsolutePath().getParent());
    Path t = Files.createTempFile(p.toAbsolutePath().getParent(), "lean-", ".tmp");
    try {
      try (OutputStream o = Files.newOutputStream(t)) {
        Properties safe = new Properties();
        if (f.equals(FILE))
          safe.putAll(P); // Preserve unknown local settings; exports remain allowlisted.
        for (String k : KEYS) safe.setProperty(k, get(k));
        safe.store(o, "VelvetG Lean additive non-sensitive settings");
      }
      try {
        Files.move(t, p, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(t, p, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(t);
    }
  }

  public static synchronized void exportSettings() throws IOException {
    save(new File("config/velvetg-lean-export.properties"));
  }

  public static String hint(String key) {
    String k = key.replace("Waiting", "").replace("Game", "");
    String prefix = key.contains("Waiting") || key.contains("Game") ? "INHERIT or " : "";
    if (k.endsWith("Font")) return prefix + "EXISTING / MINECRAFT / POPPINS / local font.ttf";
    if (k.endsWith("Sort") || k.equals("nametagStat"))
      return prefix + "EXISTING / FKDR / WLR / HWS / LV / FK / WINS / BEDS";
    if (k.endsWith("ImageScale")) return prefix + "STRETCH / COVER / CONTAIN";
    if (k.endsWith("ImageAlign"))
      return prefix + "CENTER / TOP / BOTTOM / LEFT / RIGHT / TOP_LEFT ...";
    if (k.endsWith("Color")) return prefix + "EXISTING or six RGB hex digits, e.g. 55FFFF";
    if (k.endsWith("ExitAnimation")) return prefix + "NONE / FADE / SLIDE";
    if (k.endsWith("Animation") || k.equals("animation"))
      return prefix + "INHERIT / EXISTING / NONE / SLIDE / ZOOM / BOUNCE / POP";
    if (k.endsWith("Limit")) return prefix + "1–40 players";
    if (k.endsWith("Opacity")) return prefix + "0–100 percent";
    if (k.endsWith("Scale")) return prefix + "EXISTING or 50–150 percent";
    if (k.endsWith("Ms")) return prefix + "EXISTING / INHERIT or milliseconds, 50–5000";
    if (k.endsWith("Background")) return prefix + "NONE / RANDOM / local.png / CATBOX-abcdef.png";
    if (k.equals("profileName")) return "Profile name: letters, digits, underscore or hyphen";
    return prefix + "Value: " + DEFAULT_LOOKUP.get(key) + " (default)";
  }

  private static File profileFile() throws IOException {
    String name = get("profileName");
    if (!name.matches("[A-Za-z0-9_-]{1,40}")) throw new IOException("Invalid profile name");
    return new File("config/velvetg-lean-profiles", name + ".properties");
  }

  public static synchronized void saveProfile() throws IOException {
    save(profileFile());
  }

  public static synchronized void loadProfile() throws IOException {
    importFrom(profileFile());
  }

  public static synchronized void importSettings() throws IOException {
    importFrom(new File("config/velvetg-lean-export.properties"));
  }

  private static void importFrom(File f) throws IOException {
    if (f.length() > 65536) throw new IOException("Settings too large");
    Properties q = new Properties();
    try (InputStream i = new FileInputStream(f)) {
      q.load(i);
    }
    Map<String, String> changes = new LinkedHashMap<>();
    for (String k : KEYS) {
      String v = q.getProperty(k);
      if (v != null) changes.put(k, v);
    }
    setAll(changes);
  }
}
