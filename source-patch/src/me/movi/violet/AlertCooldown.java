package me.movi.violet;

import java.util.LinkedHashMap;

public final class AlertCooldown {
    private final LinkedHashMap<String, Long> seen = new LinkedHashMap<String, Long>();
    private long soundAt = Long.MIN_VALUE;
    public boolean accept(String key, long now, long cooldown) {
        Long previous = seen.get(key);
        if (previous != null && now - previous < cooldown) return false;
        seen.remove(key); seen.put(key, now);
        if (seen.size() > 128) seen.remove(seen.keySet().iterator().next());
        return true;
    }
    public boolean sound(long now) {
        if (soundAt != Long.MIN_VALUE && now - soundAt < 1500000000L) return false;
        soundAt = now; return true;
    }
    public void clear() { seen.clear(); soundAt = Long.MIN_VALUE; }
}
