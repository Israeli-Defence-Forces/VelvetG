package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.*;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.SimpleHUDMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.movi.violet.ComboObservation;
import me.movi.violet.VioletSettings;

/** Preserve the inherited module's identity, Icon setting and draggable SimpleHUD appearance. */
public class ComboCounterMod extends SimpleHUDMod {
    private final BooleanSetting iconSetting=new BooleanSetting(TranslateText.ICON,this,true);
    public final VioletSettings.Range timeout=new VioletSettings.Range(this,"combo.timeout","Reset after (ms)",2000,500,5000,true);
    public final VioletSettings.Flag resetTarget=new VioletSettings.Flag(this,"combo.target","Reset on target change",true);
    public final VioletSettings.Flag resetHurt=new VioletSettings.Flag(this,"combo.hurt","Reset when hurt",true);
    private final ComboObservation observation=new ComboObservation();
    private Object world,player;
    public ComboCounterMod(){super(TranslateText.COMBO_COUNTER,TranslateText.COMBO_COUNTER_DESCRIPTION);}
    private void context(){if(mc==null||world!=mc.f||player!=mc.h||mc.f==null||mc.h==null){observation.clear();world=mc==null?null:mc.f;player=mc==null?null:mc.h;}}
    @EventTarget public void onRender2D(EventRender2D e){context();draw();}
    @EventTarget public void onTick(EventTick e){context();observation.tick(System.nanoTime(),(long)timeout.getValue()*1000000L);}
    @EventTarget public void onAttackEntity(EventAttackEntity e){context();if(mc!=null&&mc.h!=null&&mc.f!=null&&e.getEntity()!=null)observation.attack(e.getEntity().F(),System.nanoTime(),resetTarget.isToggled());}
    @EventTarget public void onDamageEntity(EventDamageEntity e){context();if(e.getEntity()==null)return;if(mc!=null&&e.getEntity()==mc.h&&resetHurt.isToggled())observation.clear();else observation.hurt(e.getEntity().F(),System.nanoTime());}
    @Override public void onDisable(){super.onDisable();observation.clear();world=player=null;}
    public String getText(){return observation.count()==0?"No Combo":observation.count()+" Combo";}
    public String getIcon(){return iconSetting.isToggled()?"D":null;}
}
