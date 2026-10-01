package dev.civilization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record BoatPayload(int throttle,int steer,boolean exit) implements CustomPacketPayload {
    public static final Type<BoatPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:boat_input"));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public record State(boolean active) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.parse("civilization:boat_state"));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e){
        var r=e.registrar("1");
        r.playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,BoatPayload>(){
            public BoatPayload decode(RegistryFriendlyByteBuf b){return new BoatPayload(b.readByte(),b.readByte(),b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,BoatPayload p){b.writeByte(p.throttle);b.writeByte(p.steer);b.writeBoolean(p.exit);}
        },(p,c)->BoatSystem.input((net.minecraft.server.level.ServerPlayer)c.player(),p.throttle,p.steer,p.exit));
        r.playToClient(State.TYPE,new StreamCodec<RegistryFriendlyByteBuf,State>(){
            public State decode(RegistryFriendlyByteBuf b){return new State(b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,State p){b.writeBoolean(p.active);}
        },(p,c)->{c.player().getPersistentData().putBoolean("civ_boat_pilot",p.active);c.player().getPersistentData().putLong("civ_boat_pilot_tick",c.player().tickCount);});
    }
}
