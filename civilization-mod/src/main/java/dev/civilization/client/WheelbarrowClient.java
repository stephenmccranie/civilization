package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class WheelbarrowClient {
    private static WheelbarrowEntity handling;
    // The handler's walk bob; riding decays and zeroes the player's own each tick.
    private static float bob;
    @SubscribeEvent public static void pose(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event){
        if(event.getEntity().getVehicle() instanceof WheelbarrowEntity e){event.getEntity().yBodyRot=e.getYRot();event.getEntity().yBodyRotO=e.yRotO;}
    }
    @SubscribeEvent public static void input(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null||!(p.getVehicle() instanceof WheelbarrowEntity e)){handling=null;return;}
        boolean active=mc.screen==null&&!mc.isPaused();
        if(!mc.isPaused()){
            if(handling!=e){p.setYRot(e.getYRot());p.yRotO=e.getYRot();p.setYHeadRot(e.getYRot());p.yHeadRotO=e.getYRot();handling=e;bob=0;}
            // The view turns with the handles, as a rider's does with a boat, so the mouse only looks
            // around. The previous yaw is untouched, so vanilla interpolates the turn between frames.
            float turned=Mth.wrapDegrees(e.getYRot()-e.yRotO);
            if(turned!=0){p.setYRot(p.getYRot()+turned);p.setYHeadRot(p.getYHeadRot()+turned);}
            walk(p);
        }
        int forward=active?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
        int turn=active?(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0):0;
        PacketDistributor.sendToServer(new WheelbarrowPayload(forward,turn,mc.options.keyShift.isDown()||!active));
    }
    /** The handler walks, so their actual travel drives vanilla's walk distance and bob exactly as walking does. */
    private static void walk(Player p){
        float stride=(float)Math.hypot(p.getX()-p.xo,p.getZ()-p.zo);
        p.walkDist+=stride*.6f;p.oBob=bob;bob+=(Math.min(.1f,stride)-bob)*.4f;p.bob=bob;
    }
}
