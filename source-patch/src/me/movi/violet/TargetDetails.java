package me.movi.violet;

import java.lang.reflect.Field;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModManager;
import me.eldodebug.soar.management.mods.impl.TargetInfoMod;
import vgstub.ave;
import vgstub.pr;

/** Add distance and context cleanup to the existing Target Info HUD without replacing its appearance. */
public final class TargetDetails {
    private static TargetInfoMod target;
    private static VioletSettings.Flag distance;
    private static Object world,player;
    private static Field name,head;
    private TargetDetails() {}
    public static void install(ModManager manager){for(Mod m:manager.getMods())if(m instanceof TargetInfoMod){target=(TargetInfoMod)m;distance=new VioletSettings.Flag(m,"target.distance","Distance label",true);break;}}
    public static void context(TargetInfoMod mod){
        ave mc=ave.A();if(mc.m instanceof GuiEditHUD)return;
        boolean changed=world!=mc.f||player!=mc.h;world=mc.f;player=mc.h;
        if(changed||mc.f==null||mc.h==null||mc.s==null||!(mc.s.d instanceof pr)||mc.s.d.I){
            try{if(name==null){name=TargetInfoMod.class.getDeclaredField("name");head=TargetInfoMod.class.getDeclaredField("head");name.setAccessible(true);head.setAccessible(true);}name.set(mod,null);head.set(mod,null);}
            catch(ReflectiveOperationException e){throw new IllegalStateException("Target Info baseline fields changed",e);}
        }
    }
    public static void render(TargetInfoMod mod){
        ave mc=ave.A();if(mod!=target||distance==null||!distance.isToggled()||mc.h==null||mc.s==null||!(mc.s.d instanceof pr)||mc.s.d.I)return;
        mod.drawText(Math.round(Math.sqrt(mc.h.h(mc.s.d)))+"m",Math.max(52,mod.getWidth()-28),39,6,mod.getHudFont(0));
    }
}
