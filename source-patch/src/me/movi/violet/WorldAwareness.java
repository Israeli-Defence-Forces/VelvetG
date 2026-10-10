package me.movi.violet;

import java.awt.Color;
import java.util.ArrayList;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import vgstub.*;
import vgstub.auh.a;

public final class WorldAwareness extends HUDMod {
    public static WorldAwareness instance;
    public final VioletSettings.Flag beds,bedBoxes,players,playerBoxes,names,distances,health,teamColors;
    public final VioletSettings.Range range,opacity;
    private final ArrayList<Setting> settings=new ArrayList<Setting>();
    private final ObservedBeds index=new ObservedBeds();
    private final wn[] visible=new wn[8];
    private final String[] labels=new String[8];
    private final Color[] colors=new Color[8];
    private int count,ticks,bedCursor;
    private Object world,player;
    public WorldAwareness(){super(TranslateText.CPS_DISPLAY,TranslateText.CPS_DISPLAY_DESCRIPTION);setX(15);setY(300);beds=flag("beds","Observed bed waypoints",true);bedBoxes=flag("bedBoxes","Visible bed boxes",false);players=flag("players","Visible player information",false);playerBoxes=flag("playerBoxes","Visible player boxes",false);names=flag("names","Player names",true);distances=flag("distances","Distance labels",true);health=flag("health","Observed player health",false);teamColors=flag("teamColors","Scoreboard team colours",true);range=new VioletSettings.Range(this,"world.range","Range (blocks)",64,8,128,true);settings.add(range);opacity=new VioletSettings.Range(this,"world.opacity","Opacity (%)",80,20,100,true);settings.add(opacity);}
    private VioletSettings.Flag flag(String id,String name,boolean value){VioletSettings.Flag f=new VioletSettings.Flag(this,"world."+id,name,value);settings.add(f);return f;}
    public Setting[] controls(){return settings.toArray(new Setting[settings.size()]);}
    public String getName(){return "World Awareness";}
    public String getNameKey(){return "text.violet.world";}
    public String getDescription(){return "Observed beds and visible player information.";}
    @Override public void onDisable(){super.onDisable();clear();}
    private void clear(){index.clear();count=ticks=bedCursor=0;java.util.Arrays.fill(visible,null);}
    public void tick(ave mc){if(!isToggled())return;try{observe(mc);}catch(Exception e){setToggled(false);GlideLogger.error("[Violet] World awareness disabled after observation error",e);}}
    private void observe(ave mc){
        if(mc==null||world!=mc.f||player!=mc.h){clear();world=mc==null?null:mc.f;player=mc==null?null:mc.h;}
        if(mc==null||mc.f==null||mc.h==null||mc.m!=null||!mc.w){count=0;return;}
        if(++ticks%4!=0)return;
        count=0;
        if(players.isToggled()||playerBoxes.isToggled()){int scanned=0;for(wn p:mc.f.j){if(++scanned>64||count==8)break;if(p==mc.h||p.I||p.ax()||!p.ai()||mc.h.h(p)>range.getValue()*range.getValue()||!mc.h.t(p))continue;visible[count]=p;String label=names.isToggled()?p.e_():"Player";if(distances.isToggled())label+=" "+Math.round(Math.sqrt(mc.h.h(p)))+"m";if(health.isToggled())label+=" "+Math.round(p.bn())+"HP";labels[count]=label;colors[count]=teamColor(p);count++;}}
        if(!beds.isToggled()&&!bedBoxes.isToggled()){index.clear();return;}
        MatchState.State match=MatchState.classify(true,BedWarsAwareness.sidebar(mc),BedWarsAwareness.instance.marker.getText());
        if(match==MatchState.State.COMPLETE||match==MatchState.State.WAITING){index.clear();return;}
        // No coordinates inferred from lobbies or map templates. Crosshair blocks are directly observed.
        if(mc.s!=null&&mc.s.a==a.b){cj p=mc.s.a();afh block=mc.f.p(p).c();String id=String.valueOf(afh.c.c(block));if(id.equals("minecraft:bed"))index.observe(p.n(),p.o(),p.p());else if(id.equals("minecraft:obsidian")||id.equals("minecraft:wool")||id.equals("minecraft:end_stone")||id.equals("minecraft:planks"))index.material(p.n(),p.o(),p.p(),id.substring(10));}
        int n=index.values().size(),at=0;if(n>0){int wanted=bedCursor++%n;for(ObservedBeds.Bed b:index.values()){if(at++!=wanted)continue;cj pos=new cj(b.x,b.y,b.z);if(mc.h.e(b.x,b.y,b.z)>range.getValue()*range.getValue())break;if(!mc.f.e(pos))break;aui eye=new aui(mc.h.s,mc.h.t+mc.h.aS(),mc.h.u),target=new aui(b.x+.5,b.y+.3,b.z+.5);auh hit=mc.f.a(eye,target,false,true,false);if(hit==null||pos.equals(hit.a()))index.changedVisible(b.x,b.y,b.z,mc.f.p(pos).c()==afh.c.a(new jy("bed")));break;}}
    }
    private Color teamColor(wn p){if(!teamColors.isToggled())return VioletStyle.accent();auq team=p.bO();if(team instanceof aul){vgstub.a code=((aul)team).l();if(code!=null&&code.b()>=0){int i=code.b();int base=(i&8)!=0?85:0;int r=((i&4)!=0?170:0)+base,g=((i&2)!=0?170:0)+base,b=((i&1)!=0?170:0)+base;if(i==6)r+=85;return new Color(r,g,b);}}return VioletStyle.accent();}
    @EventTarget public void render(EventRender2D e){ave mc=ave.A();boolean preview=mc.m instanceof GuiEditHUD;if(!preview&&(mc.h==null||mc.m!=null))return;int rows=(players.isToggled()?count:0)+(beds.isToggled()?Math.min(8,index.values().size()):0);setWidth(220);setHeight(22+Math.max(1,rows)*12);NanoVGManager n=Glide.getInstance().getNanoVGManager();n.setupAndDraw(()->{n.save();try{n.translate(getX(),getY());n.scale(0,0,getScale());n.setAlpha((float)opacity.getValue()/100);n.drawRoundedRect(0,0,220,getHeight(),VioletStyle.R_CARD,VioletStyle.PANEL);n.drawText("World awareness",8,6,VioletStyle.accent(),9,Fonts.SEMIBOLD);int y=22;if(players.isToggled())for(int i=0;i<count;i++){n.drawText(n.getLimitText(labels[i],8,Fonts.REGULAR,204),8,y,colors[i],8,Fonts.REGULAR);y+=12;}if(beds.isToggled()&&mc.h!=null){int shown=0;for(ObservedBeds.Bed b:index.values()){double d=Math.sqrt(mc.h.e(b.x,b.y,b.z));if(d>range.getValue()||shown++>=8)continue;String label=b.destroyed?"Bed destroyed (observed)":"Bed "+b.x+", "+b.z+(distances.isToggled()?" "+Math.round(d)+"m":"")+" · "+b.material;n.drawText(n.getLimitText(label,8,Fonts.REGULAR,204),8,y,VioletStyle.TEXT,8,Fonts.REGULAR);y+=12;}}if(preview)n.drawText("Observed information",8,y,VioletStyle.MUTED,8,Fonts.REGULAR);}finally{n.restore();}});}
    @EventTarget public void boxes(EventRender3D e){ave mc=ave.A();if(mc.h==null||mc.m!=null||!playerBoxes.isToggled()&&!bedBoxes.isToggled())return;WorldLines.begin(mc,e.getPartialTicks());try{if(playerBoxes.isToggled())for(int i=0;i<count;i++){wn p=visible[i];if(p==null||p.I||p.ax())continue;float f=e.getPartialTicks();WorldLines.box(p.p+(p.s-p.p)*f,p.q+(p.t-p.q)*f,p.r+(p.u-p.r)*f,p.J,p.K,colors[i]);}if(bedBoxes.isToggled())for(ObservedBeds.Bed b:index.values())if(!b.destroyed&&mc.h.e(b.x,b.y,b.z)<=range.getValue()*range.getValue())WorldLines.box(b.x+.5,b.y,b.z+.5,1,.56,VioletStyle.accent());}finally{WorldLines.end();}}
}
