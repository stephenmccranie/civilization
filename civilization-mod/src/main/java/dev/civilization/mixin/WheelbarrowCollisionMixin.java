package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.civilization.WheelbarrowCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/**
 * Entities meet wheelbarrows as turned solids. Carriers stay out of native entity collision, so no mod's
 * broad-phase path can stand their bounding boxes in for their bodies.
 */
@Mixin(Entity.class)
public abstract class WheelbarrowCollisionMixin {
    // Sliding along a turned face is the mover's own path, not a collision whose per-axis flags would zero
    // its velocity, so it happens before whatever (vanilla or Sable) compares the move with its outcome.
    @WrapOperation(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 civilization$slideAlongCarriers(Entity self,Vec3 motion,Operation<Vec3> collide,@Local(argsOnly=true) LocalRef<Vec3> requested){
        var slid=WheelbarrowCollision.slide(self,motion);
        if(slid!=motion)requested.set(slid);
        return collide.call(self,slid);
    }
    // Landing on a tray is a collision: it happens inside collide, so the outcome registers it as ground.
    @ModifyVariable(method="collide",at=@At("HEAD"),argsOnly=true)
    private Vec3 civilization$landOnCarriers(Vec3 motion){return WheelbarrowCollision.land((Entity)(Object)this,motion);}
}
