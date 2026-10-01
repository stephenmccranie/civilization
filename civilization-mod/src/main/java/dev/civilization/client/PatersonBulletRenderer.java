package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Small dull lead projectile, not a glowing tracer. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class PatersonBulletRenderer extends EntityRenderer<PatersonBullet> {
    private static final ResourceLocation TEXTURE=ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    public PatersonBulletRenderer(EntityRendererProvider.Context c){super(c);}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(FirearmContent.BULLET.get(),PatersonBulletRenderer::new);}
    @Override public ResourceLocation getTextureLocation(PatersonBullet b){return TEXTURE;}
    @Override public void render(PatersonBullet a,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){var v=b.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));PrototypeStoveRenderer.box(v,p,-.025f,-.025f,-.025f,.025f,.025f,.025f,0xff88898a,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);}
}
