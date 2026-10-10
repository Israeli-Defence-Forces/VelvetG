import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;

/** Builds the optional Violet layer from the exact r7 binary baseline restored by RestoreBase. */
public final class VioletPatch extends BytecodeIO {
 static final String ROOT="me/movi/violet/",RUNTIME=ROOT+"VioletRuntime";
 static void mark(ClassNode n){save(n);report.add(n.name);}

 static void replaceStrings(String entry,String[][] replacements){
  byte[] bytes=files.get(entry);if(bytes==null)return;
  ClassNode n=node(bytes);boolean changed=false;
  for(FieldNode f:n.fields)if(f.value instanceof String){String value=(String)f.value;for(String[] r:replacements)if(value.equals(r[0])){f.value=r[1];changed=true;break;}}
  for(MethodNode m:n.methods)for(AbstractInsnNode insn:m.instructions.toArray())if(insn instanceof LdcInsnNode&&((LdcInsnNode)insn).cst instanceof String){String value=(String)((LdcInsnNode)insn).cst;for(String[] r:replacements)if(value.equals(r[0])){((LdcInsnNode)insn).cst=r[1];changed=true;break;}}
  if(changed)mark(n);
 }

 public static void main(String[] args)throws Exception{
  load(args[0],files);load(args[1],mc);Map<String,byte[]> added=new LinkedHashMap<>();load(args[2],added);files.putAll(added);
  if(files.containsKey("mixins.violet.json"))throw new IllegalArgumentException("Use the original PerfPatch baseline.");
  String label=args.length>4?args[4]:"r8";

  // r8 FIX3 deliberately leaves inherited base-client strings byte-for-byte intact.
  // Rewriting legacy classes solely for branding can invalidate old StackMapTable frames.
  // Product/version identity is handled by release metadata and installer UI instead.

  ClassNode manager=node(files.get(BASE+"management/mods/ModManager.class"));
  MethodNode init=method(manager,"init");
  for(AbstractInsnNode i:init.instructions.toArray())if(i.getOpcode()==RETURN){InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(call(RUNTIME,"install","(L"+manager.name+";)V"));init.instructions.insertBefore(i,hook);}
  mark(manager);

  ClassNode tweaker=node(files.get(BASE+"injection/mixin/GlideTweaker.class"));boolean found=false;
  for(AbstractInsnNode i:method(tweaker,"injectIntoClassLoader").instructions.toArray())if(i instanceof MethodInsnNode){MethodInsnNode m=(MethodInsnNode)i;if(m.owner.equals("org/spongepowered/asm/mixin/Mixins")&&m.name.equals("addConfiguration")){InsnList hook=new InsnList();hook.add(new LdcInsnNode("mixins.violet.json"));hook.add(call(m.owner,m.name,"(Ljava/lang/String;)V"));method(tweaker,"injectIntoClassLoader").instructions.insert(i,hook);found=true;break;}}
  if(!found)throw new IllegalStateException("Mixin bootstrap missing");mark(tweaker);

  ClassNode category=node(files.get(BASE+"gui/modmenu/category/impl/ModuleCategory.class"));
  MethodNode open=method(category,"openModSettings");InsnList hook=new InsnList();LabelNode normal=new LabelNode();hook.add(new VarInsnNode(ALOAD,1));hook.add(call(RUNTIME,"openFor","(L"+BASE+"management/mods/Mod;)Z"));hook.add(new JumpInsnNode(IFEQ,normal));hook.add(new InsnNode(RETURN));hook.add(normal);open.instructions.insert(hook);

  int descriptions=0,tiles=0;
  for(MethodNode method:category.methods){boolean descriptionPending=false;int moduleSlot=-1;
   for(AbstractInsnNode instruction:method.instructions.toArray()){
    if(instruction instanceof TypeInsnNode&&((TypeInsnNode)instruction).desc.equals(BASE+"management/mods/Mod")){AbstractInsnNode next=instruction.getNext();if(next instanceof VarInsnNode&&next.getOpcode()==ASTORE)moduleSlot=((VarInsnNode)next).var;}
    if(instruction instanceof MethodInsnNode){MethodInsnNode m=(MethodInsnNode)instruction;
     if(m.owner.equals(BASE+"management/mods/Mod")&&m.name.equals("getName")){AbstractInsnNode previous=m.getPrevious();if(previous instanceof VarInsnNode&&previous.getOpcode()==ALOAD)moduleSlot=((VarInsnNode)previous).var;}
     if(m.owner.equals(BASE+"management/mods/Mod")&&m.name.equals("getDescription")){method.instructions.set(m,call(ROOT+"VioletUi","description","(L"+BASE+"management/mods/Mod;)Ljava/lang/String;"));descriptionPending=true;}
     else if(descriptionPending&&m.owner.equals(BASE+"management/nanovg/NanoVGManager")&&m.name.equals("drawText")){method.instructions.insertBefore(m,new VarInsnNode(ALOAD,0));method.instructions.set(m,call(ROOT+"VioletUi","descriptionText","(L"+m.owner+";Ljava/lang/String;FFLjava/awt/Color;FL"+BASE+"management/nanovg/font/Font;L"+category.name+";)V"));descriptionPending=false;descriptions++;}
     else if(moduleSlot>=0&&m.owner.equals(BASE+"management/nanovg/NanoVGManager")&&(m.name.equals("drawRoundedRect")||m.name.equals("drawGradientRoundedRect"))){
      int constants=0;AbstractInsnNode cursor=m.getPrevious();for(int back=0;cursor!=null&&back<24;back++,cursor=cursor.getPrevious()){if(cursor instanceof LdcInsnNode&&Float.valueOf(28).equals(((LdcInsnNode)cursor).cst))constants++;if(cursor instanceof MethodInsnNode&&((MethodInsnNode)cursor).owner.equals(m.owner))break;}
      if(constants>=2){method.instructions.insertBefore(m,new VarInsnNode(ALOAD,moduleSlot));method.instructions.set(m,call(ROOT+"VioletUi",m.name.equals("drawRoundedRect")?"tile":"gradientTile","(L"+m.owner+";FFFFFLjava/awt/Color;"+(m.name.equals("drawGradientRoundedRect")?"Ljava/awt/Color;":"")+"L"+BASE+"management/mods/Mod;)V"));tiles++;}
     }
    }
   }
  }
  if(descriptions!=2)throw new IllegalStateException("Expected two description lanes, found "+descriptions);
  if(tiles!=2)throw new IllegalStateException("Expected two module tile hooks, found "+tiles);
  System.out.println("Fitted description lanes: "+descriptions+"; Violet icon hooks: "+tiles);mark(category);

  ClassNode targetHud=node(files.get(BASE+"management/mods/impl/TargetInfoMod.class"));
  MethodNode targetRender=method(targetHud,"onRender2D");InsnList cleanup=new InsnList();cleanup.add(new VarInsnNode(ALOAD,0));cleanup.add(call(ROOT+"TargetDetails","context","(L"+targetHud.name+";)V"));targetRender.instructions.insert(cleanup);
  MethodNode targetDraw=method(targetHud,"drawNanoVG");for(AbstractInsnNode i:targetDraw.instructions.toArray())if(i.getOpcode()==RETURN){InsnList details=new InsnList();details.add(new VarInsnNode(ALOAD,0));details.add(call(ROOT+"TargetDetails","render","(L"+targetHud.name+";)V"));targetDraw.instructions.insertBefore(i,details);}mark(targetHud);

  files.put("mixins.violet.json",("{\"required\":false,\"minVersion\":\"0.7.11\",\"package\":\"me.movi.violet.mixin\",\"compatibilityLevel\":\"JAVA_8\",\"injectors\":{\"defaultRequire\":0},\"mixins\":[\"VioletInputMixin\",\"VioletKeyStateMixin\",\"VioletKeyBindingAccessor\",\"AssistControllerAccessor\",\"AssistMovementMixin\"]}").getBytes("UTF-8"));
  files.put("violet-patch-report.txt",String.join("\n",report).getBytes("UTF-8"));
  List<String> addedClasses=new ArrayList<>();for(String entry:added.keySet())if(entry.endsWith(".class"))addedClasses.add(entry.substring(0,entry.length()-6));
  files.put("violet-added-report.txt",String.join("\n",addedClasses).getBytes("UTF-8"));
  try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(Paths.get(args[3])))){for(Map.Entry<String,byte[]> e:files.entrySet()){String n=e.getKey();if(n.startsWith("META-INF/")&&(n.endsWith(".SF")||n.endsWith(".RSA")||n.endsWith(".DSA")))continue;z.putNextEntry(new ZipEntry(n));z.write(e.getValue());z.closeEntry();}}
  System.out.println("Violet built: "+report.size()+" baseline classes changed, "+added.size()+" compiled/replacement classes added.");
 }
}
