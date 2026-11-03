package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a pallet's stack one for one: each stored block is a quarter-size block, 4x4 per layer and four
 * layers above the timber, filled row by row from the bottom. Pieces fill eighth cells by volume.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class StockpileRenderer implements BlockEntityRenderer<StockpileEntity> {
    private static final float CELL=.25f,LAYER=3.5f/16,BASE=2f/16,SEAM=.004f;
    private final BlockState[] eighths=new BlockState[StockpileEntity.CAPACITY*8];
    public StockpileRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(CargoContent.ENTITY.get(),StockpileRenderer::new);}
    @Override public void render(StockpileEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int filled=0;var stack=e.stack();
        for(int i=0;i<stack.size()&&filled<eighths.length;i++){
            var entry=stack.entry(i);double volume=CargoMass.volume(entry.item());
            if(!(volume>0))continue; // Unknown legacy goods still count against capacity but have no shape to draw.
            var state=CargoMass.material(entry.item());int each=(int)Math.round(volume*8);
            for(long n=0;n<entry.count()&&filled<eighths.length;n++)for(int k=0;k<each&&filled<eighths.length;k++)eighths[filled++]=state;
        }
        for(int cell=0;cell*8<filled;cell++){
            int layer=cell/StockpileEntity.LAYER,row=cell%StockpileEntity.LAYER/4,column=cell%4,first=cell*8;
            float x=column*CELL,y=BASE+layer*LAYER,z=row*CELL;
            if(whole(first,filled))
                block(eighths[first],x,y,z,CELL,LAYER,pose,buffers,light,overlay);
            else for(int k=0;k<8&&first+k<filled;k++)
                block(eighths[first+k],x+k%2*CELL/2,y+k/4*LAYER/2,z+k/2%2*CELL/2,CELL/2,LAYER/2,pose,buffers,light,overlay);
        }
    }
    /** A cell holding one material throughout draws as one block instead of eight pieces. */
    private boolean whole(int first,int filled){
        if(first+8>filled)return false;
        for(int k=1;k<8;k++)if(eighths[first+k]!=eighths[first])return false;
        return true;
    }
    private static void block(BlockState state,float x,float y,float z,float width,float height,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(x+SEAM,y,z+SEAM);pose.scale(width-2*SEAM,height-SEAM,width-2*SEAM);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);pose.popPose();
    }
}
