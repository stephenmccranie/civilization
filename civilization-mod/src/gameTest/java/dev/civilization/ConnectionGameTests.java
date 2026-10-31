package dev.civilization;

import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ConnectionGameTests {
    @GameTest(template = "industrial")
    public static void requiredClientBuildNegotiation(GameTestHelper h) throws ReflectiveOperationException {
        // Inspect the actual startup registration, then run NeoForge's real pre-play negotiation.
        var field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var registrations = (Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>>) field.get(null);
        var server = new NegotiableNetworkComponent(registrations.get(ConnectionProtocol.PLAY).get(CaloriePayload.TYPE.id()));
        String build = ModList.get().getModContainerById(Civilization.MOD_ID).orElseThrow().getModInfo().getVersion().toString();
        h.assertTrue(!server.optional() && server.version().equals("3/" + build), "The core channel requires the running build");
        var required = List.of(server);
        h.assertTrue(NetworkComponentNegotiator.negotiate(required, required).success(), "The matching client is accepted");
        h.assertTrue(!NetworkComponentNegotiator.negotiate(required, List.of()).success(), "A missing mod is rejected before play");
        for (String version : List.of("3", "3/older-build")) {
            var client = new NegotiableNetworkComponent(server.id(), version, server.flow(), false);
            var result = NetworkComponentNegotiator.negotiate(required, List.of(client));
            h.assertTrue(!result.success() && result.failureReasons().containsKey(server.id()), "Legacy and different builds are rejected with a channel reason");
        }
        h.succeed();
    }
}
