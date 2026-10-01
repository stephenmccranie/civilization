package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
import static dev.civilization.IndustrialBlock.Kind;

/** Machines share fixed buffers. Transfers and completed batches run only on the server thread. */
public final class IndustrialBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, CoalFireHost {
    public final Kind kind;
    private final NonNullList<ItemStack> items=NonNullList.withSize(8,ItemStack.EMPTY);
    public final FluidTank input,output,lubricant;
    public int heat,progress,status;
    public final CoalFire fire=new CoalFire(this);
    public int fireHeat(){return heat;}
    public void fireHeat(int value){heat=value;}
    public int coalCapacity(){return ProductionEnergy.OIL_MB_PER_COAL;}
    public int coalBudget(){return coalCapacity();}
    public int[] coalFuelSlots(){return MachineInventory.INDUSTRIAL_INPUT;}
    public boolean coalFireIgnoresRain(){return kind==Kind.PUMP;}
    public boolean coalPowered(){return kind==Kind.PUMP||kind==Kind.REFINERY;}
    public int lubrication=1000;
    private final DepositWork work=new DepositWork();
    private Deposits.Site site;
    private boolean surveyed,firePaused;
    public boolean formed,derrickBuilt;
    public final java.util.List<ItemStack> derrickMaterials=new java.util.ArrayList<>();
    private int lastPumpView = -1;
    public IndustrialBlockEntity(BlockPos p,BlockState s){super(IndustrialContent.ENTITY.get(),p,s);kind=((IndustrialBlock)s.getBlock()).kind;
        input=tank(kind==Kind.TANK?16000:4000,stack->switch(kind){case TANK->IndustrialContent.industrial(stack);case REFINERY->stack.is(IndustrialContent.CRUDE.get());case COLUMN->stack.is(IndustrialContent.HEATED.get());case CONDENSER->stack.is(IndustrialContent.VAPOR.get());case DRILL->stack.is(IndustrialContent.FUEL.get());default->false;});
        output=tank(4000,stack->stack.is(switch(kind){case PUMP->IndustrialContent.CRUDE.get();case REFINERY->IndustrialContent.HEATED.get();case COLUMN->IndustrialContent.VAPOR.get();default->IndustrialContent.FUEL.get();}));
        lubricant=tank(4000,stack->stack.is(IndustrialContent.LUBE.get()));
    }
    private FluidTank tank(int capacity,java.util.function.Predicate<FluidStack> validator){return new FluidTank(capacity,validator){@Override protected void onContentsChanged(){setChanged();}};}
    public Direction front(){return getBlockState().getValue(CivicBlock.FACING);}
    public int duration(){return switch(kind){case PUMP->IndustrialRates.PUMP_TICKS;case DRILL->lubricant.getFluidAmount()>=10?100:100000/Math.max(200,lubrication);case REFINERY,COLUMN,CONDENSER->IndustrialProcess.forKind(kind).ticks();case TANK->1;};}

