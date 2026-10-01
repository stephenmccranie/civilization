package dev.civilization;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "civilization")
public final class CivicCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("civilization").then(Commands.literal("land").executes(ctx -> {
            var p = ctx.getSource().getPlayerOrException(); var d = CivicData.get(p.server); var dim = p.level().dimension().location().toString();
            var at = java.util.stream.Stream.concat(d.claims.keySet().stream(), d.parcels.stream().map(CivicData.Parcel::at))
                    .filter(a -> a.dimension().equals(dim) && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(a.pos())) <= 36)
                    .min(java.util.Comparator.comparingDouble(a -> p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(a.pos())))).orElse(null);
            if (at == null) return say(p, "Stand within six blocks of the controller's location.");
            if (d.claims.containsKey(at)) CivicMenu.open(p, at, true); else say(p, "Collected " + d.collect(at, p) + " Coal from this location.");
            return 1;
        })));
    }
    private static int say(ServerPlayer p, String message) { p.sendSystemMessage(Component.literal(message)); return 1; }
}
