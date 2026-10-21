"""Real trainer adapter source, executable native API contract; not a game launch."""
import importlib.util,shutil,subprocess,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('rc1_gui_contract',ROOT.parent/'combatiq/native-v4/test_native_gui_contract.py')
old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old)
STUBS={k:v for k,v in old.STUBS.items() if not k.endswith('GuiContractHarness.java')}
STUBS['me/eldodebug/soar/Glide.java']='''package me.eldodebug.soar;
public class Glide{static final Glide G=new Glide();public static Glide getInstance(){return G;}
 public me.eldodebug.soar.management.mods.ModManager getModManager(){return me.eldodebug.soar.management.mods.ModManager.INSTANCE;}
 public me.eldodebug.soar.management.nanovg.NanoVGManager getNanoVGManager(){return me.eldodebug.soar.management.nanovg.NanoVGManager.INSTANCE;}}'''
p='me/eldodebug/soar/management/mods/HUDMod.java'
STUBS[p]=STUBS[p].replace('public void drawBackground(float a,float b,float c,float d){}','''public void drawBackground(float a,float b,float c,float d){}
 public void drawRect(float a,float b,float c,float d,java.awt.Color z){}public void drawRoundedRect(float a,float b,float c,float d,float e,java.awt.Color z){}
 public void drawText(String s,float a,float b,float c,Object f,java.awt.Color color){drawText(s,a,b,c,f);}''')
p='me/eldodebug/soar/management/mods/settings/impl/NumberSetting.java'
STUBS[p]=STUBS[p].replace('public long getValueLong()', 'public double getValue(){return value;}public int getValueInt(){return (int)value;}public float getValueFloat(){return (float)value;}public long getValueLong()')
STUBS['me/eldodebug/soar/management/mods/settings/impl/ColorSetting.java']='''package me.eldodebug.soar.management.mods.settings.impl;
import java.awt.Color;import me.eldodebug.soar.management.language.TranslateText;import me.eldodebug.soar.management.mods.Mod;import me.eldodebug.soar.management.mods.settings.Setting;
public class ColorSetting extends Setting{private Color color;public ColorSetting(TranslateText t,Mod m,Color c,boolean a){super(t,m);color=c;me.eldodebug.soar.Glide.getInstance().getModManager().addSettings(this);}public Color getColor(){return color;}public void setColor(Color c){color=c;}}'''
STUBS['org/lwjgl/nanovg/NVGColor.java']='package org.lwjgl.nanovg;public class NVGColor{}'
STUBS['org/lwjgl/nanovg/NanoVG.java']='''package org.lwjgl.nanovg;public class NanoVG{public static int lines,strokes,saves,restores;
 public static void nvgSave(long v){saves++;}public static void nvgRestore(long v){restores++;}public static void nvgScissor(long v,float a,float b,float c,float d){}
 public static void nvgBeginPath(long v){}public static void nvgMoveTo(long v,float a,float b){}public static void nvgLineTo(long v,float a,float b){lines++;}
 public static void nvgStrokeColor(long v,NVGColor c){}public static void nvgStrokeWidth(long v,float w){}public static void nvgStroke(long v){strokes++;}}'''
STUBS['me/eldodebug/soar/management/nanovg/NanoVGManager.java']='''package me.eldodebug.soar.management.nanovg;
public class NanoVGManager{public static final NanoVGManager INSTANCE=new NanoVGManager();private final org.lwjgl.nanovg.NVGColor c=new org.lwjgl.nanovg.NVGColor();
 public void setupAndDraw(Runnable r){r.run();}public void save(){}public void restore(){}public void setAlpha(float a){}public long getContext(){return 1;}public org.lwjgl.nanovg.NVGColor getColor(java.awt.Color a){return c;}public void drawCircle(float x,float y,float z,java.awt.Color c){}}'''
