package me.eldodebug.soar.lean;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import me.eldodebug.soar.management.mods.impl.ScoutMod;
import me.eldodebug.soar.scout.*;

/** Real profile executor; only the native session object/HTTP transport are mocked. */
public final class ProfileCancelChecks {
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
    AtomicInteger profileCalls = new AtomicInteger();
    PikaApiClient api =
        new PikaApiClient(
            url -> {
              if (!url.contains("leaderboard")) profileCalls.incrementAndGet();
              return new PikaApiClient.Response(
                  200, null, "{\"Wins\":{\"entries\":[{\"id\":\"Tester\",\"value\":8}]}}");
            });
    try {
      api.request("Tester", "QUAD", "total", 120, true).get(2, TimeUnit.SECONDS);
      Field executorField = PikaApiClient.class.getDeclaredField("profileExecutor");
      executorField.setAccessible(true);
      ThreadPoolExecutor executor = (ThreadPoolExecutor) executorField.get(api);
      long until = System.nanoTime() + 1000000000L;
      while (executor.getActiveCount() == 0
          && executor.getQueue().isEmpty()
          && System.nanoTime() < until) Thread.sleep(2);
      if (executor.getActiveCount() == 0 && executor.getQueue().isEmpty())
        throw new AssertionError("Profile test did not start");
      Field generation = ScoutState.class.getDeclaredField("generation");
      generation.setAccessible(true);
      if (!Modifier.isVolatile(generation.getModifiers()))
        throw new AssertionError("Session generation lacks cross-thread visibility");
      generation.setLong(state, 1);
      until = System.nanoTime() + 3000000000L;
      while ((executor.getActiveCount() != 0 || !executor.getQueue().isEmpty())
          && System.nanoTime() < until) Thread.sleep(5);
      if (executor.getActiveCount() != 0 || profileCalls.get() != 0)
        throw new AssertionError("Obsolete generation performed profile HTTP");
      System.out.println(
          "PASS 3 real-profile-worker generation cancellation/visibility checks (session and HTTP"
              + " mocked)");
    } finally {
      api.close();
      singleton.set(null, old);
    }
  }
}
