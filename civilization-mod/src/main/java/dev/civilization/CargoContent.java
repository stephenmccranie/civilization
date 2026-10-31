package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CargoContent {
    private static final DeferredRegister.Blocks B=DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items I=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<BlockEntityType<?>> E=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"civilization");
    private static final DeferredRegister<MenuType<?>> M=DeferredRegister.create(Registries.MENU,"civilization");
    public static final DeferredBlock<StockpileBlock> STOCKPILE=B.register("material_stockpile",StockpileBlock::new);
    static {I.registerSimpleBlockItem(STOCKPILE);}
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<StockpileEntity>> ENTITY=E.register("material_stockpile",()->BlockEntityType.Builder.of(StockpileEntity::new,STOCKPILE.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<CargoMenu>> MENU=M.register("mass_cargo",()->new MenuType<>(CargoMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static void register(IEventBus bus){B.register(bus);I.register(bus);E.register(bus);M.register(bus);}
}
