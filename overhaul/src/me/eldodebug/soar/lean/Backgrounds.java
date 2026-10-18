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
  private static final Object LOCK = new Object();

  public static File file(boolean hud) {
    int slot = hud ? 1 : 0;
    String name = LeanSettings.get(hud ? "hudBackground" : "tabBackground");
    synchronized (LOCK) {
      if (name.equals("NONE")) return null;
      if (name.equals(chosen[slot])) return files[slot];
      if (pending[slot]) return null;
      chosen[slot] = name;
      files[slot] = null;
      pending[slot] = true;
      try {
        WORK.execute(
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
                          if (total > 8388608 || System.nanoTime() > deadline)
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
                } else if (name.matches("[A-Za-z0-9 _.-]{1,80}\\.png") && !name.contains("..")) {
                  File f = dir.resolve(name).toFile();
                  if (f.isFile()) out = f;
                }
              } catch (IOException | SecurityException ignored) {
              } finally {
                synchronized (LOCK) {
                  files[slot] = out;
                  pending[slot] = false;
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
    WORK.shutdownNow();
  }
}
