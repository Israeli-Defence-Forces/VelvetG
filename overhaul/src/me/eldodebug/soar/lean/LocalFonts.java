package me.eldodebug.soar.lean;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.nanovg.font.Font;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryUtil;

/** Optional local fonts, at most two registrations per run. Never scans fonts on startup. */
public final class LocalFonts {
  private static final ExecutorService WORK =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "VelvetG-font-read");
            t.setDaemon(true);
            return t;
          });
  private static final Map<String, Slot> SLOTS = new HashMap<>();

  private static final class Slot {
    volatile byte[] bytes;
    Font font;
  }

  public static Font get(String name, Font fallback) {
    if (!name.equals("POPPINS") && !name.matches("[A-Za-z0-9 _.-]{1,80}\\.(ttf|otf)")
        || name.contains("..")) return fallback;
    Slot s = SLOTS.get(name);
    if (s == null) {
      if (SLOTS.size() >= 2) return fallback;
      s = new Slot();
      SLOTS.put(name, s);
      Slot target = s;
      WORK.execute(
          () -> {
            try {
              byte[] data = null;
              if (name.equals("POPPINS")) {
                try (InputStream in =
                        LocalFonts.class
                            .getClassLoader()
                            .getResourceAsStream("assets/velvetg-lean/Poppins-Regular.ttf");
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                  if (in == null) return;
                  byte[] buffer = new byte[8192];
                  int count;
                  while ((count = in.read(buffer)) != -1) {
                    if (out.size() + count > 8 * 1024 * 1024) return;
                    out.write(buffer, 0, count);
                  }
                  data = out.toByteArray();
                }
              } else {
                Path p = Paths.get("config/velvetg-fonts", name);
                long n = Files.size(p);
                if (n > 0 && n <= 8 * 1024 * 1024) data = Files.readAllBytes(p);
              }
              if (data != null) {
                byte[] b = data;
                int magic = ByteBuffer.wrap(b).getInt();
                if (magic == 0x00010000 || magic == 0x4f54544f) target.bytes = b;
              }
            } catch (IOException | RuntimeException ignored) {
            }
          });
      return fallback;
    }
    if (s.font != null) return s.font;
    if (s.bytes != null) {
      byte[] data = s.bytes;
      s.bytes = null;
      ByteBuffer b = MemoryUtil.memAlloc(data.length);
      b.put(data).flip();
      String id = "velvet-local-" + SLOTS.size() + "-" + Integer.toHexString(name.hashCode());
      int result =
          NanoVG.nvgCreateFontMem(
              Glide.getInstance().getNanoVGManager().getContext(), id, b, false);
      if (result < 0) {
        MemoryUtil.memFree(b);
        return fallback;
      }
      long context = Glide.getInstance().getNanoVGManager().getContext();
      NanoVG.nvgAddFallbackFont(context, id, Fonts.UNIFONT.getName());
      NanoVG.nvgAddFallbackFont(context, id, Fonts.FALLBACK.getName());
      Font f = new Font(id, null);
      f.setLoaded(true);
      f.setBuffer(b);
      s.font = f;
      return f;
    }
    return fallback;
  }

  public static void shutdown() {
    WORK.shutdownNow();
    for (Slot s : SLOTS.values()) if (s.font != null) MemoryUtil.memFree(s.font.getBuffer());
    SLOTS.clear();
  }
}
