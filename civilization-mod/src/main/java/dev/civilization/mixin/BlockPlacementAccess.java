package dev.civilization.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The placement preview asks the same question a right-click does: the block's own state plus vanilla's survival and entity checks. */
@Mixin(BlockItem.class)
public interface BlockPlacementAccess {
    @Invoker("getPlacementState") BlockState civilization$placementState(BlockPlaceContext context);
}
