package dev.civilization;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record SurveyPayload(int menuId,int centerX,int centerZ,int zoom,int playerX,int playerZ,int[] terrain,List<Marker> markers,boolean truncated) implements CustomPacketPayload {
    public record Marker(boolean claim,int x,int y,int z,int radius,int height,String title,String details) {}
    public static final Type<SurveyPayload> TYPE=new Type<>(ResourceLocation.parse("civilization:survey"));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(TYPE,new StreamCodec<RegistryFriendlyByteBuf,SurveyPayload>() {
            public SurveyPayload decode(RegistryFriendlyByteBuf b) {
                int id=b.readVarInt(),x=b.readInt(),z=b.readInt(),zoom=b.readVarInt(),px=b.readInt(),pz=b.readInt();
                if(zoom<1||zoom>32) throw new IllegalArgumentException("Invalid map scale");
                int[] colors=new int[4096];for(int i=0;i<colors.length;i++) colors[i]=b.readInt();
                int n=b.readVarInt();if(n<0||n>256) throw new IllegalArgumentException("Too many map markers");
                var markers=new ArrayList<Marker>();for(int i=0;i<n;i++) markers.add(new Marker(b.readBoolean(),b.readInt(),b.readInt(),b.readInt(),b.readVarInt(),b.readVarInt(),b.readUtf(128),b.readUtf(512)));
                return new SurveyPayload(id,x,z,zoom,px,pz,colors,List.copyOf(markers),b.readBoolean());
            }
            public void encode(RegistryFriendlyByteBuf b,SurveyPayload p) {
                b.writeVarInt(p.menuId);b.writeInt(p.centerX);b.writeInt(p.centerZ);b.writeVarInt(p.zoom);b.writeInt(p.playerX);b.writeInt(p.playerZ);
                for(int color:p.terrain)b.writeInt(color);b.writeVarInt(p.markers.size());
                for(var m:p.markers){b.writeBoolean(m.claim);b.writeInt(m.x);b.writeInt(m.y);b.writeInt(m.z);b.writeVarInt(m.radius);b.writeVarInt(m.height);b.writeUtf(m.title,128);b.writeUtf(m.details,512);}b.writeBoolean(p.truncated);
            }
        },(p,c)->{if(c.player().containerMenu instanceof SurveyMenu m && m.containerId==p.menuId)m.snapshot=p;});
    }
}
