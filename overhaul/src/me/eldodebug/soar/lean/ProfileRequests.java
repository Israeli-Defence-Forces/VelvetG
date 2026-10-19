package me.eldodebug.soar.lean;

import java.util.*;
import java.util.concurrent.*;

/**
 * One queued/running profile group per user and session; the first fills the shared profile cache.
 */
public final class ProfileRequests {
  private final Map<String, Flight> pending = new HashMap<>();
  private final LinkedHashMap<String, Long> retries = new LinkedHashMap<>();
  private final java.util.function.LongSupplier clock;

  public ProfileRequests() {
    this(System::nanoTime);
  }

  ProfileRequests(java.util.function.LongSupplier clock) {
    this.clock = clock;
  }

  public synchronized boolean allowFetch(String name, long epoch, long generation) {
    String key = name.toLowerCase(Locale.ROOT) + "|" + epoch + "|" + generation;
    long now = clock.getAsLong();
    Long until = retries.get(key);
    if (until != null && now < until) return false;
    retries.put(key, now + 30000000000L);
    while (retries.size() > 500) retries.remove(retries.keySet().iterator().next());
    return true;
  }

  public static boolean active(boolean closed, long currentEpoch, long epoch, long generation) {
    return !closed
        && currentEpoch == epoch
        && (generation < 0
            || generation == me.eldodebug.soar.scout.Scout105Runtime.sessionGeneration());
  }

  private static final class Flight {
    final String key;
    final LinkedHashMap<String, Runnable> waiting = new LinkedHashMap<>();
    final Set<String> accepted = new HashSet<>();

    Flight(String key) {
      this.key = key;
    }
  }

  public static void execute(
      ExecutorService executor,
      Runnable task,
      ProfileRequests groups,
      String name,
      String cacheKey,
      long epoch,
      long generation) {
    groups.submit(executor, task, name, cacheKey, epoch, generation);
  }

  private synchronized void submit(
      ExecutorService executor,
      Runnable task,
      String name,
      String cacheKey,
      long epoch,
      long generation) {
    String key = name.toLowerCase(Locale.ROOT) + "|" + epoch + "|" + generation;
    Flight flight = pending.get(key);
    if (flight != null) {
      if (flight.accepted.size() < 64 && flight.accepted.add(cacheKey))
        flight.waiting.put(cacheKey, task);
      return;
    }
    if (pending.size() >= 66) return;
    flight = new Flight(key);
    flight.waiting.put(cacheKey, task);
    flight.accepted.add(cacheKey);
    pending.put(key, flight);
    final Flight work = flight;
    try {
      executor.execute(() -> drain(work));
    } catch (RejectedExecutionException e) {
      pending.remove(key);
    }
  }

  private void drain(Flight flight) {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        Runnable task;
        synchronized (this) {
          if (pending.get(flight.key) != flight) return;
          if (flight.waiting.isEmpty()) {
            pending.remove(flight.key);
            return;
          }
          Iterator<Map.Entry<String, Runnable>> i = flight.waiting.entrySet().iterator();
          task = i.next().getValue();
          i.remove();
        }
        task.run();
      }
    } finally {
      synchronized (this) {
        if (pending.get(flight.key) == flight) pending.remove(flight.key);
      }
    }
  }

  public synchronized void clear() {
    pending.clear();
    retries.clear();
  }

  public synchronized int size() {
    return pending.size();
  }
}
