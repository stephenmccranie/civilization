package dev.civilization.mixin;

import dev.civilization.WorkshopJobs;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Mob.class)
public abstract class WorkshopMobMixin {
    @Shadow protected abstract float getEquipmentDropChance(EquipmentSlot slot);
    @Redirect(method="dropCustomDeathLoot",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Mob;getEquipmentDropChance(Lnet/minecraft/world/entity/EquipmentSlot;)F"))
    private float workshop$spawnedEquipment(Mob mob,EquipmentSlot slot){
        float chance=getEquipmentDropChance(slot);
        // Picked-up/player-supplied equipment uses a guaranteed (>1) drop chance and remains recoverable.
        return chance<=1&&WorkshopJobs.gated(mob.getItemBySlot(slot))?0:chance;
    }
}
