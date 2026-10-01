package dev.civilization.client;
import dev.civilization.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import java.util.*;

@EventBusSubscriber(modid="civilization",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SurveyRenderer implements BlockEntityRenderer<SurveyBlockEntity> {
    private record Entry(DynamicTexture texture,ResourceLocation id,int revision){}
    private final LinkedHashMap<SurveyBlockEntity,Entry> textures=new LinkedHashMap<>(32,.75f,true);
    private Object world;
    private static SurveyRenderer active;
    @EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
    public static final class Cleanup {
        @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload e){if(e.getLevel().isClientSide()&&active!=null){active.clear();active.world=null;}}
    }
    public SurveyRenderer(BlockEntityRendererProvider.Context context){if(active!=null)active.clear();active=this;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(CivicContent.TABLE_ENTITY.get(),SurveyRenderer::new);}
    private void clear(){var manager=Minecraft.getInstance().getTextureManager();for(var entry:textures.values())manager.release(entry.id());textures.clear();}
    private ResourceLocation texture(SurveyBlockEntity table){
        if(world!=table.getLevel()){clear();world=table.getLevel();}
        var entry=textures.get(table);
        if(entry!=null&&entry.revision()==table.revision)return entry.id();
        DynamicTexture texture;ResourceLocation id;
        if(entry==null){
            if(textures.size()>=32){var iterator=textures.entrySet().iterator();var old=iterator.next();Minecraft.getInstance().getTextureManager().release(old.getValue().id());iterator.remove();}
            texture=new DynamicTexture(256,256,true);id=Minecraft.getInstance().getTextureManager().register("survey_table",texture);
        }else{texture=entry.texture();id=entry.id();}
        var image=texture.getPixels();
        for(int z=0;z<256;z++)for(int x=0;x<256;x++){
            int color=table.terrain[(z/4)*64+x/4];if(color==0)color=0xFFB5A584;
            pixel(image,x,z,color);
        }
        // A subtle survey grid, then claims and counter pins. Coordinates remain north-up.
        for(int n=0;n<256;n+=32)for(int v=0;v<256;v++){if(v%4==0){pixel(image,n,v,0xFF9B9671);pixel(image,v,n,0xFF9B9671);}}
        for(var m:table.markers){int x=m.x()-table.getBlockPos().getX()+128,z=m.z()-table.getBlockPos().getZ()+128;
            if(m.claim()){int r=m.radius();for(int n=Math.max(0,x-r);n<=Math.min(255,x+r);n++){pixel(image,n,z-r,0xFFFFDB68);pixel(image,n,z+r,0xFFFFDB68);}for(int n=Math.max(0,z-r);n<=Math.min(255,z+r);n++){pixel(image,x-r,n,0xFFFFDB68);pixel(image,x+r,n,0xFFFFDB68);}}
            else for(int a=-3;a<=3;a++)for(int b=-3;b<=3;b++)pixel(image,x+a,z+b,Math.abs(a)==3||Math.abs(b)==3?0xFF243D45:0xFF73E0F3);
        }
        // Brass north arrow and white table center.
        for(int y=7;y<23;y++)pixel(image,244,y,0xFFFFDB68);
        for(int n=0;n<5;n++){pixel(image,244-n,7+n,0xFFFFDB68);pixel(image,244+n,7+n,0xFFFFDB68);}
        for(int x=126;x<=130;x++)for(int z=126;z<=130;z++)pixel(image,x,z,0xFFFFFFFF);
        texture.upload();textures.put(table,new Entry(texture,id,table.revision));return id;
    }
    private static void pixel(NativeImage image,int x,int y,int argb){if(x>=0&&x<256&&y>=0&&y<256)image.setPixelRGBA(x,y,(argb&0xFF00FF00)|((argb>>16)&255)|((argb&255)<<16));}
    @Override public void render(SurveyBlockEntity table,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!table.complete||table.getLevel()==null||!SurveyTable.complete(table.getLevel(),table.getBlockPos()))return;
        var corner=SurveyTable.corner(table.getLevel(),table.getBlockPos());
        pose.pushPose();pose.translate(corner.getX()-table.getBlockPos().getX(),1.003,corner.getZ()-table.getBlockPos().getZ());
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture(table)));var m=pose.last();
        vertex(v,m,.075f,.075f,0,0,light,overlay);vertex(v,m,.075f,1.925f,0,1,light,overlay);vertex(v,m,1.925f,1.925f,1,1,light,overlay);vertex(v,m,1.925f,.075f,1,0,light,overlay);
        pose.popPose();
    }
    private static void vertex(VertexConsumer v,PoseStack.Pose pose,float x,float z,float u,float w,int light,int overlay){v.addVertex(pose.pose(),x,0,z).setColor(-1).setUv(u,w).setOverlay(overlay).setLight(light).setNormal(pose,0,1,0);}
    @Override public boolean shouldRenderOffScreen(SurveyBlockEntity table){return true;}
    @Override public int getViewDistance(){return 48;}
}
