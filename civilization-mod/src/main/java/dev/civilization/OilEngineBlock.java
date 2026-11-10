package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
/** The Hot-Bulb Engine's controller: the front corner of a native model built in sections, lit while its lamps heat the
 *  bulbs to start. */
public final class OilEngineBlock extends Block implements EntityBlock, ModeledBlock {
 public OilEngineBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,false));}
 @Override public MachineModel model(){return MachineModel.OIL_ENGINE;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);}
 /** The model fills only part of the controller's cell, so it hides no neighbour's faces and passes light through. */
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return net.minecraft.world.phys.shapes.Shapes.empty();}
 @Override protected boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return true;}
 @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OilEngineEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t!=OilEngineContent.ENTITY.get()?null:l.isClientSide?(w,p,b,e)->dev.civilization.client.OilEngineEffects.tick((OilEngineEntity)e):(w,p,b,e)->((OilEngineEntity)e).tick();}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide&&CivicAccess.allowed(l,pos,p)&&l.getBlockEntity(pos) instanceof OilEngineEntity e)p.openMenu(e);return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
  if(stack.is(IndustrialContent.CAN.get())||stack.is(IndustrialContent.FUEL_CAN.get())||stack.is(IndustrialContent.LUBE_CAN.get())){if(!l.isClientSide&&CivicAccess.allowed(l,pos,p)&&l.getBlockEntity(pos) instanceof OilEngineEntity e)e.canister(p,hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
  var engine=ModeledMachine.of(l.getBlockEntity(pos));var held=engine==null?ItemStack.EMPTY:ModeledMachine.material(engine,p,stack);
  if(!held.isEmpty()){if(!l.isClientSide)ModeledMachine.build(engine,p,held);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
 }
 @Override protected void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(pos) instanceof OilEngineEntity e)ModeledMachine.dismantle(e);super.onRemove(s,l,pos,next,moving);}
 @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("Runs on refined fuel + lubricating oil"));lines.add(Component.literal("Place for the build guide"));lines.add(Component.literal("Drain liquids before breaking."));}
}
