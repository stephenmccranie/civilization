package dev.civilization.client;

import dev.civilization.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class PrototypeStoveRenderer implements BlockEntityRenderer<PrototypeStoveEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("civilization:textures/block/prototype_stove_brass.png");
    public PrototypeStoveRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(PrototypeStoveContent.ENTITY.get(),PrototypeStoveRenderer::new);}
    @Override public void render(PrototypeStoveEntity b,float dt,PoseStack p,MultiBufferSource buffers,int light,int overlay){
        SkilletRenderer.sound(b);
        p.pushPose();SkilletRenderer.rotation(b,p);
        if(b.hasSkillet()){
            p.pushPose();p.translate(0,14.5/16,0);SkilletRenderer.vessel(p,buffers,light,overlay);SkilletRenderer.food(b.skillet(),p,buffers,light,overlay);p.popPose();
        }
        if(b.fire.lit()){
            var fire=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("civilization:textures/block/prototype_stove_ember.png")));
            for(float slotX:new float[]{2,5.25f})for(float slotY:new float[]{10,11.25f}){
                float x=(16-slotX-2.5f)/16,X=(16-slotX)/16,y=slotY/16,Y=(slotY+.5f)/16,z=-.04f/16;
                float[][] q={{x,y,z},{x,Y,z},{X,Y,z},{X,y,z}};
                for(int i=0;i<4;i++)fire.addVertex(p.last().pose(),q[i][0],q[i][1],q[i][2]).setColor(0xffffffff).setUv(i<2?0:1,i==0||i==3?1:0).setOverlay(overlay).setLight(15728880).setNormal(p.last(),0,0,-1);
            }
        }
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));p.pushPose();p.translate(3.5/16,11.5/16,-.05);p.mulPose(Axis.ZP.rotationDegrees((float)(-135+270*b.dial())));box(v,p,-.018f,0,-.018f,.018f,.09f,.006f,0xffe0b770,light,overlay);p.popPose();p.popPose();
    }
    static void box(VertexConsumer v,PoseStack p,float x,float y,float z,float X,float Y,float Z,int c,int light,int overlay){
        quad(v,p,new float[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}},0,1,0,c,light,overlay);
        quad(v,p,new float[][]{{x,y,z},{x,Y,z},{X,Y,z},{X,y,z}},0,0,-1,c,light,overlay);
        quad(v,p,new float[][]{{X,y,Z},{X,Y,Z},{x,Y,Z},{x,y,Z}},0,0,1,c,light,overlay);
        quad(v,p,new float[][]{{x,y,Z},{x,Y,Z},{x,Y,z},{x,y,z}},-1,0,0,c,light,overlay);
        quad(v,p,new float[][]{{X,y,z},{X,Y,z},{X,Y,Z},{X,y,Z}},1,0,0,c,light,overlay);
    }
    private static void quad(VertexConsumer v,PoseStack p,float[][] q,float nx,float ny,float nz,int c,int light,int overlay){for(int i=0;i<4;i++)v.addVertex(p.last().pose(),q[i][0],q[i][1],q[i][2]).setColor(c).setUv((i==2||i==3)?.7f:.3f,i<2?.3f:.7f).setOverlay(overlay).setLight(light).setNormal(p.last(),nx,ny,nz);}
}
