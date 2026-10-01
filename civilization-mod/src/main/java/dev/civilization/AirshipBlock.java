package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class AirshipBlock extends Block implements EntityBlock {
    public AirshipBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(CivicBlock.FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c) { return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new AirshipBlockEntity(p,s); }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit) {
        if(p instanceof ServerPlayer player) AirshipMenu.open(player,pos);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
