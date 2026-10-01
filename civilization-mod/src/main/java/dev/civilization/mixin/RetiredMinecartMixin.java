package dev.civilization.mixin;

import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MinecartFurnace.class)
public abstract class RetiredMinecartMixin {
    @Shadow private int fuel;
    @Shadow public double xPush;
    @Shadow public double zPush;
    @Inject(method="tick", at=@At("HEAD"))
    private void civilization$stopEngine(CallbackInfo ci) { fuel=0; xPush=0; zPush=0; }
    @Inject(method="interact", at=@At("HEAD"), cancellable=true)
    private void civilization$noFuel(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        ci.setReturnValue(InteractionResult.PASS);
    }
}
