package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class RecoveryGameTests {
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "recovery-test"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    @GameTest(template = "empty", timeoutTicks = 1800)
    public static void emptyPlayerForagesAndEats(GameTestHelper helper) {
        var player = player(helper);
        var data = CalorieFoodData.of(player);
        data.reserve().set(0);
        data.updateRecovery(player);
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(ground.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(ground, Blocks.SAND.defaultBlockState());
        player.setPos(Vec3.atCenterOf(ground.above()));
        player.setShiftKeyDown(true);
        helper.assertTrue(Foraging.canForage(player, ground), "Empty player must be able to forage desert ground");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND,
                ground, new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
        helper.runAfterDelay(CalorieConfig.FORAGE_TICKS.get() + 2, () -> {
            var morsel = player.getInventory().items.stream().filter(stack -> stack.is(RecoveryItems.MORSEL.get())).findFirst().orElseThrow();
            helper.assertTrue(morsel.getCount() == 1, "One deliberate forage must yield one morsel");
            helper.assertTrue(data.reserve().calories() == 0, "Finding an item must not directly create body calories");
            player.eat(helper.getLevel(), morsel, morsel.getFoodProperties(player));
            helper.assertTrue(data.reserve().calories() == 25, "Morsel must actually restore 25 kcal");
            helper.assertTrue(data.isDepleted(), "One small morsel must not clear depletion");
            finishRecovery(helper, player, ground, 7);
        });
    }

    private static void finishRecovery(GameTestHelper helper, FakePlayer player, BlockPos ground, int remaining) {
        if (remaining == 0) {
            var data = CalorieFoodData.of(player);
            helper.assertTrue(data.reserve().calories() == 200 && !data.isDepleted(),
                    "Eight genuinely foraged and eaten morsels must restore normal work from zero");
            helper.succeed();
            return;
        }
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND,
                ground, new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
        helper.runAfterDelay(CalorieConfig.FORAGE_TICKS.get() + 2, () -> {
            var morsel = player.getInventory().items.stream().filter(stack -> stack.is(RecoveryItems.MORSEL.get())).findFirst().orElseThrow();
            player.eat(helper.getLevel(), morsel, morsel.getFoodProperties(player));
            finishRecovery(helper, player, ground, remaining - 1);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void movingCancelsForaging(GameTestHelper helper) {
        var player = player(helper);
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
        player.setPos(Vec3.atCenterOf(ground.above()));
        player.setShiftKeyDown(true);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND,
                ground, new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
        player.setPos(player.position().add(1, 0, 0));
        helper.runAfterDelay(CalorieConfig.FORAGE_TICKS.get() + 2, () -> {
            helper.assertTrue(player.getInventory().items.stream().noneMatch(stack -> stack.is(RecoveryItems.MORSEL.get())),
                    "Interrupted forage must not grant food");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void depletionPersistsAndFoodRestoresWork(GameTestHelper helper) {
        var player = player(helper);
        var data = CalorieFoodData.of(player);
        data.reserve().set(0);
        data.updateRecovery(player);
        var speed = new PlayerEvent.BreakSpeed(player, Blocks.STONE.defaultBlockState(), 8, BlockPos.ZERO);
        NeoForge.EVENT_BUS.post(speed);
        helper.assertTrue(speed.getNewSpeed() == 2, "Depleted mining must be four times slower");
        data.consume(player, 25, "test-food");
        var tag = new CompoundTag();
        data.addAdditionalSaveData(tag);
        var restored = player(helper);
        restored.getFoodData().readAdditionalSaveData(tag);
        helper.assertTrue(CalorieFoodData.of(restored).isDepleted(), "Relog must not remove depletion");
        data.consume(player, 175, "test-food");
        helper.assertTrue(!data.isDepleted(), "200 kcal must restore normal work");
        speed = new PlayerEvent.BreakSpeed(player, Blocks.STONE.defaultBlockState(), 8, BlockPos.ZERO);
        NeoForge.EVENT_BUS.post(speed);
        helper.assertTrue(speed.getNewSpeed() == 8, "Recovered mining speed must be normal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void starvationStopsAndDepletedHealingWaits(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(0);
        data.updateRecovery(player);
        player.setHealth(6.5f);
        for (int i = 0; i < 400; i++) data.tick(player);
        helper.assertTrue(player.getHealth() == 6, "Starvation at floor must not damage player");
        data.consume(player, 100, "test-food");
        for (int i = 0; i < 80; i++) data.tick(player);
        helper.assertTrue(player.getHealth() == 6 && data.reserve().calories() == 100, "Healing must wait for recovery threshold");
        data.consume(player, 100, "test-food");
        for (int i = 0; i < 80; i++) data.tick(player);
        helper.assertTrue(player.getHealth() == 7 && data.reserve().calories() == 160, "Healing must resume after recovery");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zeroCaloriesAllowsRealBlockBreaking(GameTestHelper helper) {
        var player = player(helper);
        var data = CalorieFoodData.of(player);
        data.reserve().set(0);
        data.updateRecovery(player);
        BlockPos block = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(block, Blocks.OAK_LOG.defaultBlockState());
        player.setPos(Vec3.atCenterOf(block.above()));
        helper.assertTrue(player.gameMode.destroyBlock(block), "Basic breaking cannot be blocked by an empty calorie reserve");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(data.broken == 1 && data.reserve().calories() == 0, "Empty labor is logged, with no negative calorie debt");
            helper.succeed();
        });
    }
}

