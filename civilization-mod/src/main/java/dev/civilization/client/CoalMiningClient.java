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
    private static float yaw,pitch;
    private static int predictedDuration=CoalPickItem.DURATION;
    public static double lastArc,lastSide;
    public static boolean holding(){var p=Minecraft.getInstance().player;return p!=null&&p.getMainHandItem().is(CoalMiningContent.PICK.get());}
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
        var mc=Minecraft.getInstance();if(!e.isAttack()||mc.screen!=null||!holding())return;e.setCanceled(true);e.setSwingHand(false);
        if(e.getHand()!=InteractionHand.MAIN_HAND||pressed)return;pressed=true;
        if(mc.player.getCooldowns().isOnCooldown(CoalMiningContent.PICK.get())||mc.level.getGameTime()-predicted<predictedDuration)return;
        predictedDuration=!mc.player.isCreative()&&!mc.player.isSpectator()&&CalorieFoodData.of(mc.player).isDepleted()?40:20;
        predicted=mc.level.getGameTime();yaw=mc.player.getYRot();pitch=mc.player.getXRot();PacketDistributor.sendToServer(new CoalPickPayload(yaw,pitch));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){predicted=-100;pressed=false;return;}if(!mc.options.keyAttack.isDown()||!holding()||mc.screen!=null)pressed=false;}
    @EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Extensions {
        @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(CoalMiningContent.SHOVEL.get(),net.minecraft.resources.ResourceLocation.parse("civilization:loaded"),(s,l,p,seed)->CoalMiningContent.load(s)>0?1:0));}
        @SubscribeEvent public static void register(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){
            @Override public boolean applyForgeHandTransform(PoseStack pose,LocalPlayer p,HumanoidArm arm,ItemStack s,float partial,float equip,float swing){
                int sign=arm==HumanoidArm.RIGHT?1:-1;var t=CoalPickItem.state(s);long server=t.contains("coalSwing")?t.getLong("coalSwing"):-100;
                long start=Math.max(server,predicted);int duration=start==predicted?predictedDuration:Math.max(20,t.getInt("coalDuration"));double age=(p.level().getGameTime()+partial-start)*CoalPickItem.DURATION/duration;
                boolean active=age>=0&&age<CoalPickItem.DURATION;double arc=0,side=0;
                if(active){
                    double wind=Math.clamp(age/5,0,1),hit=Math.clamp((age-5)/2,0,1),recover=Math.clamp((age-8)/12,0,1);recover=recover*recover*(3-2*recover);
                    arc=(-50*wind+120*hit)*(1-recover);
                    float baseYaw=start==predicted?yaw:t.getFloat("swingYaw"),basePitch=start==predicted?pitch:t.getFloat("swingPitch");
                    side=Math.clamp(net.minecraft.util.Mth.wrapDegrees(p.getYRot()-baseYaw),-12,12)*(1-recover);
                    arc+=Math.clamp(p.getXRot()-basePitch,-10,10)*(1-recover);
                }
                lastArc=arc;lastSide=side;
                pose.translate(sign*.52,-.42-equip*.6,-.72);pose.translate(-sign*.08*Math.sin(Math.toRadians(arc)),.06*Math.sin(Math.toRadians(-arc)),active?-.08*Math.clamp(age/7,0,1)*(1-Math.clamp((age-8)/12,0,1)):0);
                pose.mulPose(Axis.YP.rotationDegrees((float)(sign*side)));pose.mulPose(Axis.XP.rotationDegrees((float)arc));pose.mulPose(Axis.ZP.rotationDegrees((float)(sign*(12+side*.35))));return true;
            }
        },CoalMiningContent.PICK.get());}
    }
}
