package dev.civilization;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
/** One side of a controller's tank as a NeoForge fluid capability. Liquid passes only the way the port allows, and a cached handler rechecks claim ownership on every operation. */
public final class FluidPort implements IFluidHandler {
    private final BlockEntity owner;private final Direction side;private final FluidTank tank;private final boolean accepts,extracts;
    public FluidPort(BlockEntity owner,Direction side,FluidTank tank,boolean accepts,boolean extracts){this.owner=owner;this.side=side;this.tank=tank;this.accepts=accepts;this.extracts=extracts;}
    private boolean allowed(){var l=owner.getLevel();var at=owner.getBlockPos();return l!=null&&!owner.isRemoved()&&Chunks.loaded(l,at.relative(side))&&CivicAccess.boundary(l,at,at.relative(side));}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int i){return i==0?tank.getFluid().copy():FluidStack.EMPTY;}
    @Override public int getTankCapacity(int i){return i==0?tank.getCapacity():0;}
    @Override public boolean isFluidValid(int i,FluidStack s){return i==0&&accepts&&tank.isFluidValid(s);}
    @Override public int fill(FluidStack s,FluidAction a){return accepts&&allowed()?tank.fill(s,a):0;}
    @Override public FluidStack drain(FluidStack s,FluidAction a){return extracts&&allowed()?tank.drain(s,a):FluidStack.EMPTY;}
    @Override public FluidStack drain(int n,FluidAction a){return extracts&&allowed()?tank.drain(n,a):FluidStack.EMPTY;}
}
