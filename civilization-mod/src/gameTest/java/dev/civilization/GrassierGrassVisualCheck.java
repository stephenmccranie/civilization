package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

/** Actual optional-mod transformation and altitude/snow-rule check in the guarded disposable world. */
final class GrassierGrassVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;

    static void tick(Minecraft mc) {
        if (failure != null) throw new IllegalStateException("Grassier Grass fixture", failure);
        ticks++;
        var server = mc.getSingleplayerServer();
        if (ticks == 100) {
            checkHook();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            mc.options.fov().set(60);
            server.execute(() -> {
                try {
                    var level = server.overworld();
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    level.setDayTime(6000);
                    level.setWeatherParameters(100000, 0, false, false);
                    var weather = RegionalWeather.get(level);
                    var district = weather.district(level, RegionalWeather.key(0, 0));
                    district.rain = false;
                    district.end = weather.clock + 72000;
                    weather.setDirty();
                    for (int i = 0; i < 2; i++) {
                        int x0 = i * 32, y = i == 0 ? 80 : 240;
                        level.getChunk(x0 >> 4, 0);
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                                "fillbiome " + x0 + " " + (y - 4) + " 0 " + (x0 + 15) + " " + (y + 4) + " 15 minecraft:taiga");
                        for (int x = x0 + 2; x <= x0 + 13; x++) for (int z = 2; z <= 13; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.GRASS_BLOCK.defaultBlockState());
                            for (int dy = 1; dy <= 8; dy++)
                                level.setBlockAndUpdate(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState());
                        }
                        var pos = new BlockPos(x0 + 8, y, 8);
                        var expected = i == 0 ? Biome.Precipitation.RAIN : Biome.Precipitation.SNOW;
                        if (level.getBiome(pos).value().getPrecipitationAt(pos) != expected)
                            throw new IllegalStateException("Altitude fixture does not cross the snow threshold");
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.CREATIVE);
                    player.getInventory().clearContent();
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, 8, 83, 0, java.util.Set.of(), 0, 30);
                    ready = true;
                } catch (Throwable t) { failure = t; }
            });
        }
        if (ticks > 100 && !ready) { ticks = 101; return; }
        if (ticks == 180) shot(mc, "low");
        if (ticks == 185) server.execute(() -> server.getPlayerList().getPlayers().getFirst()
                .teleportTo(server.overworld(), 40, 243, 0, java.util.Set.of(), 0, 30));
        if (ticks == 280) {
            var pos = new BlockPos(40, 240, 8);
            if (mc.level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.SNOW)
                throw new IllegalStateException("Client snow biome did not synchronize");
            shot(mc, "high");
        }
        if (ticks == 285 && net.neoforged.fml.ModList.get().isLoaded("iris")) {
            try {
                var iris = Class.forName("net.irisshaders.iris.Iris");
                var config = iris.getMethod("getIrisConfig").invoke(null);
                config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, false);
                config.getClass().getMethod("save").invoke(config);
                iris.getMethod("reload").invoke(null);
            } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        }
        if (ticks == 360) {
            shot(mc, "high-no-shaders");
            System.out.println("GRASSIER_GRASS_ALTITUDE_PASS low=80/rain high=240/snow clear-weather");
        }
        if (ticks > 370) mc.stop();
    }

    private static void checkHook() {
        if (!net.neoforged.fml.ModList.get().isLoaded("grassiergrass")) {
            System.out.println("GRASSIER_GRASS_ABSENT_PASS");
            return;
        }
        try {
            var type = Class.forName("com.leonardoinc22.shortgrass.client.render.GrassSectionBuilder");
            var method = type.getDeclaredMethod("snowBlend", boolean.class, float.class, float.class, float.class);
            method.setAccessible(true);
            for (boolean snowy : new boolean[]{false, true})
                for (float length : new float[]{0.1F, 0.58F, 1.3F})
                    for (float noise : new float[]{0, 0.5F, 1})
                        for (float height : new float[]{0, 0.5F, 1})
                            if ((float) method.invoke(null, snowy, length, noise, height) != 0)
                                throw new IllegalStateException("Actual transformed snowBlend still whitens blades");
            System.out.println("GRASSIER_GRASS_HOOK_PASS 54 snow/length/noise/height combinations");
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "grassier-grass-" + name + ".png", mc.getMainRenderTarget(),
                message -> com.mojang.logging.LogUtils.getLogger().info("{}", message.getString()));
    }
}
