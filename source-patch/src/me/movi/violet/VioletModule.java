package me.movi.violet;
import me.eldodebug.soar.management.mods.*;
import me.eldodebug.soar.management.language.TranslateText;
public final class VioletModule extends Mod {
 public VioletModule(){super(TranslateText.CPS_DISPLAY,TranslateText.CPS_DISPLAY_DESCRIPTION,ModCategory.PLAYER);}
 public String getName(){return "Violet";}public String getNameKey(){return "text.violet.module";}
 public String getDescription(){return "Left/right autoclicker with custom controls.";}
 @Override public void onEnable(){super.onEnable();VioletRuntime.reset();}
 @Override public void onDisable(){super.onDisable();VioletRuntime.reset();}
}
