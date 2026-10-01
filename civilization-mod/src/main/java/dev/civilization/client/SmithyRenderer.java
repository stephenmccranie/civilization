package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** A shallow visual coal bed and pack-native animated fire, never real world fire or free fuel. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SmithyRenderer implements BlockEntityRenderer<WorkshopBlockEntity> {
    public SmithyRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(WorkshopContent.ENTITY.get(),SmithyRenderer::new);}
    @Override public AABB getRenderBoundingBox(WorkshopBlockEntity e){return new AABB(e.getBlockPos()).inflate(3);}
    @Override public void render(WorkshopBlockEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(e.kind!=2||e.getLevel()==null)return;
        var front=e.getBlockState().getValue(WorkshopBlock.FACING);var behind=front.getOpposite();
        var p=e.getBlockPos();var level=e.getLevel();
        if(!level.getBlockState(p.relative(behind)).is(Blocks.BRICKS))return;
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch(front){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));pose.translate(-.5,0,-.5);
        var renderer=Minecraft.getInstance().getBlockRenderer();
        for(int x=0;x<2;x++)for(int z=0;z<2;z++){
            if(!level.getBlockState(p.relative(front.getClockWise(),x).relative(behind,z).above()).isAir())continue;
            pose.pushPose();pose.translate(x+.05,1.005,z+.05);pose.scale(.9f,.12f,.9f);
            renderer.renderSingleBlock(Blocks.COAL_BLOCK.defaultBlockState(),pose,buffers,light,overlay);pose.popPose();
            if(e.getBlockState().getValue(WorkshopBlock.LIT)){
                pose.pushPose();pose.translate(x+.05,1.12,z+.05);pose.scale(.9f,.3f,.9f);
                renderer.renderSingleBlock(Blocks.FIRE.defaultBlockState(),pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
            }
        }
        pose.popPose();
    }
}
