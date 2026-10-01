package dev.civilization.mixin;

import dev.civilization.client.MachineUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared skin for the native crafting table, including recipe-book repositioning. */
@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> {
    protected CraftingScreenMixin(CraftingMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Inject(method="renderBg",at=@At("HEAD"),cancellable=true)
    private void civilization$cabinet(GuiGraphics g,float partial,int mx,int my,CallbackInfo ci){
        MachineUi.panel(g,leftPos,topPos,imageWidth,imageHeight);
        MachineUi.slots(g,menu,leftPos,topPos);
        MachineUi.title(g,title.getString(),leftPos,topPos,imageWidth);
        g.fill(leftPos+90,topPos+34,leftPos+111,topPos+40,0xff756a53);
        for(int i=0;i<7;i++)g.fill(leftPos+108+i,topPos+30+i,leftPos+109+i,topPos+44-i,0xff756a53);
        ci.cancel();
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,MachineUi.INK,false);
    }
}
