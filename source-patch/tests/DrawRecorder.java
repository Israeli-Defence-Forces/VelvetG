import java.awt.Color;import java.util.*;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Font;
/** Only graphics are stubbed: execute the real screen and HUD draw methods. */
public final class DrawRecorder extends NanoVGManager {
 public List<float[]> clips;public List<String> labels;public List<String> svg;private double sx=1,sy=1,tx,ty;private ArrayDeque<double[]> stack;
 public void init(){sx=sy=1;tx=ty=0;clips=new ArrayList<>();labels=new ArrayList<>();svg=new ArrayList<>();stack=new ArrayDeque<>();}
 @Override public void setupAndDraw(Runnable r){r.run();}
 @Override public void save(){stack.push(new double[]{sx,sy,tx,ty});}
 @Override public void restore(){double[] a=stack.pop();sx=a[0];sy=a[1];tx=a[2];ty=a[3];}
 @Override public void translate(float x,float y){tx+=x*sx;ty+=y*sy;}
 @Override public void scale(float x,float y,float scale){translate(x,y);sx*=scale;sy*=scale;translate(-x,-y);}
 private String color(Color c){return String.format("#%02x%02x%02x",c.getRed(),c.getGreen(),c.getBlue());}
 @Override public void scissor(float x,float y,float w,float h){clips.add(new float[]{x,y,w,h});}
 @Override public void drawGradientRoundedRect(float x,float y,float w,float h,float r,Color a,Color b){drawRoundedRect(x,y,w,h,r,a);}
 @Override public void drawArrow(float x,float y,float w,float h,Color c){svg.add(String.format(java.util.Locale.ROOT,"<polygon points='%f,%f %f,%f %f,%f' fill='%s'/>",tx+x*sx,ty+y*sy,tx+(x+w/2)*sx,ty+(y+h)*sy,tx+(x+w)*sx,ty+y*sy,color(c)));}
 @Override public void drawRect(float x,float y,float w,float h,Color c){drawRoundedRect(x,y,w,h,0,c);}
 @Override public void drawRoundedRect(float x,float y,float w,float h,float r,Color c){svg.add(String.format(java.util.Locale.ROOT,"<rect x='%f' y='%f' width='%f' height='%f' rx='%f' fill='%s' fill-opacity='%f'/>",tx+x*sx,ty+y*sy,w*sx,h*sy,r*sx,color(c),c.getAlpha()/255.0));}
 @Override public void drawOutlineRoundedRect(float x,float y,float w,float h,float r,float line,Color c){svg.add(String.format(java.util.Locale.ROOT,"<rect x='%f' y='%f' width='%f' height='%f' rx='%f' fill='none' stroke='%s' stroke-width='%f'/>",tx+x*sx,ty+y*sy,w*sx,h*sy,r*sx,color(c),line*sx));}
 @Override public void drawCircle(float x,float y,float r,Color c){svg.add(String.format(java.util.Locale.ROOT,"<circle cx='%f' cy='%f' r='%f' fill='%s'/>",tx+x*sx,ty+y*sy,r*sx,color(c)));}
 @Override public void drawText(String text,float x,float y,Color c,float size,Font font){labels.add(text);String clean=text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");svg.add(String.format(java.util.Locale.ROOT,"<text x='%f' y='%f' font-family='Inter, sans-serif' font-size='%f' fill='%s' fill-opacity='%f'>%s</text>",tx+x*sx,ty+(y+size)*sy,size*sx,color(c),c.getAlpha()/255.0,clean));}
 @Override public void drawCenteredText(String text,float x,float y,Color c,float size,Font font){drawText(text,x-getTextWidth(text,size,font)/2,y,c,size,font);}
 @Override public float getTextWidth(String text,float size,Font font){return text.length()*size*.52f;}
 @Override public String getLimitText(String text,float size,Font font,float width){int max=(int)(width/(size*.52f));return text.length()<=max?text:text.substring(0,Math.max(0,max-3))+"...";}
}
