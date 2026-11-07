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

/**
 * Eight menu slots, plus the Tannery's pit, which holds a steeping batch's hides and bark as real stock; work and fuel
 * commit on the server, with no reserved/offline inventories.
 */
public final class WorkshopBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider,CoalFireHost,ModeledController {
    public static final int PIT_HIDES=8,PIT_BARK=9;
    private final NonNullList<ItemStack> items=NonNullList.withSize(10,ItemStack.EMPTY);
    public final int kind;
    public final CoalFire fire=new CoalFire(this);
    public int selected=-1,progress,status,steep;
    public int[] coalFuelSlots(){return MachineInventory.WORKSHOP_FUEL;}
    /** The Smithy's footing and brickwork store its fire's heat. */
    public int masonry(){return kind==2?24:0;}
    /** The Smithy burns Coal, the Tannery steeps, and the loom and Assembly Bench are worked by hand. */
    public static boolean fired(int kind){return kind==2;}
    public static boolean handWorked(int kind){return kind==1||kind==ManufacturingJobs.BENCH;}
    public boolean steeped(){return kind==0;}
    public int pendingExperience;
    public void awardExperience(net.minecraft.world.phys.Vec3 at){
        if(level instanceof net.minecraft.server.level.ServerLevel server && pendingExperience>0){
            int earned=pendingExperience;pendingExperience=0;setChanged();
            net.minecraft.world.entity.ExperienceOrb.award(server,at,earned);
        }
    }
    private boolean firePaused;
    public static final int WORK_BUTTON=30000;
    private Player worker;
    public boolean beginWork(Player player){
        if(!handWorked(kind)||!stillValid(player)||player.isSpectator()||worker!=null&&worker!=player)return false;
        if(structure()!=MachineStructure.COMPLETE)return false;
        if(!(player.containerMenu instanceof WorkshopMenu menu)||menu.inventory!=this)return false;
        var quote=MachineWork.match(this,job());
        if(quote==null||!MachineInventory.room(this,outputSlots(kind),WorkshopJobs.result(this,quote)))return false;
        worker=player;setChanged();return true;
    }
    private ItemStack workingResult=ItemStack.EMPTY;
    public WorkshopBlockEntity(BlockPos p,BlockState s){super(WorkshopContent.ENTITY.get(),p,s);kind=((WorkshopBlock)s.getBlock()).kind;}
    private final ModeledBuild build=new ModeledBuild();
    @Override public MachineModel model(){return ((WorkshopBlock)getBlockState().getBlock()).model();}
    @Override public ModeledBuild build(){return build;}
    @Override public Direction front(){return getBlockState().getValue(WorkshopBlock.FACING);}
    @Override public boolean working(){return getBlockState().getValue(MachineFeedback.WORKING);}
    @Override public void sectionsChanged(){status=MachineStatus.Workshop.INCOMPLETE;}
    @Override public int afloat(){return steeped()?items.get(PIT_HIDES).getCount():0;}
    /** Workshops built as native models check their sections; the Assembly Bench checks its two halves. */
    int structure(){
        if(model()==null)return AssemblyBenchBlock.complete(level,worldPosition,getBlockState())?MachineStructure.COMPLETE:MachineStructure.INCOMPLETE;
        ModeledMachine.reconcile(this);return ModeledMachine.status(this);
    }
    public WorkshopJobs.Job job(){return MachineWork.job(this,kind,selected);}
    public void select(int value){if(value< -1||value>=WorkshopJobs.outputs(kind).size()||value==selected)return;selected=value;progress=0;worker=null;workingResult=ItemStack.EMPTY;setChanged();}
    public void tick(){if(level!=null&&!level.isClientSide&&Math.floorMod(level.getGameTime()+worldPosition.asLong(),10)==0)process();}
    public void process(){
        if(level==null||level.isClientSide)return;
        if(handWorked(kind)){processHandWork();working(status==MachineStatus.Workshop.WORKING);return;}
        if(steeped()){processSteep();working(status==MachineStatus.Workshop.WORKING);return;}
        firePaused=false;processWork();if(!firePaused)fire.tick(10);working(status==MachineStatus.Workshop.WORKING);
    }
    private void processHandWork(){
        if(worker==null||!worker.isAlive()||worker.isSpectator()||!stillValid(worker)||!(worker.containerMenu instanceof WorkshopMenu menu)||menu.inventory!=this){worker=null;pause(MachineStatus.Workshop.HAND_READY,false);return;}
        if(structure()!=MachineStructure.COMPLETE){worker=null;pause(MachineStatus.Workshop.INCOMPLETE,false);return;}
        if(settleInputs()){worker=null;pause(MachineStatus.Workshop.HOT_METAL,true);return;}
        var job=job();var match=MachineWork.match(this,job);
        if(match==null){worker=null;pause(MachineStatus.Workshop.NEEDS_MATERIALS,true);return;}
        var result=WorkshopJobs.result(this,match);
        if(!ItemStack.matches(result,workingResult)){progress=0;workingResult=result.copy();}
        if(!MachineInventory.room(this,outputSlots(kind),result)){worker=null;pause(MachineStatus.Workshop.OUTPUT_FULL,false);return;}
        double labor=2.5*ThermalRules.calorieFactor(ThermalSystem.comfort(worker));
        var food=CalorieFoodData.of(worker);
        if(CalorieFoodData.active(worker)&&(food.isDepleted()||!food.reserve().canAfford(labor))){worker=null;pause(MachineStatus.Workshop.NEEDS_FOOD,false);return;}
        // Charge only performed work. No fuel, stored power, offline catch-up or automatic repeat.
        food.spendOther(worker,labor,"hand_work",job.id());
        progress=Math.min(job.ticks(),progress+10);status=MachineStatus.Workshop.WORKING;setChanged();
        if(progress<job.ticks())return;
        EquipmentGrade.ensure(result,level.random,level.registryAccess());
        for(int i=0;i<4;i++)items.get(i).shrink(match.consumed()[i]);
        MachineInventory.insert(this,outputSlots(kind),result);
        EnergyLog.machine(level,worldPosition,"hand_batch",job.name(),1,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString(),result.getCount());
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.35f,1f);
        worker=null;progress=0;workingResult=ItemStack.EMPTY;status=MachineStatus.Workshop.HAND_READY;setChanged();
    }
    /** Pouring a water bucket into the Tannery's empty pit starts a batch: up to eight hides, one Tanbark each. A pit that
     * is busy, unfinished or unloaded takes nothing; its menu shows why. */
    public boolean pour(Player p,InteractionHand hand){
        if(!steeped()||!stillValid(p)||!items.get(PIT_HIDES).isEmpty()||structure()!=MachineStructure.COMPLETE)return false;
        int batch=Math.min(Tanning.PIT,Math.min(count(WorkshopContent.HIDE.get()),count(WorkshopContent.TANBARK.get())));
        if(batch==0)return false;
        items.set(PIT_HIDES,take(WorkshopContent.HIDE.get(),batch));items.set(PIT_BARK,take(WorkshopContent.TANBARK.get(),batch));steep=0;
        p.setItemInHand(hand,ItemUtils.createFilledResult(p.getItemInHand(hand),p,new ItemStack(Items.BUCKET)));
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,net.minecraft.sounds.SoundSource.BLOCKS,1f,1f);
        status=MachineStatus.Workshop.WORKING;working(true);setChanged();return true;
    }
    public int minutesLeft(){return (int)Math.ceil((Tanning.TICKS-steep)/1200.0);}
    private int count(Item item){int n=0;for(int i=0;i<2;i++)if(items.get(i).is(item))n+=items.get(i).getCount();return n;}
    private ItemStack take(Item item,int count){
        var taken=ItemStack.EMPTY;
        for(int i=0;i<2&&count>0;i++){var s=items.get(i);if(!s.is(item))continue;int n=Math.min(count,s.getCount());if(taken.isEmpty())taken=s.copyWithCount(n);else taken.grow(n);s.shrink(n);count-=n;}
        return taken;
    }
    /** The pit needs no fire or worker: a complete Tannery steeps its batch while loaded, then empties into Leather. */
    private void processSteep(){
        if(structure()!=MachineStructure.COMPLETE){pause(MachineStatus.Workshop.INCOMPLETE,false);return;}
        var pit=items.get(PIT_HIDES);
        if(pit.isEmpty()){pause(count(WorkshopContent.HIDE.get())>0&&count(WorkshopContent.TANBARK.get())>0?MachineStatus.Workshop.POUR:MachineStatus.Workshop.NEEDS_MATERIALS,false);return;}
        if(steep<Tanning.TICKS){steep=Math.min(Tanning.TICKS,steep+10);setChanged();}
        if(steep<Tanning.TICKS){status=MachineStatus.Workshop.WORKING;working(true);return;}
        var leather=new ItemStack(Items.LEATHER,pit.getCount());
        if(!MachineInventory.room(this,outputSlots(kind),leather)){pause(MachineStatus.Workshop.OUTPUT_FULL,false);return;}
        MachineInventory.insert(this,outputSlots(kind),leather);
        pendingExperience=(int)Math.min(Integer.MAX_VALUE,(long)pendingExperience+pit.getCount());
        EnergyLog.machine(level,worldPosition,"workshop_batch","Tanning",1,"minecraft:leather",pit.getCount());
        items.set(PIT_HIDES,ItemStack.EMPTY);items.set(PIT_BARK,ItemStack.EMPTY);steep=0;
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.BUCKET_FILL,net.minecraft.sounds.SoundSource.BLOCKS,.4f,.8f);
        pause(MachineStatus.Workshop.NEEDS_MATERIALS,false);setChanged();
    }
    private void processWork(){
        if(level==null||level.isClientSide)return;
        int structure=structure();
        if(structure!=MachineStructure.COMPLETE){firePaused=structure==MachineStructure.UNLOADED;if(structure==MachineStructure.INCOMPLETE)fire.extinguish();pause(MachineStatus.Workshop.INCOMPLETE,false);return;}
        if(MachineWeather.wetWorkFace(level,worldPosition,getBlockState())){pause(MachineStatus.Workshop.WET,false);return;}
        if(!fire.hot()){pause(fire.lit()?MachineStatus.Workshop.WARMING:MachineStatus.Workshop.FIRE_UNLIT,false);return;}
        if(settleInputs()){pause(MachineStatus.Workshop.HOT_METAL,true);return;}
        var job=job();if(job==null){pause(MachineWork.matches(this,kind,selected).size()>1?MachineStatus.Workshop.AMBIGUOUS:MachineStatus.Workshop.NEEDS_MATERIALS,true);return;}
        var match=MachineWork.match(this,job);if(match==null){pause(MachineStatus.Workshop.NEEDS_MATERIALS,true);return;}
        var result=WorkshopJobs.result(this,match);
        if(!ItemStack.matches(result,workingResult)){progress=0;workingResult=result.copy();}
        if(!MachineInventory.room(this,outputSlots(kind),result)){pause(MachineStatus.Workshop.OUTPUT_FULL,false);return;}
        MachineInventory.refill(this,MachineInventory.WORKSHOP_FUEL);
        // Saved work may already exceed a recipe whose duration was shortened in an update.
        if(progress<job.ticks()){
            int work=Math.min(10,job.ticks()-progress);
            fire.draw(work);
            progress+=work;
        }
        status=MachineStatus.Workshop.WORKING;working(true);setChanged();
        if(progress<job.ticks())return;
        EquipmentGrade.ensure(result, level.random, level.registryAccess());
        for(int i=0;i<4;i++)items.get(i).shrink(match.consumed()[i]);
        MachineInventory.insert(this,outputSlots(kind),result);
        if(!job.name().equals("Repair"))pendingExperience=(int)Math.min(Integer.MAX_VALUE,(long)pendingExperience+1);
        progress=0;workingResult=ItemStack.EMPTY;
        EnergyLog.machine(level,worldPosition,"workshop_batch",job.name(),1,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString(),result.getCount());
        level.playSound(null,worldPosition,kind==2?net.minecraft.sounds.SoundEvents.ANVIL_USE:net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.25f,1f);
    }
    /**
     * Inputs settle before any quotation: legacy or ungraded equipment acquires one stable identity, and metal that has
     * cooled in air takes its loss. Hot metal is not yet material; returns whether any input is still hot.
     */
    private boolean settleInputs(){
        boolean hot=false;
        for(int i=0;i<4;i++){
            var input=items.get(i);
            boolean missing=EquipmentGrade.eligible(input)&&(!EquipmentGrade.hasGrade(input)||!input.has(EquipmentGrade.WEAR_SEED.get()));
            EquipmentGrade.ensure(input,level.random,level.registryAccess());
            if(missing)setChanged();
            HotMetal.cool(input,level.getGameTime());
            hot|=input.has(HotMetal.HOT.get());
        }
        return hot;
    }
    private void pause(int state,boolean reset){status=state;if(reset&&progress!=0){progress=0;workingResult=ItemStack.EMPTY;setChanged();}working(false);}
    /** What the model's lit bones show: the fire burning, or the Tannery's pit holding a batch. */
    private boolean lit(){return steeped()?!items.get(PIT_HIDES).isEmpty():fire.lit();}
    private void working(boolean value){if(getBlockState().getValue(MachineFeedback.WORKING)!=value||getBlockState().getValue(WorkshopBlock.LIT)!=lit())level.setBlock(worldPosition,getBlockState().setValue(MachineFeedback.WORKING,value).setValue(WorkshopBlock.LIT,lit()),3);}
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->kind;case 1->selected;case 2->steeped()?steep:progress;case 3->status;case 4->fire.state();case 5->fire.heat();case 6->items.get(PIT_HIDES).getCount();default->0;};}
        public void set(int i,int v){}public int getCount(){return 7;}
    };
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);if(model()!=null)build.save(t,r);t.putInt("productionXp",pendingExperience);fire.save(t);ContainerHelper.saveAllItems(t,items,r);if(selected>=0)t.putString("jobId",WorkshopJobs.outputs(kind).get(selected));t.putInt("progress",progress);t.putInt("steep",steep);if(!workingResult.isEmpty())t.put("workResult",workingResult.save(r));}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);if(model()!=null){int before=build.sections;build.load(t,r,model());if(level!=null&&level.isClientSide&&build.sections!=before)ModeledMachine.redraw(this);}pendingExperience=Math.max(0,t.getInt("productionXp"));fire.load(t);items.clear();ContainerHelper.loadAllItems(t,items,r);selected=WorkshopJobs.index(kind,t.getString("jobId"));progress=Math.clamp(t.getInt("progress"),0,20000);steep=Math.clamp(t.getInt("steep"),0,Tanning.TICKS);worker=null;workingResult=ItemStack.parseOptional(r,t.getCompound("workResult"));if(t.contains("jobId")&&selected<0){progress=0;workingResult=ItemStack.EMPTY;}}
    /** Clients draw a modeled machine from its built sections and a Tannery's pit from its hides, so they receive those
     * and nothing else. The pit fills and empties as the controller's lit state changes, which resends this. */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){
        var t=new CompoundTag();if(model()!=null)build.save(t,r);
        var pit=NonNullList.withSize(items.size(),ItemStack.EMPTY);pit.set(PIT_HIDES,items.get(PIT_HIDES));ContainerHelper.saveAllItems(t,pit,r);
        return t;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new WorkshopMenu(id,inv,this,data);}
    @Override public int getContainerSize(){return 10;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int n){var s=ContainerHelper.removeItem(items,i,n);if(!s.isEmpty())changedInput(i);return s;}
    @Override public ItemStack removeItemNoUpdate(int i){var s=ContainerHelper.takeItem(items,i);changedInput(i);return s;}
    @Override public void setItem(int i,ItemStack s){
        if(i<4&&level!=null&&!level.isClientSide)EquipmentGrade.ensure(s,level.random,level.registryAccess());
        items.set(i,s);s.limitSize(s.getMaxStackSize());changedInput(i);
    }
    private void changedInput(int i){if(i<4){progress=0;workingResult=ItemStack.EMPTY;}setChanged();}
    public static int[] outputSlots(int kind){return kind>=2?MachineInventory.SMITHY_OUTPUT:MachineInventory.WORKSHOP_OUTPUT;}
    @Override public void clearContent(){items.clear();worker=null;progress=0;steep=0;workingResult=ItemStack.EMPTY;setChanged();}
    @Override public boolean stillValid(Player p){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.level()==level&&p.distanceToSqr(worldPosition.getCenter())<=64&&CivicAccess.allowed(level,worldPosition,p)&&(!(getBlockState().getBlock() instanceof AssemblyBenchBlock)||AssemblyBenchBlock.allowed(level,worldPosition,getBlockState(),p));}
    @Override public boolean canPlaceItem(int i,ItemStack s){if(i==4||i==6)return fired(kind)&&s.is(KilnContent.MINERAL_COAL.get());return i>=0&&i<4&&(kind>=2||i<2)&&MachineWork.accepts(this,kind,selected,s);}
    @Override public int[] getSlotsForFace(Direction side){
        if(side==Direction.DOWN)return outputSlots(kind);
        if(side==Direction.UP)return kind>=2?new int[]{0,1,2,3}:new int[]{0,1};
        return fired(kind)?MachineInventory.WORKSHOP_FUEL:new int[0];
    }
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction side){return side!=Direction.DOWN&&canPlaceItem(i,s);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction side){return side==Direction.DOWN&&MachineInventory.contains(outputSlots(kind),i);}
}
