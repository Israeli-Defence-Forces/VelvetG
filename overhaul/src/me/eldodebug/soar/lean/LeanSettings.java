package me.eldodebug.soar.lean;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Additive settings. Existing Glide and VelvetG settings are never rewritten. */
public final class LeanSettings {
  private static final Properties P = new Properties();
  private static final File FILE = new File("config/velvetg-lean.properties");
  public static final String[] KEYS = {
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
  public static final String[] DEFAULTS = {
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

  private static final Map<String, String> DEFAULT_LOOKUP = new HashMap<>();

  static {
    for (int i = 0; i < KEYS.length; i++) DEFAULT_LOOKUP.put(KEYS[i], DEFAULTS[i]);
    try {
      if (FILE.isFile() && FILE.length() <= 65536)
        try (InputStream in = new FileInputStream(FILE)) {
          P.load(in);
        }
    } catch (IOException ignored) {
    }
  }

  private LeanSettings() {}

  public static synchronized String get(String k) {
    String fallback = DEFAULT_LOOKUP.get(k);
    return fallback == null ? "" : P.getProperty(k, fallback);
  }

  public static boolean on(String k) {
    return "true".equalsIgnoreCase(get(k));
  }

  public static int number(String k, int low, int high) {
    try {
      return Math.max(low, Math.min(high, Integer.parseInt(get(k))));
    } catch (NumberFormatException e) {
      return low;
    }
  }

  public static synchronized void set(String k, String v) throws IOException {
    boolean allowed = false;
    for (String a : KEYS) if (a.equals(k)) allowed = true;
    if (!allowed || v == null || v.length() > 512 || !v.matches("[A-Za-z0-9_,. /-]*"))
      throw new IOException("Invalid setting");
    P.setProperty(k, v);
    save(FILE);
  }

  private static void save(File f) throws IOException {
    Path p = f.toPath();
    Files.createDirectories(p.toAbsolutePath().getParent());
    Path t = Files.createTempFile(p.toAbsolutePath().getParent(), "lean-", ".tmp");
    try {
      try (OutputStream o = Files.newOutputStream(t)) {
        Properties safe = new Properties();
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

  public static synchronized void importSettings() throws IOException {
    File f = new File("config/velvetg-lean-export.properties");
    if (f.length() > 65536) throw new IOException("Settings too large");
    Properties q = new Properties();
    try (InputStream i = new FileInputStream(f)) {
      q.load(i);
    }
    for (String k : KEYS) {
      String v = q.getProperty(k);
      if (v != null && v.length() <= 512 && v.matches("[A-Za-z0-9_,. /-]*")) P.setProperty(k, v);
    }
    save(FILE);
  }
}
