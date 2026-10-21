import me.eldodebug.soar.knockback.*;
import java.util.Arrays;import java.lang.management.*;
public class BenchTrainer {
 static long bytes(){try{Object b=ManagementFactory.getThreadMXBean();Class<?> c=Class.forName("com.sun.management.ThreadMXBean");c.getMethod("setThreadAllocatedMemoryEnabled",boolean.class).invoke(b,true);return ((Long)c.getMethod("getThreadAllocatedBytes",long.class).invoke(b,Thread.currentThread().getId())).longValue();}catch(Exception unavailable){return -1;}}
 static long gc(){long total=0;for(GarbageCollectorMXBean b:ManagementFactory.getGarbageCollectorMXBeans())if(b.getCollectionCount()>0)total+=b.getCollectionCount();return total;}
 static volatile Object sink;
 static void batch(String mode,int i){
  if(mode.equals("disabled")||mode.equals("native-bridge")){TrainerRuntime.tickStart();TrainerRuntime.tickEnd();}
  else{long now=(i+1L)*50000000L;double d=Math.max(0,Math.sin(i*.3));engine.tick(i,now,true,d==0,d,Math.cos(i*.3)*.2,0);engine.sample(d==0,d,Math.cos(i*.3)*.2);if(i%7==0)engine.attack(now);if(i%20==0){engine.feedback(true,now);engine.feedback(false,now+1);}if(i%19==0)engine.jump(now+1);sink=engine.snapshot(now);}
 }
 static TrainerEngine engine=new TrainerEngine();
 public static void main(String[] args){String mode=args[0];ave.A();TrainerRuntime.configure(mode.equals("native-bridge"),0,5,6,false,false,0,"note.pling");engine.configure(0,5,30);
  for(int i=0;i<1000;i++)batch(mode,i);int n=5000;long[] times=new long[n];long g=gc(),allocated=bytes(),sum=0;
  for(int i=0;i<n;i++){long t=System.nanoTime();batch(mode,1000+i);times[i]=System.nanoTime()-t;sum+=times[i];}
  allocated=bytes()-allocated;Arrays.sort(times);System.out.println("{\"mode\":\""+mode+"\",\"timed_batches\":"+n+",\"mean_ns\":"+(sum/(double)n)+",\"p95_ns\":"+times[(int)(n*.95)]+",\"p99_ns\":"+times[(int)(n*.99)]+",\"producer_bytes_per_batch\":"+(allocated/(double)n)+",\"gc_collections\":"+(gc()-g)+"}");}
}
