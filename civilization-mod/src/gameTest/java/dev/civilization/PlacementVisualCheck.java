package dev.civilization;

import dev.civilization.client.PlacementPlan;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Held controllers and multi-cell blocks preview their placement; the player's own right-click then places exactly the
 * previewed cells, or nothing. Survival carries building blocks in the offhand, so most stages hold the item there. */
final class PlacementVisualCheck {
    /** After `setup`, hold `item` in `hand` and aim at the top of `ground` from `eye` (relative to that face's center). */
    private record Stage(String name, ItemLike item, InteractionHand hand, BlockPos ground, Vec3 eye, Direction facing, boolean valid, Consumer<ServerLevel> setup) {}
    private static final int STAGE_TICKS = 45;
    private static final Consumer<ServerLevel> NOTHING = level -> {};
    private static final List<Stage> STAGES = List.of(
            new Stage("kiln", KilnContent.KILN, InteractionHand.MAIN_HAND, new BlockPos(0, 100, 0), new Vec3(2.8, 2.2, -3.2), Direction.NORTH, true, NOTHING),
            new Stage("smithy-west", WorkshopContent.SMITHY, InteractionHand.OFF_HAND, new BlockPos(10, 100, 0), new Vec3(-3.6, 2.2, 2.2), Direction.WEST, true, NOTHING),
            new Stage("bench", WorkshopContent.BENCH, InteractionHand.OFF_HAND, new BlockPos(-8, 100, 0), new Vec3(1.2, 2.0, 3.6), Direction.SOUTH, true, NOTHING),
            new Stage("bench-blocked", WorkshopContent.BENCH, InteractionHand.OFF_HAND, new BlockPos(-8, 100, -5), new Vec3(1.2, 2.0, 3.6), Direction.SOUTH, false, dirt(new BlockPos(-9, 101, -5))),
            // The controller fits, but dirt fills its chamber and a front wall cell: those parts turn red.
            new Stage("kiln-obstructed", KilnContent.KILN, InteractionHand.OFF_HAND, new BlockPos(18, 100, 0), new Vec3(2.8, 2.2, -3.2), Direction.NORTH, true, dirt(new BlockPos(18, 102, 1), new BlockPos(19, 101, 0))),
            // A shell built first leaves only the controller's own ghost, facing the player in its gap.
            new Stage("drill-into-shell", IndustrialContent.DRILL, InteractionHand.OFF_HAND, new BlockPos(-8, 100, 7), new Vec3(0, 1.7, -3.2), Direction.NORTH, true, shell(IndustrialContent.DRILL.get(), new BlockPos(-8, 101, 7))),
            new Stage("oven", BakingOvenContent.OVEN, InteractionHand.OFF_HAND, new BlockPos(24, 100, 0), new Vec3(2.6, 2.2, -3.2), Direction.NORTH, true, NOTHING),
            new Stage("derrick", IndustrialContent.PUMP, InteractionHand.OFF_HAND, new BlockPos(38, 100, 0), new Vec3(0, 2.6, -3.6), Direction.NORTH, true, NOTHING),
            new Stage("fertilizer-furnace", KilnContent.RETORT, InteractionHand.OFF_HAND, new BlockPos(48, 100, 0), new Vec3(2.8, 2.2, -3.2), Direction.NORTH, true, NOTHING));
    private static int ticks;
    private static volatile String failure;
    private static volatile boolean ready;
    private static PlacementPlan shown;

    private static Consumer<ServerLevel> dirt(BlockPos... cells) {
        return level -> { for (var cell : cells) level.setBlockAndUpdate(cell, Blocks.DIRT.defaultBlockState()); };
    }
    private static Consumer<ServerLevel> shell(net.minecraft.world.level.block.Block block, BlockPos controller) {
        var state = block.defaultBlockState().setValue(CivicBlock.FACING, Direction.NORTH);
        return level -> { for (var part : MachineStructure.guideParts(state)) MachineStructure.placePart(level, controller, Direction.NORTH, part); };
    }

