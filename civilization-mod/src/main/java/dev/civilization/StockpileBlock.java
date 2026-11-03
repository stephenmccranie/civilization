package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * A pallet, or with TOP the block its stack rises into. The top has no state of its own: its shape, clicks and
 * breaking all belong to the pallet below, the pallet's renderer draws it, and it vanishes without that pallet.
 */
@EventBusSubscriber(modid="civilization")
public final class StockpileBlock extends Block implements EntityBlock {
    public static final BooleanProperty TOP=BooleanProperty.create("top");
    private static final int OWN=StockpileEntity.ONE_HIGH.layers();
    /** The 2-pixel timber base plus each started layer in the pallet's block, and the started layers in the block above. */
    private static final VoxelShape[] BASE=new VoxelShape[OWN+1],ABOVE=new VoxelShape[StockpileEntity.TWO_HIGH.layers()-OWN+1];
    static{
        double layer=StockpileEntity.TWO_HIGH.height()*16;
        for(int n=0;n<BASE.length;n++)BASE[n]=Block.box(0,0,0,16,2+n*layer,16);
        for(int n=0;n<ABOVE.length;n++)ABOVE[n]=n==0?Shapes.empty():Block.box(0,0,0,16,n*layer,16);
    }
    public StockpileBlock(){super(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2,3600000).noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK));registerDefaultState(defaultBlockState().setValue(TOP,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(TOP);}
    /** The pallet a cell belongs to: itself, or for a top the pallet below. */
    static StockpileEntity pallet(BlockGetter l,BlockPos p,BlockState s){return l.getBlockEntity(s.getValue(TOP)?p.below():p) instanceof StockpileEntity e?e:null;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        var e=pallet(l,p,s);int layers=e==null?0:e.layers();
        return s.getValue(TOP)?ABOVE[Math.max(0,layers-OWN)]:BASE[Math.min(layers,OWN)];
    }
    @Override protected RenderShape getRenderShape(BlockState s){return s.getValue(TOP)?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(TOP)?null:new StockpileEntity(p,s);}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState next,LevelAccessor l,BlockPos p,BlockPos from){
        return s.getValue(TOP)&&d==Direction.DOWN&&!(next.is(this)&&!next.getValue(TOP))?Blocks.AIR.defaultBlockState():s;
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block block,BlockPos from,boolean moving){
        if(from.equals(p.above())&&!s.getValue(TOP)&&l.getBlockEntity(p) instanceof StockpileEntity e)e.aboveChanged();
        super.neighborChanged(s,l,p,block,from,moving);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){super.onPlace(state,level,pos,previous,moving);if(!previous.is(this))StockpileGroup.invalidateAround(level,pos);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!next.is(this))StockpileGroup.invalidateAround(level,pos);super.onRemove(state,level,pos,next,moving);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return !level.isClientSide&&type==CargoContent.ENTITY.get()?(l,p,s,e)->((StockpileEntity)e).tick():null;}
    /** Every right-click, empty-handed or not, lands here: held goods go onto the pallets, and with nothing that fits it opens them. */
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult h){
        var e=pallet(l,pos,s);
        if(!l.isClientSide&&e!=null&&e.cargoValid(p)&&!e.putHeld(p))p.openMenu(e);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event){
        if(event.getState().is(CargoContent.STOCKPILE.get())&&pallet(event.getLevel(),event.getPos(),event.getState()) instanceof StockpileEntity e&&!e.stack().isEmpty()){
            event.setCanceled(true);event.getPlayer().displayClientMessage(Component.literal("Take the goods off this pallet before removing it."),true);
        }
    }
}
