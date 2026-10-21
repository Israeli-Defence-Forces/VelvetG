import me.eldodebug.soar.combatiq.CombatIQNative;
public final class IntegrationV3 {
 public static void main(String[] args)throws Exception {
  ave.A();
  Thread foreign=new Thread(new Runnable(){public void run(){CombatIQNative.onClientTick();}},"Integrated Server");
  foreign.start();foreign.join();
  java.lang.reflect.Field owner=CombatIQNative.class.getDeclaredField("clientThread");owner.setAccessible(true);
  if(owner.get(null)!=null)throw new AssertionError("foreign first caller claimed client ownership");
  CombatIQNative.onClientTick();
  Object network=ave.SINGLETON.handler.manager;
  in attack=new in(in$a.ATTACK);
  CombatIQNative.onSendAttempt(attack);
  CombatIQNative.onNetworkPacket(network,attack,26);
  CombatIQNative.onNetworkPacket(network,attack,23);
  CombatIQNative.onNetworkPacket(network,attack,24);
  CombatIQNative.onNetworkPacket(new Object(),attack,26);
  CombatIQNative.onMouseEvent(0,true);CombatIQNative.onMouseEvent(1,true);CombatIQNative.onUseIntent();
  CombatIQNative.onSwing(ave.SINGLETON.h);
  ave.SINGLETON.h.swordBlock=true;ave.SINGLETON.h.C=false;
  ave.SINGLETON.h.v=.2;ave.SINGLETON.h.w=.4;ave.SINGLETON.h.s=1;
  ave.SINGLETON.f.target=new pk();ave.SINGLETON.f.target.s=3;
  CombatIQNative.onPlayerPoint(ave.SINGLETON.h,4);CombatIQNative.onPlayerPoint(ave.SINGLETON.h,3);
  CombatIQNative.onClientTick();
  hf hurt=new hf();
  CombatIQNative.onInboundBeforeFilter(hurt);
  CombatIQNative.onNetworkPacket(network,hurt,6);
  CombatIQNative.onHandlerReturn(hurt);
  CombatIQNative.onAttackIntent();CombatIQNative.onSendAttempt(attack);
  Thread.sleep(120);CombatIQNative.onClientTick();
  CombatIQNative.Snapshot snapshot=CombatIQNative.snapshot();
  try{snapshot.diagnostics.add("mutate");throw new AssertionError("mutable snapshot");}
  catch(UnsupportedOperationException expected){}
  if(!CombatIQNative.requestReportExport())throw new AssertionError("export not accepted");
  ave.SINGLETON.handler.manager=new Object();CombatIQNative.onContextPoint();
  CombatIQNative.onNetworkPacket(network,attack,26);
  CombatIQNative.onNetworkPacket(ave.SINGLETON.handler.manager,attack,26);
  CombatIQNative.setEnabled(false);
 }
}
