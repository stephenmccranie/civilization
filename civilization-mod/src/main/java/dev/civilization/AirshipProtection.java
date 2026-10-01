package dev.civilization;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Fixed assembly geometry for the first flight prototype. */
@EventBusSubscriber(modid="civilization")
public final class AirshipProtection {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.RightClickBlock e){if(e.getLevel() instanceof ServerLevel l){var s=AirshipSystem.at(l,e.getPos());if(s!=null&&!AirshipSystem.owner(s,e.getEntity()))e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breaking(BlockEvent.BreakEvent e){if(e.getLevel() instanceof ServerLevel l&&AirshipSystem.at(l,e.getPos())!=null)e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void placing(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel l&&AirshipSystem.at(l,e.getPos())!=null)e.setCanceled(true);}
}
