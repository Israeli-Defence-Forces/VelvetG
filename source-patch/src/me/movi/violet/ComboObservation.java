package me.movi.violet;

/** One recent local attack can correlate with one observed hurt event; never count swings alone. */
public final class ComboObservation {
    private int target=-1,pending=-1,combo;
    private long attempted,lastHit;
    public void attack(int id,long now,boolean resetOnTarget){if(resetOnTarget&&target!=id)combo=0;target=pending=id;attempted=now;}
    public boolean hurt(int id,long now){if(pending!=id||pending<0||now-attempted>750000000L)return false;pending=-1;combo++;lastHit=now;return true;}
    public void tick(long now,long timeout){if(combo>0&&now-lastHit>=timeout)combo=0;if(pending>=0&&now-attempted>750000000L)pending=-1;}
    public int count(){return combo;}
    public void clear(){target=pending=-1;combo=0;attempted=lastHit=0;}
}
