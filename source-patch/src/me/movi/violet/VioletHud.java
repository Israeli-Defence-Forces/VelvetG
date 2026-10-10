package me.movi.violet;

import java.awt.Color;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import vgstub.ave;

public final class VioletHud extends HUDMod {
    private final float[] history = new float[36];
    private long sampled;
    private int cursor;

    public VioletHud() {
        super(TranslateText.CPS_DISPLAY, TranslateText.CPS_DISPLAY_DESCRIPTION);
        setX(15); setY(80);
    }

    public String getName() { return "Violet HUD"; }
    public String getNameKey() { return "text.violet.hud"; }
    public String getDescription() { return "Click counts and activity. Drag in Edit HUD."; }

    @EventTarget public void render(EventRender2D event) {
        if (VioletRuntime.settings == null) return;
        boolean preview = ave.A().m instanceof GuiEditHUD;
        long now = System.nanoTime();
        int l = total(0, now), r = total(1, now);

        if (!preview && ave.A().m != null && VioletRuntime.settings.flag("menus")) return;
        if (!preview && VioletRuntime.settings.flag("idle") && l + r == 0) return;

        int w = cardWidth(), h = cardHeight();
        setWidth(w); setHeight(h);
        NanoVGManager n = Glide.getInstance().getNanoVGManager();
        long started = VioletDiagnostics.enabled() ? System.nanoTime() : 0;
        n.setupAndDraw(() -> {
            n.save();
            try { n.translate(getX(), getY()); n.scale(0, 0, getScale()); drawCard(n, 0, 0, w, h, preview); }
            finally { n.restore(); }
        });
        if (started != 0) VioletDiagnostics.render(System.nanoTime() - started);
    }

    public static int cardWidth() { return VioletRuntime.settings.choice("layout") == 0 ? 124 : VioletRuntime.settings.choice("layout") == 1 ? 144 : 176; }
    public static int cardHeight() { int layout = VioletRuntime.settings.choice("layout"); return layout == 0 ? 22 : layout == 1 ? (VioletRuntime.settings.flag("graph") ? 52 : 42) : 66 + (VioletRuntime.settings.flag("average") ? 12 : 0) + (VioletRuntime.settings.flag("graph") ? 10 : 0); }
    void clearHistory() { java.util.Arrays.fill(history, 0); sampled = 0; cursor = 0; }

    void tick(long now) {
        if (VioletRuntime.settings == null || now - sampled < 100000000L) return;
        history[cursor] = total(0, now) + total(1, now);
        cursor = (cursor + 1) % history.length;
        sampled = now;
    }

    private static int total(int i, long now) { return VioletRuntime.engine.physicalCps(i, now) + VioletRuntime.engine.generatedCps(i, now); }

    public void drawCard(NanoVGManager n, float x, float y, float w, float h, boolean preview) {
        long now = System.nanoTime();
        int lp = preview ? 3 : VioletRuntime.engine.physicalCps(0, now), lg = preview ? 10 : VioletRuntime.engine.generatedCps(0, now);
        int rp = preview ? 0 : VioletRuntime.engine.physicalCps(1, now), rg = preview ? 8 : VioletRuntime.engine.generatedCps(1, now);
        Color accent = VioletStyle.accent();
        int opacity = (int) (VioletRuntime.settings.number("opacity") * 2.55);

        n.drawRoundedRect(x, y, w, h, VioletStyle.R_CARD, VioletStyle.alpha(VioletStyle.PANEL, opacity));
        n.drawOutlineRoundedRect(x + .5f, y + .5f, w - 1, h - 1, VioletStyle.R_CARD, .6f, VioletStyle.alpha(accent, 65));
        if (VioletRuntime.settings.choice("layout") == 0) {
            n.drawCircle(x + 8, y + 11, 2, preview || VioletRuntime.module.isToggled() ? accent : VioletStyle.MUTED);
            n.drawText("Violet", x + 15, y + 7, accent, VioletStyle.T_SMALL, Fonts.SEMIBOLD);
            n.drawText("L " + (lp + lg), x + 56, y + 7, VioletStyle.TEXT, VioletStyle.T_SMALL, Fonts.MEDIUM);
            n.drawText("R " + (rp + rg), x + 94, y + 7, VioletStyle.TEXT, VioletStyle.T_SMALL, Fonts.MEDIUM);
            return;
        }

        n.drawText("Violet", x + 8, y + 6, accent, VioletStyle.T_SMALL, Fonts.SEMIBOLD);
        String status = preview ? "Preview" : VioletRuntime.module.isToggled() ? VioletRuntime.state : "Disabled";
        status = n.getLimitText(status, VioletStyle.T_SMALL, Fonts.REGULAR, w - 66);
        n.drawText(status, x + w - 8 - n.getTextWidth(status, VioletStyle.T_SMALL, Fonts.REGULAR), y + 6, VioletStyle.MUTED, VioletStyle.T_SMALL, Fonts.REGULAR);

        if (VioletRuntime.settings.choice("layout") == 2) {
            n.drawText("Physical   L " + lp + "   R " + rp, x + 8, y + 19, VioletStyle.TEXT, VioletStyle.T_SMALL, Fonts.MEDIUM);
            n.drawText("Generated  L " + lg + "   R " + rg, x + 8, y + 31, VioletStyle.TEXT, VioletStyle.T_SMALL, Fonts.MEDIUM);
            n.drawText("Combined " + (lp + rp + lg + rg) + " CPS", x + 8, y + 43, accent, VioletStyle.T_SMALL, Fonts.SEMIBOLD);
            n.drawText("L " + (int)VioletRuntime.settings.number("leftMin") + "–" + (int)VioletRuntime.settings.number("leftMax") + "  R " + (int)VioletRuntime.settings.number("rightMin") + "–" + (int)VioletRuntime.settings.number("rightMax"), x + 8, y + 55, VioletStyle.MUTED, 7, Fonts.REGULAR);
            if (VioletRuntime.settings.flag("average")) n.drawText("Average (5s) " + Double.toString(preview ? 16.0 : Math.round(VioletRuntime.engine.averageCps(now) * 10) / 10.0), x + 8, y + 67, VioletStyle.MUTED, 7, Fonts.REGULAR);
        } else {
        float left = x + w / 4, right = x + w * 3 / 4;
        n.drawRect(x + w / 2, y + 18, .5f, Math.max(17, h - 25), VioletStyle.alpha(VioletStyle.LINE, 130));
        n.drawCenteredText("LEFT", left, y + 18, VioletStyle.MUTED, 7, Fonts.MEDIUM);
        n.drawCenteredText("RIGHT", right, y + 18, VioletStyle.MUTED, 7, Fonts.MEDIUM);
        n.drawCenteredText((lp + lg) + " CPS", left, y + 28, VioletStyle.TEXT, VioletStyle.T_BODY, Fonts.SEMIBOLD);
        n.drawCenteredText((rp + rg) + " CPS", right, y + 28, VioletStyle.TEXT, VioletStyle.T_BODY, Fonts.SEMIBOLD);

        }
        if (VioletRuntime.settings.flag("graph")) {
            float bar = (w - 12) / history.length;
            for (int i = 0; i < history.length; i++) {
                float value = preview ? 8 + (float) Math.sin(i * .5) * 4 : history[(cursor + i) % history.length];
                float size = Math.max(2, Math.min(8, value * .3f));
                n.drawRoundedRect(x + 6 + i * bar, y + h - 5 - size, Math.max(1, bar - 1), size, 1, VioletStyle.alpha(accent, 70 + i * 4));
            }
        }
    }
}
