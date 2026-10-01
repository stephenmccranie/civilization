package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Chooses the ordinary static wheel or the controller's animated wheel, never both. */
public final class EngineFlywheelBlock extends EnginePieceBlock {
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    public EngineFlywheelBlock() { super(true); registerDefaultState(defaultBlockState().setValue(LINKED, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LINKED);
    }
    public static void refresh(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EngineFlywheelBlock block)) return;
        var front = state.getValue(CivicBlock.FACING);
        var controller = pos.relative(front.getClockWise()).relative(front).below();
        boolean linked = level.hasChunkAt(controller)
                && level.getBlockEntity(controller) instanceof OilEngineEntity engine && engine.front() == front;
        if (state.getValue(LINKED) != linked) level.setBlock(pos, state.setValue(LINKED, linked), Block.UPDATE_CLIENTS);
        // Restore the static model after controller removal without scans or forced chunks.
        if (linked) level.scheduleTick(pos, block, 20);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel server) server.scheduleTick(pos, this, 1);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { refresh(level, pos); }
}
