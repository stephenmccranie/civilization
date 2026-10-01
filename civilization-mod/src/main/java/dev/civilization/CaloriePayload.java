package dev.civilization;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CaloriePayload(double calories, double capacity, double sprintMinimum, boolean depleted, double recovery) implements CustomPacketPayload {
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
        event.registrar("3").playToClient(TYPE, CODEC, (payload, context) ->
                CalorieFoodData.of(context.player()).receive(payload.calories, payload.capacity, payload.sprintMinimum, payload.depleted, payload.recovery));
    }
}
