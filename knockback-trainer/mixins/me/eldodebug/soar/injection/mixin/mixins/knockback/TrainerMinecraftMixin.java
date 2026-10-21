package me.eldodebug.soar.injection.mixin.mixins.knockback;
import me.eldodebug.soar.knockback.TrainerRuntime;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="ave",remap=false)
public abstract class TrainerMinecraftMixin{
 @Inject(method="s()V",at=@At("HEAD"),require=1)private void trainer$start(CallbackInfo c){TrainerRuntime.tickStart();}
 @Inject(method="s()V",at=@At("RETURN"),require=1)private void trainer$end(CallbackInfo c){TrainerRuntime.tickEnd();}
 @Inject(method="s()V",at=@At(value="INVOKE",target="Lorg/lwjgl/input/Keyboard;getEventKeyState()Z",ordinal=0,shift=At.Shift.AFTER),require=1)
 private void trainer$key(CallbackInfo c){TrainerRuntime.keyboard();}
 @Inject(method="s()V",at=@At(value="INVOKE",target="Lorg/lwjgl/input/Mouse;getEventButton()I",ordinal=0,shift=At.Shift.AFTER),require=1)
 private void trainer$mouse(CallbackInfo c){TrainerRuntime.mouse();}
}
