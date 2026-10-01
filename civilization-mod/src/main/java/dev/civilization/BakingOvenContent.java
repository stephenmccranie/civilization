package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class BakingOvenContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Civilization.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,Civilization.MOD_ID);
    public static final DeferredBlock<BakingOvenBlock> OVEN=BLOCKS.register("baking_oven",BakingOvenBlock::new);
    public static final DeferredItem<BlockItem> ITEM=ITEMS.registerSimpleBlockItem(OVEN);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BakingOvenEntity>> ENTITY=ENTITIES.register("baking_oven",()->BlockEntityType.Builder.of(BakingOvenEntity::new,OVEN.get()).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(OvenDialPayload::register);}
}