STUBS['TrainerGuiHarness.java']='''import me.eldodebug.soar.knockback.*;import me.eldodebug.soar.management.mods.*;import me.eldodebug.soar.management.mods.settings.*;import me.eldodebug.soar.management.mods.settings.impl.*;import me.eldodebug.soar.management.event.impl.*;
public class TrainerGuiHarness{
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static Setting setting(String key){for(Setting s:ModManager.INSTANCE.settings)if(s.getNameKey().equals("knockbacktrainer."+key))return s;throw new AssertionError(key);}
 public static void main(String[] args)throws Exception{
  ave.A();KnockbackTrainerMod m=new KnockbackTrainerMod();check(!m.isToggled(),"default off");
  java.util.Set<String> keys=new java.util.HashSet<String>();for(Setting s:ModManager.INSTANCE.settings)check(keys.add(s.getNameKey()),"duplicate key");check(keys.size()==22,"all settings");
  m.setX(71);m.setY(82);m.setScale(1.3f);m.setToggled(true);m.onTick(new EventTick());check(TrainerRuntime.enabled(),"live independent module");
  TrainerRuntime.tickStart();TrainerRuntime.tickEnd();ave.SINGLE.h.C=false;ave.SINGLE.h.w=.3;ave.SINGLE.h.bb.b=1.4;TrainerRuntime.tickStart();TrainerRuntime.tickEnd();
  m.onRender2D(new EventRender2D());check(HUDMod.draws>0,"native text draw");check(org.lwjgl.nanovg.NanoVG.lines>0,"real graph path calls");check(org.lwjgl.nanovg.NanoVG.strokes==2,"batched rising/falling graph");
  int compact=m.getHeight();((BooleanSetting)setting("expanded")).setToggled(true);m.onRender2D(new EventRender2D());check(m.getHeight()>compact,"expanded stats");
  check(m.getX()==71&&m.getY()==82&&m.getScale()==1.3f,"drag/scale preserved");
  ((NumberSetting)setting("scale")).setValue(1.7);m.onTick(new EventTick());check(m.getScale()==1.7f,"scale setting");m.setScale(1.2f);m.onTick(new EventTick());check(((NumberSetting)setting("scale")).getValueFloat()==1.2f,"editor scale sync");
  ((BooleanSetting)setting("reset")).setToggled(true);m.onTick(new EventTick());check(!((BooleanSetting)setting("reset")).isToggled()&&TrainerRuntime.snapshot().attacks==0,"reset action latch");
  // Emulate native profile's stable-key/type save and load after new module construction.
  java.util.Map<String,Object> saved=new java.util.HashMap<String,Object>();for(Setting s:ModManager.INSTANCE.settings){Object value=s instanceof BooleanSetting?((BooleanSetting)s).isToggled():s instanceof NumberSetting?((NumberSetting)s).getValue():s instanceof ColorSetting?((ColorSetting)s).getColor():((TextSetting)s).getText();saved.put(s.getNameKey(),value);}
  ModManager.INSTANCE.settings.clear();KnockbackTrainerMod second=new KnockbackTrainerMod();for(Setting s:ModManager.INSTANCE.settings){Object value=saved.get(s.getNameKey());if(s instanceof BooleanSetting)((BooleanSetting)s).setToggled((Boolean)value);else if(s instanceof NumberSetting)((NumberSetting)s).setValue((Double)value);else if(s instanceof ColorSetting)((ColorSetting)s).setColor((java.awt.Color)value);else ((TextSetting)s).setText((String)value);}
  check(((BooleanSetting)setting("expanded")).isToggled(),"stable persisted setting keys");check(((NumberSetting)setting("scale")).getValueFloat()==1.2f,"settings round trip");
  HUDMod.fail=true;m.onRender2D(new EventRender2D());int count=HUDMod.draws;m.onRender2D(new EventRender2D());check(HUDMod.draws==count,"renderer fault isolation");check(HUDMod.saves==HUDMod.restores,"graphics restore on error");
  m.setToggled(false);check(!TrainerRuntime.enabled(),"disable stops trainer");System.out.println("PASS: "+checks+" native trainer controls/draw/persistence contract assertions; no Minecraft or actual profile I/O");
 }
}'''
class NativeHud(unittest.TestCase):
 def test_actual_adapter_draw_controls_and_persistence_contract(self):
  javac=shutil.which('javac');java=shutil.which('java')
  if not javac or not java:self.skipTest('JDK required')
  with tempfile.TemporaryDirectory() as d:
   d=Path(d);sources=[]
   for name,text in STUBS.items():
    p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);sources.append(p)
   sources+=list((ROOT/'src').rglob('*.java'))+list((ROOT/'native').rglob('*.java'))
   sources+=[ROOT/'tests/MockMinecraft.java',*list((ROOT/'tests/org').rglob('*.java'))]
   v=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True);flags=['-source','8','-target','8'] if v.startswith('javac 1.8') else ['--release','8']
   subprocess.run([javac,*flags,'-d',str(d/'classes'),*map(str,sources)],check=True,capture_output=True,text=True)
   p=subprocess.run([java,'-Xverify:all','-cp',str(d/'classes'),'TrainerGuiHarness'],check=True,capture_output=True,text=True,timeout=20)
   print(p.stdout.strip());self.assertIn('PASS:',p.stdout)
