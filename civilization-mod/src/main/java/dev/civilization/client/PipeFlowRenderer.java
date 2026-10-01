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
/** Colored flow inside clear static pipe geometry; empty pipes render no backing. */
@EventBusSubscriber(modid="civilization",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PipeFlowRenderer implements BlockEntityRenderer<PipeFlowEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    public PipeFlowRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(IndustrialContent.PIPE_ENTITY.get(),PipeFlowRenderer::new);}
    @Override public int getViewDistance(){return 48;}
    @Override public void render(PipeFlowEntity pipe,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(pipe.getLevel()==null)return;
        var state=pipe.getBlockState();var axes=java.util.EnumSet.noneOf(Direction.Axis.class);
        for(var d:Direction.values())if(state.getValue(FluidPipeBlock.CONNECTIONS.get(d)))axes.add(d.getAxis());
        double time=pipe.getLevel().getGameTime()+partial;
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for(var d:Direction.values()){
            if(!state.getValue(FluidPipeBlock.CONNECTIONS.get(d)))continue;
            int i=d.ordinal();boolean active=pipe.until[i]>time;
            if(!active)continue;
            int color=PipeFlowEntity.color(pipe.fluids[i]);
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(d.getRotation());
            for(int face=0;face<8;face++){
                pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(face*45));
                float start=axes.size()>1?.251f:0;
                for(int n=0;n<8;n++){
                    float a=start+(.5f-start)*n/8,b=start+(.5f-start)*(n+1)/8;
                    double wave=.78+.22*Math.cos((a*4-pipe.signs[i]*time/14)*Math.PI*2);
                    quad(v,pose.last(),-.0901f,.0901f,a,b,.2175f,tint(color,wave),light,overlay);
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
