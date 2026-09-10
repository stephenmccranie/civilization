package dev.civilization;

import java.util.Map;
import static java.util.Map.entry;

/** Intentional defaults for every vanilla edible item; server overrides take precedence. */
public final class FoodCatalog {
    private FoodCatalog() {}
    public static final Map<String, Double> KCAL = Map.ofEntries(
            entry("apple", 200.0), entry("baked_potato", 400.0), entry("beef", 200.0),
            entry("beetroot", 100.0), entry("beetroot_soup", 400.0), entry("bread", 500.0),
            entry("carrot", 150.0), entry("chicken", 150.0), entry("chorus_fruit", 150.0),
            entry("cod", 100.0), entry("cooked_beef", 700.0), entry("cooked_chicken", 500.0),
            entry("cooked_cod", 350.0), entry("cooked_mutton", 500.0), entry("cooked_porkchop", 700.0),
            entry("cooked_rabbit", 400.0), entry("cooked_salmon", 450.0), entry("cookie", 100.0),
            entry("dried_kelp", 50.0), entry("enchanted_golden_apple", 200.0), entry("golden_apple", 200.0),
            entry("golden_carrot", 400.0), entry("honey_bottle", 300.0), entry("melon_slice", 75.0),
            entry("mushroom_stew", 400.0), entry("mutton", 150.0), entry("poisonous_potato", 50.0),
            entry("porkchop", 200.0), entry("potato", 100.0), entry("pufferfish", 25.0),
            entry("pumpkin_pie", 600.0), entry("rabbit", 150.0), entry("rabbit_stew", 900.0),
            entry("rotten_flesh", 100.0), entry("salmon", 150.0), entry("spider_eye", 25.0),
            entry("suspicious_stew", 400.0), entry("sweet_berries", 75.0), entry("glow_berries", 75.0),
            entry("tropical_fish", 75.0), entry("ominous_bottle", 0.0));
}
