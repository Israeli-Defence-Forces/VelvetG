package me.eldodebug.soar.lean;

import java.io.IOException;
import java.util.*;

/** GUI pixels, bounded drag coordinates, no disk writes until explicit Apply. */
public final class TabPlacement {
  private static boolean active, dragging;
  private static float x = 8, y = 8, w = 240, h = 160, offsetX, offsetY;
  private static int screenW, screenH;

  public static boolean active() {
    return active;
  }

  public static float x() {
    return x;
  }

  public static float y() {
    return y;
  }

  public static float width() {
    return w;
  }

  public static float height() {
    return h;
  }

  public static void observed(float px, float py, float pw, float ph) {
    if (!active) {
      x = px;
      y = py;
    }
    w = pw;
    h = ph;
  }

  public static void begin(int sw, int sh) {
    active = true;
    dragging = false;
    screenW = sw;
    screenH = sh;
    clamp();
  }

  public static boolean down(int px, int py) {
    dragging = active && px >= x && px <= x + w && py >= y && py <= y + h;
    offsetX = px - x;
    offsetY = py - y;
    return dragging;
  }

  public static void move(int px, int py) {
    if (!dragging) return;
    int grid = LeanSettings.number("tabSnapGrid", 1, 32);
    x = Math.round((px - offsetX) / grid) * grid;
    y = Math.round((py - offsetY) / grid) * grid;
    clamp();
  }

  private static void clamp() {
    x = Math.max(0, Math.min(x, Math.max(0, screenW - w)));
    y = Math.max(0, Math.min(y, Math.max(0, screenH - h)));
  }

  public static void up() {
    dragging = false;
  }

  public static void cancel() {
    active = false;
    dragging = false;
  }

  public static void save() throws IOException {
    Map<String, String> v = new LinkedHashMap<>();
    v.put("tabX", Integer.toString(Math.round(x)));
    v.put("tabY", Integer.toString(Math.round(y)));
    LeanSettings.setAll(v);
    cancel();
  }
}
