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

public final class IndustrialBlock extends Block implements EntityBlock {
    public enum Kind { PUMP, REFINERY, DRILL, TANK, COLUMN, CONDENSER }
    public final Kind kind;
    public IndustrialBlock(Properties p,Kind kind){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(MachineFeedback.WORKING,false).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,MachineFeedback.WORKING,net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new IndustrialBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide&&type==IndustrialContent.ENTITY.get()?(world,pos,state,entity)->((IndustrialBlockEntity)entity).tick():null;}
    @Override public void animateTick(BlockState state,Level l,BlockPos pos,net.minecraft.util.RandomSource random){
        if(!state.getValue(MachineFeedback.WORKING)&&!state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))return;
        if(kind==Kind.REFINERY){var outlet=pos.relative(state.getValue(CivicBlock.FACING).getOpposite(),4).above(8);if(random.nextFloat()<.4f)l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,outlet.getX()+.5,outlet.getY(),outlet.getZ()+.5,0,.04,0);}
        else if((kind==Kind.PUMP||kind==Kind.DRILL)&&random.nextFloat()<.2f)l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.05,pos.getZ()+.5,0,.025,0);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(stack.is(IndustrialContent.CAN.get())||stack.is(IndustrialContent.CRUDE_CAN.get())||stack.is(IndustrialContent.FUEL_CAN.get())||stack.is(IndustrialContent.LUBE_CAN.get())){
            if(!l.isClientSide && CivicAccess.allowed(l,pos,p) && l.getBlockEntity(pos) instanceof IndustrialBlockEntity machine)machine.canister(p,hand);
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        return MachineConstruction.useOn(stack,state,l,pos,p);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        if(!l.isClientSide&&CivicAccess.allowed(l,pos,p)&&l.getBlockEntity(pos) instanceof IndustrialBlockEntity machine)p.openMenu(machine);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level l,BlockPos p,BlockState next,boolean moving){if(!state.is(next.getBlock())&&l.getBlockEntity(p) instanceof IndustrialBlockEntity machine){if(!l.isClientSide&&kind==Kind.PUMP)ModeledDerrick.dismantle(machine);Containers.dropContents(l,p,machine);}super.onRemove(state,l,p,next,moving);}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(switch(kind){case PUMP->"Crude oil: 50 mB/s from oil fields";case REFINERY->"Coal-fired crude oil heater";case DRILL->"Fuel-powered coal seam extraction";case TANK->"16,000 mB · one liquid";case COLUMN->"Hot crude → vapor, oil and sulfur";case CONDENSER->"Vapor → refined fuel · 40 mB/s";}).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind!=Kind.TANK)lines.add(Component.literal(kind==Kind.PUMP?"Place at ground level; open to assemble.":"Place for the construction guide.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind==Kind.PUMP)lines.add(Component.literal("Clear 8 × 10 × 26 blocks behind controller.").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.literal("Drain liquids before breaking.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
