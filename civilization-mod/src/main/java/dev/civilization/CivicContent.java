package dev.civilization;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CivicContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("civilization");
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "civilization");
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<LandMenu>> LAND_MENU = MENUS.register("land_controller",
            () -> new net.minecraft.world.inventory.MenuType<>(LandMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<CivicMenu>> SHOP_MENU = MENUS.register("trade_counter",
            () -> new net.minecraft.world.inventory.MenuType<>(CivicMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<SurveyMenu>> SURVEY_MENU = MENUS.register("survey_map",
            () -> new net.minecraft.world.inventory.MenuType<>(SurveyMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,"civilization");
    public static final DeferredBlock<SurveyBlock> TABLE = BLOCKS.register("survey_table", () -> new SurveyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().pushReaction(PushReaction.BLOCK),true));
    public static final DeferredBlock<SurveyBlock> TABLE_PART = BLOCKS.register("survey_table_section", () -> new SurveyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().pushReaction(PushReaction.BLOCK),false));
    // Keep the former item ID so existing maps become placeable table controllers.
    public static final DeferredItem<BlockItem> SURVEY_ITEM = ITEMS.register("survey_map", () -> new BlockItem(TABLE.get(),new net.minecraft.world.item.Item.Properties().stacksTo(32)));
    public static final DeferredItem<BlockItem> TABLE_PART_ITEM = ITEMS.registerSimpleBlockItem(TABLE_PART);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<SurveyBlockEntity>> TABLE_ENTITY = ENTITIES.register("survey_table", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(SurveyBlockEntity::new,TABLE.get()).build(null));
    public static final DeferredBlock<CivicBlock> LAND = BLOCKS.register("land_controller", () -> new CivicBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).pushReaction(PushReaction.BLOCK), true));
    public static final DeferredBlock<CivicBlock> SHOP = BLOCKS.register("trade_counter", () -> new CivicBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).pushReaction(PushReaction.BLOCK), false));
    public static final DeferredItem<BlockItem> LAND_ITEM = ITEMS.registerSimpleBlockItem(LAND);
    public static final DeferredItem<BlockItem> SHOP_ITEM = ITEMS.registerSimpleBlockItem(SHOP);
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); MENUS.register(bus); ENTITIES.register(bus); }
}
