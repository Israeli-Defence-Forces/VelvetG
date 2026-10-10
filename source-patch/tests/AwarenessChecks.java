import me.movi.violet.*;

public final class AwarenessChecks {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static final class World implements ProjectilePrediction.Environment {
        boolean wet,loaded=true,wall;
        int rays;
        public boolean loaded(double x,double y,double z){return loaded;}
        public boolean water(double x,double y,double z){return wet;}
        public boolean collision(double x,double y,double z,double nx,double ny,double nz,double[] hit){rays++;if(wall&&nx>=2){hit[0]=2;hit[1]=ny;hit[2]=nz;return true;}return false;}
    }
    public static void main(String[] args){
        check(MatchState.classify(false,new String[]{"BedWars"},"bedwars")==MatchState.State.DISCONNECTED,"Disconnected state");
        check(MatchState.classify(true,new String[]{"§dBED WARS","Starting in 10s"},"bedwars")==MatchState.State.WAITING,"Formatting and waiting adapter");
        check(MatchState.classify(true,new String[]{"BedWars","Lobby"},"bedwars")==MatchState.State.LOBBY,"Lobby state");
        check(MatchState.classify(true,new String[]{"BedWars","Diamond II in 3:00"},"bedwars")==MatchState.State.ACTIVE,"Active match evidence");
        check(MatchState.classify(true,new String[]{"BedWars","Victory"},"bedwars")==MatchState.State.COMPLETE,"Completion state");
        check(MatchState.classify(true,new String[]{"BedWars"},"bedwars")==MatchState.State.UNKNOWN,"Title alone does not imply active gameplay");
        check(MatchState.classify(true,new String[]{"Survival","Diamond"},"bedwars")==MatchState.State.UNKNOWN,"Unknown server fails closed");
        check(MatchState.classify(true,new String[]{"Custom BW","Beds: 3"},"custom bw")==MatchState.State.ACTIVE,"Configurable server marker");
        AlertCooldown alerts=new AlertCooldown();
        check(alerts.accept("fire:1",0,100),"First observed alert accepted");
        check(!alerts.accept("fire:1",99,100),"Duplicate cooldown");
        check(alerts.accept("fire:2",99,100),"Separate entity identity");
        check(alerts.accept("fire:1",100,100),"Exact cooldown boundary");
        check(alerts.sound(0)&&!alerts.sound(1499999999L)&&alerts.sound(1500000000L),"Global sound overlap guard");
        alerts.clear();check(alerts.accept("fire:1",100,100)&&alerts.sound(100),"Match reset clears alerts and sound guard");
        World world=new World();double[] out=new double[51];
        int n=ProjectilePrediction.predict(out,0,0,0,1,0,0,2,0,0,0,2,world);
        check(n==3&&Math.abs(out[6]-1.99)<.0001&&Math.abs(out[7]+.05)<.0001,"Arrow drag and gravity order");
        ProjectilePrediction.predict(out,0,0,0,1,0,0,1,0,0,0,2,world);
        check(Math.abs(out[7]+.03)<.0001,"Pearl gravity");
        ProjectilePrediction.predict(out,0,0,0,1,0,0,3,0,0,0,2,world);
        check(Math.abs(out[7]+.03)<.0001,"Snowball gravity");
        ProjectilePrediction.predict(out,0,0,0,1,0,0,4,0,0,0,2,world);
        check(Math.abs(out[7]+.03)<.0001,"Egg gravity");
        ProjectilePrediction.predict(out,0,0,0,1,0,0,0,.1,0,0,2,world);
        check(Math.abs(out[6]-2.045)<.0001&&out[7]==0,"Fireball acceleration with drag");
        world.wet=true;ProjectilePrediction.predict(out,0,0,0,1,0,0,2,0,0,0,2,world);
        check(Math.abs(out[6]-1.6)<.0001,"Observed water drag");
        world.wet=false;world.wall=true;n=ProjectilePrediction.predict(out,0,0,0,1,0,0,2,0,0,0,16,world);
        check(out[(n-1)*3]==2&&n<17,"Swept block collision terminates estimate");
        world.loaded=false;world.rays=0;n=ProjectilePrediction.predict(out,0,0,0,1,0,0,2,0,0,0,16,world);
        check(n==1&&world.rays==0,"Unloaded chunks never queried or treated as impact");
        world.loaded=true;world.wall=false;world.rays=0;n=ProjectilePrediction.predict(out,0,0,0,1,0,0,2,0,0,0,10000,world);
        check(n==17&&world.rays==16,"Hard collision-query and storage budget");
        check(ProjectilePrediction.predict(out,Double.NaN,0,0,1,0,0,2,0,0,0,16,world)==0,"Invalid entity positions rejected");
        check(ProjectilePrediction.segmentDistanceSquared(5,1,0,0,0,0,10,0,0)==1,"Approach distance detects mid-segment crossing");
        check(ProjectilePrediction.segmentDistanceSquared(0,0,0,3,0,0,3,0,0)==9,"Stationary segment distance");
        System.out.println("PASS: "+checks+" scoreboard, projectile physics, collision budgets, alert cooldown and sound checks");
    }
}
