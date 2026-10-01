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
    @Override protected void onRemove(BlockState state,Level l,BlockPos p,BlockState next,boolean moving){if(!state.is(next.getBlock())&&l.getBlockEntity(p) instanceof IndustrialBlockEntity machine)Containers.dropContents(l,p,machine);super.onRemove(state,l,p,next,moving);}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(switch(kind){case PUMP->"50 mB/s crude. Build over an oil field.";case REFINERY->"50 mB/s crude -> heated crude. Requires Coal.";case DRILL->"Refined fuel -> coal. Build over a coal seam.";case TANK->"16,000 mB. One liquid at a time.";case COLUMN->"50 mB/s heated crude -> 40 vapor + 10 oil, plus sulfur.";case CONDENSER->"40 mB/s vapor -> refined fuel. Air-cooled.";}).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind!=Kind.TANK){
            var counts=new java.util.LinkedHashMap<String,Integer>();
            for(var part:IndustrialStructure.parts(kind))if(!part.material().equals("air"))counts.merge(part.material()+(part.units()==2?" half":part.units()==1?" beam":part.units()==3?" eighth":""),1,Integer::sum);
            var entries=counts.entrySet().stream().map(e->e.getValue()+" "+e.getKey()).toList();
            for(int i=0;i<entries.size();i+=3)lines.add(Component.literal((i==0?"Build: ":"       ")+String.join(", ",entries.subList(i,Math.min(i+3,entries.size())))).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if(kind!=Kind.TANK)lines.add(Component.literal("Right-click controller with structural items to fill the guide.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(kind!=Kind.TANK)lines.add(Component.literal(kind==Kind.PUMP?"Set controller two blocks above the foundation. Pipe from the far output port.":kind==Kind.DRILL?"Liquid intake: back.":"Connect pipes to the flanged ports in the build.").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.literal("Canisters: right-click. Drain before breaking.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
