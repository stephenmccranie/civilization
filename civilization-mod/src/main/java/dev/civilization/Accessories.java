package dev.civilization;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Accessories {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Civilization.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AccessoryInventory>> INVENTORY =
            TYPES.register("accessories", () -> AttachmentType.serializable(AccessoryInventory::new)
                    .copyOnDeath().build());

    private Accessories() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(Worn::register);
        NeoForge.EVENT_BUS.addListener(Accessories::drops);
        NeoForge.EVENT_BUS.addListener(Accessories::tick);
        NeoForge.EVENT_BUS.addListener(Accessories::tracking);
    }

    /** Worn items drop with their contents beside them, since a filled container cannot be carried. */
    private static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        var inventory = player.getData(INVENTORY);
        for (int i = 0; i < 3; i++) {
            var stack = inventory.getStackInSlot(i).copy();
            if (stack.isEmpty()) continue;
            var contents = stack.remove(DataComponents.CONTAINER);
            if (contents != null) contents.nonEmptyStream().forEach(item -> event.getDrops().add(drop(player, item)));
            event.getDrops().add(drop(player, stack));
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private static ItemEntity drop(ServerPlayer player, ItemStack stack) {
        return new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
    }

    public static boolean wearing(ServerPlayer player, net.minecraft.world.item.Item item) {
        var inventory = player.getData(INVENTORY);
        for (int i = 0; i < 3; i++) if (inventory.getStackInSlot(i).is(item)) return true;
        return false;
    }

    /** Players see what others wear: the worn items, never their contents, follow each change and each new watcher. */
    private static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getData(INVENTORY).changed) {
            player.getData(INVENTORY).changed = false;
            PacketDistributor.sendToPlayersTrackingEntity(player, Worn.of(player));
        }
    }

    private static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer worn && event.getEntity() instanceof ServerPlayer watcher)
            PacketDistributor.sendToPlayer(watcher, Worn.of(worn));
    }

    public static final class AccessoryInventory extends ItemStackHandler {
        private Backpack.Contents backpack;
        private boolean changed = true;
        public AccessoryInventory() { super(3); }

        @Override public int getSlotLimit(int slot) { return 1; }
        /** Any accessory, but only one backpack at a time. */
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof AccessoryItem)) return false;
            for (int i = 0; i < getSlots(); i++) if (i != slot && stack.is(Backpack.ITEM) && getStackInSlot(i).is(Backpack.ITEM)) return false;
            return true;
        }
        @Override protected void onContentsChanged(int slot) { backpack = null; changed = true; }
        /** One live view of the worn backpack's contents, so every writer changes the same stacks. */
        public Backpack.Contents backpack() {
            if (backpack == null) for (int i = 0; i < getSlots(); i++) if (getStackInSlot(i).is(Backpack.ITEM)) backpack = new Backpack.Contents(getStackInSlot(i));
            return backpack;
        }
    }

    /** The items a player wears, for drawing them on other clients. */
    public record Worn(int entity, List<Item> items) implements CustomPacketPayload {
        public static final Type<Worn> TYPE = new Type<>(ResourceLocation.parse("civilization:worn_accessories"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
        static Worn of(ServerPlayer player) {
            var inventory = player.getData(INVENTORY);
            return new Worn(player.getId(), List.of(inventory.getStackInSlot(0).getItem(), inventory.getStackInSlot(1).getItem(), inventory.getStackInSlot(2).getItem()));
        }
        static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, StreamCodec.composite(ByteBufCodecs.VAR_INT, Worn::entity,
                    ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(3)), Worn::items, Worn::new), (worn, context) -> {
                // The own player's accessories arrive whole through its inventory menu.
                if (context.player().level().getEntity(worn.entity) instanceof Player other && other != context.player()) {
                    var inventory = other.getData(INVENTORY);
                    for (int i = 0; i < 3; i++) inventory.setStackInSlot(i, new ItemStack(worn.items.get(i)));
                }
            });
        }
    }
}
