package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class BlockCarrySlotMixin {
    @Inject(method="mayPlace",at=@At("HEAD"),cancellable=true)
    private void civilization$blockSlot(ItemStack stack,CallbackInfoReturnable<Boolean> result){
        var slot=(Slot)(Object)this;
        if(slot.container instanceof Inventory inv&&BlockCarry.restricted(inv.player)&&BlockCarry.filledContainer(stack)){result.setReturnValue(false);return;}
        if(slot.container instanceof Inventory inv&&BlockCarry.restricted(inv.player)&&BlockCarry.block(stack)&&slot.getContainerSlot()!=BlockCarry.OFFHAND)result.setReturnValue(false);
    }
    @Inject(method="getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",at=@At("RETURN"),cancellable=true)
    private void civilization$handLimit(ItemStack stack,CallbackInfoReturnable<Integer> result){
        var slot=(Slot)(Object)this;
        if(slot.container instanceof Inventory inv&&BlockCarry.restricted(inv.player)&&BlockCarry.block(stack))result.setReturnValue(Math.min(BlockCarry.LIMIT,result.getReturnValue()));
    }
}
