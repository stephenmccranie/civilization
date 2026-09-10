package dev.civilization.mixin;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    // Filter on access so both newly generated and saved villagers obey the production gate.
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void civilization$kilnGate(CallbackInfoReturnable<MerchantOffers> callback) {
        callback.getReturnValue().removeIf(offer -> offer.getResult().is(Items.BRICK)
                || dev.civilization.CookingContent.requiresCooking(offer.getResult()));
    }
}
