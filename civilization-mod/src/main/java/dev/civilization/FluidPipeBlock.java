package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class FluidPipeBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final BooleanProperty OPEN=BooleanProperty.create("open");
    public static final java.util.Map<Direction,BooleanProperty> CONNECTIONS=PipeBlock.PROPERTY_BY_DIRECTION;
    public FluidPipeBlock(Properties p){super(p);var s=stateDefinition.any().setValue(OPEN,true);for(var prop:CONNECTIONS.values())s=s.setValue(prop,false);registerDefaultState(s);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OPEN);for(var prop:CONNECTIONS.values())b.add(prop);}
    private boolean connects(LevelAccessor l,BlockPos p,Direction d){var s=l.getBlockState(p.relative(d));return s.is(this)||(s.getBlock() instanceof BulkBlock bulk&&bulk.liquid)||(s.getBlock() instanceof OilEngineBlock&&d.getAxis()!=Direction.Axis.Y&&d.getAxis()!=s.getValue(CivicBlock.FACING).getAxis())||s.is(IndustrialContent.PORT.get())||s.getBlock() instanceof IndustrialBlock;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){var s=defaultBlockState();for(var d:Direction.values())s=s.setValue(CONNECTIONS.get(d),connects(c.getLevel(),c.getClickedPos(),d));return s;}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos n){return s.setValue(CONNECTIONS.get(d),connects(l,p,d));}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){
        super.onPlace(s,l,p,old,moving);if(!l.isClientSide&&!old.is(this))l.scheduleTick(p,this,1);
    }
    @Override protected void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos p,net.minecraft.util.RandomSource random){
        var next=s.setValue(OPEN,true);for(var d:Direction.values())next=next.setValue(CONNECTIONS.get(d),connects(l,p,d));
        if(next!=s)l.setBlockAndUpdate(p,next);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        var axes=java.util.EnumSet.noneOf(Direction.Axis.class);
        for(var d:Direction.values())if(s.getValue(CONNECTIONS.get(d)))axes.add(d.getAxis());
        if(axes.isEmpty())return Block.box(4,4,0,12,12,16);
        var shape=axes.size()>1?Block.box(4,4,4,12,12,12):Shapes.empty();
        for(var d:Direction.values())if(s.getValue(CONNECTIONS.get(d)))shape=Shapes.or(shape,switch(d){
            case DOWN->Block.box(4,0,4,12,8,12);case UP->Block.box(4,8,4,12,16,12);
            case NORTH->Block.box(4,4,0,12,12,8);case SOUTH->Block.box(4,4,8,12,12,16);
            case WEST->Block.box(0,4,4,8,12,12);case EAST->Block.box(8,4,4,16,12,12);});
        return shape;
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PipeFlowEntity(p,s);}
    @Override protected boolean triggerEvent(BlockState s,Level l,BlockPos p,int id,int value){
        if(l.getBlockEntity(p) instanceof PipeFlowEntity pipe){pipe.flow(id,value);return true;}return false;
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack s,net.minecraft.world.item.Item.TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag f){lines.add(net.minecraft.network.chat.Component.literal("50 mB/s · shared across the network"));lines.add(net.minecraft.network.chat.Component.literal("Up to 128 pipes per network"));}
}
