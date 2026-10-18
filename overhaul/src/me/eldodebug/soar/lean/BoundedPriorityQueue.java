package me.eldodebug.soar.lean;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Admission bound also applies to manual /stats jobs. Executor rejection completes their futures.
 */
public final class BoundedPriorityQueue extends PriorityBlockingQueue<Runnable> {
  public synchronized boolean offer(Runnable r) {
    return size() < 160 && super.offer(r);
  }
}
