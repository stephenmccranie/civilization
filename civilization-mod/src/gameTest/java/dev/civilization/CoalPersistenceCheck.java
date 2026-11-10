package dev.civilization;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Coal custody across a server restart and a chunk unload. {@code seed} leaves every custodian holding stock in one
 * chunk and stops the server in the same tick, so a struck chunk is still falling and an undercut pile's fall is still
 * scheduled. {@code reload} counts the stock after the restart, waits until the chunk and its entities have been saved
 * and dropped from memory, loads them again and counts once more.
 */
@EventBusSubscriber(modid = "civilization")
public final class CoalPersistenceCheck {
    private static final String PHASE = Fixture.phase("coal");
    private static final int CHUNK_X = 20, CHUNK_Z = 20, SETTLE = 200;
    /** A floor-level cell in the middle of the chunk, on the flat world's stone. */
    private static final BlockPos ORIGIN = new BlockPos(CHUNK_X * 16 + 8, -61, CHUNK_Z * 16 + 8);
    private static final BlockPos FACE = ORIGIN.offset(-4, 0, -4), PILE = ORIGIN.offset(0, 0, -4), UNDERCUT = ORIGIN.offset(4, 0, -4),
            SLAB = ORIGIN.offset(-4, 0, 3), RAIL = ORIGIN.offset(0, 0, 3), SCREEN = ORIGIN.offset(2, 0, 3), CHEST = ORIGIN.offset(5, 0, 3);
    private static final AABB AREA = new AABB(ORIGIN).inflate(7, 4, 7);
    /** One corner patch struck from the face; a one-cell pillar left under the undercut pile. */
    private static final long STRUCK = ~0x33L, PILLAR = 0x22220000L;
    /**
     * 4 struck + 37 piled + 12 undercut + 7 blocked raw units lie loose; the cart, screen and shovel hold the rest.
     * Only the seed's last tick may see a floating pile: the undercut one, which must have fallen once the world runs again.
     */
    private static final String EXPECTED = "faces=" + Long.toHexString(STRUCK) + "," + Long.toHexString(PILLAR) + " loose=60 blocked=7 floating=" + (PHASE.equals("seed") ? 1 : 0) + " cart=123 screen=135/1 shovel=9";
    private static int ticks, unforcedAt, reforcedAt;
    /** The server has saved the unforced chunk and dropped it, so the next load reads what was saved. */
    private static boolean dropped;

    @SubscribeEvent public static void start(ServerStartedEvent e) {
        ticks = unforcedAt = reforcedAt = 0; dropped = false;
        if (!PHASE.isEmpty()) e.getServer().overworld().setChunkForced(CHUNK_X, CHUNK_Z, true);
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload e) {
        var at = e.getChunk().getPos();
        if (unforcedAt > 0 && at.x == CHUNK_X && at.z == CHUNK_Z && e.getLevel() instanceof ServerLevel l && l == l.getServer().overworld()) dropped = true;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        if (PHASE.isEmpty()) return;
        var l = e.getServer().overworld(); ticks++;
        if (PHASE.equals("seed")) {
            if (ticks != 100) return;
            seed(l); verify(l, "as seeded");
            System.out.println("COAL_SEED_PASS " + EXPECTED); e.getServer().halt(false);
        } else if (ticks == SETTLE) {
            verify(l, "across the restart");
            l.setChunkForced(CHUNK_X, CHUNK_Z, false); unforcedAt = ticks;
        } else if (unforcedAt > 0 && reforcedAt == 0) {
            if (dropped && l.getEntitiesOfClass(CoalMinecart.class, AREA).isEmpty()) {
                l.setChunkForced(CHUNK_X, CHUNK_Z, true); reforcedAt = ticks;
            } else if (ticks - unforcedAt > 1200) throw new IllegalStateException("The coal chunk did not unload within a minute");
        } else if (reforcedAt > 0 && ticks == reforcedAt + SETTLE) {
            verify(l, "across the chunk unload");
            System.out.println("COAL_RELOAD_PASS unloaded_for_ticks=" + (reforcedAt - unforcedAt) + " " + EXPECTED); e.getServer().halt(false);
        }
    }

