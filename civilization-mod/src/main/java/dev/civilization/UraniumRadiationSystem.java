package dev.civilization;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends a bounded list of nearby stocked chests without loading chunks. */
final class UraniumRadiationSystem {
    UraniumRadiationSystem() { NeoForge.EVENT_BUS.addListener(this::tick); }

    private void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 0) return;
        var level = player.serverLevel();
        var center = player.blockPosition();
        var found = new ArrayList<BlockPos>();
        int minX = (center.getX() - 16) >> 4, maxX = (center.getX() + 16) >> 4;
        int minZ = (center.getZ() - 16) >> 4, maxZ = (center.getZ() + 16) >> 4;
        for (int cx = minX; cx <= maxX; cx++) for (int cz = minZ; cz <= maxZ; cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (!(chunk instanceof LevelChunk)) continue;
            for (var entry : chunk.getBlockEntities().entrySet()) {
                if (!(entry.getValue() instanceof ChestBlockEntity chest)) continue;
                var pos = entry.getKey();
                if (pos.distSqr(center) > 16 * 16) continue;
                boolean uranium = false;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    var stack = chest.getItem(slot);
                    if (stack.is(FrontierContent.RAW_URANIUM.get())
                            || stack.is(FrontierContent.URANIUM_ORE.asItem())) {
                        uranium = true;
                        break;
                    }
                }
                if (uranium) found.add(pos.immutable());
                if (found.size() >= 64) break;
            }
            if (found.size() >= 64) break;
        }
        PacketDistributor.sendToPlayer(player, new UraniumRadiationPayload(found));
    }
}
