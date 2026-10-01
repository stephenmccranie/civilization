package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Nearby chest positions only; inventory contents never travel to clients. */
public record UraniumRadiationPayload(List<BlockPos> chests) implements CustomPacketPayload {
    public static final Type<UraniumRadiationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Civilization.MOD_ID, "uranium_radiation"));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, new StreamCodec<RegistryFriendlyByteBuf, UraniumRadiationPayload>() {
            @Override public UraniumRadiationPayload decode(RegistryFriendlyByteBuf buffer) {
                int count = buffer.readVarInt();
                if (count < 0 || count > 64) throw new IllegalArgumentException("Too many uranium chests");
                var positions = new ArrayList<BlockPos>(count);
                for (int i = 0; i < count; i++) positions.add(buffer.readBlockPos());
                return new UraniumRadiationPayload(List.copyOf(positions));
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, UraniumRadiationPayload packet) {
                buffer.writeVarInt(packet.chests.size());
                for (var pos : packet.chests) buffer.writeBlockPos(pos);
            }
        }, (packet, context) -> dev.civilization.client.UraniumRadiation.accept(packet.chests()));
    }
}
