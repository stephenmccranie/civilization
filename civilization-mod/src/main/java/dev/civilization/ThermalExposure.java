package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Mean radiant temperature at a body: the surfaces it sees along fixed sight lines, plus the flames and lava in view.
 * Reads only loaded cells around the body and stores nothing; being seen never changes the heat field.
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

    private ThermalExposure() {}

    private static double emission(double celsius) {
        double kelvin = celsius + KELVIN;
        return SIGMA * kelvin * kelvin * kelvin * kelvin;
    }

    /** Mean radiant temperature in °C, or {@code air} where the surroundings are not all loaded. */
    static double radiant(ServerLevel level, ThermalField field, Vec3 torso, double air) {
        double reach = ThermalRules.RADIANT_REACH;
        if (!Chunks.loadedAround(level, torso, reach)) return air;
        double power = 0;
        for (var direction : SIGHT) power += emission(seen(level, field, torso, torso.add(direction.scale(reach)))) / SIGHT.length;
        int emitters = 0;
        for (long key : BlockHeatSources.near(level, torso, reach)) {
            if (emitters == MAX_EMITTERS) break;
            var pos = BlockPos.of(key);
            var state = level.getBlockState(pos);
            double radiance = BlockHeatSources.radiance(state);
            if (radiance == 0) continue;
            boolean lava = state.getFluidState().is(FluidTags.LAVA);
            // A lava pool shines from its surface; lava beneath more lava is hidden.
            if (lava && level.getFluidState(pos.above()).is(FluidTags.LAVA)) continue;
            var from = lava ? new Vec3(pos.getX() + .5, pos.getY() + 1.01, pos.getZ() + .5) : Vec3.atCenterOf(pos);
            emitters++;
            var sight = level.clip(new ClipContext(from, torso, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (sight.getType() != HitResult.Type.MISS) continue;
            // Flux softened over about a block, since a fire is not a point; a body absorbs a quarter of it per area.
            power += radiance / (4 * Math.PI * (from.distanceToSqr(torso) + 1)) / 4;
        }
        return Math.min(Math.pow(power / SIGMA, .25) - KELVIN, ThermalRules.MAX_RADIANT_C);
    }

    /** Temperature of the first surface on the line, or of the air where the line ends. */
    private static double seen(ServerLevel level, ThermalField field, Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, field, (f, pos) -> {
            var state = level.getBlockState(pos);
            if (state.isAir() || state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty()) return null;
            // Flames and lava radiate only as emitters, so a sight line meeting one counts it as ambient.
            return BlockHeatSources.source(state) ? ThermalField.ambient(level, pos) : f.localTemperature(level, pos);
        }, f -> f.localTemperature(level, BlockPos.containing(to)));
    }
}
