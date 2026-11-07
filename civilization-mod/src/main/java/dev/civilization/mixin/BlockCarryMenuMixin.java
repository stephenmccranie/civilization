package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.civilization.BlockCarry;
import dev.civilization.ChestArmful;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerMenu.class)
public abstract class BlockCarryMenuMixin implements dev.civilization.BlockCarryMenuAccess {
    @Invoker("addSlot") public abstract Slot civilization$addCarrySlot(Slot slot);
    @Shadow @Final public int containerId;
    @Unique private dev.civilization.NearbyStores civilization$nearby;
    @Override public dev.civilization.NearbyStores civilization$nearby(){return civilization$nearby;}
    @Override public void civilization$setNearby(dev.civilization.NearbyStores nearby){civilization$nearby=nearby;}
    @Inject(method="broadcastChanges",at=@At("TAIL"))
    private void civilization$syncNearby(CallbackInfo ci){if(civilization$nearby!=null)civilization$nearby.sync(containerId);}
    /**
     * A move whose range spans all the pockets moves goods into the player's inventory, from whatever the menu shows:
     * building blocks and buckets into the hand and then the worn pack, other goods into the pockets, hotbar first,
     * then the pack, unless they come from it. Narrower ranges are the menu's own moves into its container or between
     * the main pockets and the hotbar.
     */
    @WrapMethod(method="moveItemStackTo")
    private boolean civilization$intoPlayer(ItemStack stack,int from,int to,boolean reverse,Operation<Boolean> original){
        var menu=(AbstractContainerMenu)(Object)this;
        int first=-1,last=-1,pockets=0;
        for(int i=from;i<to;i++){
            var slot=menu.slots.get(i);
            if(slot.container instanceof Inventory&&slot.getContainerSlot()<Inventory.INVENTORY_SIZE){pockets++;if(first<0)first=i;last=i;}
        }
        if(pockets<Inventory.INVENTORY_SIZE)return original.call(stack,from,to,reverse);
        var inv=(Inventory)menu.slots.get(first).container;
        if(BlockCarry.restricted(inv.player)&&(BlockCarry.carried(stack)||BlockCarry.filledContainer(stack)))return BlockCarry.insert(inv,stack)>0;
        boolean moved=original.call(stack,first,last+1,true);
        int pack=-1;
        for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i) instanceof dev.civilization.Backpack.PackSlot slot){if(pack<0)pack=i;if(slot.getItem()==stack)return moved;}
        return pack<0?moved:original.call(stack,pack,pack+dev.civilization.Backpack.SLOTS,false)|moved;
    }
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void civilization$click(int index,int button,ClickType type,Player player,CallbackInfo ci){
        var menu=(AbstractContainerMenu)(Object)this;
        if(BlockCarry.forbiddenClick(menu,index,button,type,player)||BlockCarry.swapIntoHand(menu,index,type,player))ci.cancel();
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
