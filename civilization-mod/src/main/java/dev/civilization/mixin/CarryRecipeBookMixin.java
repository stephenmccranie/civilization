package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Reserve the hand well when deciding whether recipes require overlay mode. */
@Mixin({CraftingScreen.class,AbstractFurnaceScreen.class})
public abstract class CarryRecipeBookMixin {
    // init reads this after super.init, which has attached the menu's Hand.
    @ModifyConstant(method="init",constant=@Constant(intValue=379))
    private int civilization$carryWidth(int original){
        return ((AbstractContainerScreen<?>)(Object)this).getMenu().slots.stream().anyMatch(s->s instanceof BlockCarry.HandSlot)?480:original;
    }
}
