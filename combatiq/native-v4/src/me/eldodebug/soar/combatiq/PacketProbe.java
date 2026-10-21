package me.eldodebug.soar.combatiq;

import java.lang.reflect.Field;

/** Numeric allowlist verified against the SHA-pinned official 1.8.9 client registry.
 * No toString(), chat payloads, credentials or unrelated packet fields are read.
 * Class names select a candidate; exact class identity and field types must validate.
 */
public final class PacketProbe {
    private PacketProbe() {}
    private static final ClassValue<Layout> LAYOUTS = new ClassValue<Layout>() {
        protected Layout computeValue(Class<?> type) { return new Layout(type); }
    };
    private static final class Layout {
        final String name;
        final Field action, entity, x, y, z;
        final int family;
        Layout(Class<?> actual) {
            String n = actual.getName();
            int f = 0; Field a = null, id = null, fx = null, fy = null, fz = null;
            try {
                Class<?> type = actual;
                if (n.startsWith("gv$")) type = actual.getSuperclass();
                String base = type.getName();
                // Independent identity test, not displayed/simple class-name matching.
                if (Class.forName(base, false, actual.getClassLoader()) != type)
                    throw new IllegalStateException("class identity");
                if (base.equals("is")) {
                    f = 1; a = field(type, "b", Class.forName("is$a", false, actual.getClassLoader()));
                    id = field(type, "a", int.class);
                } else if (base.equals("in")) {
                    f = 2; a = field(type, "b", Class.forName("in$a", false, actual.getClassLoader()));
                    id = field(type, "a", int.class);
                } else if (base.equals("ir")) {
                    f = 3; a = field(type, "c", Class.forName("ir$a", false, actual.getClassLoader()));
                } else if (base.equals("ja")) {
                    f = 4; a = field(type, "c", int.class);
                } else if (base.equals("hm")) {
                    f = 5; id = field(type, "a", int.class);
                    fx = field(type, "b", int.class); fy = field(type, "c", int.class); fz = field(type, "d", int.class);
                } else if (base.equals("hf")) {
                    f = 6; id = field(type, "a", int.class); a = field(type, "b", byte.class);
                } else if (base.equals("gv")) {
                    f = 7; id = field(type, "a", int.class);
                    fx = field(type, "b", byte.class); fy = field(type, "c", byte.class); fz = field(type, "d", byte.class);
                } else if (base.equals("hz")) {
                    f = 8; id = field(type, "a", int.class);
                    fx = field(type, "b", int.class); fy = field(type, "c", int.class); fz = field(type, "d", int.class);
                } else if (base.equals("fi")) {
                    f = 9; fx = field(type, "a", double.class); fy = field(type, "b", double.class); fz = field(type, "c", double.class);
                }
                if (f > 0 && a != null && (f <= 3) && !a.getType().isEnum())
                    throw new IllegalStateException("action not enum");
            } catch (Throwable badLayout) { f = -1; }
            family = f; action = a; entity = id; x = fx; y = fy; z = fz; name = n;
        }
    }
    private static Field field(Class<?> owner, String name, Class<?> type) throws Exception {
        Field f = owner.getDeclaredField(name);
        if (f.getType() != type) throw new NoSuchFieldException("field type");
        f.setAccessible(true); return f;
    }
    /** Data codes 11..24 are stable in native schema 2; stage kind is 5, 6 or 15. */
    public static void capture(Object packet, int stage) {
        if (packet == null) return;
        Layout l = LAYOUTS.get(packet.getClass());
        if (l.family == 0) { CombatIQNative.recordFilteredPacket(); return; }
        if (l.family < 0) { CombatIQNative.reportPacketFailure(5); return; }
        try {
            int code;
            if (l.family <= 3) {
                String action = ((Enum<?>)l.action.get(packet)).name();
                if (l.family == 1) code = action.equals("START_SPRINTING") ? 11 : action.equals("STOP_SPRINTING") ? 12 : 23;
                else if (l.family == 2) code = action.equals("ATTACK") ? 13 : action.equals("INTERACT") ? 14 : action.equals("INTERACT_AT") ? 15 : -1;
                else code = action.equals("RELEASE_USE_ITEM") ? 16 : 24;
            } else code = l.family == 4 ? 17 : l.family == 5 ? 18 : l.family == 6 ? 19 : l.family == 7 ? 20 : l.family == 8 ? 21 : 22;
            int entity = l.entity == null ? -1 : l.entity.getInt(packet);
            double a = l.x == null ? Double.NaN : l.x.getDouble(packet);
            double b = l.y == null ? Double.NaN : l.y.getDouble(packet);
            double c = l.z == null ? Double.NaN : l.z.getDouble(packet);
            if (l.family == 6) a = l.action.getByte(packet);
            if (l.family == 4) a = l.action.getInt(packet);
            CombatIQNative.emit(stage, code, Float.NaN, l.name, entity, 0, a, b, c);
        } catch (Throwable failure) {
            CombatIQNative.reportPacketFailure(6);
        }
    }
}

