import me.eldodebug.soar.combatiq.CombatIQNative;
import java.util.Arrays;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
/** Synthetic callback batches, NOT Minecraft frames or FPS. */
public class BenchV4 {
 static volatile int sink;
 static bev movement=new bev();
 static in attack=new in(in$a.ATTACK);
 static void batch(String mode) {
  CombatIQNative.onClientTick();
  CombatIQNative.onMovementSample(movement);
  CombatIQNative.onSendAttempt(attack);
  if(mode.equals("snapshot")) {
   CombatIQNative.Snapshot s=CombatIQNative.snapshot();
   sink=s.status.length()+s.sprint.length()+s.action.length()+s.health.length();
  }
  if(mode.equals("packet-burst"))for(int j=0;j<10;j++)CombatIQNative.onSendAttempt(attack);
 }
 static long gc(){long total=0;for(GarbageCollectorMXBean g:ManagementFactory.getGarbageCollectorMXBeans())if(g.getCollectionCount()>0)total+=g.getCollectionCount();return total;}
 public static void main(String[] args) {
  String mode=args[0];int n=Integer.parseInt(args[1]);
  for(int i=0;i<1000;i++)batch(mode);
  long beforeGC=gc();long[] times=new long[n];long sum=0;
  for(int i=0;i<n;i++){long t=System.nanoTime();batch(mode);long elapsed=System.nanoTime()-t;times[i]=elapsed;sum+=elapsed;}
  Arrays.sort(times);
  System.out.println("{\"mean_batch_ns\":"+(sum/(double)n)+",\"median_batch_ns\":"+times[n/2]+",\"p95_batch_ns\":"+times[(int)(n*.95)]+",\"p99_batch_ns\":"+times[(int)(n*.99)]+",\"gc_collections\":"+(gc()-beforeGC)+",\"batches\":"+n+",\"warmup_batches\":1000}");
 }
}
