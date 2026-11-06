package dev.civilization;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The Pallets and parked wheelbarrows a player could open from where they stand, beside every menu that shows the Hand
 * except the player's own inventory: one list of their goods, merged by item, nearest store first. The server finds the
 * candidates once when the menu opens, never loading chunks, rechecks each one's access on every use and sends the list
 * whenever it changes; the client keeps the last list it received.
 */
public final class NearbyStores implements CargoInventory {
    private record Seen(CargoInventory cargo,long revision){}
    private final Player player;
    private final List<CargoHolder> candidates;
    /** The store whose own menu this is, never listed beside itself. */
    private final CargoHolder own;
    private List<Seen> seen=List.of();
    /** The stores as last sent to the client, which the next sync compares against. */
    private List<Seen> sent;
    private List<CargoLedger.Entry> entries;
    private int stores;
    private NearbyStores(Player player,List<CargoHolder> candidates,CargoHolder own){this.player=player;this.candidates=candidates;this.own=own;entries=List.of();}
    private NearbyStores(int stores,List<CargoLedger.Entry> entries){this(null,List.of(),null);this.stores=stores;this.entries=entries;}
    public static void register(IEventBus bus){bus.addListener(NearbyStores::payloads);}

    /** The menu's nearby stores, or null where none are shown. */
    public static NearbyStores of(AbstractContainerMenu menu){return ((BlockCarryMenuAccess)menu).civilization$nearby();}
    /** Menus that show the Hand; the player's own inventory opens without the server knowing, so it has none. */
    public static void attach(AbstractContainerMenu menu,ServerPlayer p){
        if(menu instanceof InventoryMenu||BlockCarry.handSlot(menu)==null||of(menu)!=null)return;
        ((BlockCarryMenuAccess)menu).civilization$setNearby(new NearbyStores(p,CargoHolder.around(p.level(),p.position()),menu instanceof CargoMenu c?c.holder():null));
    }
    /** How many stores are listed; the panel shows while there is at least one, even an empty one to stack goods onto. */
    public int stores(){return stores;}

    /** Rechecks which stores the player may use now and merges their goods when anything changed since the last look. */
    private void refresh(){
        var owned=own==null?null:own.cargo();var now=new ArrayList<Seen>();
        for(var store:candidates)if(store.cargoValid(player)){
            var cargo=store.cargo();if(cargo!=owned&&now.stream().noneMatch(s->s.cargo==cargo))now.add(new Seen(cargo,cargo.revision()));
        }
        if(now.equals(seen))return;
        seen=List.copyOf(now);stores=seen.size();
        var merged=new ArrayList<CargoLedger.Entry>();
        for(var s:seen)for(int i=0;i<s.cargo.size();i++){
            var e=s.cargo.entry(i);int at=0;while(at<merged.size()&&!ItemStack.isSameItemSameComponents(merged.get(at).item(),e.item()))at++;
            if(at==merged.size())merged.add(e);else merged.set(at,new CargoLedger.Entry(merged.get(at).item(),merged.get(at).count()+e.count()));
        }
        entries=List.copyOf(merged);
    }
    /**
     * Sends the list when it differs from the last one sent, from the menu's own synchronization each tick. Client menus
     * also synchronize (anvils and stonecutters, for one); their received copy has no player and sends nothing.
     */
    public void sync(int containerId){
        if(!(player instanceof ServerPlayer p))return;
        refresh();if(seen.equals(sent))return;
        sent=seen;PacketDistributor.sendToPlayer(p,new Sync(containerId,stores,entries));
    }

    @Override public double capacity(){return seen.stream().mapToDouble(s->s.cargo.capacity()).sum();}
    @Override public double load(){return seen.stream().mapToDouble(s->s.cargo.load()).sum();}
    @Override public int size(){return entries.size();}
    @Override public long revision(){return seen.hashCode();}
    @Override public long count(int index){return entries.get(index).count();}
    @Override public CargoLedger.Entry entry(int index){return entries.get(index);}
    /** Onto the nearest stores first; a Pallet group sorts what it takes by item. */
    @Override public long insert(ItemStack item,long offered){
        refresh();long moved=0;
        for(var s:seen)if(moved<offered)moved+=s.cargo.insert(item,offered-moved);
        refresh();return moved;
    }
    @Override public long remove(int index,long requested){
        refresh();return index<0||index>=entries.size()?0:take(entries.get(index).item(),requested);
    }
    /** Up to requested of the item, from the nearest stores holding it first, resolved against the stores as they are now. */
    public long take(ItemStack item,long requested){
        refresh();long taken=0;
        for(var s:seen)if(taken<requested){int at=s.cargo.indexOf(item);if(at>=0)taken+=s.cargo.remove(at,requested-taken);}
        refresh();return taken;
    }

    /** The list for the client's panel and its recipe checks. */
    public record Sync(int containerId,int stores,List<CargoLedger.Entry> entries) implements CustomPacketPayload {
        public static final Type<Sync> TYPE=new Type<>(ResourceLocation.parse("civilization:nearby_stores"));
        private static final StreamCodec<RegistryFriendlyByteBuf,CargoLedger.Entry> ENTRY=StreamCodec.composite(
                ItemStack.STREAM_CODEC.map(i->i,i->i.copyWithCount(1)),CargoLedger.Entry::item,ByteBufCodecs.VAR_LONG,CargoLedger.Entry::count,CargoLedger.Entry::new);
        static final StreamCodec<RegistryFriendlyByteBuf,Sync> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Sync::containerId,ByteBufCodecs.VAR_INT,Sync::stores,
                ENTRY.apply(ByteBufCodecs.list()),Sync::entries,Sync::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** A click on the panel: an entry's item, or none for an empty cell; mouse button; Shift held. */
    public record Click(int containerId,ItemStack item,int button,boolean quick) implements CustomPacketPayload {
        public static final Type<Click> TYPE=new Type<>(ResourceLocation.parse("civilization:nearby_click"));
        static final StreamCodec<RegistryFriendlyByteBuf,Click> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Click::containerId,
                ItemStack.OPTIONAL_STREAM_CODEC.map(i->i,i->i.copyWithCount(1)),Click::item,ByteBufCodecs.VAR_INT,Click::button,ByteBufCodecs.BOOL,Click::quick,Click::new);
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static void payloads(RegisterPayloadHandlersEvent e){
        var r=e.registrar("1");
        r.playToClient(Sync.TYPE,Sync.CODEC,(sync,context)->{
            var menu=context.player().containerMenu;
            if(menu.containerId==sync.containerId())((BlockCarryMenuAccess)menu).civilization$setNearby(new NearbyStores(sync.stores(),sync.entries()));
        });
        r.playToServer(Click.TYPE,Click.CODEC,(click,context)->{
            if(!(context.player() instanceof ServerPlayer p))return;
            var menu=p.containerMenu;var near=menu.containerId==click.containerId()?of(menu):null;if(near==null||near.player!=p)return;
            near.refresh();
            CargoMenu.click(menu,p,near,click.item().isEmpty()?-1:near.indexOf(click.item()),click.button(),click.quick());
            menu.broadcastChanges();
        });
    }
}
