package dev.civilization;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.LoggerFactory;

/**
 * The lines rivers run along, for clients to draw their water flowing downstream. On a Terrain Diffusion world each
 * player gets the rivers within {@link #REACH} blocks when they arrive and again once they have moved {@link #MOVE}
 * blocks. The lines are read off the server thread, since a river region may still be building.
 */
@EventBusSubscriber(modid = "civilization")
public final class RiverFlow {
    private RiverFlow() {}
    /** Floats per point of a line: block x, block z, blocks of river above the point, half-width in blocks. */
    public static final int POINT = 4;
    private static final int REACH = 192, MOVE = 64;
    // Most points a payload may carry
    private static final int MAX_POINTS = 16384;
    private record Centre(ServerLevel level, int x, int z) {}
    // Where each player's last lines were read around
    private static final Map<UUID, Centre> SENT = new ConcurrentHashMap<>();
    private static final ExecutorService FETCH = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "Civilization rivers");
        thread.setDaemon(true);
        return thread;
    });

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0
                || !TerrainDiffusion.supported(level.getChunkSource().getGenerator().getBiomeSource())) return;
        for (var player : level.players()) {
            var last = SENT.get(player.getUUID());
            int x = player.getBlockX(), z = player.getBlockZ();
            if (last != null && last.level == level && (long) (x - last.x) * (x - last.x) + (long) (z - last.z) * (z - last.z) < MOVE * MOVE) continue;
            var centre = new Centre(level, x, z);
            UUID id = player.getUUID();
            SENT.put(id, centre);
            var server = level.getServer();
            FETCH.execute(() -> {
                if (SENT.get(id) != centre) return;
                float[][] lines;
                try {
                    lines = TerrainDiffusion.riverLines(x, z, REACH);
                } catch (RuntimeException ex) {
                    LoggerFactory.getLogger(RiverFlow.class).error("River lines unavailable around {}, {}; that water stays still", x, z, ex);
                    return;
                }
                server.execute(() -> {
                    var to = server.getPlayerList().getPlayer(id);
                    if (to != null && SENT.get(id) == centre) PacketDistributor.sendToPlayer(to, new Lines(lines));
                });
            });
        }
    }

    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent event) { SENT.remove(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void travelled(PlayerEvent.PlayerChangedDimensionEvent event) { SENT.remove(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { SENT.clear(); }

    /** The river lines around a player, replacing those they had. */
    public record Lines(float[][] lines) implements CustomPacketPayload {
        public static final Type<Lines> TYPE = new Type<>(ResourceLocation.parse("civilization:river_lines"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, new StreamCodec<RegistryFriendlyByteBuf, Lines>() {
                public Lines decode(RegistryFriendlyByteBuf buf) {
                    float[][] lines = new float[buf.readVarInt()][];
                    int points = 0;
                    for (int k = 0; k < lines.length; k++) {
                        int n = buf.readVarInt();
                        points += n;
                        if (n < 2 || points > MAX_POINTS) throw new IllegalArgumentException("River line points");
                        float[] line = lines[k] = new float[n * POINT];
                        for (int f = 0; f < line.length; f++) {
                            line[f] = buf.readFloat();
                            if (!Float.isFinite(line[f])) throw new IllegalArgumentException("River line value");
                        }
                    }
                    return new Lines(lines);
                }

                public void encode(RegistryFriendlyByteBuf buf, Lines payload) {
                    buf.writeVarInt(payload.lines.length);
                    for (float[] line : payload.lines) {
                        buf.writeVarInt(line.length / POINT);
                        for (float f : line) buf.writeFloat(f);
                    }
                }
            }, (payload, context) -> dev.civilization.client.PhotonRivers.accept(context.player().level(), payload.lines));
        }
    }
}
