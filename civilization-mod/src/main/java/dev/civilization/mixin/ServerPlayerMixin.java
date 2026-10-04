package dev.civilization.mixin;

import dev.civilization.CalorieFoodData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method="canHarmPlayer",at=@At("HEAD"),cancellable=true)
    private void civilization$arenaConsent(net.minecraft.world.entity.player.Player other,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){
        var player=(ServerPlayer)(Object)this;
        if(dev.civilization.ArenaService.opposed(dev.civilization.ArenaData.get(player.server),player,other))ci.setReturnValue(true);
    }
    @Inject(method = "checkMovementStatistics", at = @At("HEAD"))
    private void civilization$movement(double dx, double dy, double dz, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        CalorieFoodData.of(player).move(player, dx, dy, dz);
    }
}
