import me.eldodebug.soar.combatiq.CombatIQNative;
import java.util.Arrays;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.reflect.Method;
/** Fixed synthetic callback workload, NOT Minecraft frames or FPS. */
public class BenchV4 {
 static volatile int sink;
 static bev movement=new bev();
 static in attack=new in(in$a.ATTACK);
 static hz unrelated=new hz();
 static void batch(String mode) {
  CombatIQNative.onClientTick();
  CombatIQNative.onMovementSample(movement);
  CombatIQNative.onSendAttempt(attack);
  CombatIQNative.onInboundBeforeFilter(unrelated);
  if(mode.equals("enabled-idle")||mode.equals("compact-hud")||mode.equals("expanded-diagnostics")||mode.equals("deep-diagnostics")) {
   CombatIQNative.Snapshot s=CombatIQNative.snapshot();
   sink=s.status.length()+s.compactState.length()+s.action.length()+s.health.length();
   if(mode.equals("expanded-diagnostics")||mode.equals("deep-diagnostics"))for(int i=0;i<s.diagnostics.size();i++)sink+=s.diagnostics.get(i).length();
  }
 }
 static long gc(boolean time){long total=0;for(GarbageCollectorMXBean g:ManagementFactory.getGarbageCollectorMXBeans()){long n=time?g.getCollectionTime():g.getCollectionCount();if(n>0)total+=n;}return total;}
 static long allocated(){
  try {Object bean=ManagementFactory.getThreadMXBean();Class<?> api=Class.forName("com.sun.management.ThreadMXBean");
   if(!((Boolean)api.getMethod("isThreadAllocatedMemorySupported").invoke(bean)))return -1;
   api.getMethod("setThreadAllocatedMemoryEnabled",boolean.class).invoke(bean,true);
   return ((Long)api.getMethod("getThreadAllocatedBytes",long.class).invoke(bean,Thread.currentThread().getId())).longValue();
  }catch(Exception unavailable){return -1;}
 }
 public static void main(String[] args)throws Exception {
  String mode=args[0];int n=Integer.parseInt(args[1]);
  CombatIQNative.configure(300,33554432,mode.equals("deep-diagnostics")?2:1,15);
  for(int i=0;i<1000;i++)batch(mode);
  // The recorder is asynchronous. Give warmup output time to drain before timing.
  Thread.sleep(100);
  long[] times=new long[n];long sum=0,beforeGC=gc(false),beforeGCTime=gc(true),beforeAllocation=allocated();
  long loopStart=System.nanoTime();
  for(int i=0;i<n;i++){long t=System.nanoTime();batch(mode);long elapsed=System.nanoTime()-t;times[i]=elapsed;sum+=elapsed;}
  long loopElapsed=System.nanoTime()-loopStart,afterAllocation=allocated();
  long allocation=beforeAllocation<0||afterAllocation<0?-1:afterAllocation-beforeAllocation;
  Arrays.sort(times);
  System.out.println("{\"mean_batch_ns\":"+(sum/(double)n)+",\"median_batch_ns\":"+times[n/2]+",\"p95_batch_ns\":"+times[(int)(n*.95)]+",\"p99_batch_ns\":"+times[(int)(n*.99)]+",\"gc_collections\":"+(gc(false)-beforeGC)+",\"gc_time_ms\":"+(gc(true)-beforeGCTime)+",\"producer_allocated_bytes\":"+allocation+",\"timed_loop_ns\":"+loopElapsed+",\"batches\":"+n+",\"warmup_batches\":1000}");
  CombatIQNative.setEnabled(false);
 }
}
