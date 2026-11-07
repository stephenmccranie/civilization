package dev.civilization;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Vanilla spawns fish and the other surface swimmers only from sea level to 13 blocks below it, which leaves every river
 * and lake standing above the sea empty. They may also spawn in the top 13 blocks of any water open to the sky, as the
 * sea is, with water over them. Only placed blocks are read; the biome still decides the species.
 */
final class OpenWaterSwimmers {
    static final int DEPTH = 13;
    // Every swimmer whose vanilla rule is the sea band, and tropical fish, whose rule includes it
    private static final List<EntityType<?>> SWIMMERS = List.of(EntityType.COD, EntityType.SALMON,
            EntityType.PUFFERFISH, EntityType.TROPICAL_FISH, EntityType.SQUID, EntityType.DOLPHIN);

    private OpenWaterSwimmers() {}

    static void register(IEventBus modBus) {
        modBus.addListener(RegisterSpawnPlacementsEvent.class, event -> SWIMMERS.forEach(type -> allow(event, type)));
    }

    // Registered without an operation, so vanilla's sea band still applies alongside
    private static <T extends Entity> void allow(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, (t, level, reason, pos, random) -> inOpenWater(level, pos));
    }

    /** Within DEPTH blocks below the surface of open water, where that surface plays the part of vanilla's sea level. */
    static boolean inOpenWater(LevelReader level, BlockPos pos) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return pos.getY() >= surface - DEPTH && pos.getY() <= surface
                && level.getFluidState(pos.atY(surface - 1)).is(FluidTags.WATER)
                && level.getBlockState(pos.above()).is(Blocks.WATER);
    }
}
