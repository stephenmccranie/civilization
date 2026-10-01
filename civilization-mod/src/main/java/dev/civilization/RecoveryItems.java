package dev.civilization;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RecoveryItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Civilization.MOD_ID);
    public static final DeferredItem<Item> MORSEL = ITEMS.registerSimpleItem("foraged_morsel",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0).build()));

    private RecoveryItems() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
