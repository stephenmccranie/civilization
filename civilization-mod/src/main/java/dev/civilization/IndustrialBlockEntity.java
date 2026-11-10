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
public final class IndustrialBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, CoalFireHost, ModeledController, Engine {
    public final Kind kind;
    private final NonNullList<ItemStack> items=NonNullList.withSize(8,ItemStack.EMPTY);
    public final FluidTank input,output,lubricant;
    public int progress,status;
    public final CoalFire fire=new CoalFire(this);
    /** The Coal Engine's brick firebox and the Fired Heater's brick hearth. */
    public int masonry(){return kind==Kind.COAL_ENGINE?4:20;}
    public int[] coalFuelSlots(){return MachineInventory.INDUSTRIAL_INPUT;}
    /** The Coal Engine's fire burns behind its firedoor. */
    public boolean coalFireIgnoresRain(){return kind==Kind.COAL_ENGINE;}
    public boolean coalPowered(){return kind==Kind.COAL_ENGINE||kind==Kind.REFINERY;}
    /** What a machine's input slots take: the derrick's casing, or Coal for a fire. */
    public static Item supply(Kind kind){return switch(kind){case DERRICK->ManufacturingContent.part("iron","plate");case REFINERY,COAL_ENGINE->KilnContent.MINERAL_COAL.get();default->null;};}
    /** The derrick's well: blocks drilled below its floor, blocks of that cased, whether the bit has struck the oil body,
     *  and the engine work given towards the next block or pumped step. */
    public int depth,cased;
    public boolean struck;
    public double effort;
    /** The engine driving the derrick this step, and whether it gave any work since the last. */
    private BlockPos engine;
    private boolean powered;
    /** The Coal Engine's last tick of work. */
    private long workedTick=Long.MIN_VALUE;
    public int lubrication=1000;
    private final DepositWork work=new DepositWork();
    private Deposits.Site site;
    private boolean surveyed,firePaused;
    public boolean formed;
    private final ModeledBuild build=new ModeledBuild();
    private int lastPumpView = -1;
    public IndustrialBlockEntity(BlockPos p,BlockState s){super(IndustrialContent.ENTITY.get(),p,s);kind=((IndustrialBlock)s.getBlock()).kind;
        input=tank(kind==Kind.TANK?16000:4000,stack->switch(kind){case TANK->IndustrialContent.industrial(stack);case REFINERY->stack.is(IndustrialContent.CRUDE.get());case COLUMN->stack.is(IndustrialContent.HEATED.get());case CONDENSER->stack.is(IndustrialContent.VAPOR.get());case DRILL->stack.is(IndustrialContent.FUEL.get());default->false;});
        output=tank(4000,stack->stack.is(switch(kind){case DERRICK->IndustrialContent.CRUDE.get();case REFINERY->IndustrialContent.HEATED.get();case COLUMN->IndustrialContent.VAPOR.get();default->IndustrialContent.FUEL.get();}));
        lubricant=tank(4000,stack->stack.is(IndustrialContent.LUBE.get()));
    }
    private FluidTank tank(int capacity,java.util.function.Predicate<FluidStack> validator){return new FluidTank(capacity,validator){@Override protected void onContentsChanged(){setChanged();}};}
    public Direction front(){return getBlockState().getValue(CivicBlock.FACING);}
    @Override public MachineModel model(){return ((ModeledBlock)getBlockState().getBlock()).model();}
    @Override public ModeledBuild build(){return build;}
    /** The derrick's beam rocks and the engine's flywheel turns while work flows; the engine's firedoor glows while lit. */
    @Override public boolean working(){return formed&&getBlockState().getValue(MachineFeedback.WORKING);}
    @Override public boolean lit(){return formed&&getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);}
    @Override public void sectionsChanged(){formed=false;}
    public int duration(){return switch(kind){case DERRICK->struck?IndustrialRates.PUMP_TICKS:IndustrialRates.DRILL_TICKS;case DRILL->lubricant.getFluidAmount()>=10?100:100000/Math.max(200,lubrication);case REFINERY,COLUMN,CONDENSER->IndustrialProcess.forKind(kind).ticks();case TANK,COAL_ENGINE->1;};}
    /** The derrick's well column, under the walking beam's head. */
    public BlockPos well(){return worldPosition.relative(front().getOpposite(),IndustrialRates.WELL);}

    public void tick(){
        if(!(level instanceof ServerLevel server)||kind==Kind.TANK)return;
        if(engine!=null)drive();
        if(server.getGameTime()%IndustrialRates.STEP_TICKS!=0)return;
        process();
        if(kind==Kind.DERRICK && (server.getGameTime()%20==0 || lastPumpView<0)){
            int view=output.getFluidAmount()+(formed?8192:0);
            if(view!=lastPumpView){lastPumpView=view;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
        }
    }
    void process(){
        firePaused=false;processWork();if(!firePaused&&coalPowered()&&level!=null&&!level.isClientSide){fire.tick(IndustrialRates.STEP_TICKS);syncFire();}
    }
    private void syncFire(){var state=getBlockState();if(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)!=fire.lit())level.setBlock(worldPosition,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,fire.lit()),3);}
    private void processWork(){
        if(!(level instanceof ServerLevel server)||kind==Kind.TANK)return;
        if(model()!=null)ModeledMachine.reconcile(this);
        int structure=IndustrialStructure.bindStatus(this);
        if(structure==MachineStructure.UNLOADED){formed=false;firePaused=true;pause(MachineStatus.Industry.UNLOADED);return;}
        if(structure!=MachineStructure.COMPLETE){formed=false;if(coalPowered())fire.extinguish();pause(MachineStatus.Industry.INCOMPLETE);return;}
        formed=true;
        if(kind==Kind.REFINERY&&MachineWeather.wetWorkFace(level,worldPosition,getBlockState())){pause(MachineStatus.Industry.WET);return;}
        if(coalPowered()&&!fire.hot()){pause(fire.lit()?MachineStatus.Industry.WARMING:MachineStatus.Industry.FIRE_UNLIT);return;}
        if(kind==Kind.COAL_ENGINE){status=server.getGameTime()-workedTick<=IndustrialRates.STEP_TICKS?MachineStatus.Industry.WORKING:MachineStatus.Industry.READY;working(status==MachineStatus.Industry.WORKING);return;}
        MachineInventory.refill(this,MachineInventory.INDUSTRIAL_INPUT);
        transfer(true);
        if(kind==Kind.COLUMN||kind==Kind.CONDENSER){refine();return;}
        // The derrick's site is the one its well stands over, so a well always reaches the body it was sited on.
        if(!surveyed&&(kind==Kind.DERRICK||kind==Kind.DRILL)){site=Deposits.at(server,kind==Kind.DERRICK?well():worldPosition);surveyed=true;}
        if(kind==Kind.DERRICK){derrick(server);return;}
        DepositWork.Target target=null;
        if(kind==Kind.DRILL){
            if(site==null||site.kind()!=Deposits.Kind.COAL){pause(MachineStatus.Industry.WRONG_SITE);return;}
            target=work.find(server,site,worldPosition);if(target.status()!=MachineStatus.Industry.WORKING){pause(target.status());return;}
        }
        int amount=target==null?IndustrialProcess.forKind(kind).inputMb():target.units();
        if(kind==Kind.DRILL){if(!MachineInventory.room(this,MachineInventory.INDUSTRIAL_OUTPUT,KilnContent.MINERAL_COAL.toStack(amount))){pause(MachineStatus.Industry.OUTPUT_FULL);return;}if(input.getFluidAmount()<IndustrialRates.DRILL_FUEL_MB){pause(MachineStatus.Industry.NEEDS_FUEL);return;}}
        else{
            if(output.getSpace()<amount){pause(MachineStatus.Industry.OUTPUT_FULL);return;}
            if(input.getFluidAmount()<amount){pause(MachineStatus.Industry.NEEDS_INPUT);return;}
        }
        status=MachineStatus.Industry.WORKING;working(true);progress+=IndustrialRates.STEP_TICKS;setChanged();
        if(progress<duration())return;
        progress=0;
        if(kind==Kind.DRILL){
            if(!work.extract(server,site,worldPosition,target))return;
            input.drain(IndustrialRates.DRILL_FUEL_MB,EXECUTE);if(lubricant.getFluidAmount()>=10){lubricant.drain(10,EXECUTE);lubrication=1000;}else lubrication=Math.max(200,lubrication-10);MachineInventory.insert(this,MachineInventory.INDUSTRIAL_OUTPUT,KilnContent.MINERAL_COAL.toStack(amount));
        }else{
            // Checked hot this step, so the draw always succeeds.
            fire.draw(amount*(double)ProductionEnergy.COAL/ProductionEnergy.OIL_MB_PER_COAL);
            input.drain(amount,EXECUTE);
            output.fill(new FluidStack(IndustrialContent.HEATED.get(),amount),EXECUTE);
        }
        EnergyLog.machine(level,worldPosition,"industry_"+kind.name().toLowerCase(java.util.Locale.ROOT)+"_batch",kind==Kind.DRILL?"civilization:refined_fuel_mb":"civilization:crude_oil_mb",kind==Kind.DRILL?IndustrialRates.DRILL_FUEL_MB:250,kind==Kind.DRILL?"civilization:mineral_coal":"civilization:heated_crude_mb",amount);
        setChanged();
    }
    /** The derrick drills its well a block at a time, lining each new four blocks with an Iron Plate of casing, until the
     *  bit reaches the oil body; then it pumps the body's physical oil. Its engine's work is all it spends. */
    private void derrick(ServerLevel server){
        engine=null;
        if(site==null||site.kind()!=Deposits.Kind.OIL){pause(MachineStatus.Industry.WRONG_SITE);return;}
        DepositWork.Target target=null;
        if(!struck){
            var bit=well().below(depth+1);
            if(!Chunks.loaded(server,bit)){pause(MachineStatus.Industry.UNLOADED);return;}
            var state=server.getBlockState(bit);
            if(site.body(bit)||state.is(IndustrialContent.RESERVOIR_OIL.get())){struck=true;effort=0;setChanged();}
            else{
                if(server.isOutsideBuildHeight(bit)||state.getDestroySpeed(server,bit)<0&&!state.is(IndustrialContent.OIL_SEEP.get())){pause(MachineStatus.Industry.BLOCKED);return;}
                if(!DepositWork.allowed(server,worldPosition,bit)){pause(MachineStatus.Industry.CLAIM_BLOCKED);return;}
                if(cased<=depth){
                    if(!items.get(0).is(supply(kind))){pause(MachineStatus.Industry.NEEDS_CASING);return;}
                    items.get(0).shrink(1);cased+=IndustrialRates.CASING_BLOCKS;setChanged();
                }
            }
        }
        if(struck){
            target=work.find(server,site,worldPosition);if(target.status()!=MachineStatus.Industry.WORKING){pause(target.status());return;}
            if(output.getSpace()<target.units()){pause(MachineStatus.Industry.OUTPUT_FULL);return;}
        }
        engine=Engine.near(server,worldPosition);
        if(engine==null){pause(MachineStatus.Industry.NO_ENGINE);return;}
        status=powered?MachineStatus.Industry.WORKING:MachineStatus.Industry.NEEDS_POWER;working(powered);powered=false;
        if(effort<duration())return;
        effort=0;setChanged();
        if(!struck){depth++;return;}
        if(!work.extract(server,site,worldPosition,target))return;
        output.fill(new FluidStack(IndustrialContent.CRUDE.get(),target.units()),EXECUTE);
        EnergyLog.machine(level,worldPosition,"industry_derrick_batch","civilization:oil_reserve_mb",target.units(),"civilization:crude_oil_mb",target.units());
    }
    /** Each tick the derrick takes its engine's work, never more than its current block or step still needs. */
    private void drive(){
        double need=duration()-effort;
        if(need<=0||!Chunks.loaded(level,engine))return;
        var source=Engine.of(level.getBlockEntity(engine));if(source==null)return;
        double work=source.requestWork(worldPosition,Math.min(1,need));
        if(work>0){effort+=work;powered=true;setChanged();}
    }
    /** The Coal Engine's work: up to half power, drawn from its firebox's stored heat while that is at working heat. */
    @Override public double requestWork(BlockPos consumer,double demand){
        if(!(level instanceof ServerLevel)||!formed||!(demand>0)||workedTick==level.getGameTime())return 0;
        if(!Chunks.loaded(level,consumer)||consumer.distSqr(worldPosition)>RANGE*RANGE||!CivicAccess.boundary(level,worldPosition,consumer))return 0;
        double power=Math.min(demand,IndustrialRates.COAL_ENGINE_POWER);
        if(!fire.draw(power*IndustrialRates.COAL_WORK_UNITS))return 0;
        workedTick=level.getGameTime();return power;
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
        public int get(int i){return switch(i){case 0->kind.ordinal();case 1->input.getFluidAmount();case 2->output.getFluidAmount();case 3->IndustrialContent.fluidId(input.getFluid());case 4->kind==Kind.DERRICK?(int)effort:progress;case 5->status;case 6->fire.heat();case 7->lubricant.getFluidAmount();case 8->kind==Kind.DRILL&&lubricant.getFluidAmount()>=10?1000:lubrication;case 9->duration();case 10->IndustrialContent.fluidId(output.getFluid());case 11->coalPowered()?fire.state():0;case 12->model()!=null&&build.sections==model().all?1:0;case 13->depth;case 14->cased;case 15->struck?1:0;default->0;};}
        public void set(int i,int v){}public int getCount(){return 16;}
    };
    @Override public void onLoad(){
        super.onLoad();
        if(level!=null&&!level.isClientSide&&supply(kind)!=null){
            for(int slot:new int[]{3,4})if(!items.get(slot).isEmpty()){
                net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,items.get(slot));
                items.set(slot,ItemStack.EMPTY);setChanged();
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(model()!=null)build.save(t,r);fire.save(t);ContainerHelper.saveAllItems(t,items,r);t.put("input",input.writeToNBT(r,new CompoundTag()));t.put("output",output.writeToNBT(r,new CompoundTag()));t.put("lubricant",lubricant.writeToNBT(r,new CompoundTag()));t.putInt("lubrication",lubrication);t.putInt("cursor",work.cursor);t.putInt("progress",progress);t.putInt("depth",depth);t.putInt("cased",cased);t.putBoolean("struck",struck);t.putDouble("effort",effort);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);if(model()!=null){int before=build.sections;build.load(t,r,model());if(level!=null&&level.isClientSide&&build.sections!=before)ModeledMachine.redraw(this);}fire.load(t);ContainerHelper.loadAllItems(t,items,r);input.readFromNBT(r,t.getCompound("input"));output.readFromNBT(r,t.getCompound("output"));lubricant.readFromNBT(r,t.getCompound("lubricant"));lubrication=t.contains("lubrication")?Math.clamp(t.getInt("lubrication"),200,1000):1000;work.cursor=Math.max(0,t.getInt("cursor"));depth=Math.max(0,t.getInt("depth"));cased=Math.max(0,t.getInt("cased"));struck=t.getBoolean("struck");effort=Math.max(0,t.getDouble("effort"));progress=Math.clamp(t.getInt("progress"),0,duration()-1);formed=t.getBoolean("formed");surveyed=false;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=saveWithoutMetadata(r);t.putBoolean("formed",formed);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new IndustrialMenu(id,inv,this,data);}
    @Override public int[] getSlotsForFace(Direction side){return (kind==Kind.DRILL||kind==Kind.COLUMN)&&side==Direction.DOWN?MachineInventory.INDUSTRIAL_OUTPUT:supply(kind)!=null&&side!=Direction.DOWN?MachineInventory.INDUSTRIAL_INPUT:new int[0];}
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack stack,Direction side){return side!=Direction.DOWN&&canPlaceItem(i,stack);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack stack,Direction side){return (kind==Kind.DRILL||kind==Kind.COLUMN)&&MachineInventory.contains(MachineInventory.INDUSTRIAL_OUTPUT,i)&&side==Direction.DOWN;}
    @Override public int getContainerSize(){return 8;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int count){var s=ContainerHelper.removeItem(items,i,count);if(!s.isEmpty())setChanged();return s;}
    @Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack stack){items.set(i,stack);stack.limitSize(stack.getMaxStackSize());setChanged();}
    @Override public boolean canPlaceItem(int i,ItemStack s){return MachineInventory.contains(MachineInventory.INDUSTRIAL_INPUT,i)&&supply(kind)!=null&&s.is(supply(kind));}
    @Override public boolean stillValid(Player p){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64&&CivicAccess.allowed(level,worldPosition,p);}
    @Override public void clearContent(){items.clear();setChanged();}
}
