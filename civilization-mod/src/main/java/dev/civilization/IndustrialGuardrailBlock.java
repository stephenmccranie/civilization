package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Open industrial railing; adjacent rail sections connect without a tool or mode. */
public final class IndustrialGuardrailBlock extends Block {
    public IndustrialGuardrailBlock(Properties p) {
        super(p);
        var s=stateDefinition.any();
        for(var d:Direction.Plane.HORIZONTAL)s=s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),false);
        registerDefaultState(s);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        for(var d:Direction.Plane.HORIZONTAL)b.add(PipeBlock.PROPERTY_BY_DIRECTION.get(d));
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) {
        var s=defaultBlockState();
        for(var d:Direction.Plane.HORIZONTAL)s=s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),c.getLevel().getBlockState(c.getClickedPos().relative(d)).is(this));
        return s;
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos n) {
        return d.getAxis().isHorizontal()?s.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),other.is(this)):s;
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {
        var shape=Block.box(7,0,7,9,16,9);
        for(var d:Direction.Plane.HORIZONTAL)if(s.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d)))shape=Shapes.or(shape,switch(d){
            case NORTH->Block.box(7,0,0,9,16,7);case SOUTH->Block.box(7,0,9,9,16,16);
            case WEST->Block.box(0,0,7,7,16,9);case EAST->Block.box(9,0,7,16,16,9);default->Shapes.empty();});
        return shape;
    }
    @Override protected BlockState rotate(BlockState s,Rotation r) {
        var result=s;for(var d:Direction.Plane.HORIZONTAL)result=result.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(r.rotate(d)),s.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d)));return result;
    }
    @Override protected BlockState mirror(BlockState s,Mirror m) {
        var result=s;for(var d:Direction.Plane.HORIZONTAL)result=result.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(m.mirror(d)),s.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d)));return result;
    }
}
