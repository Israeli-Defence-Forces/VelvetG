package me.eldodebug.soar.combatiq;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bounded O(1) event accumulation. Caller owns the recorder gate; renderers only
 * receive the immutable published snapshot. No CSV parsing or server inference. */
final class LiveDiagnostics {
    private final String[] timeline=new String[16];
    private final double[] releases=new double[64];
    private int cursor,count,releaseCount,releaseCursor;
    private boolean down,known,sprintBeforeKnown,sprintBefore;
    private long releaseAt,lastAttack,lastFeedback,firstNs,eventNs;
    private int lastTarget=-1,chain;
    private long rawDown,duplicateDown,attacks,uses,feedback,swings;
    private String transition="Sprint transition unknown";
    void reset(){cursor=count=releaseCount=releaseCursor=chain=0;known=false;releaseAt=lastAttack=lastFeedback=0;
        rawDown=duplicateDown=attacks=uses=feedback=swings=firstNs=eventNs=0;lastTarget=-1;sprintBeforeKnown=false;transition="Sprint transition unknown";
        Arrays.fill(timeline,null);Arrays.fill(releases,0);}
    private void note(String text){timeline[cursor++ & 15]="+"+Math.max(0,(eventNs-firstNs)/1000000L)+"ms "+text;count=Math.min(16,count+1);}
    void context(){known=false;releaseAt=lastAttack=lastFeedback=0;chain=0;lastTarget=-1;sprintBeforeKnown=false;note("Context boundary; pending correlations cleared");}
    void observe(int kind,int data,long ns,int entity,int flags){
        eventNs=ns;if(firstNs==0)firstNs=ns;
        if(kind==12){context();return;}
        if(kind==2){rawDown++;if(known&&down)duplicateDown++;
            if(known&&!down&&releaseAt!=0){double ms=(ns-releaseAt)/1e6;releases[releaseCursor++ & 63]=ms;releaseCount=Math.min(64,releaseCount+1);note("Forward release "+Math.round(ms)+" ms (intent unknown)");}
            down=true;known=true;releaseAt=0;
        }else if(kind==1){if(known&&down)releaseAt=ns;down=false;known=true;}
        else if(kind==10){sprintBefore=data!=0;sprintBeforeKnown=true;}
        else if(kind==11){if(sprintBeforeKnown&&sprintBefore!=(data!=0)){transition=data==0?"Local sprint stopped":"Local sprint restarted";note(transition);}sprintBeforeKnown=false;}
        else if(kind==5&&data==13){attacks++;chain=entity>=0&&entity==lastTarget&&lastAttack!=0&&ns-lastAttack<=750000000L?chain+1:1;
            lastTarget=entity;lastAttack=ns;note("ATTACK attempt; target "+entity+"; chain "+chain);
            if(lastFeedback!=0&&ns-lastFeedback<=500000000L)note("Attack near local feedback; source unknown");
        }else if(kind==5&&(data==11||data==12))note(data==11?"START sprint action attempt":"STOP sprint action attempt");
        else if(kind==14){uses++;note("Use intent (not proof of blocking)");}
        else if(kind==20){swings++;note("Local swing observed; acceptance unknown");}
    }
    void localFeedback(long ns){lastFeedback=ns;feedback++;note("Local feedback; attacker unknown");}
    String transition(){return transition;}
    List<String> lines(int mask){
        ArrayList<String> result=new ArrayList<String>();
        if((mask&1)!=0){double[] sorted=Arrays.copyOf(releases,releaseCount);Arrays.sort(sorted);
            result.add(releaseCount==0?"W-tap: no complete forward release":"Forward releases: "+releaseCount+"; median "+Math.round(sorted[sorted.length/2])+" ms");
            result.add("Raw down: "+rawDown+"; repeated down: "+duplicateDown);
            int[] bins=new int[4];for(double ms:sorted)bins[ms<50?0:ms<100?1:ms<=250?2:3]++;
            result.add("Release bins <50/<100/<=250/>250 ms: "+bins[0]+"/"+bins[1]+"/"+bins[2]+"/"+bins[3]);
        }
        if((mask&2)!=0)result.add("Block-use: "+uses+" use intents; "+swings+" swings; "+attacks+" attack attempts");
        if((mask&4)!=0)result.add("Hit-select: "+feedback+" local feedback observations; source unknown");
        if((mask&8)!=0)result.add("Combo: latest attempt chain "+chain+"; target "+lastTarget+" (not accepted hits)");
        result.add(transition);
        for(int i=0;i<count;i++)result.add(timeline[(cursor-count+i)&15]);
        return result;
    }
}
