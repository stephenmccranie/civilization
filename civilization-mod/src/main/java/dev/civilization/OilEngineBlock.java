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
public final class OilEngineBlock extends Block implements EntityBlock {
 public OilEngineBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING);}
 @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OilEngineEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&t==OilEngineContent.ENTITY.get()?(w,p,b,e)->((OilEngineEntity)e).tick():null;}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide&&CivicAccess.allowed(l,pos,p)&&l.getBlockEntity(pos) instanceof OilEngineEntity e)p.openMenu(e);return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(stack.is(IndustrialContent.CAN.get())||stack.is(IndustrialContent.FUEL_CAN.get())||stack.is(IndustrialContent.LUBE_CAN.get())){if(!l.isClientSide&&CivicAccess.allowed(l,pos,p)&&l.getBlockEntity(pos) instanceof OilEngineEntity e)e.canister(p,hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);}return MachineConstruction.useOn(stack,s,l,pos,p);}
 @Override public void animateTick(BlockState s,Level l,BlockPos pos,net.minecraft.util.RandomSource r){
  if(!(l.getBlockEntity(pos) instanceof OilEngineEntity e))return;
  var back=e.front().getOpposite();var right=e.front().getClockWise();
  if(e.status==3)l.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,pos.getX()+.5+back.getStepX()*2.375,pos.getY()+1.4075,pos.getZ()+.5+back.getStepZ()*2.375,0,.01,0);
  if(e.status==5){l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5+right.getStepX()*.1875+back.getStepX()*1.8333333,pos.getY()+2.5075,pos.getZ()+.5+right.getStepZ()*.1875+back.getStepZ()*1.8333333,0,.03,0);if(r.nextInt(4)==0)l.playLocalSound(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,net.minecraft.sounds.SoundEvents.PISTON_EXTEND,net.minecraft.sounds.SoundSource.BLOCKS,.12f,.65f,false);}
 }
 @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("Hot-bulb engine • refined fuel + lubricating oil"));lines.add(Component.literal("Build: 8 casing slabs, cylinder, flywheel"));lines.add(Component.literal("Fuel: front • Oil: underneath • Place for guide"));lines.add(Component.literal("Drain before breaking — stored liquids are lost."));}
}
