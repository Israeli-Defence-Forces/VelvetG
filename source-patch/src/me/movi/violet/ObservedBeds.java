package me.movi.violet;

import java.util.LinkedHashMap;
import java.util.Collection;

/** No map assumptions or chunk scans: each entry starts with a directly observed bed block. */
public final class ObservedBeds {
    public static final class Bed { public final int x,y,z;public boolean destroyed;public String material="Unknown";Bed(int x,int y,int z){this.x=x;this.y=y;this.z=z;} }
    private final LinkedHashMap<String,Bed> beds=new LinkedHashMap<String,Bed>();
    private static String key(int x,int y,int z){return x+":"+y+":"+z;}
    public void observe(int x,int y,int z){String key=key(x,y,z);Bed b=beds.get(key);if(b==null){b=new Bed(x,y,z);beds.put(key,b);if(beds.size()>32)beds.remove(beds.keySet().iterator().next());}b.destroyed=false;}
    public void changedVisible(int x,int y,int z,boolean stillBed){Bed b=beds.get(key(x,y,z));if(b!=null)b.destroyed=!stillBed;}
    public void material(int x,int y,int z,String value){for(Bed b:beds.values())if(!b.destroyed&&Math.abs(b.x-x)<=2&&Math.abs(b.y-y)<=2&&Math.abs(b.z-z)<=2)b.material=value;}
    public Collection<Bed> values(){return beds.values();}
    public void clear(){beds.clear();}
}
