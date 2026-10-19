package me.eldodebug.soar.lean;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import me.eldodebug.soar.scout.*;

/** Same synthetic workload for original/Overhaul1/Overhaul2. Never launches Minecraft. */
public final class ComparisonBench {
  static volatile long sink;

  public static void main(String[] args) throws Exception {
    List<ScoutRow> rows = new ArrayList<>();
    for (int i = 0; i < 40; i++)
      rows.add(new ScoutRow("Player" + i, null, 20, 0xffffff, false, false, null));
    FrameProbe.enabled = false;
    for (int i = 0; i < 200000; i++) sink += TableSupport.prepare(rows, false).size();
    int iterations = 500000;
    long start = System.nanoTime();
    for (int i = 0; i < iterations; i++) sink += TableSupport.prepare(rows, false).size();
    System.out.println("synthetic40RowNs=" + (System.nanoTime() - start) / (double) iterations);
  }
}
