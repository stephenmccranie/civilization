package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.*;

/** Unticked. Only strikes rebuild collision or invalidate the chunk mesh. */
public final class CoalWorkfaceEntity extends BlockEntity {
    public static final ModelProperty<Long> MASK=new ModelProperty<>();
    private long mask=CoalGeometry.FULL;
    private VoxelShape shape=CoalGeometry.shape(mask);
    public CoalWorkfaceEntity(BlockPos p,BlockState s){super(CoalMiningContent.FACE_ENTITY.get(),p,s);}
    public long mask(){return mask;}
    public VoxelShape shape(){return shape;}
    public void mask(long value){
        if(mask==value)return;
        mask=value;shape=CoalGeometry.shape(value);setChanged();requestModelDataUpdate();
        if(level!=null){
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
            if(!level.isClientSide)level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
        }
    }
    @Override public void onLoad(){
        super.onLoad();
        // Repair support for already-saved piles too, without adding a block-entity ticker.
        if(level!=null&&!level.isClientSide&&level.getBlockState(worldPosition.above()).is(CoalMiningContent.PILE.get()))
            level.scheduleTick(worldPosition.above(),CoalMiningContent.PILE.get(),1);
    }
    @Override public ModelData getModelData(){return ModelData.builder().with(MASK,mask).build();}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putLong("coalMask",mask);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);mask=t.contains("coalMask")?t.getLong("coalMask"):CoalGeometry.FULL;shape=CoalGeometry.shape(mask);requestModelDataUpdate();if(level!=null&&level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
