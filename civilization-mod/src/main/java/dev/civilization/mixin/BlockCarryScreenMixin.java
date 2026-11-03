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
    private void civilization$carrySlot(CallbackInfo ci){
        // Creative maps catalog/hotbar clicks using its native slot count.
        if((Object)this instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)return;
        var p=Minecraft.getInstance().player;if(p!=null){BlockCarry.attachMenu(menu,p.getInventory());Backpack.attachMenu(menu,p);}
    }
    @Inject(method="render",at=@At("HEAD"))
    private void civilization$handLayout(GuiGraphics g,int mouseX,int mouseY,float partial,CallbackInfo ci){
        var p=Minecraft.getInstance().player;if(p==null||!BlockCarry.restricted(p))return;
        int x=MachineUi.handSlotX((AbstractContainerScreen<?>)(Object)this);
        for(var slot:menu.slots)if(slot.container==p.getInventory()&&slot.getContainerSlot()==BlockCarry.OFFHAND&&slot.x<0)((SlotPositionAccess)slot).civilization$setX(x);
        MachineUi.packLayout(menu);
    }
    @Inject(method="renderSlot",at=@At("HEAD"))
    private void civilization$handPanel(GuiGraphics g,Slot slot,CallbackInfo ci){
        var p=Minecraft.getInstance().player;
        if(p!=null&&BlockCarry.restricted(p)&&slot.container==p.getInventory()&&slot.getContainerSlot()==BlockCarry.OFFHAND&&slot.x<0){
            // renderSlot already runs inside the screen's left/top translation.
            MachineUi.handPanel(g,slot.x,slot.y);
        }
        if(slot instanceof Backpack.PackSlot){
            if(slot.getContainerSlot()==0&&MachineUi.packLayout(menu) instanceof net.minecraft.client.renderer.Rect2i panel)MachineUi.packPanel(g,panel);
            MachineUi.slot(g,slot.x,slot.y);
        }
    }
    @Inject(method="hasClickedOutside",at=@At("RETURN"),cancellable=true)
    private void civilization$handInside(double x,double y,int left,int top,int button,CallbackInfoReturnable<Boolean> result){
        var p=Minecraft.getInstance().player;if(p==null||!BlockCarry.restricted(p))return;
        for(var slot:menu.slots)if(slot.container==p.getInventory()&&slot.getContainerSlot()==BlockCarry.OFFHAND&&slot.x<0&&x>=left+slot.x-MachineUi.HAND_LEFT&&x<left+slot.x-MachineUi.HAND_LEFT+MachineUi.HAND_WIDTH&&y>=top+slot.y-MachineUi.HAND_TOP&&y<top+slot.y-MachineUi.HAND_TOP+MachineUi.HAND_HEIGHT)result.setReturnValue(false);
        if(MachineUi.packLayout(menu) instanceof net.minecraft.client.renderer.Rect2i panel&&panel.contains((int)x-left,(int)y-top))result.setReturnValue(false);
    }
}
