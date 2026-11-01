package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Reserve the hand well when deciding whether recipes require overlay mode. */
@Mixin({CraftingScreen.class,AbstractFurnaceScreen.class})
public abstract class CarryRecipeBookMixin {
    @ModifyConstant(method="init",constant=@Constant(intValue=379))
    private int civilization$carryWidth(int original){var p=Minecraft.getInstance().player;return p!=null&&BlockCarry.restricted(p)?480:original;}
}
