package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Mean radiant temperature at a body: the surfaces and sky it sees along fixed sight lines, the flames and lava in
 * view, and direct sun unless blocks or cloud stand in the way. Reads only loaded cells and stores nothing; being
 * seen never changes the heat field.
 */
final class ThermalExposure {
    private static final double SIGMA = 5.670e-8, KELVIN = 273.15;
    private static final int MAX_EMITTERS = 64;
    private static final Vec3[] SIGHT = new Vec3[32];
    static {
        // Evenly spread directions on a sphere, each standing for an equal share of the view.
        double turn = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < SIGHT.length; i++) {
            double y = 1 - (i + .5) * 2 / SIGHT.length, r = Math.sqrt(1 - y * y);
            SIGHT[i] = new Vec3(Math.cos(turn * i) * r, y, Math.sin(turn * i) * r);
        }
    }

    /** Where the sun is and how much of the sky is cloud, from 0 (clear) to 1 (overcast). */
    record Sky(Vec3 sun, double cloud) {
        static Sky at(ServerLevel level, BlockPos pos) {
            return new Sky(ThermalRules.sun(level.getTimeOfDay(1)), RegionalWeather.cooling(level, pos));
        }
    }

    /** Mean radiant temperature in °C, and the share of full clear-sky direct sun reaching the body. */
    record Exposure(double radiant, double sunlight) {}

    private ThermalExposure() {}

    private static double emission(double celsius) {
        double kelvin = celsius + KELVIN;
        return SIGMA * kelvin * kelvin * kelvin * kelvin;
    }

    /** What the body sees, or the air alone where its surroundings are not all loaded. */
    static Exposure sample(ServerLevel level, ThermalField field, Vec3 torso, double air, Sky sky) {
        double reach = ThermalRules.RADIANT_REACH;
        if (!Chunks.loadedAround(level, torso, reach)) return new Exposure(air, 0);
        // A clear sky is far colder than the air and glows with scattered sunlight by day; cloud radiates near air.
        double kelvin = air + KELVIN, clear = .0552 * Math.pow(kelvin, 1.5) - KELVIN;
        double skyPower = (1 - sky.cloud) * (emission(clear) + ThermalRules.SKY_DIFFUSE * Math.max(0, sky.sun.y))
                + sky.cloud * emission(air);
        double power = 0;
        for (var direction : SIGHT) power += seen(level, field, torso, direction, skyPower) / SIGHT.length;
        power += emitters(level, torso);
        double sunlight = sunlit(level, torso, sky.sun) ? 1 - sky.cloud : 0;
        power += ThermalRules.SUN_ABSORBED * ThermalRules.directSun(sky.sun.y) * sunlight;
        return new Exposure(Math.min(Math.pow(power / SIGMA, .25) - KELVIN, ThermalRules.MAX_RADIANT_C), sunlight);
    }

    private static boolean opaque(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty());
    }

    /** Radiant power along one sight line: the first surface it meets, else the sky or the air where it ends. */
    private static double seen(ServerLevel level, ThermalField field, Vec3 torso, Vec3 direction, double skyPower) {
        var end = torso.add(direction.scale(ThermalRules.RADIANT_REACH));
        return BlockGetter.traverseBlocks(torso, end, field, (f, pos) -> {
            var state = level.getBlockState(pos);
            if (!opaque(level, pos, state)) return null;
            // Flames and lava radiate only as emitters, so a sight line meeting one counts it as ambient.
            return emission(BlockHeatSources.source(state) ? ThermalField.ambient(level, pos) : f.localTemperature(level, pos));
        }, f -> {
            var at = BlockPos.containing(end);
            boolean sky = direction.y > 0 && at.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
            return sky ? skyPower : emission(f.localTemperature(level, at));
        });
    }

    /** Absorbed flux from the flames and lava surfaces with a clear line to the torso, nearest first. */
    private static double emitters(ServerLevel level, Vec3 torso) {
        double power = 0;
        int seen = 0;
        for (long key : BlockHeatSources.near(level, torso, ThermalRules.RADIANT_REACH)) {
            if (seen == MAX_EMITTERS) break;
            var pos = BlockPos.of(key);
            var state = level.getBlockState(pos);
            double radiance = BlockHeatSources.radiance(state);
            if (radiance == 0) continue;
            boolean lava = state.getFluidState().is(FluidTags.LAVA);
            // A lava pool shines from its surface; lava beneath more lava is hidden.
            if (lava && level.getFluidState(pos.above()).is(FluidTags.LAVA)) continue;
            var from = lava ? new Vec3(pos.getX() + .5, pos.getY() + 1.01, pos.getZ() + .5) : Vec3.atCenterOf(pos);
            seen++;
            var sight = level.clip(new ClipContext(from, torso, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (sight.getType() != HitResult.Type.MISS) continue;
            // Flux softened over about a block, since a fire is not a point; a body absorbs a quarter of it per area.
            power += radiance / (4 * Math.PI * (from.distanceToSqr(torso) + 1)) / 4;
        }
        return power;
    }

    /** Whether the sun is up and no block stands between it and the torso; unloaded ground counts as open. */
    private static boolean sunlit(ServerLevel level, Vec3 torso, Vec3 sun) {
        if (sun.y < ThermalRules.MIN_SUN_HEIGHT) return false;
        return BlockGetter.traverseBlocks(torso, torso.add(sun.scale(ThermalRules.SUN_REACH)), level, (l, pos) -> {
            if (pos.getY() >= l.getMaxBuildHeight() || !Chunks.loaded(l, pos)) return true;
            return opaque(l, pos, l.getBlockState(pos)) ? false : null;
        }, l -> true);
    }
}
