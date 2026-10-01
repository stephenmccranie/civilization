package dev.civilization.client;

import dev.civilization.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BakingOvenRenderer implements BlockEntityRenderer<BakingOvenEntity> {
    private final GeoBlockRenderer<BakingOvenEntity> renderer=new GeoBlockRenderer<>(new MachineGeoModel<>("baking_oven")){
        @Override public RenderType getRenderType(BakingOvenEntity a,net.minecraft.resources.ResourceLocation t,MultiBufferSource b,float partial){return RenderType.entityCutoutNoCull(t);}
        @Override public void renderRecursively(PoseStack p,BakingOvenEntity a,software.bernie.geckolib.cache.object.GeoBone bone,RenderType type,MultiBufferSource b,VertexConsumer v,boolean rerender,float partial,int light,int overlay,int color){
            if(bone.getName().equals("window_glass")){type=RenderType.entityTranslucent(getTextureLocation(a));v=b.getBuffer(type);}
            super.renderRecursively(p,a,bone,type,b,v,rerender,partial,light,overlay,color);
        }
        @Override public void preRender(PoseStack p,BakingOvenEntity a,software.bernie.geckolib.cache.object.BakedGeoModel m,MultiBufferSource b,VertexConsumer v,boolean rerender,float partial,int light,int overlay,int color){
            super.preRender(p,a,m,b,v,rerender,partial,light,overlay,color);
            getGeoModel().getBone("oven_door").ifPresent(bone->bone.setRotY((float)Math.toRadians(100*(a.previousDoor+(a.door-a.previousDoor)*partial))));
            getGeoModel().getBone("draft_control").ifPresent(bone->bone.setRotZ((float)Math.toRadians(-135+270*a.dial())));
            for(String name:new String[]{"bread","tray"})getGeoModel().getBone(name).ifPresent(bone->bone.setHidden(true));
        }
    };
    public BakingOvenRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(BakingOvenContent.ENTITY.get(),BakingOvenRenderer::new);}
    @Override public AABB getRenderBoundingBox(BakingOvenEntity a){return new AABB(a.getBlockPos()).inflate(3,4,3);}
    @Override public void render(BakingOvenEntity a,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){var front=a.getBlockState().getValue(CivicBlock.FACING);var right=front.getClockWise();var back=front.getOpposite();p.pushPose();p.translate((right.getStepX()+back.getStepX())*.5,0,(right.getStepZ()+back.getStepZ())*.5);renderer.render(a,partial,p,b,light,overlay);p.popPose();}
}
