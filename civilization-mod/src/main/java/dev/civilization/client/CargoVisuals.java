package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.CargoMass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

/** A bounded representative stack, never one rendered object per stored item. */
public final class CargoVisuals {
    private static final int[] BARROW_ORDER={2,3,0,1,4,5},STOCKPILE_ORDER={4,3,5,1,7,0,2,6,8};
    private CargoVisuals(){}
    public static void render(ItemStack icon,float fill,boolean barrow,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(icon.isEmpty()||fill<=0)return;
        var state=CargoMass.material(icon);if(state.isAir())return;
        int columns=barrow?2:3,limit=barrow?12:18,count=Math.clamp((int)Math.ceil(fill*limit),1,limit);
        float size=barrow?.32f:.28f,height=barrow?.20f:.26f,bottom=barrow?.617f:.125f;
        for(int i=0;i<count;i++){
            int layer=i/(columns*3),cell=i%(columns*3);
            int ordered=(barrow?BARROW_ORDER:STOCKPILE_ORDER)[cell];
            int x=ordered%columns,z=ordered/columns;
            pose.pushPose();pose.translate((x-columns/2f)*size+.006f,bottom+layer*height,(z-1.5f)*size+(barrow?.18f:0)+.006f);pose.scale(size-.012f,height-.008f,size-.012f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);pose.popPose();
        }
    }
}
