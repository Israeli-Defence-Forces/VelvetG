package me.eldodebug.soar.lean;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import me.eldodebug.soar.scout.*;

public final class KernelChecks {
  static int checks;

  static void ok(boolean b, String m) {
    if (!b) throw new AssertionError(m);
    checks++;
  }

  static Object field(Object o, String n) throws Exception {
    Field f = o.getClass().getDeclaredField(n);
    f.setAccessible(true);
    return f.get(o);
  }

  public static void main(String[] args) throws Exception {
    String secret = "pika_0123456789abcdef01234567";
    for (String s : Arrays.asList("QUAD", "QUADS", "quad", " quads "))
      ok(KeyVault.mode(s).equals("QUAD"), "QUAD normalization");
    for (String s : Arrays.asList("ALL_MODES", "SOLO", "DOUBLES"))
      ok(KeyVault.mode(s).equals(s), "supported mode");
    ok(KeyVault.mode(null).equals("ALL_MODES"), "null mode");
    ok(KeyVault.pika("play.pika-network.net:25565"), "Pika suffix");
    ok(!KeyVault.pika("pika-network.net.evil.test"), "host suffix spoof");
    ok(!KeyVault.valid("key\r\nsecret"), "header injection");
    ok(!KeyVault.valid(""), "missing key");
    ok(KeyVault.valid(secret), "key valid");
    ok(!KeyVault.redact("message " + secret).contains(secret), "secret redacted");
    Object conn = new Object();
    long now = 100000;
    ok(!KeyVault.request(conn, "play.pika-network.net", now, true), "join delay");
    ok(KeyVault.request(conn, "play.pika-network.net", now + 3000, true), "auto fetch");
    ok(!KeyVault.request(conn, "play.pika-network.net", now + 4000, true), "cooldown");
    ok(
        KeyVault.capture(
                "Player: (!) Your stats API key: " + secret, "play.pika-network.net", 0, now + 4000)
            == null,
        "spoof rejected");
    ok(
        KeyVault.capture("(!) Your stats API key:", "play.pika-network.net", 0, now + 4000) == null,
        "header arms");
    ok(
        secret.equals(KeyVault.capture(secret, "play.pika-network.net", 0, now + 4001)),
        "split response");
    ok(KeyVault.capture(secret, "play.pika-network.net", 0, now + 4002) == null, "no replay");
    for (int i = 1; i <= 2; i++)
      ok(
          KeyVault.request(conn, "play.pika-network.net", now + 3000 + 60000 * i, true),
          "bounded retry");
    ok(!KeyVault.request(conn, "play.pika-network.net", now + 200000, true), "retry limit");
    ok(
        KeyVault.capture("(!) Your stats API key: " + secret, "evil.test", 0, now + 124000) == null,
        "off domain");
    ok(
        KeyVault.capture(
                "(!) Your stats API key: " + secret, "play.pika-network.net", 2, now + 124000)
            == null,
        "action bar rejected");
    ok(
        KeyVault.capture(
                "(!) Your stats API key: " + secret, "play.pika-network.net", 0, now + 140000)
            == null,
        "expired capture");
    ok(!KeyVault.manual("evil.test", now), "manual capture rejects other servers");
    ok(
        KeyVault.manual("play.pika-network.net", now),
        "explicit manual key request opens capture without automatic fetching");
    ok(
        secret.equals(
            KeyVault.capture(
                "(!) Your stats API key: " + secret, "play.pika-network.net", 0, now + 1)),
        "manual response captured and hidden");
    Field stored = KeyVault.class.getDeclaredField("key");
    stored.setAccessible(true);
    stored.set(null, secret);
    KeyVault.response(200, secret);
    ok(KeyVault.status().equals("authenticated"), "authenticated status");
    ok(KeyVault.usable(secret), "authenticated key allowed");
    KeyVault.response(401, "old-key");
    ok(KeyVault.status().equals("authenticated"), "old request cannot reject rotated key");
    KeyVault.response(401, secret);
    ok(KeyVault.status().contains("rejected"), "genuine rejection");
    ok(!KeyVault.usable(secret), "rejected key blocks further production HTTP");
    Object rotated = new Object();
    KeyVault.request(rotated, "play.pika-network.net", now, false);
    ok(
        KeyVault.request(rotated, "play.pika-network.net", now + 3000, true),
        "reauthentication after rejection");
    stored.set(null, "");
    Field ioField = KeyVault.class.getDeclaredField("IO_LOCK");
    ioField.setAccessible(true);
    ExecutorService authWorker = Executors.newSingleThreadExecutor();
    try {
      synchronized (ioField.get(null)) {
        ok(
            !authWorker
                .submit(() -> KeyVault.request(new Object(), "play.pika-network.net", 1L, false))
                .get(1, TimeUnit.SECONDS),
            "tick auth state does not wait for blocked key-file I/O");
      }
    } finally {
      authWorker.shutdownNow();
    }
    Path temp = Files.createTempDirectory("lean-image-test");
    BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
    Path png = temp.resolve("screen.png");
    ImageIO.write(image, "PNG", png.toFile());
    ExecutorService wrongThread = Executors.newSingleThreadExecutor();
    try {
      boolean loaded =
          wrongThread.submit(() -> AsyncImages.load(1L, png.toFile())).get(1, TimeUnit.SECONDS);
      Field ownerField = AsyncImages.class.getDeclaredField("owner");
      ownerField.setAccessible(true);
      ok(
          !loaded && ownerField.get(null) == null,
          "worker cannot claim render ownership or upload gallery images");
    } finally {
      wrongThread.shutdownNow();
    }
    BufferedImage thumb = AsyncImages.decode(png.toFile());
    ok(thumb.getWidth() <= 1024 && thumb.getHeight() <= 1024, "subsampled decode");
    Files.write(temp.resolve("bad.png"), new byte[] {1, 2, 3});
    try {
      AsyncImages.decode(temp.resolve("bad.png").toFile());
      throw new AssertionError("corrupt image accepted");
    } catch (IOException e) {
      checks++;
    }
    ok(Files.exists(png), "source screenshot retained");
    List<ScoutRow> rows = new ArrayList<>();
    for (int i = 0; i < 50; i++)
      rows.add(
          new ScoutRow(
              "Player" + i,
              ScoutStats.status("Player" + i, ScoutStats.Status.OK, ""),
              i,
              0xffffff,
              false,
              false,
              null));
    ok(TableSupport.prepare(rows, false).size() == 40, "TAB 40 limit");
    ok(rows.size() == 50, "source list unchanged");
    LeanSettings.set("hudLimit", "7");
    ok(TableSupport.prepare(rows, true).size() == 7, "independent HUD limit");
    LeanSettings.set("hudLimit", "40");
    LeanSettings.set("tabColumns", "BEDS,NAME,GUILD,FKDR,BEDS,INVALID");
    ok(
        TableSupport.columns(Arrays.asList("NAME"), false)
            .equals(Arrays.asList("BEDS", "NAME", "GUILD", "FKDR")),
        "column reorder dedup whitelist");
    LeanSettings.set("tabColumns", "EXISTING");
    LeanSettings.set("maximumFPS", "true");
    ok(!TableSupport.cosmetic(true), "FPS effect bypass");
    LeanSettings.set("maximumFPS", "false");
    LeanSettings.set("animation", "NONE");
    ok(!TableSupport.animations(true), "animation None disables motion");
    ok(TableSupport.cosmetic(true), "animation None preserves independent shadow setting");
    LeanSettings.set("animation", "EXISTING");
    ExecutorService pool = TableSupport.profileWorkers(r -> new Thread(r));
    ok(((ThreadPoolExecutor) pool).getQueue().remainingCapacity() == 64, "bounded profile queue");
    pool.shutdownNow();
    ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>();
    for (int i = 0; i < 1000; i++) TableSupport.boundedPut(map, i, i);
    ok(map.size() <= 500, "bounded profile cache");
    LeanSettings.exportSettings();
    ok(
        !new String(Files.readAllBytes(Paths.get("config/velvetg-lean-export.properties")), "UTF-8")
            .contains(secret),
        "safe export");
    try {
      LeanSettings.set("apiKey", secret);
      throw new AssertionError("key export allowed");
    } catch (IOException e) {
      checks++;
    }
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    ExecutorService http = Executors.newCachedThreadPool();
    server.setExecutor(http);
    final int[] code = {200};
    final String[] body = {"{}"};
    final boolean[] header = {false};
    server.createContext(
        "/",
        ex -> {
          header[0] = secret.equals(ex.getRequestHeaders().getFirst("X-Api-Key"));
          String path = ex.getRequestURI().getPath();
          if (path.equals("/slow"))
            try {
              Thread.sleep(250);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          ex.getResponseHeaders().add("Retry-After", "2");
          if (code[0] == 302)
            ex.getResponseHeaders()
                .add("Location", "http://127.0.0.1:" + server.getAddress().getPort() + "/leak");
          byte[] bytes = body[0].getBytes("UTF-8");
          try {
            ex.sendResponseHeaders(code[0], code[0] == 204 ? -1 : bytes.length);
            if (code[0] != 204) ex.getResponseBody().write(bytes);
          } finally {
            ex.close();
          }
        });
    server.start();
    try {
      URL u = new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/");
      for (int c : new int[] {200, 204, 400, 401, 403, 404, 429, 500, 302}) {
        code[0] = c;
        Object r = LeanHttp.fetch(u, secret, true, 1000, 1000);
        ok(((Integer) field(r, "status")) == c, "HTTP " + c);
        ok(header[0], "header authentication " + c);
        ok(!field(r, "body").toString().contains(secret), "secret not returned " + c);
      }
      code[0] = 200;
      body[0] = "{";
      ok(
          field(LeanHttp.fetch(u, secret, true, 1000, 1000), "body").equals("{"),
          "transport leaves parsing to scheduler");
      try {
        LeanHttp.fetch(new URL(u, "slow"), secret, true, 50, 50);
        throw new AssertionError("timeout missing");
      } catch (SocketTimeoutException e) {
        checks++;
      }
      try {
        LeanHttp.get("https://evil.test/");
        throw new AssertionError("external accepted");
      } catch (IOException e) {
        ok(!e.toString().contains(secret), "safe endpoint rejection");
      }
      try {
        LeanHttp.fetch(new URL("http://localhost/"), secret, true, 50, 50);
        throw new AssertionError("mock host accepted");
      } catch (IOException e) {
        checks++;
      }
    } finally {
      server.stop(0);
      http.shutdownNow();
    }
    FrameProbe.enabled = false;
    ok(FrameProbe.start() == 0, "disabled profiler bypass");
    FrameProbe.enabled = true;
    FrameProbe.frame();
    FrameProbe.frame();
    ok(FrameProbe.summary().contains("p99="), "percentiles");
    FrameProbe.enabled = false;
    System.out.println(
        "PASS " + checks + " kernel, security, bounded cache, gallery and mock HTTP checks");
  }
}
