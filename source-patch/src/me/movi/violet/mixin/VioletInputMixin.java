package me.movi.violet.mixin;

import me.movi.violet.VioletRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vgstub.ave;

/**
 * One deliberately boring tick hook. r7 depended on three ordinal INVOKE locations in ave.s();
 * r8 moves Violet's work to HEAD so minor OptiFine/base changes do not shift an ordinal and
 * take the entire client down with them.
 */
@Mixin(value = ave.class, remap = false)
public abstract class VioletInputMixin {
    @Inject(method = "s()V", at = @At("HEAD"), require = 0, remap = false)
    private void violet$tick(CallbackInfo ci) {
        VioletRuntime.tick((ave) (Object) this);
    }
}
