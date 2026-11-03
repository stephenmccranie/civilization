package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Drive input only: W/S drive and A/D turn; clients never submit a position, heading, velocity or wheel phase. */
public record WheelbarrowPayload(int forward,int turn,boolean release) implements CustomPacketPayload {
    public static final Type<WheelbarrowPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:wheelbarrow_input"));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("3").playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,WheelbarrowPayload>(){
        public WheelbarrowPayload decode(RegistryFriendlyByteBuf b){return new WheelbarrowPayload(b.readByte(),b.readByte(),b.readBoolean());}
        public void encode(RegistryFriendlyByteBuf b,WheelbarrowPayload p){b.writeByte(p.forward);b.writeByte(p.turn);b.writeBoolean(p.release);}
    },(input,context)->{if(context.player().getVehicle() instanceof WheelbarrowEntity barrow)barrow.input(context.player(),input);});}
}
