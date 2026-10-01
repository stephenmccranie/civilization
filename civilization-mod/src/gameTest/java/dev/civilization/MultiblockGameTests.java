package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class MultiblockGameTests {
    private static KilnBlockEntity controller(GameTestHelper h, boolean works, Direction front) {
        var pos = h.absolutePos(new BlockPos(5, 1, 5));
        h.getLevel().setBlockAndUpdate(pos, (works ? KilnContent.RETORT.get() : KilnContent.KILN.get())
                .defaultBlockState().setValue(AbstractFurnaceBlock.FACING, front));
        return (KilnBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static void ticks(GameTestHelper h, KilnBlockEntity machine, int count) {
        CoalFireFixture.light(machine);
        for (int i = 0; i < count; i++) KilnBlockEntity.tick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
    @GameTest(template = "industrial")
    public static void standaloneControllersCannotOperate(GameTestHelper h) {
        for (boolean works : new boolean[]{false, true}) {
            var machine = controller(h, works, Direction.NORTH);
            machine.setItem(0, works ? IndustrialContent.ENRICHED_BLEND.toStack() : new ItemStack(Items.CLAY));
            machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
            ticks(h, machine, 500);
            h.assertTrue(machine.getItem(0).getCount() == 1 && machine.getItem(1).getCount() == 1 && machine.getItem(2).isEmpty(), "Bare controller cannot burn fuel or process items");
            h.assertTrue(machine.structureStatus() == MachineStructure.INCOMPLETE, "Bare controller reports missing structure");
        }
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void brokenStructureStopsBeforeBatchCompletion(GameTestHelper h) {
        var machine = controller(h, false, Direction.NORTH);
        KilnGameTests.buildShell(h, machine.getBlockPos(), Direction.NORTH, false);
        machine.setItem(0, new ItemStack(Items.CLAY));
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack(2));
        ticks(h, machine, 199);
        var wall = machine.getBlockPos().above(2);
        var wallState = h.getLevel().getBlockState(wall);
        h.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
        ticks(h, machine, 1);
        h.assertTrue(machine.getItem(2).isEmpty() && machine.getItem(0).getCount() == 1 && machine.getItem(1).getCount() == 1, "No completion or new fuel consumption after breach");
        h.assertTrue(!machine.getBlockState().getValue(AbstractFurnaceBlock.LIT), "Breach extinguishes the controller");
        h.assertTrue(machine.checkStructure().problem().equals(wall), "Feedback identifies the missing block exactly");
        h.getLevel().setBlockAndUpdate(wall, wallState);
        ((CutBlockEntity) h.getLevel().getBlockEntity(wall)).material(Blocks.COBBLESTONE.defaultBlockState());
        ticks(h, machine, 199);
        h.assertTrue(machine.getItem(2).isEmpty(), "Repair cannot reuse old partial progress");
        ticks(h, machine, 1);
        h.assertTrue(machine.getItem(2).getCount() == 4 && machine.getItem(1).isEmpty(), "Repair permits a new full batch with another fuel");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void worksAcceptsAgedCopperAndHopperButNeedsEmptyChamber(GameTestHelper h) {
        var machine = controller(h, true, Direction.NORTH);
        var pos = machine.getBlockPos();
        KilnGameTests.buildShell(h, pos, Direction.NORTH, true);
        for (var part : MachineStructure.parts(true)) if (part.material().equals("copper")) {
            var cut = (CutBlockEntity) h.getLevel().getBlockEntity(MachineStructure.position(pos, Direction.NORTH, part));
            cut.material(Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState());
        }
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.assertTrue(machine.checkStructure().status() == MachineStructure.COMPLETE, "Aged/waxed copper and downward input hatch form correctly");
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        h.assertTrue(machine.checkStructure().status() == MachineStructure.INCOMPLETE, "Sideways hopper is not a valid input hatch");
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.BRICKS.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.south().above(), Blocks.STONE.defaultBlockState());
        h.assertTrue(machine.checkStructure().material().equals("air"), "Obstructed chamber must be cleared");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void structuresFaceAllHorizontalDirections(GameTestHelper h) {
        for (var front : Direction.Plane.HORIZONTAL) {
            var machine = controller(h, true, front);
            KilnGameTests.buildShell(h, machine.getBlockPos(), front, true);
            h.assertTrue(machine.checkStructure().status() == MachineStructure.COMPLETE, "Structure must rotate with facing " + front);
            var chimney = machine.getBlockPos().relative(front.getOpposite()).above(3);
            h.getLevel().setBlockAndUpdate(chimney, Blocks.AIR.defaultBlockState());
            h.assertTrue(machine.checkStructure().problem().equals(chimney), "Chimney is one block behind controller and three up");
            // Clear this orientation before the next controller is placed.
            for (var part : MachineStructure.parts(true)) h.getLevel().setBlockAndUpdate(MachineStructure.position(machine.getBlockPos(), front, part), Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void saveCannotPreserveAFalseFormedState(GameTestHelper h) {
        var machine = controller(h, true, Direction.NORTH);
        KilnGameTests.buildShell(h, machine.getBlockPos(), Direction.NORTH, true);
        machine.setItem(0, IndustrialContent.ENRICHED_BLEND.toStack());
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, machine, 100);
        var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        h.getLevel().setBlockAndUpdate(machine.getBlockPos().above(2), Blocks.AIR.defaultBlockState());
        var loaded = new FertilizerRetortBlockEntity(machine.getBlockPos(), machine.getBlockState());
        loaded.setLevel(h.getLevel()); loaded.loadWithComponents(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(loaded);
        ticks(h, loaded, 500);
        h.assertTrue(loaded.getItem(2).isEmpty() && loaded.getItem(0).getCount() == 1, "Reload checks real blocks before using saved work");
        h.assertTrue(loaded.structureStatus() == MachineStructure.INCOMPLETE, "Formed status is recomputed, not trusted from save");
        h.succeed();
    }
}
