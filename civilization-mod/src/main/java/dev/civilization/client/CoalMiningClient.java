package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class CoalMiningClient {
    private static boolean pressed;
    private static long predicted=-100;
    private static final CoalPickMotion MOTION=new CoalPickMotion();
    private static long frameTime;
    private static float cameraYaw,cameraPitch;
    private static int predictedDuration=CoalPickItem.DURATION;
    public static double lastArc,lastSide,lastAge;
    public static boolean holding(){var p=Minecraft.getInstance().player;return p!=null&&p.getMainHandItem().is(CoalMiningContent.PICK.get());}
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
        var mc=Minecraft.getInstance();if(!e.isAttack()||mc.screen!=null||!holding())return;e.setCanceled(true);e.setSwingHand(false);
        if(e.getHand()!=InteractionHand.MAIN_HAND||pressed)return;pressed=true;
        if(mc.player.getCooldowns().isOnCooldown(CoalMiningContent.PICK.get())||mc.level.getGameTime()-predicted<predictedDuration)return;
        predictedDuration=CoalPickItem.DURATION*(!mc.player.isCreative()&&!mc.player.isSpectator()&&CalorieFoodData.of(mc.player).isDepleted()?2:1);
        predicted=mc.level.getGameTime();PacketDistributor.sendToServer(new CoalPickPayload(mc.player.getYRot(),mc.player.getXRot()));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!holding()){predicted=-100;pressed=false;frameTime=0;MOTION.reset();return;}if(!mc.options.keyAttack.isDown()||mc.screen!=null)pressed=false;}
    @EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Extensions {
        @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(CoalMiningContent.SHOVEL.get(),net.minecraft.resources.ResourceLocation.parse("civilization:loaded"),(s,l,p,seed)->CoalMiningContent.load(s)>0?1:0));}
        @SubscribeEvent public static void register(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){
            @Override public boolean applyForgeHandTransform(PoseStack pose,LocalPlayer p,HumanoidArm arm,ItemStack s,float partial,float equip,float swing){
                int sign=arm==HumanoidArm.RIGHT?1:-1;var t=CoalPickItem.state(s);long server=t.contains("coalSwing")?t.getLong("coalSwing"):-100;
                // Acknowledging this local click must not restart its predicted windup.
                boolean local=predicted>=0&&server<=predicted+8;
                long start=local?predicted:server;int duration=local?predictedDuration:Math.max(CoalPickItem.DURATION,t.getInt("coalDuration"));double age=(p.level().getGameTime()+partial-start)*CoalPickItem.DURATION/duration;
                long now=System.nanoTime();float viewYaw=p.getViewYRot(partial),viewPitch=p.getViewXRot(partial);
                double dt=(now-frameTime)/1e9;
                if(frameTime==0||dt>.25||Minecraft.getInstance().isPaused())MOTION.reset();
                else MOTION.advance(net.minecraft.util.Mth.wrapDegrees(viewYaw-cameraYaw),viewPitch-cameraPitch,dt);
                frameTime=now;cameraYaw=viewYaw;cameraPitch=viewPitch;
                var motion=CoalPickMotion.swing(age,CoalPickItem.CONTACT,CoalPickItem.DURATION);
                lastArc=motion.pitch()-MOTION.pitch();lastSide=MOTION.yaw();lastAge=age;
                pose.translate(sign*.52+MOTION.yaw()*.004+sign*motion.x(),-.42-equip*.6-MOTION.pitch()*.003+motion.y(),-.8+motion.z());
                pose.mulPose(Axis.YP.rotationDegrees((float)-MOTION.yaw()));pose.mulPose(Axis.XP.rotationDegrees((float)-MOTION.pitch()));
                // Rotate around the lower grip, giving the pick head a broad physical arc.
                pose.translate(0,-.22,0);
                pose.mulPose(Axis.YP.rotationDegrees((float)(sign*motion.yaw())));pose.mulPose(Axis.XP.rotationDegrees((float)motion.pitch()));pose.mulPose(Axis.ZP.rotationDegrees((float)(sign*(12+motion.roll())+MOTION.yaw()*.3)));
                pose.translate(0,.22,0);return true;
            }
        },CoalMiningContent.PICK.get());}
    }
}
