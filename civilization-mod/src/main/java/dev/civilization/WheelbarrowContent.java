package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Empty handling prototype; shared-parts assembly supplies acquisition, cargo remains separate. */
public final class WheelbarrowContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"civilization");
    public static final DeferredItem<WheelbarrowItem> ITEM=ITEMS.register("wheelbarrow",WheelbarrowItem::new);
    public static final DeferredHolder<EntityType<?>,EntityType<WheelbarrowEntity>> ENTITY=ENTITIES.register("wheelbarrow",()->EntityType.Builder.<WheelbarrowEntity>of(WheelbarrowEntity::new,MobCategory.MISC).sized(.8f,.75f).clientTrackingRange(8).updateInterval(1).build("civilization:wheelbarrow"));
    public static void register(IEventBus bus){ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(WheelbarrowPayload::register);}
}
