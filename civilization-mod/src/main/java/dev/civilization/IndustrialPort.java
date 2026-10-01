package dev.civilization;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import static dev.civilization.IndustrialBlock.Kind;
/** Sided NeoForge fluid capability; a cached handler rechecks claim ownership on every operation. */
public final class IndustrialPort implements IFluidHandler {
    private final IndustrialBlockEntity machine;private final Direction side;private final FluidTank tank;private final boolean accepts,extracts;
    public IndustrialPort(IndustrialBlockEntity machine,Direction side){this.machine=machine;this.side=side;
        boolean output=machine.kind==Kind.PUMP&&side==machine.front();
        boolean lube=machine.kind==Kind.DRILL&&side==machine.front().getCounterClockWise();
        tank=lube?machine.lubricant:output?machine.output:machine.input;
        accepts=lube||machine.kind==Kind.TANK||!output&&side==machine.front().getOpposite()&&machine.kind==Kind.DRILL;
        extracts=machine.kind==Kind.TANK||output;
    }
    private boolean allowed(){var l=machine.getLevel();var at=machine.getBlockPos();return l!=null&&!machine.isRemoved()&&l.hasChunkAt(at.relative(side))&&CivicAccess.boundary(l,at,at.relative(side));}
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int i){return i==0?tank.getFluid().copy():FluidStack.EMPTY;}
    @Override public int getTankCapacity(int i){return i==0?tank.getCapacity():0;}
    @Override public boolean isFluidValid(int i,FluidStack s){return i==0&&accepts&&tank.isFluidValid(s);}
    @Override public int fill(FluidStack s,FluidAction a){return accepts&&allowed()?tank.fill(s,a):0;}
    @Override public FluidStack drain(FluidStack s,FluidAction a){return extracts&&allowed()?tank.drain(s,a):FluidStack.EMPTY;}
    @Override public FluidStack drain(int n,FluidAction a){return extracts&&allowed()?tank.drain(n,a):FluidStack.EMPTY;}
}
