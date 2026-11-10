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

public final class IndustrialBlock extends Block implements EntityBlock, ModeledBlock {
    public enum Kind { DERRICK, HEATER, DRILL, TANK, COLUMN, CONDENSER, COAL_ENGINE }
    public final Kind kind;
    public static MachineModel model(Kind kind){return switch(kind){case DERRICK->MachineModel.OIL_DERRICK;case HEATER->MachineModel.FIRED_HEATER;case COLUMN->MachineModel.DISTILLATION_COLUMN;case COAL_ENGINE->MachineModel.COAL_ENGINE;default->null;};}
    @Override public MachineModel model(){return model(kind);}
    /** A native model fills only part of its controller's cell, so the controller hides none of its neighbours' faces
     *  and, like the model's other cells, passes light through undimmed. */
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return model()!=null?net.minecraft.world.phys.shapes.Shapes.empty():super.getOcclusionShape(s,l,p);}
    @Override protected boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return model()!=null||super.propagatesSkylightDown(s,l,p);}
    public IndustrialBlock(Properties p,Kind kind){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(MachineFeedback.WORKING,false).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,MachineFeedback.WORKING,net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new IndustrialBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return type!=IndustrialContent.ENTITY.get()?null:!l.isClientSide?(world,pos,state,entity)->((IndustrialBlockEntity)entity).tick():kind==Kind.COAL_ENGINE||kind==Kind.HEATER?(world,pos,state,entity)->smoke(world,pos,state):null;}
    /** A coal-fired machine's stack smokes while its fire burns; the Coal Engine's also puffs with every stroke while it
     *  works. The Fired Heater's stack stands 79 model units behind its firedoor's centre. */
    private void smoke(Level l,BlockPos pos,BlockState state){
        if(!state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))return;
        var back=state.getValue(CivicBlock.FACING).getOpposite();double run=kind==Kind.HEATER?79/16.0:0;
        double x=pos.getX()+.5+back.getStepX()*run,y=pos.getY()+(kind==Kind.HEATER?112:58)/16.0,z=pos.getZ()+.5+back.getStepZ()*run;var r=l.random;
        if(r.nextFloat()<.15f)l.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,x,y,z,0,.03,0);
        if(kind==Kind.COAL_ENGINE&&state.getValue(MachineFeedback.WORKING)&&l.getGameTime()%12==0)for(int i=0;i<3;i++)l.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,x+(r.nextDouble()-.5)*.2,y,z+(r.nextDouble()-.5)*.2,0,.12+r.nextDouble()*.05,0);
    }
    @Override public void animateTick(BlockState state,Level l,BlockPos pos,net.minecraft.util.RandomSource random){
        if(!state.getValue(MachineFeedback.WORKING)&&!state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))return;
        if(kind==Kind.DRILL&&random.nextFloat()<.2f)l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.05,pos.getZ()+.5,0,.025,0);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(stack.is(IndustrialContent.CAN.get())||stack.is(IndustrialContent.CRUDE_CAN.get())||stack.is(IndustrialContent.FUEL_CAN.get())||stack.is(IndustrialContent.LUBE_CAN.get())){
            if(!l.isClientSide && CivicAccess.allowed(l,pos,p) && l.getBlockEntity(pos) instanceof IndustrialBlockEntity machine)machine.canister(p,hand);
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        var modeled=ModeledMachine.of(l.getBlockEntity(pos));
        var held=modeled==null?ItemStack.EMPTY:ModeledMachine.material(modeled,p,stack);
        if(!held.isEmpty()){
            if(!l.isClientSide)ModeledMachine.build(modeled,p,held);
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        return MachineConstruction.useOn(stack,state,l,pos,p);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        if(!l.isClientSide&&CivicAccess.allowed(l,pos,p)&&l.getBlockEntity(pos) instanceof IndustrialBlockEntity machine)p.openMenu(machine);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level l,BlockPos p,BlockState next,boolean moving){if(!state.is(next.getBlock())&&l.getBlockEntity(p) instanceof IndustrialBlockEntity machine){if(!l.isClientSide&&machine.model()!=null)ModeledMachine.dismantle(machine);Containers.dropContents(l,p,machine);}super.onRemove(state,l,p,next,moving);}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(switch(kind){case DERRICK->"Drills a well, then pumps crude at 50 mB/s";case COAL_ENGINE->"Half-power engine fired by Coal";case HEATER->"Coal-fired crude oil heater";case DRILL->"Fuel-powered coal seam extraction";case TANK->"16,000 mB · one liquid";case COLUMN->"Hot crude → vapor, oil and sulfur";case CONDENSER->"Vapor → refined fuel · 40 mB/s";}).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind!=Kind.TANK)lines.add(Component.literal(model()!=null?"Supply held materials to the controller.":"Place for the construction guide.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind==Kind.DERRICK)lines.add(Component.literal("Driven by an engine within 8 blocks.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind==Kind.COAL_ENGINE)lines.add(Component.literal("Drives a machine within 8 blocks.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind!=Kind.COAL_ENGINE)lines.add(Component.literal("Drain liquids before breaking.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
