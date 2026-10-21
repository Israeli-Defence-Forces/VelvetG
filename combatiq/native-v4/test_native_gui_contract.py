"""Execute the real module/control/render source against its verified native API contract.
This tests adapter behavior, not NanoVG, native config implementation or Minecraft launch.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parent
STUBS={
'me/eldodebug/soar/Glide.java':'''package me.eldodebug.soar;
public class Glide {private static final Glide INSTANCE=new Glide();
 public static Glide getInstance(){return INSTANCE;}
 public me.eldodebug.soar.management.mods.ModManager getModManager(){return me.eldodebug.soar.management.mods.ModManager.INSTANCE;}
 public Renderer getNanoVGManager(){return new Renderer();}
 public static class Renderer {public void setupAndDraw(Runnable r){r.run();}}
}''',
'me/eldodebug/soar/management/language/TranslateText.java':'''package me.eldodebug.soar.management.language;public enum TranslateText{EDIT_HUD}''',
'me/eldodebug/soar/management/mods/ModManager.java':'''package me.eldodebug.soar.management.mods;
public class ModManager{public static final ModManager INSTANCE=new ModManager();
 public final java.util.List<me.eldodebug.soar.management.mods.settings.Setting> settings=new java.util.ArrayList<me.eldodebug.soar.management.mods.settings.Setting>();
 public void addSettings(me.eldodebug.soar.management.mods.settings.Setting...s){for(me.eldodebug.soar.management.mods.settings.Setting e:s)settings.add(e);}}''',
'me/eldodebug/soar/management/mods/Mod.java':'''package me.eldodebug.soar.management.mods;
public class Mod{private boolean enabled;public String getName(){return "";}public String getNameKey(){return "";}public String getDescription(){return "";}
 public boolean isToggled(){return enabled;}public void setToggled(boolean value){enabled=value;if(value)onEnable();else onDisable();}
 public void onEnable(){}public void onDisable(){}}''',
'me/eldodebug/soar/management/mods/HUDMod.java':'''package me.eldodebug.soar.management.mods;
import me.eldodebug.soar.management.language.TranslateText;
public class HUDMod extends Mod{public static int saves,restores,measurements,draws;public static boolean fail;public static Object font=new Object();
 private int x=10,y=20,width,height;private float scale=1;
 public HUDMod(TranslateText a,TranslateText b){}public boolean isEditing(){return false;}
 public int getX(){return x;}public int getY(){return y;}public int getWidth(){return width;}public int getHeight(){return height;}
 public void setX(int v){x=v;}public void setY(int v){y=v;}public void setWidth(int v){width=v;}public void setHeight(int v){height=v;}
 public void setScale(float v){scale=v;}public float getScale(){return scale;}public Object getHudFont(int i){return font;}
 public float getTextWidth(String s,float z,Object f){measurements++;return s.length()*5;}
 public void save(){saves++;}public void restore(){restores++;}
 public void drawBackground(float a,float b,float c,float d){}
 public void drawText(String s,float a,float b,float c,Object f){draws++;if(fail)throw new IllegalStateException("renderer fault");}}''',
'me/eldodebug/soar/management/mods/settings/Setting.java':'''package me.eldodebug.soar.management.mods.settings;
import me.eldodebug.soar.management.language.TranslateText;import me.eldodebug.soar.management.mods.Mod;
public class Setting{public Setting(TranslateText t,Mod m){}public String getName(){return "";}public String getNameKey(){return "";}}''',
'me/eldodebug/soar/management/mods/settings/impl/BooleanSetting.java':'''package me.eldodebug.soar.management.mods.settings.impl;
import me.eldodebug.soar.management.language.TranslateText;import me.eldodebug.soar.management.mods.Mod;import me.eldodebug.soar.management.mods.settings.Setting;
public class BooleanSetting extends Setting{private boolean value;
 public BooleanSetting(TranslateText t,Mod m,boolean v){super(t,m);value=v;me.eldodebug.soar.Glide.getInstance().getModManager().addSettings(this);}
 public boolean isToggled(){return value;}public void setToggled(boolean v){value=v;}}''',
'me/eldodebug/soar/management/mods/settings/impl/NumberSetting.java':'''package me.eldodebug.soar.management.mods.settings.impl;
import me.eldodebug.soar.management.language.TranslateText;import me.eldodebug.soar.management.mods.Mod;import me.eldodebug.soar.management.mods.settings.Setting;
public class NumberSetting extends Setting{private double value;
 public NumberSetting(TranslateText t,Mod m,double v,double a,double b,boolean i){super(t,m);value=v;me.eldodebug.soar.Glide.getInstance().getModManager().addSettings(this);}
 public long getValueLong(){return (long)value;}public void setValue(double v){value=v;}}''',
'me/eldodebug/soar/management/mods/settings/impl/TextSetting.java':'''package me.eldodebug.soar.management.mods.settings.impl;
import me.eldodebug.soar.management.language.TranslateText;import me.eldodebug.soar.management.mods.Mod;import me.eldodebug.soar.management.mods.settings.Setting;
public class TextSetting extends Setting{private String value;
 public TextSetting(TranslateText t,Mod m,String v){super(t,m);value=v;me.eldodebug.soar.Glide.getInstance().getModManager().addSettings(this);}
 public String getText(){return value;}public void setText(String v){value=v;}}''',
'me/eldodebug/soar/management/event/EventTarget.java':'''package me.eldodebug.soar.management.event;public @interface EventTarget{}''',
'me/eldodebug/soar/management/event/impl/EventTick.java':'''package me.eldodebug.soar.management.event.impl;public class EventTick{}''',
'me/eldodebug/soar/management/event/impl/EventRender2D.java':'''package me.eldodebug.soar.management.event.impl;public class EventRender2D{}''',
'GuiContractHarness.java':'''import me.eldodebug.soar.combatiq.*;
import me.eldodebug.soar.management.mods.*;import me.eldodebug.soar.management.mods.settings.*;
import me.eldodebug.soar.management.mods.settings.impl.*;import me.eldodebug.soar.management.event.impl.*;
public class GuiContractHarness{
 static Setting setting(String key){for(Setting s:ModManager.INSTANCE.settings)if(s.getNameKey().equals(key))return s;throw new AssertionError(key);}
 static void bool(String key,boolean v){((BooleanSetting)setting(key)).setToggled(v);}
 public static void main(String[] args)throws Exception{
  CombatIQMod mod=new CombatIQMod();if(mod.isToggled()||CombatIQNative.enabled())throw new AssertionError("default off");
  java.util.Set<String> keys=new java.util.HashSet<String>();for(Setting s:ModManager.INSTANCE.settings)if(!keys.add(s.getNameKey()))throw new AssertionError("duplicate key");
  if(keys.size()!=13)throw new AssertionError("missing native settings "+keys);
  mod.setX(41);mod.setY(52);mod.setScale(1.25f);mod.setToggled(true);mod.onTick(new EventTick());
  if(CombatIQNative.enabled())throw new AssertionError("idle module recording");
  bool("combatiq.recording",true);mod.onTick(new EventTick());CombatIQNative.onClientTick();
  if(!CombatIQNative.enabled())throw new AssertionError("recording control");
  mod.onRender2D(new EventRender2D());int measurements=HUDMod.measurements;
  mod.onRender2D(new EventRender2D());if(HUDMod.measurements!=measurements)throw new AssertionError("uncached layout");
  HUDMod.font=new Object();mod.onRender2D(new EventRender2D());if(HUDMod.measurements==measurements)throw new AssertionError("theme/font refresh");
  int compact=mod.getHeight();bool("combatiq.expanded",true);mod.onRender2D(new EventRender2D());if(mod.getHeight()<=compact)throw new AssertionError("expanded view");
  if(mod.getX()!=41||mod.getY()!=52||mod.getScale()!=1.25f)throw new AssertionError("widget state changed");
  java.lang.reflect.Field f=CombatIQNative.class.getDeclaredField("GATE");f.setAccessible(true);
  synchronized(f.get(null)){
   Thread render=new Thread(new Runnable(){public void run(){mod.onRender2D(new EventRender2D());}});render.start();render.join(500);
   if(render.isAlive())throw new AssertionError("render blocks on recorder gate");
  }
  bool("combatiq.export",true);mod.onTick(new EventTick());if(((BooleanSetting)setting("combatiq.export")).isToggled())throw new AssertionError("export latch");
  HUDMod.fail=true;mod.onRender2D(new EventRender2D());int draws=HUDMod.draws;
  mod.onRender2D(new EventRender2D());if(HUDMod.draws!=draws)throw new AssertionError("render fault not isolated");
  if(HUDMod.saves!=HUDMod.restores)throw new AssertionError("graphics state unbalanced");
  mod.setToggled(false);if(CombatIQNative.enabled())throw new AssertionError("disable only hid HUD");
  System.out.println("PASS native adapter controls/render contract; no Minecraft/NanoVG/config runtime");
 }
}'''
}

class NativeGuiContract(unittest.TestCase):
    def test_controls_layout_snapshot_and_fault_isolation(self):
        java=shutil.which('java');javac=shutil.which('javac')
        if not java or not javac:self.skipTest('JDK required')
        with tempfile.TemporaryDirectory() as d:
            d=Path(d);classes=d/'classes';classes.mkdir();sources=[]
            for name,source in STUBS.items():
                p=d/'stubs'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source);sources.append(p)
            sources+=list((ROOT/'src/me/eldodebug/soar/combatiq').glob('*.java'))
            sources+=[p for p in (ROOT/'tests-v4').glob('*.java') if p.name not in ('IntegrationV3.java','StressV4.java','HarnessV4.java','BenchV4.java')]
            sources+=[ROOT/'native-module/me/eldodebug/soar/combatiq/CombatIQMod.java']
            version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True)
            flags=['-source','8','-target','8'] if version.startswith('javac 1.8') else ['--release','8']
            subprocess.run([javac,*flags,'-d',str(classes),*map(str,sources)],check=True,capture_output=True,text=True)
            p=subprocess.run([java,'-Xverify:all','-Dvelvet.combatiq.exportDir='+str(d/'exports'),
                              '-cp',str(classes),'GuiContractHarness'],check=True,capture_output=True,text=True,timeout=15)
            self.assertIn('PASS native adapter controls/render contract',p.stdout)
