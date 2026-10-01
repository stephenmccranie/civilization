package dev.civilization;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public final class FoundryRecipe extends AbstractCookingRecipe {
    public FoundryRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int time) {
        super(KilnContent.FOUNDRY_RECIPE_TYPE.get(), group, category, ingredient, result, experience, time);
    }
    @Override public RecipeSerializer<?> getSerializer() { return KilnContent.FOUNDRY_SERIALIZER.get(); }
    @Override public ItemStack getToastSymbol() { return new ItemStack(KilnContent.FOUNDRY.get()); }
}
