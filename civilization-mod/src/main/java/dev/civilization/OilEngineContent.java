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
 private static final DeferredRegister<net.minecraft.sounds.SoundEvent> S=DeferredRegister.create(Registries.SOUND_EVENT,"civilization");
 public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> BLOWLAMP=sound("engine_blowlamp"),KICK=sound("engine_kick"),KNOCK=sound("engine_knock"),BED=sound("engine_bed");
 private static DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> sound(String name){return S.register(name,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("civilization",name)));}
 public static final DeferredBlock<OilEngineBlock> ENGINE=B.register("oil_engine",()->new OilEngineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
 static { I.registerSimpleBlockItem(ENGINE); }
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<OilEngineEntity>> ENTITY=E.register("oil_engine",()->BlockEntityType.Builder.of(OilEngineEntity::new,ENGINE.get()).build(null));
 public static final DeferredHolder<MenuType<?>,MenuType<OilEngineMenu>> MENU=M.register("oil_engine",()->new MenuType<>(OilEngineMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
 public static void register(IEventBus bus){B.register(bus);I.register(bus);E.register(bus);M.register(bus);S.register(bus);bus.addListener(OilEngineContent::capabilities);}
 private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,ENTITY.get(),(m,side)->side==m.front()?new FluidPort(m,side,m.fuel,true,false):side==net.minecraft.core.Direction.DOWN?new FluidPort(m,side,m.oil,true,false):null);}
}
