package me.eldodebug.soar.lean;

import com.sun.net.httpserver.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import me.eldodebug.soar.scout.*;

/** Same local HTTP profile workload for original and patched clients. No game launch. */
public final class ProfileCountBench {
  public static void main(String[] args) throws Exception {
    AtomicInteger calls = new AtomicInteger();
    ExecutorService serverWorkers = Executors.newFixedThreadPool(2);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(serverWorkers);
    server.createContext(
        "/",
        exchange -> {
          String json;
          if (exchange.getRequestURI().getPath().endsWith("/leaderboard"))
            json = "{\"Wins\":{\"entries\":[{\"id\":\"Tester\",\"value\":8}]}}";
          else {
            calls.incrementAndGet();
            json = "{\"username\":\"Tester\",\"rank\":{\"level\":42}}";
          }
          byte[] body = json.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
    PikaApiClient api =
        new PikaApiClient(
            url -> {
              HttpURLConnection c =
                  (HttpURLConnection)
                      new URL(
                              "http://127.0.0.1:"
                                  + server.getAddress().getPort()
                                  + new URL(url).getFile())
                          .openConnection();
              c.setConnectTimeout(1500);
              c.setReadTimeout(2000);
              try (InputStream in = c.getInputStream();
                  ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                return new PikaApiClient.Response(
                    c.getResponseCode(),
                    null,
                    new String(out.toByteArray(), StandardCharsets.UTF_8));
              } finally {
                c.disconnect();
              }
            });
    try {
      for (String mode : new String[] {"QUAD", "SOLO", "DOUBLES"})
        api.request("Tester", mode, "total", 120, true).get(3, TimeUnit.SECONDS);
      Field field = PikaApiClient.class.getDeclaredField("profileExecutor");
      field.setAccessible(true);
      ThreadPoolExecutor executor = (ThreadPoolExecutor) field.get(api);
      long deadline = System.nanoTime() + 7000000000L;
      while ((calls.get() == 0 || executor.getActiveCount() != 0 || !executor.getQueue().isEmpty())
          && System.nanoTime() < deadline) Thread.sleep(5);
      if (calls.get() == 0 || executor.getActiveCount() != 0 || !executor.getQueue().isEmpty())
        throw new AssertionError("Profile workload did not settle");
      System.out.println("profileHttpRequestsForThreeSelections=" + calls.get());
    } finally {
      api.close();
      server.stop(0);
      serverWorkers.shutdownNow();
    }
  }
}
