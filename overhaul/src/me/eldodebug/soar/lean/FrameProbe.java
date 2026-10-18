package me.eldodebug.soar.lean;

import java.lang.management.*;
import java.util.*;

/** No allocations, clock calls, or MXBean reads on the disabled path. Render thread only. */
public final class FrameProbe {
  public static volatile boolean enabled = LeanSettings.on("profiler");
  private static final long[] frames = new long[4096], tab = new long[4096], hud = new long[4096];
  private static final long[] cpu = new long[4096];
  private static int cn;
  private static long cpuStart;
  private static int fn, tn, hn;
  private static long last;

  public static long start() {
    return enabled ? System.nanoTime() : 0;
  }

  public static void frame() {
    if (!enabled) {
      last = 0;
      return;
    }
    long n = System.nanoTime();
    if (last != 0) frames[fn++ & 4095] = n - last;
    last = n;
  }

  public static void section(boolean h, long start) {
    if (start == 0 || !enabled) return;
    long n = System.nanoTime() - start;
    if (h) hud[hn++ & 4095] = n;
    else tab[tn++ & 4095] = n;
  }

  private static final class Beans {
    static final ThreadMXBean THREAD = ManagementFactory.getThreadMXBean();
  }

  public static void cpuStart() {
    if (enabled && Beans.THREAD.isCurrentThreadCpuTimeSupported())
      cpuStart = Beans.THREAD.getCurrentThreadCpuTime();
  }

  public static void cpuEnd() {
    if (enabled && cpuStart > 0) {
      cpu[cn++ & 4095] = Math.max(0, Beans.THREAD.getCurrentThreadCpuTime() - cpuStart);
      cpuStart = 0;
    }
  }

  public static String summary() {
    return "Frame interval "
        + stats(frames, fn)
        + "; main-thread CPU "
        + stats(cpu, cn)
        + "; TAB CPU "
        + stats(tab, tn)
        + "; HUD CPU "
        + stats(hud, hn)
        + "; "
        + gc();
  }

  private static String stats(long[] a, int count) {
    int n = Math.min(4096, count);
    if (n == 0) return "no samples";
    long[] b = Arrays.copyOf(a, n);
    Arrays.sort(b);
    double sum = 0;
    for (long v : b) sum += v;
    return String.format(
        Locale.ROOT,
        "n=%d avg=%.3f p95=%.3f p99=%.3f ms",
        n,
        sum / n / 1e6,
        b[(int) Math.ceil(n * .95) - 1] / 1e6,
        b[(int) Math.ceil(n * .99) - 1] / 1e6);
  }

  private static String gc() {
    long count = 0, ms = 0;
    for (GarbageCollectorMXBean g : ManagementFactory.getGarbageCollectorMXBeans()) {
      count += Math.max(0, g.getCollectionCount());
      ms += Math.max(0, g.getCollectionTime());
    }
    return "GC lifetime "
        + count
        + " / "
        + ms
        + " ms; threads "
        + ManagementFactory.getThreadMXBean().getThreadCount();
  }

  public static void export() {
    try {
      java.nio.file.Files.createDirectories(java.nio.file.Paths.get("config"));
      java.nio.file.Files.write(
          java.nio.file.Paths.get("config/velvetg-performance.txt"),
          Arrays.asList(
              summary(),
              InitProbe.summary(),
              "Image cache "
                  + AsyncImages.count()
                  + " / "
                  + AsyncImages.bytes()
                  + " GPU bytes; text metrics "
                  + TextCache.size(),
              NativeHooks.apiSummary()),
          java.nio.charset.StandardCharsets.UTF_8);
    } catch (java.io.IOException ignored) {
    }
  }
}
