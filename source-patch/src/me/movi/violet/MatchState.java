package me.movi.violet;

import java.util.Locale;

/** Text adapters operate on the visible sidebar only; ambiguous formats remain UNKNOWN. */
public final class MatchState {
    public enum State { DISCONNECTED, UNKNOWN, LOBBY, WAITING, ACTIVE, COMPLETE }
    private MatchState() {}
    public static State classify(boolean connected, String[] lines, String marker) {
        if (!connected) return State.DISCONNECTED;
        StringBuilder text = new StringBuilder();
        for (String line : lines) if (line != null) text.append(line.replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase(Locale.ROOT)).append('\n');
        String value = text.toString();
        marker = marker == null ? "" : marker.trim().toLowerCase(Locale.ROOT);
        if (marker.isEmpty() || !(value.contains(marker) || marker.equals("bedwars") && value.contains("bed wars"))) return State.UNKNOWN;
        if (value.contains("victory") || value.contains("game over") || value.contains("winner") || value.contains("match ended")) return State.COMPLETE;
        if (value.contains("starting in") || value.contains("waiting") || value.contains("players:")) return State.WAITING;
        if (value.contains("lobby") || value.contains("click to play")) return State.LOBBY;
        if (value.contains("diamond") || value.contains("emerald") || value.contains("beds left") || value.contains("bed gone") || value.contains("beds:")) return State.ACTIVE;
        return State.UNKNOWN;
    }
}
