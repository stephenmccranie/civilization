package dev.civilization;

import java.util.ArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Validate the complete exchange against a copy before touching inventory. Main 36 slots only. */
public final class CivicItems {
    public static boolean exchange(ServerPlayer player, ItemStack input, int inputCount, ItemStack output, int outputCount) {
        if (inputCount < 0 || outputCount < 0) return false;
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < 36; i++) slots.add(player.getInventory().getItem(i).copy());
        if (!exchange(slots, input, inputCount, output, outputCount)) return false;
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, slots.get(i));
        player.getInventory().setChanged(); return true;
    }
    /** Mutates a disposable inventory copy; callers commit only after both sides validate. */
    public static boolean exchange(java.util.List<ItemStack> slots, ItemStack input, int inputCount, ItemStack output, int outputCount) {
        if (inputCount < 0 || outputCount < 0) return false;
        int left = inputCount;
        for (var stack : slots) if (ItemStack.isSameItemSameComponents(stack, input)) { int n = Math.min(left, stack.getCount()); stack.shrink(n); left -= n; }
        if (left != 0) return false;
        left = outputCount;
        if (left > 0 && output.isEmpty()) return false;
        for (var stack : slots) if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, output)) {
            int n = Math.min(left, Math.max(0, stack.getMaxStackSize() - stack.getCount())); stack.grow(n); left -= n;
        }
        for (int i = 0; i < slots.size() && left > 0; i++) if (slots.get(i).isEmpty()) {
            int n = Math.min(left, output.getMaxStackSize()); slots.set(i, output.copyWithCount(n)); left -= n;
        }
        if (left != 0) return false;
        return true;
    }
    public static int count(ServerPlayer player, ItemStack template) {
        int count = 0; for (int i = 0; i < 36; i++) { var s = player.getInventory().getItem(i); if (ItemStack.isSameItemSameComponents(s, template)) count += s.getCount(); } return count;
    }
}
