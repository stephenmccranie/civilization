package dev.civilization;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Real fuel slots backed by the claim, not menu display items or a second spendable balance. */
public final class LandCoalInventory implements Container {
    private final CivicData data;
    private final CivicData.Claim claim;
    private final ServerPlayer player;
    public LandCoalInventory(CivicData data, CivicData.Claim claim, ServerPlayer player) {
        this.data = data; this.claim = claim; this.player = player; claim.syncCoal();
    }
    public boolean mayDeposit() {
        return claim.energy == 0 ? !data.conflicts(claim, System.currentTimeMillis()) : data.canUse(claim, player.getUUID()) || data.owns(claim.buyer, player.getUUID());
    }
    public boolean mayWithdraw() { return claim.buyer == null && data.canUse(claim, player.getUUID()); }
    @Override public int getContainerSize() { return 27; }
    @Override public boolean isEmpty() { return claim.coalSlots.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return claim.coalSlots.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { var result = ContainerHelper.removeItem(claim.coalSlots, slot, count); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var result = ContainerHelper.takeItem(claim.coalSlots, slot); setChanged(); return result; }
    @Override public void setItem(int slot, ItemStack stack) { claim.coalSlots.set(slot, stack); stack.limitSize(getMaxStackSize()); setChanged(); }
    @Override public int getMaxStackSize() { return 32; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(KilnContent.MINERAL_COAL.get()) && mayDeposit(); }
    @Override public void setChanged() {
        long before = claim.energy;
        long whole = claim.coalSlots.stream().mapToLong(ItemStack::getCount).sum();
        claim.energy = whole * claim.coalUnit + before % claim.coalUnit;
        if (before == 0 && claim.energy > 0) {
            claim.owner = CivicService.personal(player); claim.whitelist.clear();
            claim.lastUpdate = System.currentTimeMillis(); claim.eligibleAt = claim.lastUpdate + CivicConfig.takeoverMillis();
        }
        data.setDirty();
    }
    @Override public boolean stillValid(Player p) { return p == player && data.claims.get(claim.at) == claim && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(claim.at.pos())) <= 64; }
    @Override public void clearContent() { claim.coalSlots.clear(); setChanged(); }
}
