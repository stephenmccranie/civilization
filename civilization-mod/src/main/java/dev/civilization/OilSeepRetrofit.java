package dev.civilization;

import java.nio.file.Files;
import java.util.Map;
import java.util.WeakHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in, incremental backfill for saved chunks that predate physical surface oil. */
@EventBusSubscriber(modid = Civilization.MOD_ID)
public final class OilSeepRetrofit {
    public static final String MARKER = "civilization-retrofit-oil.flag";
    private static final Map<ServerLevel, LongLinkedOpenHashSet> QUEUES = new WeakHashMap<>();

    private OilSeepRetrofit() {}

    private static boolean enabled(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD)
                && Files.isRegularFile(level.getServer().getWorldPath(LevelResource.ROOT).resolve(MARKER));
    }

    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)
                || !(chunk.getLevel() instanceof ServerLevel level) || !enabled(level)) return;
        long key = chunk.getPos().toLong();
        // Do not scan or mutate a chunk inside the load callback.
        level.getServer().execute(() -> QUEUES.computeIfAbsent(level, ignored -> new LongLinkedOpenHashSet()).add(key));
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        var queue = QUEUES.get(level);
        if (queue == null || queue.isEmpty()) return;
        for (int i = 0; i < 32 && !queue.isEmpty(); i++) {
            long key = queue.removeFirstLong();
            int cx = net.minecraft.world.level.ChunkPos.getX(key), cz = net.minecraft.world.level.ChunkPos.getZ(key);
            if (!level.hasChunk(cx, cz)) continue;
            LevelChunk chunk = level.getChunk(cx, cz);
            Deposits.Site site = Deposits.candidate(level, Math.floorDiv(cx, 16), Math.floorDiv(cz, 16));
            if (site == null || site.kind() != Deposits.Kind.OIL
                    || cx * 16 > site.x() + site.radius() || cx * 16 + 15 < site.x() - site.radius()
                    || cz * 16 > site.z() + site.radius() || cz * 16 + 15 < site.z() - site.radius()) continue;
            process(level, chunk, site);
            // A site slice can change thousands of blocks; do at most one per tick.
            break;
        }
    }

    static void process(ServerLevel level, LevelChunk chunk, Deposits.Site site) {
        var state = chunk.getData(OilRetrofitChunk.TYPE);
        if (state.done) return;
        retrofit(level, chunk, site);
        state.done = true;
        chunk.setUnsaved(true);
    }

    /** Only natural geology is replaced; existing oil means this chunk was already generated. */
    static int retrofit(ServerLevel level, LevelChunk chunk, Deposits.Site site) {
        int minX = Math.max(chunk.getPos().getMinBlockX(), site.x() - site.radius());
        int maxX = Math.min(chunk.getPos().getMaxBlockX(), site.x() + site.radius());
        int minZ = Math.max(chunk.getPos().getMinBlockZ(), site.z() - site.radius());
        int maxZ = Math.min(chunk.getPos().getMaxBlockZ(), site.z() + site.radius());
        if (minX > maxX || minZ > maxZ) return 0;
        boolean generated = false;
        for (int x = minX; x <= maxX && !generated; x++) for (int z = minZ; z <= maxZ && !generated; z++) {
            for (int y = site.bottom(); y <= site.ceiling(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!site.body(pos)) continue;
                if (chunk.getBlockState(pos).is(IndustrialContent.RESERVOIR_OIL.get())) {
                    generated = true;
                    break;
                }
            }
        }
        int changed = 0;
        if (!generated) for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            for (int y = site.bottom(); y <= site.ceiling(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!site.body(pos)) continue;
                var old = chunk.getBlockState(pos);
                if (!Deposits.naturalRock(old)) continue;
                var replacement = y <= site.top() ? IndustrialContent.RESERVOIR_OIL.get().defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                if (level.setBlock(pos, replacement, 2)) changed++;
            }
        }
        if (site.x() >> 4 == chunk.getPos().x && site.z() >> 4 == chunk.getPos().z) {
            BlockPos ground = new BlockPos(site.x(), site.y() - 1, site.z());
            var below = level.getBlockState(ground);
            if (below.is(IndustrialContent.OIL_SEEP.get()) || naturalSurface(below)) {
                BlockPos top = ground.above();
                if (level.getBlockState(top).isAir()) {
                    if (!below.is(IndustrialContent.OIL_SEEP.get())) level.setBlock(ground, IndustrialContent.OIL_SEEP.get().defaultBlockState(), 3);
                    if (level.setBlock(top, IndustrialContent.SURFACE_OIL.get().defaultBlockState(), 3)) changed++;
                    SurfaceOilFlow.activate(level, top);
                }
            }
        }
        return changed;
    }

    private static boolean naturalSurface(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL) || Deposits.naturalRock(state);
    }
}
