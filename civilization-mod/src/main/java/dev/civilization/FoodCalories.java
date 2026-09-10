package dev.civilization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class FoodCalories {
    private static List<? extends String> cachedList;
    private static Map<String, Double> values = Map.of();
    private FoodCalories() {}

    public static synchronized double of(ItemStack stack, FoodProperties properties) {
        if (properties == null) return 0;
        if (stack.is(RecoveryItems.MORSEL.get())) return CalorieConfig.FORAGE_KCAL.get();
        if (stack.is(FarmingContent.RATION.get())) return breadCalories() * 3;
        List<? extends String> configured = CalorieConfig.FOODS.get();
        if (!configured.equals(cachedList)) {
            var replacement = new HashMap<String, Double>();
            for (String entry : configured) {
                String[] pair = entry.split("=", 2);
                replacement.put(pair[0], Double.parseDouble(pair[1]));
            }
            values = Map.copyOf(replacement);
            cachedList = List.copyOf(configured);
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        double defaultValue = id.getNamespace().equals("minecraft") && FoodCatalog.KCAL.containsKey(id.getPath())
                ? FoodCatalog.KCAL.get(id.getPath()) : properties.nutrition() * CalorieConfig.FOOD_FALLBACK.get();
        if (stack.is(CookingContent.BREAD_DOUGH.get())) defaultValue = 300;
        if (stack.is(CookingContent.COOKIE_DOUGH.get())) defaultValue = 25;
        if (stack.is(CookingContent.UNBAKED_PIE.get())) defaultValue = 200;
        return values.getOrDefault(id.toString(), defaultValue);
    }

    public static double breadCalories() {
        var bread = net.minecraft.world.item.Items.BREAD.getDefaultInstance();
        return of(bread, bread.getFoodProperties(null));
    }
}
