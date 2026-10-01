package dev.civilization;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Crude oil resists swimming without changing movement in other liquids. */
@EventBusSubscriber(modid = "civilization")
public final class OilMovement {
    private OilMovement() {}

    @SubscribeEvent public static void slow(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        var feet = BlockPos.containing(living.getX(), living.getY() + .1, living.getZ());
        var torso = feet.above();
        if (!crude(living, feet) && !crude(living, torso)) return;
        var motion = living.getDeltaMovement();
        living.setDeltaMovement(motion.x * .65, motion.y * .8, motion.z * .65);
    }

    private static boolean crude(LivingEntity living, BlockPos pos) {
        return living.level().getFluidState(pos).getType().getFluidType() == IndustrialContent.CRUDE_TYPE.get();
    }
}
