package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Full ground block with a flush engraved control face. Funds live in SavedData. */
public final class ArenaBlock extends Block {
    public ArenaBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
    @Override public void setPlacedBy(Level l,BlockPos pos,BlockState s,LivingEntity e,ItemStack item){
        if(l instanceof ServerLevel level && e instanceof ServerPlayer p)ArenaService.placed(level,pos,p,s.getValue(CivicBlock.FACING));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){return MachineConstruction.useOn(stack,s,l,pos,p);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(p instanceof ServerPlayer server)ArenaMenu.open(server,pos);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&l instanceof ServerLevel level)ArenaService.removed(level,pos);
        super.onRemove(s,l,pos,next,moving);
    }
    @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag f){
        lines.add(net.minecraft.network.chat.Component.literal("Place flush in level ground at the exact oval center.").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(net.minecraft.network.chat.Component.literal("Supply stone bricks, cut slabs and oak fence gates; empty hand opens matches and wagers.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
