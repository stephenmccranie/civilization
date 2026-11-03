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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.*;

/**
 * A worn pack: grade-scaled slots of ordinary stacks, with building blocks among them up to a grade-scaled cap.
 * Its contents are the item's own container contents. Bound to a wheelbarrow or Pallet, it keeps the hand and the
 * pack's block stacks topped up from that store after each placement while the player builds within reach of it.
 */
@EventBusSubscriber(modid="civilization")
public final class Backpack {
    /** At 100% grade; the grade scales both. */
    public static final int SLOTS=9,BLOCKS=24;
    /** How far from the bound store a player builds from it. */
    public static final double RADIUS=16;
    /** The bound store, a Pallet's position or a wheelbarrow's identity in one dimension, and whether supply is on. */
    public record Supply(ResourceKey<Level> dimension,Either<BlockPos,UUID> store,boolean on){
        private static final Codec<Supply> CODEC=RecordCodecBuilder.create(i->i.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Supply::dimension),
                Codec.either(BlockPos.CODEC,UUIDUtil.CODEC).fieldOf("store").forGetter(Supply::store),
                Codec.BOOL.fieldOf("on").forGetter(Supply::on)).apply(i,Supply::new));
        private static final StreamCodec<ByteBuf,Supply> STREAM=StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION),Supply::dimension,
                ByteBufCodecs.either(BlockPos.STREAM_CODEC,UUIDUtil.STREAM_CODEC),Supply::store,
                ByteBufCodecs.BOOL,Supply::on,Supply::new);
        public String storeName(){return store.left().isPresent()?"Pallet":"wheelbarrow";}
    }
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,"civilization");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"civilization");
    public static final DeferredItem<AccessoryItem> ITEM=ITEMS.register("backpack",()->new AccessoryItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Supply>> SUPPLY=COMPONENTS.registerComponentType("backpack_supply",b->b.persistent(Supply.CODEC).networkSynchronized(Supply.STREAM));
    public static final DeferredHolder<MenuType<?>,MenuType<BackpackMenu>> MENU=MENUS.register("backpack",()->IMenuTypeExtension.create(BackpackMenu::new));
    private Backpack(){}
    public static void register(IEventBus bus){ITEMS.register(bus);COMPONENTS.register(bus);MENUS.register(bus);bus.addListener(Action::register);}

    public static int slots(ItemStack pack){return scaled(SLOTS,pack);}
    public static int blocks(ItemStack pack){return scaled(BLOCKS,pack);}
    private static int scaled(int full,ItemStack pack){return Math.max(0,(int)Math.round(full*EquipmentGrade.value(pack)/100.0));}
    /** The live contents of the backpack the player wears, or null. */
    public static Contents worn(Player p){return p.getData(Accessories.INVENTORY).backpack();}

    /** A backpack's slots, written back to the item on every change. Building blocks among them stay within its cap. */
    public static final class Contents extends SimpleContainer {
        private final ItemStack pack;
        Contents(ItemStack pack){
            // Never fewer slots than saved stacks, so no contents are ever cut off.
            super(Math.max(slots(pack),(int)pack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().count()));
            this.pack=pack;pack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(getItems());
        }
        public ItemStack pack(){return pack;}
        @Override public void setChanged(){super.setChanged();pack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(getItems()));}
        /** How many more building blocks the pack takes. */
        public int blockRoom(){int n=0;for(var s:getItems())if(BlockCarry.block(s))n+=s.getCount();return Math.max(0,blocks(pack)-n);}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return !stack.is(ITEM)&&!BlockCarry.filledContainer(stack)&&(!BlockCarry.block(stack)||blockRoom()>0);}
        /** The most of `stack` that a slot holds, building blocks counting against the cap. */
        public int limit(int slot,ItemStack stack){
            int max=Math.min(getMaxStackSize(),stack.getMaxStackSize());var held=getItem(slot);
            return BlockCarry.block(stack)?Math.min(max,(ItemStack.isSameItemSameComponents(held,stack)?held.getCount():0)+blockRoom()):max;
        }
        /** Puts what fits of `stack` onto matching stacks, then into empty slots; `stack` keeps the rest. */
        public void insert(ItemStack stack){
            if(!canPlaceItem(0,stack))return;
            for(int pass=0;pass<2;pass++)for(int i=0;i<getContainerSize()&&!stack.isEmpty();i++){
                var held=getItem(i);if(pass==0?held.isEmpty()||!ItemStack.isSameItemSameComponents(held,stack):!held.isEmpty())continue;
                int n=Math.min(stack.getCount(),limit(i,stack)-held.getCount());if(n<=0)continue;
                if(held.isEmpty())setItem(i,stack.split(n));else{held.grow(n);stack.shrink(n);setChanged();}
            }
        }
        /** Takes up to `n` of the item, from the last matching stack first. */
        public int take(ItemStack item,int n){
            int taken=0;
            for(int i=getContainerSize()-1;i>=0&&taken<n;i--)if(ItemStack.isSameItemSameComponents(getItem(i),item))taken+=removeItem(i,n-taken).getCount();
            return taken;
        }
    }

    /** The bound store, if it is in the player's dimension and loaded: a Pallet by its position, a wheelbarrow among loaded entities. */
    static CargoHolder store(ServerPlayer p,Supply s){
        if(s==null||p.level().dimension()!=s.dimension())return null;
        var level=p.serverLevel();
        return s.store().<CargoHolder>map(at->level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof StockpileEntity e?e:null,id->level.getEntity(id) instanceof WheelbarrowEntity w?w:null);
    }
    private static Either<BlockPos,UUID> address(CargoHolder store){return store instanceof WheelbarrowEntity w?Either.right(w.getUUID()):Either.left(BlockPos.containing(store.cargoPosition()));}
    /** The bound store's goods while supply is on and the player may use the store from where they stand, otherwise null. */
    static CargoInventory supplying(ServerPlayer p,Contents pack){
        var s=pack.pack().get(SUPPLY);if(s==null||!s.on())return null;
        var store=store(p,s);return store!=null&&store.cargoValid(p,RADIUS)?store.cargo():null;
    }
    /** Whether the pack the player wears is bound to this store: 0 without a pack, 1 elsewhere or unbound, 2 here. */
    static int binding(Player p,CargoHolder store){
        var pack=worn(p);if(pack==null)return 0;var s=pack.pack().get(SUPPLY);
        return s!=null&&s.dimension()==p.level().dimension()&&s.store().equals(address(store))?2:1;
    }
    /** Binds the worn pack to the store with supply on, or unbinds it when it is already bound there. */
    static void bind(ServerPlayer p,CargoHolder store){
        var pack=worn(p);if(pack==null)return;
        if(binding(p,store)==2){unbind(p);return;}
        var s=new Supply(p.level().dimension(),address(store),true);pack.pack().set(SUPPLY,s);
        p.displayClientMessage(Component.literal("Backpack bound to this "+s.storeName()+": building within "+(int)RADIUS+" blocks draws from it"),true);
    }
    static void unbind(ServerPlayer p){
        var pack=worn(p);if(pack==null||pack.pack().remove(SUPPLY)==null)return;
        p.displayClientMessage(Component.literal("Backpack unbound"),true);
    }
    static void toggle(ServerPlayer p){
        var pack=worn(p);if(pack==null){p.displayClientMessage(Component.literal("Wear a backpack to draw supplies"),true);return;}
        var s=pack.pack().get(SUPPLY);
        if(s==null){p.displayClientMessage(Component.literal("Bind the backpack first: open a wheelbarrow or Pallet and press Bind"),true);return;}
        pack.pack().set(SUPPLY,new Supply(s.dimension(),s.store(),!s.on()));
        p.displayClientMessage(Component.literal("Backpack supply "+(s.on()?"off":"on")),true);
    }
    /** Puts the pack's building blocks onto the bound store, as far as it accepts them. */
    static void unload(ServerPlayer p,Contents pack){
        var store=supplying(p,pack);if(store==null){p.displayClientMessage(Component.literal(shortage(p,pack,null)),true);return;}
        long moved=0;
        for(int i=0;i<pack.getContainerSize();i++){
            var held=pack.getItem(i);if(!BlockCarry.block(held))continue;
            int n=(int)store.insert(held,held.getCount());if(n>0){held.shrink(n);moved+=n;pack.setChanged();}
        }
        p.displayClientMessage(Component.literal(moved>0?"Unloaded "+moved+" blocks · "+CargoInventory.blocks(store.load(),store.capacity()):"The bound store takes none of these blocks"),true);
    }
    /** Why the bound store supplies nothing more of the item; null when the pack is not bound. */
    private static String shortage(ServerPlayer p,Contents pack,ItemStack item){
        var s=pack.pack().get(SUPPLY);if(s==null)return item==null?"The backpack is not bound to a store":null;
        String name="bound "+s.storeName();var store=store(p,s);
        if(!s.on())return "Backpack supply is off";
        if(store==null)return "The "+name+" is not nearby";
        if(store.cargoPosition().distanceTo(p.position())>RADIUS)return "Out of reach of the "+name+": come within "+(int)RADIUS+" blocks";
        if(!store.cargoValid(p,RADIUS))return store instanceof WheelbarrowEntity w&&w.isVehicle()?"The "+name+" is being pushed":"You may not use the "+name;
        return item==null?"":"The "+name+" has no more "+item.getHoverName().getString();
    }

    private static final Map<UUID,ItemStack> HAND=new HashMap<>();
    private static final Set<UUID> BUILT=new HashSet<>();
    /** Every placement route, hand, saw cut, controller feed or Builder's Line, fires this event. */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent e){if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p&&BlockCarry.restricted(p))BUILT.add(p.getUUID());}
    /** Refills after this tick's placements, then remembers the hand's block for when it runs out. */
    @SubscribeEvent(priority=EventPriority.LOW) public static void tick(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(BUILT.remove(p.getUUID()))refill(p,HAND.get(p.getUUID()));
        var hand=p.getOffhandItem();if(BlockCarry.block(hand))HAND.put(p.getUUID(),hand.copyWithCount(1));
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
        if(want!=null&&BlockCarry.block(want)){
            int room=Math.min(BlockCarry.LIMIT,want.getMaxStackSize())-hand.getCount(),got=0;
            if(store!=null){int at=store.indexOf(want);if(at>=0)got=(int)store.remove(at,room);}
            got+=pack.take(want,room-got);
            if(got>0){if(hand.isEmpty())inv.setItem(BlockCarry.OFFHAND,want.copyWithCount(got));else hand.grow(got);inv.setChanged();}
        }
        if(store!=null)for(int i=0;i<pack.getContainerSize();i++){
            var held=pack.getItem(i);if(!BlockCarry.block(held))continue;
            int at=store.indexOf(held);if(at<0)continue;
            int n=(int)store.remove(at,pack.limit(i,held)-held.getCount());if(n>0){held.grow(n);pack.setChanged();}
        }
        if(want!=null&&inv.getItem(BlockCarry.OFFHAND).isEmpty()){var why=shortage(p,pack,want);if(why!=null)p.displayClientMessage(Component.literal(why),true);}
    }

    /** Opens the worn pack's menu. */
    static void open(ServerPlayer p){
        var pack=worn(p);if(pack==null){p.displayClientMessage(Component.literal("Wear a backpack in an accessory slot to open it"),true);return;}
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new BackpackMenu(id,inv,pack,status(p,pack)),pack.pack().getHoverName()),buf->buf.writeVarInt(pack.getContainerSize()));
    }
    /** What the menu shows of the binding: 0 none, 1 Pallet, 2 wheelbarrow; supply on; distance to a store found nearby, or -1. */
    private static ContainerData status(ServerPlayer p,Contents pack){return new ContainerData(){
        public int get(int i){
            var s=pack.pack().get(SUPPLY);if(s==null)return i==2?-1:0;
            return switch(i){case 0->s.store().left().isPresent()?1:2;case 1->s.on()?1:0;default->{var store=store(p,s);yield store==null?-1:(int)Math.round(store.cargoPosition().distanceTo(p.position()));}};
        }
        public void set(int i,int value){}
        public int getCount(){return 3;}
    };}

    /** Client keys: open the pack, or switch supply on and off. */
    public record Action(int id) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(ResourceLocation.parse("civilization:backpack"));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
        static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::id,Action::new),
                (action,context)->{if(context.player() instanceof ServerPlayer p){if(action.id==0)open(p);else toggle(p);}});}
    }
}
