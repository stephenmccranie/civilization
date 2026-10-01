package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.*;

/** Unticked material identity. Geometry is baked into chunk meshes, not rendered per frame. */
public final class CutBlockEntity extends BlockEntity {
    public static final ModelProperty<BlockState> MATERIAL = new ModelProperty<>();
    public static final ModelProperty<BlockState[]> CELLS = new ModelProperty<>();
    public static final ModelProperty<BlockPos> POSITION = new ModelProperty<>();
    private BlockState material = Blocks.COBBLESTONE.defaultBlockState();
    private BlockState[] cells;
    public CutBlockEntity(BlockPos pos, BlockState state) { super(CuttingContent.ENTITY.get(), pos, state); }
    public BlockState material() { return material; }
    public boolean hasCells() {return cells!=null;}
    public BlockState[] cells() {return cells==null?CutCells.filled(CutBlock.bounds(getBlockState()),material):cells.clone();}
    public void cells(BlockState[] value) {cells=value.clone();material=CutCells.material(cells);setChanged();requestModelDataUpdate();if(level!=null && !level.captureBlockSnapshots){syncLightFaces();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
    public void material(BlockState value) {
        material = value; cells=null; setChanged(); requestModelDataUpdate();
        if (level != null) {syncLightFaces();level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);}
    }
    void syncLightFaces(){
        if(level==null||!getBlockState().is(CuttingContent.PIECE.get()))return;
        int faces=CutCells.opaqueFaces(cells());
        if(getBlockState().getValue(CutBlock.LIGHT_FACES)!=faces)
            level.setBlock(worldPosition,getBlockState().setValue(CutBlock.LIGHT_FACES,faces),3);
        level.getChunkSource().getLightEngine().checkBlock(worldPosition);
    }
    @Override public void onLoad(){
        super.onLoad();
        if(level instanceof net.minecraft.server.level.ServerLevel server
                &&getBlockState().getValue(CutBlock.LIGHT_FACES)!=CutCells.opaqueFaces(cells()))
            server.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    @Override public ModelData getModelData() { return ModelData.builder().with(MATERIAL, material).with(CELLS,cells()).with(POSITION,worldPosition).build(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider); tag.putString("material", BuiltInRegistries.BLOCK.getKey(material.getBlock()).toString());
        if(cells!=null){var list=new net.minecraft.nbt.ListTag();for(var cell:cells)list.add(net.minecraft.nbt.StringTag.valueOf(cell==null?"":BuiltInRegistries.BLOCK.getKey(cell.getBlock()).toString()));tag.put("cells",list);}
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        var id = ResourceLocation.tryParse(tag.getString("material"));
        material = id != null && BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id).defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
        if (material.is(CuttingContent.PIECE.get())) material = Blocks.COBBLESTONE.defaultBlockState();
        cells=null;
        if(tag.contains("cells")){var list=tag.getList("cells",net.minecraft.nbt.Tag.TAG_STRING);if(list.size()==8){cells=new BlockState[8];for(int i=0;i<8;i++){var key=ResourceLocation.tryParse(list.getString(i));if(key!=null && BuiltInRegistries.BLOCK.containsKey(key)){var state=BuiltInRegistries.BLOCK.get(key).defaultBlockState();if(CuttingContent.cuttable(state))cells[i]=state;}}}}
        requestModelDataUpdate();
        // Material packets can arrive after the section was meshed with the fallback material.
        // Refreshing ModelData alone does not invalidate that already-built chunk mesh.
        if(level!=null && level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
