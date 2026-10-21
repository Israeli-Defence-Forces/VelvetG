package me.eldodebug.soar.injection.mixin.mixins.combatiq;
import me.eldodebug.soar.combatiq.CombatIQNative;
import me.eldodebug.soar.combatiq.CombatIQMod;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Optional registration after the native list exists. Never replaces existing modules. */
@Mixin(value=ModManager.class, remap=false)
public abstract class CombatIQModuleMixin {
    @Inject(method="init()V",at=@At("RETURN"),require=0)
    private void combatiq$register(CallbackInfo ci) {
        try {
            ModManager manager=(ModManager)(Object)this;
            for (Mod mod:manager.getMods()) if ("combatiq.module".equals(mod.getNameKey())) return;
            manager.getMods().add(new CombatIQMod());
        } catch (Throwable failure) { CombatIQNative.reportPacketFailure(8); }
    }
}
