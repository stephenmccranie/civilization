package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class WorkshopContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Civilization.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,Civilization.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,Civilization.MOD_ID);
    public static final DeferredBlock<WorkshopBlock> TANNERY=block("tannery",0), TEXTILE=block("textile_workshop",1), SMITHY=block("smithy",2);
    public static final DeferredItem<Item> HIDE=ITEMS.registerSimpleItem("raw_hide"), CLOTH=ITEMS.registerSimpleItem("cloth");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<WorkshopBlockEntity>> ENTITY=ENTITIES.register("workshop",()->BlockEntityType.Builder.of(WorkshopBlockEntity::new,TANNERY.get(),TEXTILE.get(),SMITHY.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<WorkshopMenu>> MENU=MENUS.register("workshop",()->new MenuType<>(WorkshopMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private static DeferredBlock<WorkshopBlock> block(String name,int kind){var b=BLOCKS.register(name,()->new WorkshopBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE),kind));ITEMS.registerSimpleBlockItem(b);return b;}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);}
}
