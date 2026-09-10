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
public class KilnBlockEntity extends AbstractFurnaceBlockEntity {
    public static final int FUEL_TICKS = 1600;
    private MachineStructure.Result structure = new MachineStructure.Result(MachineStructure.INCOMPLETE, null, "");
    public int structureStatus() { return structure.status(); }
    public boolean isFertilizerWorks() { return this instanceof FertilizerRetortBlockEntity; }
    public boolean requiresStructure() { return true; }
    public MachineStructure.Result checkStructure() {
        structure = requiresStructure()
                ? MachineStructure.check(level, worldPosition, getBlockState().getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING), isFertilizerWorks())
                : new MachineStructure.Result(MachineStructure.COMPLETE, null, "");
        return structure;
    }
    /** Small, synchronized state vocabulary shared by all three menus. */
    public int operatingStatus() {
        if (requiresStructure() && structureStatus() != MachineStructure.COMPLETE) return 0;
        if (getBlockState().getValue(MachineFeedback.WORKING)) return 1;
        if (!getItem(0).isEmpty() && !canPlaceItem(0, getItem(0))) return 5;
        if (getItem(0).isEmpty()) return getItem(2).isEmpty() ? 2 : 6;
        var recipe = level.getRecipeManager().getRecipeFor(processType,
                new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)), level).orElseThrow().value();
        var result = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(getItem(0)), level.registryAccess());
        var output = getItem(2);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize())) return 3;
        return dataAccess.get(0) > 0 ? 2 : 4;
    }
    private final net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> processType;
    public KilnBlockEntity(BlockPos pos, BlockState state) { this(KilnContent.ENTITY.get(), pos, state, KilnContent.RECIPE_TYPE.get()); }
    protected KilnBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> entityType, BlockPos pos, BlockState state,
                              net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> processType) {
        super(entityType, pos, state, processType);
        this.processType = processType;
    }
    protected String auditPrefix() { return "kiln"; }
    public static boolean acceptsFuel(ItemStack stack) { return stack.is(KilnContent.MINERAL_COAL.get()); }
    @Override protected int getBurnDuration(ItemStack stack) { return acceptsFuel(stack) ? FUEL_TICKS : 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 1 ? acceptsFuel(stack) : slot == 0 && level != null && level.getRecipeManager()
                .getRecipeFor(processType, new net.minecraft.world.item.crafting.SingleRecipeInput(stack), level).isPresent();
    }
    @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new KilnMenu(id, inventory, this, dataAccess); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        // Vanilla reconstructs duration from the fuel slot, which may be empty after ignition.
        dataAccess.set(1, dataAccess.get(0) > 0 ? FUEL_TICKS : 0);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
        // Active machines validate every work tick; idle ones stagger checks once per second.
        if (kiln.dataAccess.get(0) > 0 || !kiln.getItem(0).isEmpty() || Math.floorMod(level.getGameTime() + pos.asLong(), 20) == 0)
            kiln.checkStructure();
        if (kiln.structureStatus() != MachineStructure.COMPLETE) {
            if (kiln.structureStatus() == MachineStructure.INCOMPLETE && (kiln.dataAccess.get(0) > 0 || kiln.dataAccess.get(2) > 0)) {
                kiln.dataAccess.set(0, 0); kiln.dataAccess.set(2, 0);
                kiln.setChanged();
                EnergyLog.machine(level, pos, kiln.auditPrefix() + "_structure_broken", null, 0, null, 0);
            }
            if (state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT) || state.getValue(MachineFeedback.WORKING))
                level.setBlock(pos, state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, false)
                        .setValue(MachineFeedback.WORKING, false), 3);
            return;
        }
        // Resume saved heat after a neighboring chunk reloads.
        if (kiln.dataAccess.get(0) > 0 && !state.getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT)) {
            state = state.setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, true);
            level.setBlock(pos, state, 3);
        }
        var inputItem = kiln.getItem(0).getItem();
        int inputCount = kiln.getItem(0).getCount();
        int outputCount = kiln.getItem(2).getCount();
        int fuel = kiln.getItem(1).getCount();
        int progress = kiln.dataAccess.get(2);
        int heat = kiln.dataAccess.get(0);
        serverTick(level, pos, state, kiln);
        boolean completed = kiln.getItem(0).getCount() < inputCount;
        boolean working = completed || kiln.dataAccess.get(2) > progress;
        var current = kiln.getBlockState();
        if (current.getValue(MachineFeedback.WORKING) != working)
            level.setBlock(pos, current.setValue(MachineFeedback.WORKING, working), 3);
        if (kiln.getItem(1).getCount() < fuel) {
            EnergyLog.machine(level, pos, kiln.auditPrefix() + "_fuel", "civilization:mineral_coal", 1, null, 0);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.2f, 0.9f);
        }
        if (completed) {
            EnergyLog.machine(level, pos, kiln.auditPrefix() + "_batch", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inputItem).toString(),
                    1, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(kiln.getItem(2).getItem()).toString(), kiln.getItem(2).getCount() - outputCount);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DECORATED_POT_PLACE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.15f, 1.2f);
        }
        // Mark work in progress dirty too, so a normal chunk save preserves elapsed fuel/work.
        if (heat != kiln.dataAccess.get(0) || progress != kiln.dataAccess.get(2)) kiln.setChanged();
    }
}
