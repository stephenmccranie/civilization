package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class WheelbarrowClient {
    @SubscribeEvent public static void pose(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event){
        if(event.getEntity().getVehicle() instanceof WheelbarrowEntity e){event.getEntity().yBodyRot=e.getYRot();event.getEntity().yBodyRotO=e.yRotO;}
    }
    @SubscribeEvent public static void input(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof WheelbarrowEntity e))return;
        // Entity.tick resets the rider's previous yaw after the vehicle ticks.
        // Apply steering afterwards so vanilla camera interpolation retains the
        // whole turn between frames, including third-person free look.
        if(!mc.isPaused()){
            float delta=Mth.wrapDegrees(e.getYRot()-e.yRotO);
            mc.player.setYRot(mc.player.getYRot()+delta);
            mc.player.setYHeadRot(mc.player.getYHeadRot()+delta);
        }
        boolean exit=mc.options.keyShift.isDown();boolean active=mc.screen==null&&!mc.isPaused();
        int forward=active?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
        int turn=active?(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0):0;
        PacketDistributor.sendToServer(new WheelbarrowPayload(forward,turn,exit||!active));
    }
}
