package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record OvenDialPayload(BlockPos pos,double delta) implements CustomPacketPayload {
    public static final Type<OvenDialPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:oven_dial"));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,OvenDialPayload>(){
        public OvenDialPayload decode(RegistryFriendlyByteBuf b){return new OvenDialPayload(b.readBlockPos(),b.readDouble());}
        public void encode(RegistryFriendlyByteBuf b,OvenDialPayload p){b.writeBlockPos(p.pos);b.writeDouble(p.delta);}
    },(p,c)->{var player=c.player();var l=player.level();if(l.hasChunkAt(p.pos)&&player.distanceToSqr(p.pos.getCenter())<=64){var s=l.getBlockState(p.pos);var oven=BakingOvenBlock.owner(l,p.pos,s);if(oven!=null&&CivicAccess.allowed(l,p.pos,player)&&CivicAccess.allowed(l,oven.getBlockPos(),player))oven.turn(p.delta);}});}
}
