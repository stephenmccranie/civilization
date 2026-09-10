package dev.civilization;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

public final class CookingRecipe extends AbstractCookingRecipe {
    public CookingRecipe(String group, CookingBookCategory category, Ingredient ingredient,
                         ItemStack result, float experience, int time) {
        super(CookingContent.RECIPE_TYPE.get(), group, category, ingredient, result, experience, time);
    }
    @Override public RecipeSerializer<?> getSerializer() { return CookingContent.SERIALIZER.get(); }
    @Override public ItemStack getToastSymbol() { return CookingContent.STATION_ITEM.toStack(); }
}
