package dev.civilization;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
public class EnginePieceBlock extends Block {
 public EnginePieceBlock(boolean wheel){super(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,OilEngineStructure.placementFacing(c.getLevel(),c.getClickedPos(),this,c.getHorizontalDirection().getOpposite()));}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
}
