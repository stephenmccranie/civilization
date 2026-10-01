package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ProtectedMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void civilization$access(int slot, int mouse, ClickType type, Player player, CallbackInfo callback) {
        if (player.level().isClientSide) return;
        for (var entry : ((AbstractContainerMenu)(Object)this).slots) if (!CivicAccess.container(entry.container, player)) {
            player.closeContainer(); callback.cancel(); return;
        }
    }
}
