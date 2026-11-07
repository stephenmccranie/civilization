package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.civilization.NearbyStores;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/**
 * A crafted result shift-clicked out of a crafting table or stonecutter fills the pockets, the Hand and the pack as any
 * shift-click does, and what they cannot take goes onto the menu's nearby stores, so bulk crafting runs until the
 * ingredients or every destination run out.
 */
@Mixin({CraftingMenu.class,StonecutterMenu.class})
public abstract class CraftedOutputMixin {
    @ModifyExpressionValue(method="quickMoveStack",at=@At(value="INVOKE",target="moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",ordinal=0))
    private boolean civilization$ontoStores(boolean moved,@Local(ordinal=1) ItemStack output){
        var near=NearbyStores.of((AbstractContainerMenu)(Object)this);
        return near!=null&&near.store(output)||moved;
    }
}
