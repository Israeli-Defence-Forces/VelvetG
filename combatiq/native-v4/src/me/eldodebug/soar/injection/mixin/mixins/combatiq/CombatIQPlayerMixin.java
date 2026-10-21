package me.eldodebug.soar.injection.mixin.mixins.combatiq;
import me.eldodebug.soar.combatiq.CombatIQNative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Exact bew.d(Z)V setter and bew.m()V lifecycle verified against official client. */
@Mixin(targets="bew", remap=false)
public abstract class CombatIQPlayerMixin {
    @Inject(method="d(Z)V", at=@At("HEAD"), require=1)
    private void combatiq$beforeSprint(boolean value, CallbackInfo ci) { CombatIQNative.onPlayerPoint(this, 1); }
    @Inject(method="d(Z)V", at=@At("RETURN"), require=1)
    private void combatiq$afterSprint(boolean value, CallbackInfo ci) { CombatIQNative.onPlayerPoint(this, 2); }
    @Inject(method="m()V", at=@At("RETURN"), require=1)
    private void combatiq$afterLiving(CallbackInfo ci) { CombatIQNative.onPlayerPoint(this, 3); }
}
