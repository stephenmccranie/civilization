package dev.civilization;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ArenaContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"civilization");
    public static final DeferredBlock<ArenaBlock> PIT=BLOCKS.register("gladiator_pit",()->new ArenaBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> ITEM=ITEMS.registerSimpleBlockItem(PIT);
    public static final DeferredHolder<MenuType<?>,MenuType<ArenaMenu>> MENU=MENUS.register("gladiator_pit",()->new MenuType<>(ArenaMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static void register(IEventBus bus) { BLOCKS.register(bus);ITEMS.register(bus);MENUS.register(bus);bus.addListener(ArenaPayload::register); }
    private ArenaContent() {}
}
