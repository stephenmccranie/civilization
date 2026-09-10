package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class CookingStationBlock extends AbstractFurnaceBlock {
    public static final MapCodec<CookingStationBlock> CODEC = simpleCodec(CookingStationBlock::new);
    public CookingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MachineFeedback.WORKING, false));
    }
    @Override protected MapCodec<? extends CookingStationBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MachineFeedback.WORKING);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CookingStationBlockEntity(pos, state); }
    @Override protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof CookingStationBlockEntity station) player.openMenu(station);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CookingContent.ENTITY.get(), KilnBlockEntity::tick);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MachineFeedback.animate(state, level, pos, random, true);
    }
}
