package me.eldodebug.soar.combatiq;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.event.impl.EventRender2D;

/** Source-level native adapter. ABI-compiled against the available r8 client;
 * exact v3 baseline registration/config persistence and live NanoVG remain untested.
 * Existing native HUDMod owns dragging, scale, preview and theme rendering.
 */
public final class CombatIQMod extends HUDMod {
    private final NamedBoolean recording = new NamedBoolean("Recording", "combatiq.recording", false);
    private final NamedBoolean visible = new NamedBoolean("HUD visible", "combatiq.hud", true);
    private final NamedBoolean expanded = new NamedBoolean("Expanded", "combatiq.expanded", false);
    private final Runnable draw = new Runnable() { public void run() { drawSnapshot(); } };

    // Non-null generic translation fallback prevents base blacklist notifications
    // from dereferencing null; all public names/keys are unique overrides below.
    public CombatIQMod() { super(TranslateText.EDIT_HUD, TranslateText.EDIT_HUD); }
    @Override public String getName() { return "CombatIQ"; }
    @Override public String getNameKey() { return "combatiq.module"; }
    @Override public String getDescription() { return "Experimental local combat telemetry; server outcome unknown"; }
    @Override public void onEnable() {
        super.onEnable();
        if (isToggled()) CombatIQNative.setEnabled(recording.isToggled());
    }
    @Override public void onDisable() {
        CombatIQNative.setEnabled(false);
        super.onDisable();
    }
    @EventTarget public void onTick(EventTick event) {
        CombatIQNative.setEnabled(isToggled() && recording.isToggled());
    }
    @EventTarget public void onRender2D(EventRender2D event) {
        if (!visible.isToggled() && !isEditing()) return;
        Glide.getInstance().getNanoVGManager().setupAndDraw(draw);
    }
    private void drawSnapshot() {
        CombatIQNative.Snapshot s = CombatIQNative.snapshot();
        int height=expanded.isToggled()?100:48;
        setWidth(240); setHeight(height);
        save();
        try {
            drawBackground(getX(), getY(), 240, height);
            drawText("CombatIQ  |  Server outcome unknown",getX()+7,getY()+13,10,getHudFont(0));
            drawText(s.sprint,getX()+7,getY()+27,10,getHudFont(0));
            drawText(s.lastRelease,getX()+7,getY()+41,10,getHudFont(0));
            if (expanded.isToggled()) {
                drawText(s.action,getX()+7,getY()+55,10,getHudFont(0));
                drawText(s.status,getX()+7,getY()+69,10,getHudFont(0));
                drawText(s.health,getX()+7,getY()+83,10,getHudFont(0));
                drawText("Coverage: local observations only",getX()+7,getY()+97,10,getHudFont(0));
            }
        } finally { restore(); }
    }
    private final class NamedBoolean extends BooleanSetting {
        private final String label, key;
        NamedBoolean(String label,String key,boolean value) {
            super(TranslateText.EDIT_HUD,CombatIQMod.this,value);
            this.label=label;this.key=key;
        }
        @Override public String getName() { return label; }
        @Override public String getNameKey() { return key; }
    }
}
