package dev.civilization.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Creative's compact inventory page has no accessory column; hide its wrappers there. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryAccessoriesMixin {
    @ModifyArgs(method = "selectTab", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void civilization$hideAccessories(Args args) {
        int sourceIndex = args.get(1);
        if (sourceIndex >= 46 && sourceIndex < 49) {
            args.set(2, -2000);
            args.set(3, -2000);
        }
    }
}
