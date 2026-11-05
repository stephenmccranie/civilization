package dev.civilization;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Cutting a log of a natural tree also breaks every log of that tree at or above the cut, each through the
 * player's own block-breaking path, so claims, calories, tool wear and loot apply per log exactly as by hand.
 */
@EventBusSubscriber(modid = "civilization")
public final class TreeFelling {
    /** A giant jungle tree, the largest vanilla tree, has at most about 130 trunk logs plus a few short branches. */
    static final int MAX_LOGS = 256;
    /** The felled logs' own break events must not start felling again. */
    private static boolean felling;

    /** Runs last, so only a cut every other rule has accepted fells anything. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cut(BlockEvent.BreakEvent e) {
        if (felling || !(e.getPlayer() instanceof ServerPlayer player) || player.isCreative() || !e.getState().is(BlockTags.LOGS)) return;
        var level = player.serverLevel();
        var logs = tree(level, e.getPos());
        felling = true;
        try {
            for (var pos : logs) {
                if (!affordable(player.getMainHandItem())) return;
                // A break plays its particles and sound for everyone but the breaker, whose client predicted it;
                // the cutter's client predicted only the cut, so it is sent each felled log's break too.
                var effect = new ClientboundLevelEventPacket(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(level.getBlockState(pos)), false);
                if (level.mayInteract(player, pos) && player.gameMode.destroyBlock(pos)) player.connection.send(effect);
            }
        } finally {
            felling = false;
        }
    }

    /**
     * The logs connected to the cut through any face, edge or corner without dipping below the cut's height, in
     * order of distance from it and without the cut itself. Empty unless one of them touches natural foliage and
     * there are at most {@link #MAX_LOGS}. Only loaded positions are read.
     */
    static List<BlockPos> tree(ServerLevel level, BlockPos cut) {
        var logs = new LinkedHashSet<BlockPos>(List.of(cut));
        var open = new ArrayDeque<BlockPos>(List.of(cut));
        boolean natural = false;
        while (!open.isEmpty()) {
            var log = open.poll();
            for (var near : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
                if (near.getY() < cut.getY() || !Chunks.loaded(level, near)) continue;
                var state = level.getBlockState(near);
                var next = near.immutable();
                if (!state.is(BlockTags.LOGS)) natural |= foliage(state);
                else if (logs.add(next)) {
                    if (logs.size() > MAX_LOGS) return List.of();
                    open.add(next);
                }
            }
        }
        if (!natural) return List.of();
        logs.remove(cut);
        return List.copyOf(logs);
    }

    /** Leaves a tree grew, which players cannot place; nether trees grow wart blocks instead. */
    private static boolean foliage(BlockState state) {
        return state.is(BlockTags.WART_BLOCKS)
                || state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }

    /** The tool pays for another log and still for the cut, which follows the felling, so only the cut can break it. */
    private static boolean affordable(ItemStack tool) {
        var rule = tool.get(DataComponents.TOOL);
        return !tool.isDamageableItem() || rule == null || tool.getMaxDamage() - tool.getDamageValue() >= 2 * rule.damagePerBlock();
    }
}
