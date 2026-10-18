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
  private int page;
  private boolean placing;
  private final List<String> visible = new ArrayList<>();

  public void b() {
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
    int count = Math.min(6, visible.size() - page * 6);
    if (count < 0) {
      page = 0;
      count = Math.min(6, visible.size());
    }
    for (int i = 0; i < count; i++) {
      String k = visible.get(page * 6 + i);
      n.add(new avs(i, l / 2 - 150, 62 + i * 23, 300, 20, k + ": " + LeanSettings.get(k)));
    }
    n.add(new avs(20, l / 2 - 150, m - 48, 68, 20, "Previous"));
    n.add(new avs(21, l / 2 - 80, m - 48, 55, 20, "Next"));
    n.add(new avs(22, l / 2 - 23, m - 48, 70, 20, "API key"));
    n.add(new avs(23, l / 2 + 49, m - 48, 48, 20, "Apply"));
    n.add(new avs(24, l / 2 + 99, m - 48, 51, 20, "Done"));
    n.add(new avs(25, 8, 8, 70, 20, "Export"));
    n.add(new avs(26, 80, 8, 70, 20, "Import"));
    n.add(new avs(27, l - 108, 8, 100, 20, "Place TAB"));
  }

  public void a(int x, int y, float partial) {
    c();
    a(q, "VelvetG Lean · performance and integration", l / 2, 17, 0xffffff);
    search.g();
    if (secret != null) {
      a(q, "Enter API key (hidden); Apply saves locally", l / 2, m - 91, 0xffcc77);
      a(q, repeat('*', Math.min(48, secret.length())), l / 2, m - 69, 0xffffff);
    } else if (!editing.isEmpty()) {
      a(q, "Edit " + editing, l / 2, m - 91, 0xffcc77);
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
      if (id >= 0 && id < 6 && page * 6 + id < visible.size()) {
        editing = visible.get(page * 6 + id);
        secret = null;
        edit.a(LeanSettings.get(editing));
        edit.b(true);
      } else if (id == 20) {
        page = Math.max(0, page - 1);
      } else if (id == 21) {
        if ((page + 1) * 6 < visible.size()) page++;
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
        notice = "Click the screen to set TAB top-left";
      }
      refresh();
    } catch (IOException e) {
      notice = "Could not save settings";
    }
  }

  protected void a(int x, int y, int button) {
    if (placing) {
      try {
        LeanSettings.set("tabX", Integer.toString(x));
        LeanSettings.set("tabY", Integer.toString(y));
        notice = "TAB position saved";
      } catch (IOException e) {
        notice = "Position save failed";
      }
      placing = false;
      return;
    }
    search.a(x, y, button);
    if (secret == null) edit.a(x, y, button);
    super.a(x, y, button);
  }

  protected void a(char c, int code) {
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
