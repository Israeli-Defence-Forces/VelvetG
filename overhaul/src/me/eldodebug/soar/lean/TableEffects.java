package me.eldodebug.soar.lean;

import me.eldodebug.soar.management.nanovg.NanoVGManager;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

public final class TableEffects {
  public static float x, y, scale;
  private static final long[] last = {-2, -2}, opened = new long[2];
  private static long frame;
  private static final ImageLayout image = new ImageLayout();

  public static void nextFrame() {
    frame++;
  }

  public static void prepare(boolean hud, float px, float py, float s, float w, float h) {
    TableSupport.context(hud);
    if (!hud
        && !TabPlacement.active()
        && TableSupport.viewportWidth() > 0
        && TableSupport.viewportHeight() > 0) {
      px = Math.max(0, Math.min(px, Math.max(0, TableSupport.viewportWidth() - w * s)));
      py = Math.max(0, Math.min(py, Math.max(0, TableSupport.viewportHeight() - h * s)));
    }
    x = px;
    y = py;
    scale = s;
    if (!hud) TabPlacement.observed(px, py, w * s, h * s);
    if (LeanSettings.on("maximumFPS") || TabPlacement.active()) return;
    String a = TableSupport.value("ShowAnimation");
    if (a.equals("INHERIT")) a = LeanSettings.get("animation");
    if (a.equals("NONE") || a.equals("EXISTING")) return;
    long now = System.nanoTime();
    int i = hud ? 1 : 0;
    if (last[i] < frame - 1) opened[i] = now;
    last[i] = frame;
    double t =
        Math.max(
            0,
            Math.min(
                1,
                (now - opened[i])
                    / (TableSupport.number(
                            "ShowMs", LeanSettings.number("animationMs", 50, 1500), 50, 5000)
                        * 1000000.0)));
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
    int opacity = TableSupport.number("ImageOpacity", 18, 0, 100);
    if (opacity == 0) return;
    java.io.File f = Backgrounds.file(TableSupport.isHud());
    if (f == null || !AsyncImages.load(n.getContext(), f)) return;
    long dimensions = AsyncImages.dimensions(f);
    image.compute(
        x,
        y,
        w,
        h,
        (int) (dimensions >>> 32),
        (int) dimensions,
        TableSupport.value("ImageScale"),
        TableSupport.value("ImageAlign"),
        TableSupport.number("ImageZoom", 100, 25, 400) / 100f);
    if (image.width <= 0 || image.height <= 0) return;
    NVGPaint paint = NVGPaint.calloc();
    n.save();
    try {
      NanoVG.nvgBeginPath(n.getContext());
      NanoVG.nvgRoundedRect(n.getContext(), x, y, w, h, r);
      NanoVG.nvgImagePattern(
          n.getContext(),
          image.x,
          image.y,
          image.width,
          image.height,
          0,
          AsyncImages.image(f),
          opacity / 100f,
          paint);
      NanoVG.nvgFillPaint(n.getContext(), paint);
      NanoVG.nvgFill(n.getContext());
    } finally {
      n.restore();
      paint.free();
    }
  }
}
