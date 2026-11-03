package dev.civilization;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Two static native halves, one inventory. No per-frame geometry or polling for adjacent stock. */
public final class AssemblyBenchBlock extends WorkshopBlock implements MultiCellBlock {
    public static final BooleanProperty PART=BooleanProperty.create("part"), WIDE=BooleanProperty.create("wide");
    private static final VoxelShape[][] SHAPES=shapes();
    public AssemblyBenchBlock(Properties properties){super(properties,ManufacturingJobs.BENCH);registerDefaultState(defaultBlockState().setValue(PART,false).setValue(WIDE,false));}
    @Override protected MapCodec<AssemblyBenchBlock> codec(){return simpleCodec(AssemblyBenchBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(PART,WIDE);}
    public static BlockPos other(BlockPos pos,BlockState state){return pos.relative(state.getValue(PART)?state.getValue(FACING).getCounterClockWise():state.getValue(FACING).getClockWise());}
    public static BlockPos root(BlockPos pos,BlockState state){return state.getValue(PART)?other(pos,state):pos;}
    private static boolean paired(BlockState a,BlockState b){return b.is(a.getBlock())&&b.getValue(WIDE)&&a.getValue(PART)!=b.getValue(PART)&&a.getValue(FACING)==b.getValue(FACING);}
    public static boolean complete(Level level,BlockPos pos,BlockState state){return !state.getValue(WIDE)||level.hasChunkAt(other(pos,state))&&paired(state,level.getBlockState(other(pos,state)));}
    public static boolean allowed(Level level,BlockPos pos,BlockState state,Player player){return CivicAccess.allowed(level,pos,player)&&(!state.getValue(WIDE)||level.hasChunkAt(other(pos,state))&&CivicAccess.allowed(level,other(pos,state),player));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var state=defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()).setValue(WIDE,true);
        var pos=context.getClickedPos();var mate=other(pos,state);var level=context.getLevel();
        return level.hasChunkAt(mate)&&!level.isOutsideBuildHeight(mate)&&level.getWorldBorder().isWithinBounds(mate)
            &&level.getBlockState(mate).canBeReplaced(context)&&allowed(level,pos,state,context.getPlayer())&&level.noCollision(new AABB(mate))?state:null;
    }
    @Override public java.util.List<Cell> cells(BlockPos pos,BlockState state){return java.util.List.of(new Cell(pos,state),new Cell(other(pos,state),state.setValue(PART,true)));}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack){
        if(!level.isClientSide&&state.getValue(WIDE)&&!state.getValue(PART)){var mate=cells(pos,state).get(1);level.setBlock(mate.pos(),mate.state(),3);}
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(PART)?null:super.newBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return state.getValue(PART)?null:super.getTicker(level,state,type);}
    @Override protected void openContainer(Level level,BlockPos pos,Player player){
        var state=level.getBlockState(pos);var owner=root(pos,state);
        if(level.hasChunkAt(owner)&&allowed(level,pos,state,player))super.openContainer(level,owner,player);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        // Creative breaking the extension removes the owner without producing a bench item.
        if(!level.isClientSide&&player.isCreative()&&state.getValue(WIDE)&&state.getValue(PART)&&level.hasChunkAt(other(pos,state))&&paired(state,level.getBlockState(other(pos,state))))level.removeBlock(other(pos,state),false);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
        if(!level.isClientSide&&!state.is(next.getBlock())&&state.getValue(WIDE)){
            var mate=other(pos,state);
            if(level.hasChunkAt(mate)&&paired(state,level.getBlockState(mate))){
                if(state.getValue(PART))level.destroyBlock(mate,true);else level.removeBlock(mate,false);
            }
        }
        super.onRemove(state,level,pos,next,moved);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(WIDE)?SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(PART)?1:0]:super.getShape(state,level,pos,context);}
    private static VoxelShape[][] shapes(){
        var result=new VoxelShape[4][2];
        double[][] boxes={{0,12.5,0,32,14.5,16},{3,9.5,1,29,12.5,2.3},{3,9.5,13.7,29,12.5,15},{3,4,4,29,5,13}};
        var all=new java.util.ArrayList<double[]>(java.util.Arrays.asList(boxes));
        for(int x:new int[]{1,28})for(int z:new int[]{1,12})all.add(new double[]{x,0,z,x+3,12.5,z+3});
        for(var front:Direction.Plane.HORIZONTAL)for(int part=0;part<2;part++){
            VoxelShape shape=Shapes.empty();
            for(var b:all){double min=Math.max(part*16,b[0]),max=Math.min((part+1)*16,b[3]);if(min>=max)continue;
                double[] a=rotate(front,(min-part*16)/16,b[2]/16),c=rotate(front,(max-part*16)/16,b[5]/16);
                shape=Shapes.or(shape,Shapes.create(new AABB(Math.min(a[0],c[0]),b[1]/16,Math.min(a[1],c[1]),Math.max(a[0],c[0]),b[4]/16,Math.max(a[1],c[1]))));
            }
            result[front.get2DDataValue()][part]=shape.optimize();
        }
        return result;
    }
    private static double[] rotate(Direction facing,double x,double z){return switch(facing){case EAST->new double[]{1-z,x};case SOUTH->new double[]{1-x,1-z};case WEST->new double[]{z,1-x};default->new double[]{x,z};};}
}
