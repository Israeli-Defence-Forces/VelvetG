package me.eldodebug.soar.lean;

import java.util.*;
import me.eldodebug.soar.management.nanovg.*;
import me.eldodebug.soar.management.nanovg.font.Font;
import org.lwjgl.nanovg.NanoVG;

/** Owner-thread bounded LRU caches; hits do not allocate measurement keys or arrays. */
public final class TextCache {
  private static final class Key {
    String font, text;
    int size;

    Key set(String f, String t, float s) {
      font = f;
      text = t;
      size = Float.floatToIntBits(s);
      return this;
    }

    public int hashCode() {
      return (font.hashCode() * 31 + size) * 31 + text.hashCode();
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof Key)) return false;
      Key k = (Key) obj;
      return size == k.size && font.equals(k.font) && text.equals(k.text);
    }

    Key copy() {
      Key k = new Key();
      k.font = font;
      k.text = text;
      k.size = size;
      return k;
    }
  }

  private static final Key LOOKUP = new Key();
  private static final LinkedHashMap<Key, Float> WIDTHS =
      new LinkedHashMap<Key, Float>(1024, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Key, Float> e) {
          return size() > 8192;
        }
      };
  private static final LinkedHashMap<Key, Float> CENTERS =
      new LinkedHashMap<Key, Float>(1024, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Key, Float> e) {
          return size() > 4096;
        }
      };
  private static final float[] BOUNDS = new float[4];
  private static long context;

  private static void check(long c) {
    if (c != context) {
      WIDTHS.clear();
      CENTERS.clear();
      context = c;
    }
  }

  public static float width(NanoVGManager n, String s, float size, Font font) {
    check(n.getContext());
    s = s == null ? "" : s;
    LOOKUP.set(font.getName(), s, size);
    Float v = WIDTHS.get(LOOKUP);
    if (v == null) {
      v = n.getTextWidth(s, size, font);
      WIDTHS.put(LOOKUP.copy(), v);
    }
    return v;
  }

  public static float center(NanoVGManager n, String s, float y, float h, float size, Font font) {
    try {
      check(n.getContext());
      s = s == null ? "" : s;
      LOOKUP.set(font.getName(), s, size);
      Float v = CENTERS.get(LOOKUP);
      if (v == null) {
        NanoVG.nvgFontSize(context, size);
        NanoVG.nvgFontFace(context, font.getName());
        NanoVG.nvgTextAlign(context, 17);
        NanoVG.nvgTextBounds(context, 0, 0, s, BOUNDS);
        v = (BOUNDS[1] + BOUNDS[3]) * .5f;
        CENTERS.put(LOOKUP.copy(), v);
      }
      return y + h * .5f - v - size * .5f;
    } catch (RuntimeException e) {
      return y + Math.max(0, (h - size) * .5f);
    }
  }

  public static int size() {
    return WIDTHS.size() + CENTERS.size();
  }
}
