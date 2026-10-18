package me.eldodebug.soar.lean;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import me.eldodebug.soar.scout.*;

public final class SchedulerChecks {
  static int checks;

  static void ok(boolean b, String m) {
    if (!b) throw new AssertionError(m);
    checks++;
  }

  static String fixture(String user) {
    StringBuilder b = new StringBuilder("{");
    for (String stat :
        Arrays.asList(
            "Wins",
            "Losses",
            "Final kills",
            "Final deaths",
            "Beds destroyed",
            "Highest winstreak reached")) {
      if (b.length() > 1) b.append(',');
      b.append('"')
          .append(stat)
          .append("\":{\"entries\":[{\"id\":\"")
          .append(user)
          .append("\",\"value\":8}]}");
    }
    return b.append('}').toString();
  }

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    ExecutorService workers = Executors.newCachedThreadPool();
    server.setExecutor(workers);
    AtomicInteger code = new AtomicInteger(200), count = new AtomicInteger();
    AtomicReference<String> body = new AtomicReference<>(fixture("Tester"));
    AtomicReference<String> query = new AtomicReference<>("");
    AtomicInteger delay = new AtomicInteger();
    server.createContext(
        "/",
        ex -> {
          if (ex.getRequestURI().getQuery() != null
              && ex.getRequestURI().getQuery().contains("mode=")) {
            count.incrementAndGet();
            query.set(ex.getRequestURI().getQuery());
          }
          try {
            if (delay.get() > 0) Thread.sleep(delay.get());
            byte[] data = body.get().getBytes("UTF-8");
            ex.getResponseHeaders().set("Retry-After", "1");
            ex.sendResponseHeaders(code.get(), code.get() == 204 ? -1 : data.length);
            if (code.get() != 204) ex.getResponseBody().write(data);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            ex.close();
          }
        });
    server.start();
    String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    AtomicLong clock = new AtomicLong(System.currentTimeMillis());
    PikaApiClient.Transport transport =
        path -> {
          URL u = new URL(path);
          return LeanHttp.fetch(
              new URL(base + "?" + u.getQuery()), "pika_TEST_SECRET_0123456789", true, 1000, 500);
        };
    try {
      PikaApiClient api = new PikaApiClient(transport, clock::get);
      try {
        for (String mode : Arrays.asList("ALL_MODES", "SOLO", "DOUBLES", "QUAD", "QUADS")) {
          api.clear();
          code.set(200);
          body.set(fixture("Tester"));
          ScoutStats s = api.request("Tester", mode, "monthly", 1, true).get(3, TimeUnit.SECONDS);
          ok(s.status == ScoutStats.Status.OK && s.wins == 8, "real-shape parser " + mode);
          ok(
              query.get().contains("mode=" + (mode.equals("QUADS") ? "QUAD" : mode)),
              "mode URL " + mode);
        }
        for (String period : Arrays.asList("total", "weekly", "monthly", "yearly")) {
          api.clear();
          api.request("Tester", "QUAD", period, 120, true).get(3, TimeUnit.SECONDS);
          ok(query.get().contains("interval=" + period), "period URL " + period);
        }
        api.clear();
        delay.set(120);
        int start = count.get();
        CompletableFuture<ScoutStats> a = api.request("Tester", "QUAD", "total", 1, true),
            b = api.request("Tester", "QUAD", "total", 1, true);
        ok(a == b, "deduplicated future");
        a.get(3, TimeUnit.SECONDS);
        ok(count.get() == start + 1, "deduplicated HTTP");
        delay.set(0);
        int calls = count.get();
        api.request("Tester", "QUAD", "total", 1, true).get();
        ok(count.get() == calls, "fresh cache hit");
        clock.addAndGet(121000);
        code.set(500);
        ScoutStats stale =
            api.request("Tester", "QUAD", "total", 120, true).get(4, TimeUnit.SECONDS);
        ok(stale.status == ScoutStats.Status.OK && stale.stale, "stale result preserved");
        api.clear();
        code.set(429);
        ScoutStats limited =
            api.request("Tester", "QUAD", "total", 120, true).get(3, TimeUnit.SECONDS);
        ok(limited.status == ScoutStats.Status.RATE_LIMITED, "rate limited status");
        calls = count.get();
        api.request("OtherName", "QUAD", "total", 120, true).get();
        ok(count.get() == calls, "shared 429 cooldown");
        clock.addAndGet(3000);
        for (int c : new int[] {204, 400, 401, 403, 404, 500}) {
          api.clear();
          code.set(c);
          calls = count.get();
          ScoutStats result =
              api.request("Tester", "QUAD", "total", 120, true).get(4, TimeUnit.SECONDS);
          ok(result.status != ScoutStats.Status.OK, "HTTP error parsed " + c);
          ok(!result.detail.contains("pika_TEST_SECRET"), "no key in error " + c);
          ok(count.get() - calls <= 2, "bounded retries " + c);
        }
        api.clear();
        code.set(200);
        body.set("{");
        ok(
            api.request("Tester", "QUAD", "total", 120, true).get(3, TimeUnit.SECONDS).status
                == ScoutStats.Status.INVALID_RESPONSE,
            "malformed JSON");
        api.clear();
        body.set("{}");
        ScoutStats empty =
            api.request("Tester", "QUAD", "total", 120, true).get(3, TimeUnit.SECONDS);
        ok(!empty.hasStats(), "empty stats");
        api.clear();
        body.set("{\"Wins\":{\"entries\":[{\"id\":\"Tester\",\"value\":8}]}}");
        ok(
            api.request("Tester", "QUAD", "total", 120, true).get(3, TimeUnit.SECONDS).wins == 8,
            "partial JSON");
        ok(
            api.request("invalid name", "QUAD", "total", 120, true).get().status
                == ScoutStats.Status.INVALID_REQUEST,
            "invalid user");
        ok(
            api.request(null, "QUAD", "total", 120, true).get().status
                == ScoutStats.Status.INVALID_REQUEST,
            "null user");
        api.clear();
        delay.set(1000);
        ScoutStats timeout =
            api.request("Tester", "QUAD", "total", 120, true).get(4, TimeUnit.SECONDS);
        ok(timeout.status == ScoutStats.Status.TIMEOUT, "request timeout");
        delay.set(0);
      } finally {
        api.close();
      }
      PikaApiClient redacted =
          new PikaApiClient(
              path -> {
                throw new IOException("unsafe echo pika_TEST_SECRET_0123456789");
              });
      try {
        ScoutStats s =
            redacted.request("Tester", "QUAD", "total", 120, true).get(4, TimeUnit.SECONDS);
        ok(
            !s.detail.contains("pika_TEST_SECRET"),
            "exception secret redacted before status/logging");
      } finally {
        redacted.close();
      }
      CountDownLatch gate = new CountDownLatch(1);
      PikaApiClient slow =
          new PikaApiClient(
              path -> {
                try {
                  gate.await();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  throw new InterruptedIOException();
                }
                return new PikaApiClient.Response(200, null, fixture("Tester"));
              });
      try {
        List<CompletableFuture<ScoutStats>> pending = new ArrayList<>();
        for (int i = 0; i < 220; i++)
          pending.add(slow.request("Player" + i, "QUAD", "total", 120, i >= 100));
        ok(slow.queueSize() <= 160, "manual and automatic queue bounded");
        slow.retain(Collections.emptySet(), "QUAD", "total");
        long until = System.nanoTime() + 1000000000L;
        while (slow.inFlightSize() > 0 && System.nanoTime() < until) Thread.yield();
        ok(
            slow.queueSize() < 160,
            "disconnect removes automatic queued work; manual lookups retained");
        ok(pending.stream().anyMatch(CompletableFuture::isCancelled), "obsolete futures cancelled");
      } finally {
        gate.countDown();
        slow.close();
      }
    } finally {
      server.stop(0);
      workers.shutdownNow();
    }
    System.out.println("PASS " + checks + " existing-scheduler mock integration checks");
  }
}
