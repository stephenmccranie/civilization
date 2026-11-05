package dev.civilization.mixin;

import dev.civilization.WoodenBucket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hand and dispenser buckets both scoop through here; the wooden bucket leaves lava where it is. */
@Mixin(LiquidBlock.class)
public abstract class LavaPickupMixin {
    @Inject(method="pickupBlock",at=@At("HEAD"),cancellable=true)
    private void civilization$woodenBucket(Player player,LevelAccessor level,BlockPos pos,BlockState state,CallbackInfoReturnable<ItemStack> ci){
        if(WoodenBucket.refuses(((LiquidBlock)(Object)this).fluid,player))ci.setReturnValue(ItemStack.EMPTY);
    }
}
