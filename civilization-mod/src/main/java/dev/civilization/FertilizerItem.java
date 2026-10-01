package dev.civilization;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

public final class FertilizerItem extends Item {
    public FertilizerItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = SoilSystem.cropBase(level,context.getClickedPos());
        var state = level.getBlockState(pos);
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (state.is(FarmingContent.FERTILIZED_WHEAT.get()) || level instanceof ServerLevel s && SoilSystem.fertilized(s,pos)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.civilization.already_fertilized"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!SoilSystem.crop(state) || SoilSystem.mature(state) || state.getBlock() instanceof net.minecraft.world.level.block.AttachedStemBlock) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.civilization.fertilizer_requires_growing_wheat"), true);
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && !Geography.canFarm(server, pos.below())) {
            player.displayClientMessage(Component.translatable("message.civilization.geography.fertilizer_blocked"), true);
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server && SoilSystem.fertilize(server,pos)) {
            if(state.is(Blocks.WHEAT))level.setBlock(pos,FarmingContent.FERTILIZED_WHEAT.get().defaultBlockState().setValue(CropBlock.AGE,state.getValue(CropBlock.AGE)),3);
            if (!player.isCreative()) context.getItemInHand().shrink(1);
            CalorieFoodData.of(player).spendOther(player, CalorieConfig.FERTILIZE.get(), "fertilizer_labor");
            EnergyLog.production(player, "fertilizer_apply", pos.toShortString(), "civilization:fertilizer",
                    player.isCreative() ? 0 : 1, 0, 0);
            ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0);
            player.displayClientMessage(Component.translatable("message.civilization.fertilized"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
