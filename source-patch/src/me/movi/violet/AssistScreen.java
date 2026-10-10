package me.movi.violet;

import java.awt.Color;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.TextSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import vgstub.axu;
import vgstub.ave;

/** Shared compact assist settings using the same spacing, typography and control geometry as Violet. */
public final class AssistScreen extends axu {
    private final Mod module;
    private final Setting[] allRows;
    private Setting[] rows;
    private int page;
    private float x, y, scale;
    private NumberSetting drag, edit;
    private ComboSetting popup;
    private TextSetting textEdit;
    private float popupY;
    private String text = "", error = "", help = "";
    private boolean replace, confirm;

    public AssistScreen(AssistModule module) {
        this(module, module.kind == 0 ? new Setting[]{AssistRuntime.restoreTool, AssistRuntime.toolFilterMode, AssistRuntime.toolBlocks}
            : module.kind == 1 ? new Setting[]{AssistRuntime.containers, AssistRuntime.jump, AssistRuntime.sneak}
            : new Setting[]{AssistRuntime.health, AssistRuntime.cooldown, AssistRuntime.soupMode, AssistRuntime.creativeSoup, AssistRuntime.refillSoup});
    }

    public AssistScreen(Mod module, Setting[] settings) { this.module = module; allRows = settings; selectPage(0); }
    private void selectPage(int value) { page = Math.max(0, Math.min((allRows.length-1)/8, value)); rows = java.util.Arrays.copyOfRange(allRows, page*8, Math.min(allRows.length, page*8+8)); drag = edit = null; textEdit = null; popup = null; }

    private int height() { return 112 + rows.length * 40; }

    public void layout() {
        scale = Math.max(.1f, Math.min(Math.min(120, (float) VioletRuntime.settings.number("guiScale")) * .72f / 100f, Math.min((l - 40) / 420f, (m - 40) / (float) height())));
        x = (l - 420 * scale) / 2;
        y = (m - height() * scale) / 2;
    }

    @Override public void a(int mx, int my, float partial) {
        layout();
        float u = (mx - x) / scale, v = (my - y) / scale;
        if (drag != null) value(drag, u);
        NanoVGManager n = Glide.getInstance().getNanoVGManager();
        n.setupAndDraw(() -> {
            n.drawRect(0, 0, l, m, VioletStyle.alpha(VioletStyle.PANEL, 160));
            n.save(); n.translate(x, y); n.scale(0, 0, scale); draw(n, u, v); n.restore();
        });
    }

