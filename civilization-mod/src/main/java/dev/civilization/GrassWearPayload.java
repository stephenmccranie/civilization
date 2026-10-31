package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-owned chunk snapshot or a batch of changed visual stages; never accepts client wear claims. */
public record GrassWearPayload(ResourceLocation dimension,int chunkX,int chunkZ,boolean replace,long[] cells) implements CustomPacketPayload {
    public static final Type<GrassWearPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID,"grass_wear"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GrassWearPayload> CODEC=new StreamCodec<>(){
        public GrassWearPayload decode(RegistryFriendlyByteBuf b){
            var dimension=b.readResourceLocation();int x=b.readInt(),z=b.readInt();boolean replace=b.readBoolean();int n=b.readVarInt();
            if(n<0||n>GrassWear.MAX_CELLS)throw new IllegalArgumentException("Too many grass wear cells");
            var cells=new long[n];for(int i=0;i<n;i++){
                long cell=b.readVarLong(),key=cell>>4;int stage=(int)(cell&15);
                if(stage<1||stage>GrassWear.STAGES||key>>8!=(long)GrassWear.y(key))throw new IllegalArgumentException("Invalid grass wear cell");
                cells[i]=cell;
            }
            return new GrassWearPayload(dimension,x,z,replace,cells);
        }
        public void encode(RegistryFriendlyByteBuf b,GrassWearPayload p){
            if(p.cells.length>GrassWear.MAX_CELLS)throw new IllegalArgumentException("Too many grass wear cells");
            b.writeResourceLocation(p.dimension);b.writeInt(p.chunkX);b.writeInt(p.chunkZ);b.writeBoolean(p.replace);b.writeVarInt(p.cells.length);
            for(long cell:p.cells)b.writeVarLong(cell);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,(p,c)->dev.civilization.client.GrassWearClient.accept(c.player().level(),p));}
}
