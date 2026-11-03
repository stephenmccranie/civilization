package dev.civilization.mixin;

import dev.civilization.*;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Mouse rays hit the turned body and non-solid handles, not an axis-aligned envelope. */
@Mixin(ProjectileUtil.class)
public abstract class WheelbarrowPickingMixin {
    @Redirect(method="getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB civilization$pickMeasured(Entity entity,Entity viewer,Vec3 from,Vec3 to,AABB area,Predicate<Entity> filter,double reach){
        if(!(entity instanceof WheelbarrowEntity e))return entity.getBoundingBox();
        // Vanilla clips the returned box: a speck at the hit, or an empty box at the ray's
        // start, which it neither clips nor counts as containing that start.
        return WheelbarrowCollision.pick(e.position(),e.getYRot(),from,to).map(hit->new AABB(hit,hit).inflate(1e-4)).orElse(new AABB(from,from));
    }
}
