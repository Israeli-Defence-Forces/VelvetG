package me.movi.violet;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Cache at most 64 explicit block identifiers; no parsing work until settings change. */
public final class BlockFilter {
    private String previous = "";
    private final Set<String> ids = new HashSet<String>();
    public boolean allows(String block, int mode, String list) {
        if (mode == 0) return true;
        list = list == null ? "" : list.substring(0, Math.min(512, list.length()));
        if (!list.equals(previous)) {
            previous = list; ids.clear();
            for (String value : list.split(",", 65)) {
                value = value.trim().toLowerCase(Locale.ROOT);
                if (value.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+") && ids.size() < 64) ids.add(value.indexOf(':') < 0 ? "minecraft:" + value : value);
            }
        }
        boolean contains = ids.contains(block);
        return mode == 1 ? contains : mode == 2 && !contains;
    }
}
