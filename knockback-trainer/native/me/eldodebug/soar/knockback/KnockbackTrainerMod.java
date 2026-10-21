package me.eldodebug.soar.knockback;
import java.awt.Color;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.*;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import static org.lwjgl.nanovg.NanoVG.*;
/** Real native draggable widget; ordinary native settings/config own persistence. */
public final class KnockbackTrainerMod extends HUDMod {
 private final Toggle timing=new Toggle("Jump Timing Feedback","timing",true),graph=new Toggle("Ground Distance Graph","graph",true),stats=new Toggle("Knockback Statistics","stats",true),
  fallingSound=new Toggle("Falling attack sound (attempts)","fallSound",true),timingSound=new Toggle("Timing sound","timingSound",false),popup=new Toggle("On-screen feedback","popup",true),
  debug=new Toggle("Debug indicators","debug",false),expanded=new Toggle("Expanded HUD","expanded",false),reset=new Toggle("Reset session","reset",false);
 private final Number target=new Number("Target offset ticks","target",0,0,6,true),window=new Number("Matching window ticks","window",5,0,10,true),
  volume=new Number("Sound volume","volume",.5,0,1,false),duration=new Number("Feedback seconds","duration",2,.3,6,false),history=new Number("Graph history seconds","history",6,2,30,true),
  graphScale=new Number("Graph height in blocks","graphScale",3,.5,16,false),alpha=new Number("Background opacity","alpha",.7,0,1,false),hudScale=new Number("HUD scale","scale",1,.5,3,false);
 private final Tint good=new Tint("Perfect / ascending colour","good",new Color(87,220,172)),bad=new Tint("Early / descending colour","bad",new Color(244,140,94)),accent=new Tint("Attack / marker colour","accent",new Color(135,159,255)),lateColor=new Tint("Late / missed colour","lateColor",new Color(245,100,126));
 private final SoundName sound=new SoundName();
 private final Runnable draw=new Runnable(){public void run(){drawTrainer();}};
 private boolean failed;private float previousScale=Float.NaN;private int backgroundAlpha=-1;
 private Color background=new Color(18,22,34,180),up=new Color(87,220,172),down=new Color(244,140,94),mark=new Color(135,159,255);
 private static final Color UNKNOWN=new Color(180,185,200),GRID=new Color(130,145,170,50);
 private TrainerEngine.Snapshot cached;private String groundLabel="Ground: waiting for world",reason="Awaiting local hurt + velocity";private String mean="Mean error: --";private final String[] recent=new String[6];
 public KnockbackTrainerMod(){super(TranslateText.EDIT_HUD,TranslateText.EDIT_HUD);setWidth(320);setHeight(185);}
 @Override public String getName(){return "Knockback Trainer";}
 @Override public String getNameKey(){return "knockbacktrainer.module";}
 @Override public String getDescription(){return "Live jump timing, ground graph and local attempt statistics";}
 @Override public void onEnable(){failed=false;super.onEnable();controls();}
 @Override public void onDisable(){TrainerRuntime.configure(false,0,5,6,false,false,0,"note.pling");super.onDisable();}
 private void controls(){
  TrainerRuntime.configure(isToggled(),target.getValueInt(),window.getValueInt(),history.getValueInt(),fallingSound.isToggled(),timingSound.isToggled(),volume.getValueFloat(),sound.getText());
  if(reset.isToggled()){reset.setToggled(false);TrainerRuntime.reset();}
  if(Float.isNaN(previousScale)){previousScale=getScale();hudScale.setValue(previousScale);}
  else if(hudScale.getValueFloat()!=previousScale){setScale(hudScale.getValueFloat());previousScale=getScale();}
  else if(getScale()!=previousScale){previousScale=getScale();hudScale.setValue(previousScale);}
  int a=(int)(255*Math.max(0,Math.min(1,alpha.getValue())));if(a!=backgroundAlpha){backgroundAlpha=a;background=new Color(18,22,34,a);}
  up=good.getColor();down=bad.getColor();mark=accent.getColor();
 }
 @EventTarget public void onTick(EventTick e){try{controls();}catch(Throwable error){TrainerRuntime.configure(false,0,5,6,false,false,0,"note.pling");}}
 @EventTarget public void onRender2D(EventRender2D e){
  if(failed)return;
  try{Glide.getInstance().getNanoVGManager().setupAndDraw(draw);}
  catch(Throwable fault){failed=true;}
 }
 private void text(String s,float y,Color color){drawText(s,getX()+10,getY()+y,10,getHudFont(0),color);}
 private void text(String s,float y){drawText(s,getX()+10,getY()+y,10,getHudFont(0));}
 private void drawTrainer(){
  TrainerEngine.Snapshot s=TrainerRuntime.snapshot();boolean editing=isEditing();
  if(cached!=s){cached=s;mean=Double.isNaN(s.average)?"Mean signed error: --":"Mean signed error: "+(Math.round(s.average*100)/100.0)+" ticks";
   int n=s.distance.length;groundLabel=n==0?"Ground: waiting for world":TrainerEngine.validDistance(s.distance[n-1])?"Ground: "+groundText(s.distance[n-1])+" | dots = attack attempts":"Ground: UNKNOWN (no surface within 16 blocks)";
   reason=s.result==null?"Awaiting local hurt + velocity":s.result.reason;if(reason.length()>53)reason=reason.substring(0,50)+"...";
   for(int i=0;i<recent.length;i++)recent[i]=i<s.recent.length?TrainerEngine.lastText(s.recent[i]):null;}
  int height=43+(graph.isToggled()?92:0)+(stats.isToggled()?66:0)+(expanded.isToggled()?105:0)+(debug.isToggled()?27:0);
  setWidth(320);setHeight(height);save();
  try{
   drawRoundedRect(getX(),getY(),320,height,7,background);text("Knockback Trainer",16);
   if(!TrainerRuntime.fault().isEmpty())text(TrainerRuntime.fault(),32,UNKNOWN);
   else if(timing.isToggled()){
    TrainerEngine.Result r=s.result;double age=r==null?Double.POSITIVE_INFINITY:(System.nanoTime()-r.ns)/1e9;
    if((popup.isToggled()&&age<duration.getValue())||editing){
     Color c=r==null?mark:r.grade==TrainerEngine.Grade.PERFECT?up:r.grade==TrainerEngine.Grade.UNKNOWN?UNKNOWN:r.grade==TrainerEngine.Grade.EARLY?down:lateColor.getColor();
     NanoVGManager nvg=Glide.getInstance().getNanoVGManager();nvg.save();
     try{nvg.setAlpha(editing?1f:(float)Math.max(0,Math.min(1,(duration.getValue()-age)/.3)));text(r==null?"Jump timing: ready":s.summary,32,c);}finally{nvg.restore();}
    }else text("Jump timing ready | local observation clock",32,UNKNOWN);
   }
   int y=43;if(graph.isToggled()){drawGraph(s,y,editing);y+=92;}
   if(stats.isToggled()){text(s.attackText,y+12);text(s.streakText,y+26);text(s.counts,y+40);text(mean,y+54);y+=66;}
   if(expanded.isToggled()){
    text("Accepted falling-hit %: UNKNOWN",y+12,UNKNOWN);text(reason,y+25,UNKNOWN);
    for(int i=0;i<recent.length;i++)if(recent[i]!=null)text(recent[i],y+38+i*12);y+=105;
   }
   if(debug.isToggled()){text(TrainerRuntime.fault().isEmpty()?s.notice:TrainerRuntime.fault(),y+12,UNKNOWN);text("Hurt + velocity proxy; server tick / outcome unknown",y+24,UNKNOWN);}
  }finally{restore();}
 }
 private void drawGraph(TrainerEngine.Snapshot s,int offset,boolean editing){
  NanoVGManager renderer=Glide.getInstance().getNanoVGManager();long vg=renderer.getContext();float left=getX()+10,top=getY()+offset,width=300,height=67;
  drawRect(left,top+height,width,1,GRID);drawRect(left,top+height/2,width,1,GRID);
  int n=s.distance.length;float step=width/(history.getValueInt()*20-1),shift=(float)Math.max(0,Math.min(1,(System.nanoTime()-s.ns)/50000000.0));
  nvgSave(vg);try{
   nvgScissor(vg,left,top,width,height+1);
   for(int direction=0;direction<2;direction++){
    nvgBeginPath(vg);
    for(int i=1;i<n;i++){
     if(!TrainerEngine.validDistance(s.distance[i-1])||!TrainerEngine.validDistance(s.distance[i]))continue;
     boolean ascending=s.velocity[i]>=0;if(ascending!=(direction==0))continue;
     float x=left+width-(n-1-i+shift)*step,x0=x-step;
     float y=top+height-(float)(Math.min(graphScale.getValue(),s.distance[i])/graphScale.getValue()*height);
     float y0=top+height-(float)(Math.min(graphScale.getValue(),s.distance[i-1])/graphScale.getValue()*height);
     nvgMoveTo(vg,x0,y0);nvgLineTo(vg,x,y);
    }
    nvgStrokeColor(vg,renderer.getColor(direction==0?up:down));nvgStrokeWidth(vg,1.8f);nvgStroke(vg);
   }
   for(int i=0;i<n;i++)if(s.markers[i]!=0&&TrainerEngine.validDistance(s.distance[i])){
    float x=left+width-(n-1-i+shift)*step,y=top+height-(float)(Math.min(graphScale.getValue(),s.distance[i])/graphScale.getValue()*height);
    if((s.markers[i]&TrainerEngine.ATTACK)!=0)renderer.drawCircle(x,y,2.5f,(s.markers[i]&TrainerEngine.FALLING)!=0?up:mark);
    if((s.markers[i]&(TrainerEngine.TAKEOFF|TrainerEngine.LANDING))!=0)drawRect(x-.5f,top+height-6,1,6,mark);
    if((s.markers[i]&TrainerEngine.IMPACT)!=0)drawRect(x-1,y-4,2,8,down);
   }
  }finally{nvgRestore(vg);}
  text(editing&&n==0?"Ground graph preview | enter world for live data":groundLabel,offset+82,UNKNOWN);
 }
 private String groundText(double d){return String.valueOf(Math.round(d*100)/100.0)+" blocks";}
 private final class Toggle extends BooleanSetting{
  private final String label,key;Toggle(String l,String k,boolean v){super(TranslateText.EDIT_HUD,KnockbackTrainerMod.this,v);label=l;key="knockbacktrainer."+k;}
  @Override public String getName(){return label;}@Override public String getNameKey(){return key;}
 }
 private final class Number extends NumberSetting{
  private final String label,key;Number(String l,String k,double v,double min,double max,boolean integer){super(TranslateText.EDIT_HUD,KnockbackTrainerMod.this,v,min,max,integer);label=l;key="knockbacktrainer."+k;}
  @Override public String getName(){return label;}@Override public String getNameKey(){return key;}
 }
 private final class Tint extends ColorSetting{
  private final String label,key;Tint(String l,String k,Color c){super(TranslateText.EDIT_HUD,KnockbackTrainerMod.this,c,false);label=l;key="knockbacktrainer."+k;}
  @Override public String getName(){return label;}@Override public String getNameKey(){return key;}
 }
 private final class SoundName extends TextSetting{
  SoundName(){super(TranslateText.EDIT_HUD,KnockbackTrainerMod.this,"note.pling");}
  @Override public String getName(){return "Falling attack sound name";}@Override public String getNameKey(){return "knockbacktrainer.sound";}
 }
}
