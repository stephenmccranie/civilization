package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Invisible physical cells of one native model; no inventory or item per timber. */
public final class DerrickPartBlock extends Block {
    public static final IntegerProperty CELL=IntegerProperty.create("cell",0,ModeledDerrick.CELLS.size()-1);
    public DerrickPartBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(CELL,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,CELL);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){Level world=l instanceof Level level?level:l instanceof net.minecraft.world.level.chunk.LevelChunk chunk?chunk.getLevel():null;var cell=ModeledDerrick.CELLS.get(s.getValue(CELL));if(world==null)return cell.shape(s.getValue(CivicBlock.FACING));var m=owner(world,p,s);return m==null?Shapes.empty():cell.shape(s.getValue(CivicBlock.FACING),m.derrickSections);}
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(!l.isClientSide)l.scheduleTick(p,this,100);}
    @Override protected void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos p,net.minecraft.util.RandomSource random){
        var cell=ModeledDerrick.CELLS.get(s.getValue(CELL));var at=p.subtract(ModeledDerrick.position(BlockPos.ZERO,s.getValue(CivicBlock.FACING),cell));
        if(l.hasChunkAt(at)&&(owner(l,p,s)==null||getShape(s,l,p,CollisionContext.empty()).isEmpty()))l.removeBlock(p,false);else l.scheduleTick(p,this,1200);
    }
    public static IndustrialBlockEntity owner(Level l,BlockPos p,BlockState s){
        var cell=ModeledDerrick.CELLS.get(s.getValue(CELL));var front=s.getValue(CivicBlock.FACING);
        var at=p.subtract(ModeledDerrick.position(BlockPos.ZERO,front,cell));
        return l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof IndustrialBlockEntity m&&m.kind==IndustrialBlock.Kind.PUMP&&m.front()==front?m:null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        var m=owner(l,p,s);if(m!=null&&!l.isClientSide&&CivicAccess.allowed(l,p,player)&&CivicAccess.allowed(l,m.getBlockPos(),player))player.openMenu(m);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!l.isClientSide&&!s.is(next.getBlock())){var m=owner(l,p,s);if(m!=null&&!m.derrickChanging){for(int part:ModeledDerrick.CELLS.get(s.getValue(CELL)).pieces().keySet())if(ModeledDerrick.has(m,part)){ModeledDerrick.removeSection(m,part,true);break;}}}
        super.onRemove(s,l,p,next,moving);
    }
}