    public void tick(){
        if(!(level instanceof ServerLevel server)||kind==Kind.TANK)return;
        if(server.getGameTime()%IndustrialRates.STEP_TICKS!=0)return;
        process();
        if(kind==Kind.PUMP && (server.getGameTime()%20==0 || lastPumpView<0)){
            int view=output.getFluidAmount()+(formed?8192:0);
            if(view!=lastPumpView){lastPumpView=view;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
        }
    }
    void process(){
        firePaused=false;processWork();if(!firePaused&&coalPowered()&&level!=null&&!level.isClientSide){fire.finish(IndustrialRates.STEP_TICKS,status==MachineStatus.Industry.WORKING);syncFire();}
    }
    private void syncFire(){var state=getBlockState();if(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)!=fire.lit())level.setBlock(worldPosition,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,fire.lit()),3);}
    private void processWork(){
        if(!(level instanceof ServerLevel server)||kind==Kind.TANK)return;
        int structure=IndustrialStructure.bindStatus(this);
        if(structure==MachineStructure.UNLOADED){formed=false;firePaused=true;pause(MachineStatus.Industry.UNLOADED);return;}
        if(structure!=MachineStructure.COMPLETE){if(kind==Kind.PUMP&&derrickBuilt)ModeledDerrick.dismantle(this);formed=false;if(coalPowered())fire.extinguish();pause(MachineStatus.Industry.INCOMPLETE);return;}
        formed=true;
        if(kind==Kind.REFINERY&&MachineWeather.wetWorkFace(level,worldPosition,getBlockState())){pause(MachineStatus.Industry.WET);return;}
        if(coalPowered()&&!fire.lit()){pause(MachineStatus.Industry.FIRE_UNLIT);return;}
        MachineInventory.refill(this,MachineInventory.INDUSTRIAL_INPUT);
        transfer(true);
        if(kind==Kind.COLUMN||kind==Kind.CONDENSER){refine();return;}
        if(!surveyed&&(kind==Kind.PUMP||kind==Kind.DRILL)){site=Deposits.at(server,worldPosition);surveyed=true;}
        DepositWork.Target target=null;
        if(kind==Kind.PUMP||kind==Kind.DRILL){
            if(site==null||site.kind()!=(kind==Kind.PUMP?Deposits.Kind.OIL:Deposits.Kind.COAL)){pause(MachineStatus.Industry.WRONG_SITE);return;}
            target=work.find(server,site,worldPosition);if(target.status()!=MachineStatus.Industry.WORKING){pause(target.status());return;}
        }
        int amount=target==null?IndustrialProcess.forKind(kind).inputMb():target.units();
        if(kind==Kind.DRILL){if(!MachineInventory.room(this,MachineInventory.INDUSTRIAL_OUTPUT,KilnContent.MINERAL_COAL.toStack(amount))){pause(MachineStatus.Industry.OUTPUT_FULL);return;}if(input.getFluidAmount()<IndustrialRates.DRILL_FUEL_MB){pause(MachineStatus.Industry.NEEDS_FUEL);return;}}
        else{
            if(output.getSpace()<amount){pause(MachineStatus.Industry.OUTPUT_FULL);return;}
            if(kind==Kind.REFINERY&&input.getFluidAmount()<amount){pause(MachineStatus.Industry.NEEDS_INPUT);return;}
            int cost=kind==Kind.PUMP?amount:(int)Math.ceil(amount/ThermalField.efficiency(level,worldPosition));
            int needed=Math.max(0,(cost-heat+ProductionEnergy.OIL_MB_PER_COAL-1)/ProductionEnergy.OIL_MB_PER_COAL);
            int available=0;for(int slot:MachineInventory.INDUSTRIAL_INPUT)if(items.get(slot).is(KilnContent.MINERAL_COAL.get()))available+=items.get(slot).getCount();
            if(needed>available){pause(MachineStatus.Industry.NEEDS_FUEL);return;}
        }
        status=MachineStatus.Industry.WORKING;working(true);progress+=IndustrialRates.STEP_TICKS;setChanged();
        if(progress<duration())return;
        progress=0;
        if(kind==Kind.DRILL){
            if(!work.extract(server,site,worldPosition,target))return;
            input.drain(IndustrialRates.DRILL_FUEL_MB,EXECUTE);if(lubricant.getFluidAmount()>=10){lubricant.drain(10,EXECUTE);lubrication=1000;}else lubrication=Math.max(200,lubrication-10);MachineInventory.insert(this,MachineInventory.INDUSTRIAL_OUTPUT,KilnContent.MINERAL_COAL.toStack(amount));
        }else{
            int cost=kind==Kind.PUMP?amount:(int)Math.ceil(amount/ThermalField.efficiency(level,worldPosition));
            if(kind==Kind.PUMP&&!work.extract(server,site,worldPosition,target))return;
            if(!fire.supply(cost))return;
            heat-=cost;
            fire.spent(cost);
            if(kind==Kind.REFINERY)input.drain(amount,EXECUTE);
            output.fill(new FluidStack(kind==Kind.PUMP?IndustrialContent.CRUDE.get():IndustrialContent.HEATED.get(),amount),EXECUTE);
        }
        EnergyLog.machine(level,worldPosition,"industry_"+kind.name().toLowerCase(java.util.Locale.ROOT)+"_batch",kind==Kind.DRILL?"civilization:refined_fuel_mb":kind==Kind.REFINERY?"civilization:crude_oil_mb":"civilization:oil_reserve_mb",kind==Kind.DRILL?IndustrialRates.DRILL_FUEL_MB:kind==Kind.REFINERY?250:amount,kind==Kind.DRILL?"civilization:mineral_coal":kind==Kind.REFINERY?"civilization:heated_crude_mb":"civilization:crude_oil_mb",amount);
        setChanged();
    }
    private void working(boolean active){if(level!=null&&getBlockState().getValue(MachineFeedback.WORKING)!=active)level.setBlock(worldPosition,getBlockState().setValue(MachineFeedback.WORKING,active),3);}
    private void pause(int state){status=state;working(false);if(progress!=0){progress=0;setChanged();}}
    private void refine(){
        boolean column=kind==Kind.COLUMN;var batch=IndustrialProcess.forKind(kind);int required=batch.inputMb(),produced=batch.outputMb();
        if(input.getFluidAmount()<required){pause(MachineStatus.Industry.NEEDS_INPUT);return;}
        var sulfur=items.get(1);
        if(output.getSpace()<produced||column&&(lubricant.getSpace()<batch.oilMb()||!MachineInventory.room(this,MachineInventory.INDUSTRIAL_OUTPUT,IndustrialContent.SULFUR.toStack(batch.sulfur())))){pause(MachineStatus.Industry.OUTPUT_FULL);return;}
        status=MachineStatus.Industry.WORKING;working(true);progress+=IndustrialRates.STEP_TICKS;setChanged();if(progress<duration())return;progress=0;
        input.drain(required,EXECUTE);output.fill(new FluidStack(batch.output(),produced),EXECUTE);
        if(column){lubricant.fill(new FluidStack(IndustrialContent.LUBE.get(),batch.oilMb()),EXECUTE);MachineInventory.insert(this,MachineInventory.INDUSTRIAL_OUTPUT,IndustrialContent.SULFUR.toStack(batch.sulfur()));}
        EnergyLog.machine(level,worldPosition,"industry_"+kind.name().toLowerCase(java.util.Locale.ROOT)+"_batch",column?"civilization:heated_crude_mb":"civilization:distillate_vapor_mb",required,column?"civilization:distillate_vapor_mb":"civilization:refined_fuel_mb",produced);
        if(column)EnergyLog.machine(level,worldPosition,"industry_column_coproduct","civilization:lubricating_oil_mb",200,"civilization:sulfur",1);
    }
    public void transfer(){transfer(false);}
    private void transfer(boolean alreadyBound){
        if(level==null)return;
        if(IndustrialStructure.remote(kind)){
            if(!alreadyBound&&!IndustrialStructure.bind(this))return;
            for(var part:IndustrialStructure.parts(kind))if(part.material().endsWith("_port")){
                var at=MachineStructure.position(worldPosition,front(),part);if(!CivicAccess.boundary(level,worldPosition,at))continue;
                PipeRouting.transfer(level,at,MachineStructure.side(part,front()),part.material().equals("input_port")?input:part.material().equals("aux_port")?lubricant:output,!part.material().equals("input_port"));
            }
        }else if(kind==Kind.DRILL){PipeRouting.transfer(level,worldPosition,front().getOpposite(),input,false);PipeRouting.transfer(level,worldPosition,front().getCounterClockWise(),lubricant,false);}
    }
    public void canister(Player p,InteractionHand hand){
        var held=p.getItemInHand(hand);Item result=null;
        if(held.is(IndustrialContent.CAN.get())){
            for(var tank:kind==Kind.TANK?java.util.List.of(input):java.util.List.of(output,lubricant,input))if(tank.getFluidAmount()>=1000&&IndustrialContent.can(tank.getFluid())!=null){result=IndustrialContent.can(tank.getFluid());tank.drain(1000,EXECUTE);break;}
        }else {
            net.minecraft.world.level.material.Fluid type=held.is(IndustrialContent.CRUDE_CAN.get())?IndustrialContent.CRUDE.get():held.is(IndustrialContent.FUEL_CAN.get())?IndustrialContent.FUEL.get():held.is(IndustrialContent.LUBE_CAN.get())?IndustrialContent.LUBE.get():null;
            if(type!=null){var fluid=new FluidStack(type,1000);var tank=kind==Kind.DRILL&&type==IndustrialContent.LUBE.get()?lubricant:input;if(tank.fill(fluid,SIMULATE)==1000){tank.fill(fluid,EXECUTE);result=IndustrialContent.CAN.get();}}
        }
        if(result!=null)p.setItemInHand(hand,new ItemStack(result));else p.displayClientMessage(Component.literal("Needs 1,000 mB available or room for one full canister of the right liquid."),true);
    }
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->kind.ordinal();case 1->input.getFluidAmount();case 2->output.getFluidAmount();case 3->IndustrialContent.fluidId(input.getFluid());case 4->progress;case 5->status;case 6->heat;case 7->lubricant.getFluidAmount();case 8->kind==Kind.DRILL&&lubricant.getFluidAmount()>=10?1000:lubrication;case 9->duration();case 10->IndustrialContent.fluidId(output.getFluid());case 11->coalPowered()?fire.state():0;case 12->fire.remaining();case 13->derrickBuilt?1:0;default->0;};}
        public void set(int i,int v){}public int getCount(){return 14;}
    };
    @Override public void onLoad(){
        super.onLoad();
        if(level!=null&&!level.isClientSide&&(kind==Kind.PUMP||kind==Kind.REFINERY)){
            for(int slot:new int[]{3,4})if(!items.get(slot).isEmpty()){
                net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,items.get(slot));
                items.set(slot,ItemStack.EMPTY);setChanged();
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putBoolean("derrickBuilt",derrickBuilt);var paid=new net.minecraft.nbt.ListTag();for(var stack:derrickMaterials)if(!stack.isEmpty())paid.add(stack.save(r));t.put("derrickMaterials",paid);fire.save(t);ContainerHelper.saveAllItems(t,items,r);t.put("input",input.writeToNBT(r,new CompoundTag()));t.put("output",output.writeToNBT(r,new CompoundTag()));t.put("lubricant",lubricant.writeToNBT(r,new CompoundTag()));t.putInt("lubrication",lubrication);t.putInt("cursor",work.cursor);t.putInt("heat",heat);t.putInt("progress",progress);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);derrickBuilt=t.getBoolean("derrickBuilt");derrickMaterials.clear();for(var tag:t.getList("derrickMaterials",10))derrickMaterials.add(ItemStack.parseOptional(r,(CompoundTag)tag));fire.load(t);ContainerHelper.loadAllItems(t,items,r);input.readFromNBT(r,t.getCompound("input"));output.readFromNBT(r,t.getCompound("output"));lubricant.readFromNBT(r,t.getCompound("lubricant"));lubrication=t.contains("lubrication")?Math.clamp(t.getInt("lubrication"),200,1000):1000;work.cursor=Math.max(0,t.getInt("cursor"));heat=Math.clamp(t.getInt("heat"),0,1000);progress=Math.clamp(t.getInt("progress"),0,duration()-1);formed=t.getBoolean("formed");surveyed=false;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=saveWithoutMetadata(r);t.putBoolean("formed",formed);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new IndustrialMenu(id,inv,this,data);}
    @Override public int[] getSlotsForFace(Direction side){return (kind==Kind.DRILL||kind==Kind.COLUMN)&&side==Direction.DOWN?MachineInventory.INDUSTRIAL_OUTPUT:(kind==Kind.PUMP||kind==Kind.REFINERY)&&side!=Direction.DOWN?MachineInventory.INDUSTRIAL_INPUT:new int[0];}
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack stack,Direction side){return side!=Direction.DOWN&&canPlaceItem(i,stack);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack stack,Direction side){return (kind==Kind.DRILL||kind==Kind.COLUMN)&&MachineInventory.contains(MachineInventory.INDUSTRIAL_OUTPUT,i)&&side==Direction.DOWN;}
    @Override public int getContainerSize(){return 8;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int count){var s=ContainerHelper.removeItem(items,i,count);if(!s.isEmpty())setChanged();return s;}
    @Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack stack){items.set(i,stack);stack.limitSize(stack.getMaxStackSize());setChanged();}
    @Override public boolean canPlaceItem(int i,ItemStack s){return MachineInventory.contains(MachineInventory.INDUSTRIAL_INPUT,i)&&(kind==Kind.PUMP||kind==Kind.REFINERY)&&s.is(KilnContent.MINERAL_COAL.get());}
    @Override public boolean stillValid(Player p){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64&&CivicAccess.allowed(level,worldPosition,p);}
    @Override public void clearContent(){items.clear();setChanged();}
}
