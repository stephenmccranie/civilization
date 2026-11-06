package dev.civilization.mixin;

import dev.civilization.*;
import dev.civilization.client.SidePanels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerScreen.class)
public abstract class BlockCarryScreenMixin {
    @Shadow @Final protected AbstractContainerMenu menu;
    @Inject(method="init",at=@At("TAIL"))
    private void civilization$carrySlot(CallbackInfo ci){
        // Creative maps catalog/hotbar clicks using its native slot count.
        if((Object)this instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)return;
        var p=Minecraft.getInstance().player;if(p!=null){BlockCarry.attachMenu(menu,p.getInventory());Backpack.attachMenu(menu,p);}
    }
    // The side column follows the worn pack and nearby stores each frame, before slots are drawn, hovered or clicked.
    @Inject(method="render",at=@At("HEAD"))
    private void civilization$sideLayout(GuiGraphics g,int mouseX,int mouseY,float partial,CallbackInfo ci){
        SidePanels.layout((AbstractContainerScreen<?>)(Object)this);
    }
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void civilization$columnPress(double x,double y,int button,CallbackInfoReturnable<Boolean> result){
        if(SidePanels.mouseClicked((AbstractContainerScreen<?>)(Object)this,x,y,button))result.setReturnValue(true);
    }
    @Inject(method="mouseReleased",at=@At("HEAD"),cancellable=true)
    private void civilization$columnRelease(double x,double y,int button,CallbackInfoReturnable<Boolean> result){
        if(SidePanels.mouseReleased((AbstractContainerScreen<?>)(Object)this,x,y))result.setReturnValue(true);
    }
}
