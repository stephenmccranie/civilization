package dev.civilization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record WeatherPayload(int x,int z,int rainMask) implements CustomPacketPayload {
 public static final Type<WeatherPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:weather"));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,new StreamCodec<RegistryFriendlyByteBuf,WeatherPayload>(){public WeatherPayload decode(RegistryFriendlyByteBuf b){return new WeatherPayload(b.readInt(),b.readInt(),b.readVarInt());}public void encode(RegistryFriendlyByteBuf b,WeatherPayload p){b.writeInt(p.x);b.writeInt(p.z);b.writeVarInt(p.rainMask);}},(p,c)->dev.civilization.client.LocalWeather.accept(c.player().level(),p));}
}
