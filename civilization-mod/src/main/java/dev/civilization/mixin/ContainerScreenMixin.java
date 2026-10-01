package dev.civilization.mixin;

import dev.civilization.client.MachineUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared skin for vanilla row-based containers; inventory/menu logic remains vanilla. */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin extends AbstractContainerScreen<ChestMenu> {
    protected ContainerScreenMixin(ChestMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Inject(method="renderBg",at=@At("HEAD"),cancellable=true)
    private void civilization$cabinet(GuiGraphics g,float partial,int mx,int my,CallbackInfo ci){
        MachineUi.panel(g,leftPos,topPos,imageWidth,imageHeight);
        MachineUi.slots(g,menu,leftPos,topPos);
        MachineUi.title(g,title.getString(),leftPos,topPos,imageWidth);
        ci.cancel();
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,MachineUi.INK,false);
    }
}
