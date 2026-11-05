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
    public static final TagKey<Block> COPPER = TagKey.create(Registries.BLOCK, ResourceLocation.parse("civilization:works_copper"));
    public record Part(int x, int y, int depth, String material, int units, Direction side, int corner, int railConnections, int sharedMask) {
        public Part(int x,int y,int depth,String material,int units,Direction side,int corner,int railConnections){this(x,y,depth,material,units,side,corner,railConnections,0);}
        public Part(int x,int y,int depth,String material,int units,Direction side,int corner){this(x,y,depth,material,units,side,corner,0);}
        public Part(int x,int y,int depth,String material,int units,Direction side){this(x,y,depth,material,units,side,-1);}
        public Part shared(int mask){return new Part(x,y,depth,material,units,side,corner,railConnections,mask);}
    }
    public record Result(int status, BlockPos problem, String material) {}
    private MachineStructure() {}

    /** The block structure of the Tannery or Textile Workshop; every other kiln-family machine is a native model. */
    public static List<Part> parts(BlockState state) {return WorkshopStructure.parts(((WorkshopBlock)state.getBlock()).kind);}
    /** Every controller that exposes the contextual construction guide uses this blueprint. */
    public static List<Part> guideParts(BlockState state) {
        if (state.getBlock() instanceof ArenaBlock) return ArenaStructure.PARTS;
        if (state.getBlock() instanceof BulkBlock bulk) return BulkStructure.parts(bulk.liquid);
        if (state.getBlock() instanceof OilEngineBlock) return OilEngineStructure.PARTS;
        if (state.getBlock() instanceof IndustrialBlock industrial) return industrial.kind==IndustrialBlock.Kind.PUMP?List.of():IndustrialStructure.parts(industrial.kind);
        if (state.is(CivicContent.TABLE.get())) return SurveyTable.PARTS;
        if (state.getBlock() instanceof KilnBlock kiln) return kiln.model() != null ? List.of() : parts(state);
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
            case "guardrail" -> state.is(IndustrialContent.GUARDRAIL.get());
            case "cooling" -> state.is(IndustrialContent.COOLING.get());
            case "flue" -> state.is(IndustrialContent.FLUE.get());
            case "casing" -> state.is(IndustrialContent.CASING.get());
            case "input_port", "output_port", "aux_port" -> state.is(IndustrialContent.PORT.get());
            case "ladder" -> state.is(Blocks.LADDER);
            case "bars" -> state.is(Blocks.IRON_BARS);
            case "iron" -> state.is(Blocks.IRON_BLOCK);
            case "engine_cylinder" -> state.is(OilEngineContent.CYLINDER.get());
            case "engine_flywheel" -> state.is(OilEngineContent.WHEEL.get());
            case "survey_section" -> state.is(CivicContent.TABLE_PART.get());
            case "air" -> state.isAir();
            case "stone" -> state.is(KILN_WALL);
            case "brick" -> state.is(Blocks.BRICKS);
            case "copper" -> state.is(COPPER);
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
            case "guardrail" -> IndustrialContent.GUARDRAIL.get().defaultBlockState();
            case "cooling" -> IndustrialContent.COOLING.get().defaultBlockState();
            case "flue" -> IndustrialContent.FLUE.get().defaultBlockState();
            case "casing" -> IndustrialContent.CASING.get().defaultBlockState();
            case "input_port", "output_port", "aux_port" -> IndustrialContent.PORT.get().defaultBlockState();
            case "ladder" -> Blocks.LADDER.defaultBlockState();
            case "bars" -> Blocks.IRON_BARS.defaultBlockState();
            case "iron" -> Blocks.IRON_BLOCK.defaultBlockState();
            case "engine_cylinder" -> OilEngineContent.CYLINDER.get().defaultBlockState();
            case "engine_flywheel" -> OilEngineContent.WHEEL.get().defaultBlockState();
            case "survey_section" -> CivicContent.TABLE_PART.get().defaultBlockState();
            case "air" -> Blocks.AIR.defaultBlockState();
            case "brick" -> Blocks.BRICKS.defaultBlockState();
            case "copper" -> Blocks.COPPER_BLOCK.defaultBlockState();
            case "stone" -> Blocks.COBBLESTONE.defaultBlockState();
            default -> throw new IllegalArgumentException("Unknown structure material: " + material);
        };
    }
    public static BlockState shape(Part part, Direction front) {
        if(part.units()==4){var state=materialState(part.material());
            if(part.material().equals("arena_gate"))return state.setValue(FenceGateBlock.FACING,front.getClockWise());
            if(state.is(IndustrialContent.GUARDRAIL.get())) {
                for(var d:Direction.Plane.HORIZONTAL)state=state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),(part.railConnections() & (1 << d.get2DDataValue()))!=0);
                state=state.rotate(switch(front){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;});
            }
            if(state.hasProperty(RefineryPortBlock.FACING))state=state.setValue(RefineryPortBlock.FACING,side(part,front));
            if(part.material().startsWith("engine_")||part.material().equals("survey_section"))state=state.setValue(CivicBlock.FACING,front);
            if(state.getBlock() instanceof AnvilBlock)state=state.setValue(AnvilBlock.FACING,front.getClockWise());
            if(state.is(Blocks.LADDER))state=state.setValue(LadderBlock.FACING,side(part,front));return state;}
        int corner=part.corner()>=0?part.corner():part.units()==1?2+(part.depth()==0?1:0):part.units()==3?(part.x()<0?1:0)+(part.depth()==0?2:0):0;
        var local=CutGeometry.bounds(part.units(),part.side(),corner);
        var rotation=switch(front){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};
        return CutGeometry.state(CutGeometry.rotate(local,rotation));
    }
    /** Held variants change material, never the required orientation/connections of the same block. */
    public static BlockState previewState(Part part, Direction front, BlockState held) {
        if (part.material().equals("air")) return null;
        var expected = shape(part, front);
        if (part.units() < 4 || part.material().endsWith("_port") || part.material().equals("ladder")) return expected;
        return held != null && !held.is(expected.getBlock()) && matches(held, part.material()) ? held : expected;
    }
    public static boolean matches(Level level, BlockPos pos, Part part, Direction front) {
        var actual = level.getBlockState(pos);
        if(part.sharedMask()!=0){
            // Complementary cut pieces can canonicalize into their ordinary full block.
            if(part.sharedMask()==255 && CuttingContent.cuttable(actual) && matches(actual,part.material()))return true;
            var cells=CutCells.read(level,pos);int required=CutCells.mask(CutBlock.bounds(shape(part,front)));
            if((CutCells.mask(cells)&required)!=required || obstructed(level,pos,part,front))return false;
            for(int i=0;i<8;i++)if((required&(1<<i))!=0 && !matches(cells[i],part.material()))return false;
            return true;
        }
        if (part.units() == 4) return matches(actual, part.material()) && (!part.material().startsWith("engine_") || actual.getValue(CivicBlock.FACING)==front)
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
        if(part.sharedMask()!=0){
            var cells=CutCells.read(level,pos);int mask=CutCells.mask(CutBlock.bounds(shape(part,front)));
            for(int i=0;i<8;i++)if((mask&(1<<i))!=0)cells[i]=materialState(part.material());
            level.setBlockAndUpdate(pos,shape(part,front));
            ((CutBlockEntity)level.getBlockEntity(pos)).cells(cells);return;
        }
        level.setBlockAndUpdate(pos, shape(part, front));
        if (level.getBlockEntity(pos) instanceof CutBlockEntity cut) cut.material(materialState(part.material()));
    }
    /** Shared-cell requirements allow their sibling pieces, but no extra cells or wrong materials. */
    public static boolean obstructed(Level level,BlockPos pos,Part part,Direction front){
        if(level.getBlockState(pos).isAir())return false;
        if(part.sharedMask()==0)return true;
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof SlabBlock)&&!state.is(CuttingContent.PIECE.get()))return true;
        var rotation=switch(front){case EAST->Rotation.CLOCKWISE_90;case SOUTH->Rotation.CLOCKWISE_180;case WEST->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};
        int allowed=0;
        for(int i=0;i<8;i++)if((part.sharedMask()&(1<<i))!=0)allowed|=CutCells.mask(CutGeometry.rotate(CutCells.box(i),rotation));
        var cells=CutCells.read(level,pos);
        if((CutCells.mask(cells)&~allowed)!=0)return true;
        return false;
    }
    public static Result check(Level level, BlockPos controller, Direction front) {
        return checkParts(level, controller, front, parts(level.getBlockState(controller)));
    }
    public static Result checkParts(Level level, BlockPos controller, Direction front, List<Part> parts) {
        // An unloaded shell wins over an incomplete one, without reading every part twice.
        Result missing=null;
        for (var part : parts) {
            var pos = position(controller, front, part);
            if (!Chunks.loaded(level, pos)) return new Result(UNLOADED, pos, "unloaded");
            if (missing==null && !matches(level, pos, part, front))
                missing=new Result(INCOMPLETE, pos, part.material() + (part.units() == 4 ? "" : part.units() == 2 ? "_half" : part.units()==3?"_eighth":"_quarter"));
        }
        return missing==null?new Result(COMPLETE, null, ""):missing;
    }
}
