package dev.civilization.mixin;

import dev.civilization.client.PlayerInventoryUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Replace only the backdrop draw; preserve native portrait, slots, effects and recipe book. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Redirect(method="renderBg",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void civilization$backdrop(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int width,int height){
        PlayerInventoryUi.draw(g,((InventoryScreen)(Object)this).getMenu(),x,y,width,height);
    }
}
