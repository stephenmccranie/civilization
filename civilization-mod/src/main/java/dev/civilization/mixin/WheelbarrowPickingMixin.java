package dev.civilization.mixin;

import dev.civilization.WheelbarrowEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Mouse interaction can reach non-solid handles without enlarging physical bounds. */
@Mixin(ProjectileUtil.class)
public abstract class WheelbarrowPickingMixin {
    @Redirect(method="getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB civilization$pickHandles(Entity entity){return entity instanceof WheelbarrowEntity e?e.interactionBounds():entity.getBoundingBox();}
}
