package me.eldodebug.soar.lean;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import me.eldodebug.soar.management.mods.impl.ScoutMod;
import me.eldodebug.soar.scout.*;

/** Real core/profile executors; controlled HTTP transport and native singleton. */
public final class ProfileDedupChecks {
  static int checks;

  static void ok(boolean v, String name) {
    if (!v) throw new AssertionError(name);
    checks++;
  }

  public static void main(String[] args) throws Exception {
    Class<?> uc = Class.forName("sun.misc.Unsafe");
    Field uf = uc.getDeclaredField("theUnsafe");
    uf.setAccessible(true);
    Object unsafe = uf.get(null);
    ScoutMod mock =
        (ScoutMod) uc.getMethod("allocateInstance", Class.class).invoke(unsafe, ScoutMod.class);
    Field singleton = ScoutMod.class.getDeclaredField("instance");
    singleton.setAccessible(true);
    Object old = singleton.get(null);
    Field stateField = ScoutMod.class.getDeclaredField("state");
    stateField.setAccessible(true);
    ScoutState state = new ScoutState();
    stateField.set(mock, state);
    singleton.set(null, mock);
    AtomicInteger profiles = new AtomicInteger();
    CountDownLatch began = new CountDownLatch(1), release = new CountDownLatch(1);
    PikaApiClient api =
        new PikaApiClient(
            url -> {
              if (url.contains("leaderboard"))
                return new PikaApiClient.Response(
                    200, null, "{\"Wins\":{\"entries\":[{\"id\":\"Tester\",\"value\":8}]}}");
              profiles.incrementAndGet();
              began.countDown();
              try {
                if (!release.await(2, TimeUnit.SECONDS))
                  throw new java.io.IOException("test barrier timeout");
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException("cancelled");
              }
              return new PikaApiClient.Response(
                  200,
                  null,
                  "{\"username\":\"Tester\",\"rank\":{\"level\":42},\"clan\":{\"name\":\"Example\"}}");
            });
    try {
      api.request("Tester", "QUAD", "total", 120, true).get(2, TimeUnit.SECONDS);
      ok(began.await(2, TimeUnit.SECONDS), "first background profile request starts");
      api.request("Tester", "SOLO", "total", 120, true).get(2, TimeUnit.SECONDS);
      api.request("Tester", "DOUBLES", "monthly", 120, true).get(2, TimeUnit.SECONDS);
      Field groupsField = PikaApiClient.class.getDeclaredField("lean$profiles");
      groupsField.setAccessible(true);
      ProfileRequests groups = (ProfileRequests) groupsField.get(api);
      ok(groups.size() == 1, "different modes share one pending profile group");
      release.countDown();
      long deadline = System.nanoTime() + 3000000000L;
      while (groups.size() != 0 && System.nanoTime() < deadline) Thread.sleep(5);
      ok(groups.size() == 0, "profile group completes");
      ok(profiles.get() == 1, "one profile HTTP request across three stats selections");
      ok("Example".equals(ProfileExtras.guild("Tester")), "same response supplies guild");
      for (String mode : new String[] {"QUAD", "SOLO", "DOUBLES"}) {
        ScoutStats row = api.peek("Tester", mode, mode.equals("DOUBLES") ? "monthly" : "total");
        ok(
            row != null && row.level != null && row.level == 42,
            "profile merges into " + mode + " cache");
      }
      api.clear();
      ok(groups.size() == 0, "clear drops pending groups");
    } finally {
      release.countDown();
      api.close();
      singleton.set(null, old);
    }
    System.out.println(
        "PASS "
            + checks
            + " real-profile-executor deduplication, shared enrichment and cache-merge checks"
            + " (HTTP/native session mocked)");
  }
}
