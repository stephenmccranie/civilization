package dev.civilization.mixin;

import dev.civilization.TabLatency;
import net.minecraft.Util;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class LatencyPongMixin {
    @Inject(method="handlePong",at=@At("TAIL"))
    private void civilization$latency(ServerboundPongPacket packet,CallbackInfo ci){
        if((Object)this instanceof ServerGamePacketListenerImpl connection)TabLatency.pong(connection,packet.getId(),Util.getMillis());
    }
}
