import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Explicit opt-in local key test. No secret arguments, URLs, output, redirects or retries. */
public final class OnlineSmoke {
  public static void main(String[] args) {
    HttpURLConnection c = null;
    int result = 0;
    try {
      if (args.length != 2 || !args[1].matches("[A-Za-z0-9_]{3,16}")) throw new IOException();
      Path file = Paths.get(args[0]);
      if (!Files.isRegularFile(file) || Files.size(file) > 1024) throw new IOException();
      String key;
      try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
        key = r.readLine();
      }
      if (key == null) throw new IOException();
      key = key.trim();
      if (!key.matches("[!-~]{1,512}")) throw new IOException();
      URL u =
          new URL(
              "https://stats.pika-network.net/api/profile/"
                  + args[1]
                  + "/leaderboard?type=bedwars&interval=monthly&mode=QUAD");
      c = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY);
      c.setInstanceFollowRedirects(false);
      c.setConnectTimeout(4500);
      c.setReadTimeout(8000);
      c.setUseCaches(false);
      c.setRequestProperty("X-Api-Key", key);
      c.setRequestProperty("Accept", "application/json");
      c.setRequestProperty("User-Agent", "VelvetG-Lean/Overhaul3-local-smoke");
      int status = c.getResponseCode();
      System.out.println("HTTP_STATUS=" + status);
      if (status != 200) result = 2;
    } catch (Exception ignored) {
      System.out.println(
          "ONLINE_INCONCLUSIVE: key file, network, TLS or timeout check failed; sensitive details"
              + " omitted.");
      result = 1;
    } finally {
      if (c != null) c.disconnect();
    }
    if (result != 0) System.exit(result);
  }
}
