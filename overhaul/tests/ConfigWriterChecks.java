package me.eldodebug.soar.lean;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ConfigWriterChecks {
  public static void main(String[] args) throws Exception {
    File f = Files.createTempDirectory("settings-save").resolve("settings.properties").toFile();
    Properties p = new Properties();
    p.setProperty("unrecognizedLegacyControl", "日本語");
    for (int i = 0; i < 1000; i++) {
      p.setProperty("slider", Integer.toString(i));
      ConfigWriter.enqueue(p, f);
    }
    if (ConfigWriter.queued() > 1) throw new AssertionError("Unbounded delayed save queue");
    ConfigWriter.shutdown();
    if (ConfigWriter.writes() != 1) throw new AssertionError("Slider drag wrote more than once");
    Properties actual = new Properties();
    try (Reader r = Files.newBufferedReader(f.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
      actual.load(r);
    }
    if (!"999".equals(actual.getProperty("slider"))
        || !"日本語".equals(actual.getProperty("unrecognizedLegacyControl")))
      throw new AssertionError("Settings lost or reset");
    System.out.println(
        "PASS 3 coalesced-write, shutdown-flush and unknown-setting preservation checks; 1000"
            + " changes produced one atomic write");
  }
}
