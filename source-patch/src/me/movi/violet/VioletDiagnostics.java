package me.movi.violet;

/** Opt-in, bounded, client-thread diagnostics. Never stores keys, usernames or chat. */
public final class VioletDiagnostics {
    private static final int CAPACITY = 128;
    private static final long[] times = new long[CAPACITY];
    private static final String[] events = new String[CAPACITY];
    private static int next, size;
    private static boolean enabled;
    private static String state = "";
    private static long physical, generated, ticks, totalTickNanos, maxTickNanos, renders, maxRenderNanos;

    private VioletDiagnostics() {}
    public static void enabled(boolean value) {
        if (enabled != value) { clear(); enabled = value; }
    }
    public static boolean enabled() { return enabled; }
    public static void physical(long now) { if (enabled) { physical++; record("physical", now); } }
    public static void generated(int count, long now) { if (enabled && count > 0) { generated += count; record("generated", now); } }
    public static void state(String value, long now) {
        if (enabled && !state.equals(value)) { state = value; record(value, now); }
    }
    public static void tick(long elapsed) {
        if (enabled && elapsed >= 0) { ticks++; totalTickNanos += elapsed; maxTickNanos = Math.max(maxTickNanos, elapsed); }
    }
    public static void render(long elapsed) {
        if (enabled && elapsed >= 0) { renders++; maxRenderNanos = Math.max(maxRenderNanos, elapsed); }
    }
    private static void record(String event, long now) {
        times[next] = now; events[next] = event; next = (next + 1) % CAPACITY; size = Math.min(CAPACITY, size + 1);
    }
    /** Snapshot only on an explicit diagnostics request, never from the render loop. */
    public static String report() {
        StringBuilder b = new StringBuilder("Violet diagnostics (memory only)\n");
        b.append("enabled=").append(enabled).append(" physical=").append(physical).append(" generated=").append(generated);
        b.append(" ticks=").append(ticks).append(" meanTickNs=").append(ticks == 0 ? 0 : totalTickNanos / ticks);
        b.append(" maxTickNs=").append(maxTickNanos).append(" renders=").append(renders).append(" maxRenderNs=").append(maxRenderNanos).append('\n');
        for (int i = 0; i < size; i++) { int at = (next - size + i + CAPACITY) % CAPACITY; b.append(times[at]).append(' ').append(events[at]).append('\n'); }
        return b.toString();
    }
    public static void clear() {
        java.util.Arrays.fill(events, null); next = size = 0; state = "";
        physical = generated = ticks = totalTickNanos = maxTickNanos = renders = maxRenderNanos = 0;
    }
}
