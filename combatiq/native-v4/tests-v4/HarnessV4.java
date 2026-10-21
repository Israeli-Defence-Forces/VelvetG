import me.eldodebug.soar.combatiq.CombatIQNative;
public class HarnessV4 {
 public static void main(String[] args)throws Exception {
  CombatIQNative.onClientTick();
  org.lwjgl.input.Keyboard.key=17;org.lwjgl.input.Keyboard.press=true;
  CombatIQNative.onKeyboardDispatch();
  bev movement=new bev();
  CombatIQNative.onMovementSample(movement);
  org.lwjgl.input.Keyboard.press=false;CombatIQNative.onKeyboardDispatch();
  ave.SINGLETON.t.Y.state=false;movement.b=0;CombatIQNative.onMovementSample(movement);
  CombatIQNative.onPlayerPoint(ave.SINGLETON.h,1);
  ave.SINGLETON.h.sprint=false;
  CombatIQNative.onPlayerPoint(ave.SINGLETON.h,2);
  CombatIQNative.onSendAttempt(new is(is$a.STOP_SPRINTING));
  org.lwjgl.input.Keyboard.press=true;CombatIQNative.onKeyboardDispatch();
  CombatIQNative.onPlayerPoint(ave.SINGLETON.h,1);ave.SINGLETON.h.sprint=true;CombatIQNative.onPlayerPoint(ave.SINGLETON.h,2);
  CombatIQNative.onSendAttempt(new is(is$a.START_SPRINTING));
  CombatIQNative.onAttackIntent();CombatIQNative.onSendAttempt(new in(in$a.ATTACK));
  CombatIQNative.onSendAttempt(new in(in$a.INTERACT));
  CombatIQNative.onSendAttempt(new Object());
  Thread network=new Thread(new Runnable(){public void run(){
   CombatIQNative.onInboundBeforeFilter(new hm());CombatIQNative.onInboundBeforeFilter(new hf());
  }});network.start();network.join();
  CombatIQNative.onHandlerReturn(new hf());
  ave.SINGLETON.m=new Object();CombatIQNative.onContextPoint();
  org.lwjgl.input.Keyboard.key=33;CombatIQNative.onKeyboardDispatch();
  System.out.println("PASS: native hook harness finished; JVM shutdown drains file. No Minecraft/Mixin application.");
 }
}
