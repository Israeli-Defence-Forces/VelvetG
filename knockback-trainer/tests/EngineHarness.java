import me.eldodebug.soar.knockback.TrainerEngine;
import me.eldodebug.soar.knockback.TrainerEngine.*;
public class EngineHarness {
 static int assertions;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static long ns(int t){return (t+100)*50000000L;}
 static TrainerEngine engine(){TrainerEngine e=new TrainerEngine();e.tick(10,ns(10),true,true,0,0,0);e.sample(true,0,0);return e;}
 static void tick(TrainerEngine e,int t,boolean grounded,double d,double vy){e.tick(t,ns(t),true,grounded,d,vy,0);e.sample(grounded,d,vy);}
 static void impact(TrainerEngine e,int t){e.feedback(true,ns(t));e.feedback(false,ns(t)+1000000);}
 static Snapshot s(TrainerEngine e){return e.snapshot(ns(30));}
 public static void main(String[] args){
  TrainerEngine e=engine();impact(e,10);e.jump(ns(10)+5000000);check(s(e).perfect==1,"perfect same tick");check(s(e).streak==1,"streak increments");
  e=engine();e.jump(ns(10));tick(e,12,false,.3,.1);impact(e,12);check(s(e).early==1,"pre-impact jump EARLY");check(s(e).result.error==-2,"signed early error");
  e=engine();impact(e,10);tick(e,12,true,0,0);e.jump(ns(12));check(s(e).late==1,"late jump");check(s(e).result.error==2,"signed late error");
  e=engine();impact(e,10);for(int t=11;t<=17;t++)tick(e,t,true,0,0);check(s(e).missed==1,"missed deadline");
  e=engine();e.configure(3,5,6);tick(e,11,true,0,0);impact(e,11);for(int t=12;t<=14;t++)tick(e,t,true,0,0);e.jump(ns(14));check(s(e).perfect==1,"configured offset");
  e=engine();impact(e,10);e.interrupt("GUI");check(s(e).unknown==1&&s(e).missed==0,"GUI is unknown");check(s(e).attempts==0,"unknown not a failed attempt");
  e=engine();tick(e,11,false,1,.2);impact(e,11);check(s(e).unknown==1,"airborne impact unknown");
  e=engine();tick(e,11,true,Double.NaN,0);impact(e,11);check(s(e).unknown==1,"unknown ground");
  e=engine();e.tick(11,ns(11),true,true,0,0,5);impact(e,11);check(s(e).unknown==1&&s(e).attempts==0,"environmental fall ambiguity");
  e=engine();e.feedback(true,ns(10));for(int t=11;t<=14;t++)tick(e,t,true,0,0);check(s(e).unknown==1,"unpaired hurt unknown");
  e=engine();impact(e,10);e.tick(11,ns(15),true,true,0,0,0);check(s(e).unknown==1,"tick stall invalidates pending");
  e=engine();e.attack(ns(10));tick(e,11,false,.4,.3);e.attack(ns(11));tick(e,12,false,.5,-.2);e.attack(ns(12));check(s(e).attacks==3&&s(e).falling==1,"falling vs rising attempts");
  tick(e,13,false,Double.NaN,-.2);e.attack(ns(13));check(s(e).unscored==1&&s(e).attacks==3,"unknown attacks excluded denominator");
  e.reset();check(s(e).attempts==0&&s(e).attacks==0&&s(e).best==0&&s(e).result==null,"session reset");
  e=engine();for(int i=11;i<900;i++)tick(e,i,true,0,0);check(s(e).distance.length==120,"history duration capped");e.configure(0,5,30);check(s(e).distance.length==600,"fixed capacity");
  e.boundary("world");check(s(e).distance.length==0,"world transition clears graph");
  double[][] slabs={{0,0,0,1,.5,1},{0,0,0,.5,1,1}};
  check(TrainerEngine.surfaceDistance(1,.1,.9,.1,.9,slabs)==0,"highest overlapping stair surface");
  check(TrainerEngine.surfaceDistance(1,.6,.9,.1,.9,slabs)==.5,"slab below footprint");
  check(Double.isNaN(TrainerEngine.surfaceDistance(1,2,3,2,3,slabs)),"no surface unknown");
  e=engine();tick(e,11,false,.3,.4);tick(e,12,true,0,0);Snapshot snap=s(e);check((snap.markers[1]&TrainerEngine.TAKEOFF)!=0,"takeoff marker");check((snap.markers[2]&TrainerEngine.LANDING)!=0,"landing marker");
  snap.distance[0]=99;check(s(e).distance[0]==0,"immutable snapshot copy");
  e=engine();impact(e,10);e.jump(ns(10));check((e.consumeSound()&2)!=0,"timing sound signal");check(e.consumeSound()==0,"sound signal consumed once");
  tick(e,11,false,.5,-.2);e.attack(ns(11));check((e.consumeSound()&1)!=0,"falling attempt sound signal");
  e=engine();impact(e,10);e.jump(ns(10));tick(e,13,false,Double.NaN,0);impact(e,13);check(s(e).streak==1,"unknown preserves streak");
  e=engine();impact(e,10);for(int t=11;t<17;t++)tick(e,t,true,0,0);check(s(e).streak==0,"missed breaks streak");
  e=engine();impact(e,10);e.feedback(true,ns(10)+2000000);e.feedback(false,ns(10)+3000000);e.jump(ns(10)+5000000);check(s(e).attempts==1,"duplicate impact pairing bounded");
  e=engine();e.configure(6,10,30);tick(e,11,true,0,0);impact(e,11);for(int t=12;t<=17;t++)tick(e,t,true,0,0);e.jump(ns(17));check(s(e).perfect==1,"maximum offset target");
  e=engine();e.configure(0,0,6);tick(e,11,true,0,0);impact(e,11);e.jump(ns(11)+20000000L);check(s(e).perfect==1,"zero window same-tick precision");
  e=engine();e.configure(0,0,6);tick(e,11,true,0,0);impact(e,11);tick(e,12,true,0,0);check(s(e).missed==1,"zero window deadline");
  check(TrainerEngine.surfaceDistance(.99,.1,.9,.1,.9,slabs)==0,"collision precision tolerance");
  e=engine();e.tick(11,ns(11),true,true,0,0,4);e.reset();tick(e,12,true,0,0);impact(e,12);e.jump(ns(12));check(s(e).perfect==1,"reset clears stale environmental evidence");
  System.out.println("PASS: "+assertions+" trainer engine assertions; no Minecraft runtime claim");
 }
}
