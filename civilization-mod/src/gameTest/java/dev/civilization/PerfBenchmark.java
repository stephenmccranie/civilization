package dev.civilization;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The performance benchmark: a yard of every custom model, repeated into a small town, seen from fixed viewpoints in
 * the player pack at 2560x1440. Each viewpoint is measured twice: uncapped, for what each part of the frame costs on the
 * CPU and GPU, with a Java Flight Recorder profile of every thread; and capped at the 120 fps target, for the frame
 * pacing players get. {@link PerfReport} writes the results to runs/perf/report.
 */
final class PerfBenchmark {
    private static final int Y = 100, X = 300, Z = 300, BLOCKS = 3, SPACING = 18;
    /** Seconds to settle after a move, measure uncapped and measure at the target. */
    private static final int SETTLE = 8, FIRST_SETTLE = 40, MEASURE = 12;
    static final int TARGET_FPS = 120;

    private record View(String name, Vec3 eye, float yaw, float pitch) {}
    private static final List<View> VIEWS = List.of(
            new View("landscape", new Vec3(X + 14, Y + 18, Z - 20), 180, 8),
            new View("yard", new Vec3(X + 10, Y + 5, Z - 14), 0, 12),
            new View("town", new Vec3(X + 10, Y + 16, Z - 26), 0, 24));

    private enum Phase { SETUP, MOVE, SETTLE, COST, PACING, DONE }
    private static Phase phase = Phase.SETUP;
    private static int ticks, view, phaseStart;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static jdk.jfr.Recording recording;
    private static final List<PerfReport.Pass> passes = new ArrayList<>();

    private PerfBenchmark() {}

    static void tick(Minecraft mc) {
        ticks++;
        var server = mc.getSingleplayerServer();
        if (failure != null) throw new IllegalStateException("Performance scene", failure);
        switch (phase) {
            case SETUP -> {
                if (ticks != 100) return;
                mc.options.pauseOnLostFocus = false; mc.options.hideGui = false;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                server.execute(() -> { try { build(server.overworld()); ready = true; } catch (Throwable t) { failure = t; } });
                next(Phase.MOVE);
            }
            case MOVE -> {
                if (!ready) return;
                var v = VIEWS.get(view);
                server.execute(() -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    p.teleportTo(server.overworld(), v.eye().x, v.eye().y - p.getEyeHeight(), v.eye().z, Set.of(), v.yaw(), v.pitch());
                });
                limit(mc, 260);
                next(Phase.SETTLE);
            }
            case SETTLE -> {
                int settle = view == 0 ? FIRST_SETTLE : SETTLE;
                if (elapsed() < settle * 20 || !mc.levelRenderer.hasRenderedAllSections() && elapsed() < settle * 60) return;
                recording = PerfReport.startProfile();
                PerfRecorder.INSTANCE.begin(VIEWS.get(view).name(), true);
                next(Phase.COST);
            }
            case COST -> {
                if (elapsed() < MEASURE * 20) return;
                var window = PerfRecorder.INSTANCE.end();
                var profile = PerfReport.stopProfile(recording, mc, window.name());
                Screenshot.grab(mc.gameDirectory, "perf-" + window.name() + ".png", mc.getMainRenderTarget(), message -> {});
                passes.add(new PerfReport.Pass(window, false, profile));
                limit(mc, TARGET_FPS);
                PerfRecorder.INSTANCE.begin(VIEWS.get(view).name(), false);
                next(Phase.PACING);
            }
            case PACING -> {
                if (elapsed() < MEASURE * 20) return;
                passes.add(new PerfReport.Pass(PerfRecorder.INSTANCE.end(), true, null));
                view++;
                next(view < VIEWS.size() ? Phase.MOVE : Phase.DONE);
            }
            case DONE -> {
                try { PerfReport.write(mc, passes); } catch (Exception e) { throw new IllegalStateException("Performance report", e); }
                com.mojang.logging.LogUtils.getLogger().info("PERFORMANCE MEASURED: {} passes written to runs/perf/report", passes.size());
                mc.stop();
            }
        }
    }

    private static int elapsed() { return ticks - phaseStart; }
    private static void next(Phase p) { phase = p; phaseStart = ticks; }

    /** The frame-rate cap: 260 is Minecraft's unlimited; vsync stays off so the cap alone paces frames. */
    private static void limit(Minecraft mc, int fps) {
        mc.options.enableVsync().set(false); mc.getWindow().updateVsync(false);
        mc.options.framerateLimit().set(fps); mc.getWindow().setFramerateLimit(fps);
    }

    /** A clear noon over a stone platform holding {@link #BLOCKS} yards of every custom model, fired up. */
    private static void build(ServerLevel l) {
        l.setDayTime(6000); l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, l.getServer());
        var weather = RegionalWeather.get(l);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            var district = weather.district(l, RegionalWeather.key(X + dx * 512, Z + dz * 512)); district.rain = false; district.end = weather.clock + 720000;
        }
        weather.setDirty();
        for (int x = X - 16; x <= X + 40; x++) for (int z = Z - 30; z <= Z + BLOCKS * SPACING + 4; z++) {
            l.setBlockAndUpdate(new BlockPos(x, Y, z), Blocks.STONE_BRICKS.defaultBlockState());
            for (int y = Y + 1; y <= Y + 14; y++) l.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        for (int i = 0; i < BLOCKS; i++) yard(l, new BlockPos(X, Y + 1, Z + i * SPACING));
        var p = l.getServer().getPlayerList().getPlayers().getFirst();
        p.setGameMode(GameType.CREATIVE); p.getAbilities().flying = true; p.onUpdateAbilities(); p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(KilnContent.RETORT.get()));
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
