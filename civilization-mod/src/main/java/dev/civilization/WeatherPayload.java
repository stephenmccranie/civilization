package dev.civilization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** The nine districts around a player, with the weather clock and cloud grid that place the shared cloud field. */
public record WeatherPayload(int x,int z,int rainMask,long clock,float[] cloudiness) implements CustomPacketPayload {
 public static final Type<WeatherPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:weather"));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static void register(RegisterPayloadHandlersEvent e){e.registrar("2").playToClient(TYPE,new StreamCodec<RegistryFriendlyByteBuf,WeatherPayload>(){public WeatherPayload decode(RegistryFriendlyByteBuf b){int x=b.readInt(),z=b.readInt(),rain=b.readVarInt();long clock=b.readLong();var cloudiness=new float[9];for(int i=0;i<9;i++){cloudiness[i]=b.readFloat();if(!(cloudiness[i]>=0&&cloudiness[i]<=1))throw new IllegalArgumentException("Cloudiness");}return new WeatherPayload(x,z,rain,clock,cloudiness);}public void encode(RegistryFriendlyByteBuf b,WeatherPayload p){b.writeInt(p.x);b.writeInt(p.z);b.writeVarInt(p.rainMask);b.writeLong(p.clock);for(float c:p.cloudiness)b.writeFloat(c);}},(p,c)->dev.civilization.client.LocalWeather.accept(c.player().level(),p));}
}
