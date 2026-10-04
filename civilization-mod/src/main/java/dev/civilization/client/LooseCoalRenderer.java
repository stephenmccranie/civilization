package dev.civilization.client;

import dev.civilization.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class LooseCoalRenderer extends EntityRenderer<LooseCoalEntity> {
    public LooseCoalRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.12f;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(CoalMiningContent.LOOSE.get(),LooseCoalRenderer::new);e.registerEntityRenderer(CoalMiningContent.CART.get(),c->new MinecartRenderer<>(c,ModelLayers.MINECART));}
    @Override public ResourceLocation getTextureLocation(LooseCoalEntity e){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
    @Override public void render(LooseCoalEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){p.pushPose();float scale=.22f;p.scale(scale,scale,scale);p.translate(-.5,0,-.5);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.COAL_BLOCK.defaultBlockState(),p,b,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);p.popPose();super.render(e,yaw,partial,p,b,light);}
}
