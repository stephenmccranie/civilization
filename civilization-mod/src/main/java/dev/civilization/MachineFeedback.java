package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Nearby client animation only. No persistent effects, world scans or forced particles. */
public final class MachineFeedback {
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    private MachineFeedback() {}

    public static void animate(BlockState state, Level level, BlockPos pos, RandomSource random, boolean cooking) {
        if (!state.getValue(AbstractFurnaceBlock.LIT)) return;
        boolean working = state.getValue(WORKING);
        var facing = state.getValue(AbstractFurnaceBlock.FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.53;
        double y = pos.getY() + 0.3;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.53;
        if (random.nextFloat() < 0.035f)
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    working ? 0.22f : 0.10f, cooking ? 1.15f : 0.8f, false);
        if (random.nextFloat() < 0.25f)
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        if (working && random.nextFloat() < 0.4f)
            level.addParticle(cooking ? ParticleTypes.CLOUD : ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 0, 0.025, 0);
    }
}
