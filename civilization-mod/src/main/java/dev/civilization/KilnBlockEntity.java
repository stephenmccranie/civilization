package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Reuses vanilla persistence, sided inventory, output limits and recipe completion. */
public class KilnBlockEntity extends AbstractFurnaceBlockEntity implements CoalFireHost, ModeledController {
    private net.minecraft.resources.ResourceLocation selectedRecipe;
    public java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> recipes() {
        return level.getRecipeManager().getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType<net.minecraft.world.item.crafting.AbstractCookingRecipe>)processType)
                .stream().sorted(java.util.Comparator.comparing(r -> r.id().toString())).toList();
    }
    public int selection() {
        var recipes=recipes();
        for(int i=0;i<recipes.size();i++)if(recipes.get(i).id().equals(selectedRecipe))return i;
        return -1;
    }
    public void selectRecipe(int index) {
        var recipes=recipes();
        if(index < -1 || index >= recipes.size())return;
        var next=index<0?null:recipes.get(index).id();
        if(java.util.Objects.equals(next,selectedRecipe))return;
        selectedRecipe=next;
        dataAccess.set(2,0);
        dataAccess.set(3,processingRecipe(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)))
                .map(r->r.value().getCookingTime()).orElse(200));
        setChanged();
    }
    public java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> processingRecipe(net.minecraft.world.item.crafting.SingleRecipeInput input) {
        return recipes().stream().filter(r -> selectedRecipe==null || r.id().equals(selectedRecipe))
                .filter(r -> r.value().matches(input,level)).findFirst();
    }
    public final CoalFire fire=new CoalFire(this);
    public int fireHeat(){return dataAccess.get(0);}
    public void fireHeat(int heat){dataAccess.set(0,heat);dataAccess.set(1,Math.max(FUEL_TICKS,heat));}
    public int coalCapacity(){return ProductionEnergy.HEAT_TICKS;}
    public int coalBudget(){return (int)Math.round(coalCapacity()*ThermalField.efficiency(level,worldPosition));}
    public int[] coalFuelSlots(){return MachineInventory.KILN_FUEL;}
    public static final int FUEL_TICKS = ProductionEnergy.HEAT_TICKS;
    private MachineStructure.Result structure = new MachineStructure.Result(MachineStructure.INCOMPLETE, null, "");
    public int structureStatus() { return structure.status(); }
    public boolean requiresStructure() { return true; }
    public MachineStructure.Result checkStructure() {
        if (model() != null) { ModeledMachine.reconcile(this); structure = new MachineStructure.Result(ModeledMachine.status(this), null, ""); }
        else structure = requiresStructure()
                ? MachineStructure.check(level, worldPosition, front())
                : new MachineStructure.Result(MachineStructure.COMPLETE, null, "");
        return structure;
    }
    private final ModeledBuild build = new ModeledBuild();
    @Override public MachineModel model() { return getBlockState().getBlock() instanceof ModeledBlock b ? b.model() : null; }
    @Override public ModeledBuild build() { return build; }
    @Override public net.minecraft.core.Direction front() { return getBlockState().getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING); }
    @Override public boolean working() { return getBlockState().getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT); }
    @Override public void sectionsChanged() { structure = new MachineStructure.Result(MachineStructure.INCOMPLETE, null, ""); }
    /** Clients draw a modeled machine from its built sections, so they receive those and nothing else. */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { var tag = new CompoundTag(); if (model() != null) build.save(tag, lookup); return tag; }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    /** Small, synchronized state vocabulary shared by all three menus. */
    public int operatingStatus() {
        if (requiresStructure() && structureStatus() != MachineStructure.COMPLETE) return 0;
        if (getBlockState().getValue(MachineFeedback.WORKING)) return 1;
        if (MachineWeather.wetWorkFace(level, worldPosition, getBlockState())) return 7;
        if (!getItem(0).isEmpty() && !canPlaceItem(0, getItem(0))) return 5;
        if (getItem(0).isEmpty()) return MachineInventory.count(this,MachineInventory.KILN_OUTPUT)==0 ? 2 : 6;
        var match = processingRecipe(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)));
        if(match.isEmpty())return 5;
        var recipe = match.get().value();
        var result = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)), level.registryAccess());
        if (!MachineInventory.room(this,MachineInventory.KILN_OUTPUT,result)) return 3;
        return dataAccess.get(0) > 0 ? 2 : 4;
    }
    private final net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> processType;
    public KilnBlockEntity(BlockPos pos, BlockState state) { this(KilnContent.ENTITY.get(), pos, state, KilnContent.RECIPE_TYPE.get()); }
    protected KilnBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType, BlockPos pos, BlockState state,
                              net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> processType) {
        super(entityType, pos, state, processType);
        this.processType = processType;
        items=net.minecraft.core.NonNullList.withSize(6,ItemStack.EMPTY);

    }
    @Override public int getContainerSize(){return 6;}
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side){return side==net.minecraft.core.Direction.DOWN?MachineInventory.KILN_OUTPUT:side==net.minecraft.core.Direction.UP?MachineInventory.KILN_INPUT:MachineInventory.KILN_FUEL;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,net.minecraft.core.Direction side){return side==net.minecraft.core.Direction.DOWN&&MachineInventory.contains(MachineInventory.KILN_OUTPUT,slot);}
    protected String auditPrefix() { return "kiln"; }
    public static boolean acceptsFuel(ItemStack stack) { return stack.is(KilnContent.MINERAL_COAL.get()); }
    @Override protected int getBurnDuration(ItemStack stack) { return acceptsFuel(stack) ? (int)Math.round(FUEL_TICKS * ThermalField.efficiency(level, worldPosition)) : 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return MachineInventory.contains(MachineInventory.KILN_FUEL,slot) ? acceptsFuel(stack) : MachineInventory.contains(MachineInventory.KILN_INPUT,slot) && level != null && level.getRecipeManager()
                .getRecipeFor(processType, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level).isPresent();
    }
    @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new KilnMenu(id, inventory, this, dataAccess); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);fire.load(tag);
        if (model() != null) { int before = build.sections; build.load(tag, lookup, model()); if (level != null && level.isClientSide && build.sections != before) ModeledMachine.redraw(this); }
        selectedRecipe=tag.contains("SelectedRecipe")?net.minecraft.resources.ResourceLocation.tryParse(tag.getString("SelectedRecipe")):null;
        legacyOverflow.clear();
        var old=net.minecraft.core.NonNullList.withSize(12,ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag,old,lookup);
        if(!tag.getBoolean("TwoFuelSlots"))items.set(3,ItemStack.EMPTY);
        if(!tag.getBoolean("TwoMaterialSlots")){items.set(4,ItemStack.EMPTY);items.set(5,ItemStack.EMPTY);}
        for(int i=tag.getBoolean("TwoMaterialSlots")?6:tag.getBoolean("TwoFuelSlots")?4:3;i<old.size();i++)if(!old.get(i).isEmpty())legacyOverflow.add(old.get(i));
        if(tag.contains("LegacyOverflow"))for(var entry:tag.getList("LegacyOverflow",10))
            ItemStack.parse(lookup,entry).ifPresent(legacyOverflow::add);
        // Vanilla reconstructs duration from the fuel slot, which may be empty after ignition.
        dataAccess.set(1, dataAccess.get(0) > 0 ? Math.max(FUEL_TICKS, dataAccess.get(0)) : 0);
    }
    private final java.util.List<ItemStack> legacyOverflow=new java.util.ArrayList<>();
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);fire.save(tag);tag.putBoolean("TwoFuelSlots",true);
        if(model()!=null)build.save(tag,lookup);
        tag.putBoolean("TwoMaterialSlots",true);
        if(selectedRecipe!=null)tag.putString("SelectedRecipe",selectedRecipe.toString());
        if(!legacyOverflow.isEmpty()){var list=new net.minecraft.nbt.ListTag();for(var item:legacyOverflow)list.add(item.save(lookup));tag.put("LegacyOverflow",list);}
    }
    @Override public void onLoad(){super.onLoad();returnLegacyOverflow();}
    private void returnLegacyOverflow(){
        if(level==null||level.isClientSide||legacyOverflow.isEmpty())return;
        for(var item:legacyOverflow)net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,item);
        legacyOverflow.clear();setChanged();
    }
    public static void tick(Level level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
        kiln.returnLegacyOverflow();
        MachineInventory.refill(kiln,MachineInventory.KILN_INPUT);
        // Active machines validate every work tick; idle ones stagger checks once per second.
        if (kiln.dataAccess.get(0) > 0 || !kiln.getItem(0).isEmpty() || Math.floorMod(level.getGameTime() + pos.asLong(), 20) == 0)
            kiln.checkStructure();
        if (kiln.structureStatus() != MachineStructure.COMPLETE) {
            if (kiln.structureStatus() == MachineStructure.INCOMPLETE && (kiln.dataAccess.get(0) > 0 || kiln.dataAccess.get(2) > 0)) {
                kiln.fire.extinguish(); kiln.dataAccess.set(2, 0);
                kiln.setChanged();
                EnergyLog.machine(level, pos, kiln.auditPrefix() + "_structure_broken", null, 0, null, 0);
            }
            if (state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT) || state.getValue(MachineFeedback.WORKING))
                level.setBlock(pos, state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, false)
                        .setValue(MachineFeedback.WORKING, false), 3);
            return;
        }
        if(!kiln.fire.lit()){
            if(state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)||state.getValue(MachineFeedback.WORKING))
                level.setBlock(pos,state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT,false).setValue(MachineFeedback.WORKING,false),3);
            return;
        }
        if(MachineWeather.wetWorkFace(level,pos,state)){
            kiln.fire.finish(1,false);
            var current=kiln.getBlockState();
            if(current.getValue(MachineFeedback.WORKING)||current.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)!=kiln.fire.lit())
                level.setBlock(pos,current.setValue(MachineFeedback.WORKING,false)
                        .setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT,kiln.fire.lit()),3);
            return;
        }
        if(!kiln.fire.supply(1)){kiln.fire.finish(1,false);return;}
        // Resume saved heat after a neighboring chunk reloads.
        if (kiln.dataAccess.get(0) > 0 && !state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)) {
            state = state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true);
            level.setBlock(pos, state, 3);
        }
        MachineInventory.refill(kiln,MachineInventory.KILN_FUEL);
        var input=new net.minecraft.world.item.crafting.SingleRecipeInput(kiln.getItem(0));
        var result=kiln.processingRecipe(input).map(recipe->recipe.value().assemble(input,level.registryAccess())).orElse(ItemStack.EMPTY);
        // Vanilla cooks into slot 2 only, one unit of the input at a time. Each batch cooks into an emptied
        // slot 2; the product takes its grade from the input unit it used, then joins the output bank.
        boolean redirect=!result.isEmpty()&&MachineInventory.room(kiln,MachineInventory.KILN_OUTPUT,result);
        var originalOutput=redirect?kiln.items.set(2,ItemStack.EMPTY):ItemStack.EMPTY;
        var inputUnits=kiln.getItem(0).copy();
        var inputItem = kiln.getItem(0).getItem();
        int inputCount = kiln.getItem(0).getCount();
        int outputCount = MachineInventory.count(kiln,MachineInventory.KILN_OUTPUT);
        int fuel = kiln.getItem(1).getCount();
        int progress = kiln.dataAccess.get(2);
        int heat = kiln.dataAccess.get(0);
        // Vanilla decrements before cooking; a sentinel keeps the final paid tick productive.
        kiln.dataAccess.set(0,heat+1);
        try{serverTick(level, pos, state, kiln);}
        finally{if(redirect){var produced=kiln.items.set(2,originalOutput);if(!produced.isEmpty()){Grade.smelted(produced,inputUnits,level.random);MachineInventory.insert(kiln,MachineInventory.KILN_OUTPUT,produced);}}}
        boolean completed = kiln.getItem(0).getCount() < inputCount;
        boolean working = completed || kiln.dataAccess.get(2) > progress;
        // Vanilla burns one tick while idle; restore it and charge the shared low idle rate instead.
        kiln.dataAccess.set(0,Math.max(0,heat-(working?1:0)));
        kiln.fire.spent(working?1:0);
        kiln.fire.finish(1,working);
        var current = kiln.getBlockState();
        if(current.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)!=kiln.fire.lit()){
            level.setBlock(pos,current.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT,kiln.fire.lit()),3);current=kiln.getBlockState();
        }
        if (current.getValue(MachineFeedback.WORKING) != working)
            level.setBlock(pos, current.setValue(MachineFeedback.WORKING, working), 3);
        if (kiln.getItem(1).getCount() < fuel) {
            EnergyLog.machine(level, pos, kiln.auditPrefix() + "_fuel", "civilization:mineral_coal", 1, null, 0);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.2f, 0.9f);
        }
        if (completed) {
            EnergyLog.machine(level, pos, kiln.auditPrefix() + "_batch", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inputItem).toString(),
                    1, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(kiln.getItem(2).getItem()).toString(), MachineInventory.count(kiln,MachineInventory.KILN_OUTPUT) - outputCount);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DECORATED_POT_PLACE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.15f, 1.2f);
        }
        // Mark work in progress dirty too, so a normal chunk save preserves elapsed fuel/work.
        if (heat != kiln.dataAccess.get(0) || progress != kiln.dataAccess.get(2)) kiln.setChanged();
    }
}
