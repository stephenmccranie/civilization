package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CaloriePayload(double calories, double capacity, double sprintMinimum, boolean depleted, double recovery) implements CustomPacketPayload {
    // Wire compatibility is independent of the release version. Retain 0.35.0's existing ID
    // so its published clients work unchanged; bump only for incompatible network/content changes.
    public static final String PROTOCOL_VERSION = "14/0.35.31-dev";
    public static final Type<CaloriePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("civilization", "calories"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CaloriePayload> CODEC = new StreamCodec<>() {
        @Override public CaloriePayload decode(RegistryFriendlyByteBuf buf) {
            return new CaloriePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readDouble());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, CaloriePayload value) {
            buf.writeDouble(value.calories); buf.writeDouble(value.capacity); buf.writeDouble(value.sprintMinimum);
            buf.writeBoolean(value.depleted); buf.writeDouble(value.recovery);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        // Required by default: missing channels and incompatible protocols are rejected before play.
        event.registrar(PROTOCOL_VERSION).playToClient(TYPE, CODEC, (payload, context) ->
                CalorieFoodData.of(context.player()).receive(payload.calories, payload.capacity, payload.sprintMinimum, payload.depleted, payload.recovery));
    }
}
