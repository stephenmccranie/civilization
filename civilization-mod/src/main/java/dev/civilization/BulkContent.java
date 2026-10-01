package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;

public final class BulkContent {
    private static final DeferredRegister.Blocks B=DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items I=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<BlockEntityType<?>> E=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"civilization");
    private static final DeferredRegister<MenuType<?>> M=DeferredRegister.create(Registries.MENU,"civilization");
    public static final DeferredBlock<BulkBlock> BUNKER=B.register("coal_bunker",()->new BulkBlock(false));
    public static final DeferredBlock<BulkBlock> TANK=B.register("cargo_tank",()->new BulkBlock(true));
    static {I.registerSimpleBlockItem(BUNKER);I.registerSimpleBlockItem(TANK);}
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BulkEntity>> ENTITY=E.register("bulk_store",()->BlockEntityType.Builder.of(BulkEntity::new,BUNKER.get(),TANK.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<BulkMenu>> MENU=M.register("bulk_store",()->new MenuType<>(BulkMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static void register(IEventBus bus){B.register(bus);I.register(bus);E.register(bus);M.register(bus);bus.addListener(BulkContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ENTITY.get(),(m,side)->!m.liquid&&side!=null?m.items(side):null);
        e.registerBlockEntity(Capabilities.FluidHandler.BLOCK,ENTITY.get(),(m,side)->m.liquid&&side!=null?m.fluids(side):null);
    }
}
