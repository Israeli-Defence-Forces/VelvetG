package me.movi.violet;

import java.awt.Color;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.KeybindSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import org.lwjgl.input.Keyboard;
import vgstub.axu;

/** One compact, top-aligned settings surface with shared spacing and visible context. */
public final class VioletScreen extends axu {
    private static final String[] PAGES = {"Clicking", "Timing", "Filters", "Appearance"};
    private static final String[] SUB = {"Hands, activation and shortcuts.", "Rates and cadence.", "Choose when Violet pauses.", "HUD and screen appearance."};
    private static final String[] PRESETS = {"Balanced", "Fast", "Build"};
    private static final float W = 560f, H = 360f;

    private float scale = 1, x, y;
    private int page;
    private long lastReport;
    private VioletSettings.Row capture, drag, popup, numberEdit;
    private String editText = "", validation = "", help = "";
    private boolean selectAll, confirmReset, inspectHud, presetMenu;
    private G popupAnchor;

    private static final class G {
        final float x, y, w, cx, cy, cw, track, trackW, valueX;
        G(float x, float y, float w, float cx, float cy, float cw, float track, float trackW, float valueX) {
            this.x = x; this.y = y; this.w = w; this.cx = cx; this.cy = cy; this.cw = cw; this.track = track; this.trackW = trackW; this.valueX = valueX;
        }
    }

    public VioletScreen() { this(0); }
    public VioletScreen(int initialPage) { page = Math.max(0, Math.min(3, initialPage)); }

    @Override public void b() { VioletRuntime.reset(); }

    public void layout() {
        scale = Math.max(.1f, Math.min(Math.min(120, (float) VioletRuntime.settings.number("guiScale")) * .72f / 100f, Math.min((l - 48) / W, (m - 48) / H)));
        x = (l - W * scale) / 2;
        y = (m - H * scale) / 2;
    }

    @Override public void a(int mx, int my, float partial) {
        layout();
        float u = (mx - x) / scale, v = (my - y) / scale;
        if (drag != null) slider(drag, u, geometry(indexOf(drag)));
        NanoVGManager n = Glide.getInstance().getNanoVGManager();
        n.setupAndDraw(() -> {
            n.drawRect(0, 0, l, m, VioletStyle.alpha(VioletStyle.PANEL, 160));
            n.save(); n.translate(x, y); n.scale(0, 0, scale); draw(n, u, v); n.restore();
        });
    }

    private G geometry(int i) {
        if (page == 3) {
            float a = 130 + (i % 2) * 207, b = 158 + (i / 2) * 42;
            return new G(a, b, 200, a + 6, b + 14, 188, a + 6, 100, a + 150);
        }
        float b = 98 + i * VioletStyle.ROW_H;
        return new G(130, b, 414, 390, b + 2, 148, 390, 100, 498);
    }

    private int indexOf(VioletSettings.Row row) {
        int i = 0;
        for (VioletSettings.Row r : VioletRuntime.settings.rows) if (r.page == page) { if (r == row) return i; i++; }
        return -1;
    }

    private int hudWidth() { return VioletHud.cardWidth(); }
    private int hudHeight() { return VioletHud.cardHeight(); }

