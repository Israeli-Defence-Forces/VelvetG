import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;

public final class LeanPatch extends BytecodeIO {
  static final String L = BASE + "lean/";
  static final Set<String> changed = new TreeSet<>();

  static ClassNode get(String n) {
    return node(files.get(BASE + n + ".class"));
  }

  static void put(ClassNode c) {
    save(c);
    changed.add(c.name + ".class");
  }

  static InsnList invoke(String n, String desc) {
    InsnList x = new InsnList();
    x.add(call(L + n.substring(0, n.indexOf('.')), n.substring(n.indexOf('.') + 1), desc));
    return x;
  }

  static void beforeReturn(MethodNode m, InsnList b) {
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i.getOpcode() == RETURN) {
        InsnList copy = new InsnList();
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode j : b.toArray()) copy.add(j.clone(labels));
        m.instructions.insertBefore(i, copy);
      }
  }

  static void addFrameHook(ClassNode c, String name, String at, String callName) {
    MethodNode m =
        new MethodNode(
            ACC_PRIVATE,
            name,
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
            null,
            null);
    AnnotationNode a = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Inject;");
    a.values = new ArrayList<Object>();
    a.values.add("method");
    a.values.add(Arrays.asList("runGameLoop"));
    AnnotationNode loc = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
    loc.values = new ArrayList<Object>();
    loc.values.add("value");
    loc.values.add(at);
    a.values.add("at");
    a.values.add(Arrays.asList(loc));
    m.visibleAnnotations = new ArrayList<AnnotationNode>();
    m.visibleAnnotations.add(a);
    m.instructions.add(call(L + "FrameProbe", callName, "()V"));
    m.instructions.add(new InsnNode(RETURN));
    c.methods.add(m);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4)
      throw new IllegalArgumentException(
          "Expected original.jar minecraft-1.8.9.jar additions.jar output.jar");
    if (!VerifyPatch.hex(Files.readAllBytes(Paths.get(args[0])))
        .equals("3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813"))
      throw new java.io.IOException("Unexpected Lean baseline");
    if (!VerifyPatch.hex(Files.readAllBytes(Paths.get(args[1])))
        .equals("14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298"))
      throw new java.io.IOException("Unexpected Minecraft base");
    if (Files.exists(Paths.get(args[3])))
      throw new java.io.IOException("Output already exists; refusing overwrite");
    load(args[0], files);
    load(args[1], mc);
    load(args[2], files);
    ClassNode c = get("management/screenshot/ScreenshotManager");
    MethodNode m = method(c, "<init>");
    InsnList b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
    for (String name : Arrays.asList("screenshots", "removeScreenshots")) {
      b.add(new VarInsnNode(ALOAD, 0));
      b.add(new TypeInsnNode(NEW, "java/util/concurrent/CopyOnWriteArrayList"));
      b.add(new InsnNode(DUP));
      b.add(
          new MethodInsnNode(
              INVOKESPECIAL, "java/util/concurrent/CopyOnWriteArrayList", "<init>", "()V", false));
      b.add(
          new FieldInsnNode(PUTFIELD, c.name, name, "Ljava/util/concurrent/CopyOnWriteArrayList;"));
    }
    b.add(new InsnNode(RETURN));
    body(m, b);
    put(c);
    c = get("Glide");
    m = method(c, "start");
    m.instructions.insert(invoke("InitProbe.begin", "()V"));
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i instanceof FieldInsnNode && i.getOpcode() == PUTFIELD) {
        b = new InsnList();
        b.add(new LdcInsnNode(((FieldInsnNode) i).name));
        b.add(call(L + "InitProbe", "mark", "(Ljava/lang/String;)V"));
        m.instructions.insert(i, b);
      }
    put(c);
    c = get("management/screenshot/LazyScreenshotLoader");
    m = method(c, "scan");
    b = new InsnList();
    for (int i = 0; i < 3; i++) b.add(new VarInsnNode(ALOAD, i));
    b.add(call(L + "GalleryIndex", "scan", m.desc));
    b.add(new InsnNode(RETURN));
    body(m, b);
    put(c);
    c = get("management/nanovg/asset/AssetManager");
    for (MethodNode mm : c.methods) {
      if (mm.name.equals("loadImage") && mm.desc.equals("(JLjava/io/File;)Z")) {
        b = new InsnList();
        b.add(new VarInsnNode(LLOAD, 1));
        b.add(new VarInsnNode(ALOAD, 3));
        b.add(call(L + "AsyncImages", "load", mm.desc));
        b.add(new InsnNode(IRETURN));
        body(mm, b);
      } else if (mm.name.equals("getImage") && mm.desc.equals("(Ljava/io/File;)I")) {
        b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 1));
        b.add(call(L + "AsyncImages", "image", mm.desc));
        b.add(new InsnNode(IRETURN));
        body(mm, b);
      } else if (mm.name.equals("removeImage") && mm.desc.equals("(JLjava/io/File;)V")) {
        b = new InsnList();
        b.add(new VarInsnNode(LLOAD, 1));
        b.add(new VarInsnNode(ALOAD, 3));
        b.add(call(L + "AsyncImages", "remove", mm.desc));
        b.add(new InsnNode(RETURN));
        body(mm, b);
      }
    }
    // STB buffer ownership is local to successful resource image creation.
    for (MethodNode mm : c.methods)
      if (mm.name.equals("loadImage") && mm.desc.equals("(JLjy;)Z")) {
        int buffer = -1;
        for (AbstractInsnNode i : mm.instructions.toArray()) {
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).name.equals("stbi_load_from_memory")) {
            AbstractInsnNode a = i.getNext();
            while (a != null && !(a instanceof VarInsnNode)) a = a.getNext();
            if (a instanceof VarInsnNode) buffer = ((VarInsnNode) a).var;
          }
          if (buffer >= 0
              && i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals("java/util/HashMap")
              && ((MethodInsnNode) i).name.equals("put")) {
            b = new InsnList();
            b.add(new VarInsnNode(ALOAD, buffer));
            b.add(call("org/lwjgl/stb/STBImage", "stbi_image_free", "(Ljava/nio/ByteBuffer;)V"));
            mm.instructions.insert(i, b);
          }
        }
      }
    put(c);
    c = get("management/nanovg/NanoVGManager");
    for (MethodNode mm : c.methods)
      if (mm.name.equals("drawPlayerHead") && mm.desc.equals("(Ljy;FFFFFF)V")) {
        LabelNode skip = new LabelNode();
        int close = 0;
        for (AbstractInsnNode i : mm.instructions.toArray()) {
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).name.equals("nvgClosePath")
              && ++close == 1) {
            b = new InsnList();
            b.add(new LdcInsnNode("headLayers"));
            b.add(call(L + "LeanSettings", "on", "(Ljava/lang/String;)Z"));
            b.add(new JumpInsnNode(IFEQ, skip));
            mm.instructions.insert(i, b);
          }
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i)
                  .name.equals("free")) { // put the join before loading the paint receiver
            AbstractInsnNode prev = i.getPrevious();
            mm.instructions.insertBefore(prev, skip);
            break;
          }
        }
      }
    put(c);
    c = get("scout/VelvetPikaAuth");
    m = method(c, "getKey");
    b = invoke("KeyVault.get", "()Ljava/lang/String;");
    b.add(new InsnNode(ARETURN));
    body(m, b);
    m = method(c, "normalizeMode");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(call(L + "KeyVault", "mode", m.desc));
    b.add(new InsnNode(ARETURN));
    body(m, b);
    put(c);
    c = get("scout/PikaApiClient$HttpTransport");
    m = method(c, "get");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 1));
    b.add(call(L + "LeanHttp", "get", m.desc));
    b.add(new InsnNode(ARETURN));
    body(m, b);
    put(c);
    c = get("scout/PikaApiClient");
    for (MethodNode mm : c.methods)
      for (AbstractInsnNode i : mm.instructions.toArray()) {
        if (i instanceof TypeInsnNode
            && ((TypeInsnNode) i).desc.equals("java/util/concurrent/PriorityBlockingQueue"))
          ((TypeInsnNode) i).desc = L + "BoundedPriorityQueue";
        if (i instanceof MethodInsnNode
            && ((MethodInsnNode) i).owner.equals("java/util/concurrent/PriorityBlockingQueue")
            && ((MethodInsnNode) i).name.equals("<init>"))
          ((MethodInsnNode) i).owner = L + "BoundedPriorityQueue";
        if (i instanceof MethodInsnNode) {
          MethodInsnNode a = (MethodInsnNode) i;
          if (a.owner.equals("java/util/concurrent/Executors")
              && a.name.equals(
                  "newFixedThreadPool")) { // remove constant worker count; retain original thread
            // factory
            mm.instructions.insertBefore(i, new InsnNode(SWAP));
            mm.instructions.insertBefore(i, new InsnNode(POP));
            a.owner = L + "TableSupport";
            a.name = "profileWorkers";
            a.desc = "(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;";
          }
          if (a.owner.equals("java/util/concurrent/ConcurrentHashMap") && a.name.equals("put")) {
            a.setOpcode(INVOKESTATIC);
            a.owner = L + "TableSupport";
            a.name = "boundedPut";
            a.desc =
                "(Ljava/util/concurrent/ConcurrentHashMap;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
          }
        }
      }
    put(c);
    c = get("scout/PikaApiClient");
    m = method(c, "detail");
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i.getOpcode() == ARETURN)
        m.instructions.insertBefore(
            i, call(L + "KeyVault", "redact", "(Ljava/lang/String;)Ljava/lang/String;"));
    m = method(c, "request");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 1));
    LabelNode nonnull = new LabelNode();
    b.add(new JumpInsnNode(IFNONNULL, nonnull));
    b.add(new LdcInsnNode(""));
    b.add(new VarInsnNode(ASTORE, 1));
    b.add(nonnull);
    m.instructions.insert(b);
    put(c);
    c = get("scout/PikaApiClient");
    for (FieldNode f : c.fields)
      if (f.name.equals("cacheEpoch") || f.name.equals("closed")) f.access |= ACC_VOLATILE;
    for (MethodNode mm : c.methods)
      if (mm.name.startsWith("lambda$scheduleProfileHydration$"))
        for (AbstractInsnNode i : mm.instructions.toArray())
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals("java/lang/Thread")
              && ((MethodInsnNode) i).name.equals("sleep")) {
            b = new InsnList();
            b.add(new VarInsnNode(ALOAD, 0));
            b.add(new FieldInsnNode(GETFIELD, c.name, "cacheEpoch", "J"));
            b.add(new VarInsnNode(LLOAD, 4));
            b.add(new InsnNode(LCMP));
            LabelNode keep = new LabelNode();
            b.add(new JumpInsnNode(IFEQ, keep));
            b.add(new InsnNode(RETURN));
            b.add(keep);
            LabelNode currentGeneration = new LabelNode();
            b.add(new VarInsnNode(LLOAD, 6));
            b.add(new InsnNode(LCONST_0));
            b.add(new InsnNode(LCMP));
            b.add(new JumpInsnNode(IFLT, currentGeneration));
            b.add(new VarInsnNode(LLOAD, 6));
            b.add(call(BASE + "scout/Scout105Runtime", "sessionGeneration", "()J"));
            b.add(new InsnNode(LCMP));
            b.add(new JumpInsnNode(IFEQ, currentGeneration));
            b.add(new InsnNode(RETURN));
            b.add(currentGeneration);
            mm.instructions.insert(i, b);
          }
    put(c);
    c = get("scout/ScoutState");
    for (FieldNode f : c.fields) if (f.name.equals("generation")) f.access |= ACC_VOLATILE;
    put(c);
    c = get("scout/PikaParser");
    m = method(c, "profile");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 1));
    b.add(new VarInsnNode(ALOAD, 2));
    b.add(
        call(
            L + "ProfileExtras",
            "record",
            "(Lcom/google/gson/JsonObject;L" + BASE + "scout/ScoutStats$Builder;)V"));
    beforeReturn(m, b);
    put(c);
    c = get("scout/v2/Velvet2Settings");
    m = method(c, "save");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "p", "Ljava/util/Properties;"));
    b.add(new FieldInsnNode(GETSTATIC, c.name, "FILE", "Ljava/io/File;"));
    b.add(call(L + "ConfigWriter", "enqueue", "(Ljava/util/Properties;Ljava/io/File;)V"));
    b.add(new InsnNode(RETURN));
    body(m, b);
    put(c);
    c = get("scout/Scout105Runtime");
    m = method(c, "save");
    b = new InsnList();
    b.add(new FieldInsnNode(GETSTATIC, c.name, "P", "Ljava/util/Properties;"));
    b.add(new FieldInsnNode(GETSTATIC, c.name, "PREFS_FILE", "Ljava/io/File;"));
    b.add(call(L + "ConfigWriter", "enqueue", "(Ljava/util/Properties;Ljava/io/File;)V"));
    b.add(new InsnNode(RETURN));
    body(m, b);
    put(c);
    c = get("scout/ui/ScoutTextMetrics");
    m = method(c, "centeredTop");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new VarInsnNode(ALOAD, 1));
    for (int i = 2; i <= 4; i++) b.add(new VarInsnNode(FLOAD, i));
    b.add(new VarInsnNode(ALOAD, 5));
    b.add(call(L + "TextCache", "center", m.desc));
    b.add(new InsnNode(FRETURN));
    body(m, b);
    put(c);
    c = get("scout/ScoutTable");
    for (MethodNode mm : c.methods) {
      if (mm.name.equals("<init>") && mm.desc.endsWith(";ZZ)V"))
        throw new IllegalStateException("Unexpected table descriptor");
      if (mm.name.equals("<init>")
          && mm.desc.equals(
              "(Ljava/util/List;L"
                  + BASE
                  + "scout/ScoutConfig;ZLjava/lang/String;Ljava/lang/String;ZL"
                  + BASE
                  + "scout/ScoutState$Phase;Z)V")) {
        b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 1));
        b.add(new VarInsnNode(ILOAD, 3));
        b.add(new VarInsnNode(ALOAD, 7));
        b.add(new VarInsnNode(ALOAD, 2));
        b.add(
            call(
                L + "TableSupport",
                "prepare",
                "(Ljava/util/List;ZL"
                    + BASE
                    + "scout/ScoutState$Phase;L"
                    + BASE
                    + "scout/ScoutConfig;)Ljava/util/List;"));
        b.add(new VarInsnNode(ASTORE, 1));
        mm.instructions.insert(b);
      }
      for (AbstractInsnNode i : mm.instructions.toArray()) {
        if (i instanceof LdcInsnNode && "WS".equals(((LdcInsnNode) i).cst))
          ((LdcInsnNode) i).cst = "BEST WS";
        if (i instanceof FieldInsnNode) {
          FieldInsnNode f = (FieldInsnNode) i;
          if (f.getOpcode() == PUTFIELD
              && f.owner.equals(c.name)
              && Arrays.asList("animations", "shadow").contains(f.name))
            mm.instructions.insertBefore(
                i,
                call(
                    L + "TableSupport",
                    f.name.equals("animations") ? "animations" : "cosmetic",
                    "(Z)Z"));
          if (f.getOpcode() == PUTFIELD && f.owner.equals(c.name) && f.name.equals("columns")) {
            b = new InsnList();
            b.add(new VarInsnNode(ILOAD, 3));
            b.add(call(L + "TableSupport", "columns", "(Ljava/util/List;Z)Ljava/util/List;"));
            mm.instructions.insertBefore(i, b);
          }
          if (f.getOpcode() == GETSTATIC
              && f.owner.equals(BASE + "management/nanovg/font/Fonts")
              && Arrays.asList("REGULAR", "MEDIUM").contains(f.name))
            mm.instructions.insert(
                i,
                call(
                    L + "TableSupport",
                    "font",
                    "(L"
                        + BASE
                        + "management/nanovg/font/Font;)L"
                        + BASE
                        + "management/nanovg/font/Font;"));
        }
        if (i instanceof MethodInsnNode) {
          MethodInsnNode a = (MethodInsnNode) i;
          if (a.owner.equals(BASE + "management/nanovg/NanoVGManager")
              && a.name.equals("getTextWidth")) {
            a.setOpcode(INVOKESTATIC);
            a.owner = L + "TextCache";
            a.name = "width";
            a.desc =
                "(L"
                    + BASE
                    + "management/nanovg/NanoVGManager;Ljava/lang/String;FL"
                    + BASE
                    + "management/nanovg/font/Font;)F";
          }
          if (a.owner.equals(BASE + "scout/ui/ScoutNumberFormatter") && a.name.equals("stat"))
            a.owner = L + "TableSupport";
        }
      }
    }
    // Wrap drawing with try/finally so profiling context cannot leak across HUD/TAB failures.
    m = method(c, "draw");
    m.name = "lean$draw";
    MethodNode wrap = new MethodNode(ACC_PUBLIC, "draw", m.desc, null, null);
    b = wrap.instructions;
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "hud", "Z"));
    b.add(call(L + "TableSupport", "begin", "(Z)J"));
    b.add(new VarInsnNode(LSTORE, 4));
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "hud", "Z"));
    for (int k = 1; k <= 3; k++) b.add(new VarInsnNode(FLOAD, k));
    for (String f : Arrays.asList("width", "height")) {
      b.add(new VarInsnNode(ALOAD, 0));
      b.add(new FieldInsnNode(GETFIELD, c.name, f, "F"));
    }
    b.add(call(L + "TableEffects", "prepare", "(ZFFFFF)V"));
    for (int k = 1; k <= 3; k++) {
      b.add(
          new FieldInsnNode(
              GETSTATIC, L + "TableEffects", k == 1 ? "x" : k == 2 ? "y" : "scale", "F"));
      b.add(new VarInsnNode(FSTORE, k));
    }
    LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode();
    b.add(start);
    b.add(new VarInsnNode(ALOAD, 0));
    for (int i = 1; i <= 3; i++) b.add(new VarInsnNode(FLOAD, i));
    b.add(new MethodInsnNode(INVOKEVIRTUAL, c.name, "lean$draw", m.desc, false));
    b.add(end);
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "hud", "Z"));
    b.add(new VarInsnNode(LLOAD, 4));
    b.add(call(L + "FrameProbe", "section", "(ZJ)V"));
    b.add(new InsnNode(RETURN));
    b.add(handler);
    b.add(new VarInsnNode(ASTORE, 6));
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "hud", "Z"));
    b.add(new VarInsnNode(LLOAD, 4));
    b.add(call(L + "FrameProbe", "section", "(ZJ)V"));
    b.add(new VarInsnNode(ALOAD, 6));
    b.add(new InsnNode(ATHROW));
    wrap.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
    c.methods.add(wrap);
    put(c);
    c = get("scout/ui/ScoutLayoutMetrics");
    m = method(c, "<init>");
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i instanceof FieldInsnNode
          && i.getOpcode() == PUTFIELD
          && ((FieldInsnNode) i).name.equals("pad"))
        m.instructions.insertBefore(i, call(L + "TableSupport", "spacing", "(F)F"));
    put(c);
    c = get("scout/ui/ScoutPanelRenderer");
    m = method(c, "panel");
    b = new InsnList();
    b.add(new LdcInsnNode("maximumFPS"));
    b.add(call(L + "LeanSettings", "on", "(Ljava/lang/String;)Z"));
    LabelNode fullPanel = new LabelNode();
    b.add(new JumpInsnNode(IFEQ, fullPanel));
    b.add(new VarInsnNode(ALOAD, 0));
    for (int k = 1; k <= 5; k++) b.add(new VarInsnNode(FLOAD, k));
    b.add(new VarInsnNode(ALOAD, 6));
    b.add(
        new FieldInsnNode(
            GETFIELD, BASE + "scout/ui/ScoutPalette", "background", "Ljava/awt/Color;"));
    b.add(
        new MethodInsnNode(
            INVOKEVIRTUAL,
            BASE + "management/nanovg/NanoVGManager",
            "drawRoundedRect",
            "(FFFFFLjava/awt/Color;)V",
            false));
    b.add(new InsnNode(RETURN));
    b.add(fullPanel);
    m.instructions.insert(b);
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    for (int k = 1; k <= 5; k++) b.add(new VarInsnNode(FLOAD, k));
    b.add(
        call(
            L + "TableEffects",
            "background",
            "(L" + BASE + "management/nanovg/NanoVGManager;FFFFF)V"));
    beforeReturn(m, b);
    put(c);
    c = get("management/mods/impl/ScoutMod");
    m = method(c, "onSendChat");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 1));
    b.add(call(L + "NativeHooks", "chat", "(L" + BASE + "management/event/impl/EventSendChat;)Z"));
    LabelNode next = new LabelNode();
    b.add(new JumpInsnNode(IFEQ, next));
    b.add(new InsnNode(RETURN));
    b.add(next);
    m.instructions.insert(b);
    // Change only the renderTab lambda's final x/y arguments, preserving the existing fitting
    // calculation.
    for (MethodNode mm : c.methods)
      for (AbstractInsnNode i : mm.instructions.toArray())
        if (i instanceof MethodInsnNode
            && ((MethodInsnNode) i).owner.equals(BASE + "scout/ScoutTable")
            && ((MethodInsnNode) i).name.equals("draw")
            && mm.name.contains("renderTab")) {
          b = new InsnList();
          b.add(new VarInsnNode(FSTORE, mm.maxLocals));
          b.add(call(L + "TableSupport", "tabY", "(F)F"));
          b.add(new VarInsnNode(FSTORE, mm.maxLocals + 1));
          b.add(call(L + "TableSupport", "tabX", "(F)F"));
          b.add(new VarInsnNode(FLOAD, mm.maxLocals + 1));
          b.add(new VarInsnNode(FLOAD, mm.maxLocals));
          mm.maxLocals += 2;
          mm.instructions.insertBefore(i, b);
        }
    put(c);
    c = get("injection/mixin/mixins/client/MixinMinecraft");
    method(c, "onTick");
    for (MethodNode mm : c.methods) {
      if (mm.name.equals("onTick")) mm.instructions.insert(invoke("NativeHooks.tick", "()V"));
      if (mm.name.equals("postRenderTick"))
        mm.instructions.insert(invoke("NativeHooks.frame", "()V"));
      if (mm.name.equals("preShutdown"))
        mm.instructions.insert(invoke("NativeHooks.shutdown", "()V"));
    }
    addFrameHook(c, "lean$cpuStart", "HEAD", "cpuStart");
    addFrameHook(c, "lean$cpuEnd", "RETURN", "cpuEnd");
    put(c);
    c = get("injection/mixin/mixins/network/MixinNetworkManager");
    m = method(c, "preChannelRead0");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 2));
    b.add(call(L + "NativeHooks", "packet", "(Ljava/lang/Object;)Z"));
    next = new LabelNode();
    b.add(new JumpInsnNode(IFEQ, next));
    b.add(new VarInsnNode(ALOAD, 3));
    b.add(
        new MethodInsnNode(
            INVOKEVIRTUAL,
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfo",
            "cancel",
            "()V",
            false));
    b.add(new InsnNode(RETURN));
    b.add(next);
    m.instructions.insert(b);
    put(c);
    c = get("injection/mixin/mixins/render/MixinRendererLivingEntity");
    m = method(c, "preRenderName");
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i instanceof MethodInsnNode
          && ((MethodInsnNode) i).owner.equals("eu")
          && ((MethodInsnNode) i).name.equals("d")) {
        b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 1));
        b.add(
            call(
                L + "NativeHooks",
                "nametag",
                "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;"));
        m.instructions.insert(i, b);
      }
    put(c);
    c = get("injection/mixin/mixins/render/MixinRendererLivingEntity");
    m = method(c, "preRenderName");
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i instanceof MethodInsnNode
          && ((MethodInsnNode) i).owner.equals("bfl")
          && ((MethodInsnNode) i).name.equals("a")
          && ((MethodInsnNode) i).desc.equals("(IF)V")) {
        b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new VarInsnNode(ALOAD, 1));
        b.add(new VarInsnNode(DLOAD, 2));
        b.add(new VarInsnNode(DLOAD, 4));
        b.add(new VarInsnNode(DLOAD, 6));
        b.add(new MethodInsnNode(INVOKESPECIAL, c.name, "lean$above", "(Lpr;DDD)V", false));
        m.instructions.insert(i, b);
        break;
      }
    MethodNode above = new MethodNode(ACC_PRIVATE, "lean$above", "(Lpr;DDD)V", null, null);
    b = above.instructions;
    b.add(new VarInsnNode(ALOAD, 1));
    b.add(call(L + "NativeHooks", "above", "(Ljava/lang/Object;)Ljava/lang/String;"));
    b.add(new VarInsnNode(ASTORE, 8));
    b.add(new VarInsnNode(ALOAD, 8));
    LabelNode noTag = new LabelNode();
    b.add(new JumpInsnNode(IFNULL, noTag));
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new VarInsnNode(ALOAD, 1));
    b.add(new VarInsnNode(ALOAD, 8));
    b.add(new VarInsnNode(DLOAD, 2));
    b.add(new VarInsnNode(DLOAD, 4));
    b.add(new LdcInsnNode(0.28D));
    b.add(new InsnNode(DADD));
    b.add(new VarInsnNode(DLOAD, 6));
    b.add(new IntInsnNode(BIPUSH, 64));
    b.add(new MethodInsnNode(INVOKEVIRTUAL, "biv", "a", "(Lpk;Ljava/lang/String;DDDI)V", false));
    b.add(noTag);
    b.add(new InsnNode(RETURN));
    c.methods.add(above);
    put(c);
    // Guard the observed null-pipe crash using real status; retain connected close and IOException
    // handling.
    c = get("discord/ipc/IPCClient");
    m = method(c, "close");
    LabelNode closeBody = null;
    for (AbstractInsnNode i : m.instructions.toArray())
      if (i instanceof JumpInsnNode) {
        closeBody = ((JumpInsnNode) i).label;
        break;
      }
    if (closeBody == null) throw new IllegalStateException("IPC close guard missing");
    while (m.instructions.getFirst() != closeBody) m.instructions.remove(m.instructions.getFirst());
    m.localVariables = null;
    m.visibleLocalVariableAnnotations = null;
    m.invisibleLocalVariableAnnotations = null;
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(
        new FieldInsnNode(
            GETFIELD, c.name, "pipe", "L" + BASE + "discord/ipc/entities/pipe/Pipe;"));
    next = new LabelNode();
    b.add(new JumpInsnNode(IFNONNULL, next));
    b.add(new InsnNode(RETURN));
    b.add(next);
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(
        new MethodInsnNode(
            INVOKEVIRTUAL,
            c.name,
            "getStatus",
            "()L" + BASE + "discord/ipc/entities/pipe/PipeStatus;",
            false));
    b.add(
        new FieldInsnNode(
            GETSTATIC,
            BASE + "discord/ipc/entities/pipe/PipeStatus",
            "CONNECTED",
            "L" + BASE + "discord/ipc/entities/pipe/PipeStatus;"));
    LabelNode connectedPipe = new LabelNode();
    b.add(new JumpInsnNode(IF_ACMPEQ, connectedPipe));
    b.add(new InsnNode(RETURN));
    b.add(connectedPipe);
    m.instructions.insert(b);
    put(c);
    c = get("discord/DiscordRPC");
    m = method(c, "stop");
    b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "client", "L" + BASE + "discord/ipc/IPCClient;"));
    next = new LabelNode();
    b.add(new JumpInsnNode(IFNONNULL, next));
    b.add(new InsnNode(RETURN));
    b.add(next);
    m.instructions.insert(b);
    put(c);
    StageTwo.apply();
    try (ZipOutputStream out =
        new ZipOutputStream(
            Files.newOutputStream(
                Paths.get(args[3]), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
      for (Map.Entry<String, byte[]> e : files.entrySet()) {
        ZipEntry z = new ZipEntry(e.getKey());
        z.setTime(0);
        out.putNextEntry(z);
        out.write(e.getValue());
        out.closeEntry();
      }
    }
    for (String x : changed) System.out.println(x);
  }
}
