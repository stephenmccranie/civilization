package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class HotMetalGameTests {
    private static PlayerInteractEvent.RightClickBlock click(FakePlayer player, BlockPos anvil) {
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, anvil, new BlockHitResult(Vec3.atCenterOf(anvil), Direction.UP, anvil, false));
        HotMetal.strike(event);
        return event;
    }

    @GameTest(template = "industrial") public static void aHeatTakesTheWholeLoad(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(1, KilnContent.MINERAL_COAL.toStack(16));
        GradeGameTests.select(f, "heat_iron_ingot");
        f.setItem(0, GradeGameTests.ingots(1100, 1000, 950, 900, 850, 800, 750, 700, 650, 600));
        FoundryGameTests.run(h, f, 400 + 40);
        long now = h.getLevel().getGameTime();
        var load = f.getItem(2);
        h.assertTrue(load.is(Items.IRON_INGOT) && HotMetal.hot(load, now) && GradeGameTests.grades(load).equals(List.of(1100, 1000, 950, 900, 850, 800, 750, 700)),
                "One heat takes a whole load of eight, hot, each keeping its grade: " + GradeGameTests.grades(load));
        h.assertTrue(GradeGameTests.grades(f.getItem(0)).equals(List.of(650, 600)), "The rest wait for the next heat");
        h.assertTrue(!f.canTakeItemThroughFace(2, load, Direction.DOWN), "Hoppers cannot take hot metal");
        h.assertTrue(!Grade.stacks(load, new ItemStack(Items.IRON_INGOT)), "Hot metal stays apart from cold");
        var spent = HotMetal.heated(new ItemStack(Items.IRON_INGOT), now - HotMetal.COOL_TICKS);
        h.assertTrue(!HotMetal.hot(spent, now), "Metal heated a minute ago has cooled");
        HotMetal.cool(spent, now);
        h.assertTrue(Grade.stacks(spent, new ItemStack(Items.IRON_INGOT)), "Cooled metal stacks with cold metal again");
        f.setItem(0, ItemStack.EMPTY); f.setItem(2, ItemStack.EMPTY); f.selectRecipe(-1);
        f.setItem(0, new ItemStack(Items.DIAMOND)); FoundryGameTests.run(h, f, 400 + 40);
        h.assertTrue(f.getItem(2).is(Items.DIAMOND) && HotMetal.hot(f.getItem(2), now) && Grade.value(f.getItem(2)) == Grade.STANDARD,
                "A diamond, with no other Foundry job, heats on Automatic and keeps its grade");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void hammeringRefinesTheWholeLoadForItsLabor(GameTestHelper h) {
        var level = h.getLevel();
        var anvil = h.absolutePos(new BlockPos(2, 1, 2));
        // An anvil falls without a floor.
        level.setBlockAndUpdate(anvil.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(anvil, Blocks.ANVIL.defaultBlockState());
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "hammerer"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(anvil.getX() + .5, anvil.getY() + 1, anvil.getZ() + 1.5);
        var food = CalorieFoodData.of(player);
        food.reserve().set(2000);
        player.setItemInHand(InteractionHand.MAIN_HAND, GradeGameTests.ingots(900));
        h.assertTrue(!click(player, anvil).isCanceled() && food.reserve().calories() == 2000, "A cold ingot leaves the anvil to its ordinary use, free");
        player.setItemInHand(InteractionHand.MAIN_HAND, HotMetal.heated(GradeGameTests.ingots(1000, 900, 800), level.getGameTime()));
        // Every blow works all three units.
        double blow = 3 * CalorieConfig.HAMMER.get() / HotMetal.STRIKES;
        for (int i = 0; i < HotMetal.STRIKES; i++) {
            double before = food.reserve().calories();
            h.assertTrue(click(player, anvil).isCanceled(), "A hot ingot is struck instead of opening the anvil");
            if (i == 0) h.assertTrue(click(player, anvil).isCanceled() && before - food.reserve().calories() <= blow + 1e-9, "A blow takes a second to swing again");
            if (i == HotMetal.STRIKES - 2) h.assertTrue(HotMetal.hot(player.getMainHandItem(), level.getGameTime())
                    && GradeGameTests.grades(player.getMainHandItem()).equals(List.of(1000, 900, 800)), "Four blows finish nothing");
            // The swing recovers here instead of over a second.
            player.getCooldowns().removeCooldown(Items.IRON_INGOT);
        }
        var hand = player.getMainHandItem();
        var grades = GradeGameTests.grades(hand);
        h.assertTrue(hand.getCount() == 3 && !hand.has(HotMetal.HOT.get()) && grades.get(0) > 1000 && grades.get(1) > 900 && grades.get(2) > 800,
                "The fifth blow refines every unit of the load and leaves it cold: " + grades);
        double spent = 2000 - food.reserve().calories();
        h.assertTrue(spent >= .8 * HotMetal.STRIKES * blow - 1e-6 && spent <= HotMetal.STRIKES * blow + 1e-6, "Every blow is paid labor for each unit: " + spent + " kcal");
        h.succeed();
    }
}
