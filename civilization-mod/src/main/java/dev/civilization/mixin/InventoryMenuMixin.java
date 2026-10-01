package dev.civilization.mixin;

import dev.civilization.AccessorySlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuse the otherwise empty three cells immediately above the native offhand slot. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    protected InventoryMenuMixin() { super(null, 0); }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void civilization$accessories(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        for (int i = 0; i < 3; i++) addSlot(new AccessorySlot(player, i, 77, 8 + i * 18));
    }
}
