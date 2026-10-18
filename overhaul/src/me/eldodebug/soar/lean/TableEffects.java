package me.eldodebug.soar.lean;

import me.eldodebug.soar.management.nanovg.NanoVGManager;

public final class TableEffects {
  public static float x, y, scale;
  private static final long[] last = new long[2], opened = new long[2];

  public static void prepare(boolean hud, float px, float py, float s, float w, float h) {
    TableSupport.context(hud);
    x = px;
    y = py;
    scale = s;
    if (LeanSettings.on("maximumFPS")) return;
    String a = LeanSettings.get("animation");
    if (a.equals("NONE") || a.equals("EXISTING")) return;
    long now = System.nanoTime();
    int i = hud ? 1 : 0;
    if (now - last[i] > 300000000L) opened[i] = now;
    last[i] = now;
    double t =
        Math.max(
            0,
            Math.min(
                1, (now - opened[i]) / (LeanSettings.number("animationMs", 50, 1500) * 1000000.0)));
    float ease = (float) (1 - Math.pow(1 - t, 3));
    if (a.equals("SLIDE")) y += 16 * (1 - ease);
    if (a.equals("ZOOM") || a.equals("BOUNCE") || a.equals("POP")) {
      float z = .9f + .1f * ease;
      if (a.equals("BOUNCE") || a.equals("POP")) {
        double k = t - 1;
        float back = (float) (1 + 2.70158 * k * k * k + 1.70158 * k * k);
        z = (a.equals("POP") ? .7f : .9f) + (a.equals("POP") ? .3f : .1f) * back;
      }
      scale = s * z;
      x += w * s * (1 - z) * .5f;
      y += h * s * (1 - z) * .5f;
    }
  }

  public static void background(NanoVGManager n, float x, float y, float w, float h, float r) {
    if (LeanSettings.on("maximumFPS")) return;
    java.io.File f = Backgrounds.file(TableSupport.isHud());
    if (f != null) n.drawRoundedImage(f, x, y, w, h, r, .18f);
  }
}
