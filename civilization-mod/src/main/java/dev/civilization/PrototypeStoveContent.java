package dev.civilization;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class PrototypeStoveContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Civilization.MOD_ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Civilization.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,Civilization.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,Civilization.MOD_ID);
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,Civilization.MOD_ID);
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> WET=SOUNDS.register("stove_wet",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("civilization:stove_wet")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> SIZZLE=SOUNDS.register("stove_sizzle",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("civilization:stove_sizzle")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> CHAR=SOUNDS.register("stove_char",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("civilization:stove_char")));
    private static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,Civilization.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Double>> QUALITY=COMPONENTS.registerComponentType("meal_quality",b->b.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Double>> CALORIES=COMPONENTS.registerComponentType("meal_calories",b->b.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
    public static final DeferredBlock<PrototypeStoveBlock> STOVE=BLOCKS.register("prototype_stove",PrototypeStoveBlock::new);
    public static final DeferredBlock<RestingSkilletBlock> RESTING_SKILLET=BLOCKS.register("resting_skillet",RestingSkilletBlock::new);
    public static final DeferredItem<SkilletItem> SKILLET=ITEMS.register("skillet",SkilletItem::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<RestingSkilletEntity>> RESTING_ENTITY=ENTITIES.register("resting_skillet",()->BlockEntityType.Builder.of(RestingSkilletEntity::new,RESTING_SKILLET.get()).build(null));
    public static final DeferredItem<BlockItem> STOVE_ITEM=ITEMS.registerSimpleBlockItem(STOVE);
    public static final DeferredItem<WorkMealItem> MEAL=ITEMS.register("vegetable_skillet",()->new WorkMealItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0).build())));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PrototypeStoveEntity>> ENTITY=ENTITIES.register("prototype_stove",()->BlockEntityType.Builder.of(PrototypeStoveEntity::new,STOVE.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<PrototypeStoveMenu>> MENU=MENUS.register("prototype_stove",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new PrototypeStoveMenu(id,inv,buf.readBlockPos())));
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);COMPONENTS.register(bus);SOUNDS.register(bus);bus.addListener(StoveDialPayload::register);}
    public static ItemStack meal(double quality,double budget){var s=MEAL.toStack(4);s.set(QUALITY.get(),Math.clamp(quality,0,1));s.set(CALORIES.get(),Math.max(0,budget)*StoveCooking.retained(quality)/4);return s;}
}
