package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;

public final class CoalWorkfaceBlock extends Block implements EntityBlock {
    public CoalWorkfaceBlock(){super(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_BLOCK).strength(-1,3600000).noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CoalWorkfaceEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return l.getBlockEntity(p) instanceof CoalWorkfaceEntity e?e.shape():Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
}
