package me.eldodebug.soar.injection.mixin.mixins.combatiq;
import me.eldodebug.soar.combatiq.CombatIQNative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** RETURN means the handler returned on its executing thread, not necessarily
 * that a missing entity received an impulse or damage. Thread-check quick-exit
 * exceptions do not reach RETURN. No own-hit confirmation is inferred. */
@Mixin(targets="bcy", remap=false)
public abstract class CombatIQHandlerMixin {
    @Inject(method={"a(Lhm;)V", "a(Lhf;)V", "a(Lfi;)V", "a(Lgv;)V", "a(Lhz;)V"}, at=@At("RETURN"), require=0)
    private void combatiq$return(@Coerce Object packet, CallbackInfo ci) { CombatIQNative.onHandlerReturn(packet); }
}

