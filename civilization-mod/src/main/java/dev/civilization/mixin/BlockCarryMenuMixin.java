package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.civilization.BlockCarry;
import dev.civilization.ChestArmful;
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
        if(BlockCarry.carried(stack)||BlockCarry.filledContainer(stack))
            for(int i=from;i<to;i++)if(menu.slots.get(i).container instanceof Inventory inv&&BlockCarry.restricted(inv.player)){
                result.setReturnValue(BlockCarry.insert(inv,stack)>0);return;
            }
        // Ordinary goods, and in Creative everything, fill the pockets, hotbar first, before the worn pack shown beside
        // them, as pickups do. The Hand among the pockets is filled only by the carrying route above.
        int hand=-1,pack=to;
        for(int i=from;i<to;i++){
            var slot=menu.slots.get(i);
            if(slot instanceof dev.civilization.Backpack.PackSlot){if(i>from&&!(menu.slots.get(i-1) instanceof dev.civilization.Backpack.PackSlot))pack=i;}
            else if(to-from>1&&slot.container instanceof Inventory&&slot.getContainerSlot()==BlockCarry.OFFHAND)hand=i;
        }
        if(hand<0&&pack==to)return;
        boolean pockets=hand<0?civilization$move(stack,from,pack,reverse):civilization$move(stack,from,hand,reverse)|civilization$move(stack,hand+1,pack,reverse);
        result.setReturnValue(pockets|civilization$move(stack,pack,to,reverse));
    }
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void civilization$click(int index,int button,ClickType type,Player player,CallbackInfo ci){
        if(BlockCarry.forbiddenClick((AbstractContainerMenu)(Object)this,index,button,type,player))ci.cancel();
    }
    @WrapMethod(method="clicked")
    private void civilization$chestArmful(int index,int button,ClickType type,Player player,Operation<Void> original){
        var menu=(AbstractContainerMenu)(Object)this;
        var offered=ChestArmful.offered(menu,index,button,type,player).copy();
        original.call(index,button,type,player);
        ChestArmful.explain(menu,index,button,type,player,offered);
    }
    @Inject(method="canDragTo",at=@At("RETURN"),cancellable=true)
    private void civilization$drag(Slot slot,CallbackInfoReturnable<Boolean> result){
        if(slot.container instanceof Inventory inv&&BlockCarry.restricted(inv.player)){
            var item=((AbstractContainerMenu)(Object)this).getCarried();
            if(BlockCarry.filledContainer(item)||BlockCarry.carried(item)&&slot.getContainerSlot()!=BlockCarry.OFFHAND)result.setReturnValue(false);
        }
    }
}
