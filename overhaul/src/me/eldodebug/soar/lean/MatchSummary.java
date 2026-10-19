package me.eldodebug.soar.lean;

import java.util.*;

/** Reads only counters explicitly present in the visible server TAB text. No identity inference. */
public final class MatchSummary {
  public static String parse(String text) {
    if (text == null || text.length() > 16384) return null;
    String clean =
        text.replaceAll("§.", "").toLowerCase(Locale.ROOT).replace('\u00a0', ' ').replace('：', ':');
    String[] tokens = clean.replaceAll("[^a-z0-9]+", " ").trim().split(" +");
    Integer beds = null, kills = null, finals = null;
    for (int i = 0; i < tokens.length; i++) {
      if ((tokens[i].equals("bed") || tokens[i].equals("beds"))
          && i + 2 < tokens.length
          && tokens[i + 1].equals("destroyed")) beds = number(tokens[i + 2]);
      if (tokens[i].equals("final")
          && i + 2 < tokens.length
          && (tokens[i + 1].equals("kill") || tokens[i + 1].equals("kills")))
        finals = number(tokens[i + 2]);
      if ((tokens[i].equals("kill") || tokens[i].equals("kills"))
          && i + 1 < tokens.length
          && (i == 0 || !tokens[i - 1].equals("final"))) kills = number(tokens[i + 1]);
    }
    return beds == null || kills == null || finals == null
        ? null
        : "MATCH · " + beds + " beds · " + kills + " kills · " + finals + " finals";
  }

  private static Integer number(String token) {
    try {
      int n = Integer.parseInt(token);
      return n >= 0 ? n : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
