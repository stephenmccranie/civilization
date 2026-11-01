package dev.civilization.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client layout only; container slot identity and packet indices stay native. */
@Mixin(Slot.class)
public interface SlotPositionAccess {
    @Mutable @Accessor("x") void civilization$setX(int x);
}
