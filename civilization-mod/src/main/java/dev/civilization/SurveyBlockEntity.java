package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Fixed local survey. Bounded loaded-chunk sampling; no tickets, world generation or player exploration. */
public final class SurveyBlockEntity extends BlockEntity {
    public int[] terrain=new int[4096];
    public List<SurveyPayload.Marker> markers=List.of();
    public boolean complete;
    public int revision;
    private int cursor;
    public SurveyBlockEntity(BlockPos p,BlockState s){super(CivicContent.TABLE_ENTITY.get(),p,s);}
    public void sample(int count){
        if(!(level instanceof ServerLevel server)||server.dimensionType().hasCeiling())return;
        for(int n=0;n<count;n++){
            int i=cursor++&4095,x=worldPosition.getX()-128+(i%64)*4+2,z=worldPosition.getZ()-128+(i/64)*4+2;
            var chunk=server.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)continue;
            int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15);
            var pos=new BlockPos(x,y,z);int color=chunk.getBlockState(pos).getMapColor(level,pos).col;
            if(color!=0){int shade=(y&3)*3+220;int r=((color>>16)&255)*shade/255,g=((color>>8)&255)*shade/255,b=(color&255)*shade/255;terrain[i]=0xFF000000|r<<16|g<<8|b;}
        }
    }
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        if(level.getGameTime()%20==0){boolean built=SurveyTable.complete(level,worldPosition);if(complete!=built){complete=built;sync();}}
        if(!complete)return;
        var viewer=server.getNearestPlayer(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,24,false);
        if(!(viewer instanceof ServerPlayer player))return;
        sample(64);
        if(Math.floorMod(level.getGameTime()+worldPosition.asLong(),100)==0){markers=SurveyMenu.snapshot(player,-1,worldPosition.getX(),worldPosition.getZ(),1).markers();sync();}
    }
    private void sync(){revision++;setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider p){super.saveAdditional(t,p);t.putIntArray("terrain",terrain);t.putBoolean("complete",complete);var list=new ListTag();for(var m:markers){var a=new CompoundTag();a.putBoolean("claim",m.claim());a.putInt("x",m.x());a.putInt("z",m.z());a.putInt("radius",m.radius());list.add(a);}t.put("markers",list);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider p){super.loadAdditional(t,p);int[] a=t.getIntArray("terrain");terrain=a.length==4096?a:new int[4096];complete=t.getBoolean("complete");var next=new ArrayList<SurveyPayload.Marker>();for(var raw:t.getList("markers",Tag.TAG_COMPOUND)){var m=(CompoundTag)raw;if(next.size()<256)next.add(new SurveyPayload.Marker(m.getBoolean("claim"),m.getInt("x"),0,m.getInt("z"),m.getInt("radius"),0,"",""));}markers=List.copyOf(next);revision++;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider p){return saveWithoutMetadata(p);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
