package me.eldodebug.soar.lean;

import com.google.gson.*;
import java.util.*;
import me.eldodebug.soar.scout.ScoutStats;

public final class ProfileExtras {
  private static final LinkedHashMap<String, Extra> DATA = new LinkedHashMap<>(64, .75f, true);
  private static String local = "";

  private static final class Extra {
    String guild = "";
    Set<String> friends = new HashSet<>();
  }

  public static synchronized void record(JsonObject j, ScoutStats.Builder b) {
    try {
      Extra x = new Extra();
      if (j.has("clan") && j.get("clan").isJsonObject()) {
        JsonObject c = j.getAsJsonObject("clan");
        if (c.has("name")) x.guild = safe(c.get("name").getAsString(), 24);
      }
      if (j.has("friends") && j.get("friends").isJsonArray()) {
        int n = 0;
        for (JsonElement e : j.getAsJsonArray("friends")) {
          if (++n > 1000) break;
          String s =
              e.isJsonObject() && e.getAsJsonObject().has("username")
                  ? e.getAsJsonObject().get("username").getAsString()
                  : e.isJsonPrimitive() ? e.getAsString() : "";
          if (s.matches("[A-Za-z0-9_]{3,16}")) x.friends.add(s.toLowerCase(Locale.ROOT));
        }
      }
      DATA.put(b.username.toLowerCase(Locale.ROOT), x);
      while (DATA.size() > 500) DATA.remove(DATA.keySet().iterator().next());
    } catch (RuntimeException ignored) {
    }
  }

  private static String safe(String s, int n) {
    return s.replaceAll("[^A-Za-z0-9 _.-]", "")
        .substring(0, Math.min(n, s.replaceAll("[^A-Za-z0-9 _.-]", "").length()));
  }

  public static synchronized String guild(String s) {
    Extra e = DATA.get(s.toLowerCase(Locale.ROOT));
    return e == null || e.guild.isEmpty() ? "—" : e.guild;
  }

  public static synchronized void local(String s) {
    local = s == null ? "" : s.toLowerCase(Locale.ROOT);
  }

  public static synchronized boolean friend(String s) {
    Extra e = DATA.get(local);
    return e != null && e.friends.contains(s.toLowerCase(Locale.ROOT));
  }

  public static synchronized void clear() {
    DATA.clear();
    local = "";
  }
}
