package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Stationary finite liquid. Eight native fluid heights represent eight 125 mB portions. */
public final class ReservoirOil extends LiquidBlock {
    public ReservoirOil(FlowingFluid fluid,Properties p){super(fluid,p);}
    @Override public ItemStack pickupBlock(Player p,LevelAccessor l,BlockPos pos,BlockState s){return ItemStack.EMPTY;}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q){return s;}
    public static class Source extends BaseFlowingFluid.Source {
        public Source(BaseFlowingFluid.Properties p){super(p);}
        @Override public void tick(Level l,BlockPos p,FluidState s){if(l.getBlockState(p).is(IndustrialContent.SURFACE_OIL.get()))super.tick(l,p,s);}
        @Override protected boolean canBeReplacedWith(FluidState s,BlockGetter l,BlockPos p,Fluid f,Direction d){return false;}
    }
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(BaseFlowingFluid.Properties p){super(p);}
        @Override public void tick(Level l,BlockPos p,FluidState s){if(l.getBlockState(p).is(IndustrialContent.SURFACE_OIL.get()))super.tick(l,p,s);}
        @Override protected boolean canBeReplacedWith(FluidState s,BlockGetter l,BlockPos p,Fluid f,Direction d){return false;}
    }
}
