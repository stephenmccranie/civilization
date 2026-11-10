package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class MultiblockGameTests {
    private static KilnBlockEntity controller(GameTestHelper h, boolean works, Direction front) {
        var pos = h.absolutePos(new BlockPos(5, 1, 5));
        // A turned controller is a new machine: take the old one down first.
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, (works ? KilnContent.RETORT.get() : KilnContent.KILN.get())
                .defaultBlockState().setValue(AbstractFurnaceBlock.FACING, front));
        return (KilnBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    /** The machine's highest cell: the top of its chimney or tower. */
    private static BlockPos top(KilnBlockEntity machine) {
        return machine.model().cells.stream().map(c -> ModeledMachine.position(machine.getBlockPos(), machine.front(), c))
                .max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
    }
    private static void ticks(GameTestHelper h, KilnBlockEntity machine, int count) {
        CoalFireFixture.light(machine);
        for (int i = 0; i < count; i++) KilnBlockEntity.tick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
    /** A controller drawn by a native model leaves the ground beneath it and the blocks beside it drawn, since its model
     *  never fills its cell; a block-structure controller still hides them like any full block. */
    @GameTest(template = "industrial")
    public static void modeledControllersHideNoNeighbourFaces(GameTestHelper h) {
        var l = h.getLevel(); var at = h.absolutePos(new BlockPos(5, 1, 5)); var below = at.below(); int modeled = 0;
        l.setBlockAndUpdate(below, Blocks.GRASS_BLOCK.defaultBlockState());
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof ModeledBlock m) || m.model() == null) continue;
            l.setBlockAndUpdate(at, block.defaultBlockState());
            h.assertTrue(net.minecraft.world.level.block.Block.shouldRenderFace(l.getBlockState(below), l, below, Direction.UP, at), "The ground under a " + block + " stays drawn");
            modeled++;
        }
        h.assertTrue(modeled == MachineModel.all().size(), "Every machine model has one controller, found " + modeled);
        l.setBlockAndUpdate(at, IndustrialContent.COLUMN.get().defaultBlockState());
        h.assertTrue(!net.minecraft.world.level.block.Block.shouldRenderFace(l.getBlockState(below), l, below, Direction.UP, at), "A block-structure controller hides the face under it");
        h.succeed();
    }
    /** Every part of a machine carries its machine's name and gives its controller on pick-block, so Jade and the hand name
     *  the machine wherever it is aimed at. */
    @GameTest(template = "empty")
    public static void machinePartsAreNamedForTheirMachine(GameTestHelper h) {
        int named = 0;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof ModeledBlock m) || m.model() == null) continue;
            var part = MachineParts.block(m.model());
            h.assertTrue(part.getDescriptionId().equals(block.getDescriptionId()), "A " + block + " part is named " + part.getDescriptionId());
            h.assertTrue(part.getCloneItemStack(h.getLevel(), BlockPos.ZERO, part.defaultBlockState()).is(block.asItem()), "Picking a " + block + " part gives its controller");
            named++;
        }
        h.assertTrue(named == MachineModel.all().size(), "Every machine model's parts are named, found " + named);
        h.succeed();
    }
    /** A survival player walking into a masonry machine from any side, along any line across it, meets a wall: the step-up
     *  takes them onto its footing at most, never up its leaning courses. */
    @GameTest(template = "industrial")
    public static void playersCannotWalkUpMasonryMachines(GameTestHelper h) {
        var l = h.getLevel(); var at = h.absolutePos(new BlockPos(5, 1, 5));
        // Level ground under the machine and all round it, so walkers meet its foot.
        for (var cell : BlockPos.betweenClosed(at.offset(-6, -1, -6), at.offset(6, -1, 6))) l.setBlockAndUpdate(cell, Blocks.STONE.defaultBlockState());
        for (var block : java.util.List.of(KilnContent.KILN.get(), KilnContent.FOUNDRY.get(), KilnContent.RETORT.get(), WorkshopContent.SMITHY.get())) {
            l.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(at, block.defaultBlockState());
            MachineFixture.build(l, at);
            int reach = ModeledMachine.of(l.getBlockEntity(at)).model().reach;
            for (var side : Direction.Plane.HORIZONTAL) for (double across = -reach - 1; across <= reach + 1; across += .25) {
                var walker = new net.neoforged.neoforge.common.util.FakePlayer(l, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "walker"));
                walker.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var start = net.minecraft.world.phys.Vec3.atBottomCenterOf(at).relative(side, reach + 1.5).relative(side.getClockWise(), across); walker.setPos(start);
                double highest = start.y;
                for (int i = 0; i < 2 * (reach + 1.5) / .2; i++) {
                    walker.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(-side.getStepX() * .2, -.1, -side.getStepZ() * .2));
                    highest = Math.max(highest, walker.getY());
                }
                h.assertTrue(highest <= start.y + walker.maxUpStep() + 1e-3, block + ": a player walking in from the " + side + ", " + across + " across, rises " + (highest - start.y) + " blocks");
            }
        }
        h.succeed();
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
        MachineFixture.build(h.getLevel(), machine.getBlockPos());
        machine.setItem(0, new ItemStack(Items.CLAY));
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack(2));
        ticks(h, machine, 199);
        // Breaking one cell of the wall removes that whole section and refunds it.
        var wall = machine.getBlockPos().above(2);
        h.getLevel().destroyBlock(wall, false);
        ticks(h, machine, 1);
        h.assertTrue(machine.getItem(2).isEmpty() && machine.getItem(0).getCount() == 1 && machine.getItem(1).getCount() == 1, "No completion or new fuel consumption after breach");
        h.assertTrue(!machine.getBlockState().getValue(AbstractFurnaceBlock.LIT), "Breach extinguishes the controller");
        h.assertTrue(machine.build().sections != machine.model().all, "Breach removes a section");
        MachineFixture.build(h.getLevel(), machine.getBlockPos());
        ticks(h, machine, 199);
        h.assertTrue(machine.getItem(2).isEmpty(), "Repair cannot reuse old partial progress");
        ticks(h, machine, 1);
        h.assertTrue(machine.getItem(2).getCount() == 4 && machine.getItem(1).isEmpty(), "Repair permits a new full batch with another fuel");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void structuresFaceAllHorizontalDirections(GameTestHelper h) {
        for (var front : Direction.Plane.HORIZONTAL) {
            var machine = controller(h, true, front);
            MachineFixture.build(h.getLevel(), machine.getBlockPos());
            h.assertTrue(machine.checkStructure().status() == MachineStructure.COMPLETE, "Structure must rotate with facing " + front);
            var tower = top(machine);
            h.assertTrue(tower.getY() > machine.getBlockPos().getY() + 2 && h.getLevel().getBlockState(tower).is(MachineParts.block(machine.model())), "The tower rises above the furnace, facing " + front);
            h.getLevel().destroyBlock(tower, false);
            h.assertTrue(machine.checkStructure().status() == MachineStructure.INCOMPLETE, "Breaking the tower leaves the furnace incomplete, facing " + front);
        }
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void saveCannotPreserveAFalseFormedState(GameTestHelper h) {
        var machine = controller(h, true, Direction.NORTH);
        MachineFixture.build(h.getLevel(), machine.getBlockPos());
        machine.setItem(0, IndustrialContent.ENRICHED_BLEND.toStack());
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, machine, 100);
        var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        h.getLevel().setBlockAndUpdate(top(machine), Blocks.AIR.defaultBlockState());
        var loaded = new FertilizerRetortBlockEntity(machine.getBlockPos(), machine.getBlockState());
        loaded.setLevel(h.getLevel()); loaded.loadWithComponents(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(loaded);
        ticks(h, loaded, 500);
        h.assertTrue(loaded.getItem(2).isEmpty() && loaded.getItem(0).getCount() == 1, "Reload checks real blocks before using saved work");
        h.assertTrue(loaded.structureStatus() == MachineStructure.INCOMPLETE, "Formed status is recomputed, not trusted from save");
        h.succeed();
    }
}
