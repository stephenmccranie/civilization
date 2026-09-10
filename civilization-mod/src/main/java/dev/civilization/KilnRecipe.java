package dev.civilization;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public final class KilnRecipe extends AbstractCookingRecipe {
    public KilnRecipe(String group, CookingBookCategory category, Ingredient ingredient,
                      ItemStack result, float experience, int time) {
        super(KilnContent.RECIPE_TYPE.get(), group, category, ingredient, result, experience, time);
    }
    @Override public RecipeSerializer<?> getSerializer() { return KilnContent.SERIALIZER.get(); }
    @Override public ItemStack getToastSymbol() { return new ItemStack(KilnContent.KILN.get()); }
}
