package me.eldodebug.soar.lean;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import me.eldodebug.soar.management.screenshot.*;

/** Discovery only. No image bytes, GPU calls, or deletion records on startup. */
public final class GalleryIndex {
  private static final ExecutorService WORK =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "VelvetG-gallery-index");
            t.setDaemon(true);
            return t;
          });
  private static final Map<ScreenshotManager, State> STATES = new WeakHashMap<>();

  private static final class State {
    long next;
    volatile boolean pending;
    volatile File[] files;
  }

  public static void scan(
      ScreenshotManager m,
      CopyOnWriteArrayList<Screenshot> items,
      CopyOnWriteArrayList<File> removed) {
    State s;
    synchronized (STATES) {
      s = STATES.get(m);
      if (s == null) {
        s = new State();
        STATES.put(m, s);
      }
    }
    if (s.files != null) {
      File[] ready = s.files;
      s.files = null;
      Set<File> known = new HashSet<>();
      for (Screenshot i : items) known.add(i.getImage());
      Set<File> hidden = new HashSet<>(removed);
      List<Screenshot> add = new ArrayList<>();
      for (File f : ready) if (!hidden.contains(f) && known.add(f)) add.add(new Screenshot(f));
      items.addAll(add);
    }
    long now = System.nanoTime();
    if (s.pending || now < s.next) return;
    File dir;
    try {
      dir = me.eldodebug.soar.Glide.getInstance().getFileManager().getScreenshotDir();
    } catch (RuntimeException e) {
      return;
    }
    if (dir == null) return;
    s.pending = true;
    s.next = now + 15000000000L;
    State target = s;
    try {
      WORK.execute(
          () -> {
            try {
              File[] fs = discover(dir);
              if (fs != null) target.files = fs;
            } catch (SecurityException ignored) {
            } finally {
              target.pending = false;
            }
          });
    } catch (RejectedExecutionException e) {
      s.pending = false;
    }
  }

  static File[] discover(File dir) {
    try {
      File[] files =
          dir.listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
      if (files != null) Arrays.sort(files, Comparator.comparing(File::getName).reversed());
      return files;
    } catch (SecurityException e) {
      return null;
    }
  }

  public static void shutdown() {
    WORK.shutdownNow();
  }
}
