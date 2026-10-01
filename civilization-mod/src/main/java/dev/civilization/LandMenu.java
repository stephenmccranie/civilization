package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LandMenu extends AbstractContainerMenu {
    private final Container fuel;
    private final ServerPlayer player;
    private final CivicData data;
    private final CivicData.Claim claim;
    private LandPayload.State lastState;
    private long confirmationUntil;
    private int confirmationPrice;
    private CivicData.Owner confirmationOwner;
    private String notice = "";
    public LandPayload.State state = new LandPayload.State(0, "Loading…", "", "", List.of(), 0, -1, "Buy land", "", "");

    public static void open(ServerPlayer player, CivicData.Address at) {
        var claim = CivicData.get(player.server).claims.get(at);
        if (claim == null) return;
        player.openMenu(new SimpleMenuProvider((id, inv, ignored) -> new LandMenu(id, inv, player, claim), Component.literal("Land Controller")));
    }
    public LandMenu(int id, Inventory inventory) { this(id, inventory, null, null); }
    public LandMenu(int id, Inventory inventory, ServerPlayer player, CivicData.Claim claim) {
        super(CivicContent.LAND_MENU.get(), id);
        this.player = player; this.claim = claim; this.data = player == null ? null : CivicData.get(player.server);
        if (player != null) data.settle(claim, System.currentTimeMillis());
        fuel = player == null ? new SimpleContainer(27) : new LandCoalInventory(data, claim, player);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(fuel, row * 9 + col, 8 + col * 18, 18 + row * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(KilnContent.MINERAL_COAL.get()) && (fuel instanceof LandCoalInventory real ? real.mayDeposit() : (state.flags() & 1) != 0); }
            @Override public boolean mayPickup(Player ignored) { return fuel instanceof LandCoalInventory real ? real.mayWithdraw() : (state.flags() & 2) != 0; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 85 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 143));
    }
    @Override public boolean stillValid(Player p) {
        return player == null || player == p && p.level().dimension().location().toString().equals(claim.at.dimension()) && fuel.stillValid(p);
    }
    @Override public void clicked(int slot, int mouse, ClickType type, Player p) {
        if (player != null) {
            if (!stillValid(p)) { p.closeContainer(); return; }
            data.settle(claim, System.currentTimeMillis());
        }
        super.clicked(slot, mouse, type, p);
        if (player != null) broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player p, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(p)) return ItemStack.EMPTY;
        var source = slot.getItem(); var copy = source.copy();
        if (index < 27) { if (!moveItemStackTo(source, 27, 63, true)) return ItemStack.EMPTY; }
        else { if (!slots.get(0).mayPlace(source) || !moveItemStackTo(source, 0, 27, false)) return ItemStack.EMPTY; }
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(p, source); return copy;
    }
    @Override public boolean clickMenuButton(Player p, int button) {
        if (player == null || p != player || !stillValid(p)) return false;
        long now = System.currentTimeMillis(); data.settle(claim, now);
        if (button >= 0 && button < 3) notice = CivicService.tier(data, claim, player, ClaimTier.values()[button], now);
        else if (button == 10) {
            if (confirmationUntil > now) { notice = CivicService.takeover(data, claim, player, now, confirmationPrice, confirmationOwner); confirmationUntil = 0; }
            else if (claim.energy > 0 && claim.buyer == null && now >= claim.eligibleAt && !data.owns(claim.owner, player.getUUID())) {
                confirmationPrice = claim.price(); confirmationOwner = claim.owner; confirmationUntil = now + 10000;
                notice = "Confirm payment. The seller has one week to move everything out.";
            }
        } else if (button == 11) notice = "Collected " + data.collect(claim.at, player) + " Coal.";
        broadcastChanges(); return true;
    }
    public void whitelist(String name, boolean remove) {
        if (player == null || !stillValid(player) || !data.owns(claim.owner, player.getUUID())) return;
        data.settle(claim, System.currentTimeMillis());
        // Recheck after settlement; an elapsed handover can have changed the owner.
        if (!data.owns(claim.owner, player.getUUID())) { broadcastChanges(); return; }
        if (!name.matches("[A-Za-z0-9_]{1,16}")) { notice = "Enter a Minecraft username."; broadcastChanges(); return; }
        data.knownPlayers.put(player.getUUID(), player.getGameProfile().getName());
        for (var online : player.server.getPlayerList().getPlayers()) data.knownPlayers.put(online.getUUID(), online.getGameProfile().getName());
        var id = data.knownPlayers.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(name)).map(java.util.Map.Entry::getKey).findFirst().orElse(null);
        if (remove && id == null) id = claim.whitelist.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(name)).map(java.util.Map.Entry::getKey).findFirst().orElse(null);
        if (id == null) notice = "That player must join this server first.";
        else if (id.equals(claim.owner.id())) notice = "The owner already has access.";
        else if (remove) { claim.whitelist.remove(id); notice = "Removed " + name + "."; }
        else if (claim.whitelist.size() >= 128 && !claim.whitelist.containsKey(id)) notice = "This claim's whitelist is full.";
        else { claim.whitelist.put(id, data.knownPlayers.get(id)); notice = "Added " + name + "."; }
        data.setDirty(); broadcastChanges();
    }
    @Override public void broadcastChanges() {
        if (player != null) {
            data.settle(claim, System.currentTimeMillis());
            var names = new ArrayList<String>();
            claim.whitelist.forEach((id, oldName) -> {
                var name = data.knownPlayers.get(id);
                if (name == null && player.server.getProfileCache() != null) name = player.server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName).orElse(oldName);
                if (name != null) data.knownPlayers.put(id, name);
                names.add(name == null ? oldName : name);
            });
            names.sort(String.CASE_INSENSITIVE_ORDER);
            int flags = ((LandCoalInventory)fuel).mayDeposit() ? 1 : 0;
            if (((LandCoalInventory)fuel).mayWithdraw()) flags |= 2;
            if (data.owns(claim.owner, player.getUUID())) flags |= 4;
            if (data.canUse(claim, player.getUUID()) && claim.buyer == null && System.currentTimeMillis() >= claim.tierChangeAt) flags |= 8;
            if (claim.energy > 0 && claim.buyer == null && System.currentTimeMillis() >= claim.eligibleAt && !data.owns(claim.owner, player.getUUID())) flags |= 16;
            int tier = -1; for (var t : ClaimTier.values()) if (claim.radius == t.radius() && claim.height == 64 && claim.upkeep == t.upkeep) tier = t.ordinal();
            long minutes = (claim.energy / claim.rate() + 59999) / 60000;
            String status = claim.energy == 0 ? "Unclaimed — add coal" : "Protected · " + (minutes >= 60 ? minutes / 60 + "h " : "") + minutes % 60 + "m fuel";
            String details = claim.radius * 2 + " × " + claim.radius * 2 + " × " + claim.height + " · " + claim.rate() + " coal/h";
            String buy = claim.buyer != null ? "Handover: " + Math.max(1, (claim.handoverAt - System.currentTimeMillis() + 3599999) / 3600000) + "h"
                    : System.currentTimeMillis() < claim.eligibleAt ? "Opens: " + Math.max(1, (claim.eligibleAt - System.currentTimeMillis() + 86399999) / 86400000) + "d"
                    : (confirmationUntil > System.currentTimeMillis() ? "Confirm " + confirmationPrice : "Buy: " + claim.price()) + " coal";
            var snapshot = new LandPayload.State(containerId, status, details, data.ownerName(claim.owner, player.server), List.copyOf(names), flags, tier, buy, notice,
                    claim.tierChangeAt > System.currentTimeMillis() ? "Tier cooldown: " + ((claim.tierChangeAt - System.currentTimeMillis() + 59999) / 60000) + "m" : "Tier change ready");
            state = snapshot;
            if (!snapshot.equals(lastState) && player.containerMenu == this) { net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, snapshot); lastState = snapshot; }
        }
        super.broadcastChanges();
    }
}
