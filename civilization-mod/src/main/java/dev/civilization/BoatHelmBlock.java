package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class BoatHelmBlock extends HorizontalDirectionalBlock implements MultiCellBlock {
    public static final BooleanProperty PANEL=BooleanProperty.create("panel");
    /** What the helm shows: the engine order on its telegraph, reverse to fast, and its wheel turned to port, amidships or to starboard. */
    public static final IntegerProperty GEAR=IntegerProperty.create("gear",0,3),STEER=IntegerProperty.create("steer",0,2);
    /** The wheel on its pedestal and the telegraph on its column, by facing. */
    private static final VoxelShape[] WHEEL=turned(Shapes.or(Block.box(1,0,2.5,16,1.5,14.5),Block.box(4,1.5,3,12,25.25,11),Block.box(0,8,11,16,24.4,13.6)));
    private static final VoxelShape[] TELEGRAPH=turned(Shapes.or(Block.box(0,0,2.5,15,1.5,14.5),Block.box(5,1.5,5,11,12.5,11),Block.box(5.25,12.5,3.4,10.75,24,12)));
    /** A shape drawn facing north, the bow at -z, turned to each horizontal facing as the block state turns the model. */
    private static VoxelShape[] turned(VoxelShape north){
        var shapes=new VoxelShape[4];
        for(var front:Direction.Plane.HORIZONTAL){
            var shape=Shapes.empty();
            for(var b:north.toAabbs())shape=Shapes.or(shape,switch(front){case EAST->Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX);case SOUTH->Shapes.box(1-b.maxX,b.minY,1-b.maxZ,1-b.minX,b.maxY,1-b.minZ);case WEST->Shapes.box(b.minZ,b.minY,1-b.maxX,b.maxZ,b.maxY,1-b.minX);default->Shapes.create(b);});
            shapes[front.get2DDataValue()]=shape;
        }
        return shapes;
    }
    public BoatHelmBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PANEL,false).setValue(GEAR,1).setValue(STEER,1));}
    @Override protected com.mojang.serialization.MapCodec<BoatHelmBlock> codec(){return simpleCodec(BoatHelmBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,PANEL,GEAR,STEER);}
    public static BlockPos other(BlockPos p,BlockState s){return p.relative(s.getValue(PANEL)?s.getValue(FACING).getCounterClockWise():s.getValue(FACING).getClockWise());}
    public static BlockPos root(BlockPos p,BlockState s){return s.getValue(PANEL)?other(p,s):p;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){
        var s=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());var second=other(c.getClickedPos(),s);
        return c.getLevel().getWorldBorder().isWithinBounds(second)&&c.getLevel().getBlockState(second).canBeReplaced(c)&&CivicAccess.allowed(c.getLevel(),second,c.getPlayer())?s:null;
    }
    @Override public java.util.List<Cell> cells(BlockPos p,BlockState s){return java.util.List.of(new Cell(p,s),new Cell(other(p,s),s.setValue(PANEL,true)));}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){
        if(!l.isClientSide&&!s.getValue(PANEL)){var panel=cells(p,s).get(1);l.setBlock(panel.pos(),panel.state(),3);}
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        var q=other(p,s);var mate=l.getBlockState(q);
        if(mate.is(this)&&mate.getValue(PANEL)!=s.getValue(PANEL))l.destroyBlock(q,s.getValue(PANEL)&&!player.isCreative());
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return (s.getValue(PANEL)?TELEGRAPH:WHEEL)[s.getValue(FACING).get2DDataValue()];
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l instanceof net.minecraft.server.level.ServerLevel server && player instanceof net.minecraft.server.level.ServerPlayer sp){
            BoatSystem.use(server,root(p,s),sp,InteractionHand.MAIN_HAND);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag f){
        lines.add(net.minecraft.network.chat.Component.literal("Boat helm · needs a powered hull"));
        lines.add(net.minecraft.network.chat.Component.literal("W/S: gears · A/D: steer · Shift: leave"));
    }
}
