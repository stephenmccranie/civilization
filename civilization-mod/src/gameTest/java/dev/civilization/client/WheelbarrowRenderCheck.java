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
    public static int rightCameraSamples,leftCameraSamples;
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void camera(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(!cameraCheck||mc.player==null||!(mc.player.getVehicle() instanceof WheelbarrowEntity e))return;
        float turn=Mth.wrapDegrees(e.getYRot()-e.yRotO);
        if(Math.abs(turn)<.1f)return;
        float offset=Mth.wrapDegrees(mc.player.getYRot()-e.getYRot());
        // Exercise Camera.setup itself at sub-tick times, in all three views.
        // A final-heading-only assertion misses a 20 Hz camera staircase.
        for(var mode:net.minecraft.client.CameraType.values()){
            var camera=new net.minecraft.client.Camera();
            for(int frame=0;frame<=4;frame++){
                float partial=frame/4f;
                camera.setup(mc.level,mc.player,!mode.isFirstPerson(),mode.isMirrored(),partial);
                float expected=Mth.rotLerp(partial,e.yRotO,e.getYRot())+offset+(mode.isMirrored()?180:0);
                if(Math.abs(Mth.wrapDegrees(camera.getYRot()-expected))>.05f)
                    throw new IllegalStateException("Steering camera stepped instead of interpolating: "+mode+" partial="+partial+" actual="+camera.getYRot()+" expected="+expected);
            }
        }
        if(turn>0)rightCameraSamples++;else leftCameraSamples++;
    }
    public static void assertHeading(WheelbarrowEntity e){
        var renderer=(WheelbarrowRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        var pose=new PoseStack();
        // GeoEntityRenderer passes zero rotationYaw for a non-living entity.
        renderer.applyRotations(e,pose,e.tickCount,0,1,1);
        var front=pose.last().pose().transformDirection(new Vector3f(0,0,-1)).normalize();
        var expected=new Vec3(0,0,1).yRot(-e.getYRot()*Mth.DEG_TO_RAD);
        if(front.x*expected.x+front.z*expected.z<.9999)throw new IllegalStateException("Rendered wheelbarrow heading disagrees with movement: "+front+" / "+expected);
    }
}
