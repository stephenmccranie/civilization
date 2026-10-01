package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Map;
import java.util.WeakHashMap;

/** Bounded, wearer-only survey of loaded ore. Never loads chunks or reveals coordinates. */
final class GeigerSurvey {
    private static final int RANGE = 48;
    private static final int RANGE_SQUARED = RANGE * RANGE;
    private final Map<ServerPlayer, Signal> signals = new WeakHashMap<>();
    private static final class Signal { int distanceSquared = RANGE_SQUARED + 1; int nextScan; int nextClick; }

    GeigerSurvey() { NeoForge.EVENT_BUS.addListener(this::tick); }

    private void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Accessories.wearing(player, FrontierContent.GEIGER_COUNTER.get())) {
            signals.remove(player);
            return;
        }
        var signal = signals.computeIfAbsent(player, id -> new Signal());
        if (player.tickCount >= signal.nextScan) {
            signal.distanceSquared = closestOre(player.serverLevel(), player.blockPosition());
            signal.nextScan = player.tickCount + 20;
        }
        if (signal.distanceSquared > RANGE_SQUARED || player.tickCount < signal.nextClick) return;
        // One click per second at the edge, up to four per second close to a find.
        int interval = Math.max(5, (int) Math.ceil(5 + 15 * Math.sqrt(signal.distanceSquared) / RANGE));
        signal.nextClick = player.tickCount + interval;
        player.playNotifySound(SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS,
                0.35F, 1.25F + 0.35F * (1F - (float) Math.sqrt(signal.distanceSquared) / RANGE));
    }

    static int closestOre(ServerLevel level, BlockPos center) {
        int best = RANGE_SQUARED + 1;
        int minX = center.getX() - RANGE, maxX = center.getX() + RANGE;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - RANGE);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + RANGE);
        int minZ = center.getZ() - RANGE, maxZ = center.getZ() + RANGE;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (!(chunk instanceof LevelChunk)) continue;
            for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                int index = chunk.getSectionIndex(sy << 4);
                if (index < 0 || index >= chunk.getSectionsCount()) continue;
                var section = chunk.getSection(index);
                if (!section.maybeHas(state -> state.is(FrontierContent.URANIUM_ORE.get()))) continue;
                for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++)
                    for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++)
                        for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                            if (!section.getBlockState(x & 15, y & 15, z & 15).is(FrontierContent.URANIUM_ORE.get())) continue;
                            int dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
                            best = Math.min(best, dx * dx + dy * dy + dz * dz);
                        }
            }
        }
        return best;
    }
}
