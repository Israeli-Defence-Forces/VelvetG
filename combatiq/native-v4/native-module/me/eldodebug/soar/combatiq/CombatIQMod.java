package me.eldodebug.soar.combatiq;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.TextSetting;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.event.impl.EventRender2D;

/** Native module and widget: inherited HUDMod owns persistence, scale, preview,
 * position and themed drawing. Render only consumes immutable precomputed data. */
public final class CombatIQMod extends HUDMod {
    private final NamedBoolean recording=new NamedBoolean("Recording","recording",false);
    private final NamedBoolean visible=new NamedBoolean("HUD visible","hud",true);
    private final NamedBoolean expanded=new NamedBoolean("Detailed diagnostics","expanded",false);
    private final NamedBoolean wtap=new NamedBoolean("W-tap analysis","wtap",true);
    private final NamedBoolean block=new NamedBoolean("Block-hit analysis","block",true);
    private final NamedBoolean hit=new NamedBoolean("Hit-select analysis","hit",true);
    private final NamedBoolean combo=new NamedBoolean("Combo analysis","combo",true);
    private final NamedBoolean rotate=new NamedBoolean("Rotate recording files","rotate",true);
    private final NamedBoolean deep=new NamedBoolean("Deep diagnostic telemetry","deep",false);
    private final NamedBoolean export=new NamedBoolean("Export local report","export",false);
    private final NamedNumber seconds=new NamedNumber("Recording seconds","seconds",300,1,3600);
    private final NamedNumber size=new NamedNumber("Recording size MiB","sizeMiB",32,1,256);
    private final NamedText location=new NamedText();
    private final Runnable draw=new Runnable(){public void run(){drawSnapshot();}};
    private volatile boolean renderFailed;
    private volatile String fault="";
    private CombatIQNative.Snapshot measured;
    private int measuredWidth=300;
    private Object measuredFont;
    private float measuredScale;

    public CombatIQMod(){super(TranslateText.EDIT_HUD,TranslateText.EDIT_HUD);CombatIQNative.setEnabled(false);}
    @Override public String getName(){return "CombatIQ";}
    @Override public String getNameKey(){return "combatiq.module";}
    @Override public String getDescription(){return "Experimental local combat evidence; server outcome unknown";}
    @Override public void onEnable(){
        renderFailed=false;fault="";
        try{super.onEnable();if(isToggled())updateControls();}
        catch(Throwable failure){fault="Module initialization unavailable";CombatIQNative.setEnabled(false);}
    }
    @Override public void onDisable(){
        CombatIQNative.setEnabled(false);
        try{super.onDisable();}catch(Throwable failure){fault="Module listener teardown unavailable";}
    }
    private void updateControls(){
        int mask=(wtap.isToggled()?1:0)|(block.isToggled()?2:0)|(hit.isToggled()?4:0)|(combo.isToggled()?8:0);
        CombatIQNative.configure(seconds.getValueLong(),size.getValueLong()*1048576L,deep.isToggled()?2:1,mask);
        CombatIQNative.setExportDirectory(location.getText());
        CombatIQNative.setRotation(rotate.isToggled());
        CombatIQNative.setEnabled(isToggled()&&recording.isToggled());
        if(export.isToggled()){export.setToggled(false);CombatIQNative.requestReportExport();}
    }
    @EventTarget public void onTick(EventTick event){
        try{updateControls();}
        catch(Throwable failure){fault="Invalid CombatIQ setting: "+failure.getClass().getSimpleName();CombatIQNative.setEnabled(false);}
    }
    @EventTarget public void onRender2D(EventRender2D event){
        if(renderFailed||!visible.isToggled()&&!isEditing())return;
        try{Glide.getInstance().getNanoVGManager().setupAndDraw(draw);}
        catch(Throwable failure){renderFailed=true;fault="CombatIQ rendering disabled after error";}
    }
    private void drawSnapshot(){
        CombatIQNative.Snapshot s=CombatIQNative.snapshot();
        boolean detailed=expanded.isToggled();int lines=detailed?s.diagnostics.size():0;
        int height=108+lines*13+(detailed?26:0);
        // Width measurements are cached by published snapshot. No new per-frame objects.
        if(measured!=s||measuredFont!=getHudFont(0)||measuredScale!=getScale()){measured=s;measuredFont=getHudFont(0);measuredScale=getScale();float width=290;
            width=Math.max(width,getTextWidth(s.status,10,getHudFont(0))+16);
            for(int i=0;i<s.diagnostics.size();i++)width=Math.max(width,getTextWidth(s.diagnostics.get(i),10,getHudFont(0))+16);
            measuredWidth=Math.min(560,(int)Math.ceil(width));}
        setWidth(measuredWidth);setHeight(height);save();
        try{
            drawBackground(getX(),getY(),measuredWidth,height);
            line("CombatIQ | Server outcome unknown",13);
            line(s.compactState,27);
            line(s.transition,41);line(s.action,55);line(s.status,69);line(s.health,83);
            line(fault.isEmpty()?"Local observations only; no hit confirmation":fault,97);
            if(detailed){for(int i=0;i<lines;i++)line(s.diagnostics.get(i),112+i*13);
                line(CombatIQNative.lastExport(),112+lines*13);
                line("Export via native module setting; limits apply next recording",125+lines*13);}
        }finally{restore();}
    }
    private void line(String text,int y){drawText(text,getX()+7,getY()+y,10,getHudFont(0));}
    private final class NamedBoolean extends BooleanSetting {
        private final String label,key;
        NamedBoolean(String label,String key,boolean value){super(TranslateText.EDIT_HUD,CombatIQMod.this,value);this.label=label;this.key="combatiq."+key;}
        @Override public String getName(){return label;}
        @Override public String getNameKey(){return key;}
    }
    private final class NamedNumber extends NumberSetting {
        private final String label,key;
        NamedNumber(String label,String key,double value,double min,double max){super(TranslateText.EDIT_HUD,CombatIQMod.this,value,min,max,true);this.label=label;this.key="combatiq."+key;}
        @Override public String getName(){return label;}
        @Override public String getNameKey(){return key;}
    }
    private final class NamedText extends TextSetting {
        NamedText(){super(TranslateText.EDIT_HUD,CombatIQMod.this,CombatIQNative.exportDirectory());}
        @Override public String getName(){return "Export directory";}
        @Override public String getNameKey(){return "combatiq.exportDir";}
    }
}
