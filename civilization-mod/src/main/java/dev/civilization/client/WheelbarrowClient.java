package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class WheelbarrowClient {
    @SubscribeEvent public static void pose(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event){
        if(event.getEntity().getVehicle() instanceof WheelbarrowEntity e){event.getEntity().yBodyRot=e.getYRot();event.getEntity().yBodyRotO=e.getYRot();}
    }
    @SubscribeEvent public static void input(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof WheelbarrowEntity))return;
        boolean exit=mc.options.keyShift.isDown();boolean active=mc.screen==null&&!mc.isPaused();
        int forward=active?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
        int turn=active?(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0):0;
        PacketDistributor.sendToServer(new WheelbarrowPayload(forward,turn,exit||!active));
    }
}
