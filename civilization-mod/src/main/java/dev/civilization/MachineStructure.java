package dev.civilization;

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
    public record Part(int x, int y, int depth, String material, int units, Direction side, int corner) {
        public Part(int x,int y,int depth,String material,int units,Direction side){this(x,y,depth,material,units,side,0);}
    }
    public record Result(int status, BlockPos problem, String material) {}
    private MachineStructure() {}

    /** Every controller that exposes the contextual construction guide uses this blueprint. */
    public static List<Part> guideParts(BlockState state) {
        if (state.getBlock() instanceof ArenaBlock) return ArenaStructure.PARTS;
        if (state.getBlock() instanceof BulkBlock bulk) return BulkStructure.parts(bulk.liquid);
        if (state.getBlock() instanceof IndustrialBlock industrial) return industrial.model()!=null?List.of():IndustrialStructure.parts(industrial.kind);
        if (state.is(CivicContent.TABLE.get())) return SurveyTable.PARTS;
        return List.of();
    }
    public static BlockPos position(BlockPos controller, Direction front, Part part) {
        return controller.relative(front.getClockWise(), part.x()).relative(front.getOpposite(), part.depth()).above(part.y());
    }
    public static boolean matches(BlockState state, String material) {
        return switch (material) {
            case "arena_stone" -> state.is(Blocks.STONE_BRICKS);
            case "arena_gate" -> state.is(Blocks.OAK_FENCE_GATE);
            case "glass" -> state.is(Blocks.GLASS);
            case "planks" -> state.is(net.minecraft.tags.BlockTags.PLANKS);
            case "casing" -> state.is(IndustrialContent.CASING.get());
            case "input_port", "output_port", "aux_port" -> state.is(IndustrialContent.PORT.get());
            case "survey_section" -> state.is(CivicContent.TABLE_PART.get());
            case "air" -> state.isAir();
            case "stone" -> state.is(KILN_WALL);
            default -> false;
        };
    }
    public static Direction side(Part part, Direction front) {
        if (part.side().getAxis().isVertical()) return part.side();
        return switch(part.side()){case EAST->front.getClockWise();case WEST->front.getCounterClockWise();case SOUTH->front.getOpposite();default->front;};
    }
    public static BlockState materialState(String material) {
        return switch (material) {
            case "arena_stone" -> Blocks.STONE_BRICKS.defaultBlockState();
            case "arena_gate" -> Blocks.OAK_FENCE_GATE.defaultBlockState();
            case "glass" -> Blocks.GLASS.defaultBlockState();
            case "planks" -> Blocks.OAK_PLANKS.defaultBlockState();
            case "casing" -> IndustrialContent.CASING.get().defaultBlockState();
            case "input_port", "output_port", "aux_port" -> IndustrialContent.PORT.get().defaultBlockState();
            case "survey_section" -> CivicContent.TABLE_PART.get().defaultBlockState();
            case "air" -> Blocks.AIR.defaultBlockState();
            case "stone" -> Blocks.COBBLESTONE.defaultBlockState();
            default -> throw new IllegalArgumentException("Unknown structure material: " + material);
        };
    }
    public static BlockState shape(Part part, Direction front) {
        if(part.units()==4){var state=materialState(part.material());
            if(part.material().equals("arena_gate"))return state.setValue(FenceGateBlock.FACING,front.getClockWise());
            if(state.hasProperty(RefineryPortBlock.FACING))state=state.setValue(RefineryPortBlock.FACING,side(part,front));
            if(part.material().equals("survey_section"))state=state.setValue(CivicBlock.FACING,front);
            return state;}
        var local=CutGeometry.bounds(part.units(),part.side(),part.corner());
        var rotation=switch(front){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};
        return CutGeometry.state(CutGeometry.rotate(local,rotation));
    }
    /** Held variants change material, never the required orientation/connections of the same block. */
    public static BlockState previewState(Part part, Direction front, BlockState held) {
        if (part.material().equals("air")) return null;
        var expected = shape(part, front);
        if (part.units() < 4) return expected;
        return held != null && !held.is(expected.getBlock()) && matches(held, part.material()) ? held : expected;
    }
    public static boolean matches(Level level, BlockPos pos, Part part, Direction front) {
        var actual = level.getBlockState(pos);
        if (part.units() == 4) return matches(actual, part.material())
                && (!part.material().equals("arena_gate") || actual.getValue(FenceGateBlock.FACING).getAxis()==front.getClockWise().getAxis());
        var expected=CutBlock.bounds(shape(part,front));
        if(actual.getBlock() instanceof SlabBlock && part.units()==2)
            return matches(SlabIntegration.material(actual),part.material()) && actual.getShape(level,pos).bounds().equals(expected);
        if(!actual.is(CuttingContent.PIECE.get()) || !(level.getBlockEntity(pos) instanceof CutBlockEntity cut))return false;
        var cells=cut.cells();if(CutCells.mask(cells)!=CutCells.mask(expected))return false;
        for(var cell:cells)if(cell!=null && !matches(cell,part.material()))return false;
        return true;
    }
    /** Shared fixture helper; survival construction still uses normal block placement. */
    public static void placePart(Level level, BlockPos controller, Direction front, Part part) {
        var pos = position(controller, front, part);
        level.setBlockAndUpdate(pos, shape(part, front));
        if (level.getBlockEntity(pos) instanceof CutBlockEntity cut) cut.material(materialState(part.material()));
    }
    public static Result checkParts(Level level, BlockPos controller, Direction front, List<Part> parts) {
        // An unloaded shell wins over an incomplete one, without reading every part twice.
        Result missing=null;
        for (var part : parts) {
            var pos = position(controller, front, part);
            if (!Chunks.loaded(level, pos)) return new Result(UNLOADED, pos, "unloaded");
            if (missing==null && !matches(level, pos, part, front))
                missing=new Result(INCOMPLETE, pos, part.material() + (part.units() == 4 ? "" : part.units() == 2 ? "_half" : "_quarter"));
        }
        return missing==null?new Result(COMPLETE, null, ""):missing;
    }
}
