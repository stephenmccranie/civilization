package dev.civilization;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FarmingContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("civilization");
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("civilization");
    public static final DeferredBlock<CropBlock> FERTILIZED_WHEAT = BLOCKS.register("fertilized_wheat",
            () -> new CropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<FertilizerItem> FERTILIZER = ITEMS.registerItem("fertilizer", FertilizerItem::new);
    public static final DeferredItem<Item> MINERAL_BLEND = ITEMS.registerSimpleItem("mineral_blend");
    public static final DeferredItem<Item> RATION = ITEMS.registerSimpleItem("field_ration",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0).build()));

    private FarmingContent() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus);
    }
}
