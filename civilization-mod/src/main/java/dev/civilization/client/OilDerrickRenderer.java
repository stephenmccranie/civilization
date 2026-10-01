package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Small moving crosshead inside the player-built wooden derrick. */
@EventBusSubscriber(modid="civilization", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class OilDerrickRenderer implements BlockEntityRenderer<IndustrialBlockEntity> {
    public OilDerrickRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.ENTITY.get(),OilDerrickRenderer::new);}
    @Override public AABB getRenderBoundingBox(IndustrialBlockEntity m){return new AABB(m.getBlockPos()).inflate(10,34,10);}
    @Override public int getViewDistance(){return 128;}

    @Override public void render(IndustrialBlockEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(m.kind!=IndustrialBlock.Kind.PUMP || !m.formed || m.getLevel()==null)return;
        boolean working=m.getBlockState().getValue(MachineFeedback.WORKING);
        double time=m.getLevel().getGameTime()+partial;
        double stroke=working?(1-Math.cos(time*.32))*.5:0;
        pose.pushPose();
        pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(switch(m.front()){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));
        pose.translate(-.5,0,-.5);

        // The built iron string is the fixed guide; this low crosshead strokes
        // around it. Its motion is feedback, not a required construction part.
        double y=3.1+stroke*1.2;
        block(pose,buffers,Blocks.IRON_BLOCK.defaultBlockState(),-.55,y,5.02,.45,.28,.46,light,overlay);
        block(pose,buffers,Blocks.IRON_BLOCK.defaultBlockState(),.60,y,5.02,.45,.28,.46,light,overlay);
        block(pose,buffers,Blocks.IRON_BLOCK.defaultBlockState(),-.55,y,4.68,1.60,.28,.26,light,overlay);
        block(pose,buffers,Blocks.COPPER_BLOCK.defaultBlockState(),.10,y+.28,4.73,.30,.16,.16,light,overlay);
        pose.popPose();
    }

    private static void block(PoseStack p,MultiBufferSource buffers,net.minecraft.world.level.block.state.BlockState state,
                              double x,double y,double z,double w,double h,double d,int light,int overlay){
        p.pushPose();p.translate(x,y,z);p.scale((float)w,(float)h,(float)d);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,p,buffers,light,overlay);p.popPose();
    }
}
