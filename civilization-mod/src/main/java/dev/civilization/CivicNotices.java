package dev.civilization;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "civilization")
public final class CivicNotices {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var d = CivicData.get(player.server); d.knownPlayers.put(player.getUUID(), player.getGameProfile().getName()); d.setDirty(); long now = System.currentTimeMillis();
        for (var c : d.claims.values()) {
            d.settle(c, now);
            if (c.buyer != null && (d.owns(c.owner, player.getUUID()) || d.owns(c.buyer, player.getUUID())))
                player.sendSystemMessage(Component.literal("Land handover at " + c.at.pos().toShortString() + " (" + c.at.dimension()
                        + ") in " + Math.max(1, (c.handoverAt - now + 3599999) / 3600000) + " hours. Keep the controller powered."));
        }
        for (var p : d.parcels) if (d.owns(p.recipient(), player.getUUID())) player.sendSystemMessage(Component.literal(
                "Collect " + p.coal() + " Coal at " + p.at().pos().toShortString() + " (" + p.at().dimension() + "). Use the controller or /civilization land nearby."));
    }
}
