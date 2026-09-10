package dev.civilization.mixin;

import dev.civilization.FarmingLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "onCraftedBy", at = @At("RETURN"))
    private void civilization$produced(Level level, Player player, int amount, CallbackInfo ci) {
        FarmingLog.crafted(player, (ItemStack) (Object) this, amount);
    }
}
