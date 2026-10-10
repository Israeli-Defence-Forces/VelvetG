import me.movi.violet.mixin.VioletKeyBindingAccessor;
/** Run with the offline-transformed avb directory FIRST in the class path. */
public class AccessorChecks {
 public static void main(String[] args){avb binding=new avb("violet.test.manual",-100,"test");VioletKeyBindingAccessor queue=(VioletKeyBindingAccessor)(Object)binding;
  avb.a(-100,true);avb.a(-100);avb.a(-100);if(queue.violet$getPending()!=2||queue.violet$getPending()!=2||!binding.d())throw new AssertionError("Queue read consumed manual input");
  if(!binding.f()||queue.violet$getPending()!=1||!binding.f()||binding.f())throw new AssertionError("Manual queue changed");System.out.println("PASS: real transformed KeyBinding queue accessor preserves both manual actions and held state");
 }
}