    public void draw(NanoVGManager n, float u, float v) {
        VioletSettings s = VioletRuntime.settings;
        Color accent = VioletStyle.accent();
        help = "";

        n.drawRoundedRect(0, 0, W, H, VioletStyle.R_WINDOW, VioletStyle.PANEL);
        n.drawOutlineRoundedRect(.5f, .5f, W - 1, H - 1, VioletStyle.R_WINDOW, .7f, VioletStyle.alpha(accent, 55));
        n.drawText("Violet", 18, 16, accent, VioletStyle.T_TITLE, Fonts.SEMIBOLD);
        button(n, 408, 14, 94, 24, VioletRuntime.module.isToggled() ? "Violet: On" : "Violet: Off", VioletRuntime.module.isToggled(), true, u, v);
        button(n, 514, 14, 28, 24, "×", false, true, u, v);
        n.drawRect(16, 49, 528, .7f, VioletStyle.LINE);

        n.drawRoundedRect(12, 57, 104, 291, VioletStyle.R_CARD, VioletStyle.CARD);
        for (int i = 0; i < PAGES.length; i++) {
            boolean selected = page == i;
            n.drawRoundedRect(18, 66 + i * 33, 92, 27, VioletStyle.R_CONTROL, selected ? VioletStyle.alpha(accent, 26) : VioletStyle.CARD);
            if (selected) n.drawRoundedRect(18, 73 + i * 33, 2, 12, 1, accent);
            n.drawText(PAGES[i], 28, 74 + i * 33, selected ? accent : VioletStyle.MUTED, VioletStyle.T_BODY, selected ? Fonts.MEDIUM : Fonts.REGULAR);
        }
        button(n, 20, 254, 88, 22, "Diagnostics", false, s.flag("diagnostics"), u, v);
        button(n, 20, 288, 88, 22, s.flag("menus") ? "Menus: Hidden" : "Menus: Shown", s.flag("menus"), true, u, v);
        button(n, 20, 320, 88, 22, VioletRuntime.hud.isToggled() ? "HUD: On" : "HUD: Off", VioletRuntime.hud.isToggled(), true, u, v);

        n.drawText(PAGES[page], 132, 59, VioletStyle.TEXT, VioletStyle.T_SECTION, Fonts.SEMIBOLD);
        n.drawText(SUB[page], 132, 78, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);
        if (page == 0) {
            button(n, 408, 57, 130, 24, "Profile: " + s.presetName(), true, true, u, v);
            n.drawArrow(526, 67, 5, 3, accent);
        }

        if (page == 3) {
            n.drawRoundedRect(130, 96, 414, 50, VioletStyle.R_CARD, VioletStyle.CARD);
            int w = hudWidth(), h = hudHeight(); float z = Math.min(.82f, 44f / h);
            n.save(); n.translate(144, 96 + (50 - h * z) / 2); n.scale(0, 0, z); VioletRuntime.hud.drawCard(n, 0, 0, w, h, true); n.restore();
            button(n, 430, 109, 100, 24, "1:1 preview", false, true, u, v);
        }

        int i = 0;
        for (VioletSettings.Row r : s.rows) if (r.page == page) {
            G g = geometry(i++);
            boolean on = s.available(r), hover = inside(u, v, g.x, g.y, g.w, page == 3 ? 38 : 26);
            if (hover) help = on ? r.help : s.unavailableReason(r);
            if (hover && on) n.drawRoundedRect(g.x, g.y, g.w, page == 3 ? 38 : 26, VioletStyle.R_CONTROL, VioletStyle.CARD);
            Color text = on ? VioletStyle.TEXT : VioletStyle.alpha(VioletStyle.MUTED, 190);
            Color value = on ? accent : VioletStyle.alpha(VioletStyle.MUTED, 150);
            float labelY = page == 3 && !(r.setting instanceof BooleanSetting) ? g.y + 2 : g.y + (page == 3 ? 14 : 8);
            n.drawText(r.name, g.x + 7, labelY, text, VioletStyle.T_BODY, Fonts.MEDIUM);

            if (!on) {
                String why = disabledLabel(r);
                n.drawText(why, g.x + g.w - 8 - n.getTextWidth(why, VioletStyle.T_SMALL, Fonts.REGULAR), g.y + (page == 3 ? 14 : 8), VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);
                continue;
            }

            if (r.setting instanceof BooleanSetting) {
                float bx = g.x + g.w - 39, by = page == 3 ? g.y + 11 : g.y + 6;
                boolean enabled = ((BooleanSetting) r.setting).isToggled();
                n.drawRoundedRect(bx, by, 29, 15, VioletStyle.R_PILL, enabled ? accent : VioletStyle.LINE);
                n.drawCircle(bx + (enabled ? 21.5f : 7.5f), by + 7.5f, 4.5f, enabled ? VioletStyle.PANEL : VioletStyle.MUTED);
            } else if (r.setting instanceof NumberSetting) {
                NumberSetting range = (NumberSetting) r.setting;
                double v0 = s.number(r.id);
                float extent = (float) ((v0 - range.getMinValue()) / (range.getMaxValue() - range.getMinValue()) * g.trackW);
                float yy = page == 3 ? g.y + 28 : g.y + 12;
                n.drawRoundedRect(g.track, yy, g.trackW, 4, 2, VioletStyle.LINE);
                n.drawRoundedRect(g.track, yy, extent, 4, 2, value);
                n.drawCircle(g.track + extent, yy + 2, 3.8f, value);
                boolean editing = numberEdit == r;
                n.save(); n.scissor(g.valueX, g.cy, 40, 22);
                button(n, g.valueX, g.cy, 40, 22, editing ? editText + (System.currentTimeMillis() % 1000 < 500 ? "|" : "") : Integer.toString((int) Math.round(v0)), editing, true, u, v);
                n.restore();
            } else if (r.setting instanceof KeybindSetting) {
                button(n, g.cx, g.cy, g.cw, 22, capture == r ? "Press key / click" : VioletRuntime.keyName(s.key(r.id)), capture == r, true, u, v);
            } else {
                VioletSettings.Pick pick = (VioletSettings.Pick) r.setting;
                button(n, g.cx, g.cy, g.cw, 22, pick.getOption().getName(), popup == r, true, u, v);
                n.drawArrow(g.cx + g.cw - 12, g.cy + 9, 5, 3, value);
            }
        }

        if (capture != null) help = "Press a key or mouse button. Escape clears the bind.";
        if (numberEdit != null) {
            NumberSetting range = (NumberSetting) numberEdit.setting;
            help = validation.isEmpty() ? "Allowed range: " + (int) range.getMinValue() + "–" + (int) range.getMaxValue() + ". Enter saves; Escape cancels." : validation;
        }
        n.drawRect(132, 306, 408, .7f, VioletStyle.LINE);
        String footer = !validation.isEmpty() ? validation : help.isEmpty() ? SUB[page] : help;
        n.drawText(n.getLimitText(footer, VioletStyle.T_SMALL, Fonts.REGULAR, 248), 132, 315, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);
        if (page == 3) quiet(n, 132, 335, "Edit HUD", u, v, 76);
        quiet(n, 394, 335, "Reset all", u, v, 64);
        button(n, 468, 332, 72, 24, "Done", true, true, u, v);

        if (popup != null) {
            float top = popupTop(); int count = ((ComboSetting) popup.setting).getOptions().size();
            n.drawRoundedRect(popupAnchor.cx - 1, top - 2, popupAnchor.cw + 2, count * 24 + 4, VioletStyle.R_CARD, VioletStyle.LINE);
            for (int option = 0; option < count; option++) {
                String label = ((ComboSetting) popup.setting).getOptions().get(option).getName();
                button(n, popupAnchor.cx, top + option * 24, popupAnchor.cw, 23, label, ((VioletSettings.Pick) popup.setting).index() == option, true, u, v);
            }
        }
        if (presetMenu) {
            n.drawRoundedRect(407, 83, 132, 76, VioletStyle.R_CARD, VioletStyle.LINE);
            for (int j = 0; j < 3; j++) button(n, 408, 84 + j * 24, 130, 23, PRESETS[j], s.presetName().equals(PRESETS[j]), true, u, v);
        }
        if (inspectHud) drawHudPreview(n, u, v);
        if (confirmReset) drawReset(n, u, v);
    }

