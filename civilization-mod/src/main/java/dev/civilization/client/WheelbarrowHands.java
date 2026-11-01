package dev.civilization.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.WheelbarrowEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Quaternionf;

/** First-person skin/sleeve arms end at the actual moving wooden grip positions. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class WheelbarrowHands {
    public record Arm(Vec3 shoulder,Vec3 grip){}
    private WheelbarrowHands(){}
    public static Arm arm(WheelbarrowEntity e,float partial,int side){
        float yaw=Mth.rotLerp(partial,e.yRotO,e.getYRot());
        var center=e.getPosition(partial);
        var shoulder=new Vec3(side*.25,1.36,1.84);
        // Native rising-handle end after its authored -24.97391 degree tilt.
        var grip=new Vec3(side*5.7375/16,.8015,1.4343);
        var pivot=new Vec3(0,.3375,-.84375);
        grip=grip.subtract(pivot).xRot(WheelbarrowEntity.PUSH_TILT_DEGREES*Mth.DEG_TO_RAD*Mth.lerp(partial,e.previousTilt,e.tilt)).add(pivot);
        float rotation=(180-yaw)*Mth.DEG_TO_RAD;
        return new Arm(center.add(shoulder.yRot(rotation)),center.add(grip.yRot(rotation)));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hideHeldItems(RenderHandEvent event){
        var p=Minecraft.getInstance().player;if(p!=null&&p.getVehicle() instanceof WheelbarrowEntity)event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!(p.getVehicle() instanceof WheelbarrowEntity e)||!mc.options.getCameraType().isFirstPerson()||mc.options.hideGui||p.isInvisible())return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var renderer=(PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(p);var model=renderer.getModel();
        var skin=p.getSkin().texture();var buffers=mc.renderBuffers().bufferSource();var pose=event.getPoseStack();var camera=event.getCamera().getPosition();
        var view=RenderSystem.getModelViewStack();view.pushMatrix();view.mul(event.getModelViewMatrix());RenderSystem.applyModelViewMatrix();mc.getMainRenderTarget().bindWrite(false);
        int light=mc.getEntityRenderDispatcher().getPackedLightCoords(p,partial);
        try{
            draw(pose,buffers,model.rightArm,model.rightSleeve,arm(e,partial,1),camera,p.getSkin().model()==PlayerSkin.Model.SLIM,1,p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),skin,light);
            draw(pose,buffers,model.leftArm,model.leftSleeve,arm(e,partial,-1),camera,p.getSkin().model()==PlayerSkin.Model.SLIM,-1,p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),skin,light);
            buffers.endBatch(RenderType.entitySolid(skin));buffers.endBatch(RenderType.entityTranslucent(skin));
        }finally{view.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    private static void draw(PoseStack pose,MultiBufferSource buffers,ModelPart arm,ModelPart sleeve,Arm placement,Vec3 camera,boolean slim,int side,boolean showSleeve,net.minecraft.resources.ResourceLocation skin,int light){
        var oldArm=arm.storePose();var oldSleeve=sleeve.storePose();boolean av=arm.visible,sv=sleeve.visible;
        var start=placement.shoulder.subtract(camera);var direction=placement.grip.subtract(placement.shoulder);double length=direction.length();
        pose.pushPose();
        try{
            pose.translate(start.x,start.y,start.z);pose.mulPose(new Quaternionf().rotationTo(0,1,0,(float)(direction.x/length),(float)(direction.y/length),(float)(direction.z/length)));
            pose.scale(1,(float)(length/.75),1);pose.translate(side*(slim?.03125:.0625),.125,0);
            arm.loadPose(PartPose.ZERO);sleeve.loadPose(PartPose.ZERO);arm.visible=true;sleeve.visible=showSleeve;
            arm.render(pose,buffers.getBuffer(RenderType.entitySolid(skin)),light,OverlayTexture.NO_OVERLAY);
            if(showSleeve)sleeve.render(pose,buffers.getBuffer(RenderType.entityTranslucent(skin)),light,OverlayTexture.NO_OVERLAY);
        }finally{arm.loadPose(oldArm);sleeve.loadPose(oldSleeve);arm.visible=av;sleeve.visible=sv;pose.popPose();}
    }
}
