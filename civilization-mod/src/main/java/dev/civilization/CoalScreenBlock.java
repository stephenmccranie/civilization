package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CoalScreenBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE=Shapes.or(box(1,0,1,4,10,4),box(12,0,1,15,10,4),box(1,0,12,4,10,15),box(12,0,12,15,10,15),box(0,9,0,16,14,16)).optimize();
    public CoalScreenBlock(){super(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3).noOcclusion());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CoalScreenEntity(p,s);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(l.getBlockEntity(p) instanceof CoalScreenEntity e&&CivicAccess.allowed(l,p,player)){e.use(player);return InteractionResult.CONSUME;}return InteractionResult.FAIL;
    }
    @Override protected void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(old.getBlock()!=next.getBlock()&&!l.isClientSide&&l.getBlockEntity(p) instanceof CoalScreenEntity e)e.spill();super.onRemove(old,l,p,next,moving);
    }
}
