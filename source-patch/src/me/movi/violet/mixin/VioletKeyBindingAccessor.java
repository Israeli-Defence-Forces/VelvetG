package me.movi.violet.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vgstub.avb;
/** Observe a queue without consuming any manual actions. */
@Mixin(value=avb.class,remap=false)
public interface VioletKeyBindingAccessor {
 @Accessor(value="i",remap=false) int violet$getPending();
}
