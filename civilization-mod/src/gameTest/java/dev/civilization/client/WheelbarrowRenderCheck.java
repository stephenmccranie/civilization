package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.WheelbarrowEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercise the actual renderer transform, not just the entity's numeric yaw. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class WheelbarrowRenderCheck {
    public static boolean cameraCheck;
    public static boolean gaitCheck;
    public static int gaitSamples;
    public static int rightCameraSamples,leftCameraSamples;
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void camera(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(gaitCheck&&mc.player!=null&&mc.player.getVehicle() instanceof WheelbarrowEntity barrow)assertGait(mc,barrow);
        if(!cameraCheck||mc.player==null||!(mc.player.getVehicle() instanceof WheelbarrowEntity barrow))return;
        float turn=Mth.wrapDegrees(mc.player.getYRot()-mc.player.yRotO);
        if(Math.abs(turn)<.1f)return;
        // A key turn carries the view round with the handles, a step past the previous yaw, so vanilla's
        // camera interpolates it between frames instead of stepping at 20 Hz.
        float look=Mth.wrapDegrees(mc.player.getYRot()-barrow.getYRot());
        if(Math.abs(look)>.01f||Math.abs(turn)>2*WheelbarrowEntity.TURN)throw new IllegalStateException("A/D turned the view "+turn+" degrees in a tick, "+look+" off the handles");
        if(turn>0)rightCameraSamples++;else leftCameraSamples++;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void assertGait(Minecraft mc,WheelbarrowEntity e){
        var renderer=(net.minecraft.client.renderer.entity.player.PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player);
        net.minecraft.client.model.PlayerModel model=renderer.getModel();
        var walker=new net.minecraft.world.entity.monster.Zombie(mc.level);
        for(float partial:new float[]{0,.5f,1}){
            // The same production player model with an ordinary non-riding
            // entity gives vanilla's gait at this distance/stride amplitude.
            model.riding=false;
            model.setupAnim(walker,e.handlerWalkAnimation.position(partial),e.handlerWalkAnimation.speed(partial),mc.player.tickCount+partial,0,0);
            float left=model.leftLeg.xRot,right=model.rightLeg.xRot;
            model.setupAnim(mc.player,0,0,mc.player.tickCount+partial,0,0);
            if(Math.abs(left-model.leftLeg.xRot)>.0001||Math.abs(right-model.rightLeg.xRot)>.0001)
                throw new IllegalStateException("Wheelbarrow legs disagree with normal walking at partial="+partial);
        }
        gaitSamples++;
    }
    public static void assertHeading(WheelbarrowEntity e){
        var renderer=(WheelbarrowRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        var pose=new PoseStack();
        // GeckoLib passes zero rotationYaw for a non-living entity.
        renderer.applyRotations(e,pose,e.tickCount,0,1,1);
        var front=pose.last().pose().transformDirection(new Vector3f(0,0,-1)).normalize();
        var expected=new Vec3(0,0,1).yRot(-e.getYRot()*Mth.DEG_TO_RAD);
        if(front.x*expected.x+front.z*expected.z<.9999)throw new IllegalStateException("Rendered wheelbarrow heading disagrees with movement: "+front+" / "+expected);
    }
}
