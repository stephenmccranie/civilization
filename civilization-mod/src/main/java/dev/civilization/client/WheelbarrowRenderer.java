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
    // The load fills quarter-size blocks from the wheel end, centred on the 0.75 x 1.125 tray floor: its front-left corner in the model frame.
    private static final net.minecraft.world.phys.Vec3 TRAY=new net.minecraft.world.phys.Vec3(-.375,.616,-.3175);
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
        pose.translate(TRAY.x,TRAY.y,TRAY.z);CargoVisuals.render(e.cargo(),WheelbarrowEntity.GRID,pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();
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
    /** The debug hitbox shows the body that actually collides, turned with the carrier: its outline at the ground and the rim, joined at each corner. */
    public static void hitbox(PoseStack pose,VertexConsumer lines,WheelbarrowEntity e,float partial,float red,float green,float blue){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
        var body=WheelbarrowCollision.BODY;var at=pose.last();
        for(int i=0;i<body.corners();i++){
            int j=(i+1)%body.corners();
            for(double y:new double[]{body.bottom,body.top})line(at,lines,body.x(i),y,body.z(i),body.x(j),y,body.z(j),red,green,blue);
            line(at,lines,body.x(i),body.bottom,body.z(i),body.x(i),body.top,body.z(i),red,green,blue);
        }
        pose.popPose();
    }
    private static void line(PoseStack.Pose at,VertexConsumer lines,double x0,double y0,double z0,double x1,double y1,double z1,float red,float green,float blue){
        float dx=(float)(x1-x0),dy=(float)(y1-y0),dz=(float)(z1-z0),length=Mth.sqrt(dx*dx+dy*dy+dz*dz);
        lines.addVertex(at,(float)x0,(float)y0,(float)z0).setColor(red,green,blue,1).setNormal(at,dx/length,dy/length,dz/length);
        lines.addVertex(at,(float)x1,(float)y1,(float)z1).setColor(red,green,blue,1).setNormal(at,dx/length,dy/length,dz/length);
    }
}
