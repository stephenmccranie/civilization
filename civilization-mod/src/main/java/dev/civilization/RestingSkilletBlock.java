package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** A broad countertop target, one pan per supported block. No precision placement. */
public final class RestingSkilletBlock extends Block implements EntityBlock {
    public RestingSkilletBlock() { super(Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()); registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CivicBlock.FACING); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(0, 0, 0, 16, 3, 16); }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(CivicBlock.FACING, r.rotate(s.getValue(CivicBlock.FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING))); }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP); }
    @Override protected BlockState updateShape(BlockState s, Direction d, BlockState neighbor, LevelAccessor l, BlockPos p, BlockPos other) { return d == Direction.DOWN && !canSurvive(s, l, p) ? Blocks.AIR.defaultBlockState() : s; }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new RestingSkilletEntity(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) { return !l.isClientSide && t == PrototypeStoveContent.RESTING_ENTITY.get() ? (w,p,b,e) -> ((RestingSkilletEntity)e).tick() : null; }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos pos, Player p, BlockHitResult hit) {
        if (!p.getMainHandItem().isEmpty() || !CivicAccess.allowed(l, pos, p)) return InteractionResult.FAIL;
        if (!l.isClientSide && l.getBlockEntity(pos) instanceof RestingSkilletEntity pan) {
            if (p.isShiftKeyDown()) {
                var item = SkilletItem.stack(pan.skillet(),l); pan.getPersistentData().putBoolean("lifted",true);
                l.removeBlock(pos, false); p.setItemInHand(InteractionHand.MAIN_HAND, item);
            } else {
                var meal = pan.skillet().serve();
                if (meal.isEmpty()) p.displayClientMessage(net.minecraft.network.chat.Component.literal(pan.skillet().observation()), true);
                else { if (!p.getInventory().add(meal)) p.drop(meal, false); pan.changed(); l.playSound(null, pos, net.minecraft.sounds.SoundEvents.DECORATED_POT_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, .4f, 1.2f); }
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof RestingSkilletEntity pan) {
            if (!pan.getPersistentData().getBoolean("lifted")) Containers.dropItemStack(l,p.getX()+.5,p.getY()+.1,p.getZ()+.5,SkilletItem.stack(pan.skillet(),l));
        }
        super.onRemove(s,l,p,next,moving);
    }
}
