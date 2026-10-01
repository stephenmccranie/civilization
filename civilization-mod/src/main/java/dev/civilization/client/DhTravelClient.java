package dev.civilization.client;

import dev.civilization.DhTravelPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class DhTravelClient {
    private static final DhTravelPolicy POLICY=new DhTravelPolicy();
    private static volatile DhTravelPolicy.View view=new DhTravelPolicy.View(1);
    private static Vec3 last;
    private static Object level;
    private static long lastTime;
    public static DhTravelPolicy.View view(){return view;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();long now=System.nanoTime();
        if(mc.level==null||mc.player==null||mc.isPaused()||level!=mc.level){
            POLICY.reset();DhWarpTerrain.reset();last=null;level=mc.level;lastTime=now;view=POLICY.view();return;
        }
        Vec3 pos=mc.gameRenderer.getMainCamera().getPosition();double dt=(now-lastTime)*1e-9;
        if(last!=null){double distance=pos.distanceTo(last);
            // Ignore discontinuous camera jumps; genuine high travel speeds remain uncapped.
            if(distance>4096)POLICY.reset();else POLICY.update(distance/Math.max(dt,1e-6),dt);
        }
        last=pos;lastTime=now;
        view=POLICY.view();
    }
}
