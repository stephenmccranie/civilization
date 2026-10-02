package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** One native oven, with bounded precomputed collision cells and one controller. */
public final class BakingOvenBlock extends Block implements EntityBlock {
    public static final IntegerProperty CELL=IntegerProperty.create("cell",0,15);
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN;
    private static final VoxelShape[][][] SHAPES=shapes();
    public BakingOvenBlock(){super(Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH).setValue(CELL,0).setValue(OPEN,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING,CELL,OPEN);}
    public static BlockPos offset(Direction front,int cell){int x=cell%2,z=cell/2%2,y=cell/4;return switch(front){case EAST->new BlockPos(-z,y,x);case SOUTH->new BlockPos(-x,y,-z);case WEST->new BlockPos(z,y,-x);default->new BlockPos(x,y,z);};}
    public static BakingOvenEntity owner(Level l,BlockPos p,BlockState s){if(!s.is(BakingOvenContent.OVEN.get()))return null;var at=p.subtract(offset(s.getValue(CivicBlock.FACING),s.getValue(CELL)));return l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof BakingOvenEntity oven?oven:null;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());return fits(c.getLevel(),c.getClickedPos(),s.getValue(CivicBlock.FACING),c)?s:null;}
    public static boolean fits(Level l,BlockPos p,Direction front,BlockPlaceContext c){for(int i=0;i<12;i++){var at=p.offset(offset(front,i));if(!l.isInWorldBounds(at)||!l.hasChunkAt(at)||!l.getBlockState(at).canBeReplaced(c)||c.getPlayer()!=null&&!CivicAccess.allowed(l,at,c.getPlayer()))return false;}for(int i=0;i<4;i++){var under=p.offset(offset(front,i)).below();if(!l.getBlockState(under).isFaceSturdy(l,under,Direction.UP))return false;}return true;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){if(!l.isClientSide)for(int i=1;i<12;i++)l.setBlock(p.offset(offset(s.getValue(CivicBlock.FACING),i)),s.setValue(CELL,i),3);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(CELL)==0?new BakingOvenEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return s.getValue(CELL)==0&&type==BakingOvenContent.ENTITY.get()?(w,p,b,e)->{if(w.isClientSide)((BakingOvenEntity)e).clientTick();else((BakingOvenEntity)e).serverTick();}:null;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[s.getValue(CivicBlock.FACING).get2DDataValue()][s.getValue(OPEN)?1:0][s.getValue(CELL)];}
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){var oven=owner(l,p,s);if(oven==null)return InteractionResult.PASS;if(!l.isClientSide&&CivicAccess.allowed(l,p,player)&&CivicAccess.allowed(l,oven.getBlockPos(),player))toggle(oven);return InteractionResult.sidedSuccess(l.isClientSide);}
    public static void toggle(BakingOvenEntity oven){var l=oven.getLevel();var s=oven.getBlockState();boolean open=!s.getValue(OPEN);for(int i=0;i<12;i++){var at=oven.getBlockPos().offset(offset(s.getValue(CivicBlock.FACING),i));var part=l.getBlockState(at);if(part.is(s.getBlock()))l.setBlock(at,part.setValue(OPEN,open),3);}l.playSound(null,oven.getBlockPos(),open?net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN:net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.5f,.8f);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!l.isClientSide&&!s.is(next.getBlock())){var oven=owner(l,p,s);if(oven!=null&&!oven.removing){if(s.getValue(CELL)!=0)l.destroyBlock(oven.getBlockPos(),true);else{oven.removing=true;for(int i=1;i<16;i++){var at=p.offset(offset(s.getValue(CivicBlock.FACING),i));var part=l.getBlockState(at);if(part.is(this)&&part.getValue(CELL)==i&&part.getValue(CivicBlock.FACING)==s.getValue(CivicBlock.FACING))l.removeBlock(at,false);}}}}super.onRemove(s,l,p,next,moving);}
    private static VoxelShape[][][] shapes(){
        var result=new VoxelShape[4][2][16];
        for(var front:Direction.Plane.HORIZONTAL)for(int open=0;open<2;open++){
            var boxes=new java.util.ArrayList<AABB>();
            // Model-unit shell; the chamber is hollow, and the moving door is visual outside the base.
            double[][] fixed={{0,4,0,32,6,32},{0,6,0,3,40,32},{29,6,0,32,40,32},{3,6,29,29,40,32},{3,6,0,29,19,29},{3,37,0,29,40,29},{13,40,25,19,51,31}};
            for(var b:fixed)add(boxes,front,b);
            for(int x:new int[]{1,26})for(int z:new int[]{1,26})add(boxes,front,new double[]{x,0,z,x+5,4,z+5});
            if(open==0)add(boxes,front,new double[]{2,18,-2,30,37,-.25});
            for(int cell=0;cell<16;cell++){var o=offset(front,cell);var bounds=new AABB(o);VoxelShape shape=Shapes.empty();for(var b:boxes)if(b.intersects(bounds)){var clipped=b.intersect(bounds).move(-o.getX(),-o.getY(),-o.getZ());shape=Shapes.or(shape,Shapes.create(clipped));}result[front.get2DDataValue()][open][cell]=shape.optimize();}
        }return result;
    }
    private static void add(java.util.List<AABB> boxes,Direction front,double[] b){double x=b[0]/16,z=(4+b[2]*.875)/16,X=b[3]/16,Z=(4+b[5]*.875)/16;double[] a=rotate(front,x,z),c=rotate(front,X,Z);boxes.add(new AABB(Math.min(a[0],c[0]),b[1]*.72/16,Math.min(a[1],c[1]),Math.max(a[0],c[0]),b[4]*.72/16,Math.max(a[1],c[1])));}
    private static double[] rotate(Direction f,double x,double z){return switch(f){case EAST->new double[]{1-z,x};case SOUTH->new double[]{1-x,1-z};case WEST->new double[]{z,1-x};default->new double[]{x,z};};}
}
