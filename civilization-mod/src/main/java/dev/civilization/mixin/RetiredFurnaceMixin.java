package dev.civilization.mixin;

import dev.civilization.KilnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class RetiredFurnaceMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable
    private net.minecraft.world.item.crafting.RecipeManager.CachedCheck<net.minecraft.world.item.crafting.SingleRecipeInput, ? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> quickCheck;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void civilization$recipeSelection(CallbackInfo ci) {
        if ((Object)this instanceof KilnBlockEntity kiln)
            quickCheck = (input, level) -> kiln.processingRecipe(input);
    }
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void civilization$retire(Level level, BlockPos pos, BlockState state,
                                            AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (furnace instanceof KilnBlockEntity) return;
        if (state.getValue(AbstractFurnaceBlock.LIT))
            level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, false), 3);
        ci.cancel();
    }
}
