package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record StoveDialPayload(BlockPos pos,double value) implements CustomPacketPayload {
    public static final Type<StoveDialPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:stove_dial"));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,StoveDialPayload>(){
        public StoveDialPayload decode(RegistryFriendlyByteBuf b){return new StoveDialPayload(b.readBlockPos(),b.readDouble());}
        public void encode(RegistryFriendlyByteBuf b,StoveDialPayload p){b.writeBlockPos(p.pos);b.writeDouble(p.value);}
    },(p,c)->{var player=c.player();if(player.level().hasChunkAt(p.pos)&&player.level().getBlockEntity(p.pos) instanceof PrototypeStoveEntity stove)stove.dial(player,p.value);});}
}
