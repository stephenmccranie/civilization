package dev.civilization.perf;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.PerfRecorder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each entity render is timed on the CPU as one object of its type. */
@Mixin(EntityRenderDispatcher.class)
public abstract class PerfEntityMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void civilization$begin(E entity, double x, double y, double z, float yaw, float partialTick, PoseStack pose,
                                                       MultiBufferSource buffers, int light, CallbackInfo ci) {
        PerfRecorder.INSTANCE.beginObject(entity.getType(), "entity", BuiltInRegistries.ENTITY_TYPE);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private <E extends Entity> void civilization$end(E entity, double x, double y, double z, float yaw, float partialTick, PoseStack pose,
                                                     MultiBufferSource buffers, int light, CallbackInfo ci) {
        PerfRecorder.INSTANCE.endObject();
    }
}
