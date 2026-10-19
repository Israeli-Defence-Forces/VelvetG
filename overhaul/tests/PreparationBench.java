package me.eldodebug.soar.lean;

import java.lang.management.ManagementFactory;
import java.util.*;
import me.eldodebug.soar.scout.*;

/** Warmed CPU/allocation experiment only. This does not render, initialize GL, or measure FPS. */
public final class PreparationBench {
  static volatile long sink;
  static final List<ScoutRow> rows = new ArrayList<>();

  static {
    for (int i = 0; i < 40; i++)
      rows.add(new ScoutRow("Player" + i, null, 20, 0xffffff, false, false, null));
  }

  static long batch(String kind) {
    long n = 0;
    if (kind.equals("roster"))
      for (int j = 0; j < 4096; j++) n += TableSupport.prepare(rows, false).size();
    else
      for (int j = 0; j < 4096; j++)
        n +=
            (int) TableSupport.scalar(9, "FontSize")
                + TableSupport.number("EntryMs", 180, 50, 5000);
    sink = n;
    return 4096;
  }

  static double[] measure(String kind, long ms, boolean allocation) {
    com.sun.management.ThreadMXBean bean =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    boolean supported = bean.isThreadAllocatedMemorySupported();
    if (supported && !bean.isThreadAllocatedMemoryEnabled())
      bean.setThreadAllocatedMemoryEnabled(true);
    long thread = Thread.currentThread().getId();
    long allocated = supported ? bean.getThreadAllocatedBytes(thread) : 0;
    long start = System.nanoTime(), until = start + ms * 1000000L, count = 0;
    do {
      count += batch(kind);
    } while (System.nanoTime() < until);
    long end = System.nanoTime(),
        bytes = supported ? bean.getThreadAllocatedBytes(thread) - allocated : 0;
    return new double[] {(end - start) / (double) count, supported ? bytes / (double) count : -1};
  }

  public static void main(String[] args) {
    String kind = args.length == 0 ? "roster" : args[0];
    FrameProbe.enabled = false;
    measure(kind, 1500, false);
    System.out.print("[");
    for (int i = 0; i < 7; i++) {
      double[] m = measure(kind, 250, true);
      if (i > 0) System.out.print(",");
      System.out.printf(Locale.ROOT, "{\"ns\":%.6f,\"bytes\":%.6f}", m[0], m[1]);
    }
    System.out.println("]");
  }
}
