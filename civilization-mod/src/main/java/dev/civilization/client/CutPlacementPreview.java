package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid="civilization", value=Dist.CLIENT)
public final class CutPlacementPreview {
    @SubscribeEvent public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if(SlabIntegration.isSlab(event.getItemStack()))event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.civilization.cut_placement"));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null || mc.screen!=null || mc.options.hideGui || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        InteractionHand hand=CuttingContent.piece(mc.player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var held=mc.player.getItemInHand(hand); if(!CuttingContent.piece(held)) return;
        var context=new BlockPlaceContext(mc.player,hand,held,hit);
        var plan=CutPlacement.plan(context);var state=plan.added();var pos=plan.pos();
        boolean valid=plan.valid();
        var pose=event.getPoseStack(); var camera=event.getCamera().getPosition();
        var matrix=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack(); matrix.pushMatrix(); matrix.mul(event.getModelViewMatrix()); com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false); pose.pushPose();
        try {
            pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
            var buffers=mc.renderBuffers().bufferSource();
            if(valid && PreviewConfig.MODE.get()==PreviewConfig.Mode.TEXTURED) {
                var consumer=new TranslucentVertexConsumer(buffers.getBuffer(PreviewRenderTypes.TEXTURED),PreviewConfig.OPACITY.get().floatValue());
                mc.getBlockRenderer().renderSingleBlock(state,pose,type->consumer,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        net.neoforged.neoforge.client.model.data.ModelData.builder().with(CutBlockEntity.MATERIAL,CuttingContent.material(held)).build(),PreviewRenderTypes.TEXTURED);
                buffers.endBatch(PreviewRenderTypes.TEXTURED);
            }
            var bounds = state.is(CuttingContent.PIECE.get()) ? CutBlock.bounds(state) : state.getShape(mc.level,pos).bounds();
            net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,buffers.getBuffer(PreviewRenderTypes.OUTLINE),bounds.inflate(.002),valid?.5f:1,valid?1:.2f,valid?1:.2f,.65f);
            buffers.endBatch(PreviewRenderTypes.OUTLINE);
        } finally {pose.popPose();matrix.popMatrix();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();}
    }
}
