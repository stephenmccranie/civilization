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
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(stack.is(PrototypeStoveContent.SKILLET.get())){
            var result=PrototypeStoveContent.SKILLET.get().useOn(new net.minecraft.world.item.context.UseOnContext(p,hand,hit));
            return result.consumesAction()?ItemInteractionResult.sidedSuccess(l.isClientSide):ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(pos) instanceof PrototypeStoveEntity stove&&stove.valid(p)){if(p.isShiftKeyDown()&&p.getMainHandItem().isEmpty()){var pan=stove.liftSkillet();if(!pan.isEmpty())p.setItemInHand(InteractionHand.MAIN_HAND,pan);}else p.openMenu(stove,b->b.writeBlockPos(pos));}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof PrototypeStoveEntity stove){if(!l.isClientSide){stove.dropBatch();Containers.dropContents(l,p,stove);}l.updateNeighbourForOutputSignal(p,this);}super.onRemove(s,l,p,next,moving);}
}
