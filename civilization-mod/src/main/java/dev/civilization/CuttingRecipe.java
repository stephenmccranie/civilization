package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class CuttingRecipe extends CustomRecipe {
    public CuttingRecipe(CraftingBookCategory category) { super(category); }
    private int[] inputs(CraftingInput input) {
        int saw = -1, material = -1;
        for (int i = 0; i < input.size(); i++) {
            var s = input.getItem(i); if (s.isEmpty()) continue;
            if (CuttingContent.saw(s) && saw == -1) saw = i;
            else if (material == -1) material = i; else return null;
        }
        if (saw < 0 || material < 0) return null;
        var tool = input.getItem(saw); var block = input.getItem(material);
        if (tool.getDamageValue() >= tool.getMaxDamage()) return null;
        if (block.is(CuttingContent.ITEM.get()) && CuttingContent.units(block) == 3) return null;
        if (!block.is(CuttingContent.ITEM.get()) && (block.has(DataComponents.BLOCK_ENTITY_DATA) || block.has(DataComponents.CONTAINER) || block.has(DataComponents.CUSTOM_DATA))) return null;
        var state = CuttingContent.material(block);
        if (!CuttingContent.cuttable(state)) return null;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && !tool.is(CuttingContent.DIAMOND_SAW.get())) return null;
        if (state.is(BlockTags.NEEDS_IRON_TOOL) && tool.is(CuttingContent.STONE_SAW.get())) return null;
        return new int[]{saw, material};
    }
    @Override public boolean matches(CraftingInput input, Level level) { return inputs(input) != null; }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        var indices = inputs(input); if (indices == null) return ItemStack.EMPTY;
        var source = input.getItem(indices[1]);
        return CuttingContent.stack(CuttingContent.material(source), CuttingContent.piece(source) ? CuttingContent.units(source)==2?1:3 : 2, 2);
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var indices = inputs(input); if (indices == null) return result;
        var saw = input.getItem(indices[0]).copyWithCount(1);
        saw.setDamageValue(saw.getDamageValue() + 1);
        if (saw.getDamageValue() < saw.getMaxDamage()) result.set(indices[0], saw);
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return CuttingContent.RECIPE.get(); }
}
