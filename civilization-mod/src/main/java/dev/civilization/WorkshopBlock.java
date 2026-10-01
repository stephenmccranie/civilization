package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class WorkshopBlock extends KilnBlock {
    public final int kind;
    public WorkshopBlock(Properties p,int kind){super(p);this.kind=kind;}
    @Override protected MapCodec<WorkshopBlock> codec(){return simpleCodec(p->new WorkshopBlock(p,kind));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WorkshopBlockEntity(p,s);}
    @Override protected void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moved){
        if(!old.is(next.getBlock())&&l.getBlockEntity(p) instanceof WorkshopBlockEntity w){w.awardExperience(net.minecraft.world.phys.Vec3.atCenterOf(p));net.minecraft.world.Containers.dropContents(l,p,w);l.updateNeighbourForOutputSignal(p,this);}
        super.onRemove(old,l,p,next,moved);
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource random){
        if(kind!=2){super.animateTick(s,l,p,random);return;}
        if(!s.getValue(LIT))return;
        var back=s.getValue(FACING).getOpposite();
        var hearth=p.above();
        if(random.nextFloat()<.2f)l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,hearth.getX()+.5,hearth.getY()+.4,hearth.getZ()+.5,0,.025,0);
        var chimney=p.relative(back).above(4);
        if(random.nextFloat()<.25f)l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,chimney.getX()+.5+s.getValue(FACING).getClockWise().getStepX()*.5,chimney.getY()+.55,chimney.getZ()+.5+s.getValue(FACING).getClockWise().getStepZ()*.5,0,.035,0);
        if(random.nextFloat()<.035f)l.playLocalSound(hearth.getX()+.5,hearth.getY(),hearth.getZ()+.5,net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE,net.minecraft.sounds.SoundSource.BLOCKS,.22f,.8f,false);
    }
    @Override protected void openContainer(Level l,BlockPos p,Player player){if(l.getBlockEntity(p) instanceof WorkshopBlockEntity w&&CivicAccess.allowed(l,p,player))player.openMenu(w);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,WorkshopContent.ENTITY.get(),(world,pos,state,w)->w.tick());}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.literal(switch(kind){case 0->"Raw Hide → Leather";case 1->"Wool → Cloth";default->"Make and repair metal equipment";}));
        var counts=new java.util.LinkedHashMap<String,Integer>();
        for(var part:WorkshopStructure.parts(kind))if(!part.material().equals("air"))counts.merge(part.material()+(part.units()==2?" half":part.units()==1?" beam":part.units()==3?" cube":""),1,Integer::sum);
        lines.add(net.minecraft.network.chat.Component.literal("Build: "+counts.entrySet().stream().map(e->e.getValue()+" "+e.getKey()).collect(java.util.stream.Collectors.joining(", "))));
        lines.add(net.minecraft.network.chat.Component.literal("Coal • Place for the build guide"));
    }
}
