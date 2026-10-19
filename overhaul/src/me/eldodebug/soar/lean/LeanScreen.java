package me.eldodebug.soar.lean;

import java.io.IOException;
import java.util.*;
import vgstub.*;

/** Uses the installed Minecraft UI and font; no OneConfig/Fabric dependency. */
public final class LeanScreen extends axu {
  private avw search, edit;
  private String editing = "";
  private volatile String notice = "";
  private StringBuilder secret;
  private int page, pageSize = 6;
  private boolean placing;
  private int preset;
  private final List<String> visible = new ArrayList<>();

  public void b() {
    pageSize = Math.max(1, Math.min(6, (m - 180) / 23));
    search = new avw(100, q, l / 2 - 150, 36, 300, 18);
    search.f(64);
    edit = new avw(101, q, l / 2 - 150, m - 73, 300, 18);
    edit.f(512);
    refresh();
  }

  private void refresh() {
    n.clear();
    visible.clear();
    for (String k : LeanSettings.KEYS)
      if (k.toLowerCase(Locale.ROOT).contains(search.b().toLowerCase(Locale.ROOT))) visible.add(k);
    int count = Math.min(pageSize, visible.size() - page * pageSize);
    if (count < 0) {
      page = 0;
      count = Math.min(pageSize, visible.size());
    }
    for (int i = 0; i < count; i++) {
      String k = visible.get(page * pageSize + i);
      n.add(new avs(i, l / 2 - 150, 62 + i * 23, 300, 20, k + ": " + LeanSettings.get(k)));
    }
    n.add(new avs(20, l / 2 - 150, m - 48, 68, 20, "Previous"));
    n.add(new avs(21, l / 2 - 80, m - 48, 55, 20, "Next"));
    n.add(new avs(22, l / 2 - 23, m - 48, 70, 20, "API key"));
    n.add(new avs(23, l / 2 + 49, m - 48, 48, 20, "Apply"));
    n.add(new avs(24, l / 2 + 99, m - 48, 51, 20, "Done"));
    n.add(new avs(25, 8, 8, 70, 20, "Export"));
    n.add(new avs(26, 80, 8, 70, 20, "Import"));
    n.add(new avs(27, l - 108, 8, 100, 20, "Move TAB"));
    n.add(new avs(28, 8, m - 24, 96, 20, "Preset"));
    n.add(new avs(29, l - 104, m - 24, 96, 20, "API status"));
    n.add(new avs(30, l / 2 - 55, m - 24, 53, 20, "Save"));
    n.add(new avs(31, l / 2 + 2, m - 24, 53, 20, "Load"));
  }

  public void a(int x, int y, float partial) {
    c();
    if (placing) {
      NativeHooks.previewTab();
      int px = Math.round(TabPlacement.x()), py = Math.round(TabPlacement.y());
      int pw = Math.round(TabPlacement.width()), ph = Math.round(TabPlacement.height());
      a(px, py, px + pw, py + ph, 0x4466ccff);
      a(
          q,
          "Drag TAB · Enter saves · Esc cancels · grid " + LeanSettings.get("tabSnapGrid"),
          l / 2,
          12,
          0xffffff);
      a(
          q,
          "Scale is adjustable in tabScale; HUD uses its existing drag editor",
          l / 2,
          m - 20,
          0xffffff);
      return;
    }
    a(q, "VelvetG Lean · performance and integration", l / 2, 17, 0xffffff);
    search.g();
    if (secret != null) {
      a(q, "Enter API key (hidden); Apply saves locally", l / 2, m - 91, 0xffcc77);
      a(q, repeat('*', Math.min(48, secret.length())), l / 2, m - 69, 0xffffff);
    } else if (!editing.isEmpty()) {
      a(q, "Edit " + editing, l / 2, m - 91, 0xffcc77);
      a(q, q.a(LeanSettings.hint(editing), l - 16), l / 2, m - 115, 0x99ccff);
      edit.g();
    } else a(q, KeyVault.status(), l / 2, m - 91, 0xaaaaaa);
    if (!notice.isEmpty()) a(q, notice, l / 2, m - 103, 0x99ccff);
    super.a(x, y, partial);
  }

  private static String repeat(char c, int n) {
    char[] a = new char[n];
    Arrays.fill(a, c);
    return new String(a);
  }

