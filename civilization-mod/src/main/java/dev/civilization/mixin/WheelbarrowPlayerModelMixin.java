package dev.civilization.mixin;

import dev.civilization.WheelbarrowEntity;
import dev.civilization.client.WheelbarrowHands;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands on the moving grips and a walking gait; ordinary players keep vanilla animation. */
@Mixin(PlayerModel.class)
public abstract class WheelbarrowPlayerModelMixin {
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void civilization$push(LivingEntity player,float swing,float amount,float age,float yaw,float pitch,CallbackInfo ci){
        if(!(player.getVehicle() instanceof WheelbarrowEntity e))return;
        var model=(PlayerModel<?>)(Object)this;
        float partial=Mth.clamp(age-player.tickCount,0,1);
        WheelbarrowHands.aim(model.rightArm,e.gripFromHandler(1,partial));WheelbarrowHands.aim(model.leftArm,e.gripFromHandler(-1,partial));
        float stride=e.handlerWalkAnimation.speed(partial);
        float phase=e.handlerWalkAnimation.position(partial)*.6662f;
        model.rightLeg.xRot=Mth.cos(phase)*1.4f*stride;model.leftLeg.xRot=Mth.cos(phase+Mth.PI)*1.4f*stride;
        model.leftLeg.yRot=model.rightLeg.yRot=0;
        model.leftSleeve.copyFrom(model.leftArm);model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);model.rightPants.copyFrom(model.rightLeg);
    }
}
