import me.movi.violet.*;

public final class VisualChecks {
    private static int checks;
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    public static void main(String[] args){
        ComboObservation combo=new ComboObservation();
        check(!combo.hurt(0,0)&&combo.count()==0,"Default entity zero cannot create phantom combo");
        combo.attack(3,0,true);check(!combo.hurt(4,1),"Unrelated hurt event ignored");
        check(combo.hurt(3,2)&&combo.count()==1,"Recent local attack and matching hurt event correlate once");
        check(!combo.hurt(3,3)&&combo.count()==1,"Repeated hurt event does not duplicate hit");
        combo.attack(3,4,true);check(!combo.hurt(3,800000005L),"Stale attempt cannot extend combo");
        combo.tick(2000000002L,2000000000L);check(combo.count()==0,"Configurable combat-end timeout");
        combo.attack(3,3000000000L,true);combo.hurt(3,3000000001L);combo.attack(4,3000000002L,true);check(combo.count()==0,"Target change resets configured combo");
        combo.hurt(4,3000000003L);combo.attack(5,3000000004L,false);check(combo.count()==1,"Optional target-change reset preserves counter when disabled");
        combo.clear();check(combo.count()==0&&!combo.hurt(5,3000000005L),"World/disable cleanup drops pending correlation");
        HealthDelta health=new HealthDelta();
        check(Double.isNaN(health.observe(1,20)),"First observation never invents damage");
        check(health.observe(1,17)==3,"Observed health drop");
        check(health.observe(1,19)==-2,"Observed healing retained");
        check(Double.isNaN(health.observe(1,Float.NaN)),"Invalid health discarded");
        check(health.observe(1,19)==0,"Invalid observation does not replace baseline");
        for(int i=2;i<140;i++)health.observe(i,20);
        check(Double.isNaN(health.observe(1,10)),"Health observations have bounded retention");
        health.clear();check(Double.isNaN(health.observe(1,10)),"World change clears health baseline");
        ObservedBeds beds=new ObservedBeds();beds.changedVisible(1,2,3,false);check(beds.values().isEmpty(),"Unobserved locations are not created by destruction checks");
        beds.observe(1,2,3);beds.observe(1,2,3);check(beds.values().size()==1,"Repeated observations deduplicate");
        beds.material(2,2,3,"obsidian");check(beds.values().iterator().next().material.equals("obsidian"),"Directly observed nearby material recorded");
        beds.changedVisible(1,2,3,false);check(beds.values().iterator().next().destroyed,"Visible block change records destruction");
        beds.observe(1,2,3);check(!beds.values().iterator().next().destroyed,"New observed bed clears historical destruction");
        for(int i=10;i<100;i++)beds.observe(i,64,i);check(beds.values().size()==32,"Bed memory has fixed upper bound");
        beds.clear();check(beds.values().isEmpty(),"Match/world cleanup clears waypoints");
        System.out.println("PASS: "+checks+" hit correlation, observed health changes, bounded bed records and lifecycle checks");
    }
}
