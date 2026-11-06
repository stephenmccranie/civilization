package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import dev.civilization.client.SidePanels;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** The recipe book opens beside the menu only where the side column still fits on the menu's right. */
@Mixin({InventoryScreen.class,CraftingScreen.class,AbstractFurnaceScreen.class})
public abstract class CarryRecipeBookMixin {
    // init reads this after super.init, which has attached the menu's Hand. The open book moves the 176-wide menu 77
    // right of centre and the column reaches ROOM beyond it; centring doubles both.
    @ModifyConstant(method="init",constant=@Constant(intValue=379))
    private int civilization$carryWidth(int original){
        return BlockCarry.handSlot(((AbstractContainerScreen<?>)(Object)this).getMenu())!=null?176+2*77+2*SidePanels.ROOM:original;
    }
}
