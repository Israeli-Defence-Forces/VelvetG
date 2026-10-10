package me.movi.violet;

import java.awt.Color;

/** Shared Violet visual tokens. Keep screens on one type/radius/spacing system instead of eyeballing each panel. */
public final class VioletStyle {
    public static final Color PANEL = new Color(22, 18, 32);
    public static final Color CARD = new Color(33, 27, 46);
    public static final Color TEXT = new Color(243, 237, 255);
    public static final Color MUTED = new Color(166, 153, 190);
    public static final Color LINE = new Color(65, 51, 87);

    public static final float R_WINDOW = 10f;
    public static final float R_CARD = 6f;
    public static final float R_CONTROL = 5f;
    public static final float R_PILL = 8f;

    public static final float T_TITLE = 18f;
    public static final float T_SECTION = 13f;
    public static final float T_BODY = 9f;
    public static final float T_SMALL = 8f;

    public static final float CONTROL_H = 24f;
    public static final float ROW_H = 25f;

    private VioletStyle() {}

    public static Color accent() {
        int i = VioletRuntime.settings == null ? 0 : VioletRuntime.settings.choice("accent");
        return i == 1 ? new Color(188, 106, 232) : i == 2 ? new Color(237, 141, 194) : new Color(177, 154, 255);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
