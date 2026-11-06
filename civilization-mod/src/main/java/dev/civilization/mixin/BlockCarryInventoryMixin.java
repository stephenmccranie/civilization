package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Inventory.class)
public abstract class BlockCarryInventoryMixin {
    @Inject(method="fillStackedContents",at=@At("TAIL"))
    private void civilization$craftingContents(net.minecraft.world.entity.player.StackedContents contents,CallbackInfo ci){
        dev.civilization.CraftingSources.account(((Inventory)(Object)this).player,contents);
    }
    @Inject(method="add(ILnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private void civilization$offhand(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> result){
        var inv=(Inventory)(Object)this;
        if(BlockCarry.restricted(inv.player)&&BlockCarry.filledContainer(stack)){result.setReturnValue(false);return;}
        if(BlockCarry.restricted(inv.player)&&BlockCarry.carried(stack))result.setReturnValue(BlockCarry.insert(inv,stack)>0);
    }
    /** Ordinary goods the pockets cannot take go into the worn backpack. */
    @Inject(method="add(ILnet/minecraft/world/item/ItemStack;)Z",at=@At("RETURN"),cancellable=true)
    private void civilization$backpack(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> result){
        var inv=(Inventory)(Object)this;
        if(stack.isEmpty()||!(inv.player instanceof net.minecraft.server.level.ServerPlayer p)||!(dev.civilization.Backpack.worn(p) instanceof dev.civilization.Backpack.Contents pack))return;
        int before=stack.getCount();pack.insert(stack);if(stack.getCount()<before)result.setReturnValue(true);
    }
    @Inject(method="placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",at=@At("HEAD"),cancellable=true)
    private void civilization$return(ItemStack stack,boolean packet,CallbackInfo ci){
        var inv=(Inventory)(Object)this;
        if(BlockCarry.restricted(inv.player)&&(BlockCarry.carried(stack)||BlockCarry.filledContainer(stack))){
            BlockCarry.insert(inv,stack);if(!stack.isEmpty())inv.player.drop(stack.copyAndClear(),false);ci.cancel();
        }
    }
}
