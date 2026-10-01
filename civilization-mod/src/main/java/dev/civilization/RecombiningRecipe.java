package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Two occupied slots, one piece consumed from each; stack counts never multiply the yield. */
public final class RecombiningRecipe extends CustomRecipe {
    public RecombiningRecipe(CraftingBookCategory category) { super(category); }
    private ItemStack source(CraftingInput input) {
        ItemStack first = ItemStack.EMPTY;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (++count > 2 || !CuttingContent.piece(stack)) return ItemStack.EMPTY;
            if (first.isEmpty()) first = stack;
            else if (CuttingContent.units(first) != CuttingContent.units(stack)
                    || !CuttingContent.material(first).is(CuttingContent.material(stack).getBlock())) return ItemStack.EMPTY;
        }
        return count == 2 && CuttingContent.cuttable(CuttingContent.material(first)) ? first : ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingInput input, Level level) { return !source(input).isEmpty(); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        var stack = source(input);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var material = CuttingContent.material(stack);
        return CuttingContent.units(stack) == 2 ? new ItemStack(material.getBlock())
                : CuttingContent.stack(material, CuttingContent.units(stack) == 3 ? 1 : 2, 1);
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return NonNullList.withSize(input.size(), ItemStack.EMPTY); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return CuttingContent.RECOMBINING.get(); }
}
