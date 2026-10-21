import me.eldodebug.soar.knockback.*;
public class RuntimeHarness {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why+" fault="+TrainerRuntime.fault());}
 static void tick(){TrainerRuntime.tickStart();TrainerRuntime.tickEnd();}
 public static void main(String[] args)throws Exception {
  ave.A();TrainerRuntime.configure(true,0,5,6,true,true,.4f,"note.pling");
  Thread foreign=new Thread(new Runnable(){public void run(){TrainerRuntime.tickStart();}});foreign.start();foreign.join();
  tick();check(TrainerRuntime.fault().isEmpty(),"native binding initialization");
  check(TrainerRuntime.snapshot().distance.length==1&&TrainerRuntime.snapshot().distance[0]==0,"real collision bridge");
  TrainerRuntime.feedback(ave.SINGLE.handler,new hf());TrainerRuntime.feedback(ave.SINGLE.handler,new hm());
  org.lwjgl.input.Keyboard.key=57;org.lwjgl.input.Keyboard.press=true;TrainerRuntime.keyboard();TrainerRuntime.keyboard();
  check(TrainerRuntime.snapshot().perfect==1,"manual jump edge after real packet bridge");
  TrainerRuntime.tickEnd();check(ave.SINGLE.h.sounds==1&&ave.SINGLE.h.volume==.4f,"native sound and volume");
  for(int i=0;i<4;i++){TrainerRuntime.tickEnd();}check(ave.SINGLE.h.sounds==1,"sound bounded / rate limited");
  ave.SINGLE.h.C=false;ave.SINGLE.h.w=-.2;ave.SINGLE.h.bb.b=1.5;tick();
  TrainerRuntime.outgoing(ave.SINGLE.handler.network,new in());TrainerRuntime.tickEnd();
  check(TrainerRuntime.snapshot().falling==1&&TrainerRuntime.snapshot().attacks==1,"falling C02 send attempt");
  check(ave.SINGLE.h.sounds==1,"five-tick audio rate limit");
  TrainerRuntime.outgoing(new Object(),new in());TrainerRuntime.tickEnd();check(TrainerRuntime.snapshot().attacks==1,"foreign manager rejected");
  ave.SINGLE.m=new Object();org.lwjgl.input.Keyboard.press=false;TrainerRuntime.keyboard();org.lwjgl.input.Keyboard.press=true;TrainerRuntime.keyboard();
  check(TrainerRuntime.snapshot().attempts==1,"GUI key ignored");
  ave.SINGLE.m=null;tick();ave.SINGLE.t.ac.code=33;ave.SINGLE.t.ac.down=false;tick();org.lwjgl.input.Keyboard.key=57;TrainerRuntime.keyboard();
  int old=TrainerRuntime.snapshot().attempts;check(old==1,"old key after remap ignored");
  ave.SINGLE.h.wet=true;tick();TrainerRuntime.outgoing(ave.SINGLE.handler.network,new in());TrainerRuntime.tickEnd();check(TrainerRuntime.snapshot().unscored==1,"water excludes falling score");ave.SINGLE.h.wet=false;
  TrainerRuntime.reset();check(TrainerRuntime.snapshot().attempts==0&&TrainerRuntime.snapshot().attacks==0,"reset control");
  ave.SINGLE.f=new adm();tick();check(TrainerRuntime.snapshot().distance.length==1,"world graph cleared");
  TrainerRuntime.configure(false,0,5,6,false,false,0,"note.pling");int count=TrainerRuntime.snapshot().distance.length;tick();check(TrainerRuntime.snapshot().distance.length==count,"off fast path");
  check(!TrainerRuntime.validSound("../../private")&&TrainerRuntime.validSound("random.orb"),"sound name validation");
  check(TrainerRuntime.fault().isEmpty(),"no runtime bridge error");
  System.out.println("PASS: "+checks+" native bridge assertions; fake world, no game launch or CSV");
 }
}
