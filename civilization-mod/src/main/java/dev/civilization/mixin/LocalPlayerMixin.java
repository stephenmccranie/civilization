package dev.civilization.mixin;

import dev.civilization.CalorieFoodData;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "hasEnoughFoodToStartSprinting", at = @At("HEAD"), cancellable = true)
    private void civilization$sprint(CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        CalorieFoodData data = CalorieFoodData.of(player);
        cir.setReturnValue(player.isPassenger() || player.getAbilities().mayfly
                || (!data.isDepleted() && data.reserve().calories() >= data.clientSprintMinimum));
    }
}
