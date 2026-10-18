package me.eldodebug.soar.lean;

import java.nio.file.*;
import java.util.*;
import me.eldodebug.soar.scout.*;

/** Logic microbenchmarks only: no Minecraft, OpenGL, NanoVG, GPU or FPS measurements. */
public final class OfflineBench {
  static volatile long sink;

  public static void main(String[] args) throws Exception {
    List<ScoutRow> rows = new ArrayList<>();
    for (int i = 0; i < 40; i++)
      rows.add(new ScoutRow("Player" + i, null, 20, 0xffffff, false, false, null));
    FrameProbe.enabled = false;
    for (int i = 0; i < 100000; i++) {
      sink += FrameProbe.start();
      TableSupport.prepare(rows, false);
    }
    int n = 200000;
    long begin = System.nanoTime();
    long v = 0;
    for (int i = 0; i < n; i++) v += FrameProbe.start();
    long disabled = System.nanoTime() - begin;
    sink = v;
    begin = System.nanoTime();
    for (int i = 0; i < n; i++) sink += TableSupport.prepare(rows, false).size();
    long roster = System.nanoTime() - begin;
    List<String> lines =
        Arrays.asList(
            "Java " + System.getProperty("java.version"),
            "Iterations " + n,
            "Disabled profiler start ns/call " + disabled / (double) n,
            "40-row preparation ns/call " + roster / (double) n,
            "These are synthetic CPU microbenchmarks, not game render overhead or FPS. No baseline"
                + " FPS comparison was run.");
    for (String s : lines) System.out.println(s);
    Files.write(Paths.get("offline-benchmark.txt"), lines, java.nio.charset.StandardCharsets.UTF_8);
  }
}