    static void tick(Minecraft mc) {
        ticks++;
        if (failure != null) throw new IllegalStateException(failure);
        var server = mc.getSingleplayerServer();
        if (ticks == 100) {
            mc.options.pauseOnLostFocus = false; mc.options.hideGui = false; mc.options.fov().set(70); mc.options.guiScale().set(3);
            mc.options.renderDistance().set(8); mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            PreviewConfig.MODE.set(PreviewConfig.Mode.TEXTURED);
            check(mc, () -> {
                var level = server.overworld();
                level.setDayTime(6000); level.setWeatherParameters(100000, 0, false, false);
                for (int x = -14; x <= 52; x++) for (int z = -8; z <= 14; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                    for (int y = 101; y <= 133; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                player.closeContainer(); player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true; player.onUpdateAbilities();
                ready = true;
            });
        }
        if (ticks <= 100 || !ready) { if (ticks > 100) ticks = 101; return; }
        if (ticks < 160) return; // Let the rebuilt platform reach the client.
        int stage = (ticks - 160) / STAGE_TICKS, at = (ticks - 160) % STAGE_TICKS;
        if (stage == STAGES.size()) {
            com.mojang.logging.LogUtils.getLogger().info("PLACEMENT PREVIEW VERIFIED: {} held placements matched real right-clicks", STAGES.size());
            mc.stop(); return;
        }
        if (stage > STAGES.size()) return;
        var current = STAGES.get(stage);
        if (at == 0) mc.gui.getChat().clearMessages(false);
        if (at == 0) check(mc, () -> {
            var level = server.overworld();
            current.setup().accept(level);
            var target = Vec3.atCenterOf(current.ground()).add(0, .5, 0);
            var eye = target.add(current.eye());
            var look = target.subtract(eye);
            float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z)), pitch = (float) Math.toDegrees(-Math.atan2(look.y, look.horizontalDistance()));
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();
            player.setItemInHand(current.hand(), new ItemStack(current.item()));
            player.teleportTo(level, eye.x, eye.y - player.getEyeHeight(), eye.z, java.util.Set.of(), yaw, pitch);
        });
        if (at == 30) {
            if (!(mc.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(current.ground()) || hit.getDirection() != Direction.UP)
                throw new IllegalStateException(current.name() + ": camera must aim at the top of " + current.ground() + ", not " + describe(mc.hitResult)
                        + " from " + mc.player.getEyePosition() + " yaw " + mc.player.getYRot() + " pitch " + mc.player.getXRot());
            shown = PlacementPlan.of(mc);
            if (shown == null || shown.valid() != current.valid() || !shown.at().equals(current.ground().above())
                    || shown.state().getValue(BlockStateProperties.HORIZONTAL_FACING) != current.facing())
                throw new IllegalStateException(current.name() + ": unexpected preview " + shown);
            Screenshot.grab(mc.gameDirectory, "placement-" + current.name() + ".png", mc.getMainRenderTarget(), message -> {});
        }
        if (at == 33) KeyMapping.click(mc.options.keyUse.getKey()); // The player's own right-click, in vanilla's hand order.
        if (at == 42) {
            var plan = shown;
            check(mc, () -> {
                var level = server.overworld();
                for (var cell : plan.cells()) {
                    var placed = level.getBlockState(cell.pos());
                    boolean ok = plan.valid() ? placed.equals(cell.state()) : !placed.is(cell.state().getBlock());
                    if (!ok) throw new IllegalStateException(current.name() + ": preview showed " + cell + " (valid=" + plan.valid() + ") but the click left " + placed);
                }
            });
        }
    }
    private static String describe(HitResult hit) {
        return hit instanceof BlockHitResult block ? block.getType() + " " + block.getBlockPos() + " " + block.getDirection() + " at " + block.getLocation() : String.valueOf(hit);
    }
    private static void check(Minecraft mc, Runnable action) {
        mc.getSingleplayerServer().execute(() -> { try { action.run(); } catch (Throwable e) { failure = e.toString(); } });
    }
}
