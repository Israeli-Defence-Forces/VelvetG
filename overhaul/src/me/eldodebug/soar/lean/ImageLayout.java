package me.eldodebug.soar.lean;

/** Reusable geometry output; no per-frame layout allocation. */
public final class ImageLayout {
  public float x, y, width, height;

  public void compute(
      float px,
      float py,
      float pw,
      float ph,
      int iw,
      int ih,
      String mode,
      String align,
      float zoom) {
    if (iw < 1 || ih < 1 || pw <= 0 || ph <= 0) {
      x = px;
      y = py;
      width = height = 0;
      return;
    }
    if (mode.equals("STRETCH")) {
      width = pw * zoom;
      height = ph * zoom;
    } else {
      float scale =
          (mode.equals("CONTAIN") ? Math.min(pw / iw, ph / ih) : Math.max(pw / iw, ph / ih)) * zoom;
      width = iw * scale;
      height = ih * scale;
    }
    x = px + (pw - width) * .5f;
    y = py + (ph - height) * .5f;
    if (align.contains("LEFT")) x = px;
    if (align.contains("RIGHT")) x = px + pw - width;
    if (align.contains("TOP")) y = py;
    if (align.contains("BOTTOM")) y = py + ph - height;
  }
}
