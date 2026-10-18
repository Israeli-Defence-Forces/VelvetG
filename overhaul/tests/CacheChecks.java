package me.eldodebug.soar.lean;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Font;
import me.eldodebug.soar.scout.*;

public final class CacheChecks {
  static int checks;

  static void ok(boolean v, String m) {
    if (!v) throw new AssertionError(m);
    checks++;
  }

  public static class MockMetrics extends NanoVGManager {
    public int calls;
    public long ctx = 1;

    public long getContext() {
      return ctx;
    }

    public float getTextWidth(String text, float size, Font font) {
      calls++;
      return text.length() * size;
    }
  }

  public static void main(String[] args) throws Exception {
    Class<?> unsafe = Class.forName("sun.misc.Unsafe");
    Field instance = unsafe.getDeclaredField("theUnsafe");
    instance.setAccessible(true);
    Object u = instance.get(null);
    MockMetrics n =
        (MockMetrics)
            unsafe.getMethod("allocateInstance", Class.class).invoke(u, MockMetrics.class);
    n.ctx = 1;
    Font regular = new Font("mock", null), other = new Font("other", null);
    ok(TextCache.width(n, "Tester", 8, regular) == 48, "measured value");
    for (int i = 0; i < 1000; i++) TextCache.width(n, "Tester", 8, regular);
    ok(n.calls == 1, "warm measurements reused");
    TextCache.width(n, "Tester", 9, regular);
    ok(n.calls == 2, "size invalidation");
    TextCache.width(n, "Tester", 9, other);
    ok(n.calls == 3, "font invalidation");
    n.ctx = 2;
    TextCache.width(n, "Tester", 9, other);
    ok(n.calls == 4, "context invalidation");
    for (int i = 0; i < 15000; i++) TextCache.width(n, "Player" + i, 8, regular);
    ok(TextCache.size() <= 8192, "bounded width cache");
    Path dir = Files.createTempDirectory("gallery-index");
    ok(GalleryIndex.discover(dir.toFile()).length == 0, "empty gallery");
    for (int i = 0; i < 500; i++)
      Files.write(dir.resolve("screen-" + i + ".png"), new byte[] {1, 2, 3});
    Files.write(dir.resolve("upper.PNG"), new byte[] {1});
    Files.write(dir.resolve("ignore.txt"), new byte[] {1});
    ok(
        GalleryIndex.discover(dir.toFile()).length == 501,
        "hundreds discovered without decoding corrupt PNGs");
    ok(
        GalleryIndex.discover(dir.resolve("ignore.txt").toFile()) == null,
        "inaccessible/non-directory listing");
    ok(Files.list(dir).count() == 502, "no screenshots deleted");
    me.eldodebug.soar.management.screenshot.ScreenshotManager manager =
        new me.eldodebug.soar.management.screenshot.ScreenshotManager();
    ok(manager.getScreenshots().isEmpty(), "startup constructor does no discovery or texture work");
    JsonObject json =
        new JsonParser()
            .parse(
                "{\"username\":\"Tester\",\"rank\":{\"level\":8},\"clan\":{\"name\":\"Example\"},\"friends\":[{\"username\":\"Friend\"},{\"username\":\"bad"
                    + " name\"}]}")
            .getAsJsonObject();
    ScoutStats.Builder builder = new ScoutStats.Builder("Tester");
    new PikaParser().profile(json, builder);
    ProfileExtras.local("Tester");
    ok(ProfileExtras.guild("Tester").equals("Example"), "guild enrichment shares profile response");
    ok(ProfileExtras.friend("Friend"), "cached friends");
    ok(!ProfileExtras.friend("bad name"), "invalid friend filtered");
    ProfileExtras.clear();
    ok(!ProfileExtras.friend("Friend"), "disconnect social cleanup");
    ok(PikaApiClient.retryAfter("2", 0) == 2000, "retry-after seconds");
    ok(PikaApiClient.retryAfter("-100", 0) >= 1000, "cooldown floor");
    ok(
        PikaApiClient.retryAfter("100000", 0) <= 86400000,
        "cooldown ceiling (respect server up to one day)");
    System.out.println(
        "PASS "
            + checks
            + " cache invalidation, gallery startup/discovery, enrichment and cooldown checks"
            + " (graphics mocked)");
  }
}
