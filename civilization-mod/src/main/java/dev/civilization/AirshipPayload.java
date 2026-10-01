package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class AirshipPayload {
    public record Action(int menu,int action,double power) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.parse("civilization:airship_action"));
        public Type<Action> type(){return TYPE;}
    }
    public record Input(int forward,int turn,int vertical,boolean exit) implements CustomPacketPayload {
        public static final Type<Input> TYPE=new Type<>(ResourceLocation.parse("civilization:airship_input"));
        public Type<Input> type(){return TYPE;}
    }
    public record State(boolean active) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(ResourceLocation.parse("civilization:airship_pilot"));
        public Type<State> type(){return TYPE;}
    }
    public record Snapshot(int menu,double power,boolean assembled,double mass,double speed,String message) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(ResourceLocation.parse("civilization:airship_snapshot"));
        public Type<Snapshot> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent e) {
        var r=e.registrar("1");
        r.playToServer(Action.TYPE,new StreamCodec<RegistryFriendlyByteBuf,Action>() {
            public Action decode(RegistryFriendlyByteBuf b){return new Action(b.readVarInt(),b.readVarInt(),b.readDouble());}
            public void encode(RegistryFriendlyByteBuf b,Action p){b.writeVarInt(p.menu);b.writeVarInt(p.action);b.writeDouble(p.power);}
        },(p,c)->{if(c.player() instanceof ServerPlayer player&&player.containerMenu instanceof AirshipMenu menu&&menu.containerId==p.menu)menu.action(player,p.action,p.power);});
        r.playToServer(Input.TYPE,new StreamCodec<RegistryFriendlyByteBuf,Input>() {
            public Input decode(RegistryFriendlyByteBuf b){return new Input(b.readByte(),b.readByte(),b.readByte(),b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,Input p){b.writeByte(p.forward);b.writeByte(p.turn);b.writeByte(p.vertical);b.writeBoolean(p.exit);}
        },(p,c)->AirshipSystem.input((ServerPlayer)c.player(),p));
        r.playToClient(State.TYPE,new StreamCodec<RegistryFriendlyByteBuf,State>() {
            public State decode(RegistryFriendlyByteBuf b){return new State(b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,State p){b.writeBoolean(p.active);}
        },(p,c)->{var t=c.player().getPersistentData();t.putBoolean("civ_airship_pilot",p.active);t.putLong("civ_airship_tick",c.player().tickCount);});
        r.playToClient(Snapshot.TYPE,new StreamCodec<RegistryFriendlyByteBuf,Snapshot>() {
            public Snapshot decode(RegistryFriendlyByteBuf b){return new Snapshot(b.readVarInt(),b.readDouble(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readUtf(256));}
            public void encode(RegistryFriendlyByteBuf b,Snapshot p){b.writeVarInt(p.menu);b.writeDouble(p.power);b.writeBoolean(p.assembled);b.writeDouble(p.mass);b.writeDouble(p.speed);b.writeUtf(p.message,256);}
        },(p,c)->{if(c.player().containerMenu instanceof AirshipMenu menu&&menu.containerId==p.menu)menu.snapshot=p;});
    }
}
