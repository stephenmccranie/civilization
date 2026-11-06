package dev.civilization.mixin;

import dev.civilization.CraftingSources;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recipe placement, from the recipe book or JEI, fills the grid from the pockets and then from the Hand, the worn pack and
 * the menu's nearby stores. Every recipe can be placed: JEI shows them all, so the recipe book's unlocks gate nothing.
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {
    @Shadow protected Inventory inventory;
    @Redirect(method="recipeClicked",at=@At(value="INVOKE",target="Lnet/minecraft/stats/ServerRecipeBook;contains(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"))
    private boolean civilization$everyRecipe(ServerRecipeBook book,RecipeHolder<?> recipe){return true;}
    @Inject(method="moveItemToGrid",at=@At("HEAD"),cancellable=true)
    private void civilization$otherSources(Slot slot,ItemStack item,int count,CallbackInfoReturnable<Integer> result){
        if(inventory.findSlotMatchingUnusedItem(item)!=-1)return;
        int taken=CraftingSources.take(inventory.player,item,count);
        if(taken==0){result.setReturnValue(-1);return;}
        if(slot.getItem().isEmpty())slot.set(item.copyWithCount(taken));else slot.getItem().grow(taken);
        result.setReturnValue(count-taken);
    }
}
