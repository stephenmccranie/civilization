package dev.civilization;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class FoundryBlock extends KilnBlock {
    public static final MapCodec<FoundryBlock> CODEC = simpleCodec(FoundryBlock::new);
    public FoundryBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<FoundryBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FoundryBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, KilnContent.FOUNDRY_ENTITY.get(), KilnBlockEntity::tick);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.literal("Ore → ingots · iron → steel"));
        lines.add(net.minecraft.network.chat.Component.literal("Coal-fired · place for the build guide"));
    }
}
