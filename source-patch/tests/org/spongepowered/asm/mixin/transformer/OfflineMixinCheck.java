package org.spongepowered.asm.mixin.transformer;
import java.io.*;import java.net.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;
import net.minecraft.launchwrapper.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
public final class OfflineMixinCheck {
 public static void main(String[] args)throws Exception{
  List<URL> urls=new ArrayList<>();for(String s:args)urls.add(new File(s).toURI().toURL());
  Launch.classLoader=new LaunchClassLoader(urls.toArray(new URL[0]));Launch.blackboard=new HashMap<>();Launch.blackboard.put("TweakClasses",new ArrayList<String>());Launch.blackboard.put("Tweaks",new ArrayList<Object>());
  MixinBootstrap.init();MixinEnvironment.getDefaultEnvironment().setObfuscationContext("notch");MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);if(Boolean.getBoolean("violet.test.combined"))Mixins.addConfiguration("mixins.soar.json");Mixins.addConfiguration("mixins.violet.json");
  java.lang.reflect.Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
  MixinTransformer t=(MixinTransformer)MixinEnvironment.getCurrentEnvironment().getActiveTransformer();Path out=Paths.get("violet-work/transformed");Files.createDirectories(out);
  try(ZipFile z=new ZipFile(args[1])){for(String name:Arrays.asList("ave","avb","bda","bev")){byte[] b=read(z.getInputStream(z.getEntry(name+".class")));byte[] result=t.transformClassBytes(name,name,b);if(java.util.Arrays.equals(b,result))throw new AssertionError("Mixin did not apply to "+name);Files.write(out.resolve(name+".class"),result);System.out.println("TRANSFORMED "+name+": "+b.length+" -> "+result.length);}}
 }
 static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toByteArray();}
}
