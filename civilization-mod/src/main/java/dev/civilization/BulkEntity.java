package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;

/** One authoritative balance; adapters never cache authority or duplicate cargo. */
public final class BulkEntity extends BlockEntity implements MenuProvider {
    public static final int COAL_CAPACITY=8192, FLUID_CAPACITY=64000;
    public final boolean liquid;
    private int coal;
    private boolean dirty;
    public boolean formed;
    private final FluidTank tank=new FluidTank(FLUID_CAPACITY,BulkEntity::supported){@Override protected void onContentsChanged(){changed();}};
    public BulkEntity(BlockPos p,BlockState s){super(BulkContent.ENTITY.get(),p,s);liquid=((BulkBlock)s.getBlock()).liquid;}
    public Direction front(){return getBlockState().getValue(CivicBlock.FACING);}
    public int amount(){return liquid?tank.getFluidAmount():coal;}
    public int capacity(){return liquid?FLUID_CAPACITY:COAL_CAPACITY;}
    public FluidStack fluid(){return tank.getFluid().copy();}
    public static boolean supported(FluidStack s){return s.is(IndustrialContent.CRUDE.get())||s.is(IndustrialContent.FUEL.get())||s.is(IndustrialContent.LUBE.get());}
    public boolean ready(){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&BulkStructure.complete(level,worldPosition,front(),liquid);}
    public boolean valid(Player p){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(CivicAccess.world(level,worldPosition))<=64&&CivicAccess.allowed(level,worldPosition,p);}
    private boolean automation(Direction side){return ready()&&level.hasChunkAt(worldPosition.relative(side))&&CivicAccess.boundary(level,worldPosition,worldPosition.relative(side));}
    private void changed(){setChanged();dirty=true;}
    public void tick(){
        if(level.getGameTime()%10!=0)return;
        boolean current=ready();if(current!=formed){formed=current;dirty=true;}
        // Local pipes remain the existing 25 mB / half-second transport, not powered dock throughput.
        if(liquid&&formed)PipeRouting.transfer(level,worldPosition,front(),tank,false);
        if(dirty){dirty=false;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    }
    public int insertCoal(int count,boolean simulate){if(liquid||count<=0||!ready())return 0;int n=Math.min(count,COAL_CAPACITY-coal);if(n>0&&!simulate){coal+=n;changed();}return n;}
    public int extractCoal(int count,boolean simulate){if(liquid||count<=0||!ready())return 0;int n=Math.min(count,coal);if(n>0&&!simulate){coal-=n;changed();}return n;}
    public IItemHandler items(Direction side){return new IItemHandler(){
        // Empty ingress plus a bounded egress view avoids vanilla hoppers treating
        // a 32-item view as the entire store being full. Neither slot owns cargo.
        public int getSlots(){return 2;}
        public ItemStack getStackInSlot(int slot){return slot==1&&automation(side)&&coal>0?KilnContent.MINERAL_COAL.toStack(Math.min(32,coal)):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot>=0&&slot<2?32:0;}
        public boolean isItemValid(int slot,ItemStack s){return slot==0&&s.is(KilnContent.MINERAL_COAL.get());}
        public ItemStack insertItem(int slot,ItemStack s,boolean simulate){if(!isItemValid(slot,s)||!automation(side))return s;int n=insertCoal(Math.min(32,s.getCount()),simulate);var rest=s.copy();rest.shrink(n);return rest;}
        public ItemStack extractItem(int slot,int n,boolean simulate){if(slot!=1||!automation(side))return ItemStack.EMPTY;int got=extractCoal(Math.min(32,n),simulate);return got==0?ItemStack.EMPTY:KilnContent.MINERAL_COAL.toStack(got);}
    };}
    public IFluidHandler fluids(Direction side){return new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int i){return i==0?fluid():FluidStack.EMPTY;}
        public int getTankCapacity(int i){return i==0?FLUID_CAPACITY:0;}
        public boolean isFluidValid(int i,FluidStack s){return liquid&&i==0&&supported(s);}
        public int fill(FluidStack s,FluidAction a){return liquid&&automation(side)?tank.fill(s,a):0;}
        public FluidStack drain(int n,FluidAction a){return liquid&&automation(side)?tank.drain(n,a):FluidStack.EMPTY;}
        public FluidStack drain(FluidStack s,FluidAction a){return liquid&&automation(side)?tank.drain(s,a):FluidStack.EMPTY;}
    };}
    public static boolean canister(ItemStack s){return s.is(IndustrialContent.CAN.get())||s.is(IndustrialContent.CRUDE_CAN.get())||s.is(IndustrialContent.FUEL_CAN.get())||s.is(IndustrialContent.LUBE_CAN.get());}
    public void useHeld(Player p,InteractionHand hand){
        if(!valid(p)||!ready())return;var held=p.getItemInHand(hand);
        if(!liquid){if(held.is(KilnContent.MINERAL_COAL.get()))held.shrink(insertCoal(held.getCount(),false));return;}
        if(held.is(IndustrialContent.CAN.get())){
            if(tank.getFluidAmount()<1000)return;var f=tank.getFluid();var filled=f.is(IndustrialContent.CRUDE.get())?IndustrialContent.CRUDE_CAN:f.is(IndustrialContent.FUEL.get())?IndustrialContent.FUEL_CAN:IndustrialContent.LUBE_CAN;
            tank.drain(1000,EXECUTE);exchange(p,hand,filled.toStack());
        }else if(canister(held)){
            var f=held.is(IndustrialContent.CRUDE_CAN.get())?IndustrialContent.CRUDE.get():held.is(IndustrialContent.FUEL_CAN.get())?IndustrialContent.FUEL.get():IndustrialContent.LUBE.get();
            var stack=new FluidStack(f,1000);if(tank.fill(stack,SIMULATE)==1000){tank.fill(stack,EXECUTE);exchange(p,hand,IndustrialContent.CAN.toStack());}
        }
    }
    private static void exchange(Player p,InteractionHand hand,ItemStack result){var held=p.getItemInHand(hand);held.shrink(1);if(held.isEmpty())p.setItemInHand(hand,result);else if(!p.getInventory().add(result))p.drop(result,false);}
    public boolean control(Player p,int action){
        if(!valid(p)||!ready()||action<0||action>1)return false;
        if(liquid){useHeld(p,InteractionHand.MAIN_HAND);return true;}
        if(action==0){for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(KilnContent.MINERAL_COAL.get())){s.shrink(insertCoal(Math.min(32,s.getCount()),false));p.getInventory().setChanged();break;}}}
        else {int n=Math.min(32,coal);var out=n==0?ItemStack.EMPTY:KilnContent.MINERAL_COAL.toStack(n);p.getInventory().add(out);extractCoal(n-out.getCount(),false);}
        return true;
    }
    // ContainerData travels as signed shorts; split full 64,000 mB quantities explicitly.
    public final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->amount()&65535;case 1->amount()>>>16;case 2->liquid?1:0;case 3->ready()?1:0;case 4->liquid?IndustrialContent.fluidId(tank.getFluid()):0;default->0;};}public void set(int i,int v){}public int getCount(){return 5;}};
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putInt("coal",coal);t.put("fluid",tank.writeToNBT(r,new CompoundTag()));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);coal=liquid?0:Math.clamp(t.getInt("coal"),0,COAL_CAPACITY);tank.readFromNBT(r,t.getCompound("fluid"));if(!liquid||!tank.isEmpty()&&!supported(tank.getFluid()))tank.setFluid(FluidStack.EMPTY);else if(tank.getFluidAmount()>FLUID_CAPACITY)tank.setFluid(tank.getFluid().copyWithAmount(FLUID_CAPACITY));formed=t.getBoolean("formed");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=saveWithoutMetadata(r);t.putBoolean("formed",formed);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new BulkMenu(id,inv,this,data);}
}
