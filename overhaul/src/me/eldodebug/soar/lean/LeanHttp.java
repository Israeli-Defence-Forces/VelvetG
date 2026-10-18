package me.eldodebug.soar.lean;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import me.eldodebug.soar.scout.PikaApiClient;

/** Transport only: keeps the existing priority/deduplication/leaderboard scheduler. */
public final class LeanHttp {
  public static PikaApiClient.Response get(String path) throws IOException {
    URL u = new URL(path);
    if (!u.getProtocol().equals("https")
        || !u.getHost().equals("stats.pika-network.net")
        || u.getPort() != -1
        || u.getUserInfo() != null) throw new IOException("Unexpected statistics endpoint");
    String k = KeyVault.get();
    if (k.isEmpty() || !KeyVault.usable(k)) return new PikaApiClient.Response(401, null, "");
    return fetch(u, k, false, 4500, 8000);
  }

  static PikaApiClient.Response fetch(URL u, String key, boolean mock, int connect, int read)
      throws IOException {
    if (mock && (!u.getHost().equals("127.0.0.1") || !u.getProtocol().equals("http")))
      throw new IOException("Invalid mock endpoint");
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    try {
      c.setInstanceFollowRedirects(false);
      c.setConnectTimeout(connect);
      c.setReadTimeout(read);
      c.setUseCaches(false);
      c.setRequestProperty("Accept", "application/json");
      c.setRequestProperty("User-Agent", "VelvetG-Lean/overhaul-1 Minecraft-1.8.9");
      c.setRequestProperty("X-Api-Key", key);
      int status = c.getResponseCode();
      if (!mock && status != 403) KeyVault.response(status, key);
      if (!mock && status == 403) {
        InputStream error = c.getErrorStream();
        if (error != null)
          try (InputStream input = error;
              ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[1024];
            int n, total = 0;
            long deadline = System.nanoTime() + read * 1000000L;
            while ((n = input.read(bytes)) != -1
                && total + n <= 16384
                && System.nanoTime() < deadline) {
              out.write(bytes, 0, n);
              total += n;
            }
            String message =
                new String(out.toByteArray(), StandardCharsets.UTF_8)
                    .toLowerCase(java.util.Locale.ROOT);
            if (message.contains("invalid api key")
                || message.contains("invalid_api_key")
                || message.contains("expired api key")
                || message.contains("unauthorized")) KeyVault.response(403, key);
          }
      }
      String retry = c.getHeaderField("Retry-After");
      if (status < 200 || status >= 300 || status == 204)
        return new PikaApiClient.Response(status, retry, "");
      if (c.getContentLengthLong() > 1048576)
        throw new IOException("Statistics response too large");
      try (InputStream in = c.getInputStream();
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buf = new byte[8192];
        int n;
        long end = System.nanoTime() + read * 1000000L;
        while ((n = in.read(buf)) != -1) {
          if (Thread.currentThread().isInterrupted() || System.nanoTime() > end)
            throw new InterruptedIOException("Statistics request interrupted or timed out");
          if (out.size() + n > 1048576) throw new IOException("Statistics response too large");
          out.write(buf, 0, n);
        }
        return new PikaApiClient.Response(
            status, retry, new String(out.toByteArray(), StandardCharsets.UTF_8));
      }
    } finally {
      c.disconnect();
    }
  }
}
