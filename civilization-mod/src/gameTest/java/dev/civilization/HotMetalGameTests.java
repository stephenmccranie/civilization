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
    /** The stack, `seconds` of cooling past the ideal moment to quench it: negative is hotter. */
    private static ItemStack aged(ItemStack stack, long now, int seconds) {
        stack.set(HotMetal.HOT.get(), now + HotMetal.COOL_TICKS - HotMetal.IDEAL_TICKS - seconds * 20L);
        return stack;
    }
    /** Every unit's grade less `tenths`. */
    private static List<Integer> less(List<Integer> grades, int tenths) { return grades.stream().map(grade -> grade - tenths).toList(); }

    @GameTest(template = "industrial") public static void theFoundryMakesRoughMetalAndKeepsItHot(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(1, KilnContent.MINERAL_COAL.toStack(16));
        f.setItem(0, new ItemStack(Items.RAW_IRON, 3));
        FoundryGameTests.run(h, f, 3 * 200 + 40);
        long now = h.getLevel().getGameTime();
        var blooms = f.getItem(2);
        h.assertTrue(blooms.is(KilnContent.IRON_BLOOM.get()) && blooms.getCount() == 3 && HotMetal.workable(blooms, now) && Grade.of(blooms).units().size() > 1,
                "Iron ore smelts into hot blooms that share one stack, each rolling its own grade");
        h.assertTrue(!f.canTakeItemThroughFace(2, blooms, Direction.DOWN), "Hoppers cannot take hot metal");
        long later = now + 10 * HotMetal.COOL_TICKS;
        HotMetal.tend(blooms, true, later);
        h.assertTrue(HotMetal.age(blooms, later) < 20, "Metal in a hot furnace stays at full heat however long it waits");
        var grades = GradeGameTests.grades(blooms);
        HotMetal.tend(blooms, false, later + HotMetal.COOL_TICKS - 20);
        h.assertTrue(HotMetal.hot(blooms, later + HotMetal.COOL_TICKS - 20) && GradeGameTests.grades(blooms).equals(grades), "Out of the fire it is still hot just under a minute later");
        HotMetal.tend(blooms, false, later + HotMetal.COOL_TICKS);
        h.assertTrue(!blooms.has(HotMetal.HOT.get()) && Grade.stacks(blooms, new ItemStack(KilnContent.IRON_BLOOM.get())), "Out of the fire it cools a minute later and stacks with cold metal");
        h.assertTrue(GradeGameTests.grades(blooms).equals(less(grades, 100)), "Cooling in air costs every unit the full ten points: " + grades + " to " + GradeGameTests.grades(blooms));
        h.succeed();
    }

    @GameTest(template = "industrial") public static void aHeatTakesTheWholeLoad(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(1, KilnContent.MINERAL_COAL.toStack(16));
        GradeGameTests.select(f, Items.IRON_INGOT);
        f.setItem(0, GradeGameTests.ingots(1100, 1000, 950, 900, 850, 800, 750, 700, 650, 600));
        FoundryGameTests.run(h, f, 400 + 40);
        long now = h.getLevel().getGameTime();
        var load = f.getItem(2);
        h.assertTrue(load.is(Items.IRON_INGOT) && HotMetal.workable(load, now) && GradeGameTests.grades(load).equals(List.of(1100, 1000, 950, 900, 850, 800, 750, 700)),
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
        double blow = 3 * CalorieConfig.BLOW.get() * ThermalRules.calorieFactor(ThermalSystem.comfort(player));
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
        h.assertTrue(HotMetal.hot(bars, now) && !HotMetal.workable(bars, now), "A hammering spends the working heat and leaves the bars to cool");
        h.assertTrue(spent >= .8 * HotMetal.ANVIL_BLOWS * blow - 1e-6 && spent <= HotMetal.ANVIL_BLOWS * blow + 1e-6, "Every blow is paid labor for each unit: " + spent + " kcal");
        h.assertTrue(click(player, anvil).isCanceled() && GradeGameTests.grades(player.getMainHandItem()).equals(grades), "Metal below working heat can't be hammered again");
        var cauldron = station(h, 4, Blocks.WATER_CAULDRON);
        int loss = HotMetal.loss(HotMetal.age(player.getMainHandItem(), now));
        h.assertTrue(click(player, cauldron).isCanceled() && !player.getMainHandItem().has(HotMetal.HOT.get()) && Grade.stacks(player.getMainHandItem(), new ItemStack(Items.IRON_INGOT)),
                "Water quenches the bars at once");
        h.assertTrue(loss > 0 && GradeGameTests.grades(player.getMainHandItem()).equals(less(grades, loss)),
                "Quenching straight off the anvil, before cherry red, costs every unit the same: " + grades + " to " + GradeGameTests.grades(player.getMainHandItem()));
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

    @GameTest(template = "industrial") public static void aSmithWhoCannotPayStrikesNothing(GameTestHelper h) {
        var anvil = station(h, 6, Blocks.ANVIL);
        var player = smith(h, anvil);
        CalorieFoodData.of(player).reserve().set(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, HotMetal.heated(GradeGameTests.units(KilnContent.IRON_BLOOM.get(), 900, 800), h.getLevel().getGameTime()));
        for (int i = 0; i < HotMetal.ANVIL_BLOWS; i++) {
            h.assertTrue(click(player, anvil).isCanceled(), "The anvil takes the click, not its own screen");
            player.getCooldowns().removeCooldown(KilnContent.IRON_BLOOM.get());
        }
        h.assertTrue(player.getMainHandItem().is(KilnContent.IRON_BLOOM.get()) && GradeGameTests.grades(player.getMainHandItem()).equals(List.of(900, 800)),
                "A smith with no food left hammers nothing out of the metal");
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

    @GameTest(template = "industrial") public static void quenchingCostsGradeByDistanceFromCherryRed(GameTestHelper h) {
        var cauldron = station(h, 2, Blocks.WATER_CAULDRON);
        var player = smith(h, cauldron);
        long now = h.getLevel().getGameTime();
        var grades = List.of(900, 800, 700);
        for (int seconds : new int[] {0, -2, 2, -4, 4, -10, 10}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, aged(GradeGameTests.units(Items.IRON_INGOT, 900, 800, 700), now, seconds));
            h.assertTrue(click(player, cauldron).isCanceled() && !player.getMainHandItem().has(HotMetal.HOT.get()), "Water quenches the bars");
            var after = GradeGameTests.grades(player.getMainHandItem());
            int lost = grades.getFirst() - after.getFirst();
            h.assertTrue(after.equals(less(grades, lost)), "Every unit loses the same: " + after);
            h.assertTrue(Math.abs(seconds) <= 2 ? lost == 0 : Math.abs(seconds) == 4 ? lost > 0 && lost < 10 : lost > 70,
                    "Quenching " + seconds + " s from cherry red costs " + lost / 10.0 + " points");
        }
        h.assertTrue(HotMetal.loss(HotMetal.COOL_TICKS) == 100, "Metal cooled in air has lost the full ten points");
        player.setItemInHand(InteractionHand.MAIN_HAND, GradeGameTests.units(Items.IRON_INGOT, 900, 800));
        h.assertTrue(!click(player, cauldron).isCanceled() && GradeGameTests.grades(player.getMainHandItem()).equals(List.of(900, 800)), "Cold metal takes nothing from water");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void aFoundrySettlesMetalThatCooledBeforeItCameIn(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        var ingots = GradeGameTests.units(Items.IRON_INGOT, 900, 800);
        ingots.set(HotMetal.HOT.get(), h.getLevel().getGameTime() - 1);
        f.setItem(0, ingots);
        h.succeedWhen(() -> h.assertTrue(!f.getItem(0).has(HotMetal.HOT.get()) && GradeGameTests.grades(f.getItem(0)).equals(List.of(800, 700)),
                "Metal that cooled in air before reaching the Foundry takes its loss before any reheat"));
    }

    @GameTest(template = "industrial") public static void metalCooledInAirLosesTheFullTenOnce(GameTestHelper h) {
        var level = h.getLevel();
        long now = level.getGameTime();
        var held = GradeGameTests.units(Items.IRON_INGOT, 900, 800);
        held.set(HotMetal.HOT.get(), now - 1);
        HotMetal.cool(held, now);
        h.assertTrue(!held.has(HotMetal.HOT.get()) && GradeGameTests.grades(held).equals(List.of(800, 700)), "Metal gone cold in air loses ten points from every unit");
        HotMetal.cool(held, now + HotMetal.COOL_TICKS);
        HotMetal.heated(held, now);
        HotMetal.quench(aged(held, now, 0), now);
        h.assertTrue(GradeGameTests.grades(held).equals(List.of(800, 700)), "It loses them once, and a good reheat and quench does not restore them");
        var smithy = WorkshopGameTests.build(h, 2, Direction.NORTH);
        var plate = ManufacturingGameTests.job(2, ManufacturingContent.part("iron", "plate"));
        ManufacturingGameTests.supply(smithy, plate);
        smithy.setItem(4, KilnContent.MINERAL_COAL.toStack(4));
        int slot = 0;
        while (!smithy.getItem(slot).is(Items.IRON_INGOT)) slot++;
        int count = smithy.getItem(slot).getCount();
        var ingots = new ItemStack(Items.IRON_INGOT, count);
        Grade.assign(ingots, 900);
        smithy.setItem(slot, HotMetal.heated(ingots, now));
        WorkshopGameTests.run(smithy, 10);
        h.assertTrue(smithy.status == MachineStatus.Workshop.HOT_METAL && smithy.getItem(slot).getCount() == count && smithy.getItem(5).isEmpty(),
                "A Smithy works no hot metal");
        smithy.getItem(slot).set(HotMetal.HOT.get(), now - 1);
        WorkshopGameTests.run(smithy, plate.ticks());
        h.assertTrue(smithy.getItem(5).getCount() == plate.output().getCount() && Grade.value(smithy.getItem(5)) == 800,
                "Metal that cooled in the Smithy takes its loss before it is worked: " + Grade.value(smithy.getItem(5)));
        var floor = h.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(floor.below(), Blocks.STONE.defaultBlockState());
        var dropped = GradeGameTests.units(Items.COPPER_INGOT, 900);
        dropped.set(HotMetal.HOT.get(), now + 20);
        var item = new ItemEntity(level, floor.getX() + .5, floor.getY() + .1, floor.getZ() + .5, dropped);
        item.setDeltaMovement(Vec3.ZERO);
        item.setNeverPickUp();
        level.addFreshEntity(item);
        h.succeedWhen(() -> h.assertTrue(!item.getItem().has(HotMetal.HOT.get()) && GradeGameTests.grades(item.getItem()).equals(List.of(800)),
                "Metal left on the ground settles once cold, ten points down"));
    }
}
