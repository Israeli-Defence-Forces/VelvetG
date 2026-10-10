package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Iterator;
import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.*;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.movi.violet.HealthDelta;
import me.movi.violet.VioletSettings;
import org.lwjgl.opengl.GL11;
import vgstub.pk;
import vgstub.pr;
import vgstub.pe;

/** Enhanced inherited module, not a second damage indicator. Its native module/profile identity stays intact. */
public class DamageParticlesMod extends Mod {
    public final VioletSettings.Range lifetime=new VioletSettings.Range(this,"damage.lifetime","Lifetime (ms)",1000,250,3000,true);
    public final VioletSettings.Range size=new VioletSettings.Range(this,"damage.size","Size (%)",100,50,150,true);
    public final VioletSettings.Range opacity=new VioletSettings.Range(this,"damage.opacity","Opacity (%)",90,20,100,true);
    public final VioletSettings.Flag animate=new VioletSettings.Flag(this,"damage.animate","Float animation",true);
    public final VioletSettings.Flag critical=new VioletSettings.Flag(this,"damage.critical","Local critical feedback",true);
    private final HealthDelta observation=new HealthDelta();
    private final ArrayList<Popup> popups=new ArrayList<Popup>(32);
    private Object world,player;
    private int attack=-1;
    private long attempted;
    private boolean criticalAttempt;
    private static final class Popup {double x,y,z;long at;String text;int rgb;}
    public DamageParticlesMod(){super(TranslateText.DAMAGE_PARTICLES,TranslateText.DAMAGE_PARTICLES_DESCRIPTION,ModCategory.RENDER);}
    private void context(){if(mc==null||world!=mc.f||player!=mc.h){clear();world=mc==null?null:mc.f;player=mc==null?null:mc.h;}}
    private void clear(){observation.clear();popups.clear();attack=-1;attempted=0;criticalAttempt=false;}
    @Override public void onDisable(){super.onDisable();clear();}
    @EventTarget public void onLoadWorld(EventLoadWorld e){clear();world=player=null;}
    @EventTarget public void onTick(EventTick e){context();long now=System.nanoTime();for(Iterator<Popup> i=popups.iterator();i.hasNext();)if(now-i.next().at>=(long)lifetime.getValue()*1000000L)i.remove();}
    @EventTarget public void onAttackEntity(EventAttackEntity e){context();pk target=e.getEntity();if(mc==null||mc.h==null||target==null)return;attack=target.F();attempted=System.nanoTime();criticalAttempt=mc.h.O>0&&!mc.h.C&&!mc.h.k_()&&!mc.h.V()&&!mc.h.a(pe.q)&&mc.h.m==null&&target instanceof pr;}
    @EventTarget public void onLivingUpdate(EventLivingUpdate event){context();pr p=event.getEntity();if(mc==null||mc.h==null||mc.f==null||p==null||p==mc.h||p.o!=mc.f||p.I||p.ax()||mc.h.h(p)>4096||!mc.h.t(p))return;double delta=observation.observe(p.F(),p.bn());if(!Double.isFinite(delta)||Math.abs(delta)<.01)return;long now=System.nanoTime();Popup popup=new Popup();popup.x=p.s;popup.y=p.t+p.K*.6;popup.z=p.u;popup.at=now;popup.text=(delta>0?"−":"+")+Math.round(Math.abs(delta)*10)/10.0;popup.rgb=delta>0?0xFF7777:0x77FF99;if(delta>0&&critical.isToggled()&&criticalAttempt&&attack==p.F()&&now-attempted<=750000000L){popup.text+=" crit?";popup.rgb=0xFFD166;attack=-1;}if(popups.size()==32)popups.remove(0);popups.add(popup);}
    @EventTarget public void onRender3D(EventRender3D event){context();if(mc==null||mc.h==null||mc.m!=null||popups.isEmpty())return;IMixinRenderManager camera=(IMixinRenderManager)mc.af();long now=System.nanoTime();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);try{GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_LIGHTING);for(Popup p:popups){double progress=Math.min(1,(now-p.at)/(lifetime.getValue()*1000000));int alpha=(int)(255*opacity.getValue()/100*(1-progress));if(alpha<4)continue;GL11.glPushMatrix();try{GL11.glTranslated(p.x-camera.getRenderPosX(),p.y-camera.getRenderPosY()+(animate.isToggled()?progress*.5:0),p.z-camera.getRenderPosZ());GL11.glRotatef(-mc.af().e,0,1,0);GL11.glRotatef(mc.af().f,1,0,0);double scale=.025*size.getValue()/100;GL11.glScaled(-scale,-scale,scale);fr.a(p.text,-fr.a(p.text)/2f,0,(alpha<<24)|p.rgb,true);}finally{GL11.glPopMatrix();}}}finally{GL11.glPopAttrib();}}
}
