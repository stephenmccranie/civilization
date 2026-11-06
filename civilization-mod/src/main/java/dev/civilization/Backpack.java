package dev.civilization;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.*;

/**
 * A worn pack: eight slots of ordinary stacks, with building blocks among them up to a cap.
 * Its contents are the item's own container contents. Bound to a wheelbarrow or Pallet, it keeps the hand and the
 * pack's block stacks topped up from that store after each placement while the player builds within reach of it.
 * Every menu that shows the Hand, the player's own inventory included, shows the pack's slots beside it.
 */
@EventBusSubscriber(modid="civilization")
public final class Backpack {
    /** Every pack's slots, two rows beside a menu, and the building blocks they hold in all. */
    public static final int SLOTS=8,BLOCKS=24;
    /** How far from the bound store a player builds from it. */
    public static final double RADIUS=16;
    /** The bound store, a Pallet's position or a wheelbarrow's identity in one dimension. A bound pack builds from it. */
    public record Supply(ResourceKey<Level> dimension,Either<BlockPos,UUID> store){
        private static final Codec<Supply> CODEC=RecordCodecBuilder.create(i->i.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Supply::dimension),
                Codec.either(BlockPos.CODEC,UUIDUtil.CODEC).fieldOf("store").forGetter(Supply::store)).apply(i,Supply::new));
        private static final StreamCodec<ByteBuf,Supply> STREAM=StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION),Supply::dimension,
                ByteBufCodecs.either(BlockPos.STREAM_CODEC,UUIDUtil.STREAM_CODEC),Supply::store,Supply::new);
        public String storeName(){return store.left().isPresent()?"Pallet":"wheelbarrow";}
    }
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,"civilization");
    public static final DeferredItem<AccessoryItem> ITEM=ITEMS.register("backpack",()->new AccessoryItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Supply>> SUPPLY=COMPONENTS.registerComponentType("backpack_supply",b->b.persistent(Supply.CODEC).networkSynchronized(Supply.STREAM));
    private Backpack(){}
    public static void register(IEventBus bus){ITEMS.register(bus);COMPONENTS.register(bus);bus.addListener(Unbind::register);}

    /** The live contents of the backpack the player wears, or null. */
    public static Contents worn(Player p){return p.getData(Accessories.INVENTORY).backpack();}

    /** A backpack's slots, written back to the item on every change. Building blocks among them stay within its cap. */
    public static final class Contents extends SimpleContainer {
        private final ItemStack pack;
        Contents(ItemStack pack){
            // Never fewer slots than saved stacks, so no contents are ever cut off.
            super(Math.max(SLOTS,(int)pack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().count()));
            this.pack=pack;pack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(getItems());
        }
        public ItemStack pack(){return pack;}
        @Override public void setChanged(){super.setChanged();pack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(getItems()));}
        /** How many more building blocks the pack takes. */
        public int blockRoom(){int n=0;for(var s:getItems())if(BlockCarry.carried(s))n+=s.getCount();return Math.max(0,BLOCKS-n);}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return !stack.is(ITEM)&&!BlockCarry.filledContainer(stack)&&(!BlockCarry.carried(stack)||blockRoom()>0);}
        /** The most of `stack` that a slot holds, building blocks counting against the cap. */
        public int limit(int slot,ItemStack stack){
            int max=Math.min(getMaxStackSize(),stack.getMaxStackSize());var held=getItem(slot);
            return BlockCarry.carried(stack)?Math.min(max,(ItemStack.isSameItemSameComponents(held,stack)?held.getCount():0)+blockRoom()):max;
        }
        /** Puts what fits of `stack` onto matching stacks, then into empty slots; `stack` keeps the rest. */
        public void insert(ItemStack stack){
            if(!canPlaceItem(0,stack))return;
            for(int pass=0;pass<2;pass++)for(int i=0;i<getContainerSize()&&!stack.isEmpty();i++){
                var held=getItem(i);if(held==stack||(pass==0?held.isEmpty()||!Grade.stacks(held,stack):!held.isEmpty()))continue;
                int n=Math.min(stack.getCount(),limit(i,stack)-held.getCount());if(n<=0)continue;
                if(held.isEmpty())setItem(i,stack.split(n));else{Grade.join(held,stack,n);setChanged();}
            }
        }
        /** Takes up to `n` of the item, from the last matching stack first. */
        public int take(ItemStack item,int n){
            int taken=0;
            for(int i=getContainerSize()-1;i>=0&&taken<n;i--)if(ItemStack.isSameItemSameComponents(getItem(i),item))taken+=removeItem(i,n-taken).getCount();
            return taken;
        }
    }

    /**
     * The pack the player wears now, seen through a fixed number of slots, so a menu's slot list never changes when a
     * pack is put on or taken off. Slots beyond the worn pack hold nothing and take nothing.
     */
    static final class View implements Container {
        private final Player player;
        View(Player player){this.player=player;}
        private Contents pack(int slot){var pack=worn(player);return pack!=null&&slot<pack.getContainerSize()?pack:null;}
        @Override public int getContainerSize(){return SLOTS;}
        @Override public boolean isEmpty(){var pack=worn(player);return pack==null||pack.isEmpty();}
        @Override public ItemStack getItem(int slot){var pack=pack(slot);return pack==null?ItemStack.EMPTY:pack.getItem(slot);}
        @Override public ItemStack removeItem(int slot,int n){var pack=pack(slot);return pack==null?ItemStack.EMPTY:pack.removeItem(slot,n);}
        @Override public ItemStack removeItemNoUpdate(int slot){var pack=pack(slot);return pack==null?ItemStack.EMPTY:pack.removeItemNoUpdate(slot);}
        @Override public void setItem(int slot,ItemStack stack){var pack=pack(slot);if(pack!=null)pack.setItem(slot,stack);}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){var pack=pack(slot);return pack!=null&&pack.canPlaceItem(slot,stack);}
        @Override public void setChanged(){var pack=worn(player);if(pack!=null)pack.setChanged();}
        @Override public boolean stillValid(Player p){return true;}
        @Override public void clearContent(){}
    }
    /** A slot of the worn pack beside a menu; building blocks stay within its cap. */
    public static final class PackSlot extends Slot {
        PackSlot(View pack,int index){super(pack,index,0,0);}
        @Override public boolean isActive(){return worn(((View)container).player)!=null;}
        @Override public boolean mayPlace(ItemStack stack){return container.canPlaceItem(getContainerSlot(),stack);}
        @Override public int getMaxStackSize(ItemStack stack){var pack=worn(((View)container).player);return pack==null?0:pack.limit(getContainerSlot(),stack);}
    }
    /** Every menu that shows the Hand shows the pack's slots after it, on server and client alike, so packet indices match. */
    public static void attachMenu(AbstractContainerMenu menu,Player p){
        if(BlockCarry.handSlot(menu)==null)return;
        for(var slot:menu.slots)if(slot instanceof PackSlot)return;
        var view=new View(p);
        for(int i=0;i<SLOTS;i++)((BlockCarryMenuAccess)menu).civilization$addCarrySlot(new PackSlot(view,i));
    }

    /** The bound store, if it is in the player's dimension and loaded: a Pallet by its position, a wheelbarrow among loaded entities. */
    static CargoHolder store(ServerPlayer p,Supply s){
        if(s==null||p.level().dimension()!=s.dimension())return null;
        var level=p.serverLevel();
        return s.store().<CargoHolder>map(at->Chunks.loaded(level,at)&&level.getBlockEntity(at) instanceof StockpileEntity e?e:null,id->level.getEntity(id) instanceof WheelbarrowEntity w?w:null);
    }
    private static Either<BlockPos,UUID> address(CargoHolder store){return store instanceof WheelbarrowEntity w?Either.right(w.getUUID()):Either.left(BlockPos.containing(store.cargoPosition()));}
    /** The bound store's goods while the player may use the store from where they stand, otherwise null. */
    static CargoInventory supplying(ServerPlayer p,Contents pack){
        var s=pack.pack().get(SUPPLY);if(s==null)return null;
        var store=store(p,s);return store!=null&&store.cargoValid(p,RADIUS)?store.cargo():null;
    }
    /** Whether the pack the player wears is bound to this store: 0 without a pack, 1 elsewhere or unbound, 2 here. */
    static int binding(Player p,CargoHolder store){
        var pack=worn(p);if(pack==null)return 0;var s=pack.pack().get(SUPPLY);
        return s!=null&&s.dimension()==p.level().dimension()&&s.store().equals(address(store))?2:1;
    }
    /** Binds the worn pack to the store, or unbinds it when it is already bound there. */
    static void bind(ServerPlayer p,CargoHolder store){
        var pack=worn(p);if(pack==null)return;
        if(binding(p,store)==2){unbind(p);return;}
        var s=new Supply(p.level().dimension(),address(store));pack.pack().set(SUPPLY,s);
        p.displayClientMessage(Component.literal("Backpack bound to this "+s.storeName()+": building within "+(int)RADIUS+" blocks draws from it"),true);
    }
    static void unbind(ServerPlayer p){
        var pack=worn(p);if(pack==null||pack.pack().remove(SUPPLY)==null)return;
        p.displayClientMessage(Component.literal("Backpack unbound"),true);
    }
    /** Why the bound store supplies nothing more of the item; null when the pack is not bound. */
    private static String shortage(ServerPlayer p,Contents pack,ItemStack item){
        var s=pack.pack().get(SUPPLY);if(s==null)return null;
        String name="bound "+s.storeName();var store=store(p,s);
        if(store==null)return "The "+name+" is not nearby";
        if(store.cargoPosition().distanceTo(p.position())>RADIUS)return "Out of reach of the "+name+": come within "+(int)RADIUS+" blocks";
        if(!store.cargoValid(p,RADIUS))return store instanceof WheelbarrowEntity w&&w.isVehicle()?"The "+name+" is being pushed":"You may not use the "+name;
        return "The "+name+" has no more "+item.getHoverName().getString();
    }

    private static final Map<UUID,ItemStack> HAND=new HashMap<>();
    private static final Set<UUID> BUILT=new HashSet<>();
    /** Every placement route, hand, saw cut, controller feed or Builder's Line, fires this event. */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent e){if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p&&BlockCarry.restricted(p))BUILT.add(p.getUUID());}
    /** Refills after this tick's placements, then remembers the hand's block for when it runs out. */
    @SubscribeEvent(priority=EventPriority.LOW) public static void tick(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(BUILT.remove(p.getUUID()))refill(p,HAND.get(p.getUUID()));
        var hand=p.getOffhandItem();if(BlockCarry.carried(hand))HAND.put(p.getUUID(),hand.copyWithCount(1));
    }
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent e){HAND.remove(e.getEntity().getUUID());BUILT.remove(e.getEntity().getUUID());}
    /**
     * Tops the hand back up with the block it holds or last held, from the bound store and then the pack, and tops up
     * the pack's own block stacks from the store. The pack never takes on a block it does not already hold.
     */
    static void refill(ServerPlayer p,ItemStack last){
        var pack=worn(p);if(pack==null)return;
        var inv=p.getInventory();var hand=inv.getItem(BlockCarry.OFFHAND);var want=hand.isEmpty()?last:hand;
        var store=supplying(p,pack);
        if(want!=null&&BlockCarry.carried(want)){
            int room=Math.min(BlockCarry.LIMIT,want.getMaxStackSize())-hand.getCount(),got=0;
            if(store!=null){int at=store.indexOf(want);if(at>=0)got=(int)store.remove(at,room);}
            got+=pack.take(want,room-got);
            if(got>0){if(hand.isEmpty())inv.setItem(BlockCarry.OFFHAND,want.copyWithCount(got));else hand.grow(got);inv.setChanged();}
        }
        if(store!=null)for(int i=0;i<pack.getContainerSize();i++){
            var held=pack.getItem(i);if(!BlockCarry.carried(held))continue;
            int at=store.indexOf(held);if(at<0)continue;
            int n=(int)store.remove(at,pack.limit(i,held)-held.getCount());if(n>0){held.grow(n);pack.setChanged();}
        }
        if(want!=null&&inv.getItem(BlockCarry.OFFHAND).isEmpty()){var why=shortage(p,pack,want);if(why!=null)p.displayClientMessage(Component.literal(why),true);}
    }

    /** The Pack panel's Unbind button. */
    public record Unbind() implements CustomPacketPayload {
        public static final Type<Unbind> TYPE=new Type<>(ResourceLocation.parse("civilization:backpack_unbind"));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
        static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,StreamCodec.unit(new Unbind()),(payload,context)->{if(context.player() instanceof ServerPlayer p)unbind(p);});}
    }
}
