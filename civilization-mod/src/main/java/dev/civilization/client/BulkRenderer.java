package dev.civilization.client;
import dev.civilization.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BulkRenderer implements BlockEntityRenderer<BulkEntity> {
    private static final ResourceLocation WHITE=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    public BulkRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(BulkContent.ENTITY.get(),BulkRenderer::new);}
    @Override public AABB getRenderBoundingBox(BulkEntity b){return new AABB(b.getBlockPos()).inflate(6);}
    @Override public void render(BulkEntity b,float dt,PoseStack p,MultiBufferSource buffers,int light,int overlay){
        if(!b.formed||b.amount()==0)return;p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(b.front()){case EAST->-90;case SOUTH->180;case WEST->90;default->0;}));p.translate(-.5,0,-.5);
        if(!b.liquid){
            float fraction=b.amount()/(float)b.capacity();
            for(int x=0;x<4;x++)for(int z=0;z<8;z++){
                float ridge=(x==1||x==2)?.25f:0;float h=Math.max(.03f,Math.min(1.85f,fraction*(1.6f+ridge)));
                p.pushPose();p.translate(-.5+x*.5,.501,.5+z*.5);p.scale(.5f,h,.5f);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.COAL_BLOCK.defaultBlockState(),p,buffers,light,overlay);p.popPose();
            }
        }else{
            var v=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));int c=PipeFlowEntity.color(IndustrialContent.fluidId(b.fluid()));float top=1+b.amount()/(float)b.capacity();
            vertex(v,p,.06f,1,.505f,c,light,overlay);vertex(v,p,.06f,top,.505f,c,light,overlay);vertex(v,p,.94f,top,.505f,c,light,overlay);vertex(v,p,.94f,1,.505f,c,light,overlay);
        }p.popPose();
    }
    private static void vertex(VertexConsumer v,PoseStack p,float x,float y,float z,int c,int light,int overlay){v.addVertex(p.last().pose(),x,y,z).setColor(c).setUv(.5f,.5f).setOverlay(overlay).setLight(light).setNormal(p.last(),0,0,-1);}
}
