package me.eldodebug.soar.lean;

import java.util.*;
import vgstub.*;

/** Diagnostics refresh once per second; no secret, HTTP call or raw response rendering. */
public final class StatusScreen extends axu {
  private final axu parent;
  private final List<String> lines = new ArrayList<>();
  private long next;

  public StatusScreen(axu parent) {
    this.parent = parent;
  }

  public void b() {
    n.clear();
    n.add(new avs(1, l / 2 - 60, m - 30, 120, 20, "Back"));
  }

  public void a(int x, int y, float partial) {
    long now = System.nanoTime();
    if (now >= next) {
      next = now + 1000000000L;
      lines.clear();
      lines.add("Authentication: " + KeyVault.status());
      lines.add("Endpoint: HTTPS stats.pika-network.net · X-Api-Key");
      lines.add("Modes: ALL_MODES / SOLO / DOUBLES / QUAD");
      lines.add(
          "Automatic key acquisition: " + (LeanSettings.on("autoKey") ? "enabled" : "disabled"));
      for (String line : NativeHooks.apiSummary().split("; ")) lines.add(line);
      lines.add("Profile images/text caches: " + AsyncImages.count() + " / " + TextCache.size());
      lines.add("Profiler: " + (FrameProbe.enabled ? "enabled" : "disabled"));
      lines.add("No key or raw API response is included in this page or exports.");
    }
    c();
    a(q, "VelvetG connection and cache status", l / 2, 17, 0xffffff);
    int row = 42;
    for (String line : lines) {
      if (row > m - 45) break;
      a(q, line, l / 2, row, 0xcccccc);
      row += 16;
    }
    super.a(x, y, partial);
  }

  protected void a(avs button) {
    j.a(parent);
  }

  protected void a(char c, int code) {
    if (code == 1) j.a(parent);
  }

  public boolean d() {
    return false;
  }
}
