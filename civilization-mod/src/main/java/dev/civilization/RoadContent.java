package dev.civilization;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Street paving is a dedicated manufactured material, separate from wall bricks. */
public final class RoadContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Civilization.MOD_ID);

    public static final DeferredBlock<Block> PAVERS = BLOCKS.register("street_pavers",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));
    public static final DeferredBlock<SlabBlock> SLAB = BLOCKS.register("street_pavers_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_SLAB)));
    public static final DeferredBlock<StairBlock> STAIRS = BLOCKS.register("street_pavers_stairs",
            () -> new StairBlock(PAVERS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_STAIRS)));

    public static final DeferredItem<BlockItem> PAVERS_ITEM = ITEMS.registerSimpleBlockItem(PAVERS);
    public static final DeferredItem<BlockItem> SLAB_ITEM = ITEMS.registerSimpleBlockItem(SLAB);
    public static final DeferredItem<BlockItem> STAIRS_ITEM = ITEMS.registerSimpleBlockItem(STAIRS);

    private RoadContent() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); }
}
