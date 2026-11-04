package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class GradeGameTests {
    /** One stack holding units of these grades. */
    static ItemStack units(Item item, int... grades) {
        var stack = new ItemStack(item);
        Grade.assign(stack, grades[0]);
        for (int i = 1; i < grades.length; i++) {
            var unit = new ItemStack(item);
            Grade.assign(unit, grades[i]);
            Grade.join(stack, unit, 1);
        }
        return stack;
    }
    static ItemStack ingots(int... grades) { return units(Items.IRON_INGOT, grades); }
    static List<Integer> grades(ItemStack stack) { return Grade.top(stack, stack.getCount()); }
    static List<Integer> sorted(List<Integer> grades) { var list = new ArrayList<>(grades); list.sort(java.util.Comparator.reverseOrder()); return list; }

    @GameTest(template = "industrial") public static void unitsLeaveFromTheTopAndNeverMultiply(GameTestHelper h) {
        var stack = ingots(60, 100, 80, 90);
        h.assertTrue(grades(stack).equals(List.of(100, 90, 80, 60)) && Grade.value(stack) == 83, "A stack keeps its units best first and shows their average");
        var part = stack.split(2);
        h.assertTrue(grades(part).equals(List.of(100, 90)) && grades(stack).equals(List.of(80, 60)), "A split hands over the top units and keeps the rest");
        h.assertTrue(grades(stack.copyWithCount(1)).equals(List.of(60)), "A copy that names no units keeps the worst one");
        stack.grow(2);
        h.assertTrue(grades(stack).equals(List.of(80, 60, 60, 60)), "Growth that names no units repeats the worst");
        var even = ingots(90, 90, 90);
        h.assertTrue(Grade.of(even).units().equals(List.of(90)) && ItemStack.isSameItemSameComponents(even, ingots(90)), "Equal grades collapse to one value and stack natively at any count");
        h.assertTrue(!ingots(75, 75).has(Grade.GRADE.get()) && ItemStack.isSameItemSameComponents(ingots(75, 75), new ItemStack(Items.IRON_INGOT)), "Standard units carry no grade, so they stack with plain ingots");
        var named = ingots(100);named.set(DataComponents.CUSTOM_NAME, Component.literal("Kept"));
        h.assertTrue(Grade.stacks(ingots(100), ingots(40, 50)) && !Grade.stacks(ingots(100), units(Items.GOLD_INGOT, 100)) && !Grade.stacks(named, ingots(40)), "Only the same material differing in grade alone combines");
        var odd = new ItemStack(Items.IRON_INGOT, 5); odd.set(Grade.GRADE.get(), new Grade(List.of(90, 80)));
        h.assertTrue(grades(odd.split(3)).equals(List.of(90, 80, 80)) && grades(odd).equals(List.of(80, 80)), "A recorded list that disagrees with its count reads as its worst units");
        var registries = h.getLevel().registryAccess();
        var mixed = ingots(120, 70, 55);
        h.assertTrue(grades(ItemStack.parse(registries, mixed.save(registries)).orElseThrow()).equals(List.of(120, 70, 55)), "Saved stacks keep every unit");
        var single = ((CompoundTag)ingots(90).save(registries)).getCompound("components").get(Grade.GRADE.getId().toString());
        h.assertTrue(single instanceof IntTag tag && tag.getAsInt() == 90, "A single grade saves as a bare number, as equipment always has");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void freshRollsAndRandomRefinement(GameTestHelper h) {
        var random = net.minecraft.util.RandomSource.create(987654321L);
        int low = 0, high = 0; long total = 0;
        for (int i = 0; i < 20000; i++) {
            int grade = Grade.roll(random); total += grade;
            if (grade < 50) low++;
            if (grade >= 100) high++;
        }
        h.assertTrue(total / 20000.0 > 74 && total / 20000.0 < 76, "Fresh units centre on 75%");
        h.assertTrue(low > 1600 && low < 2800 && high > 1600 && high < 2800, "Both low (<50%) and exceptional (100%+) units appear on roughly one in ten rolls");
        int lowest = Integer.MAX_VALUE, single = 0, large = 0; long gained = 0, gainedHigh = 0, passes = 0;
        for (int i = 0; i < 20000; i++) {
            int gain = Grade.refined(75, random) - 75;
            lowest = Math.min(lowest, gain); gained += gain;
            if (gain == 1) single++;
            if (gain >= 10) large++;
            gainedHigh += Grade.refined(125, random) - 125;
        }
        for (int i = 0; i < 2000; i++) for (int grade = 75; grade < 100; grade = Grade.refined(grade, random)) passes++;
        h.assertTrue(lowest >= 1 && gained / 20000.0 > 4.3 && gained / 20000.0 < 4.9, "Every pass raises a unit, about four and a half points on average from 75%");
        h.assertTrue(single > 5000 && single < 6600 && large > 2000 && large < 2900, "Passes vary widely: about three in ten gain a single point, one in eight gains 10 or more");
        h.assertTrue(gainedHigh / 20000.0 < 1.05 && passes / 2000.0 > 9.5 && passes / 2000.0 < 11.5, "Returns fall steeply: 75% reaches 100% in about ten passes, and at 125% a pass adds one point");
        h.assertTrue(Grade.refined(149, random) == 150 && Grade.refined(150, random) == 151 && Grade.refined(170, random) == 171, "A pass never jumps past 150%, and beyond it still adds a point");
        h.succeed();
    }

    private static void select(FoundryBlockEntity f, String path) {
        var recipes = f.recipes();
        for (int i = 0; i < recipes.size(); i++) if (recipes.get(i).id().getPath().equals(path)) { f.selectRecipe(i); return; }
        throw new IllegalStateException("No Foundry recipe " + path);
    }
    @GameTest(template = "industrial") public static void theFoundryGradesEachUnitItMakes(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(0, new ItemStack(Items.RAW_IRON, 8)); f.setItem(1, KilnContent.MINERAL_COAL.toStack(16)); FoundryGameTests.run(h, f, 8 * 200 + 40);
        h.assertTrue(f.getItem(2).is(Items.IRON_INGOT) && f.getItem(2).getCount() == 8 && Grade.of(f.getItem(2)).units().size() > 1, "Each smelted ingot rolls its own grade and the ingots share one stack");
        f.setItem(2, ItemStack.EMPTY); f.setItem(1, KilnContent.MINERAL_COAL.toStack(16)); f.setItem(0, ingots(100, 90, 70)); FoundryGameTests.run(h, f, 3 * 800 + 40);
        h.assertTrue(f.getItem(2).is(KilnContent.STEEL.get()) && grades(f.getItem(2)).equals(List.of(100, 90, 70)), "On Automatic, iron still becomes steel, each ingot keeping its grade");
        f.setItem(2, ItemStack.EMPTY); select(f, "refine_iron_ingot"); f.setItem(0, ingots(96, 40)); FoundryGameTests.run(h, f, 2 * 200 + 40);
        var refined = grades(f.getItem(2));
        h.assertTrue(refined.size() == 2 && refined.getFirst() > 96 && refined.getLast() > 40, "Refining raises every unit it passes: " + refined);
        f.setItem(2, ItemStack.EMPTY); select(f, "refine_iron_ingot"); f.setItem(0, new ItemStack(Items.IRON_INGOT)); FoundryGameTests.run(h, f, 240);
        h.assertTrue(Grade.value(f.getItem(2)) > Grade.STANDARD, "An ingot with no recorded grade refines up from the standard 75%");
        f.setItem(2, ItemStack.EMPTY); f.selectRecipe(-1); f.setItem(0, new ItemStack(Items.DIAMOND)); FoundryGameTests.run(h, f, 240);
        h.assertTrue(f.getItem(2).is(Items.DIAMOND) && Grade.value(f.getItem(2)) > Grade.STANDARD, "Diamonds, which have no other Foundry job, refine on Automatic");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void workshopsTakeTheMeanOfTheUnitsTheyUse(GameTestHelper h) {
        var smithy = WorkshopGameTests.build(h, 2, Direction.NORTH);
        var head = ManufacturingGameTests.job(2, ManufacturingContent.head("iron", "pickaxe"));
        smithy.select(WorkshopJobs.index(2, head.id())); smithy.setItem(0, ingots(110, 100, 90, 40)); smithy.setItem(4, KilnContent.MINERAL_COAL.toStack(8)); WorkshopGameTests.run(smithy, head.ticks());
        h.assertTrue(Grade.value(smithy.getItem(5)) == 100 && grades(smithy.getItem(0)).equals(List.of(40)), "A head takes the mean of the top three ingots and leaves the worst");
        smithy.clearContent();
        var fasteners = ManufacturingGameTests.job(2, ManufacturingContent.FASTENERS.get());
        smithy.select(WorkshopJobs.index(2, fasteners.id())); smithy.setItem(0, ingots(120)); smithy.setItem(4, KilnContent.MINERAL_COAL.toStack(8)); WorkshopGameTests.run(smithy, fasteners.ticks());
        h.assertTrue(smithy.getItem(5).getCount() == 8 && grades(smithy.getItem(5)).equals(java.util.Collections.nCopies(8, 120)), "Every part from one ingot carries its grade");
        var bench = WorkshopGameTests.build(h, 3, Direction.NORTH);
        var pick = ManufacturingGameTests.job(3, Items.IRON_PICKAXE);
        ManufacturingGameTests.supply(bench, pick); bench.setItem(0, units(ManufacturingContent.head("iron", "pickaxe"), 100)); WorkshopGameTests.run(bench, pick.ticks());
        var tool = bench.getItem(5);
        h.assertTrue(tool.is(Items.IRON_PICKAXE) && Grade.value(tool) == 100 && tool.getMaxDamage() == 250 && tool.has(EquipmentGrade.WEAR_SEED.get()), "A tool takes its head's grade and its own wear pattern");
        var helmet = ManufacturingGameTests.job(3, Items.IRON_HELMET);
        ManufacturingGameTests.supply(bench, helmet); bench.setItem(0, units(ManufacturingContent.part("iron", "plate"), 100, 100, 90, 80, 80)); WorkshopGameTests.run(bench, helmet.ticks());
        h.assertTrue(Grade.value(bench.getItem(5)) == 90, "Armor takes the mean of its plates");
        var backpack = ManufacturingGameTests.job(3, Backpack.ITEM.get());
        ManufacturingGameTests.supply(bench, backpack); WorkshopGameTests.run(bench, backpack.ticks());
        h.assertTrue(bench.getItem(5).is(Backpack.ITEM.get()) && EquipmentGrade.hasGrade(bench.getItem(5)), "A backpack rolls its own grade when made");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void menusMoveTheActualUnits(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL); var menu = p.inventoryMenu; var inventory = p.getInventory(); p.containerMenu = menu;
        menu.setCarried(ingots(100, 90, 80));
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, p);
        for (int slot = 9; slot < 12; slot++) menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, p);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, p);
        var spread = new ArrayList<Integer>();for (int i = 9; i < 12; i++) spread.addAll(grades(inventory.getItem(i)));
        h.assertTrue(menu.getCarried().isEmpty() && sorted(spread).equals(List.of(100, 90, 80)), "Spreading a stack gives each slot one of its own units");
        inventory.clearContent(); inventory.setItem(9, ingots(100, 90)); menu.setCarried(ingots(70));
        menu.clicked(9, 0, ClickType.PICKUP, p);
        h.assertTrue(menu.getCarried().isEmpty() && grades(inventory.getItem(9)).equals(List.of(100, 90, 70)), "Clicking joins differing grades onto one stack");
        menu.setCarried(ingots(60, 50)); menu.clicked(9, 1, ClickType.PICKUP, p);
        h.assertTrue(grades(inventory.getItem(9)).equals(List.of(100, 90, 70, 60)) && grades(menu.getCarried()).equals(List.of(50)), "A right click places the carried stack's top unit");
        menu.setCarried(ItemStack.EMPTY); menu.clicked(9, 1, ClickType.PICKUP, p);
        h.assertTrue(grades(menu.getCarried()).equals(List.of(100, 90)) && grades(inventory.getItem(9)).equals(List.of(70, 60)), "Taking half takes the top half");
        inventory.setItem(10, ingots(80)); inventory.setItem(11, ingots(85, 65)); menu.clicked(12, 0, ClickType.PICKUP_ALL, p);
        h.assertTrue(grades(menu.getCarried()).equals(List.of(100, 90, 85, 80, 70, 65, 60)) && inventory.getItem(9).isEmpty() && inventory.getItem(11).isEmpty(), "A double click collects every unit of differing stacks");
        menu.setCarried(ItemStack.EMPTY); inventory.clearContent(); inventory.setItem(9, ingots(100)); inventory.setItem(0, ingots(50, 40));
        menu.clicked(36, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(inventory.getItem(0).isEmpty() && grades(inventory.getItem(9)).equals(List.of(100, 50, 40)), "Shift-clicking joins a differing stack");
        h.assertTrue(inventory.add(ingots(95)) && grades(inventory.getItem(9)).equals(List.of(100, 95, 50, 40)), "A pickup joins the stack already carried");
        h.succeed();
    }

    private static HopperBlockEntity hopper(GameTestHelper h, BlockPos relative, ItemStack... contents) {
        var pos = h.absolutePos(relative); var state = Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN);
        h.getLevel().removeBlock(pos, false); h.getLevel().setBlockAndUpdate(pos, state); var hopper = (HopperBlockEntity)h.getLevel().getBlockEntity(pos);
        for (int i = 0; i < contents.length; i++) hopper.setItem(i, contents[i]);
        return hopper;
    }
    private static void tick(GameTestHelper h, HopperBlockEntity hopper) { HopperBlockEntity.pushItemsTick(h.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper); }
    @GameTest(template = "industrial") public static void hoppersMoveTheActualUnits(GameTestHelper h) {
        var chestPos = h.absolutePos(new BlockPos(1, 1, 9)); h.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity)h.getLevel().getBlockEntity(chestPos); chest.setItem(0, ingots(100, 90));
        tick(h, hopper(h, new BlockPos(1, 2, 9), ingots(70)));
        h.assertTrue(grades(chest.getItem(0)).equals(List.of(100, 90, 70)) && chest.getItem(1).isEmpty(), "A hopper joins a unit onto a chest's differing stack");
        var smithyPos = h.absolutePos(new BlockPos(9, 2, 9)); h.getLevel().setBlockAndUpdate(smithyPos, WorkshopContent.SMITHY.get().defaultBlockState());
        var smithy = (WorkshopBlockEntity)h.getLevel().getBlockEntity(smithyPos); smithy.setItem(0, ingots(100, 90));
        tick(h, hopper(h, new BlockPos(9, 3, 9), ingots(70)));
        h.assertTrue(grades(smithy.getItem(0)).equals(List.of(100, 90, 70)), "A hopper joins a unit onto a machine's differing stack");
        for (int i = 0; i < 4; i++) smithy.setItem(i, new ItemStack(Items.GOLD_INGOT));
        var blocked = hopper(h, new BlockPos(9, 3, 9), ingots(100, 90, 80)); tick(h, blocked);
        h.assertTrue(grades(blocked.getItem(0)).equals(List.of(100, 90, 80)), "A unit the machine refuses goes back as itself");
        smithy.setItem(5, ingots(100, 90, 80));
        var full = hopper(h, new BlockPos(9, 1, 9), new ItemStack(Items.DIRT), new ItemStack(Items.STONE), new ItemStack(Items.SAND), new ItemStack(Items.GRAVEL), new ItemStack(Items.COBBLESTONE));
        tick(h, full);
        h.assertTrue(grades(smithy.getItem(5)).equals(List.of(100, 90, 80)), "A unit a hopper cannot take stays in the machine as itself");
        h.succeed();
    }
}
