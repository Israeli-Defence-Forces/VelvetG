import me.eldodebug.soar.combatiq.CombatIQNative;
import java.lang.reflect.Field;
public class StressV4 {
 public static void main(String[] args) throws Exception {
  CombatIQNative.onClientTick();
  int count=Integer.parseInt(args[1]);
  if (args[0].equals("lifecycle")) {
   CombatIQNative.setEnabled(true);CombatIQNative.setEnabled(true);
   CombatIQNative.setEnabled(false);
   java.lang.reflect.Field f=CombatIQNative.class.getDeclaredField("starting");f.setAccessible(true);
   java.util.concurrent.atomic.AtomicBoolean live=(java.util.concurrent.atomic.AtomicBoolean)f.get(null);
   long deadline=System.nanoTime()+2000000000L;
   while(live.get() && System.nanoTime()<deadline) Thread.sleep(5);
   if(live.get())throw new AssertionError("writer failed to drain");
   CombatIQNative.setEnabled(true);CombatIQNative.onClientTick();
   CombatIQNative.onAttackIntent();
   CombatIQNative.onClientTick();
   if(!CombatIQNative.snapshot().sprint.startsWith("Sprint"))throw new AssertionError("snapshot");
   CombatIQNative.setEnabled(false);
  } else if (args[0].equals("remap")) {
   ave.SINGLETON.t.Y.code=33;
   org.lwjgl.input.Keyboard.key=17;CombatIQNative.onKeyboardDispatch();
   org.lwjgl.input.Keyboard.key=33;org.lwjgl.input.Keyboard.press=true;CombatIQNative.onKeyboardDispatch();
   org.lwjgl.input.Keyboard.press=false;CombatIQNative.onKeyboardDispatch();
   ave.SINGLETON.t.Y.state=false;CombatIQNative.onMovementSample(new bev());
   org.lwjgl.input.Keyboard.press=true;CombatIQNative.onKeyboardDispatch();
  } else {
   Runnable producer=new Runnable(){public void run(){
    for(int i=0;i<count;i++)CombatIQNative.onSendAttempt(new in(in$a.ATTACK));
   }};
   if(args[0].equals("overflow")) {
    Field f=CombatIQNative.class.getDeclaredField("GATE");f.setAccessible(true);
    synchronized(f.get(null)){producer.run();}
   } else {
    Thread a=new Thread(producer),b=new Thread(producer);
    a.start();b.start();a.join();b.join();
   }
  }
  Thread.sleep(80);
  System.out.println(CombatIQNative.status());
 }
}
