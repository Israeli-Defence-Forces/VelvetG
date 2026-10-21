package me.eldodebug.soar.injection.mixin.mixins.combatiq;
import me.eldodebug.soar.combatiq.CombatIQNative;
import java.lang.reflect.Field;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Event-loop queued write branch, verified against the official ek$4 layout. */
@Mixin(targets="ek$4",remap=false)
public abstract class CombatIQQueuedWriteMixin {
    @Unique private static volatile Field combatiq$manager,combatiq$packet;
    @Unique private static volatile boolean combatiq$failed;
    @Inject(method="run()V",at=@At(value="INVOKE",target="Lio/netty/channel/Channel;writeAndFlush(Ljava/lang/Object;)Lio/netty/channel/ChannelFuture;"),require=0)
    private void combatiq$write(CallbackInfo ci){
        if(!CombatIQNative.enabled()||combatiq$failed)return;
        try {
            if(combatiq$manager==null){Field m=this.getClass().getDeclaredField("e"),p=this.getClass().getDeclaredField("c");
                m.setAccessible(true);p.setAccessible(true);combatiq$packet=p;combatiq$manager=m;}
            CombatIQNative.onNetworkPacket(combatiq$manager.get(this),combatiq$packet.get(this),24);
        }catch(Throwable failure){combatiq$failed=true;CombatIQNative.reportPacketFailure(12);}
    }
}
