package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CivicService {
    public static final int MAX_RESERVE = 864, SHOP_COAL = 288;
    public static CivicData.Address address(ServerLevel level, BlockPos pos) { return new CivicData.Address(level.dimension().location().toString(), pos); }
    public static CivicData.Owner personal(ServerPlayer player) { return new CivicData.Owner(player.getUUID(), false); }
    public static void placed(ServerLevel level, BlockPos pos, ServerPlayer player, boolean land) {
        var d = CivicData.get(level.getServer()); var at = address(level, pos); long now = System.currentTimeMillis();
        if (land) {
            if (!d.claims.containsKey(at)) { var c = new CivicData.Claim(); c.at = at; c.owner = personal(player); c.lastUpdate = now;
                c.coalUnit = CivicConfig.coalMillis(); c.eligibleAt = now + CivicConfig.takeoverMillis(); d.claims.put(at, c); d.rebuildIndex(); }
        } else if (!d.shops.containsKey(at)) { var s = new CivicData.Shop(); s.at = at; s.owner = personal(player); d.shops.put(at, s); }
        d.setDirty();
    }
    public static String fund(CivicData d, CivicData.Claim c, ServerPlayer player, long now) {
        d.settle(c, now);
        boolean acquiring = c.energy == 0;
        if (!acquiring && !d.canUse(c, player.getUUID()) && !d.owns(c.buyer, player.getUUID())) return "Only the owner or funded buyer can supply this claim.";
        if (d.conflicts(c, now)) return "This area overlaps another powered claim.";
        int amount = Math.min(32, Math.min(CivicItems.count(player, KilnContent.MINERAL_COAL.toStack()), (int)((MAX_RESERVE * c.coalUnit - c.energy) / c.coalUnit)));
        if (amount <= 0) return "Bring Coal, or make room in the reserve.";
        if (!CivicItems.exchange(player, KilnContent.MINERAL_COAL.toStack(), amount, ItemStack.EMPTY, 0)) return "Not enough Coal.";
        if (acquiring) { c.owner = personal(player); c.whitelist.clear(); c.eligibleAt = now + CivicConfig.takeoverMillis(); }
        c.energy += amount * c.coalUnit; c.lastUpdate = now; c.syncCoal(); d.setDirty(); return "Added " + amount + " Coal. Claim protected.";
    }
    public static String withdraw(CivicData d, CivicData.Claim c, ServerPlayer player, long now) {
        d.settle(c, now);
        if (!d.canUse(c, player.getUUID())) return "Only the owner can withdraw reserve fuel.";
        if (c.buyer != null) return "Reserve locked until handover. Upkeep continues.";
        int count = (int)Math.min(32, c.energy / c.coalUnit);
        if (count == 0 || !CivicItems.exchange(player, ItemStack.EMPTY, 0, KilnContent.MINERAL_COAL.toStack(), count)) return "No whole coal available, or inventory full.";
        c.energy -= count * c.coalUnit; c.syncCoal(); d.setDirty(); return "Withdrew " + count + " Coal.";
    }
    public static String resize(CivicData d, CivicData.Claim c, ServerPlayer player, int delta, long now) {
        return tier(d, c, player, ClaimTier.step(c.radius, delta > 0), now);
    }
    public static String tier(CivicData d, CivicData.Claim c, ServerPlayer player, ClaimTier tier, long now) {
        d.settle(c, now);
        if (!d.canUse(c, player.getUUID())) return "Only the owner can resize this claim.";
        if (c.buyer != null) return "Claim size is locked during a takeover.";
        if (c.radius == tier.radius() && c.height == 64 && c.upkeep == tier.upkeep) return "This tier is already selected.";
        if (now < c.tierChangeAt) return "Tier change available in " + ((c.tierChangeAt - now + 59999) / 60000) + " minutes.";
        int cost = tier.upkeep * CivicConfig.TIER_SWITCH_HOURS;
        long energyCost = cost * c.coalUnit;
        if (c.energy <= energyCost) return "Store " + cost + " coal for the change, plus fuel to keep the claim powered.";
        int old = c.radius, oldHeight = c.height, oldRate = c.upkeep;
        c.radius = tier.radius(); c.height = 64; c.upkeep = tier.upkeep;
        if (d.conflicts(c, now)) { c.radius = old; c.height = oldHeight; c.upkeep = oldRate; return "Expansion overlaps another powered claim."; }
        c.energy -= energyCost; c.syncCoal(); c.tierChangeAt = now + CivicConfig.TIER_COOLDOWN_MILLIS;
        d.rebuildIndex(); d.setDirty(); return "Spent " + cost + " coal. Claim: " + tier.width + " x " + tier.width + ", 64 blocks tall. Upkeep: " + tier.upkeep + " coal/hour.";
    }
    public static String takeover(CivicData d, CivicData.Claim c, ServerPlayer player, long now, int expectedPrice, CivicData.Owner expectedOwner) {
        d.settle(c, now);
        if (c.energy == 0) return "Land is unclaimed. Supply coal to claim it.";
        if (d.owns(c.owner, player.getUUID())) return "You already have full access to this claim.";
        if (c.buyer != null) return "A takeover is already funded.";
        if (now < c.eligibleAt) return "This claim is not yet open to takeover.";
        int price = c.price();
        if (price != expectedPrice || !c.owner.equals(expectedOwner)) return "The price or owner changed. Review the offer again.";
        int held = Math.min(price, d.availableCoal(c.at, player.getUUID()));
        int carried = CivicItems.count(player, KilnContent.MINERAL_COAL.toStack());
        if (held + carried < price) {
            if (carried > 0 && CivicItems.exchange(player, KilnContent.MINERAL_COAL.toStack(), carried, ItemStack.EMPTY, 0)) d.parcel(c.at, personal(player), carried);
            return "Stored " + (held + carried) + " coal here. Bring " + (price - held - carried) + " more, or collect it back. Land is not reserved yet.";
        }
        if (!CivicItems.exchange(player, KilnContent.MINERAL_COAL.toStack(), price - held, ItemStack.EMPTY, 0)) return "Payment changed; review the offer again.";
        d.takeHeldCoal(c.at, player.getUUID(), held);
        c.buyer = personal(player); c.escrow = price; c.handoverAt = now + CivicConfig.MOVE_MILLIS; d.setDirty();
        for (var online : player.server.getPlayerList().getPlayers()) if (d.owns(c.owner, online.getUUID()))
            online.sendSystemMessage(net.minecraft.network.chat.Component.literal("Your land at " + c.at.pos().toShortString()
                    + " has a funded takeover. You have one week to move. Reserve fuel is locked; keep power supplied for the sale to complete."));
        return "Takeover funded. Handover in one week. If power fails, collect your refund here.";
    }
    public static String trade(CivicData d, CivicData.Shop shop, ServerPlayer player) {
        if (shop.template.isEmpty() || shop.payment.isEmpty()) return "Select both exchange items first.";
        var storage = new java.util.ArrayList<ItemStack>(); shop.inventory.forEach(s -> storage.add(s.copy()));
        // Debit the counter first: incoming items cannot fund this same exchange.
        if (!CivicItems.exchange(storage, shop.payment, shop.price, shop.template, shop.amount)) return "Counter needs payment stock or room for incoming goods.";
        if (!CivicItems.exchange(player, shop.template, shop.amount, shop.payment, shop.price)) return "Bring the requested items and make room for payment.";
        for (int i = 0; i < 27; i++) shop.inventory.set(i, storage.get(i));
        d.setDirty(); return "Trade complete.";
    }
    public static void removed(ServerLevel level, BlockPos pos, boolean land) {
        var d = CivicData.get(level.getServer()); var at = address(level, pos);
        if (land) {
            var c = d.claims.get(at); if (c == null) return; d.settle(c, System.currentTimeMillis());
            if (c.buyer != null) return; // Escrow and claim remain anchored even if its physical controller is removed.
            drop(level, pos, KilnContent.MINERAL_COAL.toStack(), (int)(c.energy / c.coalUnit));
            d.claims.remove(at); d.rebuildIndex();
        } else { var s = d.shops.remove(at); if (s != null) { for (var item : s.inventory) drop(level, pos, item, item.getCount()); } }
        d.setDirty();
    }
    private static void drop(ServerLevel level, BlockPos pos, ItemStack template, int count) {
        if (template.isEmpty()) return;
        while (count > 0) { int n = Math.min(count, template.getMaxStackSize()); net.minecraft.world.level.block.Block.popResource(level, pos, template.copyWithCount(n)); count -= n; }
    }
}