    private String disabledLabel(VioletSettings.Row r) {
        if (r.id.equals("variation")) return "Variation off";
        if (r.id.equals("graph")) return "Detailed only";
        if (r.id.equals("activation")) return "Hold-click mode";
        if (r.id.startsWith("right") || r.id.equals("blocks")) return "Right off";
        return "Left off";
    }

    private void drawHudPreview(NanoVGManager n, float u, float v) {
        n.drawRoundedRect(1, 1, 558, 358, VioletStyle.R_WINDOW, new Color(10, 8, 16, 220));
        int w = hudWidth(), h = hudHeight();
        float wanted = VioletRuntime.hud.getScale() / scale, zoom = Math.min(wanted, Math.min(500f / w, 210f / h));
        String label = zoom == wanted ? "HUD preview · 1:1" : "HUD preview · " + Math.round(zoom / wanted * 100) + "% to fit";
        n.drawText(label, 30, 67, VioletStyle.TEXT, VioletStyle.T_SECTION, Fonts.SEMIBOLD);
        n.drawText("Preview uses sample input counts.", 30, 89, VioletStyle.MUTED, VioletStyle.T_BODY, Fonts.REGULAR);
        button(n, 469, 64, 57, 25, "Close", true, true, u, v);
        n.save(); n.translate((560 - w * zoom) / 2, (360 - h * zoom) / 2); n.scale(0, 0, zoom); VioletRuntime.hud.drawCard(n, 0, 0, w, h, true); n.restore();
    }

