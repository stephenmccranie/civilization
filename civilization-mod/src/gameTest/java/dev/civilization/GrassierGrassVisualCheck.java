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
    private static float wildHeight;
    private static int meshWait;
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
                        var fixtureChunk = level.getChunk(x0 >> 4, 0);
                        fixtureChunk.getData(GrassWearChunk.TYPE).wear.load(new long[0]);
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(level, fixtureChunk.getPos(), GrassWearSystem.snapshot(fixtureChunk));
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                                "fillbiome " + x0 + " " + (y - 4) + " 0 " + (x0 + 15) + " " + (y + 4) + " 15 minecraft:taiga");
                        for (int x = x0 + 2; x <= x0 + 13; x++) for (int z = 2; z <= 13; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.GRASS_BLOCK.defaultBlockState());
                            for (int dy = 1; dy <= 16; dy++)
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
        // The fixture writes a platform into a section initially cached as empty. Mark it after
        // native chunk/block synchronization, before measuring subsequent wear-only rebuilds.
        if (ticks == 160 && net.neoforged.fml.ModList.get().isLoaded("grassiergrass"))
            try { dev.civilization.client.GrassWearClient.invalidateSection(net.minecraft.core.SectionPos.asLong(0,5,0)); }
            catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        if (ticks == 180 && net.neoforged.fml.ModList.get().isLoaded("grassiergrass") && !hasFixtureMesh()) {
            if (++meshWait > 160) { shot(mc, "missing-mesh"); throw new IllegalStateException("No fixture mesh after eight seconds"); }
            ticks--; return;
        }
        if (ticks == 180) { wildHeight = meshHeight(8.25F, 8.75F); shot(mc, "low"); }
        if (ticks == 185) wear(mc, 8);
        if (ticks == 245) checkWear(0.8F);
        if (ticks == 250) wear(mc, 24);
        if (ticks == 310) {
            checkWear(0.2F);
            if (net.neoforged.fml.ModList.get().isLoaded("grassiergrass")) {
                float worn = meshHeight(8.25F, 8.75F), untouched = meshHeight(4.25F, 5.75F);
                if (wildHeight < 1.4F || wildHeight - worn < .3F || worn > 1.35F || untouched < 1.4F)
                    throw new IllegalStateException("Grass mesh height mismatch wild=" + wildHeight + " worn=" + worn + " untouched=" + untouched);
                System.out.println("GRASS_WEAR_MESH_PASS wild=" + wildHeight + " worn=" + worn + " untouched=" + untouched);
            }
            shot(mc, "low-worn");
        }
        if (ticks == 315) server.execute(() -> server.getPlayerList().getPlayers().getFirst()
                .teleportTo(server.overworld(), 40, 243, 0, java.util.Set.of(), 0, 30));
        if (ticks == 410) {
            var pos = new BlockPos(40, 240, 8);
            if (mc.level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.SNOW)
                throw new IllegalStateException("Client snow biome did not synchronize");
            shot(mc, "high");
        }
        if (ticks == 415 && net.neoforged.fml.ModList.get().isLoaded("iris")) {
            try {
                var iris = Class.forName("net.irisshaders.iris.Iris");
                var config = iris.getMethod("getIrisConfig").invoke(null);
                config.getClass().getMethod("setShadersEnabled", boolean.class).invoke(config, false);
                config.getClass().getMethod("save").invoke(config);
                iris.getMethod("reload").invoke(null);
            } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        }
        if (ticks == 490) {
            shot(mc, "high-no-shaders");
            System.out.println("GRASSIER_GRASS_ALTITUDE_PASS low=80/rain high=240/snow clear-weather");
        }
        if (ticks > 500) mc.stop();
    }

    private static void wear(Minecraft mc, int steps) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            for (int x = 7; x <= 8; x++) for (int z = 2; z <= 13; z++)
                for (int i = 0; i < steps; i++) GrassWearSystem.record(server.overworld(), new BlockPos(x, 80, z));
        });
    }

    private static void checkWear(float scale) {
        float actual = dev.civilization.client.GrassWearClient.heightScale(8, 80, 8);
        if (Math.abs(actual - scale) > .001F) throw new IllegalStateException("Grass wear did not sync: " + actual + " expected=" + scale);
    }

    private static boolean hasFixtureMesh() {
        try {
            var cache = Class.forName("com.leonardoinc22.shortgrass.client.render.GrassSectionCache");
            var method = cache.getDeclaredMethod("meshes"); method.setAccessible(true);
            var map = (it.unimi.dsi.fastutil.longs.Long2ObjectMap<?>) method.invoke(null);
            if (meshWait == 0) {
                var mc = Minecraft.getInstance();
                System.out.println("GRASS_FIXTURE_DIAGNOSTIC position=" + mc.player.position() + " ground=" + mc.level.getBlockState(new BlockPos(8,80,8)) + " cachedSections=" + map.size());
            }
            return map.containsKey(net.minecraft.core.SectionPos.asLong(0,5,0));
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private static float meshHeight(float minX, float maxX) {
        if (!net.neoforged.fml.ModList.get().isLoaded("grassiergrass")) return 0;
        try {
            var cache = Class.forName("com.leonardoinc22.shortgrass.client.render.GrassSectionCache");
            var meshes = cache.getDeclaredMethod("meshes"); meshes.setAccessible(true);
            var map = (it.unimi.dsi.fastutil.longs.Long2ObjectMap<?>) meshes.invoke(null);
            Object mesh = map.get(net.minecraft.core.SectionPos.asLong(0, 5, 0));
            if (mesh == null) throw new IllegalStateException("No cached grass mesh in fixture");
            var bytesField = mesh.getClass().getDeclaredField("vertexBytes"); bytesField.setAccessible(true);
            var strideField = mesh.getClass().getDeclaredField("vertexStride"); strideField.setAccessible(true);
            var bytes = ((java.nio.ByteBuffer) bytesField.get(mesh)).duplicate().order(java.nio.ByteOrder.nativeOrder());
            int stride = strideField.getInt(mesh); float highest = Float.NEGATIVE_INFINITY;
            for (int offset = 0; offset + 12 <= bytes.limit(); offset += stride) {
                float x = bytes.getFloat(offset), y = bytes.getFloat(offset + 4), z = bytes.getFloat(offset + 8);
                if (x >= minX && x <= maxX && z >= 4.25F && z <= 10.75F) highest = Math.max(highest, y);
            }
            return highest;
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
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
