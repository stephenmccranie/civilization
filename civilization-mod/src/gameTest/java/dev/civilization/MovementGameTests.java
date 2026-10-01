package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public final class MovementGameTests {
    @GameTest(template = "empty")
    public static void ordinaryGroundSpeedsAndJumping(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(4, 1, 4));
        for (int x = -2; x <= 2; x++) for (int z = -2; z < 80; z++) {
            var p = origin.offset(x, -1, z);
            level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
            for (int y = 1; y <= 4; y++) level.setBlockAndUpdate(p.above(y), Blocks.AIR.defaultBlockState());
        }
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "movement-test"));
        double walk = measure(player, origin, false, false);
        double sprint = measure(player, origin, true, false);
        double hopping = measure(player, origin, true, true);
        h.assertTrue(Math.abs(walk - 3.2) < .03, "Walking 3.2 blocks/s, measured " + walk);
        h.assertTrue(Math.abs(sprint - 4.6) < .03, "Sprinting 4.6 blocks/s, measured " + sprint);
        h.assertTrue(Math.abs(hopping - sprint * 1.20) < .025, "Sprint jumping adds 20%: " + hopping);
        System.out.printf("Movement measured: walk %.3f, sprint %.3f, sprint jumping %.3f blocks/s%n", walk, sprint, hopping);
        for (int x = -2; x <= 2; x++) for (int z = -2; z < 80; z++)
            level.setBlockAndUpdate(origin.offset(x, -1, z), RoadContent.PAVERS.get().defaultBlockState());
        double roadWalk = measure(player, origin, false, false);
        double roadSprint = measure(player, origin, true, false);
        h.assertTrue(Math.abs(roadWalk - walk * 1.25) < .025, "Street paving walking +25%: " + roadWalk);
        h.assertTrue(Math.abs(roadSprint - sprint * 1.25) < .025, "Street paving sprinting +25%: " + roadSprint);
        System.out.printf("Street paving measured: walk %.3f, sprint %.3f blocks/s%n", roadWalk, roadSprint);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pavingUsesTheContactSurface(GameTestHelper h) {
        var level = h.getLevel();
        var p = h.absolutePos(new BlockPos(2, 2, 2));
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "road-contact"));
        level.setBlockAndUpdate(p, RoadContent.SLAB.get().defaultBlockState());
        player.setPos(p.getX() + .25, p.getY() + .5, p.getZ() + .25);
        player.setOnGround(true);
        h.assertTrue(RoadSurface.supports(player), "Street paver slabs are roads");
        player.setOnGround(false);
        h.assertTrue(!RoadSurface.supports(player), "No airborne road buff");
        player.setOnGround(true);
        player.getAbilities().flying = true;
        h.assertTrue(!RoadSurface.supports(player), "No creative flight road buff");
        player.getAbilities().flying = false;
        level.setBlockAndUpdate(p, CuttingContent.PIECE.get().defaultBlockState());
        var cut = (CutBlockEntity) level.getBlockEntity(p);
        var cells = new net.minecraft.world.level.block.state.BlockState[8];
        cells[0] = RoadContent.PAVERS.get().defaultBlockState();
        cells[1] = Blocks.STONE.defaultBlockState();
        cut.cells(cells);
        h.assertTrue(RoadSurface.supports(player), "Paver eighth-cube surface qualifies");
        player.setPos(p.getX() + .85, p.getY() + .5, p.getZ() + .25);
        h.assertTrue(!RoadSurface.supports(player), "Stone part of mixed assembly is not a road");
        cells[2] = Blocks.STONE.defaultBlockState();
        cut.cells(cells);
        player.setPos(p.getX() + .25, p.getY() + 1, p.getZ() + .25);
        h.assertTrue(!RoadSurface.supports(player), "Buried paving does not boost a stone surface");
        level.setBlockAndUpdate(p, Blocks.STONE_BRICKS.defaultBlockState());
        h.assertTrue(!RoadSurface.supports(player), "Stone bricks are not streets");
        level.setBlockAndUpdate(p, Blocks.BRICKS.defaultBlockState());
        h.assertTrue(!RoadSurface.supports(player), "Wall bricks no longer give a street bonus");
        level.setBlockAndUpdate(p, RoadContent.STAIRS.get().defaultBlockState());
        h.assertTrue(RoadSurface.supports(player), "Street paver stairs give a street bonus");
        h.succeed();
    }

    private static double measure(FakePlayer player, BlockPos origin, boolean sprint, boolean jump) {
        player.setPos(Vec3.atBottomCenterOf(origin));
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(0);
        player.setOnGround(true);
        player.setSprinting(sprint);
        player.setSpeed((float) player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double start = 0;
        for (int tick = 0; tick < 130; tick++) {
            if (tick == 30) start = player.getZ();
            if (jump && player.onGround()) player.jumpFromGround();
            player.travel(new Vec3(0, 0, .98));
        }
        return (player.getZ() - start) / 5;
    }

    @GameTest(template = "empty")
    public static void oldPlayerAbilitiesMigrateAndSprintDoesNotStack(GameTestHelper h) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "movement-save"));
        p.getAbilities().setWalkingSpeed(.1f);
        var saved = new CompoundTag();
        p.addAdditionalSaveData(saved);
        p.readAdditionalSaveData(saved);
        h.assertTrue(p.getAbilities().getWalkingSpeed() == PlayerMovement.WALK_ATTRIBUTE, "Old walking ability updated on load");
        p.setSprinting(true);
        p.setSprinting(true);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) / PlayerMovement.WALK_ATTRIBUTE - 4.6 / 3.2) < .00001, "Sprint multiplier cannot stack");
        p.setSprinting(false);
        h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - PlayerMovement.WALK_ATTRIBUTE) < .00001, "Stopping restores walking");
        h.assertTrue(p.getAbilities().getFlyingSpeed() == .05f, "Creative flight ability unchanged");
        h.succeed();
    }
}
