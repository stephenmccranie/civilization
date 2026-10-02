package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BuilderLinePayload {
    public record Mark(boolean cancel,float yaw,float pitch) implements CustomPacketPayload {
        public static final Type<Mark> TYPE=new Type<>(ResourceLocation.parse("civilization:line_mark"));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record State(boolean selected,BlockPos a,BlockPos b,boolean active,int completed) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.parse("civilization:line_state"));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e){
        var r=e.registrar("1");r.playToServer(Mark.TYPE,new StreamCodec<RegistryFriendlyByteBuf,Mark>(){
            public Mark decode(RegistryFriendlyByteBuf b){return new Mark(b.readBoolean(),b.readFloat(),b.readFloat());}
            public void encode(RegistryFriendlyByteBuf b,Mark p){b.writeBoolean(p.cancel());b.writeFloat(p.yaw());b.writeFloat(p.pitch());}
        },(p,c)->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player&&Float.isFinite(p.yaw())&&Float.isFinite(p.pitch())&&Math.abs(p.pitch())<=90){player.setYRot(net.minecraft.util.Mth.wrapDegrees(p.yaw()));player.setXRot(p.pitch());BuilderLineSystem.mark(player,p.cancel());}});
        r.playToClient(State.TYPE,new StreamCodec<RegistryFriendlyByteBuf,State>(){
            public State decode(RegistryFriendlyByteBuf b){return new State(b.readBoolean(),b.readBlockPos(),b.readBlockPos(),b.readBoolean(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,State p){b.writeBoolean(p.selected());b.writeBlockPos(p.a());b.writeBlockPos(p.b());b.writeBoolean(p.active());b.writeVarInt(p.completed());}
        },(p,c)->dev.civilization.client.BuilderLineClient.accept(p));
    }
}
