package dev.civilization.mixin;

import com.mojang.blaze3d.vertex.*;
import dev.civilization.WheelbarrowEntity;
import dev.civilization.client.WheelbarrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The debug hitbox draws the carrier's turned body instead of its axis-aligned broad bounds. */
@Mixin(EntityRenderDispatcher.class)
public abstract class WheelbarrowHitboxMixin {
    @Inject(method="renderHitbox",at=@At("HEAD"),cancellable=true)
    private static void civilization$measured(PoseStack pose,VertexConsumer lines,Entity entity,float partial,float red,float green,float blue,CallbackInfo ci){
        if(entity instanceof WheelbarrowEntity e){WheelbarrowRenderer.hitbox(pose,lines,e,partial,red,green,blue);ci.cancel();}
    }
}
