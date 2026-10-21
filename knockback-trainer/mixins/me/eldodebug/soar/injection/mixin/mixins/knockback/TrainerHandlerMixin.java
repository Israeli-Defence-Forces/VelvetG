package me.eldodebug.soar.injection.mixin.mixins.knockback;
import me.eldodebug.soar.knockback.TrainerRuntime;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="bcy",remap=false)
public abstract class TrainerHandlerMixin{
 @Inject(method={"a(Lhf;)V","a(Lhm;)V"},at=@At("RETURN"),require=1)
 private void trainer$feedback(@Coerce Object p,CallbackInfo c){TrainerRuntime.feedback(this,p);}
}
