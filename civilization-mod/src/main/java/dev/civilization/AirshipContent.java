package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class AirshipContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("civilization");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"civilization");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,"civilization");
    private static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> RIDERS=DeferredRegister.create(Registries.ENTITY_TYPE,"civilization");
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>,net.minecraft.world.entity.EntityType<AirshipPilotSeat>> PILOT_SEAT=RIDERS.register("airship_pilot_seat",()->net.minecraft.world.entity.EntityType.Builder.<AirshipPilotSeat>of(AirshipPilotSeat::new,net.minecraft.world.entity.MobCategory.MISC).sized(.01f,.01f).clientTrackingRange(10).updateInterval(1).noSave().build("civilization:airship_pilot_seat"));
    public static final DeferredBlock<AirshipBlock> CONTROLLER = BLOCKS.register("airship_controller", () -> new AirshipBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).pushReaction(PushReaction.BLOCK)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AirshipBlockEntity>> ENTITY = ENTITIES.register("airship_controller", () -> BlockEntityType.Builder.of(AirshipBlockEntity::new, CONTROLLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<AirshipMenu>> MENU = MENUS.register("airship_controller", () -> new MenuType<>(AirshipMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static { ITEMS.registerSimpleBlockItem(CONTROLLER); }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); RIDERS.register(bus); bus.addListener(AirshipPayload::register); }
}
