package dev.civilization;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemStackHandler;
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
        NeoForge.EVENT_BUS.addListener(Accessories::drops);
    }

    private static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        var inventory = player.getData(INVENTORY);
        for (int i = 0; i < 3; i++) {
            var stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static boolean wearing(ServerPlayer player, net.minecraft.world.item.Item item) {
        var inventory = player.getData(INVENTORY);
        for (int i = 0; i < 3; i++) if (inventory.getStackInSlot(i).is(item)) return true;
        return false;
    }

    public static final class AccessoryInventory extends ItemStackHandler {
        public AccessoryInventory() { super(3); }

        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof AccessoryItem;
        }
    }
}
