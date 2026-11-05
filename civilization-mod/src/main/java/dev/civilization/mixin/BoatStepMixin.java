package dev.civilization.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A floating boat's top is only a fifth of a block above the water, so where a river's surface steps up a block, or a
 * rapid's flowing water rises, vanilla counts the boat as sunk under the water ahead and drops its rider. Water standing
 * at most a block over the boat's top, with open air above it, is instead the surface the boat floats up onto; deeper
 * water, or water under a ceiling, still sinks it.
 */
@Mixin(Boat.class)
public abstract class BoatStepMixin {
    @Shadow private double waterLevel;

    @Inject(method = "getStatus", at = @At("HEAD"), cancellable = true)
    private void civilization$floatOntoStep(CallbackInfoReturnable<Boat.Status> status) {
        double step = civilization$stepSurface((Boat) (Object) this);
        if (Double.isNaN(step)) return;
        waterLevel = step;
        status.setReturnValue(Boat.Status.IN_WATER);
    }

    // The highest water surface over the boat's top when every column of it ends within a block of that top under open air
    private static double civilization$stepSurface(Boat boat) {
        var box = boat.getBoundingBox();
        double top = box.maxY + 0.001;
        var level = boat.level();
        var pos = new BlockPos.MutableBlockPos();
        double surface = Double.NaN;
        for (int x = Mth.floor(box.minX); x < Mth.ceil(box.maxX); x++) {
            for (int z = Mth.floor(box.minZ); z < Mth.ceil(box.maxZ); z++) {
                pos.set(x, Mth.floor(box.maxY), z);
                FluidState water = level.getFluidState(pos);
                if (!water.is(FluidTags.WATER) || pos.getY() + water.getHeight(level, pos) <= top) continue;
                do pos.move(Direction.UP); while (level.getFluidState(pos).is(FluidTags.WATER));
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return Double.NaN;
                pos.move(Direction.DOWN);
                double column = pos.getY() + level.getFluidState(pos).getHeight(level, pos);
                if (column > top + 1.0) return Double.NaN;
                surface = Double.isNaN(surface) ? column : Math.max(surface, column);
            }
        }
        return surface;
    }
}
