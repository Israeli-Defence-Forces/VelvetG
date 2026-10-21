package me.eldodebug.soar.knockback;
import java.lang.reflect.*;
import java.util.List;
/** Passive native bridge. Only the real Minecraft client thread owns mutable state. */
public final class TrainerRuntime {
 private TrainerRuntime(){}
 private static final Object[] NONE=new Object[0];private static final TrainerEngine ENGINE=new TrainerEngine();
 private static volatile boolean enabled;private static volatile String fault="";private static Thread owner;
 private static volatile TrainerEngine.Snapshot snapshot=ENGINE.snapshot(0);
 private static Class<?> mcType,entityType,boxType;private static Method instance,isClient,getHandler,getManager,getBox,collisions,keyCode,keyDown,entityId,playSound,inWater,inLava,burning,riding,eventKey,eventKeyState,eventButton,eventButtonState;
 private static Field player,world,screen,focus,settings,jumpBinding,onGround,motionY,fallDistance;
 private static Field[] bounds;private static Constructor<?> boxConstructor;private static Object previousWorld,previousPlayer,previousManager;
 private static long tick,lastSound=-100;private static int code=Integer.MIN_VALUE;private static boolean held,armed;
 private static boolean fallSound=true,timingSound=false;private static float volume=.5f;private static String sound="note.pling";
 private static Object local,level,manager;private static double ground=Double.NaN;private static boolean gameplay,environment;
 public static boolean enabled(){return enabled;}
 public static TrainerEngine.Snapshot snapshot(){return snapshot;}
 public static String fault(){return fault;}
 public static void configure(boolean on,int target,int window,int history,boolean fallAudio,boolean timingAudio,float audioVolume,String name){
  if(enabled!=on){enabled=on;if(!on){ENGINE.interrupt("Trainer disabled");gameplay=false;}else{fault="";armed=false;}}
  if(!on)return;
  ENGINE.configure(target,window,history);fallSound=fallAudio;timingSound=timingAudio;volume=Math.max(0,Math.min(1,audioVolume));
  sound=validSound(name)?name:"note.pling";
 }
 public static boolean validSound(String s){
  if(s==null||s.isEmpty()||s.length()>64||s.indexOf("..")>=0)return false;
  for(int i=0;i<s.length();i++){char c=s.charAt(i);if(!(c>='a'&&c<='z'||c>='0'&&c<='9'||c=='_'||c=='.'))return false;}return true;
 }
 public static void reset(){ENGINE.reset();snapshot=ENGINE.snapshot(System.nanoTime());}
 private static Field field(Class<?> type,String name)throws Exception {
  for(Class<?> t=type;t!=null;t=t.getSuperclass())try{Field f=t.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(name);
 }
 private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
 private static void init()throws Exception{
  if(mcType!=null)return;
  Class<?> mc=Class.forName("ave"),entity=Class.forName("pk"),aabb=Class.forName("aug");
  instance=method(mc,"A");isClient=method(mc,"aJ");getHandler=method(mc,"u");getManager=method(Class.forName("bcy"),"a");
  player=field(mc,"h");world=field(mc,"f");screen=field(mc,"m");focus=field(mc,"w");settings=field(mc,"t");
  jumpBinding=field(Class.forName("avh"),"ac");keyCode=method(Class.forName("avb"),"i");keyDown=method(Class.forName("avb"),"d");
  onGround=field(entity,"C");motionY=field(entity,"w");fallDistance=field(entity,"O");entityId=method(entity,"F");
  inWater=method(entity,"V");inLava=method(entity,"ab");burning=method(entity,"at");riding=method(entity,"au");
  getBox=method(entity,"aR");collisions=method(Class.forName("adm"),"a",aabb);playSound=method(entity,"a",String.class,float.class,float.class);
  bounds=new Field[6];for(int i=0;i<6;i++)bounds[i]=field(aabb,String.valueOf((char)('a'+i)));
  boxConstructor=aabb.getConstructor(double.class,double.class,double.class,double.class,double.class,double.class);boxConstructor.setAccessible(true);
  Class<?> keyboard=Class.forName("org.lwjgl.input.Keyboard"),mouse=Class.forName("org.lwjgl.input.Mouse");
  eventKey=keyboard.getMethod("getEventKey");eventKeyState=keyboard.getMethod("getEventKeyState");eventButton=mouse.getMethod("getEventButton");eventButtonState=mouse.getMethod("getEventButtonState");
  entityType=entity;boxType=aabb;mcType=mc;
 }
 private static boolean client()throws Exception{
  init();Object mc=instance.invoke(null,NONE);if(!((Boolean)isClient.invoke(mc,NONE)))return false;
  if(owner==null)owner=Thread.currentThread();return owner==Thread.currentThread();
 }
 public static void tickStart(){
  if(!enabled||!fault.isEmpty())return;
  try{
   if(!client())return;Object mc=instance.invoke(null,NONE);local=player.get(mc);level=world.get(mc);Object handler=getHandler.invoke(mc,NONE);
   manager=handler==null?null:getManager.invoke(handler,NONE);
   if(local!=previousPlayer||level!=previousWorld||manager!=previousManager){ENGINE.boundary("World/player/connection boundary");previousPlayer=local;previousWorld=level;previousManager=manager;armed=false;code=Integer.MIN_VALUE;}
   gameplay=local!=null&&level!=null&&screen.get(mc)==null&&focus.getBoolean(mc);
   if(gameplay)ground=groundDistance(local,level);else{ground=Double.NaN;armed=false;}
   environment=gameplay&&(((Boolean)inWater.invoke(local,NONE))||((Boolean)inLava.invoke(local,NONE))||((Boolean)burning.invoke(local,NONE))||((Boolean)riding.invoke(local,NONE)));
   long now=System.nanoTime();ENGINE.tick(++tick,now,gameplay,gameplay&&onGround.getBoolean(local),ground,gameplay?motionY.getDouble(local):Double.NaN,gameplay?Math.max(fallDistance.getFloat(local),environment?4:0):0);
   if(gameplay){Object key=jumpBinding.get(settings.get(mc));int current=((Integer)keyCode.invoke(key,NONE)).intValue();boolean down=((Boolean)keyDown.invoke(key,NONE)).booleanValue();
    if(current!=code){code=current;armed=!down;held=down;}
    edge(down,now);
   }
  }catch(Throwable failure){fail(failure);}
 }
 public static void tickEnd(){
  if(!enabled||!fault.isEmpty()||owner!=Thread.currentThread())return;
  try{
   if(gameplay){ground=groundDistance(local,level);ENGINE.sample(onGround.getBoolean(local),ground,motionY.getDouble(local));}
   int sounds=ENGINE.consumeSound();
   if(gameplay&&volume>0&&tick-lastSound>=5){boolean falling=fallSound&&(sounds&1)!=0,timing=timingSound&&(sounds&6)!=0;
    if(falling||timing){playSound.invoke(local,falling?sound:(sounds&2)!=0?"note.pling":"note.bass",volume,falling?1f:(sounds&2)!=0?1.35f:.8f);lastSound=tick;}}
   snapshot=ENGINE.snapshot(System.nanoTime());
  }catch(Throwable failure){fail(failure);}
 }
 /** Read-only collision boxes under the actual player footprint, including slabs/stairs. */
 private static double groundDistance(Object entity,Object world)throws Exception{
  Object bb=getBox.invoke(entity,NONE);double x0=bounds[0].getDouble(bb)+.001,y=bounds[1].getDouble(bb),z0=bounds[2].getDouble(bb)+.001,x1=bounds[3].getDouble(bb)-.001,z1=bounds[5].getDouble(bb)-.001;
  Object query=boxConstructor.newInstance(x0,y-16,z0,x1,y+.03125,z1);
  List<?> boxes=(List<?>)collisions.invoke(world,query);double top=Double.NEGATIVE_INFINITY;
  for(int i=0;i<boxes.size();i++){Object b=boxes.get(i);double t=bounds[4].getDouble(b);
   if(t<=y+.03125&&t>top&&bounds[3].getDouble(b)>x0&&bounds[0].getDouble(b)<x1&&bounds[5].getDouble(b)>z0&&bounds[2].getDouble(b)<z1)top=t;}
  double d=y-top;if(d>=-.03125)d=Math.max(0,d);return TrainerEngine.validDistance(d)?d:Double.NaN;
 }
 private static boolean currentGameplay()throws Exception{
  Object mc=instance.invoke(null,NONE);boolean ok=player.get(mc)==local&&world.get(mc)==level&&screen.get(mc)==null&&focus.getBoolean(mc);
  if(!ok){ENGINE.interrupt("GUI / player context interrupted");gameplay=false;armed=false;held=true;}
  return ok;
 }
 private static void edge(boolean down,long now){if(!down)armed=true;if(down&&!held&&armed){ENGINE.jump(now);snapshot=ENGINE.snapshot(now);}held=down;}
 public static void keyboard(){
  if(!enabled||!gameplay||!fault.isEmpty()||owner!=Thread.currentThread()||code==0)return;
  try{if(!currentGameplay())return;int event=((Integer)eventKey.invoke(null,NONE)).intValue();
   if(event==code)edge(((Boolean)eventKeyState.invoke(null,NONE)).booleanValue(),System.nanoTime());
  }catch(Throwable failure){fail(failure);}
 }
 public static void mouse(){
  if(!enabled||!gameplay||!fault.isEmpty()||owner!=Thread.currentThread()||code>=0)return;
  try{if(!currentGameplay())return;int button=((Integer)eventButton.invoke(null,NONE)).intValue();
   if(button>=0&&button-100==code)edge(((Boolean)eventButtonState.invoke(null,NONE)).booleanValue(),System.nanoTime());
  }catch(Throwable failure){fail(failure);}
 }
 private static final ClassValue<PacketFields> PACKETS=new ClassValue<PacketFields>(){protected PacketFields computeValue(Class<?> type){return new PacketFields(type);}};
 private static final class PacketFields{
  Field id,action,x,y,z;int family;
  PacketFields(Class<?> t){try{
   if(t.getName().equals("hf")){id=field(t,"a");action=field(t,"b");family=1;}
   else if(t.getName().equals("hm")){id=field(t,"a");x=field(t,"b");y=field(t,"c");z=field(t,"d");family=2;}
   else if(t.getName().equals("in")){id=field(t,"a");action=field(t,"b");family=3;}
  }catch(Exception bad){family=-1;}}
 }
 public static void feedback(Object handler,Object packet){
  if(!enabled||!gameplay||!fault.isEmpty()||owner!=Thread.currentThread())return;
  try{
   if(!currentGameplay())return;Object mc=instance.invoke(null,NONE);if(getHandler.invoke(mc,NONE)!=handler)return;
   PacketFields f=PACKETS.get(packet.getClass());if(f.id==null||f.id.getInt(packet)!=((Integer)entityId.invoke(local,NONE)).intValue())return;
   long now=System.nanoTime();if(f.family==1&&f.action.getByte(packet)==2)ENGINE.feedback(true,now);
   else if(f.family==2&&(Math.abs(f.x.getInt(packet))+Math.abs(f.y.getInt(packet))+Math.abs(f.z.getInt(packet))>80))ENGINE.feedback(false,now);
   snapshot=ENGINE.snapshot(now);
  }catch(Throwable failure){fail(failure);}
 }
 public static void outgoing(Object network,Object packet){
  if(!enabled||!gameplay||!fault.isEmpty()||owner!=Thread.currentThread()||network!=manager)return;
  try{if(!currentGameplay())return;PacketFields f=PACKETS.get(packet.getClass());if(f.family==3&&((Enum<?>)f.action.get(packet)).name().equals("ATTACK"))ENGINE.attack(System.nanoTime(),!environment);}
  catch(Throwable failure){fail(failure);}
 }
 public static void registrationFailure(Throwable failure){fault="Trainer registration failed: "+failure.getClass().getSimpleName();System.err.println(fault);}
 private static void fail(Throwable failure){fault="Trainer unavailable: "+failure.getClass().getSimpleName();if(owner==Thread.currentThread())ENGINE.interrupt(fault);}
}