    private static void seed(ServerLevel l) {
        if (!l.getBlockState(ORIGIN.below()).is(Blocks.STONE) || !l.getBlockState(ORIGIN).isAir()) throw new IllegalStateException("The fixture floor is not where the flat world should put it");
        var p = new FakePlayer(l, new GameProfile(java.util.UUID.randomUUID(), "coal-fixture"));
        p.setGameMode(GameType.SURVIVAL); CalorieFoodData.of(p).reserve().set(2400);

        // A real strike: its four-unit chunk is still falling when the server stops.
        l.setBlockAndUpdate(FACE, CoalMiningContent.FACE.get().defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND, CoalMiningContent.PICK.toStack());
        p.setPos(FACE.getX() + .5, FACE.getY(), FACE.getZ() - 2);
        CoalMiningGameTests.aim(p, Vec3.atLowerCornerOf(FACE).add(.25, .25, 0));
        if (!CoalMiningSystem.strike(p)) throw new IllegalStateException("The seeding strike met no coal");

        l.setBlockAndUpdate(PILE, CoalMiningContent.PILE.get().defaultBlockState().setValue(RawCoalPileBlock.UNITS, 37));

        // A pile whose support is cut away in this tick: its fall is a scheduled tick the save must keep.
        l.setBlockAndUpdate(UNDERCUT, CoalMiningContent.FACE.get().defaultBlockState());
        if (RawCoalPileBlock.insert(l, UNDERCUT.above(), 12) != 12) throw new IllegalStateException("The intact face did not take the pile");
        ((CoalWorkfaceEntity) l.getBlockEntity(UNDERCUT)).mask(PILLAR);

        // A batch that can never settle: a slab floor inside stone walls.
        for (var wall : BlockPos.betweenClosed(SLAB.offset(-1, 0, -1), SLAB.offset(1, 1, 1))) l.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        l.setBlockAndUpdate(SLAB, Blocks.STONE_SLAB.defaultBlockState()); l.setBlockAndUpdate(SLAB.above(), Blocks.AIR.defaultBlockState());
        if (LooseCoalEntity.create(l, SLAB.getCenter().add(0, .2, 0), 7) == null) throw new IllegalStateException("The blocked batch did not spawn");

        // A cart placed with its item, a screen fed and worked through its block, then the cargo that stays aboard.
        l.setBlockAndUpdate(RAIL, Blocks.RAIL.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND, CoalMiningContent.CART_ITEM.toStack());
        if (CoalMiningContent.CART_ITEM.get().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(RAIL.getCenter(), Direction.UP, RAIL, false))) != InteractionResult.CONSUME) throw new IllegalStateException("The cart was not placed");
        var cart = l.getEntitiesOfClass(CoalMinecart.class, new AABB(RAIL)).getFirst();
        cart.insert(139);
        l.setBlockAndUpdate(SCREEN, CoalMiningContent.SCREEN.get().defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.setPos(SCREEN.getX() + .5, SCREEN.getY(), SCREEN.getZ() - 1.5);
        l.getBlockState(SCREEN).useWithoutItem(l, p, new BlockHitResult(SCREEN.getCenter(), Direction.UP, SCREEN, false));
        cart.insert(123);

        l.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
        var shovel = CoalMiningContent.SHOVEL.toStack(); CoalMiningContent.load(shovel, 9);
        var chest = (ChestBlockEntity) l.getBlockEntity(CHEST); chest.setItem(0, shovel); chest.setChanged();
    }

    private static void verify(ServerLevel l, String when) {
        int loose = 0, blocked = 0, floating = 0;
        for (var p : BlockPos.betweenClosed(ORIGIN.offset(-7, 0, -7), ORIGIN.offset(7, 3, 7))) {
            var s = l.getBlockState(p);
            if (!s.is(CoalMiningContent.PILE.get())) continue;
            loose += s.getValue(RawCoalPileBlock.UNITS);
            if (!RawCoalPileBlock.supported(l, p)) floating++;
        }
        for (var e : l.getEntitiesOfClass(LooseCoalEntity.class, AREA)) {
            loose += e.units();
            if (e.blockPosition().equals(SLAB)) blocked += e.units();
        }
        int cart = l.getEntitiesOfClass(CoalMinecart.class, AREA).stream().mapToInt(CoalMinecart::load).sum();
        var faces = mask(l, FACE) + "," + mask(l, UNDERCUT);
        var screen = l.getBlockEntity(SCREEN) instanceof CoalScreenEntity s ? s.raw() + "/" + s.coal() : "missing";
        var shovel = l.getBlockEntity(CHEST) instanceof ChestBlockEntity c ? CoalMiningContent.load(c.getItem(0)) : -1;
        var found = "faces=" + faces + " loose=" + loose + " blocked=" + blocked + " floating=" + floating + " cart=" + cart + " screen=" + screen + " shovel=" + shovel;
        if (!found.equals(EXPECTED)) throw new IllegalStateException("Coal custody changed " + when + ": found " + found + ", expected " + EXPECTED);
    }
    private static String mask(ServerLevel l, BlockPos at) {
        return l.getBlockEntity(at) instanceof CoalWorkfaceEntity e ? Long.toHexString(e.mask()) : "missing";
    }
}
