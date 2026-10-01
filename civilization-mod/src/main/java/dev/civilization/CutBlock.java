package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class CutBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CutBlock> CODEC = simpleCodec(CutBlock::new);
    public static final DirectionProperty SIDE = DirectionProperty.create("side");
    public static final IntegerProperty UNITS = IntegerProperty.create("units", 1, 3);
    public static final IntegerProperty CORNER = IntegerProperty.create("corner", 0, 3);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty REVISION = BooleanProperty.create("revision");
    public static final IntegerProperty LIGHT_FACES = IntegerProperty.create("light_faces",0,63);
    private static final VoxelShape[] LIGHT_SHAPES = lightShapes();
    public CutBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(SIDE, Direction.DOWN).setValue(UNITS, 2).setValue(CORNER, 0).setValue(WATERLOGGED,false).setValue(REVISION,false).setValue(LIGHT_FACES,0)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(SIDE, UNITS, CORNER, WATERLOGGED, REVISION, LIGHT_FACES); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return true; }
    public static AABB bounds(BlockState state) {
        return CutGeometry.bounds(state.getValue(UNITS),state.getValue(SIDE),state.getValue(CORNER));
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return level.getBlockEntity(pos) instanceof CutBlockEntity cut && cut.hasCells()?CutCells.shape(cut.cells()):Shapes.create(bounds(state)); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return LIGHT_SHAPES[state.getValue(LIGHT_FACES)]; }
    private static VoxelShape[] lightShapes(){
        var shapes=new VoxelShape[64];
        for(int mask=0;mask<shapes.length;mask++){
            VoxelShape shape=Shapes.empty();
            for(var face:Direction.values())if((mask&(1<<face.ordinal()))!=0){
                VoxelShape plane=switch(face){
                    case WEST -> Shapes.box(0,0,0,1d/16,1,1);
                    case EAST -> Shapes.box(15d/16,0,0,1,1,1);
                    case DOWN -> Shapes.box(0,0,0,1,1d/16,1);
                    case UP -> Shapes.box(0,15d/16,0,1,1,1);
                    case NORTH -> Shapes.box(0,0,0,1,1,1d/16);
                    case SOUTH -> Shapes.box(0,0,15d/16,1,1,1);
                };
                shape=Shapes.or(shape,plane);
            }
            shapes[mask]=shape;
        }
        return shapes;
    }
    @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(level.getBlockEntity(pos) instanceof CutBlockEntity cut)cut.syncLightFaces();
    }
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(state);
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        if(state.getValue(WATERLOGGED))level.scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        return super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    public static Direction orientation(Direction face, double x, double y, double z, boolean forceFace) {
        if (forceFace) return face.getOpposite();
        // Face center attaches to that face; aiming within its outer quarter selects that edge.
        double[] p = {x,y,z}; Direction[] low = {Direction.WEST, Direction.DOWN, Direction.NORTH};
        Direction best = face.getOpposite(); double distance = .25;
        for (int axis = 0; axis < 3; axis++) {
            if (axis == (face.getAxis() == Direction.Axis.X ? 0 : face.getAxis() == Direction.Axis.Y ? 1 : 2)) continue;
            double edge = Math.min(p[axis], 1-p[axis]);
            if (edge < distance) { distance = edge; best = p[axis] < .5 ? low[axis] : low[axis].getOpposite(); }
        }
        return best;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CutBlockEntity(pos, state); }
    private static BlockState source(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CutBlockEntity cut ? cut.material() : Blocks.COBBLESTONE.defaultBlockState();
    }
    @Override protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof CutBlockEntity cut){float progress=Float.MAX_VALUE;int count=0;for(var cell:cut.cells())if(cell!=null){progress=Math.min(progress,cell.getDestroyProgress(player,level,pos));count++;}return count==0?0:progress*8/count;}
        return source(level,pos).getDestroyProgress(player,level,pos);
    }
    @Override public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if(level.getBlockEntity(pos) instanceof CutBlockEntity cut)for(var cell:cut.cells())if(cell!=null && !cell.canHarvestBlock(level,pos,player))return false;
        return source(level,pos).canHarvestBlock(level,pos,player);
    }
    @Override public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        return source(level,pos).getSoundType(level,pos,entity);
    }
    @Override public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return source(level,pos).getExplosionResistance(level,pos,explosion);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof CutBlockEntity cut) cut.material(CuttingContent.material(stack));
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder loot) {
        return loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CutBlockEntity cut
                ? CutCells.drops(cut.cells()) : java.util.List.of();
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if(level.getBlockEntity(pos) instanceof CutBlockEntity cut && CutCells.canonical(cut.cells())==null)return CuttingContent.stack(cut.material(),3,1);
        return CuttingContent.stack(level.getBlockEntity(pos) instanceof CutBlockEntity cut ? cut.material() : Blocks.COBBLESTONE.defaultBlockState(), state.getValue(UNITS), 1);
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation) {return CutGeometry.state(CutGeometry.rotate(bounds(state),rotation)).setValue(WATERLOGGED,state.getValue(WATERLOGGED));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror) {
        var b=bounds(state);
        return CutGeometry.state(mirror==Mirror.LEFT_RIGHT?new AABB(b.minX,b.minY,1-b.maxZ,b.maxX,b.maxY,1-b.minZ):mirror==Mirror.FRONT_BACK?new AABB(1-b.maxX,b.minY,b.minZ,1-b.minX,b.maxY,b.maxZ):b).setValue(WATERLOGGED,state.getValue(WATERLOGGED));
    }
}
