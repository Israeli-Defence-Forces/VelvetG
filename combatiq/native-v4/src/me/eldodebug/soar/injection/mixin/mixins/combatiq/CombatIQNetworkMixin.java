package me.eldodebug.soar.injection.mixin.mixins.combatiq;
import me.eldodebug.soar.combatiq.CombatIQNative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Owner-bound observations. A Channel write invocation is not a socket write. */
@Mixin(targets="ek",remap=false)
public abstract class CombatIQNetworkMixin {
    @Inject(method="a(Lff;)V",at=@At("HEAD"),require=0)
    private void combatiq$sendEntry(@Coerce Object packet,CallbackInfo ci){CombatIQNative.onNetworkPacket(this,packet,26);}
    @Inject(method="a(Lio/netty/channel/ChannelHandlerContext;Lff;)V",at=@At("HEAD"),require=0)
    private void combatiq$inbound(@Coerce Object context,@Coerce Object packet,CallbackInfo ci){CombatIQNative.onNetworkPacket(this,packet,6);}
    @Inject(method="a(Lff;[Lio/netty/util/concurrent/GenericFutureListener;)V",at=@At("HEAD"),require=0)
    private void combatiq$dispatch(@Coerce Object packet,io.netty.util.concurrent.GenericFutureListener[] listeners,CallbackInfo ci){CombatIQNative.onNetworkPacket(this,packet,23);}
    @Inject(method="a(Lff;[Lio/netty/util/concurrent/GenericFutureListener;)V",at=@At(value="INVOKE",target="Lio/netty/channel/Channel;writeAndFlush(Ljava/lang/Object;)Lio/netty/channel/ChannelFuture;"),require=0)
    private void combatiq$writeCall(@Coerce Object packet,io.netty.util.concurrent.GenericFutureListener[] listeners,CallbackInfo ci){CombatIQNative.onNetworkPacket(this,packet,24);}
}
