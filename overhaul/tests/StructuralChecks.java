package me.eldodebug.soar.lean.tests;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.lib.tree.analysis.*;

public final class StructuralChecks {
  static ClassNode node(byte[] b) {
    ClassNode c = new ClassNode();
    new ClassReader(b).accept(c, 0);
    return c;
  }

  static byte[] bytes(ZipFile z, String n) throws IOException {
    try (InputStream in = z.getInputStream(z.getEntry(n));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[8192];
      int x;
      while ((x = in.read(b)) != -1) out.write(b, 0, x);
      return out.toByteArray();
    }
  }

  static byte[] methodCode(ClassNode c, String n) {
    ClassNode d = new ClassNode();
    d.name = "Normalized";
    d.superName = "java/lang/Object";
    d.version = 52;
    d.access = Opcodes.ACC_PUBLIC;
    for (MethodNode m : c.methods)
      if (m.name.equals(n)) {
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof FrameNode) m.instructions.remove(i);
        m.maxStack = 0;
        m.maxLocals = 0;
        d.methods.add(m);
      }
    ClassWriter w = new ClassWriter(0);
    d.accept(w);
    return w.toByteArray();
  }

  public static void main(String[] args) throws Exception {
    int verified = 0, preserved = 0, parsed = 0, links = 0;
    Map<String, ClassNode> classes = new HashMap<>();
    try (ZipFile now = new ZipFile(args[0]);
        ZipFile old = new ZipFile(args[1])) {
      Set<String> names = new HashSet<>();
      for (ZipEntry e : Collections.list(now.entries())) {
        if (!names.add(e.getName())) throw new AssertionError("Duplicate " + e.getName());
        if (e.getName().endsWith(".class")) {
          ClassNode c = node(bytes(now, e.getName()));
          classes.put(c.name, c);
          parsed++;
        }
      }
      for (ZipEntry e : Collections.list(old.entries()))
        if (now.getEntry(e.getName()) == null)
          throw new AssertionError("Deleted baseline entry: " + e.getName());
      for (ZipEntry e : Collections.list(now.entries())) {
        if (!e.getName().endsWith(".class")) continue;
        byte[] b = bytes(now, e.getName());
        if (old.getEntry(e.getName()) != null && Arrays.equals(b, bytes(old, e.getName()))) {
          preserved++;
          continue;
        }
        ClassNode c = classes.get(e.getName().replace(".class", ""));
        if (c.version > 52) throw new AssertionError("Java 8 incompatible: " + c.name);
        for (MethodNode m : c.methods)
          if ((m.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0)
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(c.name, m);
        Class<?> t =
            Class.forName(c.name.replace('/', '.'), false, StructuralChecks.class.getClassLoader());
        t.getDeclaredMethods();
        t.getDeclaredFields();
        t.getDeclaredConstructors();
        verified++;
        for (MethodNode m : c.methods)
          for (AbstractInsnNode i : m.instructions.toArray())
            if (i instanceof MethodInsnNode) {
              MethodInsnNode a = (MethodInsnNode) i;
              if (a.owner.startsWith("me/eldodebug/soar/lean/")) {
                ClassNode dest = classes.get(a.owner);
                boolean found = false;
                if (dest != null)
                  for (MethodNode mm : dest.methods)
                    if (mm.name.equals(a.name) && mm.desc.equals(a.desc)) found = true;
                if (!found) {
                  Class<?> owner =
                      Class.forName(
                          a.owner.replace('/', '.'),
                          false,
                          StructuralChecks.class.getClassLoader());
                  for (java.lang.reflect.Method mm : owner.getMethods())
                    if (mm.getName().equals(a.name) && Type.getMethodDescriptor(mm).equals(a.desc))
                      found = true;
                }
                if (!found)
                  throw new AssertionError(
                      "Broken new method descriptor " + a.owner + "." + a.name + a.desc);
                links++;
              }
            }
      }
      String p = "me/eldodebug/soar/scout/PikaApiClient.class";
      if (!Arrays.equals(
          methodCode(node(bytes(now, p)), "json"), methodCode(node(bytes(old, p)), "json")))
        throw new AssertionError("Previous json VerifyError repair instructions changed");
      ClassNode ctor = classes.get("me/eldodebug/soar/management/screenshot/ScreenshotManager");
      for (MethodNode m : ctor.methods)
        if (m.name.equals("<init>"))
          for (AbstractInsnNode i : m.instructions.toArray())
            if (i instanceof MethodInsnNode) {
              String name = ((MethodInsnNode) i).name;
              if (Arrays.asList("listFiles", "delete", "loadData", "loadImage", "openConnection")
                  .contains(name)) throw new AssertionError("Startup screenshot I/O regression");
            }
      System.out.println(
          "PASS "
              + verified
              + " modified/new Java 8 classes JVM and ASM verified; "
              + preserved
              + " inherited classes byte-identical; "
              + parsed
              + " classes parsed; "
              + links
              + " new call descriptors resolved; no baseline entries deleted; JSON repair"
              + " instructions unchanged");
    }
  }
}
