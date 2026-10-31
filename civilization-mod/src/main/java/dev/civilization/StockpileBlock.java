package dev.civilization;

import net.minecraft.core.BlockPos;
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
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,2,16);
    public StockpileBlock(){super(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2,3600000).noOcclusion().pushReaction(PushReaction.BLOCK));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new StockpileEntity(p,s);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return !level.isClientSide&&type==CargoContent.ENTITY.get()?(l,p,s,e)->((StockpileEntity)e).tick():null;}
    private InteractionResult open(Level l,BlockPos pos,Player p){
        if(!l.isClientSide&&l.getBlockEntity(pos) instanceof StockpileEntity e&&e.cargoValid(p))p.openMenu(e);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult h){return open(l,pos,p);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult h){open(l,pos,p);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event){
        if(event.getLevel().getBlockEntity(event.getPos()) instanceof StockpileEntity e&&!e.cargo().isEmpty()){
            event.setCanceled(true);event.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("Empty the stockpile before removing it."),true);
        }
    }
}
