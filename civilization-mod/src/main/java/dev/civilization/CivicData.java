package dev.civilization;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Authoritative ownership, fuel, escrow and counter stock. All mutations occur on the server thread. */
public final class CivicData extends SavedData {
    public record Owner(UUID id, boolean group) {
        public CompoundTag save() { var t = new CompoundTag(); t.putUUID("id", id); t.putBoolean("group", group); return t; }
        public static Owner load(CompoundTag t) { return new Owner(t.getUUID("id"), t.getBoolean("group")); }
    }
    public record Address(String dimension, BlockPos pos) {
        public Address { pos = pos.immutable(); }
        public CompoundTag save() { var t = new CompoundTag(); t.putString("dimension", dimension); t.putLong("pos", pos.asLong()); return t; }
        public static Address load(CompoundTag t) { return new Address(t.getString("dimension"), BlockPos.of(t.getLong("pos"))); }
    }
    public static final class Claim {
        public Address at;
        public Owner owner, buyer;
        public int radius = 32, height = 64, upkeep = 1;
        public final Map<UUID, String> whitelist = new LinkedHashMap<>();
        public final net.minecraft.core.NonNullList<ItemStack> coalSlots = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        public long energy, lastUpdate, eligibleAt, handoverAt, tierChangeAt;
        public int escrow;
        public long coalUnit;
        public boolean contains(BlockPos p) {
            return p.getX() >= at.pos.getX() - radius && p.getX() < at.pos.getX() + radius
                    && p.getZ() >= at.pos.getZ() - radius && p.getZ() < at.pos.getZ() + radius
                    && p.getY() >= at.pos.getY() - height / 2 && p.getY() < at.pos.getY() + height / 2;
        }
        /** Keep stack positions stable while reconciling consumed fuel with the authoritative energy reserve. */
        public void syncCoal() {
            int wanted = (int)(energy / coalUnit), present = coalSlots.stream().mapToInt(ItemStack::getCount).sum();
            int remove = Math.max(0, present - wanted);
            for (var stack : coalSlots) { int n = Math.min(remove, stack.getCount()); stack.shrink(n); remove -= n; }
            int add = Math.max(0, wanted - present);
            for (var stack : coalSlots) if (!stack.isEmpty()) { int n = Math.min(add, stack.getMaxStackSize() - stack.getCount()); stack.grow(n); add -= n; }
            for (int i = 0; i < coalSlots.size() && add > 0; i++) if (coalSlots.get(i).isEmpty()) { int n = Math.min(32, add); coalSlots.set(i, KilnContent.MINERAL_COAL.toStack(n)); add -= n; }
        }
        public int rate() { return upkeep; }
        public int price() { return (int)Math.min(Integer.MAX_VALUE, (energy * CivicConfig.PREMIUM.get() + coalUnit - 1) / coalUnit); }
        public boolean overlaps(Claim other) {
            return at.dimension.equals(other.at.dimension)
                    && Math.abs((long)at.pos.getX() - other.at.pos.getX()) < radius + other.radius
                    && Math.abs((long)at.pos.getZ() - other.at.pos.getZ()) < radius + other.radius
                    && Math.abs((long)at.pos.getY() - other.at.pos.getY()) < (height + other.height) / 2;
        }
    }
    public static final class Shop {
        public Address at;
        public Owner owner;
        public ItemStack template = ItemStack.EMPTY, payment = KilnContent.MINERAL_COAL.toStack();
        public int amount = 1, price = 1, revision;
        public final net.minecraft.core.NonNullList<ItemStack> inventory = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        public int count(ItemStack item) { return inventory.stream().filter(s -> ItemStack.isSameItemSameComponents(s, item)).mapToInt(ItemStack::getCount).sum(); }
    }
    public record Parcel(Address at, Owner recipient, int coal) {}
    public final Map<UUID, String> knownPlayers = new LinkedHashMap<>();
    public final Map<Address, Claim> claims = new LinkedHashMap<>();
    public final Map<Address, Shop> shops = new LinkedHashMap<>();
    public final List<Parcel> parcels = new ArrayList<>();
    // A chunk index bounds routine protection lookups. It includes dormant anchors so they can be reactivated.
    private final Map<String, Map<Long, Set<Claim>>> index = new HashMap<>();
    public static CivicData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CivicData::new, CivicData::load), "civilization_civic");
    }
    public boolean owns(Owner owner, UUID player) { return owner != null && owner.id.equals(player); }
    public boolean canUse(Claim claim, UUID player) { return owns(claim.owner, player) || claim.whitelist.containsKey(player); }
    public boolean canManage(Shop shop, UUID player) {
        if (owns(shop.owner, player)) return true;
        var claim = activeAt(shop.at.dimension, shop.at.pos, System.currentTimeMillis());
        return claim != null && claim.owner.equals(shop.owner) && canUse(claim, player);
    }
    public String ownerName(Owner owner, MinecraftServer server) {
        var player = server.getPlayerList().getPlayer(owner.id);
        if (player != null) return player.getGameProfile().getName();
        return server.getProfileCache() == null ? "Player" : server.getProfileCache().get(owner.id).map(com.mojang.authlib.GameProfile::getName).orElse("Player");
    }
    public void rebuildIndex() {
        index.clear();
        for (var claim : claims.values()) {
            var chunks = index.computeIfAbsent(claim.at.dimension, ignored -> new HashMap<>());
            var p = claim.at.pos;
            for (int x = (p.getX() - claim.radius) >> 4; x <= (p.getX() + claim.radius - 1) >> 4; x++)
                for (int z = (p.getZ() - claim.radius) >> 4; z <= (p.getZ() + claim.radius - 1) >> 4; z++)
                    chunks.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(x, z), ignored -> new HashSet<>()).add(claim);
        }
    }
    public Claim activeAt(String dimension, BlockPos p, long now) {
        var chunks = index.get(dimension);
        if (chunks == null) return null;
        var candidates = chunks.get(net.minecraft.world.level.ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
        if (candidates == null) return null;
        for (var claim : candidates) if (claim.contains(p)) { settle(claim, now); if (claim.energy > 0) return claim; }
        return null;
    }
    public boolean conflicts(Claim proposed, long now) {
        for (var existing : claims.values()) if (existing != proposed && proposed.overlaps(existing)) {
            settle(existing, now); if (existing.energy > 0) return true;
        }
        return false;
    }
    /** Settle at the deadline first, so delayed/unloaded processing cannot reorder handover and exhaustion. */
    public void settle(Claim c, long now) {
        if (now <= c.lastUpdate) return;
        long until = c.buyer != null ? Math.min(now, c.handoverAt) : now;
        burn(c, until);
        if (c.buyer != null) {
            if (c.energy == 0) {
                parcel(c.at, c.buyer, c.escrow);
                c.buyer = null; c.escrow = 0; c.handoverAt = 0;
            } else if (until >= c.handoverAt) {
                parcel(c.at, c.owner, c.escrow);
                c.owner = c.buyer; c.whitelist.clear(); c.buyer = null; c.escrow = 0;
                c.eligibleAt = c.handoverAt + CivicConfig.takeoverMillis(); c.handoverAt = 0;
            }
        }
        burn(c, now);
        c.syncCoal();
        setDirty();
    }
    private static void burn(Claim c, long now) {
        long elapsed = Math.max(0, now - c.lastUpdate);
        c.energy = elapsed >= (c.energy + c.rate() - 1) / c.rate() ? 0 : c.energy - elapsed * c.rate();
        c.lastUpdate = Math.max(c.lastUpdate, now);
    }
    public void parcel(Address at, Owner owner, int amount) {
        if (amount <= 0) return;
        for (int i = 0; i < parcels.size(); i++) {
            var p = parcels.get(i);
            if (p.at.equals(at) && p.recipient.equals(owner) && (long)p.coal + amount <= Integer.MAX_VALUE) {
                parcels.set(i, new Parcel(at, owner, p.coal + amount)); setDirty(); return;
            }
        }
        parcels.add(new Parcel(at, owner, amount)); setDirty();
    }
    public int collect(Address at, ServerPlayer player) {
        int collected = 0;
        for (int i = parcels.size() - 1; i >= 0; i--) {
            var p = parcels.get(i);
            if (!p.at.equals(at) || !owns(p.recipient, player.getUUID())) continue;
            int count = Math.min(p.coal, 32);
            if (!CivicItems.exchange(player, ItemStack.EMPTY, 0, KilnContent.MINERAL_COAL.toStack(), count)) break;
            if (count == p.coal) parcels.remove(i); else parcels.set(i, new Parcel(at, p.recipient, p.coal - count));
            collected += count; setDirty();
        }
        return collected;
    }
    public int availableCoal(Address at, UUID player) {
        long amount = 0; for (var parcel : parcels) if (parcel.at.equals(at) && owns(parcel.recipient, player)) amount += parcel.coal;
        return (int)Math.min(Integer.MAX_VALUE, amount);
    }
    public void takeHeldCoal(Address at, UUID player, int amount) {
        if (amount < 0 || availableCoal(at, player) < amount) throw new IllegalArgumentException("Insufficient held coal");
        for (int i = parcels.size() - 1; i >= 0 && amount > 0; i--) {
            var p = parcels.get(i); if (!p.at.equals(at) || !owns(p.recipient, player)) continue;
            int taken = Math.min(amount, p.coal); amount -= taken;
            if (taken == p.coal) parcels.remove(i); else parcels.set(i, new Parcel(at, p.recipient, p.coal - taken));
        }
        setDirty();
    }
    public static CivicData load(CompoundTag root, HolderLookup.Provider provider) {
        var d = new CivicData();
        // Legacy groups are read only to migrate ownership; no group system survives in new saves.
        var leaders = new HashMap<UUID, UUID>();
        var members = new HashMap<UUID, Set<UUID>>();
        for (var raw : root.getList("groups", Tag.TAG_COMPOUND)) {
            var t = (CompoundTag)raw; var id = t.getUUID("id"); leaders.put(id, t.getUUID("leader"));
            var ids = new HashSet<UUID>(); for (var member : t.getList("members", Tag.TAG_INT_ARRAY)) ids.add(NbtUtils.loadUUID(member)); members.put(id, ids);
        }
        for (var raw : root.getList("knownPlayers", Tag.TAG_COMPOUND)) { var t = (CompoundTag)raw; d.knownPlayers.put(t.getUUID("id"), t.getString("name")); }
        for (var raw : root.getList("claims", Tag.TAG_COMPOUND)) {
            var t = (CompoundTag)raw; var c = new Claim(); c.at = Address.load(t.getCompound("at")); c.owner = migratedOwner(t.getCompound("owner"), leaders);
            for (var rawAccess : t.getList("whitelist", Tag.TAG_COMPOUND)) { var access = (CompoundTag)rawAccess; c.whitelist.put(access.getUUID("id"), access.getString("name")); }
            if (t.getCompound("owner").getBoolean("group")) for (var member : members.getOrDefault(t.getCompound("owner").getUUID("id"), Set.of()))
                if (!member.equals(c.owner.id)) c.whitelist.putIfAbsent(member, d.knownPlayers.getOrDefault(member, member.toString()));
            c.radius = Math.clamp(t.getInt("radius"), 8, 128);
            c.height = t.contains("height") ? Math.clamp(t.getInt("height"), 32, 64) : 32;
            c.upkeep = t.contains("upkeep") ? Math.max(1, t.getInt("upkeep")) : c.radius * c.radius / 64;
            c.tierChangeAt = t.getLong("tierChangeAt");
            c.energy = Math.max(0, t.getLong("energy")); c.lastUpdate = t.getLong("lastUpdate");
            c.coalUnit = Math.max(60000, t.getLong("coalUnit")); c.eligibleAt = t.getLong("eligibleAt"); c.handoverAt = t.getLong("handoverAt"); c.escrow = t.getInt("escrow");
            if (t.contains("buyer")) c.buyer = migratedOwner(t.getCompound("buyer"), leaders);
            net.minecraft.world.ContainerHelper.loadAllItems(t, c.coalSlots, provider); c.syncCoal(); d.claims.put(c.at, c);
        }
        for (var raw : root.getList("shops", Tag.TAG_COMPOUND)) {
            var t = (CompoundTag)raw; var s = new Shop(); s.at = Address.load(t.getCompound("at")); s.owner = migratedOwner(t.getCompound("owner"), leaders);
            s.template = ItemStack.parseOptional(provider, t.getCompound("template")); s.amount = Math.max(1, t.getInt("amount")); s.price = Math.max(1, t.getInt("price"));
            if (t.contains("sharedStorage")) {
                s.payment = ItemStack.parseOptional(provider, t.getCompound("payment"));
                net.minecraft.world.ContainerHelper.loadAllItems(t, s.inventory, provider);
            } else {
                d.setDirty();
                // Old counters had separate goods and coal capacity. Preserve overflow as local claimable items.
                int stock = t.getInt("stock"), coal = t.getInt("coal");
                for (var item : java.util.List.of(s.template.copyWithCount(Math.max(0, stock)), KilnContent.MINERAL_COAL.toStack(Math.max(0, coal)))) {
                    int left = item.getCount();
                    for (int i = 0; i < 27 && left > 0; i++) if (s.inventory.get(i).isEmpty()) { int n = Math.min(left, item.getMaxStackSize()); s.inventory.set(i, item.copyWithCount(n)); left -= n; }
                    // Goods fill at most 27 slots; only legacy coal can overflow, using the existing local parcel mechanism.
                    if (left > 0 && item.is(KilnContent.MINERAL_COAL.get())) d.parcel(s.at, s.owner, left);
                }
                if (!t.getBoolean("buying")) { var goods = s.template; s.template = s.payment; s.payment = goods; int count = s.amount; s.amount = s.price; s.price = count; }
            }
            d.shops.put(s.at, s);
        }
        for (var raw : root.getList("parcels", Tag.TAG_COMPOUND)) { var t = (CompoundTag)raw; d.parcels.add(new Parcel(Address.load(t.getCompound("at")), migratedOwner(t.getCompound("owner"), leaders), t.getInt("coal"))); }
        d.rebuildIndex(); if (!leaders.isEmpty()) d.setDirty(); return d;
    }
    private static Owner migratedOwner(CompoundTag tag, Map<UUID, UUID> leaders) {
        UUID id = tag.getUUID("id"); return new Owner(tag.getBoolean("group") ? leaders.getOrDefault(id, id) : id, false);
    }
    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider provider) {
        root.remove("groups");
        var known = new ListTag(); knownPlayers.forEach((id, name) -> { var t = new CompoundTag(); t.putUUID("id", id); t.putString("name", name); known.add(t); }); root.put("knownPlayers", known);
        var cs = new ListTag();
        for (var c : claims.values()) { var t = new CompoundTag(); t.put("at", c.at.save()); t.put("owner", c.owner.save()); t.putInt("radius", c.radius); t.putInt("height", c.height); t.putInt("upkeep", c.upkeep);
            var access = new ListTag(); c.whitelist.forEach((id, name) -> { var a = new CompoundTag(); a.putUUID("id", id); a.putString("name", name); access.add(a); }); t.put("whitelist", access);
            net.minecraft.world.ContainerHelper.saveAllItems(t, c.coalSlots, provider);
            t.putLong("tierChangeAt", c.tierChangeAt); t.putLong("energy", c.energy); t.putLong("lastUpdate", c.lastUpdate); t.putLong("coalUnit", c.coalUnit); t.putLong("eligibleAt", c.eligibleAt);
            t.putLong("handoverAt", c.handoverAt); t.putInt("escrow", c.escrow); if (c.buyer != null) t.put("buyer", c.buyer.save()); cs.add(t); }
        root.put("claims", cs);
        var ss = new ListTag();
        for (var s : shops.values()) { var t = new CompoundTag(); t.put("at", s.at.save()); t.put("owner", s.owner.save()); if (!s.template.isEmpty()) t.put("template", s.template.save(provider));
            t.putInt("amount", s.amount); t.putInt("price", s.price); t.putBoolean("sharedStorage", true);
            if (!s.payment.isEmpty()) t.put("payment", s.payment.save(provider));
            net.minecraft.world.ContainerHelper.saveAllItems(t, s.inventory, provider); ss.add(t); }
        root.put("shops", ss);
        var ps = new ListTag(); for (var p : parcels) { var t = new CompoundTag(); t.put("at", p.at.save()); t.put("owner", p.recipient.save()); t.putInt("coal", p.coal); ps.add(t); } root.put("parcels", ps);
        return root;
    }
}
