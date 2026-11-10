package dev.civilization.client;
import dev.civilization.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
/** The liquid behind the four windows of each arm's sight glass, drawn while that arm carries a transfer. */
@EventBusSubscriber(modid="civilization",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PipeFlowRenderer implements BlockEntityRenderer<PipeFlowEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    /** From the arm model: the glass's reach in from the block's face, the liquid's distance from the pipe's axis and half a window's width. */
    private static final float GLASS=2.5f/16,DEPTH=2.5f/16,HALF=1f/16;
    /** Steps of the moving highlight along one arm's glass. */
    private static final int BANDS=5;
    public PipeFlowRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.PIPE_ENTITY.get(),PipeFlowRenderer::new);}
    @Override public int getViewDistance(){return 48;}
    @Override public void render(PipeFlowEntity pipe,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(pipe.getLevel()==null)return;
        var state=pipe.getBlockState();
        double time=pipe.getLevel().getGameTime()+partial;
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for(var d:Direction.values()){
            if(!state.getValue(FluidPipeBlock.CONNECTIONS.get(d)))continue;
            int i=d.ordinal();boolean active=pipe.until[i]>time;
            if(!active)continue;
            int color=PipeFlowEntity.color(pipe.fluids[i]);
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(d.getRotation());
            for(int side=0;side<4;side++){
                pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(side*90));
                for(int n=0;n<BANDS;n++){
                    float a=.5f-GLASS+GLASS*n/BANDS,b=.5f-GLASS+GLASS*(n+1)/BANDS;
                    double wave=.78+.22*Math.cos((a*4-pipe.signs[i]*time/14)*Math.PI*2);
                    quad(v,pose.last(),-HALF,HALF,a,b,DEPTH,tint(color,wave),light,overlay);
                }
                pose.popPose();
            }
            pose.popPose();
        }
    }
    private static int tint(int c,double f){return 0xff000000|((int)(((c>>16)&255)*f)<<16)|((int)(((c>>8)&255)*f)<<8)|(int)((c&255)*f);}
    private static void quad(VertexConsumer v,PoseStack.Pose p,float x0,float x1,float y0,float y1,float z,int c,int light,int overlay){
        vertex(v,p,x0,y0,z,c,light,overlay);vertex(v,p,x1,y0,z,c,light,overlay);vertex(v,p,x1,y1,z,c,light,overlay);vertex(v,p,x0,y1,z,c,light,overlay);
    }
    private static void vertex(VertexConsumer v,PoseStack.Pose p,float x,float y,float z,int c,int light,int overlay){v.addVertex(p.pose(),x,y,z).setColor(c).setUv(.5f,.5f).setOverlay(overlay).setLight(light).setNormal(p,0,0,1);}
}
