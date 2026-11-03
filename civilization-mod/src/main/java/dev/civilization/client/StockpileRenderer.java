package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** A pallet's stack: each stored block is a quarter-size block, 4x4 per layer and four layers above the timber. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class StockpileRenderer implements BlockEntityRenderer<StockpileEntity> {
    private static final float BASE=2f/16;
    public StockpileRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(CargoContent.ENTITY.get(),StockpileRenderer::new);}
    @Override public void render(StockpileEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(0,BASE,0);CargoVisuals.render(e.stack(),StockpileEntity.GRID,pose,buffers,light,overlay);pose.popPose();
    }
}
