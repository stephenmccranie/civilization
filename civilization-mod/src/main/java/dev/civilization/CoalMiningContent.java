package dev.civilization;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Creative/admin prototype; acquisition and replacement of the drill come later. */
public final class CoalMiningContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"civilization");
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"civilization");
    private static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,"civilization");
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> LOAD=COMPONENTS.registerComponentType("raw_coal_load",b->b.persistent(Codec.intRange(0,4096)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredBlock<CoalWorkfaceBlock> FACE=BLOCKS.register("coal_workface",CoalWorkfaceBlock::new);
    public static final DeferredBlock<RawCoalPileBlock> PILE=BLOCKS.register("raw_coal_pile",RawCoalPileBlock::new);
    public static final DeferredBlock<CoalScreenBlock> SCREEN=BLOCKS.register("coal_preparation_screen",CoalScreenBlock::new);
    public static final DeferredItem<BlockItem> FACE_ITEM=ITEMS.registerSimpleBlockItem(FACE);
    public static final DeferredItem<BlockItem> SCREEN_ITEM=ITEMS.registerSimpleBlockItem(SCREEN);
    public static final DeferredItem<CoalPickItem> PICK=ITEMS.register("coal_pick",CoalPickItem::new);
    public static final DeferredItem<CoalShovelItem> SHOVEL=ITEMS.register("coal_loading_shovel",CoalShovelItem::new);
    public static final DeferredItem<CoalCartItem> CART_ITEM=ITEMS.register("coal_minecart",CoalCartItem::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CoalWorkfaceEntity>> FACE_ENTITY=BLOCK_ENTITIES.register("coal_workface",()->BlockEntityType.Builder.of(CoalWorkfaceEntity::new,FACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CoalScreenEntity>> SCREEN_ENTITY=BLOCK_ENTITIES.register("coal_preparation_screen",()->BlockEntityType.Builder.of(CoalScreenEntity::new,SCREEN.get()).build(null));
    public static final DeferredHolder<EntityType<?>,EntityType<LooseCoalEntity>> LOOSE=ENTITIES.register("loose_coal",()->EntityType.Builder.<LooseCoalEntity>of(LooseCoalEntity::new,MobCategory.MISC).sized(.22f,.22f).clientTrackingRange(8).updateInterval(3).build("civilization:loose_coal"));
    public static final DeferredHolder<EntityType<?>,EntityType<CoalMinecart>> CART=ENTITIES.register("coal_minecart",()->EntityType.Builder.<CoalMinecart>of(CoalMinecart::new,MobCategory.MISC).sized(.98f,.7f).clientTrackingRange(8).updateInterval(3).build("civilization:coal_minecart"));
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);BLOCK_ENTITIES.register(bus);ENTITIES.register(bus);COMPONENTS.register(bus);bus.addListener(CoalPickPayload::register);}
    public static int load(ItemStack stack){return stack.getOrDefault(LOAD.get(),0);}
    public static void load(ItemStack stack,int n){if(n<=0)stack.remove(LOAD.get());else stack.set(LOAD.get(),n);}
}
