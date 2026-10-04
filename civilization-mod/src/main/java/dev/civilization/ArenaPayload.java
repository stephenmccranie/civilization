package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ArenaPayload(int menu,int revision,int action,int offer) implements CustomPacketPayload {
    public static final Type<ArenaPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:arena_action"));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,ArenaPayload>(){
        public ArenaPayload decode(RegistryFriendlyByteBuf b){return new ArenaPayload(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,ArenaPayload p){b.writeVarInt(p.menu);b.writeVarInt(p.revision);b.writeVarInt(p.action);b.writeVarInt(p.offer);}
    },(p,c)->{if(c.player().containerMenu instanceof ArenaMenu m&&m.containerId==p.menu)m.action(p.revision,p.action,p.offer);});}
}
