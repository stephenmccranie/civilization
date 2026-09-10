package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public class KilnBlock extends AbstractFurnaceBlock {
    public static final MapCodec<KilnBlock> CODEC = simpleCodec(KilnBlock::new);
    public KilnBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends KilnBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new KilnBlockEntity(pos, state); }
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
