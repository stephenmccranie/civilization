package dev.civilization;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
public final class OilEngineContent {
 private static final DeferredRegister.Blocks B=DeferredRegister.createBlocks("civilization");
 private static final DeferredRegister.Items I=DeferredRegister.createItems("civilization");
 private static final DeferredRegister<BlockEntityType<?>> E=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"civilization");
 private static final DeferredRegister<MenuType<?>> M=DeferredRegister.create(Registries.MENU,"civilization");
 public static final DeferredBlock<OilEngineBlock> ENGINE=B.register("oil_engine",()->new OilEngineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
 public static final DeferredBlock<Block> CYLINDER=B.register("engine_cylinder",()->new EnginePieceBlock(false));
 public static final DeferredBlock<Block> WHEEL=B.register("engine_flywheel",EngineFlywheelBlock::new);
 static { I.registerSimpleBlockItem(ENGINE);I.registerSimpleBlockItem(CYLINDER);I.registerSimpleBlockItem(WHEEL); }
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<OilEngineEntity>> ENTITY=E.register("oil_engine",()->BlockEntityType.Builder.of(OilEngineEntity::new,ENGINE.get()).build(null));
 public static final DeferredHolder<MenuType<?>,MenuType<OilEngineMenu>> MENU=M.register("oil_engine",()->new MenuType<>(OilEngineMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
 public static void register(IEventBus bus){B.register(bus);I.register(bus);E.register(bus);M.register(bus);bus.addListener(OilEngineContent::capabilities);}
 private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,ENTITY.get(),(m,side)->side==m.front()?m.fuel:side==net.minecraft.core.Direction.DOWN?m.oil:null);}
}
