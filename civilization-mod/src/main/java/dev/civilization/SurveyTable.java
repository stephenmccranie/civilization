package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
public final class SurveyTable {
    public static final List<MachineStructure.Part> PARTS=List.of(new MachineStructure.Part(1,0,0,"survey_section",4,Direction.DOWN),new MachineStructure.Part(0,0,1,"survey_section",4,Direction.DOWN),new MachineStructure.Part(1,0,1,"survey_section",4,Direction.DOWN));
    public static List<BlockPos> positions(Level level,BlockPos anchor){var out=new ArrayList<BlockPos>();out.add(anchor);var state=level.getBlockState(anchor);if(!state.is(CivicContent.TABLE.get()))return out;for(var part:PARTS)out.add(MachineStructure.position(anchor,state.getValue(CivicBlock.FACING),part));return out;}
    public static boolean complete(Level level,BlockPos anchor){if(!level.hasChunkAt(anchor)||!level.getBlockState(anchor).is(CivicContent.TABLE.get()))return false;for(var pos:positions(level,anchor))if(!pos.equals(anchor)&&(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(CivicContent.TABLE_PART.get())))return false;return true;}
    public static BlockPos corner(Level level,BlockPos anchor){var positions=positions(level,anchor);return new BlockPos(positions.stream().mapToInt(BlockPos::getX).min().orElse(anchor.getX()),anchor.getY(),positions.stream().mapToInt(BlockPos::getZ).min().orElse(anchor.getZ()));}
}
