package dev.civilization.perf;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.PerfRecorder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each block entity render is timed on the CPU as one object of its type. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class PerfBlockEntityMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private <E extends BlockEntity> void civilization$begin(E entity, float partialTick, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
        PerfRecorder.INSTANCE.beginObject(entity.getType(), "be", BuiltInRegistries.BLOCK_ENTITY_TYPE);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private <E extends BlockEntity> void civilization$end(E entity, float partialTick, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
        PerfRecorder.INSTANCE.endObject();
    }
}
