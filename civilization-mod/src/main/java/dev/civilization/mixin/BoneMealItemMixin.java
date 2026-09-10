package dev.civilization.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void civilization$explain(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null)
            context.getPlayer().displayClientMessage(Component.translatable("message.civilization.bone_meal_disabled"), true);
        callback.setReturnValue(InteractionResult.FAIL);
    }

    // The static entry points also cover dispensers, villagers and underwater vegetation.
    @Inject(method = "applyBonemeal", at = @At("HEAD"), cancellable = true)
    private static void civilization$noGrowth(ItemStack stack, Level level, BlockPos pos, Player player,
                                              CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "growWaterPlant", at = @At("HEAD"), cancellable = true)
    private static void civilization$noWaterGrowth(ItemStack stack, Level level, BlockPos pos, Direction direction,
                                                   CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
