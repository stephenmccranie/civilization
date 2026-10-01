package dev.civilization.mixin;

import dev.civilization.client.MachineUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(RecipeBookComponent.class)
public abstract class InventoryRecipeBookMixin {
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void civilization$recipePanel(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int width,int height){
        if(Minecraft.getInstance().screen instanceof InventoryScreen || Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen)MachineUi.panel(g,x,y,width,height);
        else g.blit(texture,x,y,u,v,width,height);
    }
}
