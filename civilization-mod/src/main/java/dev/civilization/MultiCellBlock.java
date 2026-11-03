package dev.civilization;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A block whose one item placement fills several cells. Placement and the client's placement preview share these cells. */
public interface MultiCellBlock {
    record Cell(BlockPos pos, BlockState state) {}
    /** Every cell a placement of `state` at `pos` fills, with the state it receives; `pos` comes first. */
    List<Cell> cells(BlockPos pos, BlockState state);
}
