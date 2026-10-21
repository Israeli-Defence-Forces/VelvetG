package tools;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;
public final class TrainerMixinCheck {
 public static void main(String[] args)throws Exception {
  List<URL> urls=new ArrayList<URL>();
  for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(new File(p).toURI().toURL());
  Launch.blackboard=new HashMap<String,Object>();
  Launch.blackboard.put("TweakClasses",new ArrayList<String>());
  Launch.blackboard.put("Tweaks",new ArrayList<Object>());
  Launch.classLoader=new LaunchClassLoader(urls.toArray(new URL[0]));
  Launch.classLoader.addClassLoaderExclusion("org.spongepowered.");
  Launch.classLoader.addClassLoaderExclusion("tools.");
  Thread.currentThread().setContextClassLoader(Launch.classLoader);
  org.spongepowered.asm.launch.MixinTweaker tweak=new org.spongepowered.asm.launch.MixinTweaker();
  ((List<Object>)Launch.blackboard.get("Tweaks")).add(tweak);
  tweak.acceptOptions(Collections.<String>emptyList(),null,null,"combatiq-integration");
  tweak.injectIntoClassLoader(Launch.classLoader);
  MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
  Mixins.addConfiguration("mixins.combatiq.integration.json");
  Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
  Object transformer=MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
  if(transformer==null){Constructor<?> c=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();c.setAccessible(true);transformer=c.newInstance();}
  Method transform=transformer.getClass().getMethod("transformClassBytes",String.class,String.class,byte[].class);
  try(ZipFile minecraft=new ZipFile(args[0]);ZipFile client=new ZipFile(args[1])){
   for(String name:new String[]{"ave","bew","bcy","ek","ek$4","me/eldodebug/soar/management/mods/ModManager"}){
    ZipFile source=name.startsWith("me/")?client:minecraft;
    byte[] original;try(InputStream in=source.getInputStream(source.getEntry(name+".class"))){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);original=b.toByteArray();}
    byte[] changed=(byte[])transform.invoke(transformer,name.replace('/','.'),name.replace('/','.'),original);
    ClassNode node=new ClassNode();new ClassReader(changed).accept(node,0);int hooks=0,injectionCalls=0,trainer=0;
    Set<String> methods=new HashSet<String>();
    for(MethodNode m:node.methods)for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext())
     if(n instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)n;
       if(call.owner.equals("me/eldodebug/soar/combatiq/CombatIQNative"))hooks++;
       if(call.owner.equals("me/eldodebug/soar/knockback/TrainerRuntime")||call.owner.equals("me/eldodebug/soar/knockback/KnockbackTrainerMod"))trainer++;
       if(call.owner.equals(name)&&(call.name.contains("combatiq$")||call.name.contains("trainer$"))){injectionCalls++;methods.add(m.name+m.desc);}
     }
    int minimum=name.equals("ave")?6:name.equals("bew")?5:name.equals("bcy")?5:name.equals("ek")?4:1;
    int methodMinimum=name.equals("ave")?6:name.equals("bew")?3:name.equals("bcy")?5:name.equals("ek")?3:1;
    if(hooks==0||injectionCalls<minimum||methods.size()<methodMinimum)
       throw new AssertionError("Missing applied callbacks "+name+" calls="+injectionCalls+" methods="+methods);
    int trainerMinimum=name.equals("ave")?4:name.equals("bcy")||name.equals("ek")?1:0;
    if(trainer<trainerMinimum)throw new AssertionError("Trainer not applied "+name);
    if(name.endsWith("ModManager")){boolean found=false;for(MethodNode m:node.methods)for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext())if(n instanceof TypeInsnNode&&((TypeInsnNode)n).desc.equals("me/eldodebug/soar/knockback/KnockbackTrainerMod"))found=true;if(!found)throw new AssertionError("Trainer registration not applied");}
    System.out.println("TRAINER "+name+" runtime_calls="+trainer);
    System.out.println("INJECTIONS "+name+" calls="+injectionCalls+" methods="+methods);
    System.out.println("APPLIED "+name+" native_calls="+hooks+" bytes="+changed.length);
   }
  }
  System.out.println("PASS actual Mixin transformation of official target bytecode; no Minecraft launch");
 }
}
