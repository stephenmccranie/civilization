package dev.civilization.client;

import dev.civilization.MultiCellBlock;
import dev.civilization.mixin.BlockPlacementAccess;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** What a right-click with the held block item would place, answered by the placement code that click runs.
 * Claims are known only to the server, so it can still refuse a spot shown as valid inside someone's claim. */
public record PlacementPlan(BlockPos at, BlockState state, boolean valid, List<MultiCellBlock.Cell> cells) {
    /** The aimed placement, or null when the player is not aiming at a block with a block item they may place. */
    public static PlacementPlan of(Minecraft mc) {
        var player = mc.player;
        if (mc.screen != null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !player.getAbilities().mayBuild) return null;
        var hand = player.getMainHandItem().getItem() instanceof BlockItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        var context = new BlockPlaceContext(player, hand, stack, hit);
        var placing = context.canPlace() ? item.updatePlacementContext(context) : null;
        var placed = placing == null ? null : ((BlockPlacementAccess) item).civilization$placementState(placing);
        // A refused spot still shows where the block would go: its own state when it has one,
        // otherwise facing the player, as every multi-cell block and controller is placed.
        var state = placed != null ? placed
                : placing != null && item.getBlock().getStateForPlacement(placing) instanceof BlockState shaped ? shaped
                : item.getBlock().defaultBlockState().trySetValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
        var at = (placing == null ? context : placing).getClickedPos();
        return new PlacementPlan(at, state, placed != null, state.getBlock() instanceof MultiCellBlock block
                ? block.cells(at, state) : List.of(new MultiCellBlock.Cell(at, state)));
    }
}
