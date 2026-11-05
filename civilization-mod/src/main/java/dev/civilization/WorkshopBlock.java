package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public class WorkshopBlock extends KilnBlock {
    /** The Smithy is a forge built as a native model; the other workshops keep block structures. */
    @Override public MachineModel model() { return switch (kind) { case 0 -> MachineModel.TANNERY; case 2 -> MachineModel.SMITHY; default -> null; }; }
    public final int kind;
    public WorkshopBlock(Properties p,int kind){super(p);this.kind=kind;}
    private static final net.minecraft.world.phys.shapes.VoxelShape BENCH_SHAPE=net.minecraft.world.phys.shapes.Shapes.or(
        box(0,12,0,16,16,16),box(1,0,1,3,12,3),box(13,0,1,15,12,3),box(1,0,13,3,12,15),box(13,0,13,15,12,15));
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return kind==ManufacturingJobs.BENCH?BENCH_SHAPE:super.getShape(state,level,pos,context);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos){return kind==ManufacturingJobs.BENCH?net.minecraft.world.phys.shapes.Shapes.empty():super.getOcclusionShape(state,level,pos);}
    @Override protected MapCodec<? extends WorkshopBlock> codec(){return simpleCodec(p->new WorkshopBlock(p,kind));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WorkshopBlockEntity(p,s);}
    @Override protected void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moved){
        if(!old.is(next.getBlock())&&l.getBlockEntity(p) instanceof WorkshopBlockEntity w){w.awardExperience(net.minecraft.world.phys.Vec3.atCenterOf(p));net.minecraft.world.Containers.dropContents(l,p,w);l.updateNeighbourForOutputSignal(p,this);}
        super.onRemove(old,l,p,next,moved);
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource random){
        if(WorkshopBlockEntity.fired(kind))super.animateTick(s,l,p,random);
    }
    /** A water bucket fills the Tannery's pit and starts its batch. */
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(kind!=0||!stack.is(net.minecraft.world.item.Items.WATER_BUCKET))return super.useItemOn(stack,state,level,pos,player,hand,hit);
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof WorkshopBlockEntity w)w.pour(player,hand);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void openContainer(Level l,BlockPos p,Player player){if(l.getBlockEntity(p) instanceof WorkshopBlockEntity w&&CivicAccess.allowed(l,p,player))player.openMenu(w);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,WorkshopContent.ENTITY.get(),(world,pos,state,w)->w.tick());}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){
        if(kind==ManufacturingJobs.BENCH){lines.add(net.minecraft.network.chat.Component.literal("Woodworking, assembly and repairs · hand worked"));return;}
        lines.add(net.minecraft.network.chat.Component.literal(switch(kind){case 0->"Hide + Tanbark → leather";case 1->"Wool → cloth";default->"Forge heads, plates, rods and fasteners";}));
        lines.add(net.minecraft.network.chat.Component.literal(kind==0?"Bark pit · fill with a water bucket · place for the build guide":"Coal-fired · place for the build guide"));
    }
}
