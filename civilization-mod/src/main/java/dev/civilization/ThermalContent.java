package dev.civilization;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
public final class ThermalContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    public static final DeferredItem<ArmorItem> HELMET=ITEMS.register("thermal_helmet",()->new ArmorItem(ArmorMaterials.IRON,ArmorItem.Type.HELMET,new Item.Properties().durability(165)));
    public static void register(net.neoforged.bus.api.IEventBus bus){ITEMS.register(bus);}
    public static boolean wearing(net.minecraft.world.entity.player.Player p){return p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(HELMET.get());}
}
