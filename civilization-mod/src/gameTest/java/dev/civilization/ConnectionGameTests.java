package dev.civilization;

import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ConnectionGameTests {
    @GameTest(template = "industrial")
    public static void requiredClientProtocolNegotiation(GameTestHelper h) throws ReflectiveOperationException {
        // Inspect the actual startup registration, then run NeoForge's real pre-play negotiation.
        var field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var registrations = (Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>>) field.get(null);
        var server = new NegotiableNetworkComponent(registrations.get(ConnectionProtocol.PLAY).get(CaloriePayload.TYPE.id()));
        h.assertTrue(!server.optional() && server.version().equals(CaloriePayload.PROTOCOL_VERSION), "The core channel requires the stable protocol, not the running build");
        var required = List.of(server);
        h.assertTrue(NetworkComponentNegotiator.negotiate(required, required).success(), "The matching client is accepted");
        var published = new NegotiableNetworkComponent(server.id(), "15/0.35.32-dev", server.flow(), false);
        h.assertTrue(NetworkComponentNegotiator.negotiate(required, List.of(published)).success(), "The unchanged published 0.35.32 client connects to later compatible server builds");
        h.assertTrue(!NetworkComponentNegotiator.negotiate(required, List.of()).success(), "A missing mod is rejected before play");
        for (String version : List.of("14/0.35.31-dev", "13/0.35.30-dev", "12/0.35.27-dev", "11/0.35.26-dev", "10/0.35.23-dev", "9/0.35.21-dev", "8/0.35.18-dev", "7/0.35.17-dev", "6/0.35.16-dev", "5/0.35.15-dev", "4/0.35.14-dev", "3/0.35.0-dev", "3", "3/0.34.55-dev", "4")) {
            var client = new NegotiableNetworkComponent(server.id(), version, server.flow(), false);
            var result = NetworkComponentNegotiator.negotiate(required, List.of(client));
            h.assertTrue(!result.success() && result.failureReasons().containsKey(server.id()), "Legacy and incompatible protocols are rejected with a channel reason");
        }
        h.succeed();
    }
}
