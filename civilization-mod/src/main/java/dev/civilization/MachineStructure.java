package dev.civilization;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Small bounded patterns in controller-local coordinates; never requests missing chunks. */
public final class MachineStructure {
    public static final int INCOMPLETE = 0, COMPLETE = 1, UNLOADED = 2;
    public static final TagKey<Block> KILN_WALL = TagKey.create(Registries.BLOCK, ResourceLocation.parse("civilization:kiln_wall"));
    public static final TagKey<Block> COPPER = TagKey.create(Registries.BLOCK, ResourceLocation.parse("civilization:works_copper"));
    public record Part(int x, int y, int depth, String material) {}
    public record Result(int status, BlockPos problem, String material) {}
    private static final List<Part> KILN = pattern(false), WORKS = pattern(true);
    private MachineStructure() {}

    private static List<Part> pattern(boolean works) {
        var parts = new ArrayList<Part>();
        for (int y = 0; y < 3; y++) for (int depth = 0; depth < 3; depth++) for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0 && depth == 0) continue; // Controller at bottom/front center.
            String material = works ? "brick" : "stone";
            if (x == 0 && y == 1 && depth == 1) material = "air";
            else if (x == 0 && y == 1 && depth == 0) material = works ? "brick_hatch" : "stone_hatch";
            else if (works && y == 2 && x != 0 && depth != 1) material = "copper";
            parts.add(new Part(x, y, depth, material));
        }
        if (works) parts.add(new Part(0, 3, 1, "copper"));
        return List.copyOf(parts);
    }
    public static List<Part> parts(boolean works) { return works ? WORKS : KILN; }
    public static BlockPos position(BlockPos controller, Direction front, Part part) {
        return controller.relative(front.getClockWise(), part.x()).relative(front.getOpposite(), part.depth()).above(part.y());
    }
    public static boolean matches(BlockState state, String material) {
        return switch (material) {
            case "air" -> state.isAir();
            case "stone" -> state.is(KILN_WALL);
            case "brick" -> state.is(Blocks.BRICKS);
            case "copper" -> state.is(COPPER);
            case "stone_hatch" -> state.is(KILN_WALL) || hopper(state);
            case "brick_hatch" -> state.is(Blocks.BRICKS) || hopper(state);
            default -> false;
        };
    }
    private static boolean hopper(BlockState state) { return state.is(Blocks.HOPPER) && state.getValue(HopperBlock.FACING) == Direction.DOWN; }
    public static Result check(Level level, BlockPos controller, Direction front, boolean works) {
        // Check all chunk availability first: don't treat unloaded walls as broken construction.
        for (var part : parts(works)) {
            var pos = position(controller, front, part);
            if (!level.hasChunkAt(pos)) return new Result(UNLOADED, pos, "unloaded");
        }
        for (var part : parts(works)) {
            var pos = position(controller, front, part);
            if (!matches(level.getBlockState(pos), part.material())) return new Result(INCOMPLETE, pos, part.material());
        }
        return new Result(COMPLETE, null, "");
    }
}
