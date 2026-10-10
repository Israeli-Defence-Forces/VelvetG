package me.movi.violet;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
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
import org.lwjgl.opengl.GL11;
import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import vgstub.*;
import vgstub.auh.a;

/** Observed information only. Short predictions are cached on the client tick, never computed during rendering. */
public final class BedWarsAwareness extends HUDMod implements ProjectilePrediction.Environment {
    public static BedWarsAwareness instance;
    public final VioletSettings.Flag[] types = new VioletSettings.Flag[5];
    public final VioletSettings.Flag distance, paths, collision, sound, gate, fireAlerts, pearlAlerts, armorAlerts, obsidianAlerts;
    public final VioletSettings.Range range, size, opacity;
    public final VioletSettings.Pick presentation;
    public final VioletSettings.Text marker;
    private final List<Setting> settings = new ArrayList<Setting>();
    private final AlertCooldown cooldown = new AlertCooldown();
    private final Track[] tracks = new Track[8];
    private final String[] messages = new String[3];
    private final long[] expires = new long[3];
    private Object world, player;
    private ave client;
    private int count, ticks, scanCursor;
    private MatchState.State state = MatchState.State.DISCONNECTED;
    private static final String[] LABELS = {"Fireball", "Pearl", "Arrow", "Snowball", "Egg"};
    private static final class Track { pk entity; int kind, points; String label; double distance; final double[] path = new double[51]; }

