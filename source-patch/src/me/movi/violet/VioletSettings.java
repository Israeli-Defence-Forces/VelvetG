package me.movi.violet;
import java.util.*;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.management.mods.settings.impl.*;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.language.TranslateText;
/** Native persisted values, with a custom Violet UI independent of native widgets. */
public final class VioletSettings {
 private static final String[] PRESET_NAMES={"Balanced","Fast","Build"};
 public final List<Row> rows=new ArrayList<>();private final Map<String,Row> byId=new LinkedHashMap<>();
 public VioletSettings(Mod mod,Mod hud){
  flag(mod,"left","Left clicking","Generate attack clicks.",0,true);
  flag(mod,"right","Right clicking","Generate use clicks independently.",0,false);
  choice(mod,"mode","Activation","Use each click button, or a separate activation bind.",0,0,"Hold click","Hold activation","Toggle activation");
  bind(mod,"activation","Activation bind","Used by the hold/toggle activation modes.",0,-98);
  bind(mod,"toggle","Enable shortcut","Toggle Violet on or off; starts unbound.",0,0);
  bind(mod,"settings","Settings shortcut","Open this screen while playing. Fresh installs start unbound to avoid stealing an existing keybind.",0,0);
  range(mod,"leftMin","Left minimum CPS","Lower end of the attack click rate.",1,10,1,60,true);
  range(mod,"leftMax","Left maximum CPS","Upper end of the attack click rate.",1,14,1,60,true);
  range(mod,"rightMin","Right minimum CPS","Lower end of the use click rate.",1,8,1,60,true);
  range(mod,"rightMax","Right maximum CPS","Upper end of the use click rate.",1,12,1,60,true);
  flag(mod,"jitter","Vary intervals","Add configurable variation to generated intervals.",1,true);
  range(mod,"variation","Variation (%)","Interval variation strength; zero uses a steady interval.",1,6,0,25,true);
  range(mod,"delay","Start delay (ms)","Wait before the first generated click.",1,0,0,500,true);
  range(mod,"ramp","Ramp (ms)","Ease from minimum to the selected rate; zero disables.",1,350,0,1500,true);
  flag(mod,"weapon","Left: weapon only","Allow swords, axes and sticks.",2,true);
  flag(mod,"target","Left: players only","Require a player under the crosshair.",2,false);
  flag(mod,"mining","Preserve mining","Let vanilla handle a block under the crosshair.",2,true);
  flag(mod,"blocks","Right: blocks only","Require a block item in hand.",2,true);
  flag(mod,"creative","Pause in creative","Disable generated clicks in creative mode.",2,true);
  flag(mod,"using","Pause during item use","Do not interrupt bows, food or continuous item use.",2,true);
  flag(hud,"menus","Hide HUD in menus","Keep menus clean; the HUD remains visible in Edit HUD.",4,true);
  flag(mod,"idle","Hide idle HUD","Hide the HUD when both recent counts are zero.",3,false);
  flag(mod,"graph","CPS history","Show a small live total-CPS trace.",3,true);
  choice(mod,"layout","HUD layout","Compact pill or detailed two-hand card.",3,0,"Compact","Detailed","Advanced");
  range(mod,"opacity","HUD opacity (%)","Background opacity for the Violet HUD.",3,90,20,100,true);
  choice(mod,"accent","Accent","The shared Violet screen and HUD accent.",3,0,"Lavender","Plum","Rose");
  flag(hud,"average","Five-second average","Show combined CPS averaged over a fixed five-second window.",3,false);
  flag(mod,"diagnostics","Input diagnostics","Keep bounded timing and input-origin events in memory; no continuous disk logging.",2,false);
  range(mod,"guiScale","Settings scale (%)","Screen scale also fits the available display.",3,80,40,120,true);
 }
 private void put(String id,String name,String help,int page,Setting s,String[] choices){Row r=new Row(id,name,help,page,s,choices);rows.add(r);byId.put(id,r);}
 private void flag(Mod p,String id,String label,String help,int page,boolean value){put(id,label,help,page,new Flag(p,id,label,value),null);}
 private void range(Mod p,String id,String label,String help,int page,double value,double min,double max,boolean integer){put(id,label,help,page,new Range(p,id,label,value,min,max,integer),null);}
 private void bind(Mod p,String id,String label,String help,int page,int key){put(id,label,help,page,new Bind(p,id,label,key),null);}
 private void choice(Mod p,String id,String label,String help,int page,int selected,String... names){put(id,label,help,page,new Pick(p,id,label,selected,names),names);}
 public Row row(String id){return byId.get(id);}public boolean flag(String id){return ((BooleanSetting)row(id).setting).isToggled();}
 public double number(String id){NumberSetting s=(NumberSetting)row(id).setting;double v=s.getValue();return Double.isFinite(v)?VioletEngine.clamp(v,s.getMinValue(),s.getMaxValue()):s.getDefaultValue();}
 public int key(String id){int v=((KeybindSetting)row(id).setting).getKeyCode();return v>=-100&&v<256?v:0;}
 public int choice(String id){return ((Pick)row(id).setting).index();}
 public void value(String id,double v){NumberSetting s=(NumberSetting)row(id).setting;s.setValue(VioletEngine.clamp(v,s.getMinValue(),s.getMaxValue()));if(id.equals("leftMin")&&number(id)>number("leftMax"))value("leftMax",number(id));if(id.equals("leftMax")&&number(id)<number("leftMin"))value("leftMin",number(id));if(id.equals("rightMin")&&number(id)>number("rightMax"))value("rightMax",number(id));if(id.equals("rightMax")&&number(id)<number("rightMin"))value("rightMin",number(id));VioletRuntime.reset();}
 public void reset(){for(Row r:rows)r.setting.reset();VioletRuntime.reset();}
 public void preset(int i){value("delay",0);value("ramp",350);value("variation",6);((BooleanSetting)row("jitter").setting).setToggled(true);value("leftMin",i==1?16:10);value("leftMax",i==1?20:14);value("rightMin",i==2?12:8);value("rightMax",i==2?16:12);((BooleanSetting)row("left").setting).setToggled(i!=2);((BooleanSetting)row("right").setting).setToggled(i==2);((BooleanSetting)row("weapon").setting).setToggled(i!=2);VioletRuntime.reset();}
 public boolean available(Row r){if(r.id.equals("variation"))return flag("jitter")&&(flag("left")||flag("right"));if(r.id.equals("graph"))return choice("layout")!=0;if(r.id.equals("average"))return choice("layout")==2;if(r.id.equals("activation"))return choice("mode")!=0;if(r.id.startsWith("left")&&!r.id.equals("left")||r.id.equals("weapon")||r.id.equals("target")||r.id.equals("mining"))return flag("left");if(r.id.startsWith("right")&&!r.id.equals("right")||r.id.equals("blocks"))return flag("right");if(r.id.equals("delay")||r.id.equals("ramp")||r.id.equals("jitter"))return flag("left")||flag("right");return true;}
 public String unavailableReason(Row r){if(r.id.equals("average"))return "Choose Advanced layout to show the five-second average.";if(r.id.equals("variation"))return "Enable Vary intervals and a click hand to set the strength.";if(r.id.equals("graph"))return "Choose Detailed layout to show the CPS history.";return r.id.equals("activation")?"Used by Hold activation or Toggle activation; Hold click uses the click buttons.":r.id.startsWith("right")||r.id.equals("blocks")?"Enable Right clicking on the Clicking tab to edit this control.":"Enable the corresponding hand on the Clicking tab to edit this control.";}
 public String presetName(){for(int i=0;i<3;i++)if(flag("left")== (i!=2)&&flag("right")== (i==2)&&flag("weapon")== (i!=2)&&number("leftMin")== (i==1?16:10)&&number("leftMax")== (i==1?20:14)&&number("rightMin")== (i==2?12:8)&&number("rightMax")== (i==2?16:12)&&number("delay")==0&&number("ramp")==350&&flag("jitter")&&number("variation")==6)return PRESET_NAMES[i];return "Custom";}
 public int runtimeHash(){int h=17;h=31*h+choice("mode");h=31*h+key("activation");h=31*h+key("toggle");h=31*h+key("settings");h=31*h+(flag("left")?1:0);h=31*h+(flag("right")?1:0);return h;}
 public static final class Row {public final String id,name,help;public final int page;public final Setting setting;public final String[] choices;Row(String i,String n,String h,int p,Setting s,String[] c){id=i;name=n;help=h;page=p;setting=s;choices=c;}}
 public static final class Text extends TextSetting {private final String id,label;Text(Mod p,String i,String l,String value){super(TranslateText.CPS_DISPLAY,p,value);id=i;label=l;}public String getName(){return label;}public String getNameKey(){return "text.violet."+id;}}
 public static final class Flag extends BooleanSetting {private final String id,label;public Flag(Mod p,String i,String l,boolean v){super(TranslateText.CPS_DISPLAY,p,v);id=i;label=l;}public String getName(){return label;}public String getNameKey(){return "text.violet."+id;}}
 public static final class Range extends NumberSetting {private final String id,label;public Range(Mod p,String i,String l,double v,double min,double max,boolean integer){super(TranslateText.CPS_DISPLAY,p,v,min,max,integer);id=i;label=l;}@Override public void setValue(double v){super.setValue(Double.isFinite(v)?VioletEngine.clamp(v,getMinValue(),getMaxValue()):getDefaultValue());}public String getName(){return label;}public String getNameKey(){return "text.violet."+id;}}
 public static final class Bind extends KeybindSetting {private final String id,label;Bind(Mod p,String i,String l,int v){super(TranslateText.CPS_DISPLAY,p,v);id=i;label=l;}public String getName(){return label;}public String getNameKey(){return "text.violet."+id;}}
 public static final class Pick extends ComboSetting {
  private final String id,label;private final int initial;
  Pick(Mod p,String i,String l,int selected,String[] names){super(TranslateText.CPS_DISPLAY,p,TranslateText.NORMAL,options(i,names));id=i;label=l;initial=selected;setOption(getOptions().get(selected));}
  static ArrayList<Option> options(String id,String[] names){ArrayList<Option> list=new ArrayList<>();for(int i=0;i<names.length;i++)list.add(new Item(id,i,names[i]));return list;}
  public String getName(){return label;}public String getNameKey(){return "text.violet."+id;}public int index(){return Math.max(0,getOptions().indexOf(getOption()));}
  public Option getDefaultOption(){return getOptions().get(initial);}public void reset(){setOption(getDefaultOption());}
  public Option getOptionByNameKey(String key){for(Option o:getOptions())if(o.getNameKey().equals(key))return o;return getDefaultOption();}
 }
 private static final class Item extends Option {private final String key,label;Item(String id,int i,String label){super(TranslateText.NORMAL);key="text.violet.option."+id+"."+i;this.label=label;}public String getName(){return label;}public String getNameKey(){return key;}}
}
