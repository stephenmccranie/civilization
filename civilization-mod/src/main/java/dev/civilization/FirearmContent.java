package dev.civilization;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class FirearmContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"civilization");
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,"civilization");
    public static final DeferredItem<PatersonItem> PATERSON=ITEMS.register("colt_paterson",PatersonItem::new);
    public static final DeferredItem<Item> AMMO=ITEMS.register("paterson_36_ammunition",()->new Item(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>,EntityType<PatersonBullet>> BULLET=ENTITIES.register("paterson_bullet",()->EntityType.Builder.<PatersonBullet>of(PatersonBullet::new,MobCategory.MISC).sized(.08f,.08f).clientTrackingRange(8).updateInterval(1).build("civilization:paterson_bullet"));
    public static final DeferredHolder<SoundEvent,SoundEvent> SHOT=SOUNDS.register("paterson_fire",()->SoundEvent.createVariableRangeEvent(ResourceLocation.parse("civilization:paterson_fire")));
    public static void register(IEventBus bus){ITEMS.register(bus);ENTITIES.register(bus);SOUNDS.register(bus);bus.addListener(FirearmPayload::register);}
}
