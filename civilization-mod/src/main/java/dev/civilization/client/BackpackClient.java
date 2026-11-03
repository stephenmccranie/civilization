package dev.civilization.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.civilization.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The backpack key, the supply badge beside the Hand, the pack drawn on the wearer's back and its tooltip. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class BackpackClient {
    public static final KeyMapping OPEN=new KeyMapping("key.civilization.backpack",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_B,"key.categories.civilization");
    /** The worn pack and the badge's text, refreshed each tick rather than each frame. */
    private static ItemStack badgePack=ItemStack.EMPTY;
    private static Component badge;
    private BackpackClient(){}

    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        while(OPEN.consumeClick())if(mc.screen==null&&mc.player!=null)PacketDistributor.sendToServer(new Backpack.Open());
        var pack=mc.player==null?null:Backpack.worn(mc.player);
        badgePack=pack==null?ItemStack.EMPTY:pack.pack();badge=pack==null?null:badge(mc.player,pack);
    }
    /** Blocks in the pack against its cap, then the bound store's distance: green while building draws from it. */
    private static Component badge(Player p,Backpack.Contents pack){
        if(!BlockCarry.restricted(p))return null;
        var text=Component.literal((Backpack.blocks(pack.pack())-pack.blockRoom())+"/"+Backpack.blocks(pack.pack()));
        var s=pack.pack().get(Backpack.SUPPLY.get());if(s==null)return text.withStyle(ChatFormatting.GRAY);
        double distance=distance(p,s);boolean near=distance<=Backpack.RADIUS;
        String where=!s.on()?"supply off":Double.isNaN(distance)?s.storeName()+" not nearby":s.storeName()+" "+Math.round(distance)+" m";
        return text.append(" · "+where).withStyle(s.on()&&near?ChatFormatting.GREEN:ChatFormatting.GRAY);
    }
    /** The client's own estimate; the server decides, and explains a refusal when the hand runs out. */
    static double distance(Player p,Backpack.Supply s){
        if(p.level().dimension()!=s.dimension())return Double.NaN;
        return s.store().map(at->Vec3.atCenterOf(at).distanceTo(p.position()),
                id->p.level().getEntitiesOfClass(WheelbarrowEntity.class,p.getBoundingBox().inflate(Backpack.RADIUS*2),w->w.getUUID().equals(id)).stream().findFirst().map(w->w.position().distanceTo(p.position())).orElse(Double.NaN));
    }

    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
        var stack=e.getItemStack();if(!stack.is(Backpack.ITEM))return;
        e.getToolTip().add(Component.literal(Backpack.slots(stack)+" slots · up to "+Backpack.blocks(stack)+" blocks").withStyle(ChatFormatting.GRAY));
        e.getToolTip().add(Component.translatable("tooltip.civilization.backpack").withStyle(ChatFormatting.GRAY));
        var s=stack.get(Backpack.SUPPLY.get());
        if(s!=null)e.getToolTip().add(Component.literal("Bound to a "+s.storeName()+" · supply "+(s.on()?"on":"off")).withStyle(ChatFormatting.GRAY));
    }

    @EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(OPEN);}
        @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(Backpack.MENU.get(),BackpackScreen::new);}
        /** Left of the Hand slot beside the hotbar. */
        @SubscribeEvent public static void hud(RegisterGuiLayersEvent e){
            e.registerAbove(VanillaGuiLayers.HOTBAR,ResourceLocation.parse("civilization:backpack"),(g,delta)->{
                var mc=Minecraft.getInstance();if(badge==null||mc.options.hideGui||mc.player==null)return;
                int right=g.guiWidth()/2-91-29-4,y=g.guiHeight()-19;
                g.renderItem(badgePack,right-16,y);
                g.drawString(mc.font,badge,right-20-mc.font.width(badge),y+4,0xffffffff,true);
            });
        }
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(var skin:e.getSkins())if(e.getSkin(skin) instanceof PlayerRenderer renderer)renderer.addLayer(new Worn(renderer));
        }
    }

    /** The backpack on the wearer's back, drawn from its item model; other players' worn items arrive by Accessories.Worn. */
    static final class Worn extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Worn(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,float limb,float swing,float partial,float age,float yaw,float pitch){
            var inventory=player.getData(Accessories.INVENTORY);ItemStack pack=ItemStack.EMPTY;
            for(int i=0;i<inventory.getSlots();i++)if(inventory.getStackInSlot(i).is(Backpack.ITEM))pack=inventory.getStackInSlot(i);
            if(pack.isEmpty()||player.isInvisible())return;
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            // Body space runs y down with the back at z=2, outside any chestplate at 3. The item model is drawn
            // centred and upright, its north face, the straps, against the back.
            pose.translate(0,6/16f,(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()?4.5f:5.5f)/16f);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            Minecraft.getInstance().getItemRenderer().renderStatic(pack,ItemDisplayContext.NONE,light,OverlayTexture.NO_OVERLAY,pose,buffers,player.level(),player.getId());
            pose.popPose();
        }
    }
}