    public void draw(NanoVGManager n, float u, float v) {
        Color accent = VioletStyle.accent(); int h = height(); help = "";
        n.drawRoundedRect(0, 0, 420, h, VioletStyle.R_WINDOW, VioletStyle.PANEL);
        n.drawOutlineRoundedRect(.5f, .5f, 419, h - 1, VioletStyle.R_WINDOW, .7f, VioletStyle.alpha(accent, 55));
        n.drawText(module.getName(), 20, 15, accent, VioletStyle.T_TITLE, Fonts.SEMIBOLD);
        n.drawText(module.getDescription(), 20, 39, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);
        button(n, 286, 14, 80, 24, module.isToggled() ? "On" : "Off", module.isToggled(), u, v);
        button(n, 376, 14, 24, 24, "×", false, u, v);
        n.drawRect(20, 59, 380, .7f, VioletStyle.LINE);

        for (int i = 0; i < rows.length; i++) {
            Setting row = rows[i]; float yy = 70 + i * 40;
            boolean hover = hit(u, v, 20, yy, 380, 34);
            if (hover) { n.drawRoundedRect(20, yy, 380, 34, VioletStyle.R_CONTROL, VioletStyle.CARD); help = hint(row); }
            n.drawText(row.getName(), 26, yy + 12, VioletStyle.TEXT, VioletStyle.T_BODY, Fonts.MEDIUM);
            if (row instanceof BooleanSetting) {
                boolean on = ((BooleanSetting) row).isToggled();
                n.drawRoundedRect(363, yy + 10, 29, 15, VioletStyle.R_PILL, on ? accent : VioletStyle.LINE);
                n.drawCircle(363 + (on ? 21.5f : 7.5f), yy + 17.5f, 4.5f, on ? VioletStyle.PANEL : VioletStyle.MUTED);
            } else if (row instanceof NumberSetting) {
                NumberSetting range = (NumberSetting) row;
                float amount = (float) ((range.getValue() - range.getMinValue()) / (range.getMaxValue() - range.getMinValue()) * 100);
                n.drawRoundedRect(236, yy + 16, 100, 4, 2, VioletStyle.LINE);
                n.drawRoundedRect(236, yy + 16, amount, 4, 2, accent);
                n.drawCircle(236 + amount, yy + 18, 3.8f, accent);
                n.save(); n.scissor(350, yy + 6, 42, 24); button(n, 350, yy + 6, 42, 24, edit == row ? text : Integer.toString((int) Math.round(range.getValue())), edit == row, u, v); n.restore();
            } else if (row instanceof TextSetting) {
                String shown = textEdit == row ? text : ((TextSetting)row).getText();
                button(n, 230, yy + 6, 162, 24, n.getLimitText(shown, VioletStyle.T_SMALL, Fonts.REGULAR, 146), textEdit == row, u, v);
            } else {
                ComboSetting pick = (ComboSetting) row;
                button(n, 230, yy + 6, 162, 24, pick.getOption().getName(), popup == row, u, v);
                n.drawArrow(380, yy + 16, 5, 3, accent);
            }
        }

        n.drawRect(20, h - 40, 380, .7f, VioletStyle.LINE);
        String footer = !error.isEmpty() ? error : help;
        if (!footer.isEmpty()) n.drawText(n.getLimitText(footer, VioletStyle.T_SMALL, Fonts.REGULAR, 220), 20, h - 29, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);
        if (allRows.length > 8) { button(n, 20, h - 32, 28, 24, "‹", false, u, v); button(n, 54, h - 32, 28, 24, "›", false, u, v); n.drawText((page+1) + "/" + ((allRows.length+7)/8), 90, h-25, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR); }
        quiet(n, 238, h - 31, 78, "Reset module", u, v);
        button(n, 330, h - 32, 70, 24, "Done", true, u, v);

        if (popup != null) {
            n.drawRoundedRect(229, popupY - 2, 164, popup.getOptions().size() * 25 + 4, VioletStyle.R_CARD, VioletStyle.LINE);
            for (int i = 0; i < popup.getOptions().size(); i++) button(n, 230, popupY + i * 25, 162, 24, popup.getOptions().get(i).getName(), popup.getOption() == popup.getOptions().get(i), u, v);
        }
        if (confirm) {
            n.drawRoundedRect(1, 1, 418, h - 2, VioletStyle.R_WINDOW, new Color(10, 8, 16, 220));
            n.drawCenteredText("Reset " + module.getName() + "?", 210, h / 2 - 31, VioletStyle.TEXT, VioletStyle.T_SECTION, Fonts.SEMIBOLD);
            n.drawCenteredText("Defaults are restored and this module is disabled.", 210, h / 2 - 10, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);
            button(n, 100, h / 2 + 13, 100, 24, "Cancel", false, u, v);
            button(n, 220, h / 2 + 13, 100, 24, "Reset", true, u, v);
        }
    }

    private String hint(Setting row) {
        String name = row.getName();
        if ("Restore slot".equals(name)) return "Return to the previous hotbar slot after ownership ends.";
        if ("Other containers".equals(name)) return "Allow movement in non-player containers; text-entry screens remain excluded.";
        if ("Allow jumping".equals(name)) return "Allow the normal jump binding while a supported container is open.";
        if ("Allow sneaking".equals(name)) return "Allow the normal sneak binding while a supported container is open.";
        if ("Health threshold (HP)".equals(name)) return "Threshold is displayed in HP; 2 HP equals one heart.";
        if ("Use cooldown (ms)".equals(name)) return "Minimum time between automatic use attempts.";
        if ("Soup mode".equals(name)) return "Choose server-style instant use or normal vanilla eating.";
        if ("Allow in creative".equals(name)) return "Permit the module while the player is in creative mode.";
        return module.getDescription();
    }

    private void quiet(NanoVGManager n, float a, float b, float w, String label, float u, float v) {
        VioletControls.quiet(n, a, b, w, label, u, v);
    }

    private void button(NanoVGManager n, float a, float b, float w, float h, String label, boolean selected, float u, float v) {
        VioletControls.button(n, a, b, w, h, label, selected, true, u, v);
    }

    private static boolean hit(float u, float v, float x, float y, float w, float h) { return VioletControls.hit(u, v, x, y, w, h); }

