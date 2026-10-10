package me.movi.violet;

import java.awt.Color;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;

/** Shared low-level controls for Violet and its compact module settings. */
public final class VioletControls {
    private VioletControls() {}

    public static boolean hit(float u, float v, float x, float y, float w, float h) {
        return u >= x && u <= x + w && v >= y && v <= y + h;
    }

    public static void button(NanoVGManager n, float x, float y, float w, float h, String text,
                              boolean selected, boolean available, float u, float v) {
        Color accent = VioletStyle.accent();
        Color fill = selected ? VioletStyle.alpha(accent, 35)
            : available && hit(u, v, x, y, w, h) ? VioletStyle.LINE : VioletStyle.CARD;
        Color textColor = available ? (selected ? accent : VioletStyle.TEXT) : VioletStyle.alpha(VioletStyle.MUTED, 180);
        n.drawRoundedRect(x, y, w, h, VioletStyle.R_CONTROL, fill);
        n.drawCenteredText(text, x + w / 2, y + h / 2 - 4, textColor, VioletStyle.T_SMALL, Fonts.MEDIUM);
    }

    public static void quiet(NanoVGManager n, float x, float y, float w, String text, float u, float v) {
        n.drawCenteredText(text, x + w / 2, y + 4,
            hit(u, v, x, y, w, 22) ? VioletStyle.accent() : VioletStyle.MUTED,
            VioletStyle.T_SMALL, Fonts.MEDIUM);
    }
}
