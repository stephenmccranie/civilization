package dev.civilization;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CookingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Civilization.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Civilization.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Civilization.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_TYPE, Civilization.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Civilization.MOD_ID);
    public static final DeferredBlock<CookingStationBlock> STATION = BLOCKS.register("cooking_station",
            () -> new CookingStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));
    public static final DeferredItem<BlockItem> STATION_ITEM = ITEMS.registerSimpleBlockItem(STATION);
    // Cold preparation remains available without fuel; baking adds value.
    public static final DeferredItem<Item> BREAD_DOUGH = ITEMS.registerSimpleItem("bread_dough", edible(3));
    public static final DeferredItem<Item> COOKIE_DOUGH = ITEMS.registerSimpleItem("cookie_dough", edible(1));
    public static final DeferredItem<Item> UNBAKED_PIE = ITEMS.registerSimpleItem("unbaked_pie", edible(2));
    public static final DeferredItem<Item> CAKE_BATTER = ITEMS.registerSimpleItem("cake_batter", new Item.Properties().stacksTo(1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CookingStationBlockEntity>> ENTITY = ENTITIES.register("cooking_station",
            () -> BlockEntityType.Builder.of(CookingStationBlockEntity::new, STATION.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<KilnMenu>> MENU = MENUS.register("cooking_station",
            () -> new MenuType<>(KilnMenu::cooking, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingRecipe>> RECIPE_TYPE = RECIPES.register("cooking",
            () -> new RecipeType<>() { @Override public String toString() { return "civilization:cooking"; } });
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<CookingRecipe>> SERIALIZER = SERIALIZERS.register("cooking",
            () -> new SimpleCookingSerializer<>(CookingRecipe::new, 200));

    private static Item.Properties edible(int nutrition) {
        return new Item.Properties().food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0).build());
    }

    /** NPC sales must not provide a renewable shortcut around household cooking. Finite loot remains. */
    public static boolean requiresCooking(ItemStack stack) {
        return List.of(Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN, Items.COOKED_MUTTON,
                Items.COOKED_RABBIT, Items.COOKED_COD, Items.COOKED_SALMON, Items.BAKED_POTATO,
                Items.DRIED_KELP, Items.BREAD, Items.COOKIE, Items.PUMPKIN_PIE, Items.CAKE).contains(stack.getItem());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        MENUS.register(bus); RECIPES.register(bus); SERIALIZERS.register(bus);
    }
}