    private void drawReset(NanoVGManager n, float u, float v) {
        n.drawRoundedRect(1, 1, 558, 358, VioletStyle.R_WINDOW, new Color(10, 8, 16, 205));
        n.drawRoundedRect(140, 120, 310, 119, VioletStyle.R_WINDOW, VioletStyle.CARD);
        n.drawText("Reset all Violet settings?", 159, 136, VioletStyle.TEXT, VioletStyle.T_SECTION, Fonts.SEMIBOLD);
        n.drawText("Restores every Violet tab and disables clicking.", 159, 161, VioletStyle.MUTED, VioletStyle.T_BODY, Fonts.REGULAR);
        button(n, 159, 199, 120, 25, "Cancel", false, true, u, v);
        button(n, 291, 199, 139, 25, "Reset all", true, true, u, v);
    }

    private void quiet(NanoVGManager n, float x, float y, String text, float u, float v, float w) {
        VioletControls.quiet(n, x, y - 3, w, text, u, v);
    }

    private void button(NanoVGManager n, float x, float y, float w, float h, String text, boolean selected, boolean available, float u, float v) {
        VioletControls.button(n, x, y, w, h, text, selected, available, u, v);
    }

    static boolean inside(float x, float y, float a, float b, float w, float h) { return VioletControls.hit(x, y, a, b, w, h); }
    private float popupTop() { return Math.min(popupAnchor.cy + 24, 302 - ((ComboSetting) popup.setting).getOptions().size() * 24); }

    @Override protected void a(int mx, int my, int button) {
        layout(); float u = (mx - x) / scale, v = (my - y) / scale; VioletSettings s = VioletRuntime.settings;
        if (inspectHud) { if (button == 0 && inside(u, v, 469, 64, 57, 25)) inspectHud = false; return; }
        if (confirmReset) {
            if (button == 0 && inside(u, v, 159, 199, 120, 25)) confirmReset = false;
            else if (button == 0 && inside(u, v, 291, 199, 139, 25)) { VioletRuntime.module.setToggled(false); s.reset(); VioletRuntime.save(); confirmReset = false; }
            return;
        }
        if (capture != null) { ((KeybindSetting) capture.setting).setKeyCode(button - 100); capture = null; VioletRuntime.reset(); VioletRuntime.save(); return; }
        if (button != 0) return;
        if (presetMenu) {
            if (inside(u, v, 408, 84, 130, 72)) { int j = (int) ((v - 84) / 24); if (j >= 0 && j < 3) { s.preset(j); VioletRuntime.save(); } }
            presetMenu = false; return;
        }
        if (popup != null) {
            int option = (int) ((v - popupTop()) / 24);
            if (v >= popupTop() && inside(u, v, popupAnchor.cx, popupTop(), popupAnchor.cw, ((ComboSetting) popup.setting).getOptions().size() * 24)) {
                ComboSetting p = (ComboSetting) popup.setting;
                if (option >= 0 && option < p.getOptions().size()) { p.setOption(p.getOptions().get(option)); VioletRuntime.reset(); VioletRuntime.save(); }
            }
            popup = null; return;
        }
        if (numberEdit != null && !commitNumber()) return;
        if (inside(u, v, 514, 14, 28, 24) || inside(u, v, 468, 332, 72, 24)) { close(); return; }
        if (inside(u, v, 408, 14, 94, 24)) { VioletRuntime.module.toggle(); VioletRuntime.save(); return; }
        if (page == 0 && inside(u, v, 408, 57, 130, 24)) { presetMenu = true; return; }
        if (inside(u, v, 20, 254, 88, 22) && s.flag("diagnostics")) {
            long now = System.nanoTime();
            if (now - lastReport >= 1000000000L) { me.eldodebug.soar.logger.GlideLogger.info(VioletDiagnostics.report()); lastReport = now; }
            return;
        }
        if (inside(u, v, 20, 288, 88, 22)) { BooleanSetting flag = (BooleanSetting) s.row("menus").setting; flag.setToggled(!flag.isToggled()); VioletRuntime.save(); return; }
        if (inside(u, v, 20, 320, 88, 22)) { VioletRuntime.hud.setToggled(!VioletRuntime.hud.isToggled()); VioletRuntime.save(); return; }
        for (int i = 0; i < 4; i++) if (inside(u, v, 18, 66 + i * 33, 92, 27)) { page = i; drag = null; return; }
        if (page == 3 && inside(u, v, 132, 332, 76, 22)) { ave().a(new GuiEditHUD(true)); return; }
        if (inside(u, v, 394, 332, 64, 22)) { confirmReset = true; return; }
        if (page == 3 && inside(u, v, 430, 109, 100, 24)) { inspectHud = true; return; }

        int index = 0;
        for (VioletSettings.Row r : s.rows) if (r.page == page) {
            G g = geometry(index++);
            if (!s.available(r)) continue;
            if (r.setting instanceof BooleanSetting) {
                if (inside(u, v, g.x, g.y, g.w, page == 3 ? 38 : 26)) { BooleanSetting flag = (BooleanSetting) r.setting; flag.setToggled(!flag.isToggled()); VioletRuntime.reset(); VioletRuntime.save(); return; }
            } else if (r.setting instanceof NumberSetting) {
                if (inside(u, v, g.valueX, g.cy, 40, 22)) { numberEdit = r; editText = Integer.toString((int) s.number(r.id)); selectAll = true; validation = ""; return; }
                if (inside(u, v, g.track - 5, g.y + 8, g.trackW + 10, 24)) { drag = r; slider(r, u, g); return; }
            } else if (inside(u, v, g.cx, g.cy, g.cw, 22)) {
                if (r.setting instanceof KeybindSetting) capture = r; else { popup = r; popupAnchor = g; }
                return;
            }
        }
    }

