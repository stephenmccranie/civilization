package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerMenu.class)
public abstract class BlockCarryMenuMixin implements dev.civilization.BlockCarryMenuAccess {
    @Invoker("addSlot") public abstract Slot civilization$addCarrySlot(Slot slot);
    @Invoker("moveItemStackTo") protected abstract boolean civilization$move(ItemStack stack,int from,int to,boolean reverse);
    @Inject(method="moveItemStackTo",at=@At("HEAD"),cancellable=true)
    private void civilization$shift(ItemStack stack,int from,int to,boolean reverse,CallbackInfoReturnable<Boolean> result){
        var menu=(AbstractContainerMenu)(Object)this;
        if(BlockCarry.block(stack)||BlockCarry.filledContainer(stack)){
            for(int i=from;i<to;i++)if(menu.slots.get(i).container instanceof Inventory inv&&BlockCarry.restricted(inv.player)){
                result.setReturnValue(BlockCarry.insert(inv,stack)>0);return;
            }
            return;
        }
        // Ordinary goods fill the pockets before the worn pack shown beside them, as pickups do.
        for(int i=from+1;i<to;i++)if(menu.slots.get(i) instanceof dev.civilization.Backpack.PackSlot&&!(menu.slots.get(i-1) instanceof dev.civilization.Backpack.PackSlot)){
            result.setReturnValue(civilization$move(stack,from,i,reverse)|civilization$move(stack,i,to,reverse));return;
        }
    }
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void civilization$click(int index,int button,ClickType type,Player player,CallbackInfo ci){
        if(BlockCarry.forbiddenClick((AbstractContainerMenu)(Object)this,index,button,type,player))ci.cancel();
    }
    @Inject(method="canDragTo",at=@At("RETURN"),cancellable=true)
    private void civilization$drag(Slot slot,CallbackInfoReturnable<Boolean> result){
        if(slot.container instanceof Inventory inv&&BlockCarry.restricted(inv.player)){
            var item=((AbstractContainerMenu)(Object)this).getCarried();
            if(BlockCarry.filledContainer(item)||BlockCarry.block(item)&&slot.getContainerSlot()!=BlockCarry.OFFHAND)result.setReturnValue(false);
        }
    }
}
