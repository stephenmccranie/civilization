package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

/**
 * Custody of raw coal between face, falling batch, pile, shovel, cart and screen: every case counts the exact units
 * before and after. The fixture is the 5x5 stone floor {@link CoalMiningGameTests#face} lays around the workface.
 */
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class CoalCustodyGameTests {
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    /** The patch a miner standing north of the face strikes first: cells x 0-1, y 0-1, z 0. */
    private static final long FIRST_PATCH = 0x33L;

    private static BlockState pile(int units) {
        return CoalMiningContent.PILE.get().defaultBlockState().setValue(RawCoalPileBlock.UNITS, units);
    }
    /** Every raw unit lying in the fixture: settled piles plus falling batches. */
    private static int loose(GameTestHelper h) {
        var l = h.getLevel(); var from = h.absolutePos(BlockPos.ZERO); var to = h.absolutePos(new BlockPos(11, 5, 11));
        int units = 0;
        for (var p : BlockPos.betweenClosed(from, to)) {
            var s = l.getBlockState(p);
            if (s.is(CoalMiningContent.PILE.get())) units += s.getValue(RawCoalPileBlock.UNITS);
        }
        for (var e : l.getEntitiesOfClass(LooseCoalEntity.class, AABB.encapsulatingFullBlocks(from, to))) units += e.units();
        return units;
    }
    private static int dropped(GameTestHelper h, Item item) {
        var from = h.absolutePos(BlockPos.ZERO); var to = h.absolutePos(new BlockPos(11, 5, 11));
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.encapsulatingFullBlocks(from, to)).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
    /** Stone two blocks high around the floor, so nothing rolls or is thrown out of the fixture. */
    private static void wall(GameTestHelper h, BlockPos a) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) if (Math.max(Math.abs(x), Math.abs(z)) == 3)
            for (int y = -1; y <= 0; y++) h.getLevel().setBlockAndUpdate(a.offset(x, y, z), STONE);
    }
    private static FakePlayer holding(FakePlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack); return p;
    }
    private static InteractionResult scoop(ServerPlayer p, BlockPos pile) {
        return CoalMiningContent.SHOVEL.get().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(pile.getCenter(), Direction.UP, pile, false)));
    }
    private static InteractionResult placeCart(ServerPlayer p, BlockPos rail) {
        return CoalMiningContent.CART_ITEM.get().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(rail.getCenter(), Direction.UP, rail, false)));
    }
    /** Lays a rail and places a cart on it through the real item, then hands the player back what they held. */
    private static CoalMinecart cart(GameTestHelper h, FakePlayer p, BlockPos rail) {
        var held = p.getMainHandItem();
        h.getLevel().setBlockAndUpdate(rail, Blocks.RAIL.defaultBlockState());
        holding(p, CoalMiningContent.CART_ITEM.toStack());
        h.assertTrue(placeCart(p, rail) == InteractionResult.CONSUME && p.getMainHandItem().isEmpty(), "One cart item becomes one cart on the rail");
        holding(p, held);
        return h.getLevel().getEntitiesOfClass(CoalMinecart.class, new AABB(rail)).getFirst();
    }
    private static CoalScreenEntity screen(GameTestHelper h, BlockPos at) {
        h.getLevel().setBlockAndUpdate(at, CoalMiningContent.SCREEN.get().defaultBlockState());
        return (CoalScreenEntity) h.getLevel().getBlockEntity(at);
    }
    private static InteractionResult use(ServerPlayer p, CoalScreenEntity screen) {
        var at = screen.getBlockPos();
        return screen.getBlockState().useWithoutItem(screen.getLevel(), p, new BlockHitResult(at.getCenter(), Direction.UP, at, false));
    }
    private static double labor(ServerPlayer p) { return CalorieFoodData.of(p).laborSpent; }
    /** A claim over the whole floor that lists the player; clearing its whitelist revokes them. */
    private static CivicData.Claim claim(GameTestHelper h, BlockPos a, ServerPlayer member) {
        var c = new CivicData.Claim();
        c.at = new CivicData.Address(h.getLevel().dimension().location().toString(), a);
        c.owner = new CivicData.Owner(java.util.UUID.randomUUID(), false);
        c.radius = 3; c.height = 32; c.upkeep = 1; c.coalUnit = 3600000; c.energy = 360000000; c.lastUpdate = System.currentTimeMillis();
        c.whitelist.put(member.getUUID(), "coal-miner");
        var data = CivicData.get(h.getLevel().getServer()); data.claims.put(c.at, c); data.rebuildIndex();
        return c;
    }
    private static void release(GameTestHelper h, CivicData.Claim c) {
        var data = CivicData.get(h.getLevel().getServer()); data.claims.remove(c.at); data.rebuildIndex();
    }

    @GameTest(template = "industrial")
    public static void twoMinersOnOneFaceEachOwnOneContact(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel();
        var first = CoalMiningGameTests.player(h, a); var second = CoalMiningGameTests.player(h, a);
        var patch = Vec3.atLowerCornerOf(a).add(.25, .25, 0);
        CoalMiningGameTests.aim(first, patch); CoalMiningGameTests.aim(second, patch);
        h.assertTrue(CoalMiningSystem.start(first) && CoalMiningSystem.start(second), "Both miners commit a swing at the same patch");
        h.runAfterDelay(CoalPickItem.CONTACT + 1, () -> {
            CoalMiningSystem.step(first); CoalMiningSystem.step(second);
            var face = (CoalWorkfaceEntity) l.getBlockEntity(a);
            h.assertTrue(Long.bitCount(face.mask()) == 56 && loose(h) == 8, "The second contact takes the layer the first exposed, never the same cells");
            h.assertTrue(first.getMainHandItem().getDamageValue() == 1 && second.getMainHandItem().getDamageValue() == 1, "Each miner pays for one contact");
            face.mask(FIRST_PATCH);
            h.assertTrue(CoalMiningSystem.strike(first) && !CoalMiningSystem.strike(second), "Only one miner takes the last patch");
            h.assertTrue(l.getBlockState(a).isAir() && loose(h) == 12, "The cleared block released its last four units once");
            h.assertTrue(first.getMainHandItem().getDamageValue() == 2 && second.getMainHandItem().getDamageValue() == 1, "The miner who met no coal pays nothing");
            h.succeed();
        });
    }

    @GameTest(template = "industrial")
    public static void twoShovelsSplitOnePileAndOneCart(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel(); var pile = a.offset(-1, -1, -1);
        var first = holding(CoalMiningGameTests.player(h, a), CoalMiningContent.SHOVEL.toStack());
        var second = holding(CoalMiningGameTests.player(h, a), CoalMiningContent.SHOVEL.toStack());
        l.setBlockAndUpdate(pile, pile(28));
        scoop(first, pile); scoop(second, pile);
        h.assertTrue(CoalMiningContent.load(first.getMainHandItem()) == 16 && CoalMiningContent.load(second.getMainHandItem()) == 12 && l.getBlockState(pile).isAir(), "Two shovels take 16 and the remaining 12 of a 28-unit pile");
        h.assertTrue(scoop(first, pile) == InteractionResult.PASS && scoop(second, pile) == InteractionResult.PASS && loose(h) == 0, "The emptied cell gives nothing more");
        var cart = cart(h, first, a.offset(1, -1, -1)); cart.insert(CoalMinecart.CAPACITY - 20);
        cart.interact(first, InteractionHand.MAIN_HAND); cart.interact(second, InteractionHand.MAIN_HAND);
        h.assertTrue(cart.load() == CoalMinecart.CAPACITY && CoalMiningContent.load(first.getMainHandItem()) == 0 && CoalMiningContent.load(second.getMainHandItem()) == 8, "The cart takes 16 and then the 4 that still fit; 8 stay on the second shovel");
        cart.interact(second, InteractionHand.MAIN_HAND);
        h.assertTrue(cart.load() == CoalMinecart.CAPACITY && CoalMiningContent.load(second.getMainHandItem()) == 8, "A full cart refuses the rest again");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void screenTakesOnlyWhatFitsAndWorksOneBatchForTwoWorkers(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h);
        var first = holding(CoalMiningGameTests.player(h, a), ItemStack.EMPTY); var second = holding(CoalMiningGameTests.player(h, a), ItemStack.EMPTY);
        var cart = cart(h, first, a.offset(1, -1, -1)); cart.insert(300);
        var screen = screen(h, a.offset(2, -1, 0));
        use(first, screen); use(second, screen);
        h.assertTrue(cart.load() == 40 && screen.raw() == 256 && screen.coal() == 1, "Two workers in one tick: 256 then 4 units unloaded, one batch worked");
        h.runAfterDelay(10, () -> {
            use(second, screen);
            h.assertTrue(cart.load() == 40 && screen.raw() == 252 && screen.coal() == 2, "A full screen leaves the cart's cargo where it is");
            var full = new CompoundTag(); full.putInt("rawCoal", 8); full.putInt("preparedCoal", 64);
            screen.loadWithComponents(full, h.getLevel().registryAccess());
        });
        h.runAfterDelay(20, () -> {
            use(first, screen);
            h.assertTrue(screen.raw() == 48 && screen.coal() == 64 && cart.load() == 0, "Full ready Coal blocks preparation: the cart's 40 units join the 8 waiting");
            first.setShiftKeyDown(true); use(first, screen);
            h.assertTrue(screen.coal() == 32 && first.getInventory().countItem(KilnContent.MINERAL_COAL.get()) == 32, "One collection moves 32 Coal");
            use(first, screen);
            h.assertTrue(screen.coal() == 32 && first.getInventory().countItem(KilnContent.MINERAL_COAL.get()) == 32, "A full hand collects nothing more");
            h.succeed();
        });
    }

    @GameTest(template = "industrial")
    public static void repeatedRequestsMoveStockOnce(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel(); var p = CoalMiningGameTests.player(h, a); var pick = p.getMainHandItem();
        CoalMiningGameTests.aim(p, a.getCenter());
        int accepted = 0; for (int i = 0; i < 5; i++) if (CoalMiningSystem.start(p)) accepted++;
        h.assertTrue(accepted == 1 && loose(h) == 0, "Five clicks commit one swing, and a swing alone moves nothing");
        h.runAfterDelay(CoalPickItem.CONTACT + 1, () -> {
            for (int i = 0; i < 5; i++) CoalMiningSystem.step(p);
            var face = (CoalWorkfaceEntity) l.getBlockEntity(a);
            h.assertTrue(Long.bitCount(face.mask()) == 60 && loose(h) == 4 && pick.getDamageValue() == 1, "One swing makes one contact");
            double chipped = labor(p);
            p.getCooldowns().removeCooldown(pick.getItem()); p.setXRot(-90);
            h.assertTrue(CoalMiningSystem.start(p), "The next swing is accepted after recovery");
            h.runAfterDelay(CoalPickItem.CONTACT + 1, () -> {
                CoalMiningSystem.step(p);
                h.assertTrue(Long.bitCount(face.mask()) == 60 && loose(h) == 4 && pick.getDamageValue() == 1 && labor(p) == chipped, "A swing that meets no coal is free");

                var pile = a.offset(-1, -1, -1); l.setBlockAndUpdate(pile, pile(40));
                holding(p, CoalMiningContent.SHOVEL.toStack());
                for (int i = 0; i < 5; i++) scoop(p, pile);
                double scooped = labor(p);
                h.assertTrue(CoalMiningContent.load(p.getMainHandItem()) == 16 && l.getBlockState(pile).getValue(RawCoalPileBlock.UNITS) == 24 && scooped > chipped, "A full shovel scoops once");
                scoop(p, pile);
                h.assertTrue(labor(p) == scooped, "Scooping with a full shovel costs nothing");

                var cart = cart(h, p, a.offset(1, -1, -1));
                for (int i = 0; i < 5; i++) cart.interact(p, InteractionHand.MAIN_HAND);
                h.assertTrue(cart.load() == 16 && CoalMiningContent.load(p.getMainHandItem()) == 0, "An emptied shovel loads nothing more");

                holding(p, ItemStack.EMPTY); p.setXRot(0);
                for (int i = 0; i < 5; i++) cart.interact(p, InteractionHand.MAIN_HAND);
                double pushed = labor(p);
                h.assertTrue(cart.load() == 16 && pushed > scooped && cart.getDeltaMovement().horizontalDistance() < .13, "Five pushes in one tick are one push, and pushing moves no cargo");
                cart.setDeltaMovement(Vec3.ZERO);

                var screen = screen(h, a.offset(2, -1, 0));
                for (int i = 0; i < 5; i++) use(p, screen);
                h.assertTrue(cart.load() == 0 && screen.raw() == 12 && screen.coal() == 1 && labor(p) > pushed, "Five clicks unload the cart once and work one batch");
                double worked = labor(p);
                p.setShiftKeyDown(true);
                for (int i = 0; i < 5; i++) use(p, screen);
                h.assertTrue(screen.raw() == 12 && screen.coal() == 0 && p.getInventory().countItem(KilnContent.MINERAL_COAL.get()) == 1 && labor(p) == worked, "The one ready Coal is collected once");
                h.assertTrue(loose(h) == 4 + 24, "Nothing else appeared on the floor");
                h.succeed();
            });
        });
    }

    @GameTest(template = "industrial")
    public static void revokedAccessStopsEveryTransfer(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel(); var p = CoalMiningGameTests.player(h, a); var pick = p.getMainHandItem();
        var pile = a.offset(-1, -1, -1); l.setBlockAndUpdate(pile, pile(20));
        var cart = cart(h, p, a.offset(1, -1, -1)); cart.insert(300);
        var screen = screen(h, a.offset(2, -1, 0));
        holding(p, ItemStack.EMPTY); use(p, screen);
        h.assertTrue(cart.load() == 44 && screen.raw() == 252 && screen.coal() == 1, "The fixture starts with stock in the cart and the screen");
        var spare = a.offset(-1, -1, 1); l.setBlockAndUpdate(spare, Blocks.RAIL.defaultBlockState());
        var claim = claim(h, a, p);
        try {
            claim.whitelist.clear();
            double before = labor(p);
            var shovel = CoalMiningContent.SHOVEL.toStack(); holding(p, shovel);
            h.assertTrue(scoop(p, pile) == InteractionResult.FAIL && CoalMiningContent.load(shovel) == 0 && l.getBlockState(pile).getValue(RawCoalPileBlock.UNITS) == 20, "A revoked miner scoops nothing");
            CoalMiningContent.load(shovel, 16);
            h.assertTrue(cart.interact(p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && cart.load() == 44 && CoalMiningContent.load(shovel) == 16, "A revoked miner loads nothing");
            holding(p, ItemStack.EMPTY);
            h.assertTrue(cart.interact(p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && cart.getDeltaMovement().horizontalDistance() == 0, "A revoked miner cannot push the cart");
            h.assertTrue(use(p, screen) == InteractionResult.FAIL && cart.load() == 44 && screen.raw() == 252 && screen.coal() == 1, "A revoked miner neither unloads nor works the screen");
            p.setShiftKeyDown(true);
            h.assertTrue(use(p, screen) == InteractionResult.FAIL && screen.coal() == 1 && p.getInventory().countItem(KilnContent.MINERAL_COAL.get()) == 0, "A revoked miner collects no Coal");
            p.setShiftKeyDown(false);
            holding(p, CoalMiningContent.CART_ITEM.toStack());
            h.assertTrue(placeCart(p, spare) == InteractionResult.FAIL && p.getMainHandItem().getCount() == 1 && l.getEntitiesOfClass(CoalMinecart.class, new AABB(spare)).isEmpty(), "A revoked miner places no cart and keeps the item");
            holding(p, pick); CoalMiningGameTests.aim(p, a.getCenter());
            h.assertTrue(!CoalMiningSystem.strike(p) && ((CoalWorkfaceEntity) l.getBlockEntity(a)).mask() == CoalGeometry.FULL && loose(h) == 20 && labor(p) == before, "A revoked miner chips nothing, and no refusal cost labour");

            claim.whitelist.put(p.getUUID(), "coal-miner");
            CoalMiningContent.load(shovel, 0); holding(p, shovel);
            h.assertTrue(scoop(p, pile) == InteractionResult.CONSUME && CoalMiningContent.load(shovel) == 16 && l.getBlockState(pile).getValue(RawCoalPileBlock.UNITS) == 4, "Access is read at each action: once listed again the same miner scoops");
        } finally {
            release(h, claim);
        }
        h.succeed();
    }

    @GameTest(template = "industrial", timeoutTicks = 200)
    public static void fallingStockFillsOnlyFreeCellsAndKeepsTheRest(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = -1; y <= 0; y++) l.setBlockAndUpdate(a.offset(x, y, z), STONE);
        // A sealed three-cell trench whose middle pile is nearly full, open above the middle only.
        var middle = a.offset(0, -1, -1);
        l.setBlockAndUpdate(middle.west(), Blocks.AIR.defaultBlockState()); l.setBlockAndUpdate(middle.east(), Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(middle, pile(60)); l.setBlockAndUpdate(middle.above(), Blocks.AIR.defaultBlockState());
        var poured = LooseCoalEntity.create(l, middle.getCenter().add(0, 1.5, 0), 200);
        // A sealed cell whose floor is a slab: no pile can form there or beside it.
        var blocked = a.offset(-1, -1, 1);
        l.setBlockAndUpdate(blocked, Blocks.STONE_SLAB.defaultBlockState()); l.setBlockAndUpdate(blocked.above(), Blocks.AIR.defaultBlockState());
        var stuck = LooseCoalEntity.create(l, blocked.getCenter().add(0, 1, 0), 9);
        Runnable check = () -> {
            int trench = 0;
            for (var cell : java.util.List.of(middle.west(), middle, middle.east())) {
                h.assertTrue(l.getBlockState(cell).is(CoalMiningContent.PILE.get()), "Every free supported cell holds a pile");
                trench += l.getBlockState(cell).getValue(RawCoalPileBlock.UNITS);
            }
            h.assertTrue(trench == 192 && poured.isAlive() && poured.units() == 68, "Three full piles take 4 + 64 + 64 units; the other 68 stay in the batch");
            h.assertTrue(stuck.isAlive() && stuck.units() == 9 && l.getBlockState(blocked).is(Blocks.STONE_SLAB), "A batch with nowhere to settle keeps all nine units");
            h.assertTrue(loose(h) == 60 + 200 + 9, "No unit was made or lost");
        };
        h.runAfterDelay(60, check);
        h.runAfterDelay(150, () -> { check.run(); h.succeed(); });
    }

    @GameTest(template = "industrial", timeoutTicks = 100)
    public static void flowingWaterCannotWashAwayPilesOrWorkedFaces(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel();
        // A capped stone basin holding, in a row, a small pile, a water source and a worked face.
        for (int x = -2; x <= 2; x++) for (int z = -1; z <= 1; z++) for (int y = -1; y <= 0; y++) l.setBlockAndUpdate(a.offset(x, y, z), STONE);
        var source = a.below(); var pile = source.west(); var worked = source.east();
        l.setBlockAndUpdate(pile, pile(8));
        l.setBlockAndUpdate(worked, CoalMiningContent.FACE.get().defaultBlockState());
        ((CoalWorkfaceEntity) l.getBlockEntity(worked)).mask(~FIRST_PATCH);
        l.setBlockAndUpdate(source, Blocks.WATER.defaultBlockState());
        h.runAfterDelay(40, () -> {
            h.assertTrue(l.getBlockState(pile).equals(pile(8)), "Water flowing against a small pile leaves its eight units");
            h.assertTrue(l.getBlockEntity(worked) instanceof CoalWorkfaceEntity face && face.mask() == ~FIRST_PATCH, "Water flowing against a worked face leaves its remaining coal");
            h.succeed();
        });
    }

    @GameTest(template = "industrial")
    public static void pickBreakingOnContactStillReleasesItsChunkOnce(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var p = CoalMiningGameTests.player(h, a); var pick = p.getMainHandItem();
        CoalMiningGameTests.aim(p, a.getCenter()); pick.setDamageValue(pick.getMaxDamage() - 1);
        h.assertTrue(CoalMiningSystem.strike(p) && p.getMainHandItem().isEmpty(), "The last durability point is spent on a real contact");
        h.assertTrue(Long.bitCount(((CoalWorkfaceEntity) h.getLevel().getBlockEntity(a)).mask()) == 60 && loose(h) == 4, "The broken pick's contact released exactly its four units");
        h.assertTrue(!CoalMiningSystem.strike(p) && !CoalMiningSystem.start(p) && loose(h) == 4, "No pick, no further stock");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void deathCancelsTheSwingAndTheShovelLoadFollowsTheDroppedItem(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); wall(h, a); var l = h.getLevel(); var p = CoalMiningGameTests.player(h, a); var pick = p.getMainHandItem();
        CoalMiningGameTests.aim(p, a.getCenter());
        h.assertTrue(CoalMiningSystem.start(p), "The miner commits a swing");
        p.setHealth(0);
        h.runAfterDelay(CoalPickItem.CONTACT + 1, () -> {
            CoalMiningSystem.step(p);
            h.assertTrue(((CoalWorkfaceEntity) l.getBlockEntity(a)).mask() == CoalGeometry.FULL && loose(h) == 0 && pick.getDamageValue() == 0, "A miner who dies during the wind-up makes no contact");
            p.getCooldowns().removeCooldown(pick.getItem());
            h.assertTrue(!CoalMiningSystem.strike(p) && !CoalMiningSystem.start(p), "The dead neither strike nor swing");
            var shovel = CoalMiningContent.SHOVEL.toStack(); CoalMiningContent.load(shovel, 11); p.getInventory().add(shovel);
            p.getInventory().dropAll();
            var from = h.absolutePos(BlockPos.ZERO); var to = h.absolutePos(new BlockPos(11, 5, 11));
            var shovels = l.getEntitiesOfClass(ItemEntity.class, AABB.encapsulatingFullBlocks(from, to), e -> e.getItem().is(CoalMiningContent.SHOVEL.get()));
            h.assertTrue(p.getInventory().isEmpty() && shovels.size() == 1 && CoalMiningContent.load(shovels.getFirst().getItem()) == 11, "The death drop is one shovel still carrying its eleven units");
            h.assertTrue(loose(h) == 0, "Dropping a loaded shovel puts nothing on the floor");
            h.succeed();
        });
    }

    @GameTest(template = "industrial", timeoutTicks = 200)
    public static void destroyedCartsSpillExactCargo(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); wall(h, a); var l = h.getLevel(); var p = CoalMiningGameTests.player(h, a);
        var struck = cart(h, p, a.offset(1, -1, -1)); struck.insert(CoalMinecart.CAPACITY);
        var blasted = cart(h, p, a.offset(-1, -1, 1)); blasted.insert(123);
        struck.hurt(l.damageSources().playerAttack(p), 5);
        h.assertTrue(struck.isRemoved() && loose(h) == CoalMinecart.CAPACITY, "A cart broken in Survival spills its full cargo as one batch");
        blasted.hurt(l.damageSources().explosion(null, null), 5);
        h.assertTrue(blasted.isRemoved() && loose(h) == CoalMinecart.CAPACITY + 123, "A cart destroyed by an explosion spills its cargo too");
        h.assertTrue(dropped(h, CoalMiningContent.CART_ITEM.get()) == 2, "Each destroyed cart drops its item once");
        Runnable conserved = () -> h.assertTrue(loose(h) == CoalMinecart.CAPACITY + 123, "Spilled cargo settles and overflows without gaining or losing a unit");
        h.runAfterDelay(50, conserved);
        h.runAfterDelay(150, () -> { conserved.run(); h.succeed(); });
    }

    @GameTest(template = "industrial")
    public static void brokenScreenSpillsItsRawStockAndReadyCoal(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); wall(h, a); var l = h.getLevel(); var p = holding(CoalMiningGameTests.player(h, a), ItemStack.EMPTY);
        var cart = cart(h, p, a.offset(1, -1, -1)); cart.insert(22);
        var screen = screen(h, a.offset(2, -1, 0)); use(p, screen);
        h.assertTrue(cart.load() == 0 && screen.raw() == 18 && screen.coal() == 1, "The screen holds 18 raw units and one Coal");
        l.destroyBlock(screen.getBlockPos(), true);
        h.assertTrue(loose(h) == 18 && dropped(h, KilnContent.MINERAL_COAL.get()) == 1 && dropped(h, CoalMiningContent.SCREEN_ITEM.get()) == 1, "Breaking it releases exactly that stock and the screen itself");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void savedBatchAndShovelReloadTheirExactUnits(GameTestHelper h) {
        var a = CoalMiningGameTests.face(h); var l = h.getLevel();
        var batch = LooseCoalEntity.create(l, a.getCenter().add(0, 1.5, 0), 37);
        var saved = new CompoundTag();
        h.assertTrue(batch.save(saved), "A falling batch is written with its chunk");
        batch.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertTrue(loose(h) == 0, "The unloaded batch is gone from the world, not copied");
        var restored = EntityType.loadEntityRecursive(saved, l, e -> e);
        h.assertTrue(restored instanceof LooseCoalEntity reloaded && reloaded.units() == 37 && l.addFreshEntity(restored) && loose(h) == 37, "Reloading restores the one batch with its 37 units");
        var shovel = CoalMiningContent.SHOVEL.toStack(); CoalMiningContent.load(shovel, 13);
        var stored = ItemStack.parse(l.registryAccess(), shovel.save(l.registryAccess())).orElseThrow();
        h.assertTrue(CoalMiningContent.load(stored) == 13, "A stored shovel keeps its load");
        h.succeed();
    }
}
