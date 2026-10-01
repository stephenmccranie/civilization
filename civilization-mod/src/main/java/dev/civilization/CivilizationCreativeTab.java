package dev.civilization;

import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CivilizationCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Civilization.MOD_ID);

    static {
        TABS.register("civilization", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.civilization"))
                .icon(() -> KilnContent.KILN_ITEM.toStack())
                .displayItems((parameters, output) -> { BuiltInRegistries.ITEM.stream()
                        .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Civilization.MOD_ID))
                        .filter(item -> item != CuttingContent.ITEM.get() && item != FarmingContent.MINERAL_BLEND.get())
                        .sorted(Comparator.comparingInt((Item item) -> item instanceof BlockItem ? 0 : 1)
                                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).getPath()))
                        .forEach(output::accept);
                    for (var block : java.util.List.of(net.minecraft.world.level.block.Blocks.COBBLESTONE, net.minecraft.world.level.block.Blocks.STONE_BRICKS, net.minecraft.world.level.block.Blocks.BRICKS, RoadContent.PAVERS.get(), net.minecraft.world.level.block.Blocks.COPPER_BLOCK, net.minecraft.world.level.block.Blocks.OAK_PLANKS))
                        for (int units : new int[]{2, 1, 3}) output.accept(CuttingContent.stack(block.defaultBlockState(), units, 1));
                })
                .build());
    }

    private CivilizationCreativeTab() {}

    public static void register(IEventBus bus) { TABS.register(bus); }
}
