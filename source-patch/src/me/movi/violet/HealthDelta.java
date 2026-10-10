package me.movi.violet;

import java.util.LinkedHashMap;

/** Numbers describe observed health changes, not proof that a local attack caused damage. */
public final class HealthDelta {
    private final LinkedHashMap<Integer,Float> health=new LinkedHashMap<Integer,Float>();
    public double observe(int id,float hp){if(!Float.isFinite(hp)||hp<0)return Double.NaN;Float previous=health.remove(id);health.put(id,hp);if(health.size()>128)health.remove(health.keySet().iterator().next());return previous==null?Double.NaN:previous-hp;}
    public void clear(){health.clear();}
}
