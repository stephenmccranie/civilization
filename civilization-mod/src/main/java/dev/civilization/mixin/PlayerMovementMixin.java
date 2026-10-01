package dev.civilization.mixin;

import dev.civilization.PlayerMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class PlayerMovementMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "getFrictionInfluencedSpeed", at = @At("RETURN"), cancellable = true)
    private void civilization$roadSpeed(float friction, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> ci) {
        if ((Object) this instanceof Player player && dev.civilization.RoadSurface.supports(player))
            ci.setReturnValue((float) (ci.getReturnValue() * dev.civilization.RoadSurface.BONUS));
    }

    @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private net.minecraft.world.phys.Vec3 civilization$jumpBoost(net.minecraft.world.phys.Vec3 impulse) {
        return (Object) this instanceof Player ? impulse.scale(PlayerMovement.JUMP_BOOST_SCALE) : impulse;
    }

    @ModifyArg(method = "setSprinting", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addTransientModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"))
    private AttributeModifier civilization$sprintSpeed(AttributeModifier original) {
        return (Object) this instanceof Player
                ? new AttributeModifier(original.id(), PlayerMovement.SPRINT_BONUS, original.operation())
                : original;
    }
}
