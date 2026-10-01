package dev.civilization.mixin;

import dev.civilization.WorkshopJobs;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class WorkshopRepairMixin {
    @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("HEAD"),cancellable=true)
    private void workshop$repair(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> ci){
        for(int i=0;i<input.size();i++)if(WorkshopJobs.gated(input.getItem(i))){ci.setReturnValue(false);return;}
    }
}
