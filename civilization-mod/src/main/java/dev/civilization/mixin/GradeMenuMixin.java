package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.civilization.Grade;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Menus combine a graded material's stacks whatever their units' grades, and move the actual units: placing,
 * collecting, dragging and shift-clicking all hand over the top units of the stack they take from.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class GradeMenuMixin {
    @Shadow @Final public NonNullList<Slot> slots;
    @Shadow private int quickcraftStatus;
    @Shadow public abstract ItemStack getCarried();
    @Shadow public abstract boolean canTakeItemForPickAll(ItemStack stack, Slot slot);
    /** Units already handed out by the drag being placed. */
    @Unique private int civilization$dragged;

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void civilization$click(int index, int button, ClickType type, Player player, CallbackInfo ci) {
        var carried = getCarried();
        if (index < 0 || quickcraftStatus != 0 || carried.isEmpty() || !Grade.graded(carried) || button != 0 && button != 1) return;
        var slot = slots.get(index);
        var held = slot.getItem();
        if (type == ClickType.PICKUP && Grade.stacks(held, carried) && slot.mayPickup(player)) {
            if (slot.mayPlace(carried)) {
                if (Grade.join(held, carried, Math.min(button == 0 ? carried.getCount() : 1, slot.getMaxStackSize(held) - held.getCount())) > 0) slot.setByPlayer(held);
            } else slot.tryRemove(held.getCount(), carried.getMaxStackSize() - carried.getCount(), player).ifPresent(taken -> {
                slot.onTake(player, taken);
                Grade.join(carried, taken, taken.getCount());
            });
            slot.setChanged();
            ci.cancel();
        } else if (type == ClickType.PICKUP_ALL && (!slot.hasItem() || !slot.mayPickup(player))) {
            int step = button == 0 ? 1 : -1;
            for (int pass = 0; pass < 2; pass++)
                for (int i = button == 0 ? 0 : slots.size() - 1; i >= 0 && i < slots.size() && carried.getCount() < carried.getMaxStackSize(); i += step) {
                    var other = slots.get(i);
                    if (!other.hasItem() || !AbstractContainerMenu.canItemQuickReplace(other, carried, true) || !other.mayPickup(player) || !canTakeItemForPickAll(carried, other)) continue;
                    var stack = other.getItem();
                    if (pass == 0 && stack.getCount() == stack.getMaxStackSize()) continue;
                    var taken = other.safeTake(stack.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                    Grade.join(carried, taken, taken.getCount());
                }
            ci.cancel();
        }
    }

    /** A drag remembers its slots in the order they were dragged across, so the first slot receives the best units. */
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;"))
    private HashSet<Slot> civilization$dragOrder(Operation<HashSet<Slot>> original) { return new LinkedHashSet<>(); }

    @ModifyReturnValue(method = "canItemQuickReplace", at = @At("RETURN"))
    private static boolean civilization$quickReplace(boolean same, Slot slot, ItemStack stack, boolean sizeMatters) {
        return same || slot != null && slot.hasItem() && Grade.graded(stack) && Grade.stacks(slot.getItem(), stack)
                && slot.getItem().getCount() + (sizeMatters ? 0 : stack.getCount()) <= stack.getMaxStackSize();
    }

    /** A drag gives each slot its own units and the next units of the carried stack; the carried remainder keeps the worst. */
    @WrapOperation(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
    private ItemStack civilization$drag(ItemStack carried, int count, Operation<ItemStack> original, @Local Slot slot) {
        if (!Grade.graded(carried)) return original.call(carried, count);
        var units = carried.copy();
        units.split(civilization$dragged);
        var placed = units.split(count - slot.getItem().getCount());
        civilization$dragged += placed.getCount();
        if (!slot.hasItem()) return placed;
        var result = slot.getItem().copy();
        Grade.join(result, placed, placed.getCount());
        return result;
    }

    @Inject(method = "resetQuickCraft", at = @At("HEAD"))
    private void civilization$dragEnded(CallbackInfo ci) { civilization$dragged = 0; }

    @WrapMethod(method = "moveItemStackTo")
    private boolean civilization$shift(ItemStack stack, int from, int to, boolean reverse, Operation<Boolean> original) {
        boolean joined = false;
        if (Grade.graded(stack))
            for (int i = reverse ? to - 1 : from; !stack.isEmpty() && (reverse ? i >= from : i < to); i += reverse ? -1 : 1) {
                var slot = slots.get(i);
                var held = slot.getItem();
                if (!held.isEmpty() && Grade.stacks(held, stack) && Grade.join(held, stack, slot.getMaxStackSize(held) - held.getCount()) > 0) {
                    slot.setChanged();
                    joined = true;
                }
            }
        return original.call(stack, from, to, reverse) | joined;
    }
}
