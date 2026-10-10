package me.movi.violet.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Invoker;import vgstub.bda;
@Mixin(value=bda.class,remap=false)
public interface AssistControllerAccessor {@Invoker(value="n",remap=false) void violet$syncSlot();}
