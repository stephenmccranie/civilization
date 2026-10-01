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
    public record Part(int x, int y, int depth, String material, int units, Direction side, int corner, int railConnections, int sharedMask) {
        public Part(int x,int y,int depth,String material,int units,Direction side,int corner,int railConnections){this(x,y,depth,material,units,side,corner,railConnections,0);}
        public Part(int x,int y,int depth,String material,int units,Direction side,int corner){this(x,y,depth,material,units,side,corner,0);}
        public Part(int x,int y,int depth,String material,int units,Direction side){this(x,y,depth,material,units,side,-1);}
        public Part shared(int mask){return new Part(x,y,depth,material,units,side,corner,railConnections,mask);}
    }
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
            int units = 4;
            Direction side = Direction.DOWN;
            if (y == 2) units = x == 0 && depth == 1 ? 4 : x != 0 && depth != 1 ? 3 : 2;
            if (y == 1 && x != 0 && depth != 1) { units = 1; side = x < 0 ? Direction.EAST : Direction.WEST; }
            parts.add(new Part(x, y, depth, material, units, side));
        }
        if (works) parts.add(new Part(0, 3, 1, "copper", 2, Direction.DOWN));
        return List.copyOf(parts);
    }
    public static List<Part> foundryParts() {
        var a=new ArrayList<Part>();
        for(int y=0;y<3;y++)for(int z=0;z<3;z++)for(int x=-1;x<=1;x++) {
            if(x==0&&y==0&&z==0)continue;
            String material=x==0&&y==1&&z==1?"air":"brick";
            int units=y==2?(x==0&&z==2?4:x!=0&&z!=1?3:2):4;
            a.add(new Part(x,y,z,material,units,Direction.DOWN));
        }
        a.add(new Part(0,3,2,"brick",4,Direction.DOWN));
        a.add(new Part(0,4,2,"brick",2,Direction.DOWN));
        return List.copyOf(a);
    }
    private static final List<Part> FOUNDRY = foundryParts();
    public static List<Part> parts(BlockState state) {return state.getBlock() instanceof WorkshopBlock w?WorkshopStructure.parts(w.kind):state.is(KilnContent.FOUNDRY.get())?FOUNDRY:parts(state.is(KilnContent.RETORT.get()));}
    /** Every controller that exposes the contextual construction guide uses this blueprint. */
    public static List<Part> guideParts(BlockState state) {
        if (state.getBlock() instanceof BulkBlock bulk) return BulkStructure.parts(bulk.liquid);
        if (state.getBlock() instanceof OilEngineBlock) return OilEngineStructure.PARTS;
        if (state.getBlock() instanceof IndustrialBlock industrial) return IndustrialStructure.parts(industrial.kind);
        if (state.is(CivicContent.TABLE.get())) return SurveyTable.PARTS;
        if (state.getBlock() instanceof KilnBlock) return parts(state);
        return List.of();
    }
    public static List<Part> parts(boolean works) { return works ? WORKS : KILN; }
    public static BlockPos position(BlockPos controller, Direction front, Part part) {
        return controller.relative(front.getClockWise(), part.x()).relative(front.getOpposite(), part.depth()).above(part.y());
    }
    public static boolean matches(BlockState state, String material) {
        return switch (material) {
            case "glass" -> state.is(Blocks.GLASS);
            case "anvil" -> state.is(net.minecraft.tags.BlockTags.ANVIL);
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
            case "stone_hatch" -> state.is(KILN_WALL) || hopper(state);
            case "brick_hatch" -> state.is(Blocks.BRICKS) || hopper(state);
            default -> false;
        };
    }
    public static Direction side(Part part, Direction front) {
        if (part.side().getAxis().isVertical()) return part.side();
        return switch(part.side()){case EAST->front.getClockWise();case WEST->front.getCounterClockWise();case SOUTH->front.getOpposite();default->front;};
    }
    public static BlockState materialState(String material) {
        return switch (material) {
            case "glass" -> Blocks.GLASS.defaultBlockState();
            case "anvil" -> Blocks.ANVIL.defaultBlockState();
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
            case "brick", "brick_hatch" -> Blocks.BRICKS.defaultBlockState();
            case "copper" -> Blocks.COPPER_BLOCK.defaultBlockState();
            case "stone", "stone_hatch" -> Blocks.COBBLESTONE.defaultBlockState();
            default -> throw new IllegalArgumentException("Unknown structure material: " + material);
        };
    }
    public static BlockState shape(Part part, Direction front) {
        if(part.units()==4){var state=materialState(part.material());
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
        if (part.units() == 4) return matches(actual, part.material()) && (!part.material().startsWith("engine_") || actual.getValue(CivicBlock.FACING)==front);
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
    private static boolean hopper(BlockState state) { return state.is(Blocks.HOPPER) && state.getValue(HopperBlock.FACING) == Direction.DOWN; }
    public static Result check(Level level, BlockPos controller, Direction front) {
        return checkParts(level, controller, front, parts(level.getBlockState(controller)));
    }
    public static Result checkParts(Level level, BlockPos controller, Direction front, List<Part> parts) {
        // An unloaded shell wins over an incomplete one, without reading every part twice.
        Result missing=null;
        for (var part : parts) {
            var pos = position(controller, front, part);
            if (!level.hasChunkAt(pos)) return new Result(UNLOADED, pos, "unloaded");
            if (missing==null && !matches(level, pos, part, front))
                missing=new Result(INCOMPLETE, pos, part.material() + (part.units() == 4 ? "" : part.units() == 2 ? "_half" : part.units()==3?"_eighth":"_quarter"));
        }
        return missing==null?new Result(COMPLETE, null, ""):missing;
    }
}
