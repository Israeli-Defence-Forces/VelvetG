package me.eldodebug.soar.lean;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Coalesces the two existing property mirrors without rewriting unrelated configuration. */
public final class ConfigWriter {
  private static final ScheduledThreadPoolExecutor WORK =
      new ScheduledThreadPoolExecutor(
          1,
          r -> {
            Thread t = new Thread(r, "VelvetG-config-save");
            t.setDaemon(true);
            return t;
          });
  private static final Map<File, ScheduledFuture<?>> FUTURES = new HashMap<>();
  private static final Map<File, Properties> PENDING = new HashMap<>();
  private static long writes;
  private static boolean closed;

  static {
    WORK.setRemoveOnCancelPolicy(true);
  }

  public static synchronized void enqueue(Properties p, File f) {
    Properties copy = new Properties();
    copy.putAll(p);
    if (closed) {
      write(copy, f);
      return;
    }
    ScheduledFuture<?> old = FUTURES.remove(f);
    if (old != null) old.cancel(false);
    PENDING.put(f, copy);
    FUTURES.put(f, WORK.schedule(() -> flush(f), 500, TimeUnit.MILLISECONDS));
  }

  private static void flush(File f) {
    Properties p;
    synchronized (ConfigWriter.class) {
      p = PENDING.remove(f);
      FUTURES.remove(f);
    }
    if (p != null) write(p, f);
  }

  private static void write(Properties p, File f) {
    Path temp = null;
    try {
      Path path = f.toPath().toAbsolutePath();
      Files.createDirectories(path.getParent());
      temp = Files.createTempFile(path.getParent(), "velvet-settings-", ".tmp");
      try (OutputStream out = Files.newOutputStream(temp)) {
        p.store(out, "VelvetG settings");
      }
      try {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
      }
      synchronized (ConfigWriter.class) {
        writes++;
      }
    } catch (IOException | SecurityException ignored) {
    } finally {
      if (temp != null)
        try {
          Files.deleteIfExists(temp);
        } catch (IOException ignored) {
        }
    }
  }

  public static synchronized long writes() {
    return writes;
  }

  public static synchronized int queued() {
    return WORK.getQueue().size();
  }

  public static void shutdown() {
    synchronized (ConfigWriter.class) {
      closed = true;
      for (ScheduledFuture<?> f : FUTURES.values()) f.cancel(false);
      FUTURES.clear();
    }
    WORK.shutdown();
    try {
      if (!WORK.awaitTermination(2, TimeUnit.SECONDS)) return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    Map<File, Properties> snapshot;
    synchronized (ConfigWriter.class) {
      snapshot = new HashMap<>(PENDING);
      PENDING.clear();
    }
    for (Map.Entry<File, Properties> e : snapshot.entrySet()) write(e.getValue(), e.getKey());
  }
}
