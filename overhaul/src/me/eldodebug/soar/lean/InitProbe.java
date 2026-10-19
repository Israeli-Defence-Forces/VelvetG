package me.eldodebug.soar.lean;

import java.util.*;

/** Startup intervals are wall-clock elapsed checkpoints, not exclusive CPU/GPU durations. */
public final class InitProbe {
  private static final Map<String, Long> TIMES = new LinkedHashMap<>();
  private static long last, began, unix;

  public static synchronized void begin() {
    TIMES.clear();
    began = last = System.nanoTime();
    unix = System.currentTimeMillis();
  }

  public static synchronized void glideBegin() {
    if (last == 0) begin();
    mark("glideStart");
  }

  public static synchronized void mark(String name) {
    long now = System.nanoTime();
    if (last != 0 && TIMES.size() < 64 && name.matches("[A-Za-z0-9_$.-]{1,80}"))
      TIMES.put(name, now - last);
    last = now;
  }

  public static synchronized void complete() {
    mark("gameStartReturned");
    Properties p = new Properties();
    p.setProperty("release", "Overhaul3");
    p.setProperty("startedUnixMs", Long.toString(unix));
    p.setProperty("totalMs", Double.toString((last - began) / 1e6));
    double elapsed = 0;
    int index = 0;
    for (Map.Entry<String, Long> e : TIMES.entrySet()) {
      elapsed += e.getValue() / 1e6;
      String prefix = String.format(Locale.ROOT, "stage.%02d.", index++);
      p.setProperty(prefix + "name", e.getKey());
      p.setProperty(prefix + "durationMs", Double.toString(e.getValue() / 1e6));
      p.setProperty(prefix + "elapsedMs", Double.toString(elapsed));
    }
    ConfigWriter.enqueue(p, new java.io.File("config/velvetg-startup.properties"));
  }

  public static synchronized String summary() {
    StringBuilder b = new StringBuilder("Startup checkpoints ms:");
    for (Map.Entry<String, Long> e : TIMES.entrySet())
      b.append(' ').append(e.getKey()).append('=').append(e.getValue() / 1e6);
    return b.toString();
  }
}