    private void slider(VioletSettings.Row row, float u, G g) {
        NumberSetting range = (NumberSetting) row.setting;
        double value = range.getMinValue() + VioletEngine.clamp((u - g.track) / g.trackW, 0, 1) * (range.getMaxValue() - range.getMinValue());
        VioletRuntime.settings.value(row.id, Math.round(value));
    }

    private boolean commitNumber() {
        try {
            double value = Double.parseDouble(editText);
            if (!Double.isFinite(value)) throw new NumberFormatException();
            VioletRuntime.settings.value(numberEdit.id, Math.round(value));
            VioletRuntime.save();
            numberEdit = null; validation = ""; return true;
        } catch (NumberFormatException e) {
            validation = "Enter a valid number, or press Escape to cancel.";
            return false;
        }
    }

    @Override protected void b(int x, int y, int button) { if (drag != null) VioletRuntime.save(); drag = null; }

    @Override protected void a(char character, int key) {
        if (inspectHud) { if (key == 1) inspectHud = false; return; }
        if (confirmReset) { if (key == 1) confirmReset = false; return; }
        if (presetMenu) { if (key == 1) presetMenu = false; return; }
        if (popup != null) { if (key == 1) popup = null; return; }
        if (numberEdit != null) {
            if (key == 1) { numberEdit = null; validation = ""; }
            else if (key == 28 || key == 156) commitNumber();
            else if (key == 14) { editText = selectAll ? "" : editText.isEmpty() ? "" : editText.substring(0, editText.length() - 1); selectAll = false; }
            else if (key == 30 && controlDown()) selectAll = true;
            else if (key == 47 && controlDown()) { String text = axu.o(); if (text != null && text.matches("[-+]?[0-9]*\\.?[0-9]*") && text.length() <= 12) { editText = text; selectAll = false; } }
            else if ((Character.isDigit(character) || character == '.' || character == '-') && editText.length() < 12) { editText = (selectAll ? "" : editText) + character; selectAll = false; }
            return;
        }
        if (capture != null) { ((KeybindSetting) capture.setting).setKeyCode(key == 1 ? 0 : key); capture = null; VioletRuntime.reset(); VioletRuntime.save(); }
        else if (key == 1) close();
    }

    private boolean controlDown() { return Keyboard.isCreated() && (Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157)); }
    private void close() { ave().a((axu) null); }
    private vgstub.ave ave() { return vgstub.ave.A(); }
    @Override public void m() { drag = null; capture = null; popup = null; numberEdit = null; presetMenu = false; VioletRuntime.reset(); VioletRuntime.save(); }
    @Override public boolean d() { return false; }
}
