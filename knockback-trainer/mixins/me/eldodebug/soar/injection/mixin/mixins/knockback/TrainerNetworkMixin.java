package me.eldodebug.soar.injection.mixin.mixins.knockback;
import me.eldodebug.soar.knockback.TrainerRuntime;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="ek",remap=false)
public abstract class TrainerNetworkMixin{
 @Inject(method="a(Lff;)V",at=@At("HEAD"),require=1)
 private void trainer$attack(@Coerce Object p,CallbackInfo c){TrainerRuntime.outgoing(this,p);}
}
