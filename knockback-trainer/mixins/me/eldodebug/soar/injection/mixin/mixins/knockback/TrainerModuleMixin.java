package me.eldodebug.soar.injection.mixin.mixins.knockback;
import me.eldodebug.soar.knockback.KnockbackTrainerMod;import me.eldodebug.soar.management.mods.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ModManager.class,remap=false)
public abstract class TrainerModuleMixin{
 @Inject(method="init()V",at=@At("RETURN"),require=1)
 private void trainer$register(CallbackInfo c){try{ModManager m=(ModManager)(Object)this;
  for(Mod mod:m.getMods())if("knockbacktrainer.module".equals(mod.getNameKey()))return;
  m.getMods().add(new KnockbackTrainerMod());}catch(Throwable failure){me.eldodebug.soar.knockback.TrainerRuntime.registrationFailure(failure);}}
}
