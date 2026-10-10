package me.movi.violet.mixin;

import me.movi.violet.VioletRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vgstub.avb;

/** Captures vanilla key-state transitions without depending on event ordinals in Minecraft.runTick. */
@Mixin(value = avb.class, remap = false)
public abstract class VioletKeyStateMixin {
    @Inject(method = "a(IZ)V", at = @At("HEAD"), require = 0, remap = false)
    private static void violet$keyState(int code, boolean down, CallbackInfo ci) {
        VioletRuntime.keyState(code, down);
    }
}
