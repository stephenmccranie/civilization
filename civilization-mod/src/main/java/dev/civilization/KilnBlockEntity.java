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
    /** The selected output, or null on Automatic. A selection names an output, never one of its recipes. */
    private net.minecraft.resources.ResourceLocation selectedOutput;
    public java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> recipes() {
        return level.getRecipeManager().getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType<net.minecraft.world.item.crafting.AbstractCookingRecipe>)processType)
                .stream().sorted(java.util.Comparator.comparing(r -> r.id().toString())).toList();
    }
    public static net.minecraft.resources.ResourceLocation output(net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipe, HolderLookup.Provider registries) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.value().getResultItem(registries).getItem());
    }
    /** Each output once, in recipe-ID order: the recipe drawer's rows and the values a selection may take. */
    public static java.util.List<net.minecraft.resources.ResourceLocation> outputs(java.util.List<? extends net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe>> recipes, HolderLookup.Provider registries) {
        return recipes.stream().map(r -> output(r, registries)).distinct().toList();
    }
    /** The recipes a batch may run, in recipe-ID order: the selected output's, or all of them on Automatic. An output no recipe makes any more is Automatic. */
    public static <R extends net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe>> java.util.List<R> candidates(java.util.List<R> recipes, net.minecraft.resources.ResourceLocation output, HolderLookup.Provider registries) {
        var made = recipes.stream().filter(r -> output(r, registries).equals(output)).toList();
        return made.isEmpty() ? recipes : made;
    }
    public java.util.List<net.minecraft.resources.ResourceLocation> outputs() { return outputs(recipes(), level.registryAccess()); }
    public int selection() { return outputs().indexOf(selectedOutput); }
    public void select(int index) {
        var outputs=outputs();
        if(index < -1 || index >= outputs.size())return;
        var next=index<0?null:outputs.get(index);
        if(java.util.Objects.equals(next,selectedOutput))return;
        selectedOutput=next;
        dataAccess.set(2,0);
        dataAccess.set(3,processingRecipe(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)))
                .map(r->r.value().getCookingTime()).orElse(200));
        setChanged();
    }
    /** What one batch of `input` makes. Rough metal leaves hot, and so does a pass that keeps its material, a heat. */
    private ItemStack product(net.minecraft.world.item.crafting.SingleRecipeInput input) {
        var result=processingRecipe(input).map(recipe->recipe.value().assemble(input,level.registryAccess())).orElse(ItemStack.EMPTY);
        return !result.isEmpty()&&(result.getItem() instanceof RoughMetal||result.is(input.item().getItem()))?HotMetal.heated(result,level.getGameTime()):result;
    }
    public java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> processingRecipe(net.minecraft.world.item.crafting.SingleRecipeInput input) {
        return candidates(recipes(), selectedOutput, level.registryAccess()).stream().filter(r -> r.value().matches(input,level)).findFirst();
    }
    public final CoalFire fire=new CoalFire(this);
    public int[] coalFuelSlots(){return MachineInventory.KILN_FUEL;}
    /** The Kiln's fieldstone beehive; the Foundry, Fertilizer Furnace and Cooking Stove name their own. */
    public int masonry(){return 17;}
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
        var result = product(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)));
        if (result.isEmpty()) return 5;
        if (!MachineInventory.room(this,MachineInventory.KILN_OUTPUT,result)) return 3;
        return fire.hot() ? 2 : fire.lit() ? 8 : 4;
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
    /** Hot metal is taken by hand. */
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,net.minecraft.core.Direction side){return side==net.minecraft.core.Direction.DOWN&&MachineInventory.contains(MachineInventory.KILN_OUTPUT,slot)&&!stack.has(HotMetal.HOT.get());}
    protected String auditPrefix() { return "kiln"; }
    public static boolean acceptsFuel(ItemStack stack) { return stack.is(KilnContent.MINERAL_COAL.get()); }
    /** The coal fire stokes the fuel; vanilla never burns it. */
    @Override protected int getBurnDuration(ItemStack stack) { return 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return MachineInventory.contains(MachineInventory.KILN_FUEL,slot) ? acceptsFuel(stack) : MachineInventory.contains(MachineInventory.KILN_INPUT,slot) && level != null && level.getRecipeManager()
                .getRecipeFor(processType, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level).isPresent();
    }
    @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new KilnMenu(id, inventory, this, dataAccess); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);fire.load(tag);
        if (model() != null) { int before = build.sections; build.load(tag, lookup, model()); if (level != null && level.isClientSide && build.sections != before) ModeledMachine.redraw(this); }
        selectedOutput=tag.contains("SelectedOutput")?net.minecraft.resources.ResourceLocation.tryParse(tag.getString("SelectedOutput")):null;
        legacyOverflow.clear();
        var old=net.minecraft.core.NonNullList.withSize(12,ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag,old,lookup);
        if(!tag.getBoolean("TwoFuelSlots"))items.set(3,ItemStack.EMPTY);
        if(!tag.getBoolean("TwoMaterialSlots")){items.set(4,ItemStack.EMPTY);items.set(5,ItemStack.EMPTY);}
        for(int i=tag.getBoolean("TwoMaterialSlots")?6:tag.getBoolean("TwoFuelSlots")?4:3;i<old.size();i++)if(!old.get(i).isEmpty())legacyOverflow.add(old.get(i));
        if(tag.contains("LegacyOverflow"))for(var entry:tag.getList("LegacyOverflow",10))
            ItemStack.parse(lookup,entry).ifPresent(legacyOverflow::add);
    }
    private final java.util.List<ItemStack> legacyOverflow=new java.util.ArrayList<>();
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);fire.save(tag);tag.putBoolean("TwoFuelSlots",true);
        if(model()!=null)build.save(tag,lookup);
        tag.putBoolean("TwoMaterialSlots",true);
        if(selectedOutput!=null)tag.putString("SelectedOutput",selectedOutput.toString());
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
        // Once a second the fire keeps its output hot, and metal that cooled before it came in settles.
        if (level.getGameTime() % 20 == 0) {
            for (int slot : MachineInventory.KILN_OUTPUT) HotMetal.tend(kiln.getItem(slot), kiln.fire.hot(), level.getGameTime());
            for (int slot : MachineInventory.KILN_INPUT) HotMetal.cool(kiln.getItem(slot), level.getGameTime());
        }
        MachineInventory.refill(kiln,MachineInventory.KILN_INPUT);
        // Active machines validate every work tick; idle ones stagger checks once per second.
        if (kiln.fire.lit() || kiln.fire.heat() > 0 || !kiln.getItem(0).isEmpty() || Math.floorMod(level.getGameTime() + pos.asLong(), 20) == 0)
            kiln.checkStructure();
        if (kiln.structureStatus() != MachineStructure.COMPLETE) {
            if (kiln.structureStatus() == MachineStructure.INCOMPLETE && (kiln.fire.lit() || kiln.fire.heat() > 0 || kiln.dataAccess.get(2) > 0)) {
                kiln.fire.extinguish(); kiln.dataAccess.set(2, 0);
                kiln.setChanged();
                EnergyLog.machine(level, pos, kiln.auditPrefix() + "_structure_broken", null, 0, null, 0);
            }
            feedback(level, pos, false, false);
            return;
        }
        MachineInventory.refill(kiln,MachineInventory.KILN_FUEL);
        int fuel = MachineInventory.count(kiln,MachineInventory.KILN_FUEL);
        kiln.fire.tick(1);
        if (MachineInventory.count(kiln,MachineInventory.KILN_FUEL) < fuel)
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.2f, 0.9f);
        if (!kiln.fire.hot() || MachineWeather.wetWorkFace(level, pos, state)) { feedback(level, pos, kiln.fire.lit(), false); return; }
        var input=new net.minecraft.world.item.crafting.SingleRecipeInput(kiln.getItem(0));
        var result=kiln.product(input);
        boolean hot=result.has(HotMetal.HOT.get()),heat=hot&&result.is(kiln.getItem(0).getItem());
        // Vanilla cooks into slot 2 only, one unit of the input at a time. Each batch cooks into an emptied
        // slot 2; the product takes its grade from the input unit it used, a heat takes the rest of its load, then it joins the output bank.
        boolean redirect=!result.isEmpty()&&MachineInventory.room(kiln,MachineInventory.KILN_OUTPUT,result);
        var originalOutput=redirect?kiln.items.set(2,ItemStack.EMPTY):ItemStack.EMPTY;
        var inputUnits=kiln.getItem(0).copy();
        var inputItem = kiln.getItem(0).getItem();
        int inputCount = kiln.getItem(0).getCount();
        int outputCount = MachineInventory.count(kiln,MachineInventory.KILN_OUTPUT);
        int progress = kiln.dataAccess.get(2);
        // Vanilla cooks only while lit and spends a tick of burn time first; the heat itself comes from the fire.
        kiln.dataAccess.set(0,2);
        try{serverTick(level, pos, state, kiln);}
        finally{kiln.dataAccess.set(0,0);if(redirect){var produced=kiln.items.set(2,originalOutput);if(!produced.isEmpty()){Grade.smelted(produced,inputUnits,level.random);if(hot)HotMetal.heated(produced,level.getGameTime());if(heat)HotMetal.load(kiln,0,MachineInventory.KILN_OUTPUT,produced);MachineInventory.insert(kiln,MachineInventory.KILN_OUTPUT,produced);}}}
        boolean completed = kiln.getItem(0).getCount() < inputCount;
        boolean working = completed || kiln.dataAccess.get(2) > progress;
        if (working) kiln.fire.draw(1);
        feedback(level, pos, kiln.fire.lit(), working);
        if (completed) {
            EnergyLog.machine(level, pos, kiln.auditPrefix() + "_batch", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inputItem).toString(),
                    inputCount - kiln.getItem(0).getCount(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(kiln.getItem(2).getItem()).toString(), MachineInventory.count(kiln,MachineInventory.KILN_OUTPUT) - outputCount);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DECORATED_POT_PLACE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.15f, 1.2f);
        }
        // Mark work in progress dirty too, so a normal chunk save preserves elapsed work.
        if (progress != kiln.dataAccess.get(2)) kiln.setChanged();
    }
    private static void feedback(Level level, BlockPos pos, boolean lit, boolean working) {
        var state = level.getBlockState(pos);
        if (state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT) != lit || state.getValue(MachineFeedback.WORKING) != working)
            level.setBlock(pos, state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, lit).setValue(MachineFeedback.WORKING, working), 3);
    }
}
