package me.eldodebug.soar.lean;

import me.eldodebug.soar.management.nanovg.NanoVGManager;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

public final class TableEffects {
  public static float x, y, scale;
  public static float alpha = 1;
  private static final VisibilityMotion[] motion = {new VisibilityMotion(), new VisibilityMotion()};
  private static final ImageLayout image = new ImageLayout();
  private static final long[] seen = {-100, -100};
  private static final Object[] phases = new Object[2];
  private static boolean hudWanted = true, saved;
  private static long frame, tabDraw = -100;
  private static NanoVGManager drawing;

  public static void nextFrame() {
    frame++;
    hudWanted = true;
  }

  private static String show() {
    String a = TableSupport.value("ShowAnimation");
    return a.equals("INHERIT") ? LeanSettings.animation() : a;
  }

  public static boolean tabTail(boolean held) {
    if (held || LeanSettings.on("tabPersistent") || TabPlacement.active()) return false;
    TableSupport.context(false);
    if (!TableSupport.animationsEnabled()
        || TableSupport.value("HideAnimation").equals("NONE")
        || frame - tabDraw > 2) {
      motion[0].clear();
      return false;
    }
    return motion[0].update(
            false, System.nanoTime(), TableSupport.number("HideMs", 180, 50, 5000), true)
        > 0;
  }

  /** Used only in the native HUD's hide-while-TAB condition; world/context guards remain native. */
  public static boolean hideHud(boolean held) {
    hudWanted = !held;
    if (!held) return false;
    TableSupport.context(true);
    if (!TableSupport.animationsEnabled() || TableSupport.value("HideAnimation").equals("NONE")) {
      motion[1].clear();
      return true;
    }
    return motion[1].update(
            false, System.nanoTime(), TableSupport.number("HideMs", 180, 50, 5000), true)
        <= 0;
  }

  public static void clear() {
    for (VisibilityMotion m : motion) m.clear();
    tabDraw = -100;
    hudWanted = true;
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
    int slot = hud ? 1 : 0;
    if (frame - seen[slot] > 2 || phases[slot] != TableSupport.phase()) motion[slot].clear();
    seen[slot] = frame;
    phases[slot] = TableSupport.phase();
    x = px;
    y = py;
    scale = s;
    alpha = 1;
    if (!hud) {
      TabPlacement.observed(px, py, w * s, h * s);
      tabDraw = frame;
    }
    if (!TableSupport.animationsEnabled() || TableSupport.preview()) {
      motion[hud ? 1 : 0].clear();
      return;
    }
    boolean wanted = hud ? hudWanted : NativeHooks.tabWanted();
    String a = wanted ? show() : TableSupport.value("HideAnimation");
    boolean animated = !a.equals("NONE") && !a.equals("EXISTING");
    int ms =
        wanted
            ? TableSupport.number("ShowMs", LeanSettings.number("animationMs", 50, 1500), 50, 5000)
            : TableSupport.number("HideMs", 180, 50, 5000);
    float v = motion[hud ? 1 : 0].update(wanted, System.nanoTime(), ms, animated);
    if (!animated) return;
    alpha = v;
    if (a.equals("SLIDE")) y += 16 * (1 - v);
    if (a.equals("ZOOM") || a.equals("BOUNCE") || a.equals("POP")) {
      float z = .9f + .1f * v;
      // Bounded overshoot is cosmetic and has no layout/cache effect.
      if (wanted && (a.equals("BOUNCE") || a.equals("POP")))
        z += .035f * (float) Math.sin(v * Math.PI);
      if (a.equals("POP")) z = .7f + .3f * v;
      scale = s * z;
      x += w * s * (1 - z) * .5f;
      y += h * s * (1 - z) * .5f;
    }
  }

  public static void beginDraw() {
    saved = false;
    if (alpha >= 1) return;
    drawing = me.eldodebug.soar.Glide.getInstance().getNanoVGManager();
    drawing.save();
    saved = true;
    NanoVG.nvgGlobalAlpha(drawing.getContext(), alpha);
  }

  public static void endDraw() {
    if (saved) {
      saved = false;
      drawing.restore();
      drawing = null;
    }
  }

  public static float rowAlpha(float original) {
    return original * alpha;
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
      NanoVG.nvgIntersectScissor(
          n.getContext(), image.clipX, image.clipY, image.clipWidth, image.clipHeight);
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
