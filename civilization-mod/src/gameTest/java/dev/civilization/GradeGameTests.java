package dev.civilization;

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

    @GameTest(template = "industrial") public static void unitsLeaveFromTheTopAndNeverMultiply(GameTestHelper h) {
        var stack = ingots(600, 1000, 800, 900);
        h.assertTrue(grades(stack).equals(List.of(1000, 900, 800, 600)) && Grade.value(stack) == 825, "A stack keeps its units best first and shows their average");
        var part = stack.split(2);
        h.assertTrue(grades(part).equals(List.of(1000, 900)) && grades(stack).equals(List.of(800, 600)), "A split hands over the top units and keeps the rest");
        h.assertTrue(grades(stack.copyWithCount(1)).equals(List.of(600)), "A copy that names no units keeps the worst one");
        stack.grow(2);
        h.assertTrue(grades(stack).equals(List.of(800, 600, 600, 600)), "Growth that names no units repeats the worst");
        var even = ingots(900, 900, 900);
        h.assertTrue(Grade.of(even).units().equals(List.of(900)) && ItemStack.isSameItemSameComponents(even, ingots(900)), "Equal grades collapse to one value and stack natively at any count");
        h.assertTrue(!ingots(750, 750).has(Grade.GRADE.get()) && ItemStack.isSameItemSameComponents(ingots(750, 750), new ItemStack(Items.IRON_INGOT)), "Standard units carry no grade, so they stack with plain ingots");
        var named = ingots(1000);named.set(DataComponents.CUSTOM_NAME, Component.literal("Kept"));
        h.assertTrue(Grade.stacks(ingots(1000), ingots(400, 500)) && !Grade.stacks(ingots(1000), units(Items.GOLD_INGOT, 1000)) && !Grade.stacks(named, ingots(400)), "Only the same material differing in grade alone combines");
        var odd = new ItemStack(Items.IRON_INGOT, 5); odd.set(Grade.GRADE.get(), new Grade(List.of(900, 800)));
        h.assertTrue(grades(odd.split(3)).equals(List.of(900, 800, 800)) && grades(odd).equals(List.of(800, 800)), "A recorded list that disagrees with its count reads as its worst units");
        var registries = h.getLevel().registryAccess();
        var mixed = ingots(1200, 700, 550);
        h.assertTrue(grades(ItemStack.parse(registries, mixed.save(registries)).orElseThrow()).equals(List.of(1200, 700, 550)), "Saved stacks keep every unit");
        var single = ((CompoundTag)ingots(900).save(registries)).getCompound("components").get(Grade.GRADE.getId().toString());
        h.assertTrue(single instanceof net.minecraft.nbt.NumericTag number && number.getAsDouble() == 90, "A single grade saves as a bare percentage");
        var older = (CompoundTag)ingots(900).save(registries); older.getCompound("components").put(Grade.GRADE.getId().toString(), IntTag.valueOf(75));
        h.assertTrue(Grade.value(ItemStack.parse(registries, older).orElseThrow()) == 750, "A whole-percent grade saved by an older build reads unchanged");
        h.assertTrue(grades(ItemStack.parse(registries, ingots(834, 521).save(registries)).orElseThrow()).equals(List.of(834, 521)) && Grade.percent(834).equals("83.4%") && Grade.percent(750).equals("75%"), "Tenths survive saving and display as one decimal");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void noCraftingRecipeMakesGradedMaterial(GameTestHelper h) {
        // Graded metal and diamonds come only from the Foundry, the Smithy and their tools, so storage blocks and nuggets are one-way.
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING))
            h.assertTrue(!Grade.graded(holder.value().getResultItem(h.getLevel().registryAccess())), "No crafting recipe makes graded material: " + holder.id());
        h.succeed();
    }
    @GameTest(template = "industrial") public static void freshRollsAndRandomRefinement(GameTestHelper h) {
        var random = net.minecraft.util.RandomSource.create(987654321L);
        int low = 0, high = 0; long total = 0, squares = 0;
        for (int i = 0; i < 20000; i++) {
            int grade = Grade.roll(random); total += grade; squares += (long)grade * grade;
            if (grade < 500) low++;
            if (grade >= 1000) high++;
        }
        double mean = total / 20000.0, deviation = Math.sqrt(squares / 20000.0 - mean * mean);
        h.assertTrue(mean > 745 && mean < 755, "Fresh units centre on 75%");
        h.assertTrue(deviation > 97 && deviation < 103, "Fresh units spread with a 10-point standard deviation, measured " + deviation / 10 + " points");
        h.assertTrue(low > 70 && low < 190 && high > 70 && high < 190, "Both low (<50%) and exceptional (100%+) units appear on roughly one in 160 rolls");
        int lowest = Integer.MAX_VALUE, small = 0, large = 0; long gained = 0, gainedHigh = 0, passes = 0, slowPasses = 0;
        for (int i = 0; i < 20000; i++) {
            int gain = Grade.refined(750, random) - 750;
            lowest = Math.min(lowest, gain); gained += gain;
            if (gain <= 10) small++;
            if (gain >= 100) large++;
            gainedHigh += Grade.refined(1100, random) - 1100;
        }
        for (int i = 0; i < 1000; i++) for (int grade = 750; grade < 1000; grade = Grade.refined(grade, random)) passes++;
        for (int i = 0; i < 200; i++) for (int grade = 1000; grade < 1250; grade = Grade.refined(grade, random)) slowPasses++;
        h.assertTrue(lowest >= 1 && gained / 20000.0 > 42 && gained / 20000.0 < 48, "Every pass raises a unit, about four and a half points on average from 75%");
        h.assertTrue(small > 3700 && small < 4600 && large > 1900 && large < 2500, "Passes vary widely: about one in five gains a point or less, one in nine gains 10 or more");
        h.assertTrue(gainedHigh / 20000.0 > 3 && gainedHigh / 20000.0 < 4.5, "At 110% a pass gains under half a point on average, still at random");
        h.assertTrue(passes / 1000.0 > 10.5 && passes / 1000.0 < 13.5 && slowPasses / 200.0 > 90 && slowPasses / 200.0 < 112, "75% reaches 100% in about 12 passes; 100% to 125% takes about a hundred");
        h.assertTrue(Grade.refined(1499, random) == 1500 && Grade.refined(1500, random) == 1501 && Grade.refined(1700, random) == 1701, "A pass never jumps past 150%, and beyond it adds a tenth of a point");
        h.succeed();
    }

    static void select(KilnBlockEntity k, Item output) {
        int index = k.outputs().indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output));
        if (index < 0) throw new IllegalStateException("No recipe makes " + output);
        k.select(index);
    }
    @GameTest(template = "industrial") public static void theFoundryGradesEachUnitItMakes(GameTestHelper h) {
        var f = FoundryGameTests.build(h, Direction.NORTH);
        f.setItem(0, new ItemStack(Items.RAW_IRON, 8)); f.setItem(1, KilnContent.MINERAL_COAL.toStack(16)); FoundryGameTests.run(h, f, 8 * 200 + 40);
        h.assertTrue(f.getItem(2).is(KilnContent.IRON_BLOOM.get()) && f.getItem(2).getCount() == 8 && Grade.of(f.getItem(2)).units().size() > 1, "Each smelted bloom rolls its own grade and the blooms share one stack");
        f.setItem(2, ItemStack.EMPTY); f.setItem(1, KilnContent.MINERAL_COAL.toStack(16)); f.setItem(0, ingots(1000, 900, 700)); FoundryGameTests.run(h, f, 3 * 800 + 40);
        h.assertTrue(f.getItem(2).is(KilnContent.BLISTER_STEEL.get()) && grades(f.getItem(2)).equals(List.of(1000, 900, 700)), "On Automatic, iron still becomes blister steel, each ingot keeping its grade");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void workshopsTakeTheMeanOfTheUnitsTheyUse(GameTestHelper h) {
        var smithy = WorkshopGameTests.build(h, 2, Direction.NORTH);
        var head = ManufacturingGameTests.job(2, ManufacturingContent.head("iron", "pickaxe"));
        smithy.select(WorkshopJobs.index(2, head.id())); smithy.setItem(0, ingots(1100, 1000, 900, 400)); smithy.setItem(4, KilnContent.MINERAL_COAL.toStack(8)); WorkshopGameTests.run(smithy, head.ticks());
        h.assertTrue(Grade.value(smithy.getItem(5)) == 1000 && grades(smithy.getItem(0)).equals(List.of(400)), "A head takes the mean of the top three ingots and leaves the worst");
        smithy.clearContent();
        var fasteners = ManufacturingGameTests.job(2, ManufacturingContent.FASTENERS.get());
        smithy.select(WorkshopJobs.index(2, fasteners.id())); smithy.setItem(0, ingots(1200)); smithy.setItem(4, KilnContent.MINERAL_COAL.toStack(8)); WorkshopGameTests.run(smithy, fasteners.ticks());
        h.assertTrue(smithy.getItem(5).getCount() == 8 && grades(smithy.getItem(5)).equals(java.util.Collections.nCopies(8, 1200)), "Every part from one ingot carries its grade");
        var bench = WorkshopGameTests.build(h, 3, Direction.NORTH);
        var pick = ManufacturingGameTests.job(3, Items.IRON_PICKAXE);
        ManufacturingGameTests.supply(bench, pick); bench.setItem(0, units(ManufacturingContent.head("iron", "pickaxe"), 1000)); WorkshopGameTests.run(bench, pick.ticks());
        var tool = bench.getItem(5);
        h.assertTrue(tool.is(Items.IRON_PICKAXE) && Grade.value(tool) == 1000 && tool.getMaxDamage() == 250 && tool.has(EquipmentGrade.WEAR_SEED.get()), "A tool takes its head's grade and its own wear pattern");
        var helmet = ManufacturingGameTests.job(3, Items.IRON_HELMET);
        ManufacturingGameTests.supply(bench, helmet); bench.setItem(0, units(ManufacturingContent.part("iron", "plate"), 1000, 1000, 900, 800, 800)); WorkshopGameTests.run(bench, helmet.ticks());
        h.assertTrue(Grade.value(bench.getItem(5)) == 900, "Armor takes the mean of its plates");
        var backpack = ManufacturingGameTests.job(3, Backpack.ITEM.get());
        ManufacturingGameTests.supply(bench, backpack); WorkshopGameTests.run(bench, backpack.ticks());
        h.assertTrue(bench.getItem(5).is(Backpack.ITEM.get()) && EquipmentGrade.hasGrade(bench.getItem(5)), "A backpack rolls its own grade when made");
        h.succeed();
    }

    @GameTest(template = "industrial") public static void menusMoveTheActualUnits(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL); var menu = p.inventoryMenu; var inventory = p.getInventory(); p.containerMenu = menu;
        menu.setCarried(ingots(1000, 900, 800));
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 1), ClickType.QUICK_CRAFT, p);
        for (int slot : new int[]{20, 9, 31, 14}) menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 1), ClickType.QUICK_CRAFT, p);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 1), ClickType.QUICK_CRAFT, p);
        h.assertTrue(menu.getCarried().isEmpty() && grades(inventory.getItem(20)).equals(List.of(1000)) && grades(inventory.getItem(9)).equals(List.of(900))
                && grades(inventory.getItem(31)).equals(List.of(800)) && inventory.getItem(14).isEmpty(), "A right-drag deals the best units into the slots in the order dragged");
        inventory.clearContent(); inventory.setItem(9, ingots(1000, 900)); menu.setCarried(ingots(700));
        menu.clicked(9, 0, ClickType.PICKUP, p);
        h.assertTrue(menu.getCarried().isEmpty() && grades(inventory.getItem(9)).equals(List.of(1000, 900, 700)), "Clicking joins differing grades onto one stack");
        menu.setCarried(ingots(600, 500)); menu.clicked(9, 1, ClickType.PICKUP, p);
        h.assertTrue(grades(inventory.getItem(9)).equals(List.of(1000, 900, 700, 600)) && grades(menu.getCarried()).equals(List.of(500)), "A right click places the carried stack's top unit");
        menu.setCarried(ItemStack.EMPTY); menu.clicked(9, 1, ClickType.PICKUP, p);
        h.assertTrue(grades(menu.getCarried()).equals(List.of(1000, 900)) && grades(inventory.getItem(9)).equals(List.of(700, 600)), "Taking half takes the top half");
        inventory.setItem(10, ingots(800)); inventory.setItem(11, ingots(850, 650)); menu.clicked(12, 0, ClickType.PICKUP_ALL, p);
        h.assertTrue(grades(menu.getCarried()).equals(List.of(1000, 900, 850, 800, 700, 650, 600)) && inventory.getItem(9).isEmpty() && inventory.getItem(11).isEmpty(), "A double click collects every unit of differing stacks");
        menu.setCarried(ItemStack.EMPTY); inventory.clearContent(); inventory.setItem(9, ingots(1000)); inventory.setItem(0, ingots(500, 400));
        menu.clicked(36, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(inventory.getItem(0).isEmpty() && grades(inventory.getItem(9)).equals(List.of(1000, 500, 400)), "Shift-clicking joins a differing stack");
        h.assertTrue(inventory.add(ingots(950)) && grades(inventory.getItem(9)).equals(List.of(1000, 950, 500, 400)), "A pickup joins the stack already carried");
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
        var chest = (ChestBlockEntity)h.getLevel().getBlockEntity(chestPos); chest.setItem(0, ingots(1000, 900));
        tick(h, hopper(h, new BlockPos(1, 2, 9), ingots(700)));
        h.assertTrue(grades(chest.getItem(0)).equals(List.of(1000, 900, 700)) && chest.getItem(1).isEmpty(), "A hopper joins a unit onto a chest's differing stack");
        var smithyPos = h.absolutePos(new BlockPos(9, 2, 9)); h.getLevel().setBlockAndUpdate(smithyPos, WorkshopContent.SMITHY.get().defaultBlockState());
        var smithy = (WorkshopBlockEntity)h.getLevel().getBlockEntity(smithyPos); smithy.setItem(0, ingots(1000, 900));
        tick(h, hopper(h, new BlockPos(9, 3, 9), ingots(700)));
        h.assertTrue(grades(smithy.getItem(0)).equals(List.of(1000, 900, 700)), "A hopper joins a unit onto a machine's differing stack");
        for (int i = 0; i < 4; i++) smithy.setItem(i, new ItemStack(Items.GOLD_INGOT));
        var blocked = hopper(h, new BlockPos(9, 3, 9), ingots(1000, 900, 800)); tick(h, blocked);
        h.assertTrue(grades(blocked.getItem(0)).equals(List.of(1000, 900, 800)), "A unit the machine refuses goes back as itself");
        smithy.setItem(5, ingots(1000, 900, 800));
        var full = hopper(h, new BlockPos(9, 1, 9), new ItemStack(Items.DIRT), new ItemStack(Items.STONE), new ItemStack(Items.SAND), new ItemStack(Items.GRAVEL), new ItemStack(Items.COBBLESTONE));
        tick(h, full);
        h.assertTrue(grades(smithy.getItem(5)).equals(List.of(1000, 900, 800)), "A unit a hopper cannot take stays in the machine as itself");
        var sourcePos = h.absolutePos(new BlockPos(5, 2, 9)); h.getLevel().setBlockAndUpdate(sourcePos, Blocks.CHEST.defaultBlockState());
        var source = (ChestBlockEntity)h.getLevel().getBlockEntity(sourcePos); source.setItem(0, ingots(820, 820, 760, 760));
        h.assertTrue(grades(new net.neoforged.neoforge.items.wrapper.InvWrapper(source).extractItem(0, 1, true)).equals(List.of(820)), "A simulated extraction names the top unit a real one takes");
        var puller = hopper(h, new BlockPos(5, 1, 9), ingots(760)); tick(h, puller);
        h.assertTrue(grades(puller.getItem(0)).equals(List.of(820, 760)) && grades(source.getItem(0)).equals(List.of(820, 760, 760)) && puller.getItem(1).isEmpty(),
                "A hopper pulls the chest's top unit and joins it onto its own differing stack");
        h.succeed();
    }
}
