package me.movi.violet;

import java.awt.Color;
import org.lwjgl.opengl.GL11;
import vgstub.ave;
import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import vgstub.pk;

/** All overlays use depth testing and restore caller OpenGL attributes and matrices. */
final class WorldLines {
    private WorldLines() {}
    static void begin(ave mc,float partial){IMixinRenderManager camera=(IMixinRenderManager)mc.af();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();GL11.glTranslated(-camera.getRenderPosX(),-camera.getRenderPosY(),-camera.getRenderPosZ());GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false);GL11.glLineWidth(1);}
    static void end(){GL11.glPopMatrix();GL11.glPopAttrib();}
    static void box(double x,double y,double z,double w,double h,Color c){GL11.glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);double a=x-w/2,b=x+w/2,d=z-w/2,e=z+w/2;GL11.glBegin(GL11.GL_LINES);edge(a,y,d,b,y,d);edge(b,y,d,b,y,e);edge(b,y,e,a,y,e);edge(a,y,e,a,y,d);edge(a,y+h,d,b,y+h,d);edge(b,y+h,d,b,y+h,e);edge(b,y+h,e,a,y+h,e);edge(a,y+h,e,a,y+h,d);edge(a,y,d,a,y+h,d);edge(b,y,d,b,y+h,d);edge(b,y,e,b,y+h,e);edge(a,y,e,a,y+h,e);GL11.glEnd();}
    private static void edge(double x,double y,double z,double a,double b,double c){GL11.glVertex3d(x,y,z);GL11.glVertex3d(a,b,c);}
}
