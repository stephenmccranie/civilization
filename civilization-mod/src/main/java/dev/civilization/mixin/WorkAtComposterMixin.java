package dev.civilization.mixin;

import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorkAtComposter.class)
public abstract class WorkAtComposterMixin {
    @Inject(method = "makeBread", at = @At("HEAD"), cancellable = true)
    private void civilization$noFreeBaking(Villager villager, CallbackInfo callback) {
        callback.cancel();
    }
}
