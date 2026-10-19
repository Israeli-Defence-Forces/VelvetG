package me.eldodebug.soar.lean;

/** Pure owner-thread visibility state. Reversal starts from the current value, without a jump. */
public final class VisibilityMotion {
  private float start, target, value;
  private long began;
  private int duration;
  private boolean initialized;

  public float update(boolean visible, long now, int milliseconds, boolean animated) {
    float next = visible ? 1 : 0;
    if (!initialized) {
      initialized = true;
      target = 0;
      value = 0;
      start = 0;
      began = now;
    }
    sample(now);
    if (!animated) {
      start = target = value = next;
      began = now;
      return value;
    }
    if (next != target) {
      start = value;
      target = next;
      began = now;
      duration = Math.max(1, milliseconds);
    }
    return sample(now);
  }

  private float sample(long now) {
    float t = Math.max(0, Math.min(1, (now - began) / (Math.max(1, duration) * 1000000f)));
    float eased = t * t * (3 - 2 * t);
    return value = start + (target - start) * eased;
  }

  public void clear() {
    initialized = false;
    start = target = value = 0;
  }
}
