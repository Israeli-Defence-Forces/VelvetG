package me.movi.violet;
import java.awt.Color;
import org.lwjgl.nanovg.NanoVG;
import me.eldodebug.soar.management.mods.*;
import me.eldodebug.soar.management.mods.impl.*;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.*;
import me.eldodebug.soar.gui.modmenu.category.impl.ModuleCategory;
/** Keep native module-list text within its available description lane. */
public final class VioletUi {
 private static final String[] ASSIST_ICONS={"T","M","S"};
 private VioletUi(){}
 public static String description(Mod mod){if(mod instanceof HitDelayFixMod)return "Removes the missed-hit cooldown.";if(mod instanceof NoJumpDelayMod)return "Removes the jump cooldown.";if(mod instanceof NoBreakDelayMod)return "Removes the delay between block breaks.";if(mod instanceof ItemUseDelayFixMod)return "Retries held use as soon as it is available.";return mod.getDescription();}
 public static void descriptionText(NanoVGManager n,String text,float x,float y,Color color,float size,Font font,ModuleCategory category){float width=category.getX()+category.getWidth()-53-x;if(width<=0)return;n.save();if(n.getContext()!=0)NanoVG.nvgIntersectScissor(n.getContext(),x,y-2,width,size+7);n.drawText(n.getLimitText(text,size,font,width),x,y,color,size,font);n.restore();}
 private static boolean violet(Mod m){return m instanceof VioletModule||m instanceof VioletHud||m instanceof AssistModule;}
 public static void tile(NanoVGManager n,float x,float y,float w,float h,float radius,Color color,Mod mod){n.drawRoundedRect(x,y,w,h,radius,violet(mod)?VioletStyle.CARD:color);if(violet(mod))n.drawCenteredText(mod instanceof AssistModule?ASSIST_ICONS[((AssistModule)mod).kind]:"V",x+w/2,y+h/2-7,VioletStyle.accent(),15,Fonts.SEMIBOLD);}
 public static void gradientTile(NanoVGManager n,float x,float y,float w,float h,float radius,Color a,Color b,Mod mod){n.drawGradientRoundedRect(x,y,w,h,radius,a,b);if(violet(mod)){n.drawRoundedRect(x,y,w,h,radius,VioletStyle.alpha(VioletStyle.accent(),50));n.drawCenteredText(mod instanceof AssistModule?ASSIST_ICONS[((AssistModule)mod).kind]:"V",x+w/2,y+h/2-7,VioletStyle.TEXT,15,Fonts.SEMIBOLD);}}
}
