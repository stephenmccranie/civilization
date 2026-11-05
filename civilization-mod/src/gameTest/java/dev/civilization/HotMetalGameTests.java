package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
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
    private static PlayerInteractEvent.RightClickBlock click(FakePlayer player, BlockPos block) {
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, block, new BlockHitResult(Vec3.atCenterOf(block), Direction.UP, block, false));
        HotMetal.use(event);
        return event;
    }
    /** A smith standing over `station` with a full calorie reserve. */
    private static FakePlayer smith(GameTestHelper h, BlockPos station) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "smith"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(station.getX() + .5, station.getY() + 1, station.getZ() + 1.5);
        CalorieFoodData.of(player).reserve().set(2000);
        return player;
    }
    /** A station standing on stone, since an anvil falls without a floor. */
    private static BlockPos station(GameTestHelper h, int x, net.minecraft.world.level.block.Block block) {
        var pos = h.absolutePos(new BlockPos(x, 1, 2));
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return pos;
    }
    /** Works the stack in hand to the end, a blow at a time, as held use would; returns the food it cost. */
    private static double work(GameTestHelper h, FakePlayer player, BlockPos station, int blows) {
        var food = CalorieFoodData.of(player);
        double before = food.reserve().calories();
        for (int i = 0; i < blows; i++) {
            h.assertTrue(click(player, station).isCanceled(), "The stack is worked instead of the station's own use");
            // The swing recovers here instead of over a second.
            player.getCooldowns().removeCooldown(player.getMainHandItem().getItem());
        }
        return before - food.reserve().calories();
    }

    @GameTest(template = "industrial") public static void theFoundryMakesRoughMetalAndKeepsItHot(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(1, KilnContent.MINERAL_COAL.toStack(16));
        f.setItem(0, new ItemStack(Items.RAW_IRON, 3));
        FoundryGameTests.run(h, f, 3 * 200 + 40);
        long now = h.getLevel().getGameTime();
        var blooms = f.getItem(2);
        h.assertTrue(blooms.is(KilnContent.IRON_BLOOM.get()) && blooms.getCount() == 3 && HotMetal.glowing(blooms, now) && Grade.of(blooms).units().size() > 1,
                "Iron ore smelts into hot blooms that share one stack, each rolling its own grade");
        h.assertTrue(!f.canTakeItemThroughFace(2, blooms, Direction.DOWN), "Hoppers cannot take hot metal");
        long later = now + 10 * HotMetal.COOL_TICKS;
        HotMetal.tend(blooms, true, later);
        h.assertTrue(HotMetal.glowing(blooms, later), "Metal in a hot furnace stays hot however long it waits");
        HotMetal.tend(blooms, false, later + HotMetal.COOL_TICKS);
        h.assertTrue(!blooms.has(HotMetal.HOT.get()) && Grade.stacks(blooms, new ItemStack(KilnContent.IRON_BLOOM.get())), "Out of the fire it cools a minute later and stacks with cold metal");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void aHeatTakesTheWholeLoad(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(1, KilnContent.MINERAL_COAL.toStack(16));
        GradeGameTests.select(f, "heat_iron_ingot");
        f.setItem(0, GradeGameTests.ingots(1100, 1000, 950, 900, 850, 800, 750, 700, 650, 600));
        FoundryGameTests.run(h, f, 400 + 40);
        long now = h.getLevel().getGameTime();
        var load = f.getItem(2);
        h.assertTrue(load.is(Items.IRON_INGOT) && HotMetal.glowing(load, now) && GradeGameTests.grades(load).equals(List.of(1100, 1000, 950, 900, 850, 800, 750, 700)),
                "One heat takes a whole load of eight, hot, each keeping its grade: " + GradeGameTests.grades(load));
        h.assertTrue(GradeGameTests.grades(f.getItem(0)).equals(List.of(650, 600)), "The rest wait for the next heat");
        h.assertTrue(!Grade.stacks(load, new ItemStack(Items.IRON_INGOT)), "Hot metal stays apart from cold");
        var spent = HotMetal.heated(new ItemStack(Items.IRON_INGOT), now - HotMetal.COOL_TICKS);
        h.assertTrue(!HotMetal.hot(spent, now), "Metal heated a minute ago has cooled");
        HotMetal.cool(spent, now);
        h.assertTrue(Grade.stacks(spent, new ItemStack(Items.IRON_INGOT)), "Cooled metal stacks with cold metal again");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void hammeringBeatsRoughMetalIntoIngotsForItsLabor(GameTestHelper h) {
        var level = h.getLevel();
        var anvil = station(h, 2, Blocks.ANVIL);
        var player = smith(h, anvil);
        double blow = 3 * CalorieConfig.BLOW.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, GradeGameTests.units(KilnContent.IRON_BLOOM.get(), 900, 800, 700));
        h.assertTrue(click(player, anvil).isCanceled() && player.getMainHandItem().is(KilnContent.IRON_BLOOM.get()) && CalorieFoodData.of(player).reserve().calories() == 2000,
                "Cold rough metal is turned away from the anvil, free");
        HotMetal.heated(player.getMainHandItem(), level.getGameTime());
        double before = CalorieFoodData.of(player).reserve().calories();
        click(player, anvil);
        click(player, anvil);
        h.assertTrue(before - CalorieFoodData.of(player).reserve().calories() <= blow + 1e-9, "A blow takes a second to swing again");
        player.getCooldowns().removeCooldown(KilnContent.IRON_BLOOM.get());
        double spent = before - CalorieFoodData.of(player).reserve().calories() + work(h, player, anvil, HotMetal.ANVIL_BLOWS - 1);
        var bars = player.getMainHandItem();
        var grades = GradeGameTests.grades(bars);
        long now = level.getGameTime();
        h.assertTrue(bars.is(Items.IRON_INGOT) && bars.getCount() == 3 && grades.get(0) > 900 && grades.get(1) > 800 && grades.get(2) > 700,
                "Five blows on an iron anvil beat the whole stack of blooms into ingots, each a grade step up: " + grades);
        h.assertTrue(HotMetal.hot(bars, now) && !HotMetal.glowing(bars, now), "A hammering spends the working heat and leaves the bars to cool");
        h.assertTrue(spent >= .8 * HotMetal.ANVIL_BLOWS * blow - 1e-6 && spent <= HotMetal.ANVIL_BLOWS * blow + 1e-6, "Every blow is paid labor for each unit: " + spent + " kcal");
        h.assertTrue(click(player, anvil).isCanceled() && GradeGameTests.grades(player.getMainHandItem()).equals(grades), "Metal too cool to glow can't be hammered again");
        var cauldron = station(h, 4, Blocks.WATER_CAULDRON);
        h.assertTrue(click(player, cauldron).isCanceled() && !player.getMainHandItem().has(HotMetal.HOT.get()) && Grade.stacks(player.getMainHandItem(), new ItemStack(Items.IRON_INGOT)),
                "Water quenches the bars at once");
        var stone = station(h, 6, KilnContent.STONE_ANVIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, HotMetal.heated(GradeGameTests.units(KilnContent.BLISTER_STEEL.get(), 800), level.getGameTime()));
        work(h, player, stone, HotMetal.STONE_ANVIL_BLOWS - 1);
        h.assertTrue(player.getMainHandItem().is(KilnContent.BLISTER_STEEL.get()), "A Stone Anvil takes more blows than an iron one");
        work(h, player, stone, 1);
        h.assertTrue(player.getMainHandItem().is(KilnContent.STEEL.get()) && Grade.value(player.getMainHandItem()) > 800, "Blister steel hammers into a steel ingot on stone");
        var grindstone = station(h, 8, Blocks.GRINDSTONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, GradeGameTests.units(Items.DIAMOND, 900, 900));
        work(h, player, grindstone, HotMetal.GRINDSTONE_STROKES);
        h.assertTrue(player.getMainHandItem().is(Items.DIAMOND) && player.getMainHandItem().getCount() == 2 && GradeGameTests.grades(player.getMainHandItem()).stream().allMatch(g -> g > 900)
                && !player.getMainHandItem().has(HotMetal.HOT.get()), "A grindstone cuts diamonds cold, each a grade step up");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void hotMetalDroppedInWaterIsQuenched(GameTestHelper h) {
        var level = h.getLevel();
        var pool = h.absolutePos(new BlockPos(2, 1, 2));
        for (var side : Direction.values()) if (side != Direction.UP) level.setBlockAndUpdate(pool.relative(side), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pool, Blocks.WATER.defaultBlockState());
        var item = new ItemEntity(level, pool.getX() + .5, pool.getY() + .5, pool.getZ() + .5, HotMetal.heated(new ItemStack(Items.COPPER_INGOT), level.getGameTime()));
        item.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(item);
        h.succeedWhen(() -> h.assertTrue(!item.getItem().has(HotMetal.HOT.get()), "Hot metal dropped into water cools at once"));
    }
}
