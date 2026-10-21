package tools;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;

/** Structural verification only: it is not a LaunchWrapper/Mixin application test. */
public final class VerifyBindings {
    private static ClassNode read(ZipFile z, String name) throws Exception {
        ZipEntry e=z.getEntry(name+".class"); if(e==null)throw new IllegalStateException("Missing class "+name);
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        try(InputStream in=z.getInputStream(e)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);}
        ClassNode c=new ClassNode(); new ClassReader(b.toByteArray()).accept(c,0);return c;
    }
    private static void method(ZipFile z,String owner,String name,String desc)throws Exception {
        for(MethodNode m:read(z,owner).methods)if(m.name.equals(name)&&m.desc.equals(desc))return;
        throw new IllegalStateException("Missing target "+owner+"."+name+desc);
    }
    private static void field(ZipFile z,String owner,String name,String desc)throws Exception {
        for(FieldNode f:read(z,owner).fields)if(f.name.equals(name)&&f.desc.equals(desc))return;
        throw new IllegalStateException("Missing field "+owner+"."+name+":"+desc);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("minecraft.jar");
        byte[] b=Files.readAllBytes(Paths.get(args[0]));StringBuilder hash=new StringBuilder();
        for(byte x:MessageDigest.getInstance("SHA-256").digest(b))hash.append(String.format("%02x",x&255));
        if(!hash.toString().equals("14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298"))throw new SecurityException("Unknown Minecraft binary");
        try(ZipFile z=new ZipFile(args[0])){
            method(z,"bew","d","(Z)V"); method(z,"bew","m","()V");
            method(z,"ave","aw","()V"); method(z,"ave","ax","()V");
            method(z,"ave","a","(Laxu;)V"); method(z,"ave","n","()V"); method(z,"ave","o","()V");
            for(String p:new String[]{"hm","hf","fi","gv","hz"})method(z,"bcy","a","(L"+p+";)V");
            method(z,"pk","aw","()Z");method(z,"pk","av","()Z");method(z,"pk","F","()I");method(z,"wn","bW","()Z");
            method(z,"avb","i","()I");method(z,"avb","d","()Z");
            field(z,"ave","t","Lavh;");field(z,"avh","Y","Lavb;");
            field(z,"pk","C","Z");field(z,"ave","w","Z");
            field(z,"is","a","I");field(z,"is","b","Lis$a;");
            field(z,"in","a","I");field(z,"in","b","Lin$a;");field(z,"ir","c","Lir$a;");field(z,"ja","c","I");
            for(String p:new String[]{"hm","hz"})for(String f:new String[]{"a","b","c","d"})field(z,p,f,"I");
            field(z,"hf","a","I");field(z,"hf","b","B");field(z,"gv","a","I");
            for(String f:new String[]{"b","c","d"})field(z,"gv",f,"B");
            for(String f:new String[]{"a","b","c"})field(z,"fi",f,"D");
            Map<String,List<String>> registry=new HashMap<String,List<String>>();registry.put("a",new ArrayList<String>());registry.put("b",new ArrayList<String>());
            String direction=null;
            for(MethodNode m:read(z,"el$2").methods)if(m.name.equals("<init>"))for(AbstractInsnNode n=m.instructions.getFirst();n!=null;n=n.getNext()){
                if(n instanceof FieldInsnNode){FieldInsnNode f=(FieldInsnNode)n;if(f.owner.equals("fg"))direction=f.name;}
                if(n instanceof LdcInsnNode && ((LdcInsnNode)n).cst instanceof Type && direction!=null)
                    registry.get(direction).add(((Type)((LdcInsnNode)n).cst).getInternalName());
            }
            String[][] expected={{"a","2","in"},{"a","7","ir"},{"a","8","ja"},{"a","11","is"},
                {"b","18","hm"},{"b","25","hf"},{"b","20","gv"},{"b","24","hz"},{"b","8","fi"}};
            for(String[] r:expected)if(!registry.get(r[0]).get(Integer.parseInt(r[1])).equals(r[2]))throw new IllegalStateException("Registry mismatch "+Arrays.toString(r));
        }
        System.out.println("PASS: pinned binary, hook/binding method targets, packet layouts and PLAY protocol registration. Structural checks only.");
    }
}

