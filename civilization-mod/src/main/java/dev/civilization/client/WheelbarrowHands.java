package dev.civilization.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.civilization.WheelbarrowEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** The handler's arms reach from their shoulders to the moving grips: posed in third person, drawn in first. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class WheelbarrowHands {
    // Vanilla draws players at 15/16 scale.
    private static final float SCALE=.9375f;
    // The grips sit nearer the eye than vanilla's first-person hand, so first-person arms are slimmed to its size.
    private static final float SLIM=.65f;
    private WheelbarrowHands(){}
    /** Points a player-model arm from its shoulder at a grip given relative to the handler's feet in the carrier's model frame. */
    public static void aim(ModelPart arm,Vec3 grip){
        var rest=arm.getInitialPose();
        var to=grip.subtract(-rest.x/16*SCALE,(1.501-rest.y/16)*SCALE,rest.z/16*SCALE).normalize();
        // Player models mirror x and y, and an arm hangs along its +y.
        arm.xRot=(float)Math.asin(to.z);arm.yRot=0;arm.zRot=(float)Math.atan2(to.x,-to.y);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hideHeldItems(RenderHandEvent event){
        var p=Minecraft.getInstance().player;if(p!=null&&p.getVehicle() instanceof WheelbarrowEntity)event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!(p.getVehicle() instanceof WheelbarrowEntity e)||mc.getCameraEntity()!=p||!mc.options.getCameraType().isFirstPerson()||mc.options.hideGui||p.isInvisible())return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var model=((PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(p)).getModel();
        var skin=p.getSkin().texture();var buffers=mc.renderBuffers().bufferSource();var pose=event.getPoseStack();
        var feet=p.getPosition(partial).subtract(event.getCamera().getPosition());
        var view=RenderSystem.getModelViewStack();view.pushMatrix();view.mul(event.getModelViewMatrix());RenderSystem.applyModelViewMatrix();mc.getMainRenderTarget().bindWrite(false);
        int light=mc.getEntityRenderDispatcher().getPackedLightCoords(p,partial);
        pose.pushPose();
        try{
            // The player renderer's own body frame, so both views share one arm pose.
            pose.translate(feet.x,feet.y,feet.z);pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
            pose.scale(-SCALE,-SCALE,SCALE);pose.translate(0,-1.501,0);
            draw(pose,buffers,model.rightArm,model.rightSleeve,e.gripFromHandler(1,partial),p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),skin,light);
            draw(pose,buffers,model.leftArm,model.leftSleeve,e.gripFromHandler(-1,partial),p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),skin,light);
            buffers.endBatch(RenderType.entitySolid(skin));buffers.endBatch(RenderType.entityTranslucent(skin));
        }finally{pose.popPose();view.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    private static void draw(PoseStack pose,MultiBufferSource buffers,ModelPart arm,ModelPart sleeve,Vec3 grip,boolean showSleeve,ResourceLocation skin,int light){
        var oldArm=arm.storePose();var oldSleeve=sleeve.storePose();boolean av=arm.visible,sv=sleeve.visible;
        pose.pushPose();
        try{
            arm.loadPose(arm.getInitialPose());aim(arm,grip);arm.translateAndRotate(pose);pose.scale(SLIM,1,SLIM);
            arm.loadPose(PartPose.ZERO);sleeve.loadPose(PartPose.ZERO);arm.visible=true;sleeve.visible=showSleeve;
            arm.render(pose,buffers.getBuffer(RenderType.entitySolid(skin)),light,OverlayTexture.NO_OVERLAY);
            if(showSleeve)sleeve.render(pose,buffers.getBuffer(RenderType.entityTranslucent(skin)),light,OverlayTexture.NO_OVERLAY);
        }finally{arm.loadPose(oldArm);sleeve.loadPose(oldSleeve);arm.visible=av;sleeve.visible=sv;pose.popPose();}
    }
}
