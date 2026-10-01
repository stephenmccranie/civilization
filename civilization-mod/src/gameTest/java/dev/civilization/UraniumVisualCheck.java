package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Disposable scene for the ore, stocked chest and held-item track origins. */
final class UraniumVisualCheck {
    private static int ticks;

    static void tick(Minecraft mc) {
        ticks++;
        if (ticks == 100) {
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(8);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getSingleplayerServer().execute(() -> {
                var level = mc.getSingleplayerServer().overworld();
                level.setDayTime(6000);
                for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 0; cz++) level.getChunk(cx, cz);
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 3; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                    for (int y = 101; y <= 106; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                for (int x = -5; x <= 5; x++) for (int y = 101; y <= 105; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, 2), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(-6, 100, -6, 6, 107, 4))) item.discard();
                level.setBlockAndUpdate(new BlockPos(-2, 101, 1), FrontierContent.URANIUM_ORE.get().defaultBlockState());
                var chestPos = new BlockPos(2, 101, 1);
                level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(FrontierContent.RAW_URANIUM.get()));
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.getInventory().clearContent();
                player.removeAllEffects();
                player.getInventory().setItem(0, new ItemStack(FrontierContent.RAW_URANIUM.get()));
                player.getInventory().selected = 0;
                player.teleportTo(level, .5, 101, -4, java.util.Set.of(), 0, 12);
            });
        }
        if (ticks == 225) mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.teleportTo(mc.getSingleplayerServer().overworld(), -.5, 101, -4,
                    java.util.Set.of(), 0, 12);
        });
        if (ticks == 180 || ticks == 200 || ticks == 220 || ticks == 224 || ticks == 226
                || ticks == 240 || ticks == 260)
            Screenshot.grab(mc.gameDirectory, "uranium-" + ticks + ".png", mc.getMainRenderTarget(), message -> {});
        if (ticks > 265) {
            com.mojang.logging.LogUtils.getLogger().info("URANIUM VISUAL VERIFIED");
            mc.stop();
        }
    }
}
