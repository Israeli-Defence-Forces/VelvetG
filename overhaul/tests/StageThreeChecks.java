package me.eldodebug.soar.lean;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import me.eldodebug.soar.scout.*;

/** CPU state/compatibility tests only. Native drawing is not executed. */
public final class StageThreeChecks {
  static int checks;

  static void ok(boolean value, String name) {
    if (!value) throw new AssertionError(name);
    checks++;
  }

  static boolean near(float a, float b) {
    return Math.abs(a - b) < .001;
  }

  public static void main(String[] args) throws Exception {
    LeanSettings.preset("CUSTOM");
    LeanSettings.set("animation", "BOUNCE");
    LeanSettings.set("animationMs", "420");
    LeanSettings.set("tabImageOpacity", "37");
    LeanSettings.preset("MAXIMUM_FPS");
    ok(
        LeanSettings.on("maximumFPS") && LeanSettings.animation().equals("NONE"),
        "maximum FPS overrides cosmetics");
    ok(!LeanSettings.on("headLayers"), "maximum FPS skips optional hat layer");
    LeanSettings.preset("BALANCED");
    ok(
        !LeanSettings.on("maximumFPS") && LeanSettings.animation().equals("NONE"),
        "balanced presentation");
    LeanSettings.preset("VISUAL");
    ok(LeanSettings.animation().equals("SLIDE"), "visual presentation");
    LeanSettings.preset("CUSTOM");
    ok(
        LeanSettings.animation().equals("BOUNCE")
            && LeanSettings.get("animationMs").equals("420")
            && LeanSettings.get("tabImageOpacity").equals("37"),
        "custom appearance survives every preset");
    try {
      LeanSettings.preset("INVALID");
      throw new AssertionError("invalid preset accepted");
    } catch (java.io.IOException expected) {
      checks++;
    }
    TableSupport.context(false);
    ok(TableSupport.scalar(9, "FontSize") == 9, "inherited font scalar fallback");
    ok(TableSupport.number("EntryMs", 180, 50, 5000) == 180, "inherited duration fallback");
    LeanSettings.set("tabFontSize", "14");
    ok(TableSupport.scalar(9, "FontSize") == 14, "numeric scalar");
    LeanSettings.set("tabFontSize", "EXISTING");
    List<ScoutRow> rows = new ArrayList<>();
    for (int i = 0; i < 40; i++)
      rows.add(new ScoutRow("Player" + i, null, 30, 0xffffff, false, false, null));
    ok(TableSupport.prepare(rows, false) == rows, "default roster allocation-free identity");
    ok(TableSupport.prepare(Collections.emptyList(), true).isEmpty(), "empty roster");
    ok(TableSupport.prepare(null, false).isEmpty(), "null roster");
    LeanSettings.set("tabLimit", "7");
    List<ScoutRow> trimmed = TableSupport.prepare(rows, false);
    ok(trimmed.size() == 7 && rows.size() == 40, "cap preserves source roster");
    trimmed.clear();
    ok(rows.size() == 40, "capped result owns storage");
    LeanSettings.set("tabLimit", "40");
    LeanSettings.set("tabSort", "WINS");
    List<ScoutRow> sorted = TableSupport.prepare(rows, false);
    ok(sorted != rows && sorted.size() == 40, "explicit sort owns storage");
    ok(rows.get(2).name.equals("Player2"), "source order unchanged by sort");
    LeanSettings.set("tabSort", "EXISTING");
    ok(TableSupport.prepare(rows, false) == rows, "revision invalidates preparation cache");
    LeanSettings.set("tabGameLimit", "4");
    ok(
        TableSupport.prepare(rows, false, ScoutState.Phase.ACTIVE, null).size() == 4,
        "phase invalidates preparation cache");
    ok(
        !TableSupport.needsTable(null, false, null, false),
        "hidden null tables do not force per-tick rebuild");
    ok(TableSupport.needsTable(null, true, null, false), "visible missing TAB rebuilds");
    ok(TableSupport.needsTable(null, false, null, true), "visible missing HUD rebuilds");
    VisibilityMotion m = new VisibilityMotion();
    ok(near(m.update(true, 0, 100, true), 0), "show starts hidden");
    ok(near(m.update(true, 50000000, 100, true), .5f), "show midpoint");
    ok(near(m.update(false, 50000000, 100, true), .5f), "reversal has no jump");
    ok(near(m.update(false, 100000000, 100, true), .25f), "hide midpoint after reversal");
    ok(near(m.update(false, 150000000, 100, true), 0), "hide completes");
    ok(near(m.update(true, 200000000, 100, false), 1), "disabled motion immediate show");
    ok(near(m.update(false, 200000000, 100, false), 0), "disabled motion immediate hide");
    m.clear();
    ok(near(m.update(false, 300000000, 100, true), 0), "reset does not retain old world");
    ok(near(m.update(true, 350000000, 100, true), 0), "independent next entry");
    VisibilityMotion hud = new VisibilityMotion();
    ok(near(hud.update(true, 0, 400, true), 0), "HUD starts separately");
    ok(near(hud.update(true, 200000000, 400, true), .5f), "independent HUD duration");
    LeanSettings.set("tabWaitingHideAnimation", "ZOOM");
    LeanSettings.set("hudGameHideAnimation", "FADE");
    ok(
        LeanSettings.panel(false, "HideAnimation", "WAITING").equals("ZOOM")
            && LeanSettings.panel(true, "HideAnimation", "ACTIVE").equals("FADE"),
        "independent context hide styles");
    NameLabel name = new NameLabel("§7FKDR 2.00");
    String first = name.beside("§cPlayer");
    ok(first == name.beside(new String("§cPlayer")), "cached nametag join");
    ok(!name.beside("§9Player").equals(first), "team formatting invalidates nametag join");
    ok(name.beside("§9Player").contains("FKDR 2.00"), "cached stat preserved");
    TableEffects.alpha = .4f;
    ok(near(TableEffects.rowAlpha(.5f), .2f), "row alpha composes with table visibility");
    InitProbe.begin();
    InitProbe.glideBegin();
    InitProbe.mark("modulesInitialized");
    InitProbe.complete();
    ConfigWriter.shutdown();
    Properties timeline = new Properties();
    try (java.io.InputStream in =
        Files.newInputStream(Paths.get("config/velvetg-startup.properties"))) {
      timeline.load(in);
    }
    ok(timeline.getProperty("release").equals("Overhaul3"), "startup release tagged");
    ok(timeline.getProperty("stage.00.name").equals("glideStart"), "startup order retained");
    ok(Double.parseDouble(timeline.getProperty("totalMs")) >= 0, "startup duration numeric");
    Field frames = FrameProbe.class.getDeclaredField("frames");
    frames.setAccessible(true);
    long[] samples = (long[]) frames.get(null);
    samples[0] = 8000000;
    samples[1] = 10000000;
    Field fn = FrameProbe.class.getDeclaredField("fn");
    fn.setAccessible(true);
    fn.setInt(null, 2);
    FrameProbe.export();
    List<String> csv = Files.readAllLines(Paths.get("config/velvetg-frame-samples.csv"));
    ok(
        csv.size() == 3 && csv.get(1).equals("0,8.000000") && csv.get(2).equals("1,10.000000"),
        "bounded raw interval export");
    ImageLayout geometry = new ImageLayout();
    geometry.compute(10, 20, 200, 100, 100, 100, "CONTAIN", "RIGHT", 1);
    ok(
        geometry.clipX == 110 && geometry.clipWidth == 100 && geometry.clipHeight == 100,
        "contain clips to image instead of repeating edge texels");
    geometry.compute(10, 20, 200, 100, 100, 100, "COVER", "CENTER", 1);
    ok(
        geometry.clipX == 10
            && geometry.clipY == 20
            && geometry.clipWidth == 200
            && geometry.clipHeight == 100,
        "cover clips to panel bounds");
    FrameProbe.reset();
    ok(
        FrameProbe.summary().contains("Frame interval no samples"),
        "manual benchmark window resets old samples");
    System.out.println(
        "PASS "
            + checks
            + " Overhaul3 preparation, motion, preset, hidden-model, label and startup/export"
            + " checks");
  }
}
