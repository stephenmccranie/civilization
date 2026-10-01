package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Rain must actually reach the open work face; a vent may exhaust above its roof. */
public final class MachineWeather {
    private MachineWeather() {}

    public static boolean wetWorkFace(Level level, BlockPos controller, BlockState state) {
        if (level == null || !ThermalConfig.enabled() || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return false;
        BlockPos face = controller.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        return level.hasChunkAt(face) && level.isRainingAt(face);
    }
}
