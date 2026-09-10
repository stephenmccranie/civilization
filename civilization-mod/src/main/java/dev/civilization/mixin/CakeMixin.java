package dev.civilization.mixin;

import dev.civilization.CalorieConfig;
import dev.civilization.CalorieFoodData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CakeBlock.class)
public abstract class CakeMixin {
    @Redirect(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private static void civilization$cake(FoodData food, int nutrition, float saturation,
                                           LevelAccessor level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) CalorieFoodData.of(player).consume(player, CalorieConfig.CAKE.get(), "minecraft:cake_slice");
    }
}
