package me.eldodebug.soar.lean;

import java.util.*;
import java.util.function.LongSupplier;
import me.eldodebug.soar.scout.*;
import me.eldodebug.soar.scout.ui.*;

/**
 * Retains at most 40 already-renderable rows for optional exits; no fetches or native allocation.
 */
public final class RowLifecycle {
  public interface Named {
    String rowName();
  }

  private static final State[] states = {new State(), new State()};
  static LongSupplier clock = System::nanoTime;

  private static final class Item {
    Named row;
    long exit = -1;
    float x, y, alpha = 1;
    boolean positioned;

    Item(Named row) {
      this.row = row;
    }
  }

  private static final class State {
    Map<String, Item> rows = new LinkedHashMap<>();
    List<String> columns = Collections.emptyList();
    long generation = -1, revision = -1;
    ScoutState.Phase phase;
  }

  public static ScoutLayoutEngine.Result layout(
      List<String> columns,
      float[] widths,
      boolean heads,
      int ignored,
      boolean hud,
      ScoutLayoutMetrics metrics,
      List<Named> rows,
      ScoutState.Phase phase,
      boolean preview) {
    if (!preview) reconcile(rows, hud, columns, phase, Scout105Runtime.sessionGeneration());
    return ScoutLayoutEngine.compute(columns, widths, heads, rows.size(), hud, metrics);
  }

  static void reconcile(
      List<Named> rows,
      boolean hud,
      List<String> columns,
      ScoutState.Phase phase,
      long generation) {
    State state = states[hud ? 1 : 0];
    if (state.generation != generation
        || state.phase != phase
        || state.revision != LeanSettings.revision()
        || !state.columns.equals(columns)) {
      state.rows.clear();
      state.generation = generation;
      state.phase = phase;
      state.revision = LeanSettings.revision();
      state.columns = new ArrayList<>(columns);
    }
    boolean motion =
        TableSupport.animationsEnabled() && !TableSupport.value("ExitAnimation").equals("NONE");
    long now = clock.getAsLong(),
        duration = TableSupport.number("ExitMs", 180, 50, 5000) * 1000000L;
    Map<String, Item> next = new LinkedHashMap<>();
    for (Named row : rows) {
      String key = row.rowName().toLowerCase(Locale.ROOT);
      Item item = state.rows.get(key);
      if (item == null) item = new Item(row);
      item.row = row;
      item.exit = -1;
      next.put(key, item);
    }
    if (motion)
      for (Map.Entry<String, Item> entry : state.rows.entrySet()) {
        if (next.containsKey(entry.getKey()) || next.size() >= 40) continue;
        Item item = entry.getValue();
        if (item.exit < 0) item.exit = now;
        if (now - item.exit >= duration) continue;
        next.put(entry.getKey(), item);
        rows.add(item.row);
      }
    state.rows = next;
  }

  public static ScoutAnimationState.Row row(
      ScoutAnimationState.Context context,
      String key,
      float x,
      float y,
      float delta,
      int duration,
      boolean animations) {
    if (TableSupport.preview()) return context.row(key, x, y, delta, duration, animations);
    Item item = states[TableSupport.isHud() ? 1 : 0].rows.get(key);
    boolean exiting = item != null && item.exit >= 0;
    if (exiting && item.positioned) {
      x = item.x;
      y = item.y;
    }
    ScoutAnimationState.Row row = context.row(key, x, y, delta, duration, animations);
    if (exiting) {
      float progress =
          Math.max(
              0,
              Math.min(
                  1,
                  (clock.getAsLong() - item.exit)
                      / (TableSupport.number("ExitMs", 180, 50, 5000) * 1000000f)));
      row.alpha = TableSupport.animationsEnabled() ? item.alpha * (1 - progress) : 0;
      if (TableSupport.value("ExitAnimation").equals("SLIDE")) row.x += 12 * progress;
    } else if (item != null) {
      item.x = row.x;
      item.y = row.y;
      item.alpha = row.alpha;
      item.positioned = true;
    }
    return row;
  }

  public static void clear() {
    for (State state : states) {
      state.rows.clear();
      state.generation = -1;
      state.phase = null;
    }
  }

  static int size(boolean hud) {
    return states[hud ? 1 : 0].rows.size();
  }
}
