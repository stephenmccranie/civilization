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
    /** Predicts the server's drop from the hand's building stack. */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void civilization$dropFromHand(boolean all, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!dev.civilization.BlockCarry.dropsFromHand(player)) return;
        dev.civilization.BlockCarry.takeDrop(player, all);
        player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(all
                ? net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                : net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM,
                net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN));
        cir.setReturnValue(true);
    }
}
