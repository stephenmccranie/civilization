package dev.civilization.mixin;

import dev.civilization.CalorieConfig;
import dev.civilization.CalorieFoodData;
import dev.civilization.FoodCalories;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow protected FoodData foodData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void civilization$food(CallbackInfo ci) { foodData = new CalorieFoodData(); }

    @Redirect(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"))
    private void civilization$eat(FoodData data, FoodProperties properties, Level level, ItemStack stack, FoodProperties original) {
        Player player = (Player) (Object) this;
        if (!level.isClientSide) CalorieFoodData.of(player).consume(player, FoodCalories.of(stack, properties),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void civilization$jump(Player player, float ignored) {
        if (!player.level().isClientSide) CalorieFoodData.of(player).spendOther(player, CalorieConfig.JUMP.get(), "jump");
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void civilization$attack(Player player, float ignored) {
        if (!player.level().isClientSide) CalorieFoodData.of(player).spendOther(player, CalorieConfig.ATTACK.get(), "melee_hit");
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void civilization$peacefulHealing(Player player, float ignored) {
        // CalorieFoodData owns natural healing on every difficulty.
    }
}
