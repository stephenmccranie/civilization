package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.WheelbarrowEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Exercise the actual renderer transform, not just the entity's numeric yaw. */
public final class WheelbarrowRenderCheck {
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
