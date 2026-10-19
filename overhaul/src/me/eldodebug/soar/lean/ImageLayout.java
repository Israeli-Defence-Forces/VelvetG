package me.eldodebug.soar.lean;

/** Reusable geometry output; no per-frame layout allocation. */
public final class ImageLayout {
  public float x, y, width, height, clipX, clipY, clipWidth, clipHeight;

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
      width = height = clipWidth = clipHeight = 0;
      clipX = px;
      clipY = py;
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
    clipX = Math.max(px, x);
    clipY = Math.max(py, y);
    clipWidth = Math.max(0, Math.min(px + pw, x + width) - clipX);
    clipHeight = Math.max(0, Math.min(py + ph, y + height) - clipY);
  }
}