    @Override protected void a(int mx, int my, int button) {
        if (button != 0) return;
        layout(); float u = (mx - x) / scale, v = (my - y) / scale; int h = height();
        if (confirm) {
            if (hit(u, v, 100, h / 2 + 13, 100, 24)) confirm = false;
            else if (hit(u, v, 220, h / 2 + 13, 100, 24)) { module.setToggled(false); for (Setting row : allRows) row.reset(); VioletRuntime.save(); confirm = false; }
            return;
        }
        if (popup != null) {
            if (hit(u, v, 230, popupY, 162, popup.getOptions().size() * 25)) { int i = (int) ((v - popupY) / 25); popup.setOption(popup.getOptions().get(i)); VioletRuntime.save(); }
            popup = null; return;
        }
        if (textEdit != null) { textEdit.setText(text); textEdit = null; VioletRuntime.save(); }
        if (edit != null && !commit()) return;
        if (hit(u, v, 376, 14, 24, 24) || hit(u, v, 330, h - 32, 70, 24)) { ave.A().a((axu) null); return; }
        if (hit(u, v, 286, 14, 80, 24)) { module.toggle(); VioletRuntime.save(); return; }
        if (allRows.length > 8 && hit(u, v, 20, h-32, 28, 24)) { selectPage(page-1); return; }
        if (allRows.length > 8 && hit(u, v, 54, h-32, 28, 24)) { selectPage(page+1); return; }
        if (hit(u, v, 238, h - 31, 78, 22)) { confirm = true; return; }

        for (int i = 0; i < rows.length; i++) {
            float yy = 70 + i * 40; Setting row = rows[i];
            if (row instanceof BooleanSetting && hit(u, v, 20, yy, 380, 34)) { BooleanSetting flag = (BooleanSetting) row; flag.setToggled(!flag.isToggled()); VioletRuntime.save(); return; }
            if (row instanceof NumberSetting) {
                if (hit(u, v, 350, yy + 6, 42, 24)) { edit = (NumberSetting) row; text = Integer.toString((int) edit.getValue()); replace = true; error = ""; return; }
                if (hit(u, v, 230, yy + 6, 112, 24)) { drag = (NumberSetting) row; value(drag, u); return; }
            } else if (row instanceof TextSetting && hit(u, v, 230, yy + 6, 162, 24)) { textEdit = (TextSetting)row; text = textEdit.getText(); replace = true; return;
            } else if (row instanceof ComboSetting && hit(u, v, 230, yy + 6, 162, 24)) { popup = (ComboSetting) row; popupY = Math.min(yy + 31, h - 43 - popup.getOptions().size() * 25); return; }
        }
    }

    private void value(NumberSetting row, float u) { row.setValue(Math.round(row.getMinValue() + VioletEngine.clamp((u - 236) / 100, 0, 1) * (row.getMaxValue() - row.getMinValue()))); }

    private boolean commit() {
        try { double v = Double.parseDouble(text); if (!Double.isFinite(v)) throw new NumberFormatException(); edit.setValue(Math.round(v)); VioletRuntime.save(); edit = null; error = ""; return true; }
        catch (NumberFormatException e) { error = "Enter a valid number"; return false; }
    }

    @Override protected void b(int mx, int my, int button) { if (drag != null) VioletRuntime.save(); drag = null; }
    @Override protected void a(char c, int key) {
        if (confirm) { if (key == 1) confirm = false; return; }
        if (popup != null) { if (key == 1) popup = null; return; }
        if (textEdit != null) {
            if (key == 1) textEdit = null;
            else if (key == 28 || key == 156) { textEdit.setText(text); textEdit = null; VioletRuntime.save(); }
            else if (key == 14) { text = replace ? "" : text.isEmpty() ? "" : text.substring(0, text.length()-1); replace = false; }
            else if (c >= 32 && c < 127 && text.length() < 512) { text = (replace ? "" : text) + c; replace = false; }
            return;
        }
        if (edit != null) {
            if (key == 1) { edit = null; error = ""; }
            else if (key == 28 || key == 156) commit();
            else if (key == 14) { text = replace ? "" : text.isEmpty() ? "" : text.substring(0, text.length() - 1); replace = false; }
            else if ((Character.isDigit(c) || c == '-' || c == '.') && text.length() < 10) { text = (replace ? "" : text) + c; replace = false; }
            return;
        }
        if (key == 1) ave.A().a((axu) null);
    }

    @Override public void m() { drag = edit = null; textEdit = null; popup = null; VioletRuntime.reset(); VioletRuntime.save(); }
    @Override public boolean d() { return false; }
}
