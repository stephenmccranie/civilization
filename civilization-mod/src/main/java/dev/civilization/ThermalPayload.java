package dev.civilization;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record ThermalPayload(float temperature,float playerTemperature,float comfort,float solverAgeSeconds,boolean supported,List<Sample> samples) implements CustomPacketPayload {
    public static final int VIEW_RADIUS=8;
    public static final int MAX_SAMPLES=(VIEW_RADIUS*2+1)*(VIEW_RADIUS*2+1)*(VIEW_RADIUS*2+1);
    public record Sample(BlockPos pos,float temperature){}
    public static final Type<ThermalPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:heat"));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("4").playToClient(TYPE,new StreamCodec<RegistryFriendlyByteBuf,ThermalPayload>(){
        public ThermalPayload decode(RegistryFriendlyByteBuf b){return read(b);}
        public void encode(RegistryFriendlyByteBuf b,ThermalPayload p){write(b,p);}
    },(p,c)->{c.player().getPersistentData().putFloat("civilization_comfort",p.comfort);dev.civilization.client.ThermalVision.accept(p);});}
    static ThermalPayload read(RegistryFriendlyByteBuf b){float t=b.readFloat(),player=b.readFloat(),c=b.readFloat(),age=b.readFloat();boolean supported=b.readBoolean();int n=b.readVarInt();if(n<0||n>MAX_SAMPLES)throw new IllegalArgumentException("Heat sample count");var list=new ArrayList<Sample>(n);if(n>0){var origin=b.readBlockPos();for(int i=0;i<n;i++)list.add(new Sample(origin.offset(b.readByte(),b.readByte(),b.readByte()),b.readShort()/10f));}return new ThermalPayload(t,player,c,age,supported,List.copyOf(list));}
    static void write(RegistryFriendlyByteBuf b,ThermalPayload p){b.writeFloat(p.temperature);b.writeFloat(p.playerTemperature);b.writeFloat(p.comfort);b.writeFloat(p.solverAgeSeconds);b.writeBoolean(p.supported);b.writeVarInt(p.samples.size());if(!p.samples.isEmpty()){var origin=p.samples.getFirst().pos();b.writeBlockPos(origin);for(var s:p.samples){var pos=s.pos();b.writeByte(pos.getX()-origin.getX());b.writeByte(pos.getY()-origin.getY());b.writeByte(pos.getZ()-origin.getZ());b.writeShort(Math.clamp(Math.round(s.temperature()*10),Short.MIN_VALUE,Short.MAX_VALUE));}}}
}
