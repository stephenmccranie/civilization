package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws a store's goods one for one: each stored block is one quarter-size block in the store's grid, filling
 * a layer row by row from its first row, then the next layer up, in the order the goods arrived. Slabs
 * and saw-cut pieces fill eighth cells by volume. Stock beyond the grid draws as a full grid.
 */
public final class CargoVisuals {
    private static BlockState[] eighths=new BlockState[0];
    private CargoVisuals(){}
    /** Draws from the grid's first-row, first-column, bottom corner at the pose origin. */
    public static void render(CargoLedger stock,CargoGrid grid,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int room=grid.capacity()*8,filled=0;
        if(eighths.length<room)eighths=new BlockState[room];
        for(int i=0;i<stock.size()&&filled<room;i++){
            var entry=stock.entry(i);double volume=CargoMass.volume(entry.item());
            if(!(volume>0))continue; // Unknown legacy goods still count against capacity but have no shape to draw.
            var state=CargoMass.material(entry.item());int each=(int)Math.round(volume*8);
            for(long n=0;n<entry.count()&&filled<room;n++)for(int k=0;k<each&&filled<room;k++)eighths[filled++]=state;
        }
        float width=CargoGrid.CELL,height=grid.height();
        for(int cell=0;cell*8<filled;cell++){
            int level=cell/grid.layer(),row=cell%grid.layer()/grid.across(),column=cell%grid.across(),first=cell*8;
            float x=column*width,y=level*height,z=row*width;
            if(whole(first,filled))
                block(eighths[first],x,y,z,width,height,pose,buffers,light,overlay);
            else for(int k=0;k<8&&first+k<filled;k++)
                block(eighths[first+k],x+k%2*width/2,y+k/4*height/2,z+k/2%2*width/2,width/2,height/2,pose,buffers,light,overlay);
        }
    }
    /** A cell holding one material throughout draws as one block instead of eight pieces. */
    private static boolean whole(int first,int filled){
        if(first+8>filled)return false;
        for(int k=1;k<8;k++)if(eighths[first+k]!=eighths[first])return false;
        return true;
    }
    private static void block(BlockState state,float x,float y,float z,float width,float height,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(x,y,z);pose.scale(width,height,width);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);pose.popPose();
    }
}
