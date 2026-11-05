package dev.civilization;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Trees are built by hand inside the 12-block cube, so each case knows exactly which logs it holds. */
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class TreeFellingGameTests {
    private static final String CUBE = "thermal_interface";
    private static final BlockState NATURAL = Blocks.OAK_LEAVES.defaultBlockState();

    private static FakePlayer player(GameTestHelper h, GameType mode, ItemStack tool) {
        var p = new FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "feller"));
        p.setGameMode(mode); p.setItemInHand(InteractionHand.MAIN_HAND, tool); return p;
    }
    /** Fills the inclusive box, leaving logs already there so a crown can wrap its trunk. */
    private static void fill(GameTestHelper h, BlockState state, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (var at : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) if (!h.getBlockState(at).is(net.minecraft.tags.BlockTags.LOGS)) h.setBlock(at, state);
    }
    private static void cut(GameTestHelper h, FakePlayer p, int x, int y, int z) {
        h.assertTrue(p.gameMode.destroyBlock(h.absolutePos(new BlockPos(x, y, z))), "The cut itself breaks");
    }
    private static boolean is(GameTestHelper h, Block block, int x, int y, int z) { return h.getBlockState(new BlockPos(x, y, z)).is(block); }
    private static int logs(GameTestHelper h, Block log) {
        int n = 0; for (var at : BlockPos.betweenClosed(0, 0, 0, 11, 11, 11)) if (h.getBlockState(at).is(log)) n++; return n;
    }
    private static int dropped(GameTestHelper h, Item item) {
        var cube = AABB.encapsulatingFullBlocks(h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(11, 11, 11))).inflate(1);
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, cube, e -> e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }
    /** An oak trunk from y=1 to {@code top} at (5, 5) under a natural crown. */
    private static void oak(GameTestHelper h, int top, BlockState leaves) {
        fill(h, Blocks.OAK_LOG.defaultBlockState(), 5, 1, 5, 5, top, 5);
        fill(h, leaves, 3, top - 1, 3, 7, top + 1, 7);
    }

    @GameTest(template = CUBE) public static void oakFellsFromTheCutUpAndLeavesTheStump(GameTestHelper h) {
        oak(h, 6, NATURAL); var p = player(h, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE));
        var effects = new java.util.ArrayList<BlockPos>();
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), p, CommonListenerCookie.createInitial(p.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet, PacketSendListener listener) {
                if (packet instanceof ClientboundLevelEventPacket e && e.getType() == LevelEvent.PARTICLES_DESTROY_BLOCK && e.getData() == Block.getId(Blocks.OAK_LOG.defaultBlockState())) effects.add(e.getPos());
            }
        };
        cut(h, p, 5, 2, 5);
        h.assertTrue(effects.equals(java.util.stream.IntStream.rangeClosed(3, 6).mapToObj(y -> h.absolutePos(new BlockPos(5, y, 5))).toList()),
                "The cutter sees and hears each felled log break, but not the cut their client already showed");
        h.assertTrue(is(h, Blocks.OAK_LOG, 5, 1, 5) && logs(h, Blocks.OAK_LOG) == 1, "Every log from the cut up falls and the stump stays");
        h.assertTrue(is(h, Blocks.OAK_LEAVES, 4, 6, 5), "Leaves are left to decay");
        h.assertTrue(dropped(h, Items.OAK_LOG) == 5, "Each removed log drops itself once");
        h.assertTrue(p.getMainHandItem().getDamageValue() == 5, "The axe wears once per log");
        h.runAfterDelay(2, () -> { h.assertTrue(CalorieFoodData.of(p).broken == 5, "Each log costs ordinary breaking labor"); h.succeed(); });
    }

    @GameTest(template = CUBE) public static void branchesAboveTheCutFallButRootsBelowItStay(GameTestHelper h) {
        var log = Blocks.ACACIA_LOG.defaultBlockState(); var leaves = Blocks.ACACIA_LEAVES.defaultBlockState();
        fill(h, log, 5, 1, 5, 5, 3, 5);
        for (var at : new BlockPos[]{new BlockPos(6, 4, 5), new BlockPos(7, 5, 5), new BlockPos(7, 6, 5), new BlockPos(4, 4, 6), new BlockPos(3, 5, 7)}) h.setBlock(at, log);
        fill(h, leaves, 6, 7, 4, 8, 7, 6); fill(h, leaves, 2, 6, 6, 4, 6, 8);
        h.setBlock(new BlockPos(6, 1, 5), log); h.setBlock(new BlockPos(7, 2, 5), log);
        cut(h, player(h, GameType.SURVIVAL, ItemStack.EMPTY), 5, 2, 5);
        h.assertTrue(logs(h, Blocks.ACACIA_LOG) == 3 && is(h, Blocks.ACACIA_LOG, 5, 1, 5) && is(h, Blocks.ACACIA_LOG, 6, 1, 5),
                "Diagonal branches fall with the trunk, bare-handed, and the stump and roots stay");
        h.assertTrue(is(h, Blocks.ACACIA_LOG, 7, 2, 5), "A log joined to the tree only below the cut stays");
        h.assertTrue(dropped(h, Items.ACACIA_LOG) == 7, "Drops equal the logs removed"); h.succeed();
    }

    @GameTest(template = CUBE) public static void netherStemsFellUnderWartBlocks(GameTestHelper h) {
        fill(h, Blocks.CRIMSON_STEM.defaultBlockState(), 5, 1, 5, 5, 4, 5); fill(h, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 4, 4, 4, 6, 5, 6);
        cut(h, player(h, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE)), 5, 2, 5);
        h.assertTrue(logs(h, Blocks.CRIMSON_STEM) == 1 && dropped(h, Items.CRIMSON_STEM) == 3, "A huge fungus fells like a tree"); h.succeed();
    }

    @GameTest(template = CUBE) public static void logWallsWithoutNaturalLeavesStand(GameTestHelper h) {
        fill(h, Blocks.OAK_LOG.defaultBlockState(), 3, 1, 5, 7, 4, 5);
        cut(h, player(h, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE)), 5, 2, 5);
        h.assertTrue(logs(h, Blocks.OAK_LOG) == 19 && dropped(h, Items.OAK_LOG) == 1, "Only the cut log of a wall breaks"); h.succeed();
    }

    @GameTest(template = CUBE) public static void placedLeavesDoNotMakeATree(GameTestHelper h) {
        oak(h, 6, NATURAL.setValue(LeavesBlock.PERSISTENT, true));
        cut(h, player(h, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE)), 5, 2, 5);
        h.assertTrue(logs(h, Blocks.OAK_LOG) == 5 && dropped(h, Items.OAK_LOG) == 1, "Persistent leaves leave the rest standing"); h.succeed();
    }

    @GameTest(template = CUBE) public static void claimedLogsStayWithTheirOwner(GameTestHelper h) {
        oak(h, 5, NATURAL);
        for (int x = 6; x <= 8; x++) h.setBlock(new BlockPos(x, 5, 5), Blocks.OAK_LOG.defaultBlockState());
        var d = CivicData.get(h.getLevel().getServer()); long now = System.currentTimeMillis(); var c = new CivicData.Claim();
        c.at = new CivicData.Address("minecraft:overworld", h.absolutePos(new BlockPos(9, 5, 6))); c.owner = new CivicData.Owner(UUID.randomUUID(), false);
        c.radius = 1; c.height = 32; c.upkeep = 1; c.coalUnit = 3600000; c.energy = c.coalUnit * 200; c.lastUpdate = now; c.eligibleAt = now;
        d.claims.put(c.at, c); d.rebuildIndex();
        try {
            cut(h, player(h, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE)), 5, 2, 5);
            h.assertTrue(is(h, Blocks.OAK_LOG, 8, 5, 5) && is(h, Blocks.OAK_LOG, 5, 1, 5) && logs(h, Blocks.OAK_LOG) == 2,
                    "The log inside another owner's claim stays; the rest of the tree falls");
            h.assertTrue(dropped(h, Items.OAK_LOG) == 6, "Only permitted logs drop");
        } finally { d.claims.remove(c.at); d.rebuildIndex(); }
        h.succeed();
    }

    @GameTest(template = CUBE) public static void oversizedStructuresAreLeftAlone(GameTestHelper h) {
        fill(h, Blocks.OAK_LOG.defaultBlockState(), 2, 1, 2, 9, 5, 9); fill(h, NATURAL, 2, 6, 2, 9, 6, 9);
        h.assertTrue(8 * 5 * 8 > TreeFelling.MAX_LOGS, "The block exceeds the cap");
        cut(h, player(h, GameType.SURVIVAL, new ItemStack(Items.IRON_AXE)), 5, 1, 5);
        h.assertTrue(logs(h, Blocks.OAK_LOG) == 8 * 5 * 8 - 1 && dropped(h, Items.OAK_LOG) == 1, "Past the cap only the cut log breaks"); h.succeed();
    }

    @GameTest(template = CUBE) public static void fellingStopsWhereTheAxeWouldBreak(GameTestHelper h) {
        oak(h, 8, NATURAL); var axe = new ItemStack(Items.IRON_AXE); axe.setDamageValue(axe.getMaxDamage() - 4);
        var p = player(h, GameType.SURVIVAL, axe);
        cut(h, p, 5, 2, 5);
        h.assertTrue(logs(h, Blocks.OAK_LOG) == 4 && is(h, Blocks.OAK_LOG, 5, 1, 5) && is(h, Blocks.OAK_LOG, 5, 6, 5) && !is(h, Blocks.OAK_LOG, 5, 5, 5),
                "Four durability fells three logs above the cut and leaves the rest");
        h.assertTrue(p.getMainHandItem().isEmpty(), "The axe breaks on the cut, as in ordinary chopping");
        h.assertTrue(dropped(h, Items.OAK_LOG) == 4, "Drops equal the logs removed"); h.succeed();
    }

    @GameTest(template = CUBE) public static void creativeBreaksOneLog(GameTestHelper h) {
        oak(h, 6, NATURAL);
        cut(h, player(h, GameType.CREATIVE, new ItemStack(Items.IRON_AXE)), 5, 2, 5);
        h.assertTrue(logs(h, Blocks.OAK_LOG) == 5 && dropped(h, Items.OAK_LOG) == 0, "Creative breaks only the clicked log"); h.succeed();
    }
}
