package dev.civilization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record TradePayload(int menuId,int button,int revision,int amount,int price) implements CustomPacketPayload {
    public TradePayload(int menuId,int button,int revision) { this(menuId,button,revision,0,0); }
    public static final Type<TradePayload> TYPE = new Type<>(ResourceLocation.parse("civilization:trade_action"));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(TYPE,new StreamCodec<RegistryFriendlyByteBuf,TradePayload>() {
            public TradePayload decode(RegistryFriendlyByteBuf b) { return new TradePayload(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()); }
            public void encode(RegistryFriendlyByteBuf b,TradePayload p) { b.writeVarInt(p.menuId); b.writeVarInt(p.button); b.writeVarInt(p.revision); b.writeVarInt(p.amount); b.writeVarInt(p.price); }
        },(p,c)-> { if(c.player().containerMenu instanceof CivicMenu m && m.containerId == p.menuId) { if(p.button==20) m.quantities(p.amount,p.price,p.revision); else m.action(p.button,p.revision); } });
    }
}
