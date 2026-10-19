import java.util.*;
import org.spongepowered.asm.lib.*;
import org.spongepowered.asm.lib.tree.*;

/** Exact guarded changes layered after the established Overhaul1/2 transforms. */
public final class StageThree extends BytecodeIO {
  static final String L = BASE + "lean/";

  static void apply() {
    ClassNode head = LeanPatch.get("management/nanovg/NanoVGManager");
    int queries = 0;
    for (MethodNode m : head.methods)
      if (m.name.equals("drawPlayerHead") && m.desc.equals("(Ljy;FFFFFF)V"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals("org/lwjgl/nanovg/NanoVG")
              && ((MethodInsnNode) i).name.equals("nvgImageSize")) {
            // Both result arrays are freshly allocated, immediately discarded and never retained.
            int[] expected = {
              ALOAD,
              GETFIELD,
              ILOAD,
              ICONST_1,
              NEWARRAY,
              DUP,
              ICONST_0,
              FLOAD,
              F2I,
              IASTORE,
              ICONST_1,
              NEWARRAY,
              DUP,
              ICONST_0,
              FLOAD,
              F2I,
              INEG,
              IASTORE,
              INVOKESTATIC
            };
            List<AbstractInsnNode> block = new ArrayList<>();
            AbstractInsnNode n = i;
            for (int j = expected.length - 1; j >= 0; j--) {
              while (n != null && n.getOpcode() < 0) n = n.getPrevious();
              if (n == null || n.getOpcode() != expected[j])
                throw new IllegalStateException("Unexpected discarded head size query");
              block.add(n);
              n = n.getPrevious();
            }
            for (AbstractInsnNode dead : block) m.instructions.remove(dead);
            queries++;
          }
    if (queries != 1)
      throw new IllegalStateException("Unexpected head size query count " + queries);
    LeanPatch.put(head);
    ClassNode c = LeanPatch.get("scout/ScoutTable");
    int draws = 0, handlers = 0, alphas = 0;
    for (MethodNode m : c.methods) {
      for (AbstractInsnNode i : m.instructions.toArray()) {
        if (i instanceof MethodInsnNode) {
          MethodInsnNode a = (MethodInsnNode) i;
          if (m.name.equals("draw") && a.owner.equals(c.name) && a.name.equals("lean$draw")) {
            m.instructions.insertBefore(i, call(L + "TableEffects", "beginDraw", "()V"));
            m.instructions.insert(i, call(L + "TableEffects", "endDraw", "()V"));
            draws++;
          }
          if (m.name.equals("draw")
              && a.owner.equals(L + "FrameProbe")
              && a.name.equals("section")) {
            m.instructions.insertBefore(i, call(L + "TableEffects", "endDraw", "()V"));
            handlers++;
          }
          if (m.name.equals("lean$draw")
              && a.owner.equals("org/lwjgl/nanovg/NanoVG")
              && a.name.equals("nvgGlobalAlpha")) {
            m.instructions.insertBefore(i, call(L + "TableEffects", "rowAlpha", "(F)F"));
            alphas++;
          }
        }
      }
    }
    if (draws != 1 || handlers != 2 || alphas != 1)
      throw new IllegalStateException("Unexpected table draw/alpha shape");
    LeanPatch.put(c);
    c = LeanPatch.get("management/mods/impl/ScoutMod");
    int keys = 0;
    for (MethodNode m : c.methods)
      if (m.name.equals("onRender"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode) {
            MethodInsnNode a = (MethodInsnNode) i;
            if (a.owner.equals("avb") && a.name.equals("d") && a.desc.equals("()Z")) {
              m.instructions.insert(i, call(L + "TableEffects", "hideHud", "(Z)Z"));
              keys++;
            }
          }
    int missing = 0;
    for (MethodNode m : c.methods)
      if (m.name.equals("onTick"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof FieldInsnNode
              && i.getOpcode() == GETFIELD
              && ((FieldInsnNode) i).name.equals("tabTable")) {
            AbstractInsnNode next = i.getNext();
            while (next != null && next.getOpcode() < 0) next = next.getNext();
            if (next != null && next.getOpcode() == IFNONNULL) {
              InsnList b = new InsnList();
              b.add(new VarInsnNode(ALOAD, 0));
              b.add(new FieldInsnNode(GETFIELD, c.name, "tabVisible", "Z"));
              b.add(new VarInsnNode(ALOAD, 0));
              b.add(
                  new FieldInsnNode(
                      GETFIELD, c.name, "hudTable", "L" + BASE + "scout/ScoutTable;"));
              b.add(new VarInsnNode(ALOAD, 0));
              b.add(new FieldInsnNode(GETFIELD, c.name, "hudVisible", "Z"));
              b.add(
                  call(
                      L + "TableSupport",
                      "needsTable",
                      "(L" + BASE + "scout/ScoutTable;ZL" + BASE + "scout/ScoutTable;Z)Z"));
              m.instructions.insert(i, b);
              ((JumpInsnNode) next).setOpcode(IFEQ);
              missing++;
            }
          }
    if (missing != 1) throw new IllegalStateException("Unexpected model rebuild guard " + missing);
    if (keys != 1) throw new IllegalStateException("Unexpected HUD key guard " + keys);
    LeanPatch.put(c);
    c = LeanPatch.get("Glide");
    for (MethodNode m : c.methods)
      if (m.name.equals("start"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i.getOpcode() == RETURN) {
            InsnList b = new InsnList();
            b.add(new LdcInsnNode("glideStartReturned"));
            b.add(call(L + "InitProbe", "mark", "(Ljava/lang/String;)V"));
            m.instructions.insertBefore(i, b);
          }
    for (MethodNode m : c.methods)
      if (m.name.equals("start"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals(BASE + "management/mods/ModManager")
              && ((MethodInsnNode) i).name.equals("init")) {
            InsnList b = new InsnList();
            b.add(new LdcInsnNode("modulesInitialized"));
            b.add(call(L + "InitProbe", "mark", "(Ljava/lang/String;)V"));
            m.instructions.insert(i, b);
          }
    for (MethodNode m : c.methods)
      if (m.name.equals("start"))
        for (AbstractInsnNode i : m.instructions.toArray())
          if (i instanceof MethodInsnNode
              && ((MethodInsnNode) i).owner.equals(L + "InitProbe")
              && ((MethodInsnNode) i).name.equals("begin"))
            ((MethodInsnNode) i).name = "glideBegin";
    LeanPatch.put(c);
    c = LeanPatch.get("injection/mixin/mixins/client/MixinMinecraft");
    int stages = 0;
    for (MethodNode m : c.methods)
      if (m.name.startsWith("splash")) {
        InsnList b = new InsnList();
        b.add(new LdcInsnNode(m.name));
        b.add(call(L + "InitProbe", "mark", "(Ljava/lang/String;)V"));
        m.instructions.insert(b);
        stages++;
      }
    if (stages != 17) throw new IllegalStateException("Unexpected startup splash hooks " + stages);
    for (String[] hook :
        new String[][] {
          {"lean$startupBegin", "HEAD", "begin"}, {"lean$startupComplete", "RETURN", "complete"}
        }) {
      LeanPatch.addFrameHook(c, hook[0], hook[1], hook[2]);
      MethodNode m = c.methods.get(c.methods.size() - 1);
      m.visibleAnnotations.get(0).values.set(1, Arrays.asList("startGame"));
      for (AbstractInsnNode i : m.instructions.toArray())
        if (i instanceof MethodInsnNode && ((MethodInsnNode) i).owner.equals(L + "FrameProbe"))
          ((MethodInsnNode) i).owner = L + "InitProbe";
    }
    LeanPatch.put(c);
  }
}
