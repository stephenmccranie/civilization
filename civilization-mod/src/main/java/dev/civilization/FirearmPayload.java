package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record FirearmPayload(int action) implements CustomPacketPayload {
    public static final Type<FirearmPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:firearm"));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){
        var codec=new StreamCodec<RegistryFriendlyByteBuf,FirearmPayload>(){public FirearmPayload decode(RegistryFriendlyByteBuf b){return new FirearmPayload(b.readByte());}public void encode(RegistryFriendlyByteBuf b,FirearmPayload p){b.writeByte(p.action);}};
        e.registrar("1").playBidirectional(TYPE,codec,(p,c)->{
            if(c.player().level().isClientSide){if(p.action==2)c.player().getPersistentData().putInt("patersonRecoil",4);}
            else if(p.action==0)PatersonItem.fire(c.player());else if(p.action==1)PatersonItem.reload(c.player());
        });
    }
}
