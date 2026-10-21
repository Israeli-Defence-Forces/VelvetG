package me.eldodebug.soar.injection.mixin.mixins.combatiq;
import me.eldodebug.soar.combatiq.CombatIQNative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="ave", remap=false)
public abstract class CombatIQMinecraftMixin {
    @Inject(method="aw()V", at=@At("HEAD"), require=1)
    private void combatiq$attack(CallbackInfo ci) { CombatIQNative.onAttackIntent(); }
    @Inject(method="ax()V", at=@At("HEAD"), require=1)
    private void combatiq$use(CallbackInfo ci) { CombatIQNative.onUseIntent(); }
    @Inject(method="a(Laxu;)V", at=@At("RETURN"), require=1)
    private void combatiq$gui(CallbackInfo ci) { CombatIQNative.onContextPoint(); }
    @Inject(method="n()V", at=@At("RETURN"), require=1)
    private void combatiq$focus(CallbackInfo ci) { CombatIQNative.onContextPoint(); }
    @Inject(method="o()V", at=@At("RETURN"), require=1)
    private void combatiq$blur(CallbackInfo ci) { CombatIQNative.onContextPoint(); }
}
