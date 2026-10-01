package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Same finite-fuel and inventory rules as industry, without a multiblock shell. */
public final class CookingStationBlockEntity extends KilnBlockEntity {
    public CookingStationBlockEntity(BlockPos pos, BlockState state) {
        super(CookingContent.ENTITY.get(), pos, state, CookingContent.RECIPE_TYPE.get());
    }
    @Override public double coalWasteHeatFactor(){return ThermalRules.STOVE_WASTE_HEAT_FACTOR;}
    @Override public boolean requiresStructure() { return false; }
    @Override protected String auditPrefix() { return "cooking"; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new KilnMenu(CookingContent.MENU.get(), CookingContent.RECIPE_TYPE.get(), id, inventory, this, dataAccess);
    }
}
