package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class LandPayload {
    public record State(int menuId, String status, String details, String owner, List<String> whitelist,
                        int flags, int tier, String buyLabel, String notice, String tierStatus) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.parse("civilization:land_state"));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Access(int menuId, String username, boolean remove) implements CustomPacketPayload {
        public static final Type<Access> TYPE = new Type<>(ResourceLocation.parse("civilization:land_access"));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(State.TYPE, new StreamCodec<RegistryFriendlyByteBuf, State>() {
            @Override public State decode(RegistryFriendlyByteBuf b) {
                int id = b.readVarInt(); String status = b.readUtf(256), details = b.readUtf(256), owner = b.readUtf(64);
                int size = b.readVarInt(); if (size < 0 || size > 1024) throw new IllegalArgumentException("Invalid whitelist length");
                var names = new ArrayList<String>(); for (int i = 0; i < size; i++) names.add(b.readUtf(64));
                return new State(id, status, details, owner, List.copyOf(names), b.readVarInt(), b.readVarInt(), b.readUtf(128), b.readUtf(256), b.readUtf(128));
            }
            @Override public void encode(RegistryFriendlyByteBuf b, State s) {
                b.writeVarInt(s.menuId); b.writeUtf(s.status, 256); b.writeUtf(s.details, 256); b.writeUtf(s.owner, 64);
                b.writeVarInt(s.whitelist.size()); s.whitelist.forEach(name -> b.writeUtf(name, 64));
                b.writeVarInt(s.flags); b.writeVarInt(s.tier); b.writeUtf(s.buyLabel, 128); b.writeUtf(s.notice, 256); b.writeUtf(s.tierStatus, 128);
            }
        }, (state, context) -> { if (context.player().containerMenu instanceof LandMenu menu && menu.containerId == state.menuId) menu.state = state; });
        registrar.playToServer(Access.TYPE, new StreamCodec<RegistryFriendlyByteBuf, Access>() {
            @Override public Access decode(RegistryFriendlyByteBuf b) { return new Access(b.readVarInt(), b.readUtf(16), b.readBoolean()); }
            @Override public void encode(RegistryFriendlyByteBuf b, Access a) { b.writeVarInt(a.menuId); b.writeUtf(a.username, 16); b.writeBoolean(a.remove); }
        }, (access, context) -> { if (context.player().containerMenu instanceof LandMenu menu && menu.containerId == access.menuId) menu.whitelist(access.username, access.remove); });
    }
}
