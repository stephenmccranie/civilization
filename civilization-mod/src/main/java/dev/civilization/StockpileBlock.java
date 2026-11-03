package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid="civilization")
public final class StockpileBlock extends Block implements EntityBlock {
    /** The 2-pixel timber base plus each started layer of the stack. */
    private static final VoxelShape[] SHAPES=new VoxelShape[StockpileEntity.GRID.layers()+1];
    static{for(int layers=0;layers<SHAPES.length;layers++)SHAPES[layers]=Block.box(0,0,0,16,2+layers*StockpileEntity.GRID.height()*16,16);}
    public StockpileBlock(){super(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2,3600000).noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[l.getBlockEntity(p) instanceof StockpileEntity e?e.layers():0];}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new StockpileEntity(p,s);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){super.onPlace(state,level,pos,previous,moving);if(!previous.is(this))StockpileGroup.invalidateAround(level,pos);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!next.is(this))StockpileGroup.invalidateAround(level,pos);super.onRemove(state,level,pos,next,moving);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return !level.isClientSide&&type==CargoContent.ENTITY.get()?(l,p,s,e)->((StockpileEntity)e).tick():null;}
    /** Every right-click, empty-handed or not, lands here: held goods go onto the pallets, and with nothing that fits it opens them. */
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult h){
        if(!l.isClientSide&&l.getBlockEntity(pos) instanceof StockpileEntity e&&e.cargoValid(p)&&!e.putHeld(p))p.openMenu(e);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event){
        if(event.getLevel().getBlockEntity(event.getPos()) instanceof StockpileEntity e&&!e.stack().isEmpty()){
            event.setCanceled(true);event.getPlayer().displayClientMessage(Component.literal("Take the goods off this pallet before removing it."),true);
        }
    }
}
