import me.movi.violet.mixin.VioletKeyBindingAccessor;
/** The same field getter supplied by the production accessor mixin. */
public class TestKeyBinding extends avb implements VioletKeyBindingAccessor {
 private static final java.lang.reflect.Field QUEUE;
 static{try{QUEUE=avb.class.getDeclaredField("i");QUEUE.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 public TestKeyBinding(String name,int code,String category){super(name,code,category);}
 public int violet$getPending(){try{return QUEUE.getInt(this);}catch(Exception e){throw new AssertionError(e);}}
}
