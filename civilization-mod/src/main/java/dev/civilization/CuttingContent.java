package dev.civilization;

import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CuttingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Civilization.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Civilization.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Civilization.MOD_ID);
    public static final DeferredBlock<CutBlock> PIECE = BLOCKS.register("cut_block", () -> new CutBlock(Block.Properties.of().strength(2).dynamicShape()));
    public static final DeferredItem<CutItem> ITEM = ITEMS.register("cut_block", () -> new CutItem(PIECE.get(), new Item.Properties().stacksTo(32)));
    public static final DeferredItem<Item> STONE_SAW = ITEMS.register("stone_saw", () -> new Item(new Item.Properties().durability(131)));
    public static final DeferredItem<Item> IRON_SAW = ITEMS.register("iron_saw", () -> new Item(new Item.Properties().durability(250)));
    public static final DeferredItem<Item> DIAMOND_SAW = ITEMS.register("diamond_saw", () -> new Item(new Item.Properties().durability(1561)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CutBlockEntity>> ENTITY = ENTITIES.register("cut_block", () -> BlockEntityType.Builder.of(CutBlockEntity::new, PIECE.get()).build(null));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CuttingRecipe>> RECIPE = RECIPES.register("cutting", () -> new SimpleCraftingRecipeSerializer<>(CuttingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RecombiningRecipe>> RECOMBINING = RECIPES.register("recombining", () -> new SimpleCraftingRecipeSerializer<>(RecombiningRecipe::new));
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); RECIPES.register(bus); }
    public static boolean saw(ItemStack stack) { return stack.is(STONE_SAW.get()) || stack.is(IRON_SAW.get()) || stack.is(DIAMOND_SAW.get()); }
    public static BlockState material(ItemStack stack) {
        if (stack.is(ITEM.get())) {
            String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("material");
            var key = ResourceLocation.tryParse(id);
            return key != null && BuiltInRegistries.BLOCK.containsKey(key) ? BuiltInRegistries.BLOCK.get(key).defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
        }
        return stack.getItem() instanceof BlockItem b ? SlabIntegration.material(b.getBlock().defaultBlockState()) : Blocks.AIR.defaultBlockState();
    }
    // Preserve existing saved values: 2=half, 1=quarter; new value 3=eighth.
    public static int units(ItemStack stack) { int value=stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("units");return stack.is(ITEM.get()) && (value==1 || value==3) ? value : 2; }
    public static boolean piece(ItemStack stack) {return stack.is(ITEM.get()) || SlabIntegration.isSlab(stack);}
    public static double volume(int units) {return units==2?.5:units==1?.25:.125;}
    public static ItemStack stack(BlockState material, int units, int count) {
        if(units==2 && SlabIntegration.slab(material.getBlock())!=null) return new ItemStack(SlabIntegration.slab(material.getBlock()),count);
        var result = new ItemStack(ITEM.get(), count);
        var tag = new CompoundTag(); tag.putString("material", BuiltInRegistries.BLOCK.getKey(material.getBlock()).toString()); tag.putInt("units", units);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }
    public static boolean cuttable(BlockState state) {
        // Keep functional blocks intact: cutting creates inert building material, never inventory copies.
        return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && state.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) >= 0
                && state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)
                && !(state.getBlock() instanceof EntityBlock) && !(state.getBlock() instanceof FallingBlock)
                && !(state.getBlock() instanceof TntBlock);
    }
}
