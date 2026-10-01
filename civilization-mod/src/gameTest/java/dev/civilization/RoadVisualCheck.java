package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Disposable broad-road and close-paver review under the active resource pack/shader. */
final class RoadVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;

    static void tick(Minecraft mc) {
        ticks++;
        var server = mc.getSingleplayerServer();
        if (failure != null) throw new IllegalStateException("Road visual fixture", failure);
        if (ticks == 100) {
            mc.options.pauseOnLostFocus = false;
            mc.options.fov().set(55);
            mc.options.hideGui = true;
            server.execute(() -> {
                try {
                    var level = server.overworld();
                    level.setDayTime(6000);
                    level.setWeatherParameters(100000, 0, false, false);
                    for (int x = -3; x <= 15; x++) for (int z = -8; z <= 15; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE_BRICKS.defaultBlockState());
                        for (int y = 100; y <= 110; y++)
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                    for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 99, z), RoadContent.PAVERS.get().defaultBlockState());
                    for (int x = 12; x < 15; x++) for (int z = 0; z < 5; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.BRICKS.defaultBlockState());
                    for (int x = 1; x <= 4; x++)
                        level.setBlockAndUpdate(new BlockPos(x, 100, 12), RoadContent.SLAB.get().defaultBlockState());
                    for (int x = 7; x <= 10; x++)
                        level.setBlockAndUpdate(new BlockPos(x, 100, 12), RoadContent.STAIRS.get().defaultBlockState());
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.CREATIVE);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.getInventory().setItem(0, RoadContent.PAVERS_ITEM.toStack());
                    player.getInventory().setItem(1, RoadContent.SLAB_ITEM.toStack());
                    player.getInventory().setItem(2, RoadContent.STAIRS_ITEM.toStack());
                    player.teleportTo(level, 6, 106, -7, java.util.Set.of(), 0, 30);
                    ready = true;
                } catch (Throwable t) { failure = t; }
            });
        }
        if (ticks > 100 && !ready) { ticks = 101; return; }
        if (ticks == 160) shot(mc, "wide");
        if (ticks == 165) server.execute(() -> server.getPlayerList().getPlayers().getFirst()
                .teleportTo(server.overworld(), 6, 104, -1, java.util.Set.of(), 0, 55));
        if (ticks == 200) shot(mc, "close");
        if (ticks == 205) server.execute(() -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        });
        if (ticks == 220) {
            mc.options.hideGui = false;
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }
        if (ticks == 240) shot(mc, "inventory");
        if (ticks > 250) mc.stop();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "road-" + name + ".png", mc.getMainRenderTarget(),
                message -> com.mojang.logging.LogUtils.getLogger().info("{}", message.getString()));
    }
}
