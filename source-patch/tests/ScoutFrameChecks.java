import java.util.*;
import java.util.zip.*;
import java.io.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;

/** Ensure the historical Scout repair changes verification metadata, not behavior. */
public final class ScoutFrameChecks {
    private static byte[] read(ZipFile jar,String name) throws IOException {
        try(InputStream in=jar.getInputStream(jar.getEntry(name));ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();
        }
    }
    private static byte[] withoutFrames(byte[] bytes) {
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,ClassReader.SKIP_FRAMES);
        for(MethodNode method:node.methods){method.maxStack=0;method.maxLocals=0;}
        ClassWriter writer=new ClassWriter(0);node.accept(writer);return writer.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        String name="me/eldodebug/soar/scout/PikaApiClient.class";
        try(ZipFile patched=new ZipFile(args[0]);ZipFile baseline=new ZipFile(args[1])) {
            byte[] now=read(patched,name),before=read(baseline,name);
            if(Arrays.equals(now,before))throw new AssertionError("Scout must be repaired, not kept unchanged");
            if(!Arrays.equals(withoutFrames(now),withoutFrames(before)))throw new AssertionError("Scout instructions, members or debug metadata changed");
            ClassNode node=new ClassNode();new ClassReader(now).accept(node,0);boolean repaired=false;
            for(MethodNode method:node.methods)if(method.name.equals("json"))for(AbstractInsnNode i:method.instructions.toArray())if(i instanceof FrameNode) {
                FrameNode frame=(FrameNode)i;
                if(frame.stack!=null&&frame.stack.size()==1&&"java/lang/RuntimeException".equals(frame.stack.get(0)))repaired=true;
            }
            if(!repaired)throw new AssertionError("JSON exception handler lacks a RuntimeException frame");
        }
        System.out.println("PASS: 3 Scout repair checks; original behavior preserved, JSON catch frame repaired");
    }
}
