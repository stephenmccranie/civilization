package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class CivicGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "civic-test")); p.setGameMode(GameType.SURVIVAL); return p;
    }
    private static CivicData.Claim claim(CivicData d, UUID owner, BlockPos pos, long now) {
        var c = new CivicData.Claim(); c.at = new CivicData.Address("minecraft:overworld", pos); c.owner = new CivicData.Owner(owner, false);
        c.radius = 8; c.height = 32; c.upkeep = 1; c.coalUnit = 3600000; c.energy = c.coalUnit * 200; c.lastUpdate = now; c.eligibleAt = now;
        d.claims.put(c.at, c); d.rebuildIndex(); return c;
    }
    private static void put(CivicData.Shop s, int slot, net.minecraft.world.item.Item item, int count) { s.inventory.set(slot,new ItemStack(item,count)); }
    @GameTest(template = "empty") public static void physicalBarterConservesBothInventories(GameTestHelper h) {
        var d=new CivicData(); var p=player(h); var s=new CivicData.Shop(); s.template=Items.WHEAT.getDefaultInstance(); s.amount=4; s.payment=Items.IRON_INGOT.getDefaultInstance(); s.price=2;
        put(s,0,Items.IRON_INGOT,4); p.getInventory().setItem(0,new ItemStack(Items.WHEAT,12));
        CivicService.trade(d,s,p); CivicService.trade(d,s,p); CivicService.trade(d,s,p);
        h.assertTrue(s.count(s.template)==8 && s.count(s.payment)==0 && CivicItems.count(p,s.template)==4 && CivicItems.count(p,s.payment)==4,"Two barter trades consume finite payment without a third trade"); h.succeed();
    }
    @GameTest(template = "empty") public static void fullInventoriesRejectPartialExchange(GameTestHelper h) {
        var d=new CivicData(); var p=player(h); var s=new CivicData.Shop(); s.template=Items.WHEAT.getDefaultInstance(); s.payment=Items.DIAMOND.getDefaultInstance();
        for(int i=0;i<27;i++) put(s,i,Items.STONE,32); put(s,0,Items.DIAMOND,2); p.getInventory().setItem(0,new ItemStack(Items.WHEAT,1));
        CivicService.trade(d,s,p); h.assertTrue(s.count(s.payment)==2 && CivicItems.count(p,s.template)==1,"Counter cannot accept goods: neither side changes");
        put(s,0,Items.DIAMOND,1); CivicService.trade(d,s,p);
        h.assertTrue(s.count(s.template)==1 && CivicItems.count(p,s.payment)==1,"Outgoing payment frees a slot for incoming goods");
        for(int i=0;i<36;i++) p.getInventory().setItem(i,new ItemStack(Items.STONE,32)); p.getInventory().setItem(0,new ItemStack(Items.WHEAT,2)); put(s,0,Items.DIAMOND,1);
        CivicService.trade(d,s,p); h.assertTrue(s.count(s.payment)==1 && CivicItems.count(p,s.template)==2,"Customer full inventory cannot debit counter"); h.succeed();
    }
    @GameTest(template = "empty") public static void onePaymentCannotBeSpentTwice(GameTestHelper h) {
        var d=new CivicData(); var a=player(h); var b=player(h); var s=new CivicData.Shop(); s.template=Items.WHEAT.getDefaultInstance(); put(s,0,KilnContent.MINERAL_COAL.get(),1);
        a.getInventory().setItem(0,Items.WHEAT.getDefaultInstance()); b.getInventory().setItem(0,Items.WHEAT.getDefaultInstance());
        CivicService.trade(d,s,a); CivicService.trade(d,s,b);
        h.assertTrue(s.count(s.template)==1 && CivicItems.count(a,s.payment)==1 && CivicItems.count(b,s.template)==1,"Only first buyer receives final payment"); h.succeed();
    }
    @GameTest(template = "empty") public static void takeoverDeadlineAndEscrowSurviveSerialization(GameTestHelper h) {
        long now = 1000000; var d = new CivicData(); var seller = UUID.randomUUID(); var buyer = UUID.randomUUID(); var c = claim(d, seller, BlockPos.ZERO, now);
        c.buyer = new CivicData.Owner(buyer, false); c.escrow = 400; c.handoverAt = now + CivicConfig.MOVE_MILLIS;
        var restored = CivicData.load(d.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        var copy = restored.claims.get(c.at); restored.settle(copy, c.handoverAt + 3600000);
        h.assertTrue(copy.owner.id().equals(buyer) && copy.buyer == null && copy.escrow == 0, "Offline handover resolves exactly once");
        h.assertTrue(copy.energy == 31 * c.coalUnit, "One week plus one hour of real upkeep consumed");
        h.assertTrue(restored.parcels.size() == 1 && restored.parcels.getFirst().coal() == 400 && restored.parcels.getFirst().recipient().id().equals(seller), "Seller receives held payment");
        restored.settle(copy, c.handoverAt + 7200000); h.assertTrue(restored.parcels.size() == 1, "Settlement retry cannot duplicate payment"); h.succeed();
    }
    @GameTest(template = "empty") public static void exhaustionCancelsAndRefundsBeforeHandover(GameTestHelper h) {
        long now = 1000000; var d = new CivicData(); var seller = UUID.randomUUID(); var buyer = UUID.randomUUID(); var c = claim(d, seller, BlockPos.ZERO, now);
        c.energy = c.coalUnit; c.buyer = new CivicData.Owner(buyer, false); c.escrow = 2; c.handoverAt = now + CivicConfig.MOVE_MILLIS;
        d.settle(c, now + CivicConfig.MOVE_MILLIS * 2);
        h.assertTrue(c.energy == 0 && c.owner.id().equals(seller) && c.buyer == null && c.escrow == 0, "Exhaustion cancels transfer and protection");
        h.assertTrue(d.parcels.size() == 1 && d.parcels.getFirst().recipient().id().equals(buyer) && d.parcels.getFirst().coal() == 2, "Buyer alone receives full refund");
        h.assertTrue(d.activeAt("minecraft:overworld", BlockPos.ZERO, now + CivicConfig.MOVE_MILLIS * 3) == null, "No grace protection");
        d.settle(c, now + CivicConfig.MOVE_MILLIS * 3); h.assertTrue(d.parcels.size() == 1, "Refund cannot repeat"); h.succeed();
    }
    @GameTest(template = "empty") public static void takeoverLocksReserveAndSizeButAllowsFuel(GameTestHelper h) {
        var d = new CivicData(); var seller = player(h); var buyer = player(h); long now = System.currentTimeMillis(); var c = claim(d, seller.getUUID(), BlockPos.ZERO, now);
        buyer.getInventory().setItem(0, KilnContent.MINERAL_COAL.toStack(32)); c.energy = 10 * c.coalUnit;
        CivicService.takeover(d, c, buyer, now, 20, c.owner);
        h.assertTrue(c.buyer != null && c.escrow == 20 && CivicItems.count(buyer, KilnContent.MINERAL_COAL.toStack()) == 12, "Actual reserve fixes paid price");
        CivicService.withdraw(d, c, seller, now); CivicService.resize(d, c, seller, 8, now);
        h.assertTrue(c.energy == 10 * c.coalUnit && c.radius == 8, "Cannot withdraw locked energy or resize");
        CivicService.fund(d, c, buyer, now); h.assertTrue(c.energy == 22 * c.coalUnit && c.escrow == 20, "Buyer can maintain power without repricing"); h.succeed();
    }
    @GameTest(template = "empty") public static void claimBoundsOverlapAndUpkeepScale(GameTestHelper h) {
        var d = new CivicData(); long now = 1000000; var a = claim(d, UUID.randomUUID(), BlockPos.ZERO, now);
        h.assertTrue(a.contains(new BlockPos(-8, -16, -8)) && a.contains(new BlockPos(7, 15, 7)) && !a.contains(new BlockPos(8, 0, 0)), "Half-open claim bounds are exact");
        var b = claim(d, UUID.randomUUID(), new BlockPos(16, 0, 0), now);
        h.assertTrue(!d.conflicts(b, now), "Adjacent claims fit without overlap"); b.radius = 16; b.upkeep = 4; h.assertTrue(d.conflicts(b, now), "Expansion detects overlap");
        d.settle(a, now + 1000); d.settle(b, now + 1000);
        h.assertTrue(a.energy == 200 * a.coalUnit - 1000 && b.energy == 200 * b.coalUnit - 4000, "Fourfold area costs fourfold energy"); h.succeed();
    }
    @GameTest(template = "empty") public static void whitelistAccessRevokesFromOpenContainers(GameTestHelper h) {
        var d = CivicData.get(h.getLevel().getServer()); var leader = player(h); var member = player(h); var outsider = player(h);
        var pos = new BlockPos(200000, 64, 200000); var c = claim(d, leader.getUUID(), pos, System.currentTimeMillis());
        c.whitelist.put(member.getUUID(), "member");
        try {
            h.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState()); var chest = (net.minecraft.world.Container)h.getLevel().getBlockEntity(pos); chest.setItem(0, Items.DIAMOND.getDefaultInstance());
            h.assertTrue(CivicAccess.allowed(h.getLevel(), pos, member) && !CivicAccess.allowed(h.getLevel(), pos, outsider), "Members have access, outsiders do not");
            var menu = net.minecraft.world.inventory.ChestMenu.threeRows(5, member.getInventory(), chest); member.containerMenu = menu;
            c.whitelist.remove(member.getUUID()); menu.clicked(0, 0, ClickType.PICKUP, member);
            h.assertTrue(chest.getItem(0).is(Items.DIAMOND) && menu.getCarried().isEmpty(), "Already-open menu cannot bypass revocation");
            var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(), pos, h.getLevel().getBlockState(pos), outsider);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event); h.assertTrue(event.isCanceled(), "Outsider break is denied");
        } finally { d.claims.remove(c.at); d.rebuildIndex(); }
        h.succeed();
    }
    @GameTest(template = "empty") public static void pendingControllerRemovalPreservesEscrow(GameTestHelper h) {
        var level = h.getLevel(); var d = CivicData.get(level.getServer()); var seller = player(h); var pos = new BlockPos(200064, 64, 200000);
        level.setBlockAndUpdate(pos, CivicContent.LAND.get().defaultBlockState()); var c = claim(d, seller.getUUID(), pos, System.currentTimeMillis()); c.buyer = new CivicData.Owner(UUID.randomUUID(), false); c.escrow = 400; c.handoverAt = c.lastUpdate + CivicConfig.MOVE_MILLIS;
        try {
            level.removeBlock(pos, false);
            h.assertTrue(d.claims.get(c.at) == c && c.escrow == 400 && c.buyer != null, "Physical removal cannot release escrow or erase pending claim");
            level.setBlockAndUpdate(pos, CivicContent.LAND.get().defaultBlockState()); CivicService.placed(level, pos, seller, true);
            h.assertTrue(d.claims.get(c.at) == c, "Replacement controller reconnects to the original obligation");
        } finally { d.claims.remove(c.at); d.rebuildIndex(); }
        h.succeed();
    }
    @GameTest(template = "empty") public static void ghostSelectionAndStorageAreSeparate(GameTestHelper h) {
        var p=player(h); var d=CivicData.get(p.server); var pos=new BlockPos(200128,64,200000); var level=h.getLevel();
        level.setBlockAndUpdate(pos,CivicContent.SHOP.get().defaultBlockState()); CivicService.placed(level,pos,p,false); var at=CivicService.address(level,pos); var s=d.shops.get(at); p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        try {
            var m=new CivicMenu(8,p,at,false); m.setCarried(new ItemStack(Items.DIAMOND,3)); m.clicked(27,0,ClickType.PICKUP,p);
            h.assertTrue(s.template.is(Items.DIAMOND) && m.getCarried().getCount()==3 && s.count(s.template)==0,"Selector copies exactly one template without consuming cursor or adding stock");
            m.setCarried(ItemStack.EMPTY); m.clicked(27,0,ClickType.QUICK_MOVE,p); m.clicked(27,0,ClickType.SWAP,p); m.clicked(27,0,ClickType.PICKUP_ALL,p);
            h.assertTrue(m.getCarried().isEmpty() && s.template.is(Items.DIAMOND),"Ghost cannot be extracted by inventory click variants");
            p.getInventory().setItem(0,KilnContent.MINERAL_COAL.toStack(8)); m.clicked(56,0,ClickType.QUICK_MOVE,p);
            h.assertTrue(s.count(s.payment)==8 && p.getInventory().getItem(0).isEmpty(),"Real shared storage supports shift-click");
            s.owner=new CivicData.Owner(UUID.randomUUID(),false); m.clicked(0,0,ClickType.PICKUP,p); m.setCarried(Items.STONE.getDefaultInstance()); m.clicked(28,0,ClickType.PICKUP,p);
            h.assertTrue(s.count(s.payment)==8 && s.payment.is(KilnContent.MINERAL_COAL.get()),"Revoked access blocks storage and offer editing");
            m.setCarried(ItemStack.EMPTY); p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,2)); int old=s.revision; s.revision++;
            m.action(10,old); h.assertTrue(s.count(s.payment)==8,"Stale offer revision cannot trade");
            m.action(10,s.revision); h.assertTrue(s.count(s.payment)==7 && s.count(s.template)==1,"Reviewed current offer trades");
        } finally {d.shops.remove(at);} h.succeed();
    }
    @GameTest(template = "empty") public static void whitelistAndBarterStorageSurviveSave(GameTestHelper h) {
        var d=new CivicData(); var owner=UUID.randomUUID(); var friend=UUID.randomUUID(); var c=claim(d,owner,BlockPos.ZERO,1000000); c.whitelist.put(friend,"Friend");
        var s=new CivicData.Shop(); s.at=c.at; s.owner=c.owner; s.template=Items.IRON_PICKAXE.getDefaultInstance(); s.template.setDamageValue(12); s.payment=Items.DIAMOND.getDefaultInstance(); s.price=4;
        s.inventory.set(0,s.template.copy()); put(s,1,Items.DIAMOND,16); d.shops.put(s.at,s);
        var restored=CivicData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()); var copy=restored.shops.get(s.at);
        h.assertTrue(copy.template.getDamageValue()==12 && copy.count(copy.template)==1 && copy.count(copy.payment)==16 && copy.price==4,"Templates, components and shared stock persist");
        h.assertTrue(restored.canUse(restored.claims.get(c.at),friend),"Whitelist persists"); h.succeed();
    }
    @GameTest(template = "empty") public static void hoppersCannotMoveStockAcrossClaimBoundaries(GameTestHelper h) {
        var level = h.getLevel(); var d = CivicData.get(level.getServer()); var pos = new BlockPos(200192, 64, 200000);
        var c = claim(d, UUID.randomUUID(), pos, System.currentTimeMillis());
        var hopperPos = pos.offset(-9, 0, 0); var chestPos = hopperPos.east();
        try {
            var state = Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, net.minecraft.core.Direction.EAST);
            level.setBlockAndUpdate(hopperPos, state); level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity)level.getBlockEntity(hopperPos);
            var chest = (net.minecraft.world.Container)level.getBlockEntity(chestPos);
            chest.clearContent(); hopper.clearContent(); hopper.setCooldown(0); hopper.setItem(0, Items.DIAMOND.getDefaultInstance());
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, hopperPos, state, hopper);
            h.assertTrue(hopper.getItem(0).is(Items.DIAMOND) && chest.isEmpty(), "Outside hopper cannot inject into protected storage");
            d.claims.remove(c.at); d.rebuildIndex();
            hopper.setCooldown(0);
            net.minecraft.world.level.block.entity.HopperBlockEntity.pushItemsTick(level, hopperPos, state, hopper);
            h.assertTrue(hopper.isEmpty() && chest.getItem(0).is(Items.DIAMOND), "Ordinary unclaimed automation still works");
        } finally { d.claims.remove(c.at); d.rebuildIndex(); }
        h.succeed();
    }
    @GameTest(template = "empty") public static void paidRefundCanOnlyBeCollectedByRecipient(GameTestHelper h) {
        var d = new CivicData(); var buyer = player(h); var stranger = player(h); var at = new CivicData.Address("minecraft:overworld", BlockPos.ZERO);
        d.parcel(at, new CivicData.Owner(buyer.getUUID(), false), 40);
        h.assertTrue(d.collect(at, stranger) == 0, "Other players cannot take a refund");
        h.assertTrue(d.collect(at, buyer) == 32 && d.collect(at, buyer) == 8 && d.collect(at, buyer) == 0, "Collection debits physical reserved stock once");
        h.assertTrue(CivicItems.count(buyer, KilnContent.MINERAL_COAL.toStack()) == 40 && d.parcels.isEmpty(), "No duplicated or lost refund"); h.succeed();
    }
    @GameTest(template = "empty") public static void bucketAndDispenserCannotPlaceAcrossClaimEdge(GameTestHelper h) {
        var level = h.getLevel(); var d = CivicData.get(level.getServer()); var outsider = player(h);
        var pos = new BlockPos(200256, 64, 200000); var c = claim(d, UUID.randomUUID(), pos, System.currentTimeMillis());
        var source = pos.offset(-9, 0, 0); var target = source.east();
        try {
            h.assertTrue(!outsider.mayUseItemAt(target, net.minecraft.core.Direction.EAST, Items.WATER_BUCKET.getDefaultInstance()), "Direct bucket destination checks claim access");
            var state = Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.EAST);
            level.setBlockAndUpdate(source, state); level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            var inventory = (net.minecraft.world.Container)level.getBlockEntity(source); inventory.setItem(0, Items.WATER_BUCKET.getDefaultInstance());
            state.tick(level, source, level.random);
            h.assertTrue(level.getBlockState(target).isAir() && inventory.getItem(0).is(Items.WATER_BUCKET), "Dispenser cannot bypass claim with a bucket");
            d.claims.remove(c.at); d.rebuildIndex(); state.tick(level, source, level.random);
            h.assertTrue(level.getFluidState(target).is(net.minecraft.tags.FluidTags.WATER) && inventory.getItem(0).is(Items.BUCKET), "Unclaimed dispenser retains normal bucket behavior");
        } finally { d.claims.remove(c.at); d.rebuildIndex(); }
        h.succeed();
    }
    @GameTest(template = "empty") public static void coalSlotsConserveFuelAndRejectUnauthorizedMoves(GameTestHelper h) {
        var d = CivicData.get(h.getLevel().getServer()); var p = player(h); var pos = new BlockPos(210000, 64, 200000);
        long now = System.currentTimeMillis(); var c = claim(d, p.getUUID(), pos, now);
        c.energy = c.coalUnit * 10 + c.coalUnit / 2; c.lastUpdate = now + 60000;
        p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        try {
            var menu = new LandMenu(9, p.getInventory(), p, c);
            p.getInventory().setItem(0, KilnContent.MINERAL_COAL.toStack(8));
            menu.clicked(54, 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(c.energy == c.coalUnit * 18 + c.coalUnit / 2 && p.getInventory().getItem(0).isEmpty(), "Shift-click deposits real coal while preserving burning fraction");
            menu.clicked(0, 1, ClickType.PICKUP, p);
            h.assertTrue(menu.getCarried().getCount() == 9 && c.energy == c.coalUnit * 9 + c.coalUnit / 2, "Right-click takes half a stack exactly");
            menu.clicked(1, 0, ClickType.PICKUP, p);
            h.assertTrue(menu.getCarried().isEmpty() && c.coalSlots.get(1).getCount() == 9 && c.energy == c.coalUnit * 18 + c.coalUnit / 2, "Moving fuel between slots conserves energy");
            p.getInventory().setItem(0, Items.DIAMOND.getDefaultInstance()); menu.clicked(54, 0, ClickType.QUICK_MOVE, p);
            h.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND), "Non-coal stays in player inventory");
            c.buyer = new CivicData.Owner(UUID.randomUUID(), false); c.handoverAt = now + CivicConfig.MOVE_MILLIS;
            menu.clicked(0, 0, ClickType.QUICK_MOVE, p); menu.clicked(1, 0, ClickType.SWAP, p);
            h.assertTrue(c.energy == c.coalUnit * 18 + c.coalUnit / 2 && c.coalSlots.get(1).getCount() == 9, "Funded takeover blocks extraction and swapping");
            c.buyer = null; c.owner = new CivicData.Owner(UUID.randomUUID(), false); c.whitelist.put(p.getUUID(), "friend");
            c.whitelist.clear(); menu.clicked(0, 0, ClickType.PICKUP, p);
            h.assertTrue(menu.getCarried().isEmpty(), "Revocation immediately blocks an already-open fuel slot");
        } finally { d.claims.remove(c.at); d.rebuildIndex(); }
        h.succeed();
    }
    @GameTest(template = "empty") public static void generousTiersRejectOverlapAndPersist(GameTestHelper h) {
        var d = new CivicData(); var p = player(h); long now = System.currentTimeMillis(); var c = claim(d, p.getUUID(), BlockPos.ZERO, now);
        c.energy = 800 * c.coalUnit;
        CivicService.tier(d, c, p, ClaimTier.DISTRICT, now);
        h.assertTrue(c.radius == 128 && c.height == 64 && c.rate() == 16 && c.contains(new BlockPos(127,31,127)) && !c.contains(new BlockPos(128,32,128)), "Largest tier has exact generous bounds");
        c.tierChangeAt = now; CivicService.tier(d, c, p, ClaimTier.HOMESTEAD, now); c.tierChangeAt = now;
        long reserveBefore = c.energy;
        claim(d, UUID.randomUUID(), new BlockPos(80,0,0), now);
        CivicService.tier(d, c, p, ClaimTier.DISTRICT, now);
        h.assertTrue(c.radius == 32 && c.rate() == 1 && c.height == 64 && c.energy == reserveBefore && c.tierChangeAt == now, "Rejected expansion preserves settings, fuel and cooldown");
        var restored = CivicData.load(d.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(restored.claims.get(c.at).radius == 32 && restored.claims.get(c.at).height == 64, "Tier persists"); h.succeed();
    }
    @GameTest(template = "empty") public static void legacyGroupsBecomePersonalWhitelists(GameTestHelper h) {
        var d = new CivicData(); var group = UUID.randomUUID(); var leader = UUID.randomUUID(); var member = UUID.randomUUID();
        var c = claim(d, group, BlockPos.ZERO, 1000000); c.owner = new CivicData.Owner(group, true);
        var root = d.save(new CompoundTag(), h.getLevel().registryAccess());
        var ct = root.getList("claims", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0); ct.remove("height"); ct.remove("upkeep");
        var g = new CompoundTag(); g.putUUID("id", group); g.putUUID("leader", leader);
        var members = new net.minecraft.nbt.ListTag(); members.add(net.minecraft.nbt.NbtUtils.createUUID(leader)); members.add(net.minecraft.nbt.NbtUtils.createUUID(member)); g.put("members", members);
        var groups = new net.minecraft.nbt.ListTag(); groups.add(g); root.put("groups", groups);
        var restored = CivicData.load(root, h.getLevel().registryAccess()); var copy = restored.claims.get(c.at);
        h.assertTrue(restored.owns(copy.owner, leader) && restored.canUse(copy, member) && !copy.owner.group(), "Leader owns and former members retain per-claim access");
        h.assertTrue(copy.radius == 8 && copy.height == 32 && copy.rate() == 1 && copy.coalSlots.stream().mapToInt(ItemStack::getCount).sum() == 200, "Legacy geometry and reserve preserved");
        h.assertTrue(!restored.save(new CompoundTag(), h.getLevel().registryAccess()).contains("groups"), "New save contains no formal groups"); h.succeed();
    }
    @GameTest(template = "empty") public static void largeTakeoverCanBeFundedInPhysicalTrips(GameTestHelper h) {
        var d = new CivicData(); var buyer = player(h); long now = System.currentTimeMillis(); var c = claim(d, UUID.randomUUID(), BlockPos.ZERO, now); c.energy = 800 * c.coalUnit;
        for (int i = 0; i < 36; i++) buyer.getInventory().setItem(i, KilnContent.MINERAL_COAL.toStack(32));
        CivicService.takeover(d, c, buyer, now, 1600, c.owner);
        h.assertTrue(c.buyer == null && d.availableCoal(c.at, buyer.getUUID()) == 1152 && CivicItems.count(buyer, KilnContent.MINERAL_COAL.toStack()) == 0, "Partial physical delivery does not reserve the land");
        h.assertTrue(d.collect(c.at, buyer) == 32, "Unfinished payment is reclaimable");
        for (int i = 0; i < 15; i++) buyer.getInventory().setItem(i, KilnContent.MINERAL_COAL.toStack(32));
        CivicService.takeover(d, c, buyer, now, 1600, c.owner);
        h.assertTrue(c.buyer != null && c.escrow == 1600 && d.availableCoal(c.at, buyer.getUUID()) == 0 && CivicItems.count(buyer, KilnContent.MINERAL_COAL.toStack()) == 0, "Remaining delivery completes exact escrow once"); h.succeed();
    }
    @GameTest(template = "empty") public static void barterRequiresMatchingComponents(GameTestHelper h) {
        var d=new CivicData(); var p=player(h); var s=new CivicData.Shop(); s.template=Items.IRON_PICKAXE.getDefaultInstance(); s.template.setDamageValue(12); put(s,0,KilnContent.MINERAL_COAL.get(),2);
        p.getInventory().setItem(0,Items.IRON_PICKAXE.getDefaultInstance()); CivicService.trade(d,s,p);
        h.assertTrue(s.count(s.payment)==2,"Different durability does not match the requested tool");
        p.getInventory().setItem(0,s.template.copy()); CivicService.trade(d,s,p);
        h.assertTrue(s.count(s.template)==1 && s.count(s.payment)==1,"Exact component match trades and preserves the tool"); h.succeed();
    }
    @GameTest(template = "empty") public static void legacyShopMigrationPreservesDirectionAndOverflow(GameTestHelper h) {
        var owner=new CivicData.Owner(UUID.randomUUID(),false); var at=new CivicData.Address("minecraft:overworld",BlockPos.ZERO);
        var t=new CompoundTag(); t.put("at",at.save()); t.put("owner",owner.save()); t.put("template",Items.WHEAT.getDefaultInstance().save(h.getLevel().registryAccess())); t.putInt("amount",4); t.putInt("price",2); t.putInt("stock",864); t.putInt("coal",288); t.putBoolean("buying",false);
        var ss=new net.minecraft.nbt.ListTag(); ss.add(t); var root=new CompoundTag(); root.put("shops",ss);
        var d=CivicData.load(root,h.getLevel().registryAccess()); var s=d.shops.get(at);
        h.assertTrue(s.template.is(KilnContent.MINERAL_COAL.get()) && s.amount==2 && s.payment.is(Items.WHEAT) && s.price==4,"Legacy sell direction survives as receive coal/pay wheat");
        h.assertTrue(s.count(s.payment)==864 && d.availableCoal(at,owner.id())==288,"Overflow coal remains collectible locally");
        var restored=CivicData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.availableCoal(at,owner.id())==288 && restored.shops.get(at).count(s.payment)==864,"Migration cannot duplicate stock on next load"); h.succeed();
    }
    @GameTest(template = "empty") public static void tierChangeConsumesFuelAndPersistsCooldown(GameTestHelper h) {
        var d=new CivicData(); var p=player(h); long now=System.currentTimeMillis(); var c=claim(d,p.getUUID(),BlockPos.ZERO,now); c.energy=500*c.coalUnit;
        CivicService.tier(d,c,p,ClaimTier.ESTATE,now);
        h.assertTrue(c.energy==404*c.coalUnit && c.tierChangeAt==now+CivicConfig.TIER_COOLDOWN_MILLIS,"Upgrade consumes 96 coal and starts real-time cooldown");
        CivicService.tier(d,c,p,ClaimTier.HOMESTEAD,now);
        h.assertTrue(c.radius==64 && c.energy==404*c.coalUnit,"Immediate downgrade is blocked without charging");
        var restored=CivicData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()); var copy=restored.claims.get(c.at);
        h.assertTrue(copy.tierChangeAt==c.tierChangeAt,"Restart preserves deadline");
        long deadline=copy.tierChangeAt; CivicService.tier(restored,copy,p,ClaimTier.HOMESTEAD,deadline);
        h.assertTrue(copy.radius==32 && copy.energy==284*copy.coalUnit && copy.tierChangeAt==deadline+CivicConfig.TIER_COOLDOWN_MILLIS,"After 24 hours, upkeep burns 96 and downgrade costs 24 more");
        long next=copy.tierChangeAt; CivicService.tier(restored,copy,p,ClaimTier.HOMESTEAD,deadline);
        h.assertTrue(copy.energy==284*copy.coalUnit && copy.tierChangeAt==next,"Selecting current tier does not charge or extend cooldown"); h.succeed();
    }
    @GameTest(template = "empty") public static void tierFeeCannotEmptyReserve(GameTestHelper h) {
        var d=new CivicData(); var p=player(h); long now=System.currentTimeMillis(); var c=claim(d,p.getUUID(),BlockPos.ZERO,now); c.energy=96*c.coalUnit;
        CivicService.tier(d,c,p,ClaimTier.ESTATE,now);
        h.assertTrue(c.radius==8 && c.energy==96*c.coalUnit && c.tierChangeAt==0,"Fee alone is insufficient: retain fuel to keep protection");
        c.energy=97*c.coalUnit; CivicService.tier(d,c,p,ClaimTier.ESTATE,now);
        h.assertTrue(c.radius==64 && c.energy==c.coalUnit && c.coalSlots.stream().mapToInt(ItemStack::getCount).sum()==1,"Successful change removes actual coal from slots exactly"); h.succeed();
    }
    @GameTest(template="empty") public static void typedQuantitiesSetExactMultiStackBatches(GameTestHelper h) {
        var p=player(h); var d=CivicData.get(p.server); var pos=new BlockPos(220000,64,200000); var level=h.getLevel();
        level.setBlockAndUpdate(pos,CivicContent.SHOP.get().defaultBlockState()); CivicService.placed(level,pos,p,false); var at=CivicService.address(level,pos); var s=d.shops.get(at); p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        try {
            s.template=Items.WHEAT.getDefaultInstance(); var menu=new CivicMenu(18,p,at,false);
            menu.quantities(65,3,s.revision); h.assertTrue(s.amount==65 && s.price==3,"Text quantities set the exact batch, including multiple stacks");
            menu.quantities(0,9,s.revision); menu.quantities(1,10000,s.revision); menu.quantities(2,4,s.revision-1);
            h.assertTrue(s.amount==65 && s.price==3,"Invalid and stale edits leave both values intact");
            put(s,0,KilnContent.MINERAL_COAL.get(),9); for(int i=0;i<5;i++) p.getInventory().setItem(i,new ItemStack(Items.WHEAT,i==4?2:32));
            CivicService.trade(d,s,p); CivicService.trade(d,s,p); CivicService.trade(d,s,p);
            h.assertTrue(s.count(s.template)==130 && s.count(s.payment)==3 && CivicItems.count(p,s.payment)==6,"Each click exchanges exactly 65 for 3; an incomplete third batch does nothing");
            s.owner=new CivicData.Owner(UUID.randomUUID(),false); menu.quantities(1,1,s.revision);
            h.assertTrue(s.amount==65 && s.price==3,"Customers cannot edit quantities");
        } finally {d.shops.remove(at);} h.succeed();
    }
}
