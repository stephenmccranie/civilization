package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class FoundryBlockEntity extends KilnBlockEntity {
    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        super(KilnContent.FOUNDRY_ENTITY.get(), pos, state, KilnContent.FOUNDRY_RECIPE_TYPE.get());
    }
    @Override protected String auditPrefix() { return "foundry"; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new KilnMenu(KilnContent.FOUNDRY_MENU.get(), KilnContent.FOUNDRY_RECIPE_TYPE.get(), id, inventory, this, dataAccess);
    }
}
