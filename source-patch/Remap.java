import java.nio.file.*;import java.util.*;import java.util.zip.*;
import org.spongepowered.asm.lib.*;import org.spongepowered.asm.lib.commons.*;
public class Remap {
 public static void main(String[] a)throws Exception {
  boolean prefix=a[0].equals("prefix");
  try(ZipFile z=new ZipFile(a[1]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(Paths.get(a[2])))){
   Enumeration<? extends ZipEntry> en=z.entries();while(en.hasMoreElements()){
    ZipEntry e=en.nextElement();if(e.isDirectory())continue;byte[] b=read(z.getInputStream(e));String n=e.getName();
    if(n.endsWith(".class")) {ClassReader cr=new ClassReader(b);ClassWriter cw=new ClassWriter(0);
     Remapper r=new Remapper(){public String map(String s){if(prefix)return !s.contains("/")?"vgstub/"+s:s;return s.startsWith("vgstub/")?s.substring(7):s;}};
     cr.accept(new ClassRemapper(cw,r),0);b=cw.toByteArray();n=r.map(cr.getClassName())+".class";
    }
    out.putNextEntry(new ZipEntry(n));out.write(b);out.closeEntry();
   }
  }
 }
 static byte[] read(java.io.InputStream in)throws Exception{java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();byte[] t=new byte[8192];int n;while((n=in.read(t))>0)b.write(t,0,n);return b.toByteArray();}
}
