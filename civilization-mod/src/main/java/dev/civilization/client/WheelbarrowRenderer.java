package dev.civilization.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.civilization.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class WheelbarrowRenderer extends GeoEntityRenderer<WheelbarrowEntity> {
    public WheelbarrowRenderer(EntityRendererProvider.Context c){super(c,new MachineGeoModel<>("wheelbarrow"));shadowRadius=.6f;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(WheelbarrowContent.ENTITY.get(),WheelbarrowRenderer::new);}
    @Override public RenderType getRenderType(WheelbarrowEntity e,net.minecraft.resources.ResourceLocation t,MultiBufferSource b,float partial){return RenderType.entityCutoutNoCull(t);}
    @Override public void render(WheelbarrowEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        // The wheel rests on its own ground, which lies below the body's resting height over a step down.
        pose.pushPose();pose.translate(0,e.wheelDrop(partial),0);
        super.render(e,yaw,partial,pose,buffers,light);
        pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
        var axle=WheelbarrowEntity.AXLE;
        pose.translate(axle.x,axle.y,axle.z);pose.mulPose(Axis.XP.rotationDegrees(e.carriagePitch(partial)));pose.translate(-axle.x,-axle.y,-axle.z);
        CargoVisuals.render(e.cargoIcon(),e.cargoFill(),pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();
    }
    @Override protected void applyRotations(WheelbarrowEntity e,PoseStack pose,float age,float ignoredYaw,float partial,float scale){
        // GeckoLib supplies zero body yaw for non-LivingEntity animatables.
        super.applyRotations(e,pose,age,Mth.rotLerp(partial,e.yRotO,e.getYRot()),partial,scale);
    }
    @Override public void preRender(PoseStack pose,WheelbarrowEntity e,software.bernie.geckolib.cache.object.BakedGeoModel model,MultiBufferSource b,VertexConsumer v,boolean rerender,float partial,int light,int overlay,int color){
        super.preRender(pose,e,model,b,v,rerender,partial,light,overlay,color);
        getGeoModel().getBone("carriage").ifPresent(bone->bone.setRotX(e.carriagePitch(partial)*Mth.DEG_TO_RAD));
        // Distance / tyre radius, never a free-running timer when parked.
        getGeoModel().getBone("wheel").ifPresent(bone->bone.setRotX(-Mth.lerp(partial,e.previousTravel,e.renderedTravel())/WheelbarrowEntity.WHEEL_RADIUS));
    }
    /** The debug hitbox shows the measured surfaces that actually collide, turned with the carrier. */
    public static void hitbox(PoseStack pose,VertexConsumer lines,WheelbarrowEntity e,float partial,float red,float green,float blue){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
        for(var box:WheelbarrowCollision.BODY)LevelRenderer.renderLineBox(pose,lines,box,red,green,blue,1);
        pose.popPose();
    }
}
