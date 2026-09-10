package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class FertilizerRetortBlock extends KilnBlock {
    public static final MapCodec<FertilizerRetortBlock> CODEC = simpleCodec(FertilizerRetortBlock::new);
    public FertilizerRetortBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<FertilizerRetortBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FertilizerRetortBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, KilnContent.RETORT_ENTITY.get(), KilnBlockEntity::tick);
    }
}
