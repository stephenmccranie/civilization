package dev.civilization.mixin;

import dev.civilization.*;
import dev.civilization.client.MachineUi;
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
    @Shadow protected int leftPos,topPos;
    @Inject(method="init",at=@At("TAIL"))
    private void civilization$carrySlot(CallbackInfo ci){var p=Minecraft.getInstance().player;if(p!=null)BlockCarry.attachMenu(menu,p.getInventory());}
    @Inject(method="renderSlot",at=@At("HEAD"))
    private void civilization$handPanel(GuiGraphics g,Slot slot,CallbackInfo ci){
        var p=Minecraft.getInstance().player;
        if(p!=null&&slot.container==p.getInventory()&&slot.getContainerSlot()==BlockCarry.OFFHAND&&slot.x<0){
            // renderSlot already runs inside the screen's left/top translation.
            MachineUi.panel(g,slot.x-5,slot.y-15,26,34);MachineUi.slot(g,slot.x,slot.y);
            g.drawString(Minecraft.getInstance().font,"Hand",slot.x-3,slot.y-12,MachineUi.INK,false);
        }
    }
    @Inject(method="hasClickedOutside",at=@At("RETURN"),cancellable=true)
    private void civilization$handInside(double x,double y,int left,int top,int button,CallbackInfoReturnable<Boolean> result){
        var p=Minecraft.getInstance().player;if(p==null)return;
        for(var slot:menu.slots)if(slot.container==p.getInventory()&&slot.getContainerSlot()==BlockCarry.OFFHAND&&x>=left+slot.x-1&&x<left+slot.x+17&&y>=top+slot.y-1&&y<top+slot.y+17)result.setReturnValue(false);
    }
}
