package dev.civilization;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public final class RetortRecipe extends AbstractCookingRecipe {
    public RetortRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(KilnContent.RETORT_RECIPE_TYPE.get(), group, category, ingredient, result, experience, time);
    }
    @Override public RecipeSerializer<?> getSerializer() { return KilnContent.RETORT_SERIALIZER.get(); }
    @Override public ItemStack getToastSymbol() { return new ItemStack(KilnContent.RETORT.get()); }
}
