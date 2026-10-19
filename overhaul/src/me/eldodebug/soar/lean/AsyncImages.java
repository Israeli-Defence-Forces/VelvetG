package me.eldodebug.soar.lean;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryUtil;

/** All map/GPU operations run on the owning render thread; only decoding runs on the worker. */
public final class AsyncImages {
  private static final ThreadPoolExecutor WORK =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(12),
          r -> {
            Thread t = new Thread(r, "VelvetG-image-decode");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private static final LinkedHashMap<String, Entry> CACHE = new LinkedHashMap<>(32, .75f, true);
  private static long bytes, context;
  private static volatile Thread owner;
  private static boolean uploaded;

  private static final class Entry {
    volatile Pixels ready;
    volatile boolean done, stale;
    int id, width, height;
    long bytes, used;
    volatile long stamp;
    long checkAt;
    File file;
  }

  private static final class Pixels {
    final int w, h;
    final byte[] data;

    Pixels(BufferedImage image) {
      w = image.getWidth();
      h = image.getHeight();
      data = new byte[w * h * 4];
      int i = 0;
      for (int y = 0; y < h; y++)
        for (int x = 0; x < w; x++) {
          int c = image.getRGB(x, y);
          data[i++] = (byte) (c >> 16);
          data[i++] = (byte) (c >> 8);
          data[i++] = (byte) c;
          data[i++] = (byte) (c >> 24);
        }
      image.flush();
    }
  }

  private static String key(File f) {
    return f.getAbsolutePath();
  }

  public static void frame() {
    if (owner == null) owner = Thread.currentThread();
    uploaded = false;
    if (owner != null && Thread.currentThread() == owner) {
      long now = System.nanoTime();
      evict(now, false);
      for (Entry e : CACHE.values())
        if (e.done && now >= e.checkAt) {
          e.checkAt = now + 15000000000L;
          try {
            WORK.execute(
                () -> {
                  long stamp = e.file.lastModified() ^ e.file.length();
                  if (stamp != e.stamp) e.stale = true;
                });
          } catch (RejectedExecutionException ignored) {
            e.checkAt = now + 1000000000L;
          }
          break;
        }
    }
  }

  public static boolean load(long ctx, File file) {
    if (file == null || ctx == 0) return false;
    if (owner == null || Thread.currentThread() != owner) return false;
    if (context == 0) context = ctx;
    if (ctx != context) return false;
    String k = key(file);
    Entry e = CACHE.get(k);
    if (e != null && e.stale) {
      remove(ctx, file);
      return load(ctx, file);
    }
    if (e == null) {
      evict(System.nanoTime(), true);
      if (CACHE.size() >= 24) return false;
      e = new Entry();
      e.file = file;
      e.checkAt = System.nanoTime() + 15000000000L;
      e.used = System.nanoTime();
      CACHE.put(k, e);
      final Entry entry = e;
      try {
        WORK.execute(
            () -> {
              try {
                entry.ready = new Pixels(decode(file));
                entry.stamp = file.lastModified() ^ file.length();
              } catch (IOException | RuntimeException ignored) {
              } finally {
                entry.done = true;
              }
            });
      } catch (RejectedExecutionException x) {
        CACHE.remove(k);
      }
      return false;
    }
    e.used = System.nanoTime();
    if (e.id > 0) return true;
    if (e.done && e.ready != null && !uploaded) {
      Pixels image = e.ready;
      e.ready = null;
      int w = image.w, h = image.h;
      ByteBuffer pixels = MemoryUtil.memAlloc(w * h * 4);
      try {
        pixels.put(image.data);
        pixels.flip();
        e.id = NanoVG.nvgCreateImageRGBA(ctx, w, h, 0, pixels);
        if (e.id > 0) {
          e.width = w;
          e.height = h;
          e.bytes = (long) w * h * 4;
          bytes += e.bytes;
        }
        uploaded = true;
      } finally {
        MemoryUtil.memFree(pixels);
      }
      evict(System.nanoTime(), false);
    }
    return e.id > 0;
  }

  static BufferedImage decode(File f) throws IOException {
    if (!f.isFile() || f.length() > 32 * 1024 * 1024)
      throw new IOException("Image unavailable or too large");
    try (ImageInputStream in = ImageIO.createImageInputStream(f)) {
      if (in == null) throw new IOException("Unreadable image");
      Iterator<ImageReader> it = ImageIO.getImageReaders(in);
      if (!it.hasNext()) throw new IOException("Invalid image");
      ImageReader r = it.next();
      try {
        r.setInput(in, true, true);
        int w = r.getWidth(0), h = r.getHeight(0);
        if (w < 1 || h < 1 || w > 16384 || h > 16384 || (long) w * h > 16777216)
          throw new IOException("Image dimensions too large");
        ImageReadParam param = r.getDefaultReadParam();
        int sample = Math.max(1, Math.max((w + 1023) / 1024, (h + 1023) / 1024));
        param.setSourceSubsampling(sample, sample, 0, 0);
        BufferedImage src = r.read(0, param);
        return src;
      } finally {
        r.dispose();
      }
    }
  }

  private static void evict(long now, boolean slot) {
    Iterator<Map.Entry<String, Entry>> it = CACHE.entrySet().iterator();
    while (it.hasNext()) {
      Entry e = it.next().getValue();
      if (bytes > 64L * 1024 * 1024
          || (slot && CACHE.size() >= 24)
          || now - e.used > 120000000000L) {
        if (e.id > 0) NanoVG.nvgDeleteImage(context, e.id);
        bytes -= e.bytes;
        e.ready = null;
        it.remove();
      }
    }
  }

  public static long dimensions(File f) {
    if (Thread.currentThread() != owner) return 0;
    Entry e = CACHE.get(key(f));
    return e == null ? 0 : ((long) e.width << 32) | (e.height & 0xffffffffL);
  }

  public static int image(File f) {
    if (Thread.currentThread() != owner) return 0;
    Entry e = CACHE.get(key(f));
    return e == null ? 0 : e.id;
  }

  public static void remove(long ctx, File f) {
    if (Thread.currentThread() != owner || ctx != context) return;
    Entry e = CACHE.remove(key(f));
    if (e != null) {
      if (e.id > 0) NanoVG.nvgDeleteImage(ctx, e.id);
      bytes -= e.bytes;
    }
  }

  public static int count() {
    return CACHE.size();
  }

  public static long bytes() {
    return bytes;
  }

  public static void shutdown() {
    WORK.shutdownNow();
    if (Thread.currentThread() == owner) {
      for (Entry e : CACHE.values()) {
        if (e.id > 0) NanoVG.nvgDeleteImage(context, e.id);
      }
      CACHE.clear();
      bytes = 0;
    }
  }
}
