package me.eldodebug.soar.knockback;
import java.util.Arrays;
/** Client-thread-only, bounded observational trainer. No Minecraft or file dependency. */
public final class TrainerEngine {
 public enum Grade {PERFECT,EARLY,LATE,MISSED,UNKNOWN}
 public static final int TAKEOFF=1,LANDING=2,ATTACK=4,FALLING=8,IMPACT=16,JUMP=32;
 public static final int CAPACITY=600;
 public static final class Result {
  public final Grade grade;public final double error;public final String reason;public final long ns;
  Result(Grade grade,double error,String reason,long ns){this.grade=grade;this.error=error;this.reason=reason;this.ns=ns;}
 }
 public static final class Snapshot {
  public final double[] distance,velocity;public final int[] markers;public final Result[] recent;
  public final Result result;public final long ns;public final int attempts,perfect,early,late,missed,unknown,streak,best,attacks,falling,unscored;
  public final double average;public final String summary,counts,streakText,attackText,notice;
  Snapshot(TrainerEngine e,long ns){
   this.ns=ns;result=e.last;int n=Math.min(e.count,e.historySeconds*20);distance=new double[n];velocity=new double[n];markers=new int[n];
   for(int i=0;i<n;i++){int at=(e.cursor-n+i+CAPACITY)%CAPACITY;distance[i]=e.distances[at];velocity[i]=e.velocities[at];markers[i]=e.marks[at];}
   int m=Math.min(e.resultCount,6);recent=new Result[m];for(int i=0;i<m;i++)recent[i]=e.results[(e.resultCursor-1-i+32)%32];
   perfect=e.perfect;early=e.early;late=e.late;missed=e.missed;unknown=e.unknown;streak=e.streak;best=e.best;attacks=e.attacks;falling=e.falling;unscored=e.unscored;
   attempts=perfect+early+late+missed;average=e.errorCount==0?Double.NaN:e.errorSum/e.errorCount;
   summary=lastText(result);counts="P "+perfect+"  E "+early+"  L "+late+"  M "+missed+"  ? "+unknown;
   streakText="Attempts "+attempts+" | streak "+streak+" | best "+best;
   attackText=e.attacks==0?"Falling attempts: -- (no known movement context)":"Falling attempts: "+Math.round(100.0*falling/attacks)+"% ("+falling+"/"+attacks+")";
   notice=e.notice;
  }
 }
 private final double[] distances=new double[CAPACITY],velocities=new double[CAPACITY];private final int[] marks=new int[CAPACITY];
 private final double[] jumpTimes=new double[32];private final long[] jumpIds=new long[32];private final boolean[] jumpEligible=new boolean[32];
 private final Result[] results=new Result[32];private int cursor,count,resultCursor,resultCount,jumpCursor,jumpCount,pendingMarks;
 private long edgeId,consumedId,tick,ns,tickStart,lastTickNs,fallTick=-1000,stallUntil=-1;private boolean active,grounded,sampleGrounded,contextKnown,stalled;
 private double distance=Double.NaN,vy=Double.NaN,pending=Double.NaN,lastHurt=Double.NaN,lastVelocity=Double.NaN,lastPair=-1000;
 private long pendingNs;private int targetTicks=0,windowTicks=5,historySeconds=6;
 private int perfect,early,late,missed,unknown,streak,best,attacks,falling,unscored,errorCount,soundFlags;
 private double errorSum;private Result last;private String notice="Enable trainer and enter a world";
 public void configure(int target,int window,int history){
  target=Math.max(0,Math.min(6,target));window=Math.max(0,Math.min(10,window));
  if(target!=targetTicks||window!=windowTicks){interrupt("Timing settings changed");targetTicks=target;windowTicks=window;}
  historySeconds=Math.max(2,Math.min(30,history));
 }
 public void reset(){perfect=early=late=missed=unknown=streak=best=attacks=falling=unscored=errorCount=0;errorSum=0;last=null;
  cursor=count=resultCursor=resultCount=0;lastPair=-1000;fallTick=-1000;Arrays.fill(results,null);clearPending();notice="Session reset; local observations only";}
 private void clearPending(){pending=lastHurt=lastVelocity=Double.NaN;jumpCount=jumpCursor=0;consumedId=edgeId;pendingMarks=0;soundFlags=0;}
 public void boundary(String reason){interrupt(reason);cursor=count=0;lastPair=-1000;fallTick=-1000;}
 public void interrupt(String reason){if(!Double.isNaN(pending))finish(Grade.UNKNOWN,Double.NaN,reason);clearPending();active=false;contextKnown=false;notice=reason;}
 public void tick(long tick,long nano,boolean gameplay,boolean onGround,double groundDistance,double motionY,float fallDistance){
  if(nano<ns){interrupt("Client clock discontinuity");return;}
  this.tick=tick;ns=nano;tickStart=nano;if(lastTickNs!=0&&nano-lastTickNs>100000000L)stallUntil=tick+3;stalled=tick<=stallUntil;lastTickNs=nano;
  if(!gameplay){if(active)interrupt("GUI, focus or world interruption");return;}
  active=true;grounded=onGround;distance=validDistance(groundDistance)?groundDistance:Double.NaN;vy=motionY;
  if(fallDistance>3)fallTick=tick;
  if(stalled&&!Double.isNaN(pending))finish(Grade.UNKNOWN,Double.NaN,"Client tick stalled; arrival timing uncertain");
  expire(time(nano));notice=stalled?"Client stall: timing uncertain":"Local arrival timing; server damage tick / KB outcome unknown";
 }
 public void sample(boolean onGround,double groundDistance,double motionY){
  if(!active)return;int marker=pendingMarks;pendingMarks=0;
  if(contextKnown&&sampleGrounded!=onGround)marker|=onGround?LANDING:TAKEOFF;
  grounded=onGround;sampleGrounded=onGround;contextKnown=true;distance=validDistance(groundDistance)?groundDistance:Double.NaN;vy=motionY;
  distances[cursor]=distance;velocities[cursor]=vy;marks[cursor]=marker;cursor=(cursor+1)%CAPACITY;count=Math.min(CAPACITY,count+1);
 }
 public static boolean validDistance(double d){return !Double.isNaN(d)&&!Double.isInfinite(d)&&d>=0&&d<=16.001;}
 public static double surfaceDistance(double feet,double minX,double maxX,double minZ,double maxZ,double[][] boxes){
  double best=Double.NEGATIVE_INFINITY;
  for(double[] b:boxes)if(b.length>=6&&b[3]>minX&&b[0]<maxX&&b[5]>minZ&&b[2]<maxZ&&b[4]<=feet+.03125&&b[4]>best)best=b[4];
  double d=feet-best;if(d>=-.03125)d=Math.max(0,d);return validDistance(d)?d:Double.NaN;
 }
 public double time(long nano){return tick+Math.max(0,Math.min(.999999,(nano-tickStart)/50000000.0));}
 public void jump(long nano){
  if(!active)return;ns=nano;double t=time(nano);int i=jumpCursor++&31;jumpTimes[i]=t;jumpIds[i]=++edgeId;
  jumpEligible[i]=grounded||validDistance(distance)&&distance<=.12;jumpCount=Math.min(32,jumpCount+1);pendingMarks|=JUMP;
  if(!Double.isNaN(pending)&&t>=pending-windowTicks&&t<=pending+targetTicks+windowTicks+.5){
   if(!jumpEligible[i])finish(Grade.UNKNOWN,Double.NaN,"Jump pressed while airborne");
   else {consumedId=jumpIds[i];score(t-pending-targetTicks);} }
 }
 public void feedback(boolean hurt,long nano){
  if(!active)return;ns=nano;double t=time(nano);if(hurt)lastHurt=t;else lastVelocity=t;
  if(!Double.isNaN(lastHurt)&&!Double.isNaN(lastVelocity)&&Math.abs(lastHurt-lastVelocity)<=2){
   double impact=Math.min(lastHurt,lastVelocity);lastHurt=lastVelocity=Double.NaN;
   if(impact-lastPair<2)return;lastPair=impact;impact(impact,nano);
  }
 }
 private void impact(double t,long nano){
  if(!Double.isNaN(pending))finish(Grade.UNKNOWN,Double.NaN,"Overlapping local impacts");
  pendingMarks|=IMPACT;pending=t;pendingNs=nano;
  if(stalled||nano-tickStart>100000000L){finish(Grade.UNKNOWN,Double.NaN,"Delayed client processing / arrival uncertainty");return;}
  if(tick-fallTick<=10){finish(Grade.UNKNOWN,Double.NaN,"Recent fall/environmental damage cannot be excluded");return;}
  int chosen=-1;double closest=Double.POSITIVE_INFINITY;
  for(int n=0;n<jumpCount;n++){int i=(jumpCursor-1-n)&31;double error=jumpTimes[i]-t-targetTicks;
   if(jumpIds[i]>consumedId&&jumpEligible[i]&&jumpTimes[i]>=t-windowTicks&&jumpTimes[i]<=t&&Math.abs(error)<closest){chosen=i;closest=Math.abs(error);}}
  if(chosen>=0){consumedId=jumpIds[chosen];score(jumpTimes[chosen]-t-targetTicks);}
  else if(!grounded||!validDistance(distance)){finish(Grade.UNKNOWN,Double.NaN,"Airborne or ground evidence unavailable");}
 }
 private void expire(double now){
  if(!Double.isNaN(lastHurt)&&now-lastHurt>2){lastHurt=Double.NaN;finish(Grade.UNKNOWN,Double.NaN,"Hurt without nearby local knockback observation");}
  if(!Double.isNaN(lastVelocity)&&now-lastVelocity>2){lastVelocity=Double.NaN;}
  if(!Double.isNaN(pending)&&now>pending+targetTicks+windowTicks+.5)finish(Grade.MISSED,Double.NaN,"No manual jump edge in matching window");
 }
 private void score(double error){finish(Math.abs(error)<=.5?Grade.PERFECT:error<0?Grade.EARLY:Grade.LATE,error,"Paired local hurt + velocity proxy; server outcome unknown");}
 private void finish(Grade grade,double error,String reason){
  last=new Result(grade,error,reason,ns);results[resultCursor++&31]=last;resultCount=Math.min(32,resultCount+1);
  if(grade==Grade.PERFECT){perfect++;streak++;best=Math.max(best,streak);soundFlags|=2;}
  else if(grade==Grade.EARLY){early++;streak=0;soundFlags|=4;}
  else if(grade==Grade.LATE){late++;streak=0;soundFlags|=4;}
  else if(grade==Grade.MISSED){missed++;streak=0;soundFlags|=4;}
  else unknown++;
  if(!Double.isNaN(error)){errorSum+=error;errorCount++;}pending=Double.NaN;
 }
 public void attack(long nano){attack(nano,true);}
 public void attack(long nano,boolean qualifyingContext){
  if(!active)return;pendingMarks|=ATTACK;
  if(!qualifyingContext||!validDistance(distance)||Double.isNaN(vy)){unscored++;return;}
  attacks++;if(!grounded&&vy<-.01&&distance>.05){falling++;pendingMarks|=FALLING;soundFlags|=1;}
 }
 public int consumeSound(){int v=soundFlags;soundFlags=0;return v;}
 public Snapshot snapshot(long nano){return new Snapshot(this,nano);}
 public static String lastText(Result r){
  if(r==null)return "Jump timing: ready";
  if(Double.isNaN(r.error))return r.grade.name();
  double rounded=Math.round(r.error*10)/10.0;return r.grade.name()+" "+(rounded>=0?"+":"")+rounded+" ticks ("+Math.round(r.error*50)+" ms nominal)";
 }
}
