import me.movi.violet.VioletEngine;
public class EngineChecks {
 static int checks;static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static int poll(VioletEngine e,int hand,long now,boolean allow,boolean manual,double min,double max,int delay,int ramp,double variation){int n=e.poll(hand,now,allow,manual,min,max,delay,ramp,variation);if(n>0)e.generated(hand,now,n);return n;}
 public static void main(String[] args){
  VioletEngine e=new VioletEngine(()->.5);
  check(poll(e,0,0,true,false,10,10,0,0,0)==1,"Immediate activation without implicit interval");
  check(poll(e,0,0,true,false,10,10,0,0,0)==0,"No duplicate at same timestamp");
  check(poll(e,0,99000000,true,false,10,10,0,0,0)==0,"Rate deadline");
  check(poll(e,0,100000000,true,false,10,10,0,0,0)==1,"Next deadline");
  check(poll(e,1,100000000,false,false,60,60,0,0,0)==0,"Independent disabled hand");
  e.clear();for(long t=0;t<=2000000000L;t+=50000000L)poll(e,0,t,true,false,10,10,0,0,0);
  check(e.generatedCps(0,2000000000L)==10,"10 CPS sliding window");
  e.clear();int peak=0;for(long t=0;t<=2000000000L;t+=50000000L){int n=poll(e,0,t,true,false,60,60,0,0,0);peak=Math.max(peak,n);poll(e,1,t,true,false,60,60,0,0,0);}
  check(e.generatedCps(0,2000000000L)==60,"60 left queued CPS");check(e.generatedCps(1,2000000000L)==60,"60 right queued CPS");check(peak<=4,"Bounded tick budget");
  check(poll(e,0,9000000000L,true,false,60,60,0,0,0)==1,"Lag drops expired backlog");check(poll(e,0,9000000000L,true,false,60,60,0,0,0)==0,"No lag retry burst");
  e.clear();check(poll(e,0,0,true,true,60,60,0,0,0)==0,"Manual pending wins initial tick");check(poll(e,0,50000000,true,true,60,60,0,0,0)==0,"Continued manual priority");check(e.generatedCps(0,50000000)==0,"Skipped actions not counted");check(poll(e,0,100000000,true,false,60,60,0,0,0)<=4,"Resume after manual input remains bounded");
  e.clear();check(poll(e,0,0,true,false,60,60,200,0,0)==0,"Explicit start delay");check(poll(e,0,199000000,true,false,60,60,200,0,0)==0,"Delay not early");check(poll(e,0,200000000,true,false,60,60,200,0,0)==1,"No extra interval after delay");
  e.cancel();check(!e.active(0),"Cancel");check(poll(e,0,300000000,true,false,10,10,0,0,0)==1,"Immediate restart without delay");
  e.clear();e.physical(1,0);check(e.physicalCps(1,0)==1&&e.physicalCps(0,0)==0,"Physical counts independent");check(e.physicalCps(1,1000000000L)==0,"Count expiry");
  e.clear();check(poll(e,0,0,true,false,Double.NaN,Double.NaN,0,0,25)==1,"Invalid ranges sanitized");check(poll(e,0,50000000,true,false,Double.NaN,Double.NaN,0,0,25)==0,"Invalid range does not become high rate");
  e.clear();for(long t=0;t<=2000000000L;t+=50000000L)poll(e,0,t,true,false,10,20,0,1000,0);check(e.generatedCps(0,2000000000L)>=14&&e.generatedCps(0,2000000000L)<=16,"Ramp settles");
  for(double random:new double[]{0,1}){VioletEngine x=new VioletEngine(()->random);for(long t=0;t<=3000000000L;t+=50000000L)check(poll(x,0,t,true,false,60,60,0,0,25)<=4,"Extreme variation bounded");check(x.generatedCps(0,3000000000L)<=60,"Variation respects maximum rate");}
  System.out.println("PASS: "+checks+" scheduling, manual priority, 60 CPS per hand, variation, delay, ramp, lag and count checks");
 }
}
