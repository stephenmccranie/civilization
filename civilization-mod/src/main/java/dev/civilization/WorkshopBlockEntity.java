package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Eight physical slots; work and fuel commit on the server, with no reserved/offline inventories. */
public final class WorkshopBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider,CoalFireHost {
    private final NonNullList<ItemStack> items=NonNullList.withSize(8,ItemStack.EMPTY);
    public final int kind;
    public final CoalFire fire=new CoalFire(this);
    public int selected=-1,heat,progress,status;
    public int fireHeat(){return heat;}
    public void fireHeat(int value){heat=value;}
    public int coalCapacity(){return ProductionEnergy.HEAT_TICKS;}
    public int coalBudget(){return (int)Math.round(coalCapacity()*ThermalField.efficiency(level,worldPosition));}
    public int[] coalFuelSlots(){return MachineInventory.WORKSHOP_FUEL;}
    public int pendingExperience;
    public void awardExperience(net.minecraft.world.phys.Vec3 at){
        if(level instanceof net.minecraft.server.level.ServerLevel server && pendingExperience>0){
            int earned=pendingExperience;pendingExperience=0;setChanged();
            net.minecraft.world.entity.ExperienceOrb.award(server,at,earned);
        }
    }
    private boolean firePaused;
    private ItemStack workingResult=ItemStack.EMPTY;
    public WorkshopBlockEntity(BlockPos p,BlockState s){super(WorkshopContent.ENTITY.get(),p,s);kind=((WorkshopBlock)s.getBlock()).kind;}
    public WorkshopJobs.Job job(){return MachineWork.job(this,kind,selected);}
    public void select(int value){if(value< -1||value>=WorkshopJobs.jobs(kind).size()||value==selected)return;selected=value;progress=0;workingResult=ItemStack.EMPTY;setChanged();}
    public void tick(){if(level!=null&&!level.isClientSide&&Math.floorMod(level.getGameTime()+worldPosition.asLong(),10)==0)process();}
    public void process(){
        if(level==null||level.isClientSide)return;
        firePaused=false;processWork();if(!firePaused)fire.finish(10,status==MachineStatus.Workshop.WORKING);working(status==MachineStatus.Workshop.WORKING);
    }
    private void processWork(){
        if(level==null||level.isClientSide)return;
        var structure=MachineStructure.check(level,worldPosition,getBlockState().getValue(WorkshopBlock.FACING));
        if(structure.status()!=MachineStructure.COMPLETE){firePaused=structure.status()==MachineStructure.UNLOADED;if(structure.status()==MachineStructure.INCOMPLETE)fire.extinguish();pause(MachineStatus.Workshop.INCOMPLETE,false);return;}
        if(MachineWeather.wetWorkFace(level,worldPosition,getBlockState())){pause(MachineStatus.Workshop.WET,false);return;}
        if(!fire.lit()){pause(MachineStatus.Workshop.FIRE_UNLIT,false);return;}
        // Give inserted legacy/ungraded gear a stable identity before quoting repairs or upgrades.
        for(int i=0;i<4;i++){
            var input=items.get(i);
            boolean missing=EquipmentGrade.eligible(input)&&(!EquipmentGrade.hasGrade(input)||!input.has(EquipmentGrade.WEAR_SEED.get()));
            EquipmentGrade.ensure(input,level.random,level.registryAccess());
            if(missing)setChanged();
        }
        var job=job();if(job==null){pause(selected<0&&MachineWork.matches(this,kind).size()>1?MachineStatus.Workshop.AMBIGUOUS:MachineStatus.Workshop.NEEDS_MATERIALS,true);return;}
        var match=MachineWork.match(this,job);if(match==null){pause(MachineStatus.Workshop.NEEDS_MATERIALS,true);return;}
        var result=WorkshopJobs.result(job,match.first());
        if(!ItemStack.matches(result,workingResult)){progress=0;workingResult=result.copy();}
        if(!MachineInventory.room(this,outputSlots(),result)){pause(MachineStatus.Workshop.OUTPUT_FULL,false);return;}
        MachineInventory.refill(this,MachineInventory.WORKSHOP_FUEL);
        // Saved work may already exceed a recipe whose duration was shortened in an update.
        if(progress<job.ticks()){
            if(!fire.supply(1)){pause(MachineStatus.Workshop.FIRE_UNLIT,false);return;}
            int work=Math.min(10,job.ticks()-progress);
            while(work>0&&job.heatSpent(progress+work)-job.heatSpent(progress)>heat)work--;
            if(work==0){pause(MachineStatus.Workshop.FIRE_UNLIT,false);return;}
            int cost=job.heatSpent(progress+work)-job.heatSpent(progress);
            heat-=cost;fire.spent(cost);progress+=work;
        }
        status=MachineStatus.Workshop.WORKING;working(true);setChanged();
        if(progress<job.ticks())return;
        EquipmentGrade.ensure(result, level.random, level.registryAccess());
        for(int i=0;i<4;i++)items.get(i).shrink(match.consumed()[i]);
        MachineInventory.insert(this,outputSlots(),result);
        if(!job.name().equals("Repair"))pendingExperience=(int)Math.min(Integer.MAX_VALUE,(long)pendingExperience+1);
        progress=0;workingResult=ItemStack.EMPTY;
        EnergyLog.machine(level,worldPosition,"workshop_batch",job.name(),1,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString(),result.getCount());
        level.playSound(null,worldPosition,kind==2?net.minecraft.sounds.SoundEvents.ANVIL_USE:net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.25f,1f);
    }
    private void pause(int state,boolean reset){status=state;if(reset&&progress!=0){progress=0;workingResult=ItemStack.EMPTY;setChanged();}working(false);}
    private void working(boolean value){if(getBlockState().getValue(MachineFeedback.WORKING)!=value||getBlockState().getValue(WorkshopBlock.LIT)!=fire.lit())level.setBlock(worldPosition,getBlockState().setValue(MachineFeedback.WORKING,value).setValue(WorkshopBlock.LIT,fire.lit()),3);}
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->kind;case 1->selected;case 2->heat;case 3->progress;case 4->status;case 5->fire.state();case 6->fire.remaining();default->0;};}
        public void set(int i,int v){}public int getCount(){return 7;}
    };
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putInt("productionXp",pendingExperience);fire.save(t);ContainerHelper.saveAllItems(t,items,r);t.putInt("job",selected);if(selected>=0)t.putString("jobId",WorkshopJobs.jobs(kind).get(selected).id());t.putInt("heat",heat);t.putInt("progress",progress);if(!workingResult.isEmpty())t.put("workResult",workingResult.save(r));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);pendingExperience=Math.max(0,t.getInt("productionXp"));fire.load(t);items.clear();ContainerHelper.loadAllItems(t,items,r);selected=t.contains("jobId")?WorkshopJobs.index(kind,t.getString("jobId")):t.contains("job")?t.getInt("job"):-1;if(selected< -1||selected>=WorkshopJobs.jobs(kind).size())selected=-1;heat=Math.clamp(t.getInt("heat"),0,1600);progress=Math.clamp(t.getInt("progress"),0,1199);workingResult=ItemStack.parseOptional(r,t.getCompound("workResult"));if(t.contains("jobId")&&selected<0){progress=0;workingResult=ItemStack.EMPTY;}}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new WorkshopMenu(id,inv,this,data);}
    @Override public int getContainerSize(){return 8;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int n){var s=ContainerHelper.removeItem(items,i,n);if(!s.isEmpty())changedInput(i);return s;}
    @Override public ItemStack removeItemNoUpdate(int i){var s=ContainerHelper.takeItem(items,i);changedInput(i);return s;}
    @Override public void setItem(int i,ItemStack s){
        if(i<4&&level!=null&&!level.isClientSide)EquipmentGrade.ensure(s,level.random,level.registryAccess());
        items.set(i,s);s.limitSize(s.getMaxStackSize());changedInput(i);
    }
    private void changedInput(int i){if(i<4){progress=0;workingResult=ItemStack.EMPTY;}setChanged();}
    private int[] outputSlots(){return kind==2?MachineInventory.SMITHY_OUTPUT:MachineInventory.WORKSHOP_OUTPUT;}
    @Override public void clearContent(){items.clear();progress=0;workingResult=ItemStack.EMPTY;setChanged();}
    @Override public boolean stillValid(Player p){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.level()==level&&p.distanceToSqr(worldPosition.getCenter())<=64&&CivicAccess.allowed(level,worldPosition,p);}
    @Override public boolean canPlaceItem(int i,ItemStack s){if(i==4||i==6)return s.is(KilnContent.MINERAL_COAL.get());return i>=0&&i<4&&(kind==2||i<2)&&MachineWork.accepts(this,kind,selected,s);}
    @Override public int[] getSlotsForFace(Direction side){
        if(side==Direction.DOWN)return outputSlots();
        if(side==Direction.UP)return kind==2?new int[]{0,1,2,3}:new int[]{0,1};
        return MachineInventory.WORKSHOP_FUEL;
    }
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction side){return side!=Direction.DOWN&&canPlaceItem(i,s);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction side){return side==Direction.DOWN&&MachineInventory.contains(outputSlots(),i);}
}
