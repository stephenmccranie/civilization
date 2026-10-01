package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class PrototypeStoveBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public PrototypeStoveBlock(){super(Properties.ofFullCopy(Blocks.FURNACE).noOcclusion().lightLevel(s->s.getValue(LIT)?7:0));registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,LIT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PrototypeStoveEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&t==PrototypeStoveContent.ENTITY.get()?(w,p,b,e)->((PrototypeStoveEntity)e).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(pos) instanceof PrototypeStoveEntity stove&&stove.valid(p)){if(p.isShiftKeyDown()){if(!stove.finish())p.displayClientMessage(net.minecraft.network.chat.Component.literal(stove.batch()?"The batch needs more cooking before serving.":"Load a batch in the stove first."),true);}else p.openMenu(stove,b->b.writeBlockPos(pos));}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof PrototypeStoveEntity stove){if(!l.isClientSide){stove.dropBatch();Containers.dropContents(l,p,stove);}l.updateNeighbourForOutputSignal(p,this);}super.onRemove(s,l,p,next,moving);}
}