    public BedWarsAwareness() {
        super(TranslateText.CPS_DISPLAY, TranslateText.CPS_DISPLAY_DESCRIPTION); setX(15);setY(140);setWidth(210);setHeight(150);
        for(int i=0;i<5;i++){types[i]=flag("type"+i,LABELS[i]+" indicators",true);tracks[i]=new Track();}
        for(int i=5;i<tracks.length;i++)tracks[i]=new Track();
        gate=flag("gate","Active BedWars only",true);marker=new VioletSettings.Text(this,"awareness.marker","Sidebar marker","bedwars");settings.add(marker);
        distance=flag("distance","Distance labels",true);paths=flag("paths","Estimated trajectories",false);collision=flag("collision","Approach warnings",true);
        fireAlerts=flag("fire","Fireball alerts",true);pearlAlerts=flag("pearl","Pearl alerts",true);armorAlerts=flag("armor","Diamond leggings alerts",true);obsidianAlerts=flag("obsidian","Observed obsidian alerts",true);
        sound=flag("sound","Sound cues",false);
        range=new VioletSettings.Range(this,"awareness.range","Range (blocks)",64,8,128,true);settings.add(range);
        size=new VioletSettings.Range(this,"awareness.size","Indicator size",10,6,16,true);settings.add(size);
        opacity=new VioletSettings.Range(this,"awareness.opacity","Opacity (%)",80,20,100,true);settings.add(opacity);
        presentation=new VioletSettings.Pick(this,"awareness.presentation","Presentation",0,new String[]{"Indicators and alerts","Indicators only","Alerts only"});settings.add(presentation);
    }
    private VioletSettings.Flag flag(String id,String label,boolean value){VioletSettings.Flag f=new VioletSettings.Flag(this,"awareness."+id,label,value);settings.add(f);return f;}
    public Setting[] controls(){return settings.toArray(new Setting[settings.size()]);}
    public String getName(){return "BedWars Awareness";}
    public String getNameKey(){return "text.violet.awareness";}
    public String getDescription(){return "Observed projectiles and BedWars warnings.";}
    public MatchState.State matchState(){return state;}
    @Override public void onDisable(){super.onDisable();clear();}
    private void clear(){count=ticks=scanCursor=0;cooldown.clear();java.util.Arrays.fill(messages,null);java.util.Arrays.fill(expires,0);for(Track t:tracks){t.entity=null;t.points=0;}state=MatchState.State.DISCONNECTED;}
    public void tick(ave mc) {
        if(!isToggled())return;
        try { tickObserved(mc); }
        catch(Exception e){setToggled(false);GlideLogger.error("[Violet] Awareness disabled after observation error",e);}
    }
    private void tickObserved(ave mc){
        if(mc==null || world!=mc.f || player!=mc.h){clear();world=mc==null?null:mc.f;player=mc==null?null:mc.h;}
        client=mc;
        if(mc==null||mc.f==null||mc.h==null){state=MatchState.State.DISCONNECTED;return;}
        if(mc.m!=null||!mc.w){count=0;return;}
        if(++ticks%4!=0)return;
        MatchState.State next=MatchState.classify(true,sidebar(mc),marker.getText());
        if(next!=state && (next==MatchState.State.WAITING||next==MatchState.State.COMPLETE||next==MatchState.State.LOBBY)){clear();}
        state=next;count=0;
        if(gate.isToggled()&&state!=MatchState.State.ACTIVE)return;
        long now=System.nanoTime();double max=range.getValue();
        List<pk> entities=mc.f.f;
        if(entities!=null&&!entities.isEmpty()){
            int start=scanCursor%entities.size(),limit=Math.min(256,entities.size());
            for(int j=0;j<limit&&count<tracks.length;j++){
                pk e=entities.get((start+j)%entities.size());int kind=kind(e);
                if(e instanceof ws&&((ws)e).a==mc.h||e instanceof wy&&((wy)e).n()==mc.h||e instanceof wq&&((wq)e).c==mc.h)continue;
                if(kind<0||!types[kind].isToggled()||e.I||e.ax()||e==mc.h||!Double.isFinite(e.s+e.t+e.u+e.v+e.w+e.x)||mc.h.h(e)>max*max||!mc.h.t(e))continue;
                Track t=tracks[count++];t.entity=e;t.kind=kind;t.distance=Math.sqrt(mc.h.h(e));t.label=LABELS[kind]+(distance.isToggled()?" "+Math.round(t.distance)+"m":"");t.points=0;
                if(paths.isToggled()||collision.isToggled()){
                    ws fire=e instanceof ws?(ws)e:null;
                    t.points=ProjectilePrediction.predict(t.path,e.s,e.t,e.u,e.v,e.w,e.x,kind,fire==null?0:fire.b,fire==null?0:fire.c,fire==null?0:fire.d,16,this);
                }
                if(kind==0&&fireAlerts.isToggled())alert("fire:"+e.F(),"Fireball nearby",now);
                if(kind==1&&pearlAlerts.isToggled()&&t.distance<=24)alert("pearl:"+e.F(),"Pearl nearby",now);
                if(collision.isToggled())for(int k=1;k<t.points;k++){
                    int a=(k-1)*3,b=k*3;
                    if(ProjectilePrediction.segmentDistanceSquared(mc.h.s,mc.h.t+.9,mc.h.u,t.path[a],t.path[a+1],t.path[a+2],t.path[b],t.path[b+1],t.path[b+2])<4){alert("approach:"+e.F(),LABELS[kind]+" may approach",now);break;}
                }
            }
            scanCursor=(start+limit)%entities.size();
        }
        if(armorAlerts.isToggled()&&mc.f.j!=null){int n=0;for(wn p:mc.f.j){if(++n>32)break;if(p==mc.h||p.I||mc.h.h(p)>max*max||!mc.h.t(p)||p.ax())continue;zx leggings=p.bi==null?null:p.bi.b[1];if(leggings!=null&&leggings.b()==zw.d("diamond_leggings"))alert("leggings:"+p.F(),"Diamond leggings: "+p.e_(),now);}}
        if(obsidianAlerts.isToggled()&&mc.s!=null&&mc.s.a==a.b){cj pos=mc.s.a();if(mc.f.e(pos)&&mc.f.p(pos).c()==afh.c.a(new jy("obsidian")))alert("obsidian:"+pos.g(),"Obsidian observed",now);}
    }
    private void alert(String key,String text,long now){if(!cooldown.accept(key,now,60000000000L))return;for(int i=2;i>0;i--){messages[i]=messages[i-1];expires[i]=expires[i-1];}messages[0]=text;expires[0]=now+3500000000L;if(sound.isToggled()&&cooldown.sound(now))client.h.a("note.pling",.35f,1.2f);}
    private static int kind(pk e){if(e instanceof ws)return 0;if(e instanceof xa)return 1;if(e instanceof wq)return 2;if(e instanceof wx)return 3;if(e instanceof wz)return 4;return -1;}
    public static String[] sidebar(ave mc){
        auo board=mc.f.Z();auk objective=null;aul team=board.h(mc.h.e_());if(team!=null&&team.l()!=null&&team.l().b()>=0)objective=board.a(3+team.l().b());if(objective==null)objective=board.a(1);if(objective==null)return new String[0];
        ArrayList<String> lines=new ArrayList<String>(16);lines.add(objective.d());ArrayList<aum> scores=new ArrayList<aum>(board.i(objective));scores.removeIf(s->s.e()==null||s.e().startsWith("#"));scores.sort(aum.a);
        for(int i=Math.max(0,scores.size()-15);i<scores.size();i++){String name=scores.get(i).e();lines.add(aul.a(board.h(name),name));}return lines.toArray(new String[lines.size()]);
    }
    public boolean loaded(double x,double y,double z){return client.f.e(new cj(x,y,z));}
    public boolean water(double x,double y,double z){String id=String.valueOf(afh.c.c(client.f.p(new cj(x,y,z)).c()));return id.equals("minecraft:water")||id.equals("minecraft:flowing_water");}
    public boolean collision(double x,double y,double z,double nx,double ny,double nz,double[] impact){auh hit=client.f.a(new aui(x,y,z),new aui(nx,ny,nz),false,true,false);if(hit==null)return false;impact[0]=hit.c.a;impact[1]=hit.c.b;impact[2]=hit.c.c;return true;}
    @EventTarget public void render(EventRender2D e){
        ave mc=ave.A();boolean preview=mc.m instanceof GuiEditHUD;if(!preview&&(mc.m!=null||mc.h==null||gate.isToggled()&&state!=MatchState.State.ACTIVE))return;
        setWidth(210);setHeight(presentation.index()==2?62:150);NanoVGManager n=Glide.getInstance().getNanoVGManager();
        n.setupAndDraw(()->{n.save();try{n.translate(getX(),getY());n.scale(0,0,getScale());n.setAlpha((float)opacity.getValue()/100);n.drawRoundedRect(0,0,210,getHeight(),VioletStyle.R_CARD,VioletStyle.PANEL);n.drawText("BedWars",8,6,VioletStyle.accent(),9,Fonts.SEMIBOLD);
            if(presentation.index()!=2){n.drawCircle(105,66,2,VioletStyle.accent());for(int i=0;i<count;i++){Track t=tracks[i];if(t.entity==null||t.entity.I)continue;double a=Math.atan2(-(t.entity.s-mc.h.s),t.entity.u-mc.h.u)-Math.toRadians(mc.h.y);float x=105+(float)Math.sin(a)*55,y=66-(float)Math.cos(a)*37;String arrow=Math.abs(Math.sin(a))>Math.abs(Math.cos(a))?(Math.sin(a)>0?">":"<"):(Math.cos(a)>0?"^":"v");n.drawCenteredText(arrow,x,y,VioletStyle.accent(),(float)size.getValue(),Fonts.SEMIBOLD);n.drawCenteredText(t.label,x,y+12,VioletStyle.TEXT,7,Fonts.REGULAR);}}
            if(presentation.index()!=1){int top=presentation.index()==2?22:110;long now=System.nanoTime();for(int i=0;i<3;i++)if(messages[i]!=null&&now<expires[i])n.drawText(n.getLimitText(messages[i],8,Fonts.REGULAR,194),8,top+i*12,VioletStyle.TEXT,8,Fonts.REGULAR);if(preview)n.drawText("Observed warnings",8,top,VioletStyle.MUTED,8,Fonts.REGULAR);}
        }finally{n.restore();}});
    }
    @EventTarget public void renderPaths(EventRender3D e){
        ave mc=ave.A();if(!paths.isToggled()||mc.h==null||mc.m!=null||gate.isToggled()&&state!=MatchState.State.ACTIVE)return;
        IMixinRenderManager camera=(IMixinRenderManager)mc.af();double x=camera.getRenderPosX(),y=camera.getRenderPosY(),z=camera.getRenderPosZ();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();try{GL11.glTranslated(-x,-y,-z);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);GL11.glLineWidth(1);GL11.glColor4f(.75f,.6f,1,(float)opacity.getValue()/100);for(int i=0;i<count;i++){Track t=tracks[i];if(t.entity==null||t.entity.I)continue;GL11.glBegin(GL11.GL_LINE_STRIP);for(int j=0;j<t.points;j++)GL11.glVertex3d(t.path[j*3],t.path[j*3+1],t.path[j*3+2]);GL11.glEnd();}}finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    }
}
