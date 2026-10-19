package me.eldodebug.soar.lean;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import me.eldodebug.soar.scout.*;

/** CPU/config/scheduler geometry checks; does not initialize OpenGL or a Minecraft window. */
public final class StageTwoChecks {
  static int checks;

  static void ok(boolean test, String name) {
    if (!test) throw new AssertionError(name);
    checks++;
  }

  static void rejects(String key, String value) throws Exception {
    try {
      LeanSettings.set(key, value);
      throw new AssertionError("Accepted invalid " + key);
    } catch (java.io.IOException expected) {
      checks++;
    }
  }

  public static void main(String[] args) throws Exception {
    Map<String, String> changes = new LinkedHashMap<>();
    changes.put("tabColumns", "NAME,FKDR,PING");
    changes.put("hudColumns", "NAME,WLR");
    changes.put("tabWaitingColumns", "NAME,LV,FKDR");
    changes.put("tabGameColumns", "NAME,HEALTH");
    changes.put("tabWaitingLimit", "40");
    changes.put("tabGameLimit", "8");
    LeanSettings.setAll(changes);
    ok(LeanSettings.panel(false, "Columns", "WAITING").equals("NAME,LV,FKDR"), "waiting profile");
    ok(LeanSettings.panel(false, "Columns", "ACTIVE").equals("NAME,HEALTH"), "active profile");
    ok(
        LeanSettings.panel(true, "Columns", "ACTIVE").equals("NAME,WLR"),
        "independent HUD inheritance");
    List<ScoutRow> rows = new ArrayList<>();
    for (int i = 0; i < 40; i++)
      rows.add(new ScoutRow("Player" + i, null, 30, 0xffffff, false, false, null));
    ok(
        TableSupport.prepare(rows, false, ScoutState.Phase.WAITING, null).size() == 40,
        "full 40-player lobby");
    ok(
        TableSupport.columns(Collections.emptyList(), false)
            .equals(Arrays.asList("NAME", "LV", "FKDR")),
        "waiting columns");
    List<String> cached = TableSupport.columns(Collections.emptyList(), false);
    ok(
        cached == TableSupport.columns(Collections.emptyList(), false),
        "column lists cached without frame allocation");
    ok(
        TableSupport.prepare(rows, false, ScoutState.Phase.ACTIVE, null).size() == 8,
        "game cap independent");
    ok(
        TableSupport.columns(Collections.emptyList(), false)
            .equals(Arrays.asList("NAME", "HEALTH")),
        "game columns");
    LeanSettings.set("tabGameHeader", "false");
    ok(!TableSupport.flag(true, "Header"), "game header override");
    TableSupport.prepare(rows, true, ScoutState.Phase.ACTIVE, null);
    ok(TableSupport.flag(true, "Header"), "HUD header not changed");
    LeanSettings.set("tabPingUnits", "false");
    TableSupport.context(false);
    ok(TableSupport.cell("123ms", "PING").equals("123"), "ping units removed");
    ok(TableSupport.cell("123ms", "NAME").equals("123ms"), "valid player name preserved");
    rejects("autoKey", "yes");
    rejects("tabLimit", "41");
    rejects("tabGameLimit", "0");
    rejects("tabFont", "../../secret.ttf");
    rejects("hudBackground", "../image.png");
    rejects("tabColumns", "NAME;SECRET");
    rejects("tabShowAnimation", "UNKNOWN");
    rejects("tabImageOpacity", "101");
    long revision = LeanSettings.revision();
    Map<String, String> invalid = new LinkedHashMap<>();
    invalid.put("tabLimit", "12");
    invalid.put("autoKey", "yes");
    try {
      LeanSettings.setAll(invalid);
      throw new AssertionError("partial batch accepted");
    } catch (java.io.IOException expected) {
    }
    ok(
        LeanSettings.get("tabLimit").equals("40") && LeanSettings.revision() == revision,
        "invalid batch leaves all settings untouched");
    LeanSettings.exportSettings();
    String exported =
        new String(
            Files.readAllBytes(Paths.get("config/velvetg-lean-export.properties")), "ISO-8859-1");
    ok(
        !exported.contains("pika-api-key") && !exported.contains("X-Api-Key"),
        "no credentials exported");
    LeanSettings.set("tabWaitingLimit", "20");
    LeanSettings.importSettings();
    ok(LeanSettings.get("tabWaitingLimit").equals("40"), "context export/import round trip");
    LeanSettings.set("profileName", "TestProfile");
    LeanSettings.saveProfile();
    LeanSettings.set("tabGameLimit", "6");
    LeanSettings.loadProfile();
    ok(LeanSettings.get("tabGameLimit").equals("8"), "named non-sensitive profile round trip");
    rejects("profileName", "../escape");
    LeanSettings.set("tabGameTextColor", "AABBCC");
    ok(
        LeanSettings.panel(false, "TextColor", "ACTIVE").equals("AABBCC"),
        "independent scene text color");
    LeanSettings.preset("MAXIMUM_FPS");
    ok(LeanSettings.on("maximumFPS") && !TableSupport.animations(true), "maximum FPS bypass");
    ok(LeanSettings.get("tabGameColumns").equals("NAME,HEALTH"), "preset retains layout");
    LeanSettings.preset("BALANCED");
    ok(!LeanSettings.on("maximumFPS"), "FPS mode reversible");
    LeanSettings.set("animation", "EXISTING");
    LeanSettings.set("tabShowAnimation", "NONE");
    LeanSettings.set("tabEntryAnimation", "EXISTING");
    LeanSettings.set("tabResizeAnimation", "NONE");
    TableSupport.context(false);
    ok(
        TableSupport.entryEnabled(true) && !TableSupport.resizeEnabled(true),
        "show, entry and resize toggles are independent");
    LeanSettings.set("tabShowAnimation", "INHERIT");
    ImageLayout image = new ImageLayout();
    image.compute(10, 20, 200, 100, 100, 100, "COVER", "CENTER", 1);
    ok(image.width == 200 && image.height == 200 && image.y == -30, "cover crop geometry");
    image.compute(10, 20, 200, 100, 100, 100, "CONTAIN", "RIGHT", 1);
    ok(image.width == 100 && image.x == 110, "contain right alignment");
    image.compute(10, 20, 200, 100, 100, 50, "STRETCH", "TOP_LEFT", .5f);
    ok(
        image.width == 100 && image.height == 50 && image.x == 10 && image.y == 20,
        "zoom alignment");
    image.compute(0, 0, 100, 100, 0, 0, "COVER", "CENTER", 1);
    ok(image.width == 0, "invalid image dimensions");
    TabPlacement.observed(20, 40, 200, 100);
    TabPlacement.begin(640, 360);
    ok(!TabPlacement.down(5, 5), "drag only inside TAB");
    ok(TabPlacement.down(30, 50), "drag handle accepts panel");
    TabPlacement.move(1000, 1000);
    ok(TabPlacement.x() <= 440 && TabPlacement.y() <= 260, "drag clamp");
    TabPlacement.up();
    TabPlacement.save();
    ok(
        !TabPlacement.active() && LeanSettings.number("tabX", -1, 10000) <= 440,
        "drag saves once on Apply");
    ok(
        MatchSummary.parse("§aBeds Destroyed: 2 | Kills: 8\nFinal Kills: 3")
            .equals("MATCH · 2 beds · 8 kills · 3 finals"),
        "explicit visible match summary");
    ok(
        MatchSummary.parse("Final Kills: 3 Beds Destroyed: 2") == null,
        "missing ordinary kills not fabricated");
    ok(
        MatchSummary.parse("Beds Destroyed: 9999999999999999 Kills: 8 Final Kills: 1") == null,
        "counter overflow rejected");
    com.google.gson.JsonObject social =
        new com.google.gson.JsonParser()
            .parse("{\"username\":\"Local\",\"friends\":[\"Friend\"]}")
            .getAsJsonObject();
    ProfileExtras.record(social, new ScoutStats.Builder("Local"));
    ProfileExtras.local("Local");
    LeanSettings.set("friendHighlight", "true");
    TableSupport.prepare(Collections.emptyList(), false, ScoutState.Phase.WAITING, null);
    ScoutRow friend = new ScoutRow("Friend", null, 30, 0x5555ff, false, false, null);
    java.awt.Color originalColor = new java.awt.Color(0x5555ff);
    ok(
        TableSupport.color(originalColor, "NAME", friend).getRGB() != originalColor.getRGB(),
        "cached friend highlight in waiting room");
    TableSupport.prepare(Collections.emptyList(), false, ScoutState.Phase.ACTIVE, null);
    ok(
        TableSupport.color(originalColor, "NAME", friend) == originalColor,
        "team colors preserved during active match");
    ProfileExtras.clear();
    LeanSettings.set("nametagWaiting", "false");
    ok(
        !NativeHooks.tagPhase(ScoutState.Phase.WAITING)
            && NativeHooks.tagPhase(ScoutState.Phase.ACTIVE),
        "independent nametag context controls");
    AtomicLong clock = new AtomicLong();
    ProfileRequests profiles = new ProfileRequests(clock::get);
    ok(profiles.allowFetch("Tester", 0, 0), "profile first attempt");
    ok(!profiles.allowFetch("tester", 0, 0), "profile duplicate paced");
    clock.set(30000000001L);
    ok(profiles.allowFetch("Tester", 0, 0), "failed profile retry after cooldown");
    ok(profiles.allowFetch("Tester", 1, 0), "new epoch independent");
    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    AtomicInteger tasks = new AtomicInteger();
    ProfileRequests.execute(
        executor,
        () -> {
          started.countDown();
          try {
            release.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          tasks.incrementAndGet();
        },
        profiles,
        "Tester",
        "mode1",
        0,
        0);
    ok(started.await(1, TimeUnit.SECONDS), "group starts");
    for (int i = 0; i < 100; i++)
      ProfileRequests.execute(executor, tasks::incrementAndGet, profiles, "Tester", "mode1", 0, 0);
    ProfileRequests.execute(executor, tasks::incrementAndGet, profiles, "tester", "mode2", 0, 0);
    ok(profiles.size() == 1, "one pending group per player across modes");
    release.countDown();
    executor.shutdown();
    ok(
        executor.awaitTermination(2, TimeUnit.SECONDS) && tasks.get() == 2,
        "mode callbacks coalesced with exact duplicate admission removed");
    profiles.clear();
    ok(profiles.size() == 0, "disconnect clears profile groups");
    TableSupport.prepare(Collections.emptyList(), false, ScoutState.Phase.WAITING, null);
    LeanSettings.set("animation", "EXISTING");
    LeanSettings.set("tabExitAnimation", "FADE");
    LeanSettings.set("tabExitMs", "200");
    AtomicLong motionTime = new AtomicLong(1000000000L);
    RowLifecycle.clock = motionTime::get;
    RowLifecycle.Named first = () -> "First", second = () -> "Second";
    List<RowLifecycle.Named> visible = new ArrayList<>(Arrays.asList(first, second));
    RowLifecycle.reconcile(visible, false, Arrays.asList("NAME"), ScoutState.Phase.WAITING, 1);
    ok(RowLifecycle.size(false) == 2, "live rows recorded");
    Class<?> rendered = Class.forName("me.eldodebug.soar.scout.ScoutTable$RenderedRow");
    java.lang.reflect.Constructor<?> constructor = rendered.getDeclaredConstructor(ScoutRow.class);
    constructor.setAccessible(true);
    Object actualRow =
        constructor.newInstance(
            new ScoutRow("ActualPlayer", null, 20, 0xffffff, false, false, null));
    ok(
        ((RowLifecycle.Named) actualRow).rowName().equals("ActualPlayer"),
        "injected native row interface executes on Java 8");
    me.eldodebug.soar.scout.ui.ScoutAnimationState.Context nativeMotion =
        new me.eldodebug.soar.scout.ui.ScoutAnimationState.Context();
    RowLifecycle.row(nativeMotion, "first", 20, 40, .05f, 140, false);

    motionTime.addAndGet(100000000L);
    visible = new ArrayList<>(Arrays.asList(second));
    RowLifecycle.reconcile(visible, false, Arrays.asList("NAME"), ScoutState.Phase.WAITING, 1);
    ok(visible.size() == 2, "departing row retained for exit");
    motionTime.addAndGet(100000000L);
    me.eldodebug.soar.scout.ui.ScoutAnimationState.Row fading =
        RowLifecycle.row(nativeMotion, "first", 200, 400, .05f, 140, false);
    ok(fading.x == 20 && fading.y == 40, "departing row holds its cached screen position");
    ok(
        fading.alpha > .49f && fading.alpha < .51f,
        "native row adapter applies half-duration exit fade");

    motionTime.addAndGet(300000000L);
    visible = new ArrayList<>(Arrays.asList(second));
    RowLifecycle.reconcile(visible, false, Arrays.asList("NAME"), ScoutState.Phase.WAITING, 1);
    ok(visible.size() == 1 && RowLifecycle.size(false) == 1, "expired exit row released");
    List<RowLifecycle.Named> full = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      final int number = i;
      full.add(() -> "New" + number);
    }
    RowLifecycle.reconcile(full, false, Arrays.asList("NAME"), ScoutState.Phase.WAITING, 1);
    ok(full.size() == 40 && RowLifecycle.size(false) == 40, "exit rows never exceed 40-player cap");
    visible = new ArrayList<>();
    RowLifecycle.reconcile(visible, false, Arrays.asList("NAME"), ScoutState.Phase.ACTIVE, 2);
    ok(visible.isEmpty(), "phase/generation transition drops prior rows");
    RowLifecycle.clear();
    RowLifecycle.clock = System::nanoTime;
    System.out.println(
        "PASS "
            + checks
            + " context settings, safe validation, 40-player limits, drag/image geometry, match"
            + " counters, nametag scopes and profile-group checks (no GPU/game launch)");
  }
}
