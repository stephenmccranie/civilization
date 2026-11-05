package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
    public static final DeferredBlock<FoundryBlock> FOUNDRY = BLOCKS.register("foundry",
            () -> new FoundryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));
    public static final DeferredItem<BlockItem> FOUNDRY_ITEM = ITEMS.registerSimpleBlockItem(FOUNDRY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryBlockEntity>> FOUNDRY_ENTITY =
            ENTITIES.register("foundry", () -> BlockEntityType.Builder.of(FoundryBlockEntity::new, FOUNDRY.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<KilnMenu>> FOUNDRY_MENU =
            MENUS.register("foundry", () -> new MenuType<>(KilnMenu::foundry, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<FoundryRecipe>> FOUNDRY_RECIPE_TYPE =
            RECIPES.register("foundry", () -> new RecipeType<>() { @Override public String toString() { return "civilization:foundry"; } });
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<FoundryRecipe>> FOUNDRY_SERIALIZER =
            SERIALIZERS.register("foundry", () -> new SimpleCookingSerializer<>(FoundryRecipe::new, 400));
    public static final DeferredItem<Item> STEEL = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<RoughMetal> IRON_BLOOM = ITEMS.register("iron_bloom", () -> new RoughMetal(() -> Items.IRON_INGOT));
    public static final DeferredItem<RoughMetal> BLISTER_COPPER = ITEMS.register("blister_copper", () -> new RoughMetal(() -> Items.COPPER_INGOT));
    public static final DeferredItem<RoughMetal> GOLD_DORE = ITEMS.register("gold_dore", () -> new RoughMetal(() -> Items.GOLD_INGOT));
    public static final DeferredItem<RoughMetal> BLISTER_STEEL = ITEMS.register("blister_steel", () -> new RoughMetal(STEEL));
    public static final java.util.List<DeferredItem<RoughMetal>> ROUGH_METALS = java.util.List.of(IRON_BLOOM, BLISTER_COPPER, GOLD_DORE, BLISTER_STEEL);
    /** A dressed stone that serves as the first anvil, for hammering only. */
    public static final DeferredBlock<Block> STONE_ANVIL = BLOCKS.register("stone_anvil", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).noOcclusion()) {
        private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 7, 14), box(1, 7, 1, 15, 12, 15));
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    });
    public static final DeferredItem<BlockItem> STONE_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(STONE_ANVIL);
    public static final DeferredItem<Item> MACHINE_PARTS = ITEMS.registerSimpleItem("machine_parts");
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
    }
}
