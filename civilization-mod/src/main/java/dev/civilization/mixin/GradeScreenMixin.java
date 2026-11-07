package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The screen sends a drag's slots in the order they were crossed, so the menu deals the best units out first in that order. */
@Mixin(AbstractContainerScreen.class)
public abstract class GradeScreenMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;"))
    private HashSet<Slot> civilization$dragOrder(Operation<HashSet<Slot>> original) { return new LinkedHashSet<>(); }
}
