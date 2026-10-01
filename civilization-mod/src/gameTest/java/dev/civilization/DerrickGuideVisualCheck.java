package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Focused full-blueprint guide check, without the long industry tour. */
final class DerrickGuideVisualCheck {
    private static int ticks;

    static void tick(Minecraft mc) {
        ticks++;
        if (ticks == 100) {
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(8);
            mc.options.hideGui = false;
            PreviewConfig.MODE.set(PreviewConfig.Mode.TEXTURED);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getSingleplayerServer().execute(() -> {
                var level = mc.getSingleplayerServer().overworld();
                level.setDayTime(6000);
                for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) level.getChunk(cx, cz);
                for (int x = -7; x <= 7; x++) for (int z = -7; z <= 12; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE_BRICKS.defaultBlockState());
                    for (int y = 101; y <= 136; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(0, 101, 0), IndustrialContent.PUMP.get().defaultBlockState());
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                player.getInventory().clearContent();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(level, .5, 101, -3.5, java.util.Set.of(), 0, 18);
            });
        }
        if (ticks == 145 || ticks == 210 || ticks == 270 || ticks == 330) {
            try {
                var type = dev.civilization.client.MachinePreview.class;
                var selected = type.getDeclaredField("selected");
                var ghosts = type.getDeclaredField("ghosts");
                var showGuide = type.getDeclaredField("showGuide");
                selected.setAccessible(true);
                ghosts.setAccessible(true);
                showGuide.setAccessible(true);
                com.mojang.logging.LogUtils.getLogger().info("DERRICK GUIDE STATE tick={} hit={} selected={} ghosts={} shown={}",
                        ticks, mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                                ? hit.getBlockPos() + " " + mc.level.getBlockState(hit.getBlockPos()) : mc.hitResult,
                        selected.get(null), ((java.util.List<?>) ghosts.get(null)).size(), showGuide.get(null));
                if (ticks >= 210) com.mojang.logging.LogUtils.getLogger().info("DERRICK GUIDE FPS tick={} fps={}", ticks, mc.getFps());
                if (ticks >= 270 && (!Boolean.TRUE.equals(showGuide.get(null))
                        || ((java.util.List<?>) ghosts.get(null)).size() < 600))
                    throw new IllegalStateException("Full Oil Derrick guide was not visible");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
        if (ticks == 150) {
            Screenshot.grab(mc.gameDirectory, "derrick-guide-base.png", mc.getMainRenderTarget(), message -> {});
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.teleportTo(mc.getSingleplayerServer().overworld(), .5, 112, -6, java.util.Set.of(), 0, -12);
            });
        }
        if (ticks == 160) mc.options.hideGui = true;
        if (ticks == 220) { mc.options.hideGui = false; PreviewConfig.MODE.set(PreviewConfig.Mode.OUTLINE); }
        if (ticks == 280) PreviewConfig.MODE.set(PreviewConfig.Mode.TEXTURED);
        if (ticks == 310)
            Screenshot.grab(mc.gameDirectory, "derrick-guide-middle.png", mc.getMainRenderTarget(), message -> {});
        if (ticks > 335) {
            com.mojang.logging.LogUtils.getLogger().info("DERRICK GUIDE VERIFIED");
            mc.stop();
        }
    }
}
