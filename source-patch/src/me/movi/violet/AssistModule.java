package me.movi.violet;

import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;

/** Independent native modules; the standard module switch and settings gear apply. */
public final class AssistModule extends Mod {
    private static final String[] NAMES = {"Auto Tool", "Inventory Move", "Auto Soup"};
    private static final String[] DESCRIPTIONS = {
        "Selects a hotbar tool while mining.",
        "Move in inventory; text entry pauses it.",
        "Uses hotbar soup at low health."
    };

    public final int kind;

    public AssistModule(int kind) {
        super(TranslateText.CPS_DISPLAY, TranslateText.CPS_DISPLAY_DESCRIPTION, ModCategory.PLAYER);
        this.kind = kind;
    }

    public String getName() { return NAMES[kind]; }
    public String getNameKey() { return "text.violet.assist.module." + kind; }
    public String getDescription() { return DESCRIPTIONS[kind]; }

    @Override public void onDisable() {
        super.onDisable();
        AssistRuntime.disabled(kind);
    }
}
