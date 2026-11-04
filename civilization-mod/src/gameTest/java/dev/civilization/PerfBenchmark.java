package dev.civilization;

import java.lang.management.ManagementFactory;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * The performance benchmark: a walk through a town of every custom model, on a lawn in open plains, in one world whose
 * Distant Horizons terrain is stored 128 chunks out, in the player pack at 2560x1440. A run that finds no world creates
 * it from {@link #SEED}: it finds open plains for the town, generates every chunk the walk loads, waits at the town
 * while Distant Horizons generates its terrain (dev.ps1 Perf sets how far), then saves and stops; dev.ps1 Perf keeps
 * that world for every later run. A run that finds the world builds the town, waits until the world has finished
 * loading and walks the route once to load what it shows, then twice more: uncapped, for what each part of the frame
 * costs on the CPU and GPU, with a Java Flight Recorder profile of every thread; and capped at the 120 fps target, for
 * the frame pacing players get. Each leg of the walk is measured on its own; {@link PerfReport} writes the results to
 * runs/perf/report.
 */
final class PerfBenchmark {
    static final int TARGET_FPS = 120;
    private static final String WORLD = "benchmark";
    private static final long SEED = 7_355_608L;
    /** Chunks generated around the walk: the pack's render distance of 12 and the border the server loads beyond it. */
    private static final int NEAR_CHUNKS = 14;
    private static final int YARDS = 3, SPACING = 18;
    /** Walking speed in blocks per tick, how many blocks ahead the walker looks, and how far down. */
    private static final double SPEED = 4.317 / 20, LOOK = 6;
    private static final float PITCH = 5;
    /** The lawn the town stands on, relative to its corner: least and greatest x, least and greatest z. */
    private static final int[] LAWN = {-20, 32, -10, 56};
    /** The world has finished generating or loading once the threads that build it, beside the render and server threads,
     *  use under half a core: CPU nanoseconds a second. Generating or loading takes several cores; Distant Horizons'
     *  upkeep of changed chunks alone takes a tenth to a third of one. */
    private static final long QUIET = 500_000_000;

    private record Leg(String name, int[][] to) {}
    /** The walk relative to the town's corner, from {@link #START}: across the meadow to the town, along the lane
     *  between the first two yards, and out into the country. Each leg walks through its corners in turn. */
    private static final int[] START = {-44, 8};
    private static final List<Leg> LEGS = List.of(
            new Leg("approach", new int[][]{{-15, 16}}),
            new Leg("yards", new int[][]{{26, 16}}),
            new Leg("outskirts", new int[][]{{55, 10}}));
    /** The walk's corners in order, the distance walked at each, the distance at which each leg ends, and the least
     *  and greatest x and z it reaches. */
    private static final List<int[]> CORNERS = new ArrayList<>();
    private static final double[] AT, LEG_END;
    private static final int[] BOUNDS = {START[0], START[0], START[1], START[1]};
    static {
        CORNERS.add(START);
        for (var leg : LEGS) CORNERS.addAll(List.of(leg.to()));
        AT = new double[CORNERS.size()];
        for (int i = 1; i < AT.length; i++) {
            var a = CORNERS.get(i - 1); var b = CORNERS.get(i);
            AT[i] = AT[i - 1] + Math.hypot(b[0] - a[0], b[1] - a[1]);
            BOUNDS[0] = Math.min(BOUNDS[0], b[0]); BOUNDS[1] = Math.max(BOUNDS[1], b[0]); BOUNDS[2] = Math.min(BOUNDS[2], b[1]); BOUNDS[3] = Math.max(BOUNDS[3], b[1]);
        }
        LEG_END = new double[LEGS.size()];
        for (int leg = 0, corner = 0; leg < LEGS.size(); leg++) LEG_END[leg] = AT[corner += LEGS.get(leg).to().length];
    }

    private enum Phase { JOIN, GENERATE, BUILD, SETTLE, WALK }
    private static Phase phase = Phase.JOIN;
    /** The walks in turn: one unmeasured, since chunk meshes and grass blades are built as the route first brings them
     *  near, then one uncapped for costs and one capped at the target for pacing. */
    private enum Walk { WARM_UP, COST, PACING }
    private static Walk walk = Walk.WARM_UP;
    private static boolean opened, generating;
    private static int ticks, phaseStart, leg, quiet;
    private static long builderCpu;
    private static double walked;
    private static float yaw;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    /** The town's corner, on its lawn, and the walker's feet height at each block walked; set by the server. */
    private static volatile BlockPos corner;
    private static volatile float[] feet;
    private static jdk.jfr.Recording recording;
    private static final List<PerfRecorder.Window> windows = new ArrayList<>();
    private static final List<PerfReport.Pass> passes = new ArrayList<>();

    private PerfBenchmark() {}

    static void tick(Minecraft mc) {
        if (failure != null) throw new IllegalStateException("Performance benchmark", failure);
        if (!opened) { open(mc); return; }
        var server = mc.getSingleplayerServer();
        if (mc.level == null || mc.player == null || server == null) return;
        ticks++;
        switch (phase) {
            case JOIN -> {
                if (ticks < 100) return;
                mc.options.pauseOnLostFocus = false; mc.options.hideGui = false;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                // Generation leaves the CPU to the generators; the walk starts uncapped.
                limit(mc, generating ? 30 : 260);
                server.execute(() -> { try { if (generating) generate(server.overworld()); else build(server.overworld()); } catch (Throwable t) { failure = t; } });
                next(generating ? Phase.GENERATE : Phase.BUILD);
            }
            case GENERATE -> {
                // Distant Horizons' generators work without pause until its terrain is done.
                if (!ready || !settled(mc, 30)) return;
                com.mojang.logging.LogUtils.getLogger().info("PERFORMANCE WORLD GENERATED around {} in {} s", corner, elapsed() / 20);
                mc.stop();
            }
            case BUILD -> {
                if (!ready) return;
                // From here on, generating terrain would cost what players' clients never pay.
                NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, e -> {
                    if (e.isNewChunk()) failure = new IllegalStateException("Chunk " + e.getChunk().getPos() + " was generated while measuring; delete .tools/perf/world so dev.ps1 Perf generates it again");
                });
                place(mc, 0, true);
                next(Phase.SETTLE);
            }
            case SETTLE -> {
                place(mc, walked, false);
                if (elapsed() > 6000) throw new IllegalStateException("The world did not finish loading within five minutes");
                if (!settled(mc, 5)) return;
                com.mojang.logging.LogUtils.getLogger().info("Benchmark settled after {} s", elapsed() / 20);
                startLeg(mc);
                next(Phase.WALK);
            }
            case WALK -> {
                walked = Math.min(walked + SPEED, LEG_END[LEG_END.length - 1]);
                place(mc, walked, false);
                if (walked < LEG_END[leg]) return;
                windows.add(PerfRecorder.INSTANCE.end());
                if (++leg < LEGS.size()) { startLeg(mc); return; }
                var profile = walk == Walk.COST ? PerfReport.saveProfile(recording, mc) : null;
                if (walk != Walk.WARM_UP) for (var w : windows) passes.add(new PerfReport.Pass(w, walk == Walk.PACING, profile));
                windows.clear();
                if (walk == Walk.PACING) { finish(mc); return; }
                walk = Walk.values()[walk.ordinal() + 1]; leg = 0; walked = 0;
                place(mc, 0, true); limit(mc, walk == Walk.PACING ? TARGET_FPS : 260);
                // The flight recorder's start slows the game for a while, so it starts before settling; legs are cut from it by time.
                if (walk == Walk.COST) recording = PerfReport.startProfile();
                next(Phase.SETTLE);
            }
        }
    }

    private static int elapsed() { return ticks - phaseStart; }
    private static void next(Phase p) { phase = p; phaseStart = ticks; quiet = 0; }

    /** Whether every chunk section is drawn and the world's builders have been quiet for the last `seconds` seconds. */
    private static boolean settled(Minecraft mc, int seconds) {
        if (elapsed() % 20 != 0) return false;
        long cpu = builderCpu();
        quiet = cpu - builderCpu < QUIET ? quiet + 1 : 0; builderCpu = cpu;
        return quiet >= seconds && mc.levelRenderer.hasRenderedAllSections();
    }

    /** Opens the benchmark world from the title screen, or creates it when there is none. */
    private static void open(Minecraft mc) {
        if (!(mc.screen instanceof TitleScreen)) return;
        opened = true;
        generating = !mc.getLevelSource().levelExists(WORLD);
        if (generating) {
            var settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(SEED, true, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
            return;
        }
        mc.createWorldOpenFlows().openWorld(WORLD, mc::stop);
    }

    /** The frame-rate cap: 260 is Minecraft's unlimited; vsync stays off so the cap alone paces frames. */
    private static void limit(Minecraft mc, int fps) {
        mc.options.enableVsync().set(false); mc.getWindow().updateVsync(false);
        mc.options.framerateLimit().set(fps); mc.getWindow().setFramerateLimit(fps);
    }

    private static void startLeg(Minecraft mc) {
        var name = LEGS.get(leg).name();
        if (walk == Walk.COST) Screenshot.grab(mc.gameDirectory, "perf-" + name + ".png", mc.getMainRenderTarget(), message -> {});
        PerfRecorder.INSTANCE.begin(name, walk == Walk.COST);
    }

    private static void finish(Minecraft mc) {
        try { PerfReport.write(mc, passes); } catch (Exception e) { throw new IllegalStateException("Performance report", e); }
        com.mojang.logging.LogUtils.getLogger().info("PERFORMANCE MEASURED: {} passes written to runs/perf/report", passes.size());
        mc.stop();
    }

    // --- the walk ----------------------------------------------------------------------------------------------------

    /** The point `d` blocks along the walk, relative to the town's corner. */
    private static double[] point(double d) {
        int i = 1;
        while (i < AT.length - 1 && AT[i] < d) i++;
        var a = CORNERS.get(i - 1); var b = CORNERS.get(i);
        double t = Mth.clamp((d - AT[i - 1]) / (AT[i] - AT[i - 1]), 0, 1);
        return new double[]{Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1])};
    }

    /** Puts the player `d` blocks along the walk, looking along it, as their own movement would: the frames between
     *  ticks show them moving smoothly from where the last tick left them, unless they `jump` there. */
    private static void place(Minecraft mc, double d, boolean jump) {
        var p = point(d); var ahead = point(Math.min(d + LOOK, AT[AT.length - 1]));
        if (ahead[0] != p[0] || ahead[1] != p[1]) yaw += Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(p[0] - ahead[0], ahead[1] - p[1])) - yaw);
        int i = (int) d;
        var player = mc.player;
        player.setPos(corner.getX() + .5 + p[0], Mth.lerp(d - i, feet[i], feet[i + 1]), corner.getZ() + .5 + p[1]);
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(yaw); player.setYHeadRot(yaw); player.setXRot(PITCH);
        if (jump) player.setOldPosAndRot();
    }

    /** CPU nanoseconds used by every thread but the render and server threads: the builders of chunk meshes, grass blades,
     *  Distant Horizons' terrain and maps, and storage. */
    private static long builderCpu() {
        var threads = ManagementFactory.getThreadMXBean(); long sum = 0;
        for (var info : threads.getThreadInfo(threads.getAllThreadIds(), 0))
            if (info != null && !info.getThreadName().equals("Render thread") && !info.getThreadName().equals("Server thread"))
                sum += Math.max(0, threads.getThreadCpuTime(info.getThreadId()));
        return sum;
    }

    // --- the world ---------------------------------------------------------------------------------------------------

    /** Finds the town's place, generates every chunk the walk loads and moves the player to the town, around which
     *  Distant Horizons then generates its terrain. */
    private static void generate(ServerLevel l) {
        var site = site(l);
        int near = NEAR_CHUNKS * 16;
        com.mojang.logging.LogUtils.getLogger().info("Benchmark town at {}; generating the chunks around the walk", site);
        for (int cx = (site.getX() + BOUNDS[0] - near) >> 4; cx <= (site.getX() + BOUNDS[1] + near) >> 4; cx++)
            for (int cz = (site.getZ() + BOUNDS[2] - near) >> 4; cz <= (site.getZ() + BOUNDS[3] + near) >> 4; cz++) l.getChunk(cx, cz);
        corner = site.atY(l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.getX(), site.getZ()) - 1);
        l.setDefaultSpawnPos(corner, 0);
        var player = l.getServer().getPlayerList().getPlayers().getFirst();
        player.teleportTo(l, corner.getX() + .5, corner.getY() + 1, corner.getZ() + .5, Set.of(), 0, 0);
        ready = true;
    }

    /** The town's corner nearest the origin, on a 64-block grid, from which the whole walk crosses only plains. */
    private static BlockPos site(ServerLevel l) {
        var biomes = l.getChunkSource().getGenerator().getBiomeSource(); var climate = l.getChunkSource().randomState().sampler();
        int y = QuartPos.fromBlock(l.getSeaLevel());
        for (int ring = 0; ring <= 64; ring++) for (int i = -ring; i <= ring; i++) for (int j = -ring; j <= ring; j++) {
            if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
            boolean open = true;
            for (int x = BOUNDS[0] - 16; open && x <= BOUNDS[1] + 16; x += 16) for (int z = BOUNDS[2] - 16; open && z <= BOUNDS[3] + 16; z += 16)
                open = biomes.getNoiseBiome(QuartPos.fromBlock(i * 64 + x), y, QuartPos.fromBlock(j * 64 + z), climate).is(Biomes.PLAINS);
            if (open) return new BlockPos(i * 64, 0, j * 64);
        }
        throw new IllegalStateException("No open plains within 4,096 blocks of the origin; choose another benchmark seed");
    }

    /** A clear noon over the town: its lawn, {@link #YARDS} yards of every custom model, fired up, and the walk cleared of
     *  trees, with the walker's feet height along it. */
    private static void build(ServerLevel l) {
        var at = l.getSharedSpawnPos();
        l.setDayTime(6000); l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, l.getServer());
        var weather = RegionalWeather.get(l);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            var district = weather.district(l, RegionalWeather.key(at.getX() + dx * 512, at.getZ() + dz * 512)); district.rain = false; district.end = weather.clock + 720000;
        }
        weather.setDirty();
        // Placed without neighbour updates, so cut trees keep their leaves and nothing changes during the walk.
        int quietly = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (int x = LAWN[0]; x <= LAWN[1]; x++) for (int z = LAWN[2]; z <= LAWN[3]; z++) for (int dy = -3; dy <= 40; dy++)
            l.setBlock(at.offset(x, dy, z), (dy < 0 ? Blocks.DIRT : dy == 0 ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState(), quietly);
        for (int i = 0; i < YARDS; i++) yard(l, at.offset(0, 1, i * SPACING));
        int n = (int) Math.ceil(AT[AT.length - 1]) + 2;
        var ground = new float[n];
        for (int i = 0; i < n; i++) {
            var p = point(i); int x = at.getX() + Mth.floor(p[0] + .5), z = at.getZ() + Mth.floor(p[1] + .5);
            for (var column : BlockPos.betweenClosed(x - 3, at.getY() - 16, z - 3, x + 3, at.getY() + 40, z + 3)) {
                var state = l.getBlockState(column);
                if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) l.setBlock(column, Blocks.AIR.defaultBlockState(), quietly);
            }
            ground[i] = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }
        // Feet follow the ground's average over nine blocks, as a walker's head rides over small steps, but never sink below it.
        var smooth = new float[n];
        for (int i = 0; i < n; i++) {
            float sum = 0; int count = 0;
            for (int k = Math.max(0, i - 4); k <= Math.min(n - 1, i + 4); k++) { sum += ground[k]; count++; }
            smooth[i] = Math.max(sum / count, ground[i]);
        }
        var p = l.getServer().getPlayerList().getPlayers().getFirst();
        p.setGameMode(GameType.CREATIVE); p.getAbilities().flying = true; p.onUpdateAbilities(); p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(KilnContent.RETORT.get()));
        corner = at; feet = smooth; ready = true;
    }

    /** One yard: the Oil Derrick, Kiln, Foundry and Fertilizer Furnace built and burning, with the Baking Oven and Cooking
     *  Stove beside them, loaded pallets in front and loaded wheelbarrows among them. */
    private static void yard(ServerLevel l, BlockPos at) {
        machine(l, at.offset(-8, 0, 6), IndustrialContent.PUMP.get().defaultBlockState());
        int x = 0;
        for (var block : List.of(KilnContent.KILN.get(), KilnContent.FOUNDRY.get(), KilnContent.RETORT.get())) {
            var m = machine(l, at.offset(x, 0, 6), block.defaultBlockState().setValue(CivicBlock.FACING, Direction.NORTH));
            if (m instanceof KilnBlockEntity kiln) { kiln.setItem(1, KilnContent.MINERAL_COAL.toStack(8)); CoalFireFixture.light(kiln); }
            x += 6;
        }
        l.setBlockAndUpdate(at.offset(18, 0, 6), BakingOvenContent.OVEN.get().defaultBlockState());
        l.setBlockAndUpdate(at.offset(21, 0, 6), CookingContent.STATION.get().defaultBlockState());
        var goods = List.of(new ItemStack(Items.STONE), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BRICKS), new ItemStack(Items.COBBLESTONE));
        for (int i = 0; i < 8; i++) {
            var pos = at.offset(i * 2, 0, 0);
            l.setBlockAndUpdate(pos, CargoContent.STOCKPILE.get().defaultBlockState());
            ((StockpileEntity) l.getBlockEntity(pos)).cargo().insert(goods.get(i % goods.size()).copy(), 32 + 24 * (i % 4));
        }
        for (int i = 0; i < 4; i++) {
            var barrow = new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(), l);
            barrow.setYRot(i * 37); barrow.setPos(Vec3.atBottomCenterOf(at.offset(1 + i * 4, 0, 3)));
            l.addFreshEntity(barrow); barrow.cargo().insert(goods.get(i % goods.size()).copy(), 24 + 8 * i);
        }
    }

    private static net.minecraft.world.level.block.entity.BlockEntity machine(ServerLevel l, BlockPos at, net.minecraft.world.level.block.state.BlockState state) {
        l.setBlockAndUpdate(at, state);
        MachineFixture.build(l, at);
        if (MachineFixture.status(l, at) != MachineStructure.COMPLETE) throw new IllegalStateException("Incomplete " + state.getBlock());
        return l.getBlockEntity(at);
    }
}
