package dev.civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
/** Stackable narrow flue; its physical bounds match its visible shaft and collars. */
public final class RefineryFlueBlock extends Block {
    private static final VoxelShape SHAPE=Shapes.or(Block.box(4,0,4,12,16,12),Block.box(2,0,2,14,2,14),Block.box(2,14,2,14,16,14));
    public RefineryFlueBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
}
