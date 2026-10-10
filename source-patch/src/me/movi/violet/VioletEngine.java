package me.movi.violet;
import java.util.function.DoubleSupplier;
/** Deadline scheduling with manual priority and a bounded tick budget. */
public final class VioletEngine {
 public static final int MAX_CPS=60,MAX_PER_TICK=4;
 private final DoubleSupplier random;private final Hand[] hands={new Hand(),new Hand()};
 private static final class Hand {boolean active;long started,next;final ClickWindow manual=new ClickWindow(),generated=new ClickWindow();}
 /** Fixed storage for a five-second window; no timestamp boxing on input/render paths. */
 private static final class ClickWindow {
  private final long[] times=new long[512];private int first,size;
  void add(long now){prune(now);if(size==times.length){first=(first+1)%times.length;size--;}times[(first+size)%times.length]=now;size++;}
  void prune(long now){while(size>0&&now-times[first]>=5000000000L){first=(first+1)%times.length;size--;}}
  int count(long now,long window){prune(now);int n=0;for(int i=size-1;i>=0;i--){if(now-times[(first+i)%times.length]>=window)break;n++;}return n;}
  void clear(){first=size=0;}
 }
 public VioletEngine(DoubleSupplier random){this.random=random;}
 public int poll(int hand,long now,boolean allowed,boolean manualPending,double min,double max,int delay,int ramp,double variation){
  Hand h=hands[hand];min=clamp(min,1,MAX_CPS);max=clamp(max,min,MAX_CPS);
  if(!allowed){h.active=false;return 0;}
  if(!h.active){h.active=true;h.started=now;h.next=now+Math.max(0,delay)*1000000L;}
  double progress=ramp<=0?1:clamp((now-h.started-delay*1000000L)/(ramp*1000000.0),0,1);
  if(manualPending){h.next=Math.max(h.next,now+interval(min,max,variation,progress));return 0;}
  if(now<h.next)return 0;
  // Drop a stalled backlog; never repay lag with a burst of old actions.
  if(now-h.next>100000000L)h.next=now;
  int count=0;while(now>=h.next&&count<MAX_PER_TICK){count++;h.next+=interval(min,max,variation,progress);}
  if(now>=h.next)h.next=now+interval(min,max,variation,progress);
  return count;
 }
 private long interval(double min,double max,double variation,double ramp){double target=min+(max-min)*clamp(random.getAsDouble(),0,1);target=min+(target-min)*ramp;double spread=clamp(variation,0,25)/100;double factor=1-spread+2*spread*clamp(random.getAsDouble(),0,1);return Math.max(1000000000L/MAX_CPS,(long)(1000000000.0/target*factor));}
 public void physical(int hand,long now){hands[hand].manual.add(now);}
 public void generated(int hand,long now,int count){for(int i=0;i<Math.min(512,Math.max(0,count));i++)hands[hand].generated.add(now);}
 public int physicalCps(int hand,long now){return hands[hand].manual.count(now,1000000000L);}
 public int generatedCps(int hand,long now){return hands[hand].generated.count(now,1000000000L);}
 /** Fixed five-second rolling average, including quiet seconds and both hands. */
 public double averageCps(long now){int total=0;for(Hand h:hands)total+=h.manual.count(now,5000000000L)+h.generated.count(now,5000000000L);return total/5.0;}
 public boolean active(int hand){return hands[hand].active;}
 public void cancel(){for(Hand h:hands)h.active=false;}
 public void clear(){cancel();for(Hand h:hands){h.manual.clear();h.generated.clear();}}
 public static double clamp(double v,double min,double max){return Double.isFinite(v)?Math.max(min,Math.min(max,v)):min;}
}
