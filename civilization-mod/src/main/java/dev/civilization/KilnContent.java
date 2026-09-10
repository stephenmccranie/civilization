package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class KilnContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Civilization.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Civilization.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Civilization.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_TYPE, Civilization.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Civilization.MOD_ID);
    public static final DeferredBlock<KilnBlock> KILN = BLOCKS.register("brick_kiln",
            () -> new KilnBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));
    public static final DeferredItem<BlockItem> KILN_ITEM = ITEMS.registerSimpleBlockItem(KILN);
    public static final DeferredBlock<FertilizerRetortBlock> RETORT = BLOCKS.register("fertilizer_retort",
            () -> new FertilizerRetortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));
    public static final DeferredItem<BlockItem> RETORT_ITEM = ITEMS.registerSimpleBlockItem(RETORT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FertilizerRetortBlockEntity>> RETORT_ENTITY =
            ENTITIES.register("fertilizer_retort", () -> BlockEntityType.Builder.of(FertilizerRetortBlockEntity::new, RETORT.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<KilnMenu>> RETORT_MENU =
            MENUS.register("fertilizer_retort", () -> new MenuType<>(KilnMenu::retort, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<RetortRecipe>> RETORT_RECIPE_TYPE =
            RECIPES.register("retort", () -> new RecipeType<>() { @Override public String toString() { return "civilization:retort"; } });
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<RetortRecipe>> RETORT_SERIALIZER =
            SERIALIZERS.register("retort", () -> new SimpleCookingSerializer<>(RetortRecipe::new, 400));
    public static final DeferredItem<Item> MINERAL_COAL = ITEMS.registerSimpleItem("mineral_coal");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KilnBlockEntity>> ENTITY =
            ENTITIES.register("brick_kiln", () -> BlockEntityType.Builder.of(KilnBlockEntity::new, KILN.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<KilnMenu>> MENU =
            MENUS.register("brick_kiln", () -> new MenuType<>(KilnMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<KilnRecipe>> RECIPE_TYPE =
            RECIPES.register("kiln", () -> new RecipeType<>() { @Override public String toString() { return "civilization:kiln"; } });
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<KilnRecipe>> SERIALIZER =
            SERIALIZERS.register("kiln", () -> new SimpleCookingSerializer<>(KilnRecipe::new, 200));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        MENUS.register(bus); RECIPES.register(bus); SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) { event.accept(KILN_ITEM); event.accept(RETORT_ITEM); }
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(MINERAL_COAL);
        });
    }
}
