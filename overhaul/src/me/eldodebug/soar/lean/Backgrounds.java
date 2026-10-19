package me.eldodebug.soar.lean;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Optional credential-free image retrieval. No redirect or arbitrary host support. */
public final class Backgrounds {
  private static final ThreadPoolExecutor WORK =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(2),
          r -> {
            Thread t = new Thread(r, "VelvetG-background");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private static final String[] chosen = {"NONE", "NONE"};
  private static final File[] files = new File[2];
  private static final boolean[] pending = new boolean[2];
  private static final Future<?>[] tasks = new Future<?>[2];
  private static final long[] generation = new long[2];
  private static boolean closed;

  private static boolean current(int slot, long token) {
    synchronized (LOCK) {
      return !closed && generation[slot] == token;
    }
  }

  private static final Object LOCK = new Object();

  public static File file(boolean hud) {
    int slot = hud ? 1 : 0;
    String name = TableSupport.value("Background");
    synchronized (LOCK) {
      if (closed) return null;
      if (name.equals(chosen[slot])) return files[slot];
      generation[slot]++;
      if (tasks[slot] != null) {
        tasks[slot].cancel(true);
        WORK.remove((Runnable) tasks[slot]);
        tasks[slot] = null;
      }
      if (name.equals("NONE")) {
        chosen[slot] = name;
        files[slot] = null;
        pending[slot] = false;
        return null;
      }
      final long token = generation[slot];
      chosen[slot] = name;
      files[slot] = null;
      pending[slot] = true;
      try {
        tasks[slot] =
            WORK.submit(
                () -> {
                  File out = null;
                  try {
                    Path dir = Paths.get("config/velvetg-backgrounds");
                    if (name.equals("RANDOM")) {
                      File[] list =
                          dir.toFile()
                              .listFiles(
                                  f ->
                                      f.isFile()
                                          && f.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
                      if (list != null && list.length > 0)
                        out = list[new Random().nextInt(list.length)];
                    } else if (name.matches("CATBOX-[a-z0-9]{6}\\.png")) {
                      Path remote = Paths.get("cache/velvetg-backgrounds");
                      Files.createDirectories(remote);
                      Path target = remote.resolve(name);
                      if (!Files.isRegularFile(target)) {
                        URL u = new URL("https://files.catbox.moe/" + name.substring(7));
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        Path temp = null;
                        try {
                          c.setInstanceFollowRedirects(false);
                          c.setConnectTimeout(5000);
                          c.setReadTimeout(8000);
                          if (c.getResponseCode() != 200 || c.getContentLengthLong() > 8388608)
                            throw new IOException("Background unavailable");
                          temp = Files.createTempFile(remote, "image-", ".tmp");
                          try (InputStream in = c.getInputStream();
                              OutputStream o = Files.newOutputStream(temp)) {
                            byte[] b = new byte[8192];
                            int n, total = 0;
                            long deadline = System.nanoTime() + 8000000000L;
                            while ((n = in.read(b)) != -1) {
                              total += n;
                              if (total > 8388608
                                  || System.nanoTime() > deadline
                                  || !current(slot, token)
                                  || Thread.currentThread().isInterrupted())
                                throw new IOException("Background limit");
                              o.write(b, 0, n);
                            }
                          }
                          try {
                            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                          } catch (AtomicMoveNotSupportedException e) {
                            Files.move(temp, target);
                          }
                        } finally {
                          c.disconnect();
                          if (temp != null) Files.deleteIfExists(temp);
                        }
                      }
                      out = target.toFile();
                      File[] cached =
                          remote
                              .toFile()
                              .listFiles(f -> f.getName().matches("CATBOX-[a-z0-9]{6}\\.png"));
                      if (cached != null && cached.length > 16) {
                        Arrays.sort(cached, Comparator.comparingLong(File::lastModified));
                        for (int old = 0; old < cached.length - 16; old++)
                          if (!cached[old].equals(out)) Files.deleteIfExists(cached[old].toPath());
                      }
                    } else if (name.matches("[A-Za-z0-9 _.-]{1,80}\\.png")
                        && !name.contains("..")) {
                      File f = dir.resolve(name).toFile();
                      if (f.isFile()) out = f;
                    }
                  } catch (IOException | SecurityException ignored) {
                  } finally {
                    synchronized (LOCK) {
                      if (current(slot, token)) {
                        files[slot] = out;
                        pending[slot] = false;
                        tasks[slot] = null;
                      }
                    }
                  }
                });
      } catch (RejectedExecutionException e) {
        pending[slot] = false;
        chosen[slot] = "NONE";
      }
      return null;
    }
  }

  public static void shutdown() {
    synchronized (LOCK) {
      closed = true;
      for (int i = 0; i < 2; i++) {
        generation[i]++;
        if (tasks[i] != null) tasks[i].cancel(true);
        files[i] = null;
        pending[i] = false;
      }
    }
    WORK.shutdownNow();
  }
}
