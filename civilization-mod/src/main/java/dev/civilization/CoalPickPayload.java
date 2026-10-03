package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CoalPickPayload(float yaw,float pitch) implements CustomPacketPayload {
    private static final Type<CoalPickPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:coal_pick"));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,CoalPickPayload>(){
        public CoalPickPayload decode(RegistryFriendlyByteBuf b){return new CoalPickPayload(b.readFloat(),b.readFloat());}
        public void encode(RegistryFriendlyByteBuf b,CoalPickPayload p){b.writeFloat(p.yaw);b.writeFloat(p.pitch);}
    },(p,c)->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player&&Float.isFinite(p.yaw)&&Float.isFinite(p.pitch)&&Math.abs(p.pitch)<=90){player.setYRot(net.minecraft.util.Mth.wrapDegrees(p.yaw));player.setXRot(p.pitch);CoalMiningSystem.start(player);}});}
}
