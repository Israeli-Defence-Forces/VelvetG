package me.eldodebug.soar.lean;

import java.io.IOException;
import java.util.*;
import me.eldodebug.soar.management.mods.impl.ScoutMod;
import vgstub.*;

/** Native checkbox/reorder editor. The shared request service is never touched by this screen. */
public final class ColumnScreen extends axu {
  private final axu parent;
  private final String key;
  private final List<String> order = new ArrayList<>();
  private final Set<String> enabled = new HashSet<>();
  private int page, pageSize = 6;
  private String message = "Toggle columns; Up/Down changes their order. Apply saves.";

  public ColumnScreen(axu parent, String key) {
    this.parent = parent;
    this.key = key;
    String value = LeanSettings.get(key);
    if (value.equals("INHERIT"))
      value = LeanSettings.get(key.startsWith("hud") ? "hudColumns" : "tabColumns");
    if (value.equals("EXISTING") || value.equals("INHERIT")) {
      ScoutMod mod = ScoutMod.getInstance();
      value =
          mod == null
              ? "NAME,FKDR,WLR"
              : String.join(",", mod.getConfig().columns(key.startsWith("hud")));
    }
    for (String item : value.toUpperCase(Locale.ROOT).split(","))
      if (Arrays.asList(
                  "NAME", "LV", "FKDR", "WLR", "HWS", "FK", "WINS", "BEDS", "GUILD", "RANK", "PING",
                  "HEALTH", "INSIGHT")
              .contains(item)
          && !order.contains(item)) {
        order.add(item);
        enabled.add(item);
      }
    for (String item :
        Arrays.asList(
            "NAME", "LV", "FKDR", "WLR", "HWS", "FK", "WINS", "BEDS", "GUILD", "RANK", "PING",
            "HEALTH", "INSIGHT")) if (!order.contains(item)) order.add(item);
  }

  public void b() {
    pageSize = Math.max(1, Math.min(6, (m - 110) / 23));
    refresh();
  }

  private void refresh() {
    page = Math.min(page, Math.max(0, (order.size() - 1) / pageSize));
    n.clear();
    int count = Math.min(pageSize, order.size() - page * pageSize);
    for (int i = 0; i < count; i++) {
      String item = order.get(page * pageSize + i);
      int y = 48 + i * 23;
      n.add(new avs(i, l / 2 - 135, y, 170, 20, (enabled.contains(item) ? "[x] " : "[ ] ") + item));
      n.add(new avs(100 + i, l / 2 + 39, y, 44, 20, "Up"));
      n.add(new avs(200 + i, l / 2 + 87, y, 48, 20, "Down"));
    }
    n.add(new avs(20, l / 2 - 135, m - 50, 65, 20, "Previous"));
    n.add(new avs(21, l / 2 - 66, m - 50, 50, 20, "Next"));
    n.add(new avs(22, l / 2 - 12, m - 50, 58, 20, "Apply"));
    n.add(new avs(23, l / 2 + 50, m - 50, 85, 20, "Cancel"));
    n.add(new avs(24, l / 2 - 75, m - 26, 150, 20, "Use inherited/native columns"));
  }

  public void a(int x, int y, float partial) {
    c();
    a(q, "Columns · " + key, l / 2, 16, 0xffffff);
    a(q, message, l / 2, 31, 0xaaaaaa);
    super.a(x, y, partial);
  }

  protected void a(avs button) {
    int id = button.k;
    try {
      if (id >= 0 && id < 6 && page * pageSize + id < order.size()) {
        String column = order.get(page * pageSize + id);
        if (!enabled.remove(column)) enabled.add(column);
      } else if (id >= 100 && id < 106) {
        int index = page * pageSize + id - 100;
        if (index > 0 && index < order.size()) Collections.swap(order, index, index - 1);
      } else if (id >= 200 && id < 206) {
        int index = page * pageSize + id - 200;
        if (index >= 0 && index + 1 < order.size()) Collections.swap(order, index, index + 1);
      } else if (id == 20) page = Math.max(0, page - 1);
      else if (id == 21 && ((page + 1) * pageSize) < order.size()) page++;
      else if (id == 22) {
        List<String> selected = new ArrayList<>();
        for (String column : order) if (enabled.contains(column)) selected.add(column);
        if (selected.isEmpty()) {
          message = "Keep at least one column enabled";
          return;
        }
        LeanSettings.set(key, String.join(",", selected));
        j.a(parent);
        return;
      } else if (id == 23) {
        j.a(parent);
        return;
      } else if (id == 24) {
        LeanSettings.set(
            key, key.contains("Waiting") || key.contains("Game") ? "INHERIT" : "EXISTING");
        j.a(parent);
        return;
      }
    } catch (IOException e) {
      message = "Could not save columns";
    }
    refresh();
  }

  protected void a(char character, int code) {
    if (code == 1) j.a(parent);
  }

  public boolean d() {
    return false;
  }
}