  protected void a(avs button) {
    try {
      int id = button.k;
      if (id >= 0 && id < 6 && page * pageSize + id < visible.size()) {
        editing = visible.get(page * pageSize + id);
        if (editing.endsWith("Columns")) {
          j.a(new ColumnScreen(this, editing));
          return;
        }
        secret = null;
        edit.a(LeanSettings.get(editing));
        edit.b(true);
      } else if (id == 20) {
        page = Math.max(0, page - 1);
      } else if (id == 21) {
        if ((page + 1) * pageSize < visible.size()) page++;
      } else if (id == 22) {
        secret = new StringBuilder();
        editing = "";
        notice = "Keys stay in glide/pika-api-key.txt";
      } else if (id == 23) {
        if (secret != null) {
          notice = "Saving key privately...";
          if (!NativeHooks.saveKey(
              secret.toString(),
              saved -> notice = saved ? "Key saved privately" : "Key was not saved"))
            notice = "Key save queue busy; try again";
          for (int i = 0; i < secret.length(); i++) secret.setCharAt(i, '\0');
          secret = null;
        } else if (!editing.isEmpty()) {
          LeanSettings.set(editing, edit.b().trim());
          FrameProbe.enabled = LeanSettings.on("profiler");
          editing = "";
          notice = "Saved; table changes apply on next rebuild";
        }
      } else if (id == 24) {
        j.a((axu) null);
      } else if (id == 25) {
        LeanSettings.exportSettings();
        FrameProbe.export();
        notice = "Exported non-sensitive settings and measurements";
      } else if (id == 26) {
        LeanSettings.importSettings();
        notice = "Imported Lean settings";
      } else if (id == 27) {
        placing = true;
        TabPlacement.begin(l, m);
      } else if (id == 28) {
        String[] presets = {"MAXIMUM_FPS", "BALANCED", "VISUAL"};
        String chosen = presets[preset++ % presets.length];
        LeanSettings.preset(chosen);
        notice = "Applied " + chosen;
      } else if (id == 29) {
        j.a(new StatusScreen(this));
        return;
      } else if (id == 30) {
        LeanSettings.saveProfile();
        notice = "Saved profile " + LeanSettings.get("profileName");
      } else if (id == 31) {
        LeanSettings.loadProfile();
        FrameProbe.enabled = LeanSettings.on("profiler");
        notice = "Loaded profile " + LeanSettings.get("profileName");
      }
      refresh();
    } catch (IOException e) {
      notice = "Invalid value or settings could not be saved";
    }
  }

  protected void a(int x, int y, int button) {
    if (placing) {
      if (button == 0) TabPlacement.down(x, y);
      return;
    }
    search.a(x, y, button);
    if (secret == null) edit.a(x, y, button);
    super.a(x, y, button);
  }

  protected void a(int x, int y, int button, long elapsed) {
    if (placing) {
      TabPlacement.move(x, y);
      return;
    }
    super.a(x, y, button, elapsed);
  }

  protected void b(int x, int y, int button) {
    if (placing) {
      TabPlacement.up();
      return;
    }
    super.b(x, y, button);
  }

  public void m() {
    TabPlacement.cancel();
    if (secret != null) {
      for (int i = 0; i < secret.length(); i++) secret.setCharAt(i, '\0');
      secret = null;
    }
    super.m();
  }

  protected void a(char c, int code) {
    if (placing) {
      if (code == 1) {
        TabPlacement.cancel();
        placing = false;
      } else if (code == 28) {
        try {
          TabPlacement.save();
          placing = false;
          notice = "TAB position saved";
        } catch (IOException e) {
          notice = "Position save failed";
        }
      }
      return;
    }
    if (code == 1) {
      secret = null;
      j.a((axu) null);
      return;
    }
    if (secret != null) {
      if (code == 14 && secret.length() > 0) secret.deleteCharAt(secret.length() - 1);
      else if (code == 47 && axu.q()) {
        String clip = axu.o();
        if (clip != null && KeyVault.valid(clip.trim())) {
          secret.setLength(0);
          secret.append(clip.trim());
        }
      } else if (c > 32 && c < 127 && secret.length() < 512) secret.append(c);
      return;
    }
    if (!editing.isEmpty() && edit.j()) {
      edit.a(c, code);
      return;
    }
    if (search.a(c, code)) {
      page = 0;
      refresh();
    }
  }

  public boolean d() {
    return false;
  }
}
