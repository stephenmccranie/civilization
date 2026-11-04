package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public class KilnBlock extends AbstractFurnaceBlock implements ModeledBlock {
    public static final MapCodec<KilnBlock> CODEC = simpleCodec(KilnBlock::new);
    public KilnBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MachineFeedback.WORKING, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MachineFeedback.WORKING);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        MachineFeedback.animate(state, level, pos, random, false);
    }
    @Override protected MapCodec<? extends KilnBlock> codec() { return CODEC; }
    /** The Kiln is built as a native model; machines sharing this block keep block structures until theirs are drawn. */
    @Override public MachineModel model() { return MachineModel.KILN; }
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return model() != null ? net.minecraft.world.level.block.RenderShape.INVISIBLE : super.getRenderShape(state);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        var modeled = ModeledMachine.of(level.getBlockEntity(pos));
        if (modeled != null) {
            if (!ModeledMachine.accepts(modeled, stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) ModeledMachine.build(modeled, player, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return MachineConstruction.useOn(stack, state, level, pos, player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new KilnBlockEntity(pos, state); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !state.is(next.getBlock())) { var modeled = ModeledMachine.of(level.getBlockEntity(pos)); if (modeled != null) ModeledMachine.dismantle(modeled); }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof KilnBlockEntity kiln) {
            var result = kiln.checkStructure();
            if (result.status() == MachineStructure.INCOMPLETE && result.problem() != null)
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.civilization.structure_missing",
                        net.minecraft.network.chat.Component.translatable("material.civilization." + result.material()), result.problem().toShortString()), false);
            player.openMenu(kiln);
        }
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, KilnContent.ENTITY.get(), KilnBlockEntity::tick);
    }
}
