package dev.civilization;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Small, hand-recovered frontier deposits and their survey instrument. */
public final class FrontierContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Civilization.MOD_ID);

    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.register("uranium_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)));
    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.registerSimpleItem("raw_uranium",
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));

    private FrontierContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.registerSimpleBlockItem(URANIUM_ORE);
        ITEMS.register(bus);
    }
}
