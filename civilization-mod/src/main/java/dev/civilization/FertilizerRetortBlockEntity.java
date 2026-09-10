package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class FertilizerRetortBlockEntity extends KilnBlockEntity {
    public FertilizerRetortBlockEntity(BlockPos pos, BlockState state) {
        super(KilnContent.RETORT_ENTITY.get(), pos, state, KilnContent.RETORT_RECIPE_TYPE.get());
    }
    @Override protected String auditPrefix() { return "retort"; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new KilnMenu(KilnContent.RETORT_MENU.get(), KilnContent.RETORT_RECIPE_TYPE.get(), id, inventory, this, dataAccess);
    }
}
