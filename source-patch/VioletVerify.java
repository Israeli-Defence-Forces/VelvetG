import java.util.*;import java.util.zip.*;import java.io.*;import org.spongepowered.asm.lib.*;import org.spongepowered.asm.lib.tree.*;import org.spongepowered.asm.lib.tree.analysis.*;
public class VioletVerify {
 static final String ROOT="me/movi/violet/";
 static boolean methodExists(Map<String,ClassNode> classes,String owner,String name,String desc)throws Exception {
  ClassNode n=classes.get(owner);
  if(n==null){Class<?> type=Class.forName(owner.replace('/','.'),false,VioletVerify.class.getClassLoader());for(java.lang.reflect.Method m:type.getMethods())if(m.getName().equals(name)&&Type.getMethodDescriptor(m).equals(desc))return true;return false;}
  for(MethodNode m:n.methods)if(m.name.equals(name)&&m.desc.equals(desc))return true;
  if(name.equals("<init>"))return false;
  if(n.superName!=null&&methodExists(classes,n.superName,name,desc))return true;
  for(String i:n.interfaces)if(methodExists(classes,i,name,desc))return true;
  return false;
 }
 static Set<String> report(ZipFile z,String name)throws Exception{
  ZipEntry e=z.getEntry(name);Set<String> out=new HashSet<>();if(e==null)return out;
  String s=new String(read(z.getInputStream(e)),"UTF-8");for(String line:s.split("\\n")){line=line.trim();if(!line.isEmpty())out.add(line);}return out;
 }
 static Map<String,byte[]> entries(ZipFile z)throws Exception{
  Map<String,byte[]> out=new HashMap<>();for(ZipEntry e:Collections.list(z.entries()))if(!e.isDirectory())out.put(e.getName(),read(z.getInputStream(e)));return out;
 }
 public static void main(String[] args)throws Exception{
  int checked=0,parsed=0,links=0,fonts=0,untouched=0;
  try(ZipFile z=new ZipFile(args[0])){
   Set<String> changed=report(z,"violet-patch-report.txt");changed.addAll(report(z,"violet-added-report.txt"));
   Map<String,byte[]> targetEntries=entries(z);Map<String,ClassNode> classes=new HashMap<>();
   for(Map.Entry<String,byte[]> e:targetEntries.entrySet())if(e.getKey().endsWith(".class")){ClassNode n=new ClassNode();new ClassReader(e.getValue()).accept(n,0);classes.put(n.name,n);parsed++;}
   // Only JVM-load classes r8 changed or added. Some inherited LaunchWrapper-era classes are not
   // valid to reflectively load in isolation before the game's transformers run.
   for(String name:changed){ClassNode n=classes.get(name);if(n==null)throw new AssertionError("Reported class missing: "+name);if(n.version>52)throw new AssertionError("Not Java8: "+n.name);for(MethodNode m:n.methods)if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(n.name,m);Class<?> c=Class.forName(n.name.replace('/','.'),false,VioletVerify.class.getClassLoader());c.getDeclaredMethods();c.getDeclaredConstructors();checked++;}
   for(ClassNode n:classes.values())for(MethodNode m:n.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)i;if(call.owner.startsWith(ROOT)){if(!methodExists(classes,call.owner,call.name,call.desc))throw new AssertionError("Missing Violet method from "+n.name+"."+m.name+": "+call.owner+"."+call.name+call.desc);links++;}}
   // Prove every baseline class not explicitly changed/replaced is byte-for-byte untouched.
   if(args.length>1)try(ZipFile base=new ZipFile(args[1])){Map<String,byte[]> baseEntries=entries(base);for(Map.Entry<String,byte[]> e:baseEntries.entrySet()){String entry=e.getKey();if(!entry.endsWith(".class"))continue;String name=entry.substring(0,entry.length()-6);if(changed.contains(name))continue;byte[] now=targetEntries.get(entry);if(now==null)throw new AssertionError("Untouched baseline class missing: "+entry);if(!Arrays.equals(e.getValue(),now))throw new AssertionError("Unexpected rewrite of untouched baseline class: "+entry);untouched++;}}
   ClassNode fontClass=classes.get("me/eldodebug/soar/management/nanovg/font/Fonts");if(fontClass==null)throw new AssertionError("Fonts class missing");for(MethodNode m:fontClass.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof LdcInsnNode&&((LdcInsnNode)i).cst instanceof String){String path=(String)((LdcInsnNode)i).cst;if(path.startsWith("soar/fonts/")){if(z.getEntry("assets/minecraft/"+path)==null)throw new AssertionError("Missing UI font: "+path);fonts++;}}
   System.out.println("PASS: "+checked+" changed/new Java 8 classes JVM-verified; "+untouched+" inherited classes proven byte-identical; "+parsed+" classes structurally parsed; "+links+" Violet method references resolved; "+fonts+" UI font resources present");
  }
 }
 static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toByteArray();}
}
