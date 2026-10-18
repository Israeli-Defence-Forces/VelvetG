package me.eldodebug.soar.lean;

import java.util.*;

/**
 * Startup checkpoints measure elapsed time since the preceding checkpoint, not exclusive CPU time.
 */
public final class InitProbe {
  private static final Map<String, Long> TIMES = new LinkedHashMap<>();
  private static long last;

  public static void begin() {
    TIMES.clear();
    last = System.nanoTime();
  }

  public static void mark(String name) {
    long now = System.nanoTime();
    if (last != 0 && TIMES.size() < 64) TIMES.put(name, now - last);
    last = now;
  }

  public static String summary() {
    StringBuilder b = new StringBuilder("Startup checkpoints ms:");
    for (Map.Entry<String, Long> e : TIMES.entrySet())
      b.append(' ').append(e.getKey()).append('=').append(e.getValue() / 1000000.0);
    return b.toString();
  }
}
