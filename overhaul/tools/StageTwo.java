import java.util.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;

/** Additional narrow transforms; never regenerates baseline classes from decompiled source. */
public final class StageTwo extends BytecodeIO {
  static final String L = BASE + "lean/";

  static ClassNode get(String n) {
    return LeanPatch.get(n);
  }

  static void put(ClassNode c) {
    LeanPatch.put(c);
  }

  static InsnList active(ClassNode c) {
    InsnList b = new InsnList();
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "closed", "Z"));
    b.add(new VarInsnNode(ALOAD, 0));
    b.add(new FieldInsnNode(GETFIELD, c.name, "cacheEpoch", "J"));
    b.add(new VarInsnNode(LLOAD, 4));
    b.add(new VarInsnNode(LLOAD, 6));
    b.add(call(L + "ProfileRequests", "active", "(ZJJJ)Z"));
    return b;
  }

  static void apply() {
    profiles();
    tables();
    rows();
    capes();
    party();
  }

  static void profiles() {
    ClassNode c = get("scout/PikaApiClient");
    c.fields.add(
        new FieldNode(
            ACC_PRIVATE | ACC_FINAL, "lean$profiles", "L" + L + "ProfileRequests;", null, null));
    int initialized = 0, wrapped = 0;
    for (MethodNode m : c.methods) {
      if (m.name.equals("<init>"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals("java/lang/Object")
              && ((MethodInsnNode) i).name.equals("<init>")) {
            InsnList b = new InsnList();
            b.add(new VarInsnNode(ALOAD, 0));
            b.add(new TypeInsnNode(NEW, L + "ProfileRequests"));
            b.add(new InsnNode(DUP));
            b.add(new MethodInsnNode(INVOKESPECIAL, L + "ProfileRequests", "<init>", "()V", false));
            b.add(
                new FieldInsnNode(PUTFIELD, c.name, "lean$profiles", "L" + L + "ProfileRequests;"));
            m.instructions.insert(i, b);
            initialized++;
            break;
          }
      if (m.name.equals("close") || m.name.equals("clear")) {
        InsnList b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new FieldInsnNode(GETFIELD, c.name, "lean$profiles", "L" + L + "ProfileRequests;"));
        b.add(new MethodInsnNode(INVOKEVIRTUAL, L + "ProfileRequests", "clear", "()V", false));
        m.instructions.insert(b);
      }
      if (m.name.equals("scheduleProfileHydration"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode && ((MethodInsnNode) i).name.equals("execute")) {
            InsnList b = new InsnList();
            b.add(new VarInsnNode(ALOAD, 0));
            b.add(
                new FieldInsnNode(GETFIELD, c.name, "lean$profiles", "L" + L + "ProfileRequests;"));
            b.add(new VarInsnNode(ALOAD, 1));
            b.add(new VarInsnNode(ALOAD, 2));
            b.add(new VarInsnNode(LLOAD, 3));
            b.add(new VarInsnNode(LLOAD, 5));
            m.instructions.insertBefore(i, b);
            MethodInsnNode a = (MethodInsnNode) i;
            a.setOpcode(INVOKESTATIC);
            a.itf = false;
            a.owner = L + "ProfileRequests";
            a.name = "execute";
            a.desc =
                "(Ljava/util/concurrent/ExecutorService;Ljava/lang/Runnable;L"
                    + L
                    + "ProfileRequests;Ljava/lang/String;Ljava/lang/String;JJ)V";
            wrapped++;
          }
      if (m.name.startsWith("lambda$scheduleProfileHydration$")) {
        int cached = m.maxLocals++;
        InsnList b = active(c);
        LabelNode valid = new LabelNode();
        b.add(new JumpInsnNode(IFNE, valid));
        b.add(new InsnNode(RETURN));
        b.add(valid);
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(
            new FieldInsnNode(
                GETFIELD, c.name, "profileCache", "Ljava/util/concurrent/ConcurrentHashMap;"));
        b.add(new VarInsnNode(ALOAD, 2));
        b.add(
            new MethodInsnNode(
                INVOKEVIRTUAL,
                "java/util/concurrent/ConcurrentHashMap",
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                false));
        b.add(new TypeInsnNode(CHECKCAST, BASE + "scout/ScoutStats"));
        b.add(new VarInsnNode(ASTORE, cached));
        LabelNode fetch = new LabelNode();
        b.add(new VarInsnNode(ALOAD, cached));
        b.add(new JumpInsnNode(IFNULL, fetch));
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new FieldInsnNode(GETFIELD, c.name, "clock", "Ljava/util/function/LongSupplier;"));
        b.add(
            new MethodInsnNode(
                INVOKEINTERFACE, "java/util/function/LongSupplier", "getAsLong", "()J", true));
        b.add(new VarInsnNode(ALOAD, cached));
        b.add(new FieldInsnNode(GETFIELD, BASE + "scout/ScoutStats", "timestamp", "J"));
        b.add(new InsnNode(LSUB));
        b.add(new LdcInsnNode(600000L));
        b.add(new InsnNode(LCMP));
        b.add(new JumpInsnNode(IFGT, fetch));
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new VarInsnNode(ALOAD, 3));
        b.add(new VarInsnNode(ALOAD, cached));
        b.add(new VarInsnNode(LLOAD, 4));
        b.add(new VarInsnNode(LLOAD, 6));
        b.add(
            new MethodInsnNode(
                INVOKESPECIAL,
                c.name,
                "mergeProfileIntoCache",
                "(Ljava/lang/String;L" + BASE + "scout/ScoutStats;JJ)V",
                false));
        b.add(new InsnNode(RETURN));
        b.add(fetch);
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new FieldInsnNode(GETFIELD, c.name, "lean$profiles", "L" + L + "ProfileRequests;"));
        b.add(new VarInsnNode(ALOAD, 2));
        b.add(new VarInsnNode(LLOAD, 4));
        b.add(new VarInsnNode(LLOAD, 6));
        b.add(
            new MethodInsnNode(
                INVOKEVIRTUAL,
                L + "ProfileRequests",
                "allowFetch",
                "(Ljava/lang/String;JJ)Z",
                false));
        LabelNode admitted = new LabelNode();
        b.add(new JumpInsnNode(IFNE, admitted));
        b.add(new InsnNode(RETURN));
        b.add(admitted);
        m.instructions.insert(b);
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals(BASE + "scout/PikaParser")
              && ((MethodInsnNode) i).name.equals("profile")) {
            b = active(c);
            LabelNode keep = new LabelNode();
            b.add(new JumpInsnNode(IFNE, keep));
            b.add(new InsnNode(POP2));
            b.add(new InsnNode(POP));
            b.add(new InsnNode(RETURN));
            b.add(keep);
            m.instructions.insertBefore(i, b);
          }
      }
    }
    if (initialized != 1 || wrapped != 1)
      throw new IllegalStateException("Unexpected profile executor shape");
    put(c);
  }

  static void tables() {
    ClassNode c = get("scout/ScoutTable");
    Map<String, String> flags = new HashMap<>();
    flags.put("heads", "Heads");
    flags.put("header", "Header");
    flags.put("combineRank", "RankWithName");
    flags.put("alternating", "AlternatingRows");
    flags.put("dividers", "Dividers");
    flags.put("shadow", "Shadow");
    for (MethodNode m : c.methods) {
      if (m.name.equals("<init>") && m.desc.endsWith("ScoutState$Phase;Z)V")) {
        InsnList b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 4));
        b.add(new VarInsnNode(ALOAD, 5));
        b.add(new VarInsnNode(ILOAD, 3));
        b.add(new VarInsnNode(ALOAD, 7));
        b.add(
            call(
                L + "TableSupport",
                "header",
                "(Ljava/lang/String;Ljava/lang/String;ZL"
                    + BASE
                    + "scout/ScoutState$Phase;)Ljava/lang/String;"));
        b.add(new VarInsnNode(ASTORE, 4));
        m.instructions.insert(b);
      }
      if (m.name.equals("draw")) {
        InsnList b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new FieldInsnNode(GETFIELD, c.name, "preview", "Z"));
        b.add(call(L + "TableSupport", "preview", "(Z)V"));
        m.instructions.insert(b);
      }
      if (m.name.equals("fittedScale")) {
        InsnList b = new InsnList();
        b.add(new VarInsnNode(ALOAD, 0));
        b.add(new FieldInsnNode(GETFIELD, c.name, "hud", "Z"));
        b.add(call(L + "TableSupport", "context", "(Z)V"));
        b.add(new VarInsnNode(FLOAD, 2));
        b.add(new VarInsnNode(FLOAD, 3));
        b.add(call(L + "TableSupport", "viewport", "(FF)V"));
        b.add(new VarInsnNode(FLOAD, 1));
        b.add(call(L + "TableSupport", "fittedScale", "(F)F"));
        b.add(new VarInsnNode(FSTORE, 1));
        m.instructions.insert(b);
      }
      for (AbstractInsnNode i : m.instructions.toArray()) {
        if (i instanceof FieldInsnNode) {
          FieldInsnNode a = (FieldInsnNode) i;
          if (a.getOpcode() == PUTFIELD && a.owner.equals(c.name) && flags.containsKey(a.name)) {
            InsnList b = new InsnList();
            b.add(new LdcInsnNode(flags.get(a.name)));
            b.add(call(L + "TableSupport", "flag", "(ZLjava/lang/String;)Z"));
            if (Arrays.asList("shadow", "alternating", "dividers").contains(a.name))
              b.add(call(L + "TableSupport", "cosmetic", "(Z)Z"));
            m.instructions.insertBefore(i, b);
          }
          if (a.getOpcode() == PUTFIELD
              && a.owner.equals(c.name)
              && a.name.equals("cornerRadius")) {
            InsnList b = new InsnList();
            b.add(new LdcInsnNode("CornerRadius"));
            b.add(call(L + "TableSupport", "scalar", "(FLjava/lang/String;)F"));
            m.instructions.insertBefore(i, b);
          }
        }
        if (i instanceof MethodInsnNode) {
          MethodInsnNode a = (MethodInsnNode) i;
          if (a.owner.equals(BASE + "scout/ScoutConfig")
              && (a.name.equals("sort") || a.name.equals("sortChain") || a.name.equals("theme"))) {
            a.setOpcode(INVOKESTATIC);
            a.itf = false;
            a.owner = L + "TableSupport";
            a.desc = "(L" + BASE + "scout/ScoutConfig;" + a.desc.substring(1);
          }
          if (a.owner.equals(BASE + "scout/ui/ScoutPalette") && a.name.equals("resolve")) {
            a.owner = L + "TableSupport";
            a.name = "palette";
          }
          if (m.name.equals("lean$draw")
              && a.owner.equals(BASE + "scout/ui/ScoutAnimationState$Context")
              && Arrays.asList("row", "panelWidth", "panelHeight").contains(a.name)) {
            InsnList b = new InsnList();
            b.add(
                call(
                    L + "TableSupport",
                    a.name.equals("row") ? "entryEnabled" : "resizeEnabled",
                    "(Z)Z"));
            b.add(new InsnNode(SWAP));
            b.add(call(L + "TableSupport", a.name.equals("row") ? "entryMs" : "resizeMs", "(I)I"));
            b.add(new InsnNode(SWAP));
            m.instructions.insertBefore(i, b);
          }
        }
      }
      if (m.name.equals("colorCell"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i.getOpcode() == ARETURN) {
            InsnList b = new InsnList();
            b.add(new VarInsnNode(ALOAD, 1));
            b.add(new VarInsnNode(ALOAD, 2));
            b.add(
                call(
                    L + "TableSupport",
                    "color",
                    "(Ljava/awt/Color;Ljava/lang/String;L"
                        + BASE
                        + "scout/ScoutRow;)Ljava/awt/Color;"));
            m.instructions.insertBefore(i, b);
          }
      if (m.name.equals("<init>"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) i;
            if (call.owner.equals(c.name) && call.name.equals("colorCell")) {
              int row = m.maxLocals++, column = m.maxLocals++;
              InsnList b = new InsnList();
              b.add(new VarInsnNode(ASTORE, row));
              b.add(new VarInsnNode(ASTORE, column));
              b.add(new InsnNode(POP));
              b.add(new VarInsnNode(ALOAD, column));
              b.add(
                  call(
                      L + "TableSupport",
                      "cell",
                      "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
              b.add(new VarInsnNode(ALOAD, 0));
              b.add(new VarInsnNode(ALOAD, column));
              b.add(new VarInsnNode(ALOAD, row));
              m.instructions.insertBefore(i, b);
            }
            if (call.owner.equals(c.name) && call.name.equals("ellipsize")) {
              int strong = m.maxLocals++, font = m.maxLocals++;
              InsnList b = new InsnList();
              b.add(new VarInsnNode(ISTORE, strong));
              b.add(new VarInsnNode(FSTORE, font));
              b.add(call(L + "TableSupport", "nameWidth", "(F)F"));
              b.add(new VarInsnNode(FLOAD, font));
              b.add(new VarInsnNode(ILOAD, strong));
              m.instructions.insertBefore(i, b);
            }
          }
    }
    put(c);
    c = get("scout/ui/ScoutLayoutMetrics");
    for (MethodNode m : c.methods)
      if (m.name.equals("<init>"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof FieldInsnNode && i.getOpcode() == PUTFIELD) {
            String name = ((FieldInsnNode) i).name;
            if (name.equals("font") || name.equals("rowHeight"))
              m.instructions.insertBefore(
                  i,
                  call(L + "TableSupport", name.equals("font") ? "fontSize" : "rowHeight", "(F)F"));
          }
    put(c);
    c = get("management/mods/impl/ScoutMod");
    int factories = 0;
    for (MethodNode m : c.methods)
      for (AbstractInsnNode i : m.instructions.toArray())
        if (i instanceof MethodInsnNode
            && ((MethodInsnNode) i).owner.equals(BASE + "scout/ScoutTable")
            && ((MethodInsnNode) i).name.equals("<init>")
            && ((MethodInsnNode) i).desc.endsWith("ScoutState$Phase;)V")) {
          AbstractInsnNode allocation = i.getPrevious();
          while (allocation != null
              && !(allocation instanceof TypeInsnNode
                  && allocation.getOpcode() == NEW
                  && ((TypeInsnNode) allocation).desc.equals(BASE + "scout/ScoutTable")))
            allocation = allocation.getPrevious();
          if (allocation == null || allocation.getNext().getOpcode() != DUP)
            throw new IllegalStateException("Table allocation shape changed");
          m.instructions.remove(allocation.getNext());
          m.instructions.remove(allocation);
          MethodInsnNode a = (MethodInsnNode) i;
          a.setOpcode(INVOKESTATIC);
          a.owner = L + "TableSupport";
          a.name = "build";
          a.desc = a.desc.substring(0, a.desc.length() - 1) + "L" + BASE + "scout/ScoutTable;";
          factories++;
        }
    if (factories != 2)
      throw new IllegalStateException("Expected exactly two live table factories");
    put(c);
  }

  static void rows() {
    ClassNode c = get("scout/ScoutTable$RenderedRow");
    c.interfaces.add(L + "RowLifecycle$Named");
    MethodNode getter = new MethodNode(ACC_PUBLIC, "rowName", "()Ljava/lang/String;", null, null);
    getter.instructions.add(new VarInsnNode(ALOAD, 0));
    getter.instructions.add(
        new FieldInsnNode(GETFIELD, c.name, "source", "L" + BASE + "scout/ScoutRow;"));
    getter.instructions.add(
        new FieldInsnNode(GETFIELD, BASE + "scout/ScoutRow", "name", "Ljava/lang/String;"));
    getter.instructions.add(new InsnNode(ARETURN));
    c.methods.add(getter);
    put(c);
    c = get("scout/ScoutTable");
    int layout = 0, row = 0;
    for (MethodNode m : c.methods)
      for (AbstractInsnNode i : m.instructions.toArray())
        if (i instanceof MethodInsnNode) {
          MethodInsnNode a = (MethodInsnNode) i;
          if (a.owner.equals(BASE + "scout/ui/ScoutLayoutEngine") && a.name.equals("compute")) {
            InsnList b = new InsnList();
            b.add(new VarInsnNode(ALOAD, 0));
            b.add(new FieldInsnNode(GETFIELD, c.name, "rows", "Ljava/util/List;"));
            b.add(new VarInsnNode(ALOAD, 0));
            b.add(
                new FieldInsnNode(
                    GETFIELD, c.name, "phase", "L" + BASE + "scout/ScoutState$Phase;"));
            b.add(new VarInsnNode(ALOAD, 0));
            b.add(new FieldInsnNode(GETFIELD, c.name, "preview", "Z"));
            m.instructions.insertBefore(i, b);
            a.owner = L + "RowLifecycle";
            a.name = "layout";
            a.desc = a.desc.replace(")", "Ljava/util/List;L" + BASE + "scout/ScoutState$Phase;Z)");
            layout++;
          }
          if (a.owner.equals(BASE + "scout/ui/ScoutAnimationState$Context")
              && a.name.equals("row")) {
            a.setOpcode(INVOKESTATIC);
            a.itf = false;
            a.owner = L + "RowLifecycle";
            a.desc = "(L" + BASE + "scout/ui/ScoutAnimationState$Context;" + a.desc.substring(1);
            row++;
          }
        }
    if (layout != 1 || row != 1) throw new IllegalStateException("Unexpected retained-row hooks");
    put(c);
  }

  static void capes() {
    ClassNode c = get("management/cape/CapeManager");
    int preview = 0, bind = 0;
    for (MethodNode m : c.methods)
      if (m.name.equals("<init>"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode) {
            MethodInsnNode a = (MethodInsnNode) i;
            boolean image =
                a.owner.equals(BASE + "management/nanovg/NanoVGManager")
                    && a.name.equals("loadImage")
                    && (a.desc.equals("(Ljy;)V") || a.desc.equals("(Ljava/io/File;)V"));
            boolean texture =
                a.name.equals("a") && a.desc.equals("(Ljy;)V") && !a.owner.startsWith(BASE);
            if (image || texture) {
              m.instructions.insertBefore(i, new InsnNode(POP2));
              m.instructions.remove(i);
              if (image) preview++;
              else bind++;
            }
          }
    if (preview != 2 || bind != 1)
      throw new IllegalStateException("Cape preload call shape changed: " + preview + "/" + bind);
    put(c);
  }

  static void party() {
    ClassNode c = get("scout/v2/Velvet2PartyInference");
    for (MethodNode m : c.methods)
      if (m.name.equals("observe"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof LdcInsnNode && Long.valueOf(1600L).equals(((LdcInsnNode) i).cst)) {
            m.instructions.set(i, call(L + "TableSupport", "partyWindow", "()J"));
          }
    put(c);
  }
}
