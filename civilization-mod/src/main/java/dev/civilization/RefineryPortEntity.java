package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
/** No liquid duplicated here: the port exposes a validated controller buffer. */
public final class RefineryPortEntity extends BlockEntity {
    private BlockPos controller;private String role="";
    /** Placed by a section of its controller's native model, so it belongs to that section rather than to its finder. */
    private boolean assembly;
    public boolean assembly(){return assembly;}
    private java.util.OptionalInt section(IndustrialBlockEntity m){
        if(m.model()==null)return java.util.OptionalInt.empty();
        for(var port:m.model().ports)if(port.part().material().equals(role)&&MachineStructure.position(controller,m.front(),port.part()).equals(worldPosition))return java.util.OptionalInt.of(port.section());
        return java.util.OptionalInt.empty();
    }
    public void breakAssembly(){var m=owner();if(assembly&&m!=null&&!m.build().changing)section(m).ifPresent(part->ModeledMachine.removeSection(m,part,true));}
    public boolean keepAssemblyCheck(){
        if(!assembly||level==null||controller==null)return false;
        if(!Chunks.loaded(level,controller))return true;
        var m=owner();if(m==null||section(m).stream().noneMatch(m.build()::has)){level.removeBlock(worldPosition,false);return false;}return true;
    }
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide&&assembly)level.scheduleTick(worldPosition,getBlockState().getBlock(),100);}
    public RefineryPortEntity(BlockPos p,BlockState s){super(IndustrialContent.PORT_ENTITY.get(),p,s);}
    private IndustrialBlockEntity owner(){
        if(level==null||controller==null||!Chunks.loaded(level,controller)||!(level.getBlockEntity(controller) instanceof IndustrialBlockEntity m))return null;
        for(var part:IndustrialStructure.parts(m.kind))if(part.material().equals(role)&&MachineStructure.position(controller,m.front(),part).equals(worldPosition))return m;
        return null;
    }
    public net.minecraft.network.chat.Component description(){var m=owner();return net.minecraft.network.chat.Component.literal(m==null?"Port without a machine · right-click a machine's controller with ports to fit them":m.getDisplayName().getString()+" | "+(role.equals("aux_port")?"Lubricating oil output":role.equals("input_port")?"Intake":"Output"));}
    public boolean boundTo(BlockPos at){return at.equals(controller);}
    public boolean canBind(IndustrialBlockEntity m){var old=owner();return old==null||old==m||!IndustrialStructure.complete(old);}
    public void bind(IndustrialBlockEntity m,String role,Direction side){
        if(m.model()!=null&&!assembly){assembly=true;setChanged();level.scheduleTick(worldPosition,getBlockState().getBlock(),100);}
        if(!m.getBlockPos().equals(controller)||!this.role.equals(role)){controller=m.getBlockPos();this.role=role;setChanged();level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}
        if(getBlockState().getValue(RefineryPortBlock.FACING)!=side)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(RefineryPortBlock.FACING,side));
    }
    public IFluidHandler handler(Direction side){return new IFluidHandler(){
        private net.neoforged.neoforge.fluids.capability.templates.FluidTank tank(){
            var m=owner();if(m==null||isRemoved()||side!=getBlockState().getValue(RefineryPortBlock.FACING)||!Chunks.loaded(level,worldPosition.relative(side))||!CivicAccess.boundary(level,controller,worldPosition)||!CivicAccess.boundary(level,worldPosition,worldPosition.relative(side))||!IndustrialStructure.complete(m))return null;
            return role.equals("input_port")?m.input:role.equals("aux_port")?m.lubricant:m.output;
        }
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int i){var t=tank();return i==0&&t!=null?t.getFluid().copy():FluidStack.EMPTY;}
        public int getTankCapacity(int i){var t=tank();return i==0&&t!=null?t.getCapacity():0;}
        public boolean isFluidValid(int i,FluidStack s){var t=tank();return i==0&&t!=null&&role.equals("input_port")&&t.isFluidValid(s);}
        public int fill(FluidStack s,FluidAction a){var t=tank();return t!=null&&role.equals("input_port")?t.fill(s,a):0;}
        public FluidStack drain(int n,FluidAction a){var t=tank();return t!=null&&!role.equals("input_port")?t.drain(n,a):FluidStack.EMPTY;}
        public FluidStack drain(FluidStack s,FluidAction a){var t=tank();return t!=null&&!role.equals("input_port")?t.drain(s,a):FluidStack.EMPTY;}
    };}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putBoolean("assembly",assembly);if(controller!=null)t.putLong("controller",controller.asLong());t.putString("role",role);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);assembly=t.getBoolean("assembly");controller=t.contains("controller")?BlockPos.of(t.getLong("controller")):null;role=t.getString("role");}
}
