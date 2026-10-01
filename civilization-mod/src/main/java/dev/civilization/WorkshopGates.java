package dev.civilization;

import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;

@EventBusSubscriber(modid="civilization")
public final class WorkshopGates {
    public static boolean disabledTrade(net.minecraft.world.item.ItemStack stack){
        if(stack.isDamageableItem()||WorkshopJobs.gated(stack))return true;
        var enchants=net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(stack);
        return enchants.keySet().stream().anyMatch(e->e.is(net.minecraft.world.item.enchantment.Enchantments.MENDING));
    }
    @SubscribeEvent public static void anvil(AnvilUpdateEvent e){
        // Naming and enchanted books are still allowed; physical repair/combining belongs in the smithy.
        if(WorkshopJobs.gated(e.getLeft())&&!e.getRight().isEmpty()&&!e.getRight().is(Items.ENCHANTED_BOOK))e.setCanceled(true);
    }
    @SubscribeEvent public static void grindstone(GrindstoneEvent.OnPlaceItem e){
        if(!e.getTopItem().isEmpty()&&!e.getBottomItem().isEmpty()&&(WorkshopJobs.gated(e.getTopItem())||WorkshopJobs.gated(e.getBottomItem())))e.setCanceled(true);
    }
}
