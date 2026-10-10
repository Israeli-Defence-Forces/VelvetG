package me.movi.violet.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vgstub.bev;import vgstub.beu;import vgstub.ave;import me.movi.violet.AssistRuntime;
@Mixin(value=bev.class,remap=false)
public abstract class AssistMovementMixin {
 @Inject(method="a()V",at=@At("RETURN"),require=0,remap=false)
 private void violet$inventoryMove(CallbackInfo ci){AssistRuntime.move(ave.A(),(beu)(Object)this);}
}
