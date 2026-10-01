package dev.civilization;

import java.util.Set;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Retired IDs remain registered so existing inventories and worlds still load. */
public final class VanillaRetirement {
    private VanillaRetirement() {}
    public static final Set<Item> ITEMS = Set.of(Items.COAL, Items.CHARCOAL, Items.COAL_BLOCK,
            Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.FURNACE_MINECART);
    public static boolean retired(ItemStack stack) { return ITEMS.contains(stack.getItem()); }
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        for (var item : ITEMS) event.remove(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
